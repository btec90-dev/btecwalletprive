package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.WalletDatabase
import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.SeedAccountIds
import com.example.data.model.TwtAccountHelper
import com.example.data.repository.WalletRepository
import com.example.util.PdfReportExporter
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createEmptyComposeRule()

  @Before
  fun ensureFirebaseInitialized() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    if (FirebaseApp.getApps(context).isEmpty()) {
      FirebaseApp.initializeApp(
        context,
        FirebaseOptions.Builder()
          .setApplicationId("com.aistudio.balatecwallet.kzrqmv")
          .setProjectId("demo-no-project")
          .setApiKey("fake-api-key")
          .build()
      )
    }
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Balatec Secure Wallet", appName)
  }

  @Test
  fun `main activity launches and provides ActivityResultRegistryOwner`() {
    ActivityScenario.launch(MainActivity::class.java).use {
      composeTestRule.onNodeWithTag("header_gold_logo").assertIsDisplayed()
      composeTestRule.onNodeWithTag("header_sync_badge").assertIsDisplayed()
    }
  }

  @Test
  fun `normalizeAccountsOnStartup deduplicates zero balance seed duplicates to 6 canonical accounts`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, WalletDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    try {
      val dao = db.walletDao()
      val repo = WalletRepository(dao, context)
      runBlocking {
        // Insert canonical seeds + legacy 0.00 duplicates sharing names & scope
        dao.insertSeedAccounts(SeedAccountIds.defaultSeedAccounts(1000L))
        dao.insertAccount(
          AccountEntity(
            id = "legacy_dup_1",
            name = "Espèces (CFA)",
            type = AccountType.CASH,
            balance = 0.0,
            currencyCode = "CFA"
          )
        )
        dao.insertAccount(
          AccountEntity(
            id = "legacy_dup_2",
            name = "Tap Tap Send",
            type = AccountType.CUSTOM,
            balance = 0.0,
            currencyCode = "CFA"
          )
        )
        dao.insertAccount(
          AccountEntity(
            id = "legacy_twt_dup",
            name = "Compte TWT 2026 FCFA",
            type = AccountType.SAVINGS,
            balance = 0.0,
            currencyCode = "FCFA"
          )
        )

        repo.normalizeAccountsOnStartup()
        val remaining = dao.getAccountsList()
        assertEquals(6, remaining.size)
        assertEquals(SeedAccountIds.ALL_IDS, remaining.map { it.id }.toSet())
      }
    } finally {
      db.close()
    }
  }

  @Test
  fun `executeRemittanceFxTransaction deducts USD plus fee and credits exact net CFA`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, WalletDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    try {
      val dao = db.walletDao()
      val repo = WalletRepository(dao, context)
      runBlocking {
        val usdAcc = AccountEntity(
          id = "usd_src",
          name = TwtAccountHelper.currentTwtAccountName(),
          type = AccountType.SAVINGS,
          balance = 500.0,
          currencyCode = "USD"
        )
        val cfaAcc = AccountEntity(
          id = SeedAccountIds.TAP_TAP_SEND,
          name = "Tap Tap Send",
          type = AccountType.CUSTOM,
          balance = 10000.0,
          currencyCode = "CFA"
        )
        dao.insertAccount(usdAcc)
        dao.insertAccount(cfaAcc)

        val result = repo.executeRemittanceFxTransaction(
          usdAccountId = "usd_src",
          cfaAccountId = SeedAccountIds.TAP_TAP_SEND,
          amountSentUsd = BigDecimal("100.00"),
          feeUsd = BigDecimal("4.50"),
          exchangeRateUsdToCfa = BigDecimal("605"),
          payee = "Drissa Traoré",
          category = "Famille",
          timestamp = System.currentTimeMillis(),
          notes = "Envoi mensuel"
        )
        assertNotNull(result)
        val updatedUsd = dao.getAccountById("usd_src")
        val updatedCfa = dao.getAccountById(SeedAccountIds.TAP_TAP_SEND)
        assertEquals(395.50, updatedUsd?.balance ?: 0.0, 0.001)
        assertEquals(70500.00, updatedCfa?.balance ?: 0.0, 0.001)
      }
    } finally {
      db.close()
    }
  }

  @Test
  fun `exports succeed safely even with zero transactions`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, WalletDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    try {
      val repo = WalletRepository(db.walletDao(), context)
      runBlocking {
        val emptyJson = repo.exportToJson(includeTwt = false)
        assertTrue(emptyJson.contains("\"transactions\":[]") || emptyJson.contains("\"transactions\": []"))
        val emptyCsv = repo.exportToCsv(includeTwt = false)
        assertEquals("Date,Account,Payee,Category,Type,Amount,Currency,Notes\n", emptyCsv)
      }
      // On Android runtime returns non-empty PDF file; on Robolectric without native Skia PDF must return null without throwing
      val file = PdfReportExporter.generateTransactionsPdf(
        context = context,
        transactions = emptyList(),
        accountMap = emptyMap(),
        title = "Empty Ledger Report",
        periodLabel = "Toutes les dates"
      )
      if (file != null) {
        assertTrue(file.exists() && file.length() > 0L)
      }
    } finally {
      db.close()
    }
  }
}
