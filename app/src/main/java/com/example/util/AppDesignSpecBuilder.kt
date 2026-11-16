package com.example.util

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.data.model.AccountEntity
import com.example.data.model.Currencies
import com.example.data.model.PresetFamilyContacts
import com.example.data.model.RecurringTransactionEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.TwtAccountHelper
import com.example.data.model.WalletCategories
import com.example.data.sync.SyncState
import com.example.security.AccessControlManager
import com.example.security.AccessSession
import com.example.security.UserRole
import com.example.ui.theme.ArtisanThemeConfig
import com.example.ui.theme.ArtisanThemes
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class AppSpecSnapshot(
    val accessSession: AccessSession,
    val syncState: SyncState,
    val currentTheme: ArtisanThemeConfig,
    val baseCurrency: String,
    val currentLanguage: String,
    val isPinEnabled: Boolean,
    val isBiometricEnabled: Boolean,
    val accounts: List<AccountEntity>,
    val transactions: List<TransactionEntity>,
    val recurringTransactions: List<RecurringTransactionEntity>,
    val customCategories: List<Pair<String, String>>,
    val generatedAtMillis: Long = System.currentTimeMillis()
)

object AppDesignSpecBuilder {

    const val ADMIN_SPEC_FILENAME = "balatec_app_details_design.txt"

    fun isEligibleForAdminSpec(session: AccessSession): Boolean {
        return session.role == UserRole.ADMIN &&
            session.activeEmail.equals(AccessControlManager.ADMIN_EMAIL, ignoreCase = true)
    }

