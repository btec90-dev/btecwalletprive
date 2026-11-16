package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar
import java.util.UUID

enum class AccountType(val priority: Int) {
    CASH(0),
    CARD(1),
    SAVINGS(2),
    CUSTOM(3)
}

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: AccountType,
    val balance: Double,
    val currencyCode: String = "CFA",
    val emojiAvatar: String = "💵",
    val sortOrder: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

object SeedAccountIds {
    const val ESPEC_CFA = "seed_especes_cfa"
    const val TAP_TAP_SEND = "seed_tap_tap_send"
    const val MONEYGRAM = "seed_moneygram"
    const val TRANSFERT = "seed_transfert"
    const val CARTE_BANCAIRE = "seed_carte_bancaire"
    const val TWT_2026 = "seed_twt_2026"

    val ALL_IDS = setOf(
        ESPEC_CFA,
        TAP_TAP_SEND,
        MONEYGRAM,
        TRANSFERT,
        CARTE_BANCAIRE,
        TWT_2026
    )

    fun defaultSeedAccounts(now: Long = System.currentTimeMillis()): List<AccountEntity> = listOf(
        AccountEntity(
            id = ESPEC_CFA,
            name = "Espèces (CFA)",
            type = AccountType.CASH,
            balance = 0.0,
            currencyCode = "CFA",
            emojiAvatar = "💵",
            sortOrder = 0,
            updatedAt = now
        ),
        AccountEntity(
            id = CARTE_BANCAIRE,
            name = "Carte Bancaire",
            type = AccountType.CARD,
            balance = 0.0,
            currencyCode = "CFA",
            emojiAvatar = "💳",
            sortOrder = 1,
            updatedAt = now
        ),
        AccountEntity(
            id = TAP_TAP_SEND,
            name = "Tap Tap Send",
            type = AccountType.CUSTOM,
            balance = 0.0,
            currencyCode = "CFA",
            emojiAvatar = "📲",
            sortOrder = 2,
            updatedAt = now
        ),
        AccountEntity(
            id = MONEYGRAM,
            name = "MoneyGram",
            type = AccountType.CUSTOM,
            balance = 0.0,
            currencyCode = "CFA",
            emojiAvatar = "💸",
            sortOrder = 3,
            updatedAt = now
        ),
        AccountEntity(
            id = TRANSFERT,
            name = "Transfert / Virement",
            type = AccountType.CUSTOM,
            balance = 0.0,
            currencyCode = "CFA",
            emojiAvatar = "🔄",
            sortOrder = 4,
            updatedAt = now
        ),
        AccountEntity(
            id = TWT_2026,
            name = TwtAccountHelper.currentTwtAccountName(),
            type = AccountType.SAVINGS,
            balance = 0.0,
            currencyCode = "USD",
            emojiAvatar = "🪙",
            sortOrder = 5,
            updatedAt = now
        )
    )
}

object TwtAccountHelper {
    fun currentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)

    fun currentTwtAccountName(year: Int = currentYear()): String = "TWT $year"

    fun isTwtAccountName(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        val trimmed = name.trim()
        return trimmed.equals("TWT", ignoreCase = true) ||
            trimmed.contains("TWT", ignoreCase = true)
    }

    fun isTwtAccount(account: AccountEntity?): Boolean =
        account?.id == SeedAccountIds.TWT_2026 || isTwtAccountName(account?.name)

    fun normalizeTwtAccount(account: AccountEntity): AccountEntity {
        if (!isTwtAccount(account)) {
            val cleanCurrency = if (account.currencyCode.equals("FCFA", ignoreCase = true)) "CFA" else account.currencyCode
            return if (cleanCurrency != account.currencyCode) {
                account.copy(currencyCode = cleanCurrency)
            } else {
                account
            }
        }
        val targetName = currentTwtAccountName()
        return account.copy(
            name = targetName,
            type = AccountType.SAVINGS,
            currencyCode = "USD",
            emojiAvatar = "🪙"
        )
    }
}
