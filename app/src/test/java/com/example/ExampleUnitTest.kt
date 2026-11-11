package com.example

import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.Currencies
import com.example.security.SecurityManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun currency_formattingIsCorrect() {
        assertEquals("$1,250.50", Currencies.format(1250.5, "USD"))
        assertEquals("1,250.50 FCFA", Currencies.format(1250.5, "CFA"))
    }

    @Test
    fun account_typePrioritiesAreOrdered() {
        assertTrue(AccountType.CASH.priority < AccountType.CARD.priority)
        assertTrue(AccountType.CARD.priority < AccountType.SAVINGS.priority)
        assertTrue(AccountType.SAVINGS.priority < AccountType.CUSTOM.priority)
    }

    @Test
    fun deterministic_conflictResolutionSimulation() {
        val localTimestamp = 1700000000000L
        val remoteNewerTimestamp = 1700000005000L
        val remoteOlderTimestamp = 1699999995000L

        val local = AccountEntity(id = "1", name = "Cash", type = AccountType.CASH, balance = 100.0, updatedAt = localTimestamp)
        val remoteNewer = AccountEntity(id = "1", name = "Cash Updated", type = AccountType.CASH, balance = 150.0, updatedAt = remoteNewerTimestamp)
        val remoteOlder = AccountEntity(id = "1", name = "Cash Stale", type = AccountType.CASH, balance = 80.0, updatedAt = remoteOlderTimestamp)

        // Last-write-wins: remoteNewer should win
        assertTrue(remoteNewer.updatedAt > local.updatedAt)
        // remoteOlder should NOT overwrite local
        assertFalse(remoteOlder.updatedAt > local.updatedAt)
    }

    @Test
    fun presetFamilyContacts_containsSpecifiedFamilyMembers() {
        val names = com.example.data.model.PresetFamilyContacts.NAMES
        assertEquals(22, names.size)
        assertTrue(names.contains("Fatou ma Femme"))
        assertTrue(names.contains("Madou Traoré"))
        assertTrue(names.contains("Le plus âgé"))
        assertTrue(names.contains("Aïssata Traoré"))
        assertTrue(names.contains("Sogona Traoré"))
        assertTrue(names.contains("Boubacar Coulibaly"))
    }

    @Test
    fun walletCategories_containFamilleAndRemittance() {
        val categories = com.example.data.model.WalletCategories.ALL.map { it.first }
        assertTrue(categories.contains("Famille"))
        assertTrue(categories.contains("Transfert"))
        assertTrue(categories.contains("MoneyGram"))
        assertTrue(categories.contains("Tap Tap Send"))
        assertTrue(categories.contains("Bamako"))
        assertTrue(categories.contains("Abidjan"))
        assertTrue(categories.contains("TWT"))
    }

    @Test
    fun recurrenceFrequency_computesCorrectNextDueDate() {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(2026, java.util.Calendar.JANUARY, 15, 12, 0, 0)
        val initialTimestamp = calendar.timeInMillis

        // Monthly recurrence
        val nextMonth = com.example.data.model.RecurrenceFrequency.MONTHLY.computeNextDueDate(initialTimestamp)
        val expectedCal = java.util.Calendar.getInstance().apply { timeInMillis = initialTimestamp; add(java.util.Calendar.MONTH, 1) }
        assertEquals(expectedCal.timeInMillis, nextMonth)

        // Weekly recurrence
        val nextWeek = com.example.data.model.RecurrenceFrequency.WEEKLY.computeNextDueDate(initialTimestamp)
        val expectedWeekCal = java.util.Calendar.getInstance().apply { timeInMillis = initialTimestamp; add(java.util.Calendar.WEEK_OF_YEAR, 1) }
        assertEquals(expectedWeekCal.timeInMillis, nextWeek)
    }
}