    fun buildText(snapshot: AppSpecSnapshot): String {
        if (!isEligibleForAdminSpec(snapshot.accessSession)) return ""

        val utcFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

        val accountMap = snapshot.accounts.associateBy { it.id }
        val nonTwtCfaAccounts = snapshot.accounts.filter {
            !TwtAccountHelper.isTwtAccount(it) && Currencies.effectiveCurrencyCode(it) == "CFA"
        }
        val twtAccounts = snapshot.accounts.filter { TwtAccountHelper.isTwtAccount(it) }
        val nonTwtBalance = nonTwtCfaAccounts.sumOf { it.balance }
        val twtBalance = twtAccounts.sumOf { it.balance }

        val activeTransactions = snapshot.transactions.filter { !it.isDeleted }
        val cfaIncome = activeTransactions.filter {
            it.type == TransactionType.INCOME &&
                !TwtAccountHelper.isTwtAccount(accountMap[it.accountId]) &&
                Currencies.effectiveCurrencyCode(accountMap[it.accountId], "CFA") == "CFA"
        }.sumOf { it.amount }
        val cfaExpense = activeTransactions.filter {
            it.type == TransactionType.EXPENSE &&
                !TwtAccountHelper.isTwtAccount(accountMap[it.accountId]) &&
                Currencies.effectiveCurrencyCode(accountMap[it.accountId], "CFA") == "CFA"
        }.sumOf { it.amount }

        val allCategories = WalletCategories.combined(snapshot.customCategories)

        return buildString {
            appendLine("================================================================================")
            appendLine("BALATEC SECURE WALLET — LIVE APP CONTENTS, ARCHITECTURE & DESIGN (.TXT)")
            appendLine("================================================================================")
            appendLine("Admin Owner       : ${AccessControlManager.ADMIN_EMAIL}")
            appendLine("Last Auto-Updated : ${utcFmt.format(Date(snapshot.generatedAtMillis))}")
            appendLine("Application ID    : ${BuildConfig.APPLICATION_ID}")
            appendLine("Version           : ${BuildConfig.VERSION_NAME} (code ${BuildConfig.VERSION_CODE})")
            appendLine("Firestore DB ID   : ai-studio-android-balatecs-4baf2e8b-4fe1-4d7a-a144-fd45aa8e45ca")
            appendLine()

            appendLine("--------------------------------------------------------------------------------")
            appendLine("1. LIVE SESSION, SETTINGS & SYNC STATUS")
            appendLine("--------------------------------------------------------------------------------")
            appendLine("Active Email      : ${snapshot.accessSession.activeEmail ?: "Signed out"}")
            appendLine("Google Account    : ${snapshot.accessSession.googleEmail ?: "Not linked"}")
            appendLine("Role              : ${snapshot.accessSession.role} (TWT Visible: ${snapshot.accessSession.isAdminTwtVisible})")
            appendLine("Active Theme      : ${snapshot.currentTheme.name} [${snapshot.currentTheme.id}]")
            appendLine("Hardware Finish   : ${snapshot.currentTheme.hardwareName} (${colorHex(snapshot.currentTheme.metallicAccent)})")
            appendLine("Base Currency     : ${snapshot.baseCurrency} (TWT locked to USD $)")
            appendLine("Language          : ${if (snapshot.currentLanguage.startsWith("en", true)) "English (en)" else "Français (fr)"}")
            appendLine("PIN Lock          : ${if (snapshot.isPinEnabled) "Enabled (4-digit)" else "Disabled"}")
            appendLine("Biometric Unlock  : ${if (snapshot.isBiometricEnabled) "Enabled" else "Disabled"}")
            appendLine("Cloud Sync        : ${snapshot.syncState.status} | Enabled=${snapshot.syncState.isSyncEnabled} | Auth=${snapshot.syncState.isAuthenticated}")
            appendLine("Last Cloud Sync   : ${snapshot.syncState.lastSyncTime?.let { dateFmt.format(Date(it)) } ?: "Never"}")
            appendLine()

            appendLine("--------------------------------------------------------------------------------")
            appendLine("2. LIVE ACCOUNTS & BALANCES (${snapshot.accounts.size} total)")
            appendLine("--------------------------------------------------------------------------------")
            appendLine("Total Shared CFA Balance : ${Currencies.format(nonTwtBalance, "CFA")}")
            appendLine("Shared CFA Income        : ${Currencies.format(cfaIncome, "CFA")}")
            appendLine("Shared CFA Expenses      : ${Currencies.format(cfaExpense, "CFA")}")
            if (twtAccounts.isNotEmpty()) {
                appendLine("Private ${TwtAccountHelper.currentTwtAccountName()} USD Balance : ${Currencies.format(twtBalance, "USD")}")
            }
            appendLine()
            if (snapshot.accounts.isEmpty()) {
                appendLine("  (No accounts yet)")
            } else {
                snapshot.accounts.forEachIndexed { idx, acc ->
                    val isTwt = TwtAccountHelper.isTwtAccount(acc)
                    val displayName = if (isTwt) TwtAccountHelper.currentTwtAccountName() else acc.name
                    val formattedBal = Currencies.formatForAccount(acc.balance, acc, "CFA")
                    val scopeTag = if (isTwt) "ADMIN_ONLY_TWT" else "SHARED"
                    appendLine("  ${idx + 1}. ${acc.emojiAvatar} $displayName | Type=${acc.type} | Balance=$formattedBal | Scope=$scopeTag | ID=${acc.id}")
                }
            }
            appendLine()

            appendLine("--------------------------------------------------------------------------------")
            appendLine("3. LIVE TRANSACTIONS LEDGER (${activeTransactions.size} active)")
            appendLine("--------------------------------------------------------------------------------")
            if (activeTransactions.isEmpty()) {
                appendLine("  (No transactions recorded)")
            } else {
                activeTransactions.sortedByDescending { it.timestamp }.take(100).forEachIndexed { idx, tx ->
                    val acc = accountMap[tx.accountId]
                    val accName = if (TwtAccountHelper.isTwtAccount(acc)) TwtAccountHelper.currentTwtAccountName() else (acc?.name ?: "Unknown")
                    val formattedAmt = Currencies.formatForAccount(tx.amount, acc, "CFA")
                    val sign = if (tx.type == TransactionType.INCOME) "+" else "-"
                    val notePart = if (tx.notes.isNotBlank()) " | Note=${tx.notes}" else ""
                    appendLine("  ${idx + 1}. [${dateFmt.format(Date(tx.timestamp))}] $sign$formattedAmt | $accName | Payee=${tx.payee} | Cat=${tx.category}$notePart")
                }
                if (activeTransactions.size > 100) {
                    appendLine("  ... (${activeTransactions.size - 100} older transactions omitted from preview)")
                }
            }
            appendLine()

            appendLine("--------------------------------------------------------------------------------")
            appendLine("4. RECURRING SCHEDULES (${snapshot.recurringTransactions.size})")
            appendLine("--------------------------------------------------------------------------------")
            if (snapshot.recurringTransactions.isEmpty()) {
                appendLine("  (No recurring schedules configured)")
            } else {
                snapshot.recurringTransactions.forEachIndexed { idx, rec ->
                    val acc = accountMap[rec.accountId]
                    val formattedAmt = Currencies.formatForAccount(rec.amount, acc, "CFA")
                    appendLine("  ${idx + 1}. ${rec.payee} | $formattedAmt | ${rec.frequency} | Cat=${rec.category} | Active=${rec.isActive} | Next=${dateFmt.format(Date(rec.nextDueDate))}")
                }
            }
            appendLine()

            appendLine("--------------------------------------------------------------------------------")
            appendLine("5. CATEGORIES (${allCategories.size}) & FAMILY CONTACTS (${PresetFamilyContacts.NAMES.size})")
            appendLine("--------------------------------------------------------------------------------")
            appendLine("Categories (Default + Custom):")
            allCategories.forEach { (name, icon) ->
                val isCustom = snapshot.customCategories.any { it.first.equals(name, ignoreCase = true) }
                appendLine("  - $icon $name${if (isCustom) " [Custom]" else ""}")
            }
            appendLine()
            appendLine("Preset Family Contacts:")
            appendLine("  " + PresetFamilyContacts.NAMES.joinToString(", "))
            appendLine()

            appendLine("--------------------------------------------------------------------------------")
            appendLine("6. DESIGN SYSTEM & 10 SKEUOMORPHIC LEATHER THEMES")
            appendLine("--------------------------------------------------------------------------------")
            appendLine("Branding       : Circular Gold Medallion (ic_balatec_coin), #170E07 Splash, #D4AF37 Gold Rim")
            appendLine("Shapes (M3)    : extraSmall=6dp, small=10dp, medium=16dp, large=20dp, extraLarge=26dp")
            appendLine("Typography     : Serif Display (32sp) + SansSerif Tabular Numerals (tnum) for financial values")
            appendLine("Navigation     : TopAppBar (Logo + Sync Badge) + 4 Tabs (Ledger, Accounts, Categories, Settings)")
            appendLine("Surface Texture: Modifier.artisanLeatherTexture (ArtisanCard) + artisanNavigationBarLeather (NavigationBar metallic rim & stitch seam)")
            appendLine("Account Cards  : Slide-to-delete DISABLED; explicit Edit/Delete via 3-dot MoreVert menu")
            appendLine()
            ArtisanThemes.ALL_THEMES.forEachIndexed { idx, t ->
                val group = if (t.isClassic) "Classic" else "Bold"
                val darkTag = if (t.isDark) "Dark" else "Light"
                appendLine(
                    "  ${idx + 1}. ${t.name} (${t.id}) [$group/$darkTag] | Hardware=${t.hardwareName} (${colorHex(t.metallicAccent)}) | " +
                        "Primary=${colorHex(t.primaryColor)} | Container=${colorHex(t.primaryContainer)} | " +
                        "Surface=${colorHex(t.surfaceColor)} | Bg=${colorHex(t.backgroundColor)} | Border=${colorHex(t.cardBorderColor)}"
                )
            }
            appendLine()

            appendLine("--------------------------------------------------------------------------------")
            appendLine("7. ARCHITECTURE, FIRESTORE ISOLATION & SECURITY RULES")
            appendLine("--------------------------------------------------------------------------------")
            appendLine("Architecture   : Clean MVVM + Offline-first Room (accounts, transactions, recurring, contacts) + Cloud Firestore")
            appendLine("Admin Access   : ${AccessControlManager.ADMIN_EMAIL} -> Shared Wallet + Private TWT + Admin Design Spec")
            appendLine("Shared Access  : ${AccessControlManager.SECONDARY_EMAIL} -> Shared Wallet only (TWT strictly excluded)")
            appendLine("Firestore Paths:")
            appendLine("  - Shared     : shared_wallets/balatec_shared/{accounts, transactions, recurring, categories}")
            appendLine("  - TWT Private: private_twt/admin_only/{accounts, transactions, recurring}")
            appendLine("Auth & Lock    : Google Sign-In (CredentialManager) + Case-sensitive Secrets Passcode + 4-digit PIN + BiometricPrompt")
            appendLine("PDF & Exports  : PdfReportExporter (All/Account/Payee/Category/Date/Type filters) + JSON/CSV + Admin .txt")
            appendLine("================================================================================")
        }
    }

    fun getSpecFile(context: Context): File = File(context.filesDir, ADMIN_SPEC_FILENAME)

    fun syncSpecFileOnDisk(context: Context, session: AccessSession, specText: String) {
        val target = getSpecFile(context)
        if (!isEligibleForAdminSpec(session) || specText.isBlank()) {
            if (target.exists()) target.delete()
            return
        }
        target.writeText(specText, Charsets.UTF_8)
    }

    fun shareSpecTextFile(context: Context, specText: String): Boolean {
        return try {
            val file = getSpecFile(context)
            file.writeText(specText, Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Balatec Secure Wallet — Contents & Design (.txt)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, "Exporter Balatec_App_Details_Design.txt")
            context.startActivity(chooser)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun colorHex(color: Color): String {
        return String.format(Locale.US, "#%08X", color.toArgb())
    }
}
