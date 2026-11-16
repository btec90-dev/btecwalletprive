package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.ContactEntity
import com.example.data.model.RecurringTransactionEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TwtAccountHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        ContactEntity::class,
        RecurringTransactionEntity::class
    ],
    version = 4,
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

            val defaultAccounts = listOf(
                AccountEntity(
                    id = UUID.randomUUID().toString(),
                    name = "Espèces (CFA)",
                    type = AccountType.CASH,
                    balance = 0.0,
                    currencyCode = "CFA",
                    emojiAvatar = "💵",
                    sortOrder = 0,
                    updatedAt = now
                ),
                AccountEntity(
                    id = UUID.randomUUID().toString(),
                    name = "Tap Tap Send",
                    type = AccountType.CUSTOM,
                    balance = 0.0,
                    currencyCode = "CFA",
                    emojiAvatar = "📲",
                    sortOrder = 1,
                    updatedAt = now
                ),
                AccountEntity(
                    id = UUID.randomUUID().toString(),
                    name = "MoneyGram",
                    type = AccountType.CUSTOM,
                    balance = 0.0,
                    currencyCode = "CFA",
                    emojiAvatar = "💸",
                    sortOrder = 2,
                    updatedAt = now
                ),
                AccountEntity(
                    id = UUID.randomUUID().toString(),
                    name = "Transfert / Virement",
                    type = AccountType.CUSTOM,
                    balance = 0.0,
                    currencyCode = "CFA",
                    emojiAvatar = "🔄",
                    sortOrder = 3,
                    updatedAt = now
                ),
                AccountEntity(
                    id = UUID.randomUUID().toString(),
                    name = "Carte Bancaire",
                    type = AccountType.CARD,
                    balance = 0.0,
                    currencyCode = "CFA",
                    emojiAvatar = "💳",
                    sortOrder = 4,
                    updatedAt = now
                ),
                AccountEntity(
                    id = UUID.randomUUID().toString(),
                    name = TwtAccountHelper.currentTwtAccountName(),
                    type = AccountType.SAVINGS,
                    balance = 0.0,
                    currencyCode = "USD",
                    emojiAvatar = "🪙",
                    sortOrder = 5,
                    updatedAt = now
                )
            )

            defaultAccounts.forEach { dao.insertAccount(it) }
            // Zero pre-loaded transactions on initial install
        }
    }
}
