package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class TransactionType {
    INCOME,
    EXPENSE
}

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["timestamp"]),
        Index(value = ["category"])
    ]
)
data class TransactionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val accountId: String,
    val type: TransactionType,
    val amount: Double,
    val payee: String,
    val category: String,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

object WalletCategories {
    val ALL = listOf(
        "Famille" to "👨‍👩‍👧‍👦",
        "Transfert" to "🔄",
        "MoneyGram" to "💸",
        "Tap Tap Send" to "📲",
        "Bamako" to "🇲🇱",
        "Abidjan" to "🇨🇮",
        "TWT" to "🎁",
        "Food & Dining" to "🍔",
        "Shopping" to "🛍️",
        "Housing & Rent" to "🏠",
        "Transportation" to "🚗",
        "Salary & Wages" to "💰",
        "Investments" to "📈",
        "Entertainment" to "🎬",
        "Health & Medical" to "💊",
        "Utilities & Bills" to "⚡",
        "Education" to "📚",
        "Travel" to "✈️",
        "Personal Care" to "✨",
        "Gifts & Charity" to "🎁",
        "Other" to "📦"
    )

    fun getIcon(category: String): String {
        return ALL.firstOrNull { it.first.equals(category, ignoreCase = true) }?.second ?: "💳"
    }
}

object PresetFamilyContacts {
    val NAMES = listOf(
        "Madou Traoré",
        "Le plus âgé",
        "Aïssata Traoré",
        "Djenebou Traoré",
        "Sogona Traoré",
        "Mawa Traoré",
        "Moussa Traoré",
        "Sarata Traoré",
        "Fatoumata Traoré",
        "Mayimouna Traoré",
        "Adama Traoré",
        "Chaka Traoré",
        "Alima Traoré",
        "Hafissatou Traoré",
        "Yacou Traoré",
        "Yaya Traoré",
        "Oumou Traoré",
        "Drissa Traoré",
        "Tène Traoré",
        "Boubacar Coulibaly",
        "Yaya Coulibaly",
        "Fatou ma Femme"
    )
}
