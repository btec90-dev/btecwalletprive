package com.example.data.repository

import android.content.Context
import com.example.data.local.WalletDao
import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.ContactEntity
import com.example.data.model.RecurringTransactionEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.TwtAccountHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    suspend fun getAccountsList(): List<AccountEntity> = walletDao.getAccountsList()
    suspend fun getTransactionsList(): List<TransactionEntity> = walletDao.getTransactionsList()
    suspend fun getContactsList(): List<ContactEntity> = walletDao.getContactsList()
    suspend fun getRecurringTransactionsList(): List<RecurringTransactionEntity> = walletDao.getRecurringTransactionsList()

    /**
     * Migrates legacy TWT names ("Compte TWT", "Compte TWT 2026 FCFA", etc.) to dynamic "TWT [Year]"
     * with strict USD ($) currency and strips "FCFA" from any non-TWT account currency codes.
     */
    suspend fun normalizeAccountsOnStartup() {
        val existing = walletDao.getAccountsList()
        var hasTwt = false
        for (acc in existing) {
            val normalized = TwtAccountHelper.normalizeTwtAccount(acc)
            if (TwtAccountHelper.isTwtAccount(normalized)) {
                hasTwt = true
            }
            if (normalized != acc) {
                walletDao.updateAccount(normalized.copy(updatedAt = System.currentTimeMillis()))
            }
        }
        if (!hasTwt && existing.isNotEmpty()) {
            walletDao.insertAccount(
                AccountEntity(
                    name = TwtAccountHelper.currentTwtAccountName(),
                    type = AccountType.SAVINGS,
                    balance = 0.0,
                    currencyCode = "USD",
                    emojiAvatar = "🪙",
                    sortOrder = 5,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun transferBetweenAccounts(fromAccountId: String, toAccountId: String, amount: Double, notes: String) {
        val now = System.currentTimeMillis()
        val fromAcc = walletDao.getAccountById(fromAccountId) ?: return
        val toAcc = walletDao.getAccountById(toAccountId) ?: return

        walletDao.updateAccount(fromAcc.copy(balance = fromAcc.balance - amount, updatedAt = now))
        walletDao.updateAccount(toAcc.copy(balance = toAcc.balance + amount, updatedAt = now))

        val txOut = TransactionEntity(
            accountId = fromAccountId,
            type = TransactionType.EXPENSE,
            amount = amount,
            payee = "Transfert vers ${toAcc.name}",
            category = "Transfert",
            notes = notes,
            timestamp = now,
            updatedAt = now
        )
        val txIn = TransactionEntity(
            accountId = toAccountId,
            type = TransactionType.INCOME,
            amount = amount,
            payee = "Transfert depuis ${fromAcc.name}",
            category = "Transfert",
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
    ) {
        val now = System.currentTimeMillis()
        val acc = walletDao.getAccountById(accountId) ?: return

        walletDao.updateAccount(acc.copy(balance = acc.balance - amount, updatedAt = now))

        val tx = TransactionEntity(
            accountId = accountId,
            type = TransactionType.EXPENSE,
            amount = amount,
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

    suspend fun addAccount(account: AccountEntity) {
        val normalized = TwtAccountHelper.normalizeTwtAccount(account)
        walletDao.insertAccount(normalized.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateAccount(account: AccountEntity) {
        val normalized = TwtAccountHelper.normalizeTwtAccount(account)
        walletDao.updateAccount(normalized.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteAccount(accountId: String) {
        val now = System.currentTimeMillis()
        walletDao.markAccountDeleted(accountId, now)
        walletDao.markTransactionsDeletedForAccount(accountId, now)
    }

    suspend fun addTransaction(transaction: TransactionEntity) {
        val now = System.currentTimeMillis()
        val txWithTime = transaction.copy(updatedAt = now)
        walletDao.insertTransaction(txWithTime)

        walletDao.getAccountById(transaction.accountId)?.let { acc ->
            val delta = if (transaction.type == TransactionType.INCOME) transaction.amount else -transaction.amount
            walletDao.updateAccount(
                acc.copy(
                    balance = acc.balance + delta,
                    updatedAt = now
                )
            )
        }
    }

    suspend fun updateTransaction(oldTx: TransactionEntity, newTx: TransactionEntity) {
        val now = System.currentTimeMillis()
        walletDao.updateTransaction(newTx.copy(updatedAt = now))

        if (oldTx.accountId == newTx.accountId) {
            walletDao.getAccountById(newTx.accountId)?.let { acc ->
                val oldDelta = if (oldTx.type == TransactionType.INCOME) oldTx.amount else -oldTx.amount
                val newDelta = if (newTx.type == TransactionType.INCOME) newTx.amount else -newTx.amount
                walletDao.updateAccount(
                    acc.copy(
                        balance = acc.balance - oldDelta + newDelta,
                        updatedAt = now
                    )
                )
            }
        } else {
            walletDao.getAccountById(oldTx.accountId)?.let { oldAcc ->
                val oldDelta = if (oldTx.type == TransactionType.INCOME) oldTx.amount else -oldTx.amount
                walletDao.updateAccount(
                    oldAcc.copy(
                        balance = oldAcc.balance - oldDelta,
                        updatedAt = now
                    )
                )
            }
            walletDao.getAccountById(newTx.accountId)?.let { newAcc ->
                val newDelta = if (newTx.type == TransactionType.INCOME) newTx.amount else -newTx.amount
                walletDao.updateAccount(
                    newAcc.copy(
                        balance = newAcc.balance + newDelta,
                        updatedAt = now
                    )
                )
            }
        }
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        val now = System.currentTimeMillis()
        walletDao.markTransactionDeleted(transaction.id, now)

        walletDao.getAccountById(transaction.accountId)?.let { acc ->
            val delta = if (transaction.type == TransactionType.INCOME) -transaction.amount else transaction.amount
            walletDao.updateAccount(
                acc.copy(
                    balance = acc.balance + delta,
                    updatedAt = now
                )
            )
        }
    }

    suspend fun clearAllTransactions() {
        walletDao.clearTransactions()
        walletDao.clearRecurringTransactions()
    }

    suspend fun getDeletedTransactionsList(): List<TransactionEntity> {
        return walletDao.getDeletedTransactionsList()
    }

    suspend fun hardDeleteTransaction(id: String) {
        walletDao.hardDeleteTransaction(id)
    }

    // Recurring Transaction Operations
    suspend fun addRecurringTransaction(recurring: RecurringTransactionEntity) {
        val now = System.currentTimeMillis()
        walletDao.insertRecurringTransaction(recurring.copy(updatedAt = now))
    }

    suspend fun updateRecurringTransaction(recurring: RecurringTransactionEntity) {
        walletDao.updateRecurringTransaction(recurring.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteRecurringTransaction(id: String) {
        walletDao.markRecurringTransactionDeleted(id, System.currentTimeMillis())
    }

    suspend fun toggleRecurringTransactionActive(id: String, isActive: Boolean) {
        val list = walletDao.getRecurringTransactionsList()
        val item = list.firstOrNull { it.id == id } ?: return
        walletDao.updateRecurringTransaction(item.copy(isActive = isActive, updatedAt = System.currentTimeMillis()))
    }

    suspend fun executeRecurringTransactionNow(recurringId: String): TransactionEntity? {
        val list = walletDao.getRecurringTransactionsList()
        val item = list.firstOrNull { it.id == recurringId } ?: return null
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
        return generatedTx
    }

    suspend fun processDueRecurringTransactions(): Int {
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
        return generatedCount
    }

    suspend fun mergeRemoteData(
        remoteAccounts: List<AccountEntity>,
        remoteTransactions: List<TransactionEntity>
    ): Pair<Int, Int> {
        val localAccounts = walletDao.getAccountsList().associateBy { it.id }
        val accountsToUpsert = mutableListOf<AccountEntity>()

        for (remote in remoteAccounts) {
            val normalized = TwtAccountHelper.normalizeTwtAccount(remote)
            val local = localAccounts[normalized.id]
            if (local == null || normalized.updatedAt > local.updatedAt) {
                accountsToUpsert.add(normalized)
            }
        }
        if (accountsToUpsert.isNotEmpty()) {
            walletDao.upsertAccounts(accountsToUpsert)
        }

        val localTransactions = walletDao.getTransactionsList().associateBy { it.id }
        val deletedIds = walletDao.getDeletedTransactionsList().map { it.id }.toSet()
        val transactionsToUpsert = mutableListOf<TransactionEntity>()

        for (remote in remoteTransactions) {
            if (remote.id in deletedIds) continue
            val local = localTransactions[remote.id]
            if (local == null || remote.updatedAt > local.updatedAt) {
                transactionsToUpsert.add(remote)
            }
        }
        if (transactionsToUpsert.isNotEmpty()) {
            walletDao.upsertTransactions(transactionsToUpsert)
        }

        return Pair(accountsToUpsert.size, transactionsToUpsert.size)
    }

    suspend fun exportToCsv(includeTwt: Boolean = true): String {
        val allAccounts = walletDao.getAccountsList().filter { includeTwt || !TwtAccountHelper.isTwtAccount(it) }
        val accounts = allAccounts.associateBy { it.id }
        val twtIds = walletDao.getAccountsList().filter { TwtAccountHelper.isTwtAccount(it) }.map { it.id }.toSet()
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
            val cur = com.example.data.model.Currencies.effectiveCurrencyCode(acc, "CFA")

            sb.append("\"$dateStr\",\"$accName\",\"$payee\",\"$cat\",\"${tx.type.name}\",${tx.amount},\"$cur\",\"$notes\"\n")
        }
        return sb.toString()
    }

    suspend fun exportToJson(includeTwt: Boolean = true): String {
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
                put("balance", normalized.balance)
                put("currencyCode", com.example.data.model.Currencies.effectiveCurrencyCode(normalized, "CFA"))
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
                put("amount", tx.amount)
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

        return root.toString(2)
    }

    suspend fun importFromJson(jsonString: String, includeTwt: Boolean = true): Pair<Int, Int> {
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
                balance = obj.getDouble("balance"),
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
                    amount = obj.getDouble("amount"),
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

        return mergeRemoteData(parsedAccounts, parsedTransactions)
    }
}
