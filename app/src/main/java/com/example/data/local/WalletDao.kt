package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.example.data.model.AccountEntity
import com.example.data.model.ContactEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    // Accounts - ordered with CASH (0) first, CARD (1), SAVINGS (2), CUSTOM (3)
    @Query("SELECT * FROM accounts WHERE isDeleted = 0 ORDER BY type ASC, sortOrder ASC, name ASC")
    fun getAccountsFlow(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE isDeleted = 0 ORDER BY type ASC, sortOrder ASC, name ASC")
    suspend fun getAccountsList(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("UPDATE accounts SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun markAccountDeleted(id: String, timestamp: Long = System.currentTimeMillis())

    @Upsert
    suspend fun upsertAccounts(accounts: List<AccountEntity>)

    // Transactions
    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getTransactionsFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY timestamp DESC")
    suspend fun getTransactionsList(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getTransactionsForAccount(accountId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("UPDATE transactions SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun markTransactionDeleted(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE transactions SET isDeleted = 1, updatedAt = :timestamp WHERE accountId = :accountId")
    suspend fun markTransactionsDeletedForAccount(accountId: String, timestamp: Long = System.currentTimeMillis())

    @Upsert
    suspend fun upsertTransactions(transactions: List<TransactionEntity>)

    @Query("DELETE FROM accounts")
    suspend fun clearAccounts()

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    // Contacts / Saved Payees
    @Query("SELECT * FROM contacts WHERE isDeleted = 0 ORDER BY name ASC")
    fun getContactsFlow(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE isDeleted = 0 ORDER BY name ASC")
    suspend fun getContactsList(): List<ContactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Query("UPDATE contacts SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun markContactDeleted(id: String, timestamp: Long = System.currentTimeMillis())

    // Recurring Transactions / Scheduled Auto-Ledger
    @Query("SELECT * FROM recurring_transactions WHERE isDeleted = 0 ORDER BY nextDueDate ASC")
    fun getRecurringTransactionsFlow(): Flow<List<com.example.data.model.RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE isDeleted = 0 ORDER BY nextDueDate ASC")
    suspend fun getRecurringTransactionsList(): List<com.example.data.model.RecurringTransactionEntity>

    @Query("SELECT * FROM recurring_transactions WHERE isDeleted = 0 AND isActive = 1")
    suspend fun getActiveRecurringTransactions(): List<com.example.data.model.RecurringTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringTransaction(recurring: com.example.data.model.RecurringTransactionEntity)

    @Update
    suspend fun updateRecurringTransaction(recurring: com.example.data.model.RecurringTransactionEntity)

    @Query("UPDATE recurring_transactions SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun markRecurringTransactionDeleted(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM recurring_transactions")
    suspend fun clearRecurringTransactions()
}
