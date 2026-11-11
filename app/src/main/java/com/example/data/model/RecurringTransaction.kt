package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Calendar
import java.util.UUID

enum class RecurrenceFrequency(val labelFr: String, val labelEn: String) {
    DAILY("Quotidien", "Daily"),
    WEEKLY("Hebdomadaire", "Weekly"),
    BI_WEEKLY("Bi-hebdomadaire", "Bi-weekly"),
    MONTHLY("Mensuel (ex. Loyer, Salaire)", "Monthly (e.g. Rent, Salary)"),
    YEARLY("Annuel", "Yearly");

    fun displayName(isFrench: Boolean = true): String = if (isFrench) labelFr else labelEn

    fun computeNextDueDate(fromTimestamp: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = fromTimestamp }
        when (this) {
            DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            BI_WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 2)
            MONTHLY -> cal.add(Calendar.MONTH, 1)
            YEARLY -> cal.add(Calendar.YEAR, 1)
        }
        return cal.timeInMillis
    }
}

@Entity(
    tableName = "recurring_transactions",
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["nextDueDate"]),
        Index(value = ["isActive"])
    ]
)
data class RecurringTransactionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val accountId: String,
    val type: TransactionType,
    val amount: Double,
    val payee: String,
    val category: String,
    val frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    val startDate: Long = System.currentTimeMillis(),
    val lastExecutedDate: Long = 0L,
    val nextDueDate: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)
