package com.example.data.repository

import com.example.data.local.WalletDao
import com.example.data.model.AccountEntity
import com.example.data.model.ContactEntity
import com.example.data.model.RecurringTransactionEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WalletRepository(private val walletDao: WalletDao) {

    val accountsFlow: Flow<List<AccountEntity>> = walletDao.getAccountsFlow()
    val transactionsFlow: Flow<List<TransactionEntity>> = walletDao.getTransactionsFlow()
    val contactsFlow: Flow<List<ContactEntity>> = walletDao.getContactsFlow()
    val recurringTransactionsFlow: Flow<List<RecurringTransactionEntity>> = walletDao.getRecurringTransactionsFlow()

    suspend fun getAccountsList(): List<AccountEntity> = walletDao.getAccountsList()
    suspend fun getTransactionsList(): List<TransactionEntity> = walletDao.getTransactionsList()
    suspend fun getContactsList(): List<ContactEntity> = walletDao.getContactsList()
    suspend fun getRecurringTransactionsList(): List<RecurringTransactionEntity> = walletDao.getRecurringTransactionsList()

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
            payee = "Transfer to ${toAcc.name}",
            category = "Transfer",
            notes = notes,
            timestamp = now,
            updatedAt = now
        )
        val txIn = TransactionEntity(
            accountId = toAccountId,
            type = TransactionType.INCOME,
            amount = amount,
            payee = "Transfer from ${fromAcc.name}",
            category = "Transfer",
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
        destinationCountry: String, // "Bamako" or "Abidjan"
        channel: String, // "MoneyGram" or "Tap Tap Send"
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
            category = destinationCountry, // Automatically Bamako or Abidjan
            notes = "$channel remittance to $destinationCountry: $notes",
            timestamp = now,
            updatedAt = now
        )
        walletDao.insertTransaction(tx)

        // Save payee as contact automatically
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
        walletDao.insertAccount(account.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateAccount(account: AccountEntity) {
        walletDao.updateAccount(account.copy(updatedAt = System.currentTimeMillis()))
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

        // Update corresponding account balance
        walletDao.getAccountById(transaction.accountId)?.let { acc ->
            val delta = if (transaction.type == TransactionType.INCOME) transaction.amount else -transaction.amount
            walletDao.updateAccount(acc.copy(
                balance = acc.balance + delta,
                updatedAt = now
            ))
        }
    }

    suspend fun updateTransaction(oldTx: TransactionEntity, newTx: TransactionEntity) {
        val now = System.currentTimeMillis()
        walletDao.updateTransaction(newTx.copy(updatedAt = now))

        // Adjust old account if changed, or update delta
        if (oldTx.accountId == newTx.accountId) {
            walletDao.getAccountById(newTx.accountId)?.let { acc ->
                val oldDelta = if (oldTx.type == TransactionType.INCOME) oldTx.amount else -oldTx.amount
                val newDelta = if (newTx.type == TransactionType.INCOME) newTx.amount else -newTx.amount
                walletDao.updateAccount(acc.copy(
                    balance = acc.balance - oldDelta + newDelta,
                    updatedAt = now
                ))
            }
        } else {
            // Revert old account
            walletDao.getAccountById(oldTx.accountId)?.let { oldAcc ->
                val oldDelta = if (oldTx.type == TransactionType.INCOME) oldTx.amount else -oldTx.amount
                walletDao.updateAccount(oldAcc.copy(
                    balance = oldAcc.balance - oldDelta,
                    updatedAt = now
                ))
            }
            // Apply to new account
            walletDao.getAccountById(newTx.accountId)?.let { newAcc ->
                val newDelta = if (newTx.type == TransactionType.INCOME) newTx.amount else -newTx.amount
                walletDao.updateAccount(newAcc.copy(
                    balance = newAcc.balance + newDelta,
                    updatedAt = now
                ))
            }
        }
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        val now = System.currentTimeMillis()
        walletDao.markTransactionDeleted(transaction.id, now)

        // Revert balance
        walletDao.getAccountById(transaction.accountId)?.let { acc ->
            val delta = if (transaction.type == TransactionType.INCOME) -transaction.amount else transaction.amount
            walletDao.updateAccount(acc.copy(
                balance = acc.balance + delta,
                updatedAt = now
            ))
        }
    }

    suspend fun clearAllTransactions() {
        walletDao.clearTransactions()
    }

    // Recurring Transaction Operations & Automatic Engine
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
        walletDao.updateRecurringTransaction(item.copy(
            lastExecutedDate = now,
            nextDueDate = nextDue,
            updatedAt = now
        ))
        return generatedTx
    }

    suspend fun processDueRecurringTransactions(): Int {
        val now = System.currentTimeMillis()
        val activeRecurring = walletDao.getActiveRecurringTransactions()
        var generatedCount = 0

        for (item in activeRecurring) {
            var currentDue = item.nextDueDate
            var loopCount = 0
            // Process any overdue occurrences up to 12 cycles catch-up
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

    /**
     * Deterministic Conflict Resolution (Last-Write-Wins based on UTC timestamp)
     */
    suspend fun mergeRemoteData(
        remoteAccounts: List<AccountEntity>,
        remoteTransactions: List<TransactionEntity>
    ): Pair<Int, Int> {
        val localAccounts = walletDao.getAccountsList().associateBy { it.id }
        val accountsToUpsert = mutableListOf<AccountEntity>()

        for (remote in remoteAccounts) {
            val local = localAccounts[remote.id]
            if (local == null || remote.updatedAt > local.updatedAt) {
                accountsToUpsert.add(remote)
            }
        }
        if (accountsToUpsert.isNotEmpty()) {
            walletDao.upsertAccounts(accountsToUpsert)
        }

        val localTransactions = walletDao.getTransactionsList().associateBy { it.id }
        val transactionsToUpsert = mutableListOf<TransactionEntity>()

        for (remote in remoteTransactions) {
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

    /**
     * Export data as RFC 4180 CSV
     */
    suspend fun exportToCsv(): String {
        val accounts = walletDao.getAccountsList().associateBy { it.id }
        val transactions = walletDao.getTransactionsList()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

        val sb = java.lang.StringBuilder()
        sb.append("Date,Account,Payee,Category,Type,Amount,Currency,Notes\n")

        for (tx in transactions) {
            val acc = accounts[tx.accountId]
            val accName = (acc?.name ?: "Unknown").replace("\"", "\"\"")
            val payee = tx.payee.replace("\"", "\"\"")
            val cat = tx.category.replace("\"", "\"\"")
            val notes = tx.notes.replace("\"", "\"\"")
            val dateStr = sdf.format(Date(tx.timestamp))
            val cur = acc?.currencyCode ?: "USD"

            sb.append("\"$dateStr\",\"$accName\",\"$payee\",\"$cat\",\"${tx.type.name}\",${tx.amount},\"$cur\",\"$notes\"\n")
        }
        return sb.toString()
    }

    /**
     * Export data as structured JSON backup
     */
    suspend fun exportToJson(): String {
        val accounts = walletDao.getAccountsList()
        val transactions = walletDao.getTransactionsList()

        val root = JSONObject()
        root.put("app", "Balatec Secure Wallet")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val accArray = JSONArray()
        for (acc in accounts) {
            val obj = JSONObject().apply {
                put("id", acc.id)
                put("name", acc.name)
                put("type", acc.type.name)
                put("balance", acc.balance)
                put("currencyCode", acc.currencyCode)
                put("emojiAvatar", acc.emojiAvatar)
                put("sortOrder", acc.sortOrder)
                put("updatedAt", acc.updatedAt)
                put("isDeleted", acc.isDeleted)
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

        return root.toString(2)
    }

    /**
     * Import data from JSON backup
     */
    suspend fun importFromJson(jsonString: String): Pair<Int, Int> {
        val root = JSONObject(jsonString)
        val accArray = root.optJSONArray("accounts") ?: JSONArray()
        val txArray = root.optJSONArray("transactions") ?: JSONArray()

        val parsedAccounts = mutableListOf<AccountEntity>()
        for (i in 0 until accArray.length()) {
            val obj = accArray.getJSONObject(i)
            parsedAccounts.add(
                AccountEntity(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    type = com.example.data.model.AccountType.valueOf(obj.optString("type", "CUSTOM")),
                    balance = obj.getDouble("balance"),
                    currencyCode = obj.optString("currencyCode", "USD"),
                    emojiAvatar = obj.optString("emojiAvatar", "💵"),
                    sortOrder = obj.optInt("sortOrder", 0),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                    isDeleted = obj.optBoolean("isDeleted", false)
                )
            )
        }

        val parsedTransactions = mutableListOf<TransactionEntity>()
        for (i in 0 until txArray.length()) {
            val obj = txArray.getJSONObject(i)
            parsedTransactions.add(
                TransactionEntity(
                    id = obj.getString("id"),
                    accountId = obj.getString("accountId"),
                    type = TransactionType.valueOf(obj.optString("type", "EXPENSE")),
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

        return mergeRemoteData(parsedAccounts, parsedTransactions)
    }
}
