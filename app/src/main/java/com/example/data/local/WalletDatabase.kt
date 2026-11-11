package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.ContactEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        ContactEntity::class,
        com.example.data.model.RecurringTransactionEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class WalletDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao

    companion object {
        @Volatile
        private var INSTANCE: WalletDatabase? = null

        fun getDatabase(context: Context): WalletDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WalletDatabase::class.java,
                    "balatec_secure_wallet.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate default starter data
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let { database ->
                                    populateInitialData(database.walletDao())
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun populateInitialData(dao: WalletDao) {
            val now = System.currentTimeMillis()
            val cashAccId = UUID.randomUUID().toString()
            val cardAccId = UUID.randomUUID().toString()
            val savingsAccId = UUID.randomUUID().toString()
            val twtAccId = UUID.randomUUID().toString()

            val cashAccount = AccountEntity(
                id = cashAccId,
                name = "Physical Cash Wallet",
                type = AccountType.CASH,
                balance = 480.00,
                currencyCode = "USD",
                emojiAvatar = "💵",
                sortOrder = 0,
                updatedAt = now
            )

            val cardAccount = AccountEntity(
                id = cardAccId,
                name = "Artisan Black Card",
                type = AccountType.CARD,
                balance = 3250.75,
                currencyCode = "USD",
                emojiAvatar = "💳",
                sortOrder = 1,
                updatedAt = now
            )

            val savingsAccount = AccountEntity(
                id = savingsAccId,
                name = "Vault Reserve",
                type = AccountType.SAVINGS,
                balance = 14500.00,
                currencyCode = "USD",
                emojiAvatar = "🏦",
                sortOrder = 2,
                updatedAt = now
            )

            val twtAccount = AccountEntity(
                id = twtAccId,
                name = "TWT Bonuses Account",
                type = AccountType.SAVINGS,
                balance = 2450.00,
                currencyCode = "CFA",
                emojiAvatar = "🪙",
                sortOrder = 3,
                updatedAt = now
            )

            dao.insertAccount(cashAccount)
            dao.insertAccount(cardAccount)
            dao.insertAccount(savingsAccount)
            dao.insertAccount(twtAccount)

            // Initial transactions including Bamako, Abidjan, MoneyGram, Tap Tap Send, and TWT Bonuses
            val tx1 = TransactionEntity(
                id = UUID.randomUUID().toString(),
                accountId = cardAccId,
                type = TransactionType.EXPENSE,
                amount = 74.50,
                payee = "Artisan Espresso & Roastery",
                category = "Food & Dining",
                timestamp = now - 3600000 * 4,
                notes = "Handcrafted pour-over & pastry",
                updatedAt = now
            )

            val tx2 = TransactionEntity(
                id = UUID.randomUUID().toString(),
                accountId = cardAccId,
                type = TransactionType.EXPENSE,
                amount = 210.00,
                payee = "Bespoke Leather Goods",
                category = "Shopping",
                timestamp = now - 86400000 * 2,
                notes = "Full-grain saddle stitch briefcase strap",
                updatedAt = now
            )

            val tx3 = TransactionEntity(
                id = UUID.randomUUID().toString(),
                accountId = savingsAccId,
                type = TransactionType.INCOME,
                amount = 4500.00,
                payee = "Balatec Engineering Tech",
                category = "Salary & Wages",
                timestamp = now - 86400000 * 5,
                notes = "Monthly engineering consulting dividend",
                updatedAt = now
            )

            val tx4 = TransactionEntity(
                id = UUID.randomUUID().toString(),
                accountId = cashAccId,
                type = TransactionType.EXPENSE,
                amount = 150.00,
                payee = "Amadou Diallo (Bamako Transfer)",
                category = "Bamako",
                timestamp = now - 86400000 * 3,
                notes = "MoneyGram remittance to Bamako",
                updatedAt = now
            )

            val tx5 = TransactionEntity(
                id = UUID.randomUUID().toString(),
                accountId = cardAccId,
                type = TransactionType.EXPENSE,
                amount = 200.00,
                payee = "Koffi Kouassi (Abidjan Transfer)",
                category = "Abidjan",
                timestamp = now - 86400000 * 4,
                notes = "Tap Tap Send remittance to Abidjan",
                updatedAt = now
            )

            val tx6 = TransactionEntity(
                id = UUID.randomUUID().toString(),
                accountId = twtAccId,
                type = TransactionType.INCOME,
                amount = 750.00,
                payee = "TWT Ecosystem Rewards",
                category = "Bonuses (TWT)",
                timestamp = now - 86400000 * 1,
                notes = "Trust Wallet Token staking & community bonuses",
                updatedAt = now
            )

            dao.insertTransaction(tx1)
            dao.insertTransaction(tx2)
            dao.insertTransaction(tx3)
            dao.insertTransaction(tx4)
            dao.insertTransaction(tx5)
            dao.insertTransaction(tx6)

            // Initial contacts
            dao.insertContact(ContactEntity(name = "Amadou Diallo", channel = "MoneyGram", details = "Bamako, Mali"))
            dao.insertContact(ContactEntity(name = "Koffi Kouassi", channel = "Tap Tap Send", details = "Abidjan, Côte d'Ivoire"))

            // Initial Recurring Transactions (Monthly Rent, Salary, TWT Staking Bonus)
            val rentRec = com.example.data.model.RecurringTransactionEntity(
                id = UUID.randomUUID().toString(),
                accountId = cardAccId,
                type = TransactionType.EXPENSE,
                amount = 1200.00,
                payee = "Immobilière Bamako (Loyer)",
                category = "Housing & Rent",
                frequency = com.example.data.model.RecurrenceFrequency.MONTHLY,
                startDate = now - (86400000L * 30),
                lastExecutedDate = now - (86400000L * 30),
                nextDueDate = now + (86400000L * 5), // due in 5 days
                isActive = true,
                notes = "Loyer mensuel appartement",
                updatedAt = now
            )

            val salaryRec = com.example.data.model.RecurringTransactionEntity(
                id = UUID.randomUUID().toString(),
                accountId = cardAccId,
                type = TransactionType.INCOME,
                amount = 3500.00,
                payee = "Entreprise Balatec Tech",
                category = "Salary & Wages",
                frequency = com.example.data.model.RecurrenceFrequency.MONTHLY,
                startDate = now - (86400000L * 30),
                lastExecutedDate = now - (86400000L * 30),
                nextDueDate = now + (86400000L * 15), // due in 15 days
                isActive = true,
                notes = "Salaire mensuel fixe",
                updatedAt = now
            )

            val twtBonusRec = com.example.data.model.RecurringTransactionEntity(
                id = UUID.randomUUID().toString(),
                accountId = twtAccId,
                type = TransactionType.INCOME,
                amount = 500.00,
                payee = "TWT Staking Rewards",
                category = "Bonuses (TWT)",
                frequency = com.example.data.model.RecurrenceFrequency.MONTHLY,
                startDate = now - (86400000L * 30),
                lastExecutedDate = now - (86400000L * 30),
                nextDueDate = now + (86400000L * 2), // due in 2 days
                isActive = true,
                notes = "Bonus mensuel TWT",
                updatedAt = now
            )

            dao.insertRecurringTransaction(rentRec)
            dao.insertRecurringTransaction(salaryRec)
            dao.insertRecurringTransaction(twtBonusRec)
        }
    }
}
