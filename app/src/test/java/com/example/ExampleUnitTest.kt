package com.example

import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.Currencies
import com.example.data.model.TwtAccountHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun currency_formattingIsCorrect() {
        assertEquals("$1,250.50", Currencies.format(1250.5, "USD"))
        assertEquals("1,250.50 CFA", Currencies.format(1250.5, "CFA"))
        // Legacy FCFA code must format cleanly without F prefix
        assertEquals("1,250.50 CFA", Currencies.format(1250.5, "FCFA"))
    }

    @Test
    fun twtAccount_alwaysUsesDynamicYearAndUsdSymbol() {
        val currentTwt = TwtAccountHelper.currentTwtAccountName()
        val year = TwtAccountHelper.currentYear()
        assertEquals("TWT $year", currentTwt)

        val legacyTwt = AccountEntity(
            id = "twt_legacy",
            name = "Compte TWT 2026 FCFA",
            type = AccountType.SAVINGS,
            balance = 250.0,
            currencyCode = "FCFA"
        )
        val normalized = TwtAccountHelper.normalizeTwtAccount(legacyTwt)
        assertEquals(currentTwt, normalized.name)
        assertEquals("USD", normalized.currencyCode)
        assertEquals("$250.00", Currencies.formatForAccount(normalized.balance, normalized))
        assertEquals("USD", Currencies.effectiveCurrencyCode(legacyTwt))
        assertEquals("$250.00", Currencies.formatForAccount(250.0, legacyTwt))
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
    fun presetFamilyContacts_contains21SpecifiedFamilyMembers() {
        val names = com.example.data.model.PresetFamilyContacts.NAMES
        assertEquals(21, names.size)
        assertTrue(names.contains("Drissa Traoré"))
        assertTrue(names.contains("Fatou ma Femme"))
        assertTrue(names.contains("Madou Traoré"))
        assertTrue(names.contains("Aïssata Traoré"))
        assertTrue(names.contains("Sogona Traoré"))
        assertTrue(names.contains("Boubacar Coulibaly"))
    }

    @Test
    fun walletCategories_containBonusFamilleAndCitiesWithoutPaymentRails() {
        val categories = com.example.data.model.WalletCategories.ALL.map { it.first }
        assertTrue(categories.contains("Bonus"))
        assertTrue(categories.contains("Famille"))
        assertTrue(categories.contains("Bamako"))
        assertTrue(categories.contains("Abidjan"))
        assertFalse(categories.contains("Transfert"))
        assertFalse(categories.contains("MoneyGram"))
        assertFalse(categories.contains("Tap Tap Send"))
        assertFalse(categories.contains("TWT"))

        val cities = com.example.data.model.PresetFamilyContacts.CITIES.map { it.first }
        assertEquals(listOf("Bamako", "Abidjan"), cities)
    }

    @Test
    fun seedAccountIds_provideExactlySixDeterministicAccounts() {
        val seeds = com.example.data.model.SeedAccountIds.defaultSeedAccounts(1700000000000L)
        assertEquals(6, seeds.size)
        assertEquals(com.example.data.model.SeedAccountIds.ALL_IDS, seeds.map { it.id }.toSet())
        val twt = seeds.first { it.id == com.example.data.model.SeedAccountIds.TWT_2026 }
        assertEquals(TwtAccountHelper.currentTwtAccountName(), twt.name)
        assertEquals("USD", twt.currencyCode)
        assertTrue(TwtAccountHelper.isTwtAccount(twt))
    }

    @Test
    fun bigDecimal_fxAndCurrencyArithmetic_isExact() {
        val parsed = Currencies.parseBigDecimal("1 250,50")
        assertEquals(java.math.BigDecimal("1250.50"), parsed)
        assertEquals(null, Currencies.parseBigDecimal(""))
        assertEquals(null, Currencies.parseBigDecimal("abc"))

        val sentUsd = java.math.BigDecimal("100.00")
        val feeUsd = java.math.BigDecimal("3.99")
        val rate = java.math.BigDecimal("605")
        assertEquals(java.math.BigDecimal("103.99"), Currencies.computeTotalUsdDeduction(sentUsd, feeUsd))
        assertEquals(java.math.BigDecimal("60500.00"), Currencies.computeNetCfaFromFx(sentUsd, rate))

        val sum = Currencies.sumAmounts(listOf(0.1, 0.2, 100.55))
        assertEquals(java.math.BigDecimal("100.85"), sum)
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

    @Test
    fun appDesignSpecBuilder_onlyGeneratesForPrimaryAdminAndMasksSensitiveIdsByDefault() {
        val adminSession = com.example.security.AccessSession(
            activeEmail = "410bala@gmail.com",
            googleEmail = "410bala@gmail.com",
            role = com.example.security.UserRole.ADMIN
        )
        val secondarySession = com.example.security.AccessSession(
            activeEmail = "traore200@gmail.com",
            googleEmail = "traore200@gmail.com",
            role = com.example.security.UserRole.SHARED_USER
        )

        val accounts = listOf(
            AccountEntity(id = "a1", name = "Cash CFA", type = AccountType.CASH, balance = 50000.0, currencyCode = "CFA"),
            AccountEntity(id = "a2", name = TwtAccountHelper.currentTwtAccountName(), type = AccountType.SAVINGS, balance = 350.0, currencyCode = "USD")
        )
        val txs = listOf(
            com.example.data.model.TransactionEntity(
                id = "t1",
                accountId = "a2",
                type = com.example.data.model.TransactionType.INCOME,
                amount = 150.0,
                payee = "Balatec Bonus",
                category = "Bonus"
            )
        )

        val maskedAdminSpec = com.example.util.AppDesignSpecBuilder.buildText(
            com.example.util.AppSpecSnapshot(
                accessSession = adminSession,
                syncState = com.example.data.sync.SyncState(isSyncEnabled = true, isAuthenticated = true, userEmail = "410bala@gmail.com"),
                currentTheme = com.example.ui.theme.ArtisanThemes.ESPRESSO_BROWN,
                baseCurrency = "CFA",
                currentLanguage = "fr",
                isPinEnabled = true,
                isBiometricEnabled = true,
                includeSensitiveIds = false,
                accounts = accounts,
                transactions = txs,
                recurringTransactions = emptyList(),
                customCategories = listOf("CustomTest" to "⭐")
            )
        )

        assertTrue(maskedAdminSpec.contains("410***@gmail.com"))
        assertTrue(maskedAdminSpec.contains("ai-studio-***"))
        assertFalse(maskedAdminSpec.contains("410bala@gmail.com"))
        assertFalse(maskedAdminSpec.contains("ai-studio-android-balatecs-4baf2e8b-4fe1-4d7a-a144-fd45aa8e45ca"))
        assertTrue(maskedAdminSpec.contains("Espresso Brown"))
        assertTrue(maskedAdminSpec.contains("Classic Saddle Tan"))
        assertTrue(maskedAdminSpec.contains("Ivory White Leather"))
        assertTrue(maskedAdminSpec.contains("Cash CFA"))
        assertTrue(maskedAdminSpec.contains("50,000.00 CFA"))
        assertTrue(maskedAdminSpec.contains("$350.00"))
        assertTrue(maskedAdminSpec.contains("Balatec Bonus"))
        assertTrue(maskedAdminSpec.contains("CustomTest [Custom]"))

        val unmaskedAdminSpec = com.example.util.AppDesignSpecBuilder.buildText(
            com.example.util.AppSpecSnapshot(
                accessSession = adminSession,
                syncState = com.example.data.sync.SyncState(isSyncEnabled = true, isAuthenticated = true, userEmail = "410bala@gmail.com"),
                currentTheme = com.example.ui.theme.ArtisanThemes.ESPRESSO_BROWN,
                baseCurrency = "CFA",
                currentLanguage = "fr",
                isPinEnabled = true,
                isBiometricEnabled = true,
                includeSensitiveIds = true,
                accounts = accounts,
                transactions = txs,
                recurringTransactions = emptyList(),
                customCategories = listOf("CustomTest" to "⭐")
            )
        )
        assertTrue(unmaskedAdminSpec.contains("410bala@gmail.com"))
        assertTrue(unmaskedAdminSpec.contains("ai-studio-android-balatecs-4baf2e8b-4fe1-4d7a-a144-fd45aa8e45ca"))

        val secondarySpec = com.example.util.AppDesignSpecBuilder.buildText(
            com.example.util.AppSpecSnapshot(
                accessSession = secondarySession,
                syncState = com.example.data.sync.SyncState(),
                currentTheme = com.example.ui.theme.ArtisanThemes.ESPRESSO_BROWN,
                baseCurrency = "CFA",
                currentLanguage = "fr",
                isPinEnabled = false,
                isBiometricEnabled = false,
                accounts = accounts,
                transactions = txs,
                recurringTransactions = emptyList(),
                customCategories = emptyList()
            )
        )
        assertEquals("", secondarySpec)
    }
}

