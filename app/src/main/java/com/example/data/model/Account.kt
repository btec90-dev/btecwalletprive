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

object TwtAccountHelper {
    fun currentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)

    fun currentTwtAccountName(year: Int = currentYear()): String = "TWT $year"

    fun isTwtAccountName(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        val trimmed = name.trim()
        return trimmed.equals("TWT", ignoreCase = true) ||
            trimmed.contains("TWT", ignoreCase = true)
    }

    fun isTwtAccount(account: AccountEntity?): Boolean = isTwtAccountName(account?.name)

    fun normalizeTwtAccount(account: AccountEntity): AccountEntity {
        if (!isTwtAccountName(account.name)) {
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
