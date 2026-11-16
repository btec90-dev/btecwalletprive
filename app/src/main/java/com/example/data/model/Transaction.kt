package com.example.data.model

import androidx.room.Entity
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
        "Bonus" to "💎",
        "Famille" to "👨‍👩‍👧‍👦",
        "Bamako" to "🇲🇱",
        "Abidjan" to "🇨🇮",
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

    val DEFAULT_NAMES: Set<String> = ALL.map { it.first }.toSet()

    fun getIcon(category: String, customCategories: List<Pair<String, String>> = emptyList()): String {
        return customCategories.firstOrNull { it.first.equals(category, ignoreCase = true) }?.second
            ?: ALL.firstOrNull { it.first.equals(category, ignoreCase = true) }?.second
            ?: "💳"
    }

    fun combined(customCategories: List<Pair<String, String>>): List<Pair<String, String>> {
        val existingLower = ALL.map { it.first.lowercase() }.toSet()
        val extras = customCategories.filter { it.first.lowercase() !in existingLower }
        return ALL + extras
    }
}

object PresetFamilyContacts {
    val NAMES = listOf(
        "Drissa Traoré",
        "Adama Traoré",
        "Madou Traoré",
        "Aïssata Traoré",
        "Djenebou Traoré",
        "Sogona Traoré",
        "Mawa Traoré",
        "Moussa Traoré",
        "Sarata Traoré",
        "Fatoumata Traoré",
        "Mayimouna Traoré",
        "Chaka Traoré",
        "Alima Traoré",
        "Hafissatou Traoré",
        "Yacou Traoré",
        "Yaya Traoré",
        "Oumou Traoré",
        "Tène Traoré",
        "Boubacar Coulibaly",
        "Yaya Coulibaly",
        "Fatou ma Femme"
    )

    val CITIES = listOf(
        "Bamako" to "🇲🇱",
        "Abidjan" to "🇨🇮"
    )

    val REMITTANCE_FAMILY_CATEGORIES = setOf(
        "Famille",
        "Bamako",
        "Abidjan"
    )
}
