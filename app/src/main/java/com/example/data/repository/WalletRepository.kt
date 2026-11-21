package com.example.data.repository

import android.content.Context
import com.example.data.local.WalletDao
import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.ContactEntity
import com.example.data.model.Currencies
import com.example.data.model.RecurringTransactionEntity
import com.example.data.model.SeedAccountIds
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.TwtAccountHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RemittanceFxResult(
    val usdDeductionTx: TransactionEntity,
    val cfaCreditTx: TransactionEntity,
    val totalUsdDeducted: BigDecimal,
    val netCfaReceived: BigDecimal
)

class WalletRepository(
    private val walletDao: WalletDao,
    context: Context? = null
) {

    private val prefs = context?.getSharedPreferences("balatec_categories_prefs", Context.MODE_PRIVATE)

    val accountsFlow: Flow<List<AccountEntity>> = walletDao.getAccountsFlow()
    val transactionsFlow: Flow<List<TransactionEntity>> = walletDao.getTransactionsFlow()
    val contactsFlow: Flow<List<ContactEntity>> = walletDao.getContactsFlow()
    val recurringTransactionsFlow: Flow<List<RecurringTransactionEntity>> = walletDao.getRecurringTransactionsFlow()

    private val _customCategoriesFlow = MutableStateFlow(loadCustomCategories())
    val customCategoriesFlow: StateFlow<List<Pair<String, String>>> = _customCategoriesFlow.asStateFlow()

    private fun loadCustomCategories(): List<Pair<String, String>> {
        val raw = prefs?.getString("custom_categories_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<Pair<String, String>>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val name = obj.optString("name").trim()
                val icon = obj.optString("icon", "🏷️").ifBlank { "🏷️" }
                if (name.isNotBlank()) list.add(name to icon)
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persistCustomCategories(list: List<Pair<String, String>>) {
        _customCategoriesFlow.value = list
        val arr = JSONArray()
        list.forEach { (name, icon) ->
            arr.put(JSONObject().apply {
                put("name", name)
                put("icon", icon)
            })
        }
        prefs?.edit()?.putString("custom_categories_json", arr.toString())?.apply()
    }

    fun addCustomCategory(name: String, icon: String) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        val cleanIcon = icon.ifBlank { "🏷️" }
        val current = _customCategoriesFlow.value.toMutableList()
        current.removeAll { it.first.equals(cleanName, ignoreCase = true) }
        current.add(cleanName to cleanIcon)
        persistCustomCategories(current)
    }

    fun deleteCustomCategory(name: String) {
        val updated = _customCategoriesFlow.value.filterNot { it.first.equals(name, ignoreCase = true) }
        persistCustomCategories(updated)
    }

    fun mergeRemoteCategories(remote: List<Pair<String, String>>) {
        if (remote.isEmpty()) return
        val merged = _customCategoriesFlow.value.toMutableList()
        for ((name, icon) in remote) {
            if (name.isNotBlank() && merged.none { it.first.equals(name, ignoreCase = true) }) {
                merged.add(name to icon.ifBlank { "🏷️" })
            }
        }
        persistCustomCategories(merged)
    }

    suspend fun getAccountsList(): List<AccountEntity> = withContext(Dispatchers.IO) {
        walletDao.getAccountsList()
    }

    suspend fun getTransactionsList(): List<TransactionEntity> = withContext(Dispatchers.IO) {
        walletDao.getTransactionsList()
    }

    suspend fun getContactsList(): List<ContactEntity> = withContext(Dispatchers.IO) {
        walletDao.getContactsList()
    }

    suspend fun getRecurringTransactionsList(): List<RecurringTransactionEntity> = withContext(Dispatchers.IO) {
        walletDao.getRecurringTransactionsList()
    }

    suspend fun getDeletedAccountsList(): List<AccountEntity> = withContext(Dispatchers.IO) {
        walletDao.getDeletedAccountsList()
    }

    suspend fun hardDeleteAccount(id: String) = withContext(Dispatchers.IO) {
        walletDao.hardDeleteAccount(id)
    }

    /**
     * Migrates legacy TWT names, normalizes currencies, deduplicates default seed accounts with 0.00 balance
     * sharing the same name and scope, and ensures exactly 1 canonical record per default seed account (6 total).
     */
    suspend fun normalizeAccountsOnStartup() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val existing = walletDao.getAccountsList()

        // 1. Normalize names & currency codes (e.g. Compte TWT 2026 FCFA -> TWT 2026 USD, strip FCFA -> CFA)
        for (acc in existing) {
            val normalized = TwtAccountHelper.normalizeTwtAccount(acc)
            if (normalized != acc) {
                walletDao.updateAccount(normalized.copy(updatedAt = now))
            }
        }

        // 2. Migrate/deduplicate default seed accounts & delete 0.00 duplicates sharing name & scope
        val seeds = SeedAccountIds.defaultSeedAccounts(now)
        val afterNorm = walletDao.getAccountsList()

        // Group by (normalized name, TWT scope)
        val grouped = afterNorm.groupBy { acc ->
            acc.name.trim().lowercase() to TwtAccountHelper.isTwtAccount(acc)
        }

        for ((_, group) in grouped) {
            if (group.size <= 1) continue

            // Prefer deterministic seed_* ID; if another has non-zero balance, prefer non-zero or preserve into seed
            val seedMatch = group.firstOrNull { it.id in SeedAccountIds.ALL_IDS }
            val nonZero = group.firstOrNull {
                Currencies.toMoneyBigDecimal(it.balance).compareTo(BigDecimal.ZERO) != 0
            }
            val keeper = when {
                seedMatch != null && (nonZero == null || nonZero.id == seedMatch.id) -> seedMatch
                nonZero != null -> nonZero
                else -> group.maxByOrNull { it.updatedAt } ?: group.first()
            }

            for (other in group) {
                if (other.id == keeper.id) continue
                val otherBal = Currencies.toMoneyBigDecimal(other.balance)
                if (otherBal.compareTo(BigDecimal.ZERO) == 0) {
                    walletDao.reassignTransactionsAccount(other.id, keeper.id, now)
                    walletDao.reassignRecurringAccount(other.id, keeper.id, now)
                    walletDao.markAccountDeleted(other.id, now)
                }
            }
        }

        // 3. One-time migration: align legacy random-UUID default accounts to deterministic seed_* IDs
        val cleanupKey = "duplicate_seed_cleanup_v1"
        val currentAccounts = walletDao.getAccountsList()
        for (seed in seeds) {
            val seedScope = TwtAccountHelper.isTwtAccount(seed)
            val matching = currentAccounts.filter { acc ->
                acc.id == seed.id ||
                    (acc.name.trim().equals(seed.name.trim(), ignoreCase = true) &&
                        TwtAccountHelper.isTwtAccount(acc) == seedScope)
            }
            if (matching.isEmpty()) {
                walletDao.insertSeedAccount(seed)
            } else if (matching.none { it.id == seed.id } && prefs?.getBoolean(cleanupKey, false) != true) {
                val legacy = matching.firstOrNull {
                    Currencies.toMoneyBigDecimal(it.balance).compareTo(BigDecimal.ZERO) != 0
                } ?: matching.first()
                val migratedSeed = seed.copy(
                    balance = Currencies.toMoneyBigDecimal(legacy.balance).toDouble(),
                    updatedAt = maxOf(legacy.updatedAt, now)
                )
                walletDao.insertSeedAccount(migratedSeed)
                for (oldAcc in matching) {
                    walletDao.reassignTransactionsAccount(oldAcc.id, migratedSeed.id, now)
                    walletDao.reassignRecurringAccount(oldAcc.id, migratedSeed.id, now)
                    walletDao.markAccountDeleted(oldAcc.id, now)
                }
            }
        }

        prefs?.edit()?.putBoolean(cleanupKey, true)?.apply()
    }

    suspend fun transferBetweenAccounts(fromAccountId: String, toAccountId: String, amount: Double, notes: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val fromAcc = walletDao.getAccountById(fromAccountId) ?: return@withContext
        val toAcc = walletDao.getAccountById(toAccountId) ?: return@withContext
        val amountBd = Currencies.toMoneyBigDecimal(amount)
        if (amountBd.compareTo(BigDecimal.ZERO) <= 0) return@withContext

        val newFromBal = Currencies.toMoneyBigDecimal(fromAcc.balance).subtract(amountBd).setScale(2, RoundingMode.HALF_UP)
        val newToBal = Currencies.toMoneyBigDecimal(toAcc.balance).add(amountBd).setScale(2, RoundingMode.HALF_UP)

        walletDao.updateAccount(fromAcc.copy(balance = newFromBal.toDouble(), updatedAt = now))
        walletDao.updateAccount(toAcc.copy(balance = newToBal.toDouble(), updatedAt = now))

        val txOut = TransactionEntity(
            accountId = fromAccountId,
            type = TransactionType.EXPENSE,
            amount = amountBd.toDouble(),
            payee = "Transfert vers ${toAcc.name}",
            category = "Other",
            notes = notes,
            timestamp = now,
            updatedAt = now
        )
        val txIn = TransactionEntity(
            accountId = toAccountId,
            type = TransactionType.INCOME,
            amount = amountBd.toDouble(),
            payee = "Transfert depuis ${fromAcc.name}",
            category = "Other",
            notes = notes,
            timestamp = now,
            updatedAt = now
        )
        walletDao.insertTransaction(txOut)
        walletDao.insertTransaction(txIn)
    }

    suspend fun sendMoneyGramOrTapTapSend(
        accountId: String,
        amount: Double,
        recipientName: String,
        destinationCountry: String,
        channel: String,
        notes: String
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val acc = walletDao.getAccountById(accountId) ?: return@withContext
        val amountBd = Currencies.toMoneyBigDecimal(amount)
        if (amountBd.compareTo(BigDecimal.ZERO) <= 0) return@withContext

        val newBal = Currencies.toMoneyBigDecimal(acc.balance).subtract(amountBd).setScale(2, RoundingMode.HALF_UP)
        walletDao.updateAccount(acc.copy(balance = newBal.toDouble(), updatedAt = now))

        val tx = TransactionEntity(
            accountId = accountId,
            type = TransactionType.EXPENSE,
            amount = amountBd.toDouble(),
            payee = recipientName,
            category = destinationCountry,
            notes = "$channel -> $destinationCountry: $notes",
            timestamp = now,
            updatedAt = now
        )
        walletDao.insertTransaction(tx)

        walletDao.insertContact(
            ContactEntity(
                name = recipientName,
                channel = channel,
                details = destinationCountry,
                updatedAt = now
            )
        )
    }

    suspend fun executeRemittanceFxTransaction(
        usdAccountId: String,
        cfaAccountId: String,
        amountSentUsd: BigDecimal,
        feeUsd: BigDecimal,
        exchangeRateUsdToCfa: BigDecimal,
        payee: String,
        category: String,
        timestamp: Long,
        notes: String
    ): RemittanceFxResult? = withContext(Dispatchers.IO) {
        if (amountSentUsd.compareTo(BigDecimal.ZERO) <= 0 ||
            feeUsd.compareTo(BigDecimal.ZERO) < 0 ||
            exchangeRateUsdToCfa.compareTo(BigDecimal.ZERO) <= 0
        ) {
            return@withContext null
        }
        val usdAcc = walletDao.getAccountById(usdAccountId) ?: return@withContext null
        val cfaAcc = walletDao.getAccountById(cfaAccountId) ?: return@withContext null

        val now = System.currentTimeMillis()
        val totalUsdDeducted = Currencies.computeTotalUsdDeduction(amountSentUsd, feeUsd)
        val netCfaReceived = Currencies.computeNetCfaFromFx(amountSentUsd, exchangeRateUsdToCfa)

        val fxNoteDetail = buildString {
            append("FX Remittance: ")
            append(Currencies.format(amountSentUsd, "USD"))
            if (feeUsd.compareTo(BigDecimal.ZERO) > 0) {
                append(" + Fee ")
                append(Currencies.format(feeUsd, "USD"))
            }
            append(" @ 1 USD = ")
            append(exchangeRateUsdToCfa.stripTrailingZeros().toPlainString())
            append(" CFA -> ")
            append(Currencies.format(netCfaReceived, "CFA"))
            if (notes.isNotBlank()) {
                append(" | ")
                append(notes.trim())
            }
        }

        val usdTx = TransactionEntity(
            accountId = usdAccountId,
            type = TransactionType.EXPENSE,
            amount = totalUsdDeducted.toDouble(),
            payee = payee.trim(),
            category = category,
            notes = fxNoteDetail,
            timestamp = timestamp,
            updatedAt = now
        )

        val cfaTx = TransactionEntity(
            accountId = cfaAccountId,
            type = TransactionType.INCOME,
            amount = netCfaReceived.toDouble(),
            payee = payee.trim(),
            category = category,
            notes = fxNoteDetail,
            timestamp = timestamp,
            updatedAt = now
        )

        val newUsdBalance = Currencies.toMoneyBigDecimal(usdAcc.balance)
            .subtract(totalUsdDeducted)
            .setScale(2, RoundingMode.HALF_UP)
        val newCfaBalance = Currencies.toMoneyBigDecimal(cfaAcc.balance)
            .add(netCfaReceived)
            .setScale(2, RoundingMode.HALF_UP)

        walletDao.insertTransaction(usdTx)
        walletDao.insertTransaction(cfaTx)
        walletDao.updateAccount(usdAcc.copy(balance = newUsdBalance.toDouble(), updatedAt = now))
        walletDao.updateAccount(cfaAcc.copy(balance = newCfaBalance.toDouble(), updatedAt = now))

        RemittanceFxResult(
            usdDeductionTx = usdTx,
            cfaCreditTx = cfaTx,
            totalUsdDeducted = totalUsdDeducted,
            netCfaReceived = netCfaReceived
        )
    }

    suspend fun addAccount(account: AccountEntity) = withContext(Dispatchers.IO) {
        val normalized = TwtAccountHelper.normalizeTwtAccount(account)
        val roundedBal = Currencies.toMoneyBigDecimal(normalized.balance).toDouble()
        walletDao.insertAccount(normalized.copy(balance = roundedBal, updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateAccount(account: AccountEntity) = withContext(Dispatchers.IO) {
        val normalized = TwtAccountHelper.normalizeTwtAccount(account)
        val roundedBal = Currencies.toMoneyBigDecimal(normalized.balance).toDouble()
        walletDao.updateAccount(normalized.copy(balance = roundedBal, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteAccount(accountId: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        walletDao.markAccountDeleted(accountId, now)
        walletDao.markTransactionsDeletedForAccount(accountId, now)
    }

    suspend fun addTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val amountBd = Currencies.toMoneyBigDecimal(transaction.amount)
        val txWithTime = transaction.copy(amount = amountBd.toDouble(), updatedAt = now)
        walletDao.insertTransaction(txWithTime)

        walletDao.getAccountById(transaction.accountId)?.let { acc ->
            val currentBal = Currencies.toMoneyBigDecimal(acc.balance)
            val newBal = if (transaction.type == TransactionType.INCOME) {
                currentBal.add(amountBd)
            } else {
                currentBal.subtract(amountBd)
            }.setScale(2, RoundingMode.HALF_UP)
            walletDao.updateAccount(
                acc.copy(
                    balance = newBal.toDouble(),
                    updatedAt = now
                )
            )
        }
    }

    suspend fun updateTransaction(oldTx: TransactionEntity, newTx: TransactionEntity) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val oldAmountBd = Currencies.toMoneyBigDecimal(oldTx.amount)
        val newAmountBd = Currencies.toMoneyBigDecimal(newTx.amount)
        walletDao.updateTransaction(newTx.copy(amount = newAmountBd.toDouble(), updatedAt = now))

        val oldDelta = if (oldTx.type == TransactionType.INCOME) oldAmountBd else oldAmountBd.negate()
        val newDelta = if (newTx.type == TransactionType.INCOME) newAmountBd else newAmountBd.negate()

        if (oldTx.accountId == newTx.accountId) {
            walletDao.getAccountById(newTx.accountId)?.let { acc ->
                val updatedBal = Currencies.toMoneyBigDecimal(acc.balance)
                    .subtract(oldDelta)
                    .add(newDelta)
                    .setScale(2, RoundingMode.HALF_UP)
                walletDao.updateAccount(
                    acc.copy(
                        balance = updatedBal.toDouble(),
                        updatedAt = now
                    )
                )
            }
        } else {
            walletDao.getAccountById(oldTx.accountId)?.let { oldAcc ->
                val updatedOldBal = Currencies.toMoneyBigDecimal(oldAcc.balance)
                    .subtract(oldDelta)
                    .setScale(2, RoundingMode.HALF_UP)
                walletDao.updateAccount(
                    oldAcc.copy(
                        balance = updatedOldBal.toDouble(),
                        updatedAt = now
                    )
                )
            }
            walletDao.getAccountById(newTx.accountId)?.let { newAcc ->
                val updatedNewBal = Currencies.toMoneyBigDecimal(newAcc.balance)
                    .add(newDelta)
                    .setScale(2, RoundingMode.HALF_UP)
                walletDao.updateAccount(
                    newAcc.copy(
                        balance = updatedNewBal.toDouble(),
                        updatedAt = now
                    )
                )
            }
        }
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        walletDao.markTransactionDeleted(transaction.id, now)

        val amountBd = Currencies.toMoneyBigDecimal(transaction.amount)
        walletDao.getAccountById(transaction.accountId)?.let { acc ->
            val currentBal = Currencies.toMoneyBigDecimal(acc.balance)
            val updatedBal = if (transaction.type == TransactionType.INCOME) {
                currentBal.subtract(amountBd)
            } else {
                currentBal.add(amountBd)
            }.setScale(2, RoundingMode.HALF_UP)
            walletDao.updateAccount(
                acc.copy(
                    balance = updatedBal.toDouble(),
                    updatedAt = now
                )
            )
        }
    }

    suspend fun clearAllTransactions() = withContext(Dispatchers.IO) {
        walletDao.clearTransactions()
        walletDao.clearRecurringTransactions()
    }

    suspend fun getDeletedTransactionsList(): List<TransactionEntity> = withContext(Dispatchers.IO) {
        walletDao.getDeletedTransactionsList()
    }

    suspend fun hardDeleteTransaction(id: String) = withContext(Dispatchers.IO) {
        walletDao.hardDeleteTransaction(id)
    }

    // Recurring Transaction Operations
    suspend fun addRecurringTransaction(recurring: RecurringTransactionEntity) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val amountBd = Currencies.toMoneyBigDecimal(recurring.amount)
        walletDao.insertRecurringTransaction(recurring.copy(amount = amountBd.toDouble(), updatedAt = now))
    }

    suspend fun updateRecurringTransaction(recurring: RecurringTransactionEntity) = withContext(Dispatchers.IO) {
        val amountBd = Currencies.toMoneyBigDecimal(recurring.amount)
        walletDao.updateRecurringTransaction(recurring.copy(amount = amountBd.toDouble(), updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteRecurringTransaction(id: String) = withContext(Dispatchers.IO) {
        walletDao.markRecurringTransactionDeleted(id, System.currentTimeMillis())
    }

    suspend fun toggleRecurringTransactionActive(id: String, isActive: Boolean) = withContext(Dispatchers.IO) {
        val list = walletDao.getRecurringTransactionsList()
        val item = list.firstOrNull { it.id == id } ?: return@withContext
        walletDao.updateRecurringTransaction(item.copy(isActive = isActive, updatedAt = System.currentTimeMillis()))
    }

    suspend fun executeRecurringTransactionNow(recurringId: String): TransactionEntity? = withContext(Dispatchers.IO) {
        val list = walletDao.getRecurringTransactionsList()
        val item = list.firstOrNull { it.id == recurringId } ?: return@withContext null
        val now = System.currentTimeMillis()

        val generatedTx = TransactionEntity(
            accountId = item.accountId,
            type = item.type,
            amount = item.amount,
            payee = item.payee,
            category = item.category,
            notes = if (item.notes.isNotBlank()) "[${item.frequency.displayName()}] ${item.notes}" else "[${item.frequency.displayName()}] Échéance programmée",
            timestamp = now,
            updatedAt = now
        )
        addTransaction(generatedTx)

        val nextDue = item.frequency.computeNextDueDate(now)
        walletDao.updateRecurringTransaction(
            item.copy(
                lastExecutedDate = now,
                nextDueDate = nextDue,
                updatedAt = now
            )
        )
        generatedTx
    }

    suspend fun processDueRecurringTransactions(): Int = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val activeRecurring = walletDao.getActiveRecurringTransactions()
        var generatedCount = 0

        for (item in activeRecurring) {
            var currentDue = item.nextDueDate
            var loopCount = 0
            while (currentDue <= now && loopCount < 12) {
                val generatedTx = TransactionEntity(
                    accountId = item.accountId,
                    type = item.type,
                    amount = item.amount,
                    payee = item.payee,
                    category = item.category,
                    notes = if (item.notes.isNotBlank()) "[${item.frequency.displayName()}] ${item.notes}" else "[${item.frequency.displayName()}] Échéance automatique",
                    timestamp = currentDue,
                    updatedAt = now
                )
                addTransaction(generatedTx)
                generatedCount++
                loopCount++
                currentDue = item.frequency.computeNextDueDate(currentDue)
            }

            if (loopCount > 0) {
                walletDao.updateRecurringTransaction(
                    item.copy(
                        lastExecutedDate = now,
                        nextDueDate = currentDue,
                        updatedAt = now
                    )
                )
            }
        }
        generatedCount
    }

    suspend fun markTransactionSynced(id: String) = withContext(Dispatchers.IO) {
        walletDao.markTransactionSynced(id)
    }

    suspend fun mergeRemoteSharedAccounts(remoteAccounts: List<AccountEntity>) = withContext(Dispatchers.IO) {
        val localAccounts = walletDao.getAccountsList()
        val twtIds = localAccounts.filter { TwtAccountHelper.isTwtAccount(it) }.map { it.id }.toSet()
        val localSharedAccounts = localAccounts.filter { it.id !in twtIds }
        val remoteAccIds = remoteAccounts.map { it.id }.toSet()

        for (localAcc in localSharedAccounts) {
            if (localAcc.id !in remoteAccIds) {
                walletDao.hardDeleteAccount(localAcc.id)
            }
        }
        if (remoteAccounts.isNotEmpty()) {
            walletDao.upsertAccounts(remoteAccounts)
        }
    }

    suspend fun mergeRemoteSharedTransactions(remoteTransactions: List<TransactionEntity>) = withContext(Dispatchers.IO) {
        val localAccounts = walletDao.getAccountsList()
        val twtIds = localAccounts.filter { TwtAccountHelper.isTwtAccount(it) }.map { it.id }.toSet()
        val localTxs = walletDao.getTransactionsList().filter { it.accountId !in twtIds }
        val remoteTxIds = remoteTransactions.map { it.id }.toSet()

        for (localTx in localTxs) {
            val isDeletedFlag = localTx.isDeleted || localTx.notes.contains("[DELETED]", ignoreCase = true)
            if (localTx.isSyncedToCloud && (localTx.id !in remoteTxIds || isDeletedFlag)) {
                walletDao.hardDeleteTransaction(localTx.id)
            }
        }

        val txsToUpsert = mutableListOf<TransactionEntity>()
        for (remote in remoteTransactions) {
            val local = walletDao.getTransactionById(remote.id)
            if (local == null || remote.updatedAt > local.updatedAt) {
                txsToUpsert.add(remote.copy(isSyncedToCloud = true))
            }
        }
        if (txsToUpsert.isNotEmpty()) {
            walletDao.upsertTransactions(txsToUpsert)
        }
    }

    suspend fun mergeRemoteTwtAccounts(remoteAccounts: List<AccountEntity>) = withContext(Dispatchers.IO) {
        val localAccounts = walletDao.getAccountsList()
        val twtIds = localAccounts.filter { TwtAccountHelper.isTwtAccount(it) }.map { it.id }.toSet()
        val localTwtAccounts = localAccounts.filter { TwtAccountHelper.isTwtAccount(it) }
        val remoteAccIds = remoteAccounts.map { it.id }.toSet()

        for (localAcc in localTwtAccounts) {
            if (localAcc.id !in remoteAccIds) {
                walletDao.hardDeleteAccount(localAcc.id)
            }
        }
        if (remoteAccounts.isNotEmpty()) {
            walletDao.upsertAccounts(remoteAccounts.map { TwtAccountHelper.normalizeTwtAccount(it) })
        }
    }

    suspend fun mergeRemoteTwtTransactions(remoteTransactions: List<TransactionEntity>) = withContext(Dispatchers.IO) {
        val localAccounts = walletDao.getAccountsList()
        val twtIds = localAccounts.filter { TwtAccountHelper.isTwtAccount(it) }.map { it.id }.toSet()
        val localTxs = walletDao.getTransactionsList().filter { it.accountId in twtIds }
        val remoteTxIds = remoteTransactions.map { it.id }.toSet()

        for (localTx in localTxs) {
            val isDeletedFlag = localTx.isDeleted || localTx.notes.contains("[DELETED]", ignoreCase = true)
            if (localTx.isSyncedToCloud && (localTx.id !in remoteTxIds || isDeletedFlag)) {
                walletDao.hardDeleteTransaction(localTx.id)
            }
        }

        val txsToUpsert = mutableListOf<TransactionEntity>()
        for (remote in remoteTransactions) {
            val local = walletDao.getTransactionById(remote.id)
            if (local == null || remote.updatedAt > local.updatedAt) {
                txsToUpsert.add(remote.copy(isSyncedToCloud = true))
            }
        }
        if (txsToUpsert.isNotEmpty()) {
            walletDao.upsertTransactions(txsToUpsert)
        }
    }

    suspend fun mergeRemoteData(
        remoteAccounts: List<AccountEntity>,
        remoteTransactions: List<TransactionEntity>
    ): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val sharedAccs = remoteAccounts.filter { !TwtAccountHelper.isTwtAccount(it) }
        val twtAccs = remoteAccounts.filter { TwtAccountHelper.isTwtAccount(it) }
        val localAccounts = walletDao.getAccountsList()
        val sharedTxs = remoteTransactions.filter { tx ->
            val acc = localAccounts.firstOrNull { it.id == tx.accountId }
            !TwtAccountHelper.isTwtAccount(acc)
        }
        val twtTxs = remoteTransactions.filter { tx ->
            val acc = localAccounts.firstOrNull { it.id == tx.accountId }
            TwtAccountHelper.isTwtAccount(acc)
        }

        mergeRemoteSharedAccounts(sharedAccs)
        mergeRemoteSharedTransactions(sharedTxs)
        if (twtAccs.isNotEmpty() || twtTxs.isNotEmpty()) {
            mergeRemoteTwtAccounts(twtAccs)
            mergeRemoteTwtTransactions(twtTxs)
        }
        Pair(remoteAccounts.size, remoteTransactions.size)
    }

    suspend fun exportToCsv(includeTwt: Boolean = true): String = withContext(Dispatchers.IO) {
        val allAccountsRaw = walletDao.getAccountsList()
        val allAccounts = allAccountsRaw.filter { includeTwt || !TwtAccountHelper.isTwtAccount(it) }
        val accounts = allAccounts.associateBy { it.id }
        val twtIds = allAccountsRaw.filter { TwtAccountHelper.isTwtAccount(it) }.map { it.id }.toSet()
        val transactions = walletDao.getTransactionsList().filter { includeTwt || it.accountId !in twtIds }
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

        val sb = StringBuilder()
        sb.append("Date,Account,Payee,Category,Type,Amount,Currency,Notes\n")

        for (tx in transactions) {
            val acc = accounts[tx.accountId]
            val accName = (acc?.name ?: "Unknown").replace("\"", "\"\"")
            val payee = tx.payee.replace("\"", "\"\"")
            val cat = tx.category.replace("\"", "\"\"")
            val notes = tx.notes.replace("\"", "\"\"")
            val dateStr = sdf.format(Date(tx.timestamp))
            val cur = Currencies.effectiveCurrencyCode(acc, "CFA")
            val amt = Currencies.toMoneyBigDecimal(tx.amount).toPlainString()

            sb.append("\"$dateStr\",\"$accName\",\"$payee\",\"$cat\",\"${tx.type.name}\",$amt,\"$cur\",\"$notes\"\n")
        }
        sb.toString()
    }

    suspend fun exportToJson(includeTwt: Boolean = true): String = withContext(Dispatchers.IO) {
        val allAccounts = walletDao.getAccountsList()
        val twtIds = allAccounts.filter { TwtAccountHelper.isTwtAccount(it) }.map { it.id }.toSet()
        val accounts = allAccounts.filter { includeTwt || it.id !in twtIds }
        val transactions = walletDao.getTransactionsList().filter { includeTwt || it.accountId !in twtIds }
        val contacts = walletDao.getContactsList()
        val customCats = _customCategoriesFlow.value

        val root = JSONObject()
        root.put("app", "Balatec Secure Wallet")
        root.put("version", 2)
        root.put("exportedAt", System.currentTimeMillis())

        val accArray = JSONArray()
        for (acc in accounts) {
            val normalized = TwtAccountHelper.normalizeTwtAccount(acc)
            val obj = JSONObject().apply {
                put("id", normalized.id)
                put("name", normalized.name)
                put("type", normalized.type.name)
                put("balance", Currencies.toMoneyBigDecimal(normalized.balance).toDouble())
                put("currencyCode", Currencies.effectiveCurrencyCode(normalized, "CFA"))
                put("emojiAvatar", normalized.emojiAvatar)
                put("sortOrder", normalized.sortOrder)
                put("updatedAt", normalized.updatedAt)
                put("isDeleted", normalized.isDeleted)
            }
            accArray.put(obj)
        }
        root.put("accounts", accArray)

        val txArray = JSONArray()
        for (tx in transactions) {
            val obj = JSONObject().apply {
                put("id", tx.id)
                put("accountId", tx.accountId)
                put("type", tx.type.name)
                put("amount", Currencies.toMoneyBigDecimal(tx.amount).toDouble())
                put("payee", tx.payee)
                put("category", tx.category)
                put("timestamp", tx.timestamp)
                put("notes", tx.notes)
                put("updatedAt", tx.updatedAt)
                put("isDeleted", tx.isDeleted)
            }
            txArray.put(obj)
        }
        root.put("transactions", txArray)

        val catArray = JSONArray()
        for ((name, icon) in customCats) {
            catArray.put(JSONObject().apply {
                put("name", name)
                put("icon", icon)
            })
        }
        root.put("categories", catArray)

        val contactsArray = JSONArray()
        for (c in contacts) {
            contactsArray.put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("channel", c.channel)
                put("details", c.details)
                put("updatedAt", c.updatedAt)
            })
        }
        root.put("contacts", contactsArray)

        root.toString(2)
    }

    suspend fun importFromJson(jsonString: String, includeTwt: Boolean = true): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val root = JSONObject(jsonString)
        val accArray = root.optJSONArray("accounts") ?: JSONArray()
        val txArray = root.optJSONArray("transactions") ?: JSONArray()
        val catArray = root.optJSONArray("categories") ?: JSONArray()
        val contactsArray = root.optJSONArray("contacts") ?: JSONArray()

        val excludedTwtAccountIds = mutableSetOf<String>()
        val parsedAccounts = mutableListOf<AccountEntity>()
        for (i in 0 until accArray.length()) {
            val obj = accArray.getJSONObject(i)
            val accId = obj.getString("id")
            val rawName = obj.getString("name")
            if (!includeTwt && TwtAccountHelper.isTwtAccountName(rawName)) {
                excludedTwtAccountIds.add(accId)
                continue
            }
            val parsed = AccountEntity(
                id = accId,
                name = rawName,
                type = runCatching { AccountType.valueOf(obj.optString("type", "CUSTOM")) }.getOrDefault(AccountType.CUSTOM),
                balance = Currencies.toMoneyBigDecimal(obj.getDouble("balance")).toDouble(),
                currencyCode = obj.optString("currencyCode", "CFA"),
                emojiAvatar = obj.optString("emojiAvatar", "💵"),
                sortOrder = obj.optInt("sortOrder", 0),
                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                isDeleted = obj.optBoolean("isDeleted", false)
            )
            parsedAccounts.add(TwtAccountHelper.normalizeTwtAccount(parsed))
        }

        val parsedTransactions = mutableListOf<TransactionEntity>()
        for (i in 0 until txArray.length()) {
            val obj = txArray.getJSONObject(i)
            val accId = obj.getString("accountId")
            if (!includeTwt && accId in excludedTwtAccountIds) continue
            parsedTransactions.add(
                TransactionEntity(
                    id = obj.getString("id"),
                    accountId = accId,
                    type = runCatching { TransactionType.valueOf(obj.optString("type", "EXPENSE")) }.getOrDefault(TransactionType.EXPENSE),
                    amount = Currencies.toMoneyBigDecimal(obj.getDouble("amount")).toDouble(),
                    payee = obj.getString("payee"),
                    category = obj.getString("category"),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    notes = obj.optString("notes", ""),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                    isDeleted = obj.optBoolean("isDeleted", false)
                )
            )
        }

        val importedCats = mutableListOf<Pair<String, String>>()
        for (i in 0 until catArray.length()) {
            val obj = catArray.getJSONObject(i)
            val cName = obj.optString("name")
            val cIcon = obj.optString("icon", "🏷️")
            if (cName.isNotBlank()) importedCats.add(cName to cIcon)
        }
        mergeRemoteCategories(importedCats)

        for (i in 0 until contactsArray.length()) {
            val obj = contactsArray.getJSONObject(i)
            val cName = obj.optString("name")
            if (cName.isNotBlank()) {
                walletDao.insertContact(
                    ContactEntity(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        name = cName,
                        channel = obj.optString("channel", ""),
                        details = obj.optString("details", ""),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
        }

        mergeRemoteData(parsedAccounts, parsedTransactions)
    }
}
