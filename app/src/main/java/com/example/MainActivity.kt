package com.example

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.data.sync.SyncStatus
import com.example.security.BiometricHelper
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.AuthGateBottomSheet
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.ExportImportDialog
import com.example.ui.screens.LedgerScreen
import com.example.ui.screens.LockScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.BalatecWalletTheme
import com.example.ui.theme.LocalArtisanTheme
import com.example.ui.viewmodel.WalletViewModel
import java.util.Locale

class MainActivity : FragmentActivity() {

    private val viewModel: WalletViewModel by viewModels()

    override fun attachBaseContext(newBase: android.content.Context) {
        val prefs = newBase.getSharedPreferences("balatec_settings_prefs", android.content.Context.MODE_PRIVATE)
        val lang = prefs.getString("language", "fr") ?: "fr"
        val locale = if (lang.startsWith("en", ignoreCase = true)) Locale.ENGLISH else Locale.FRENCH
        Locale.setDefault(locale)
        val config = android.content.res.Configuration(newBase.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        val localizedContext = newBase.createConfigurationContext(config)
        super.attachBaseContext(localizedContext)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val credentialManager = androidx.credentials.CredentialManager.create(this)
        viewModel.cloudSyncManager.attemptAutoSignIn(
            credentialManager = credentialManager,
            scope = lifecycleScope,
            onAuthSuccess = {
                viewModel.onAuthSuccess()
            }
        )

        setContent {
            val currentThemeConfig by viewModel.currentTheme.collectAsState()
            val currentLanguage by viewModel.currentLanguage.collectAsState()
            val isLocked by viewModel.isLocked.collectAsState()

            // Dynamic locale support for English and French (default French)
            val locale = if (currentLanguage.startsWith("en", ignoreCase = true)) Locale.ENGLISH else Locale.FRENCH
            val baseContext = LocalContext.current
            val localizedContext = remember(baseContext, locale) {
                Locale.setDefault(locale)
                val configuration = android.content.res.Configuration(baseContext.resources.configuration).apply {
                    setLocale(locale)
                    setLayoutDirection(locale)
                }
                baseContext.createConfigurationContext(configuration)
            }

            CompositionLocalProvider(
                LocalConfiguration provides localizedContext.resources.configuration,
                LocalContext provides localizedContext
            ) {
                BalatecWalletTheme(themeConfig = currentThemeConfig) {
                    val theme = LocalArtisanTheme.current

                    if (isLocked) {
                        val isBiometricAvailable = remember {
                            BiometricHelper.isBiometricAvailable(this@MainActivity) && viewModel.securityManager.isBiometricEnabled()
                        }

                        LockScreen(
                            isBiometricAvailable = isBiometricAvailable,
                            onPinEntered = { pin -> viewModel.unlockWithPin(pin) },
                            onBiometricClick = {
                                BiometricHelper.promptBiometric(
                                    activity = this@MainActivity,
                                    title = getString(R.string.biometric_prompt_title),
                                    subtitle = getString(R.string.biometric_prompt_sub),
                                    onSuccess = { viewModel.unlockWithBiometric() },
                                    onError = { /* fallback to pin */ }
                                )
                            }
                        )
                    } else {
                        MainWalletApp(viewModel = viewModel, activity = this@MainActivity)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainWalletApp(
    viewModel: WalletViewModel,
    activity: FragmentActivity
) {
    val theme = LocalArtisanTheme.current
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Accounts, 1: Ledger, 2: Analytics, 3: Settings

    val accounts by viewModel.accounts.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val recurringTransactions by viewModel.recurringTransactions.collectAsState()
    val totalBalance by viewModel.totalBalance.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val baseCurrency by viewModel.baseCurrency.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val currentThemeConfig by viewModel.currentTheme.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val isPinEnabled by viewModel.isPinEnabled.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()

    var showAuthGateSheet by remember { mutableStateOf(false) }
    var showExportImportDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.bw_circle_logo_1794353937187),
                            contentDescription = stringResource(R.string.app_name),
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = theme.primaryColor
                        )
                    }
                },
                actions = {
                    // Subtle status indicator in the header showing offline/sync status
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                when (syncState.status) {
                                    SyncStatus.SYNCED -> Color(0xFF2E7D32).copy(alpha = 0.12f)
                                    SyncStatus.SYNCING -> Color(0xFF1976D2).copy(alpha = 0.12f)
                                    else -> theme.surfaceColor.copy(alpha = 0.85f)
                                }
                            )
                            .clickable {
                                if (!syncState.isAuthenticated) {
                                    showAuthGateSheet = true
                                } else {
                                    viewModel.syncNow()
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (syncState.status) {
                                            SyncStatus.SYNCED -> Color(0xFF2E7D32)
                                            SyncStatus.SYNCING -> Color(0xFF1976D2)
                                            else -> Color(0xFF9E9E9E)
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = when (syncState.status) {
                                    SyncStatus.SYNCING -> Icons.Default.Sync
                                    SyncStatus.SYNCED -> Icons.Default.CloudDone
                                    else -> Icons.Default.CloudOff
                                },
                                contentDescription = null,
                                tint = when (syncState.status) {
                                    SyncStatus.SYNCED -> Color(0xFF2E7D32)
                                    SyncStatus.SYNCING -> Color(0xFF1976D2)
                                    else -> Color(0xFF757575)
                                },
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = when (syncState.status) {
                                    SyncStatus.SYNCING -> stringResource(R.string.sync_status_syncing)
                                    SyncStatus.SYNCED -> stringResource(R.string.sync_status_synced)
                                    else -> stringResource(R.string.sync_status_local)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = when (syncState.status) {
                                    SyncStatus.SYNCED -> Color(0xFF2E7D32)
                                    SyncStatus.SYNCING -> Color(0xFF1976D2)
                                    else -> theme.primaryColor
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = theme.backgroundColor
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = theme.surfaceColor,
                tonalElevation = 6.dp
            ) {
                // 1. Ledger icon is first
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = stringResource(R.string.nav_ledger)) },
                    label = { Text(stringResource(R.string.nav_ledger)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = theme.primaryColor,
                        selectedTextColor = theme.primaryColor,
                        indicatorColor = theme.primaryContainer,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    ),
                    modifier = Modifier.testTag("nav_item_ledger")
                )

                // 2. Account icon is second
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = stringResource(R.string.nav_accounts)) },
                    label = { Text(stringResource(R.string.nav_accounts)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = theme.primaryColor,
                        selectedTextColor = theme.primaryColor,
                        indicatorColor = theme.primaryContainer,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    ),
                    modifier = Modifier.testTag("nav_item_accounts")
                )

                // 3. Categories icon replaces Analytics
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Category, contentDescription = stringResource(R.string.nav_categories)) },
                    label = { Text(stringResource(R.string.nav_categories)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = theme.primaryColor,
                        selectedTextColor = theme.primaryColor,
                        indicatorColor = theme.primaryContainer,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    ),
                    modifier = Modifier.testTag("nav_item_categories")
                )

                // 4. Settings icon
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.nav_settings)) },
                    label = { Text(stringResource(R.string.nav_settings)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = theme.primaryColor,
                        selectedTextColor = theme.primaryColor,
                        indicatorColor = theme.primaryContainer,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    ),
                    modifier = Modifier.testTag("nav_item_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> LedgerScreen(
                    transactions = transactions,
                    accounts = accounts,
                    recurringList = recurringTransactions,
                    baseCurrency = baseCurrency,
                    onAddTransaction = { viewModel.addTransaction(it) },
                    onUpdateTransaction = { oldTx, newTx -> viewModel.updateTransaction(oldTx, newTx) },
                    onDeleteTransaction = { viewModel.deleteTransaction(it) },
                    onDeleteTransactions = { viewModel.deleteTransactions(it) },
                    onAddRecurring = { viewModel.addRecurringTransaction(it) },
                    onToggleRecurringActive = { id, active -> viewModel.toggleRecurringActive(id, active) },
                    onExecuteRecurringNow = { viewModel.executeRecurringNow(it) },
                    onDeleteRecurring = { viewModel.deleteRecurringTransaction(it) },
                    onProcessDueRecurring = { viewModel.checkAndProcessDueRecurring() }
                )
                1 -> AccountsScreen(
                    accounts = accounts,
                    totalBalance = totalBalance,
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    baseCurrency = baseCurrency,
                    onAddAccount = { name, type, balance, cur, emoji ->
                        viewModel.addAccount(name, type, balance, cur, emoji)
                    },
                    onUpdateAccount = { viewModel.updateAccount(it) },
                    onDeleteAccount = { viewModel.deleteAccount(it) }
                )
                2 -> CategoriesScreen(
                    customCategories = emptyList(),
                    onAddCategory = { _, _ -> },
                    onDeleteCategory = { _ -> }
                )
                3 -> SettingsScreen(
                    currentThemeConfig = currentThemeConfig,
                    baseCurrency = baseCurrency,
                    currentLanguage = currentLanguage,
                    syncState = syncState,
                    isPinEnabled = isPinEnabled,
                    isBiometricEnabled = isBiometricEnabled,
                    onThemeSelected = { viewModel.setTheme(it) },
                    onCurrencySelected = { viewModel.setBaseCurrency(it) },
                    onLanguageSelected = { viewModel.setLanguage(it) },
                    onToggleSync = { enabled ->
                        if (enabled && !syncState.isAuthenticated) {
                            showAuthGateSheet = true
                        } else if (!enabled) {
                            viewModel.cloudSyncManager.disableSync()
                        }
                    },
                    onSyncNow = { viewModel.syncNow() },
                    onSignOut = {
                        val credentialManager = androidx.credentials.CredentialManager.create(context)
                        viewModel.signOutCloud(credentialManager)
                    },
                    onTogglePin = { viewModel.togglePin(it) },
                    onSetPin = { viewModel.setPin(it) },
                    onToggleBiometric = { viewModel.toggleBiometric(it) },
                    onOpenExportImport = { showExportImportDialog = true },
                    onClearTransactions = { viewModel.clearAllTransactions() }
                )
            }
        }
    }

    // Account Wall / Gate Bottom Sheet
    if (showAuthGateSheet) {
        AuthGateBottomSheet(
            cloudSyncManager = viewModel.cloudSyncManager,
            onDismissRequest = { showAuthGateSheet = false },
            onAuthSuccess = {
                viewModel.onAuthSuccess()
            }
        )
    }

    // Export / Import Dialog
    if (showExportImportDialog) {
        ExportImportDialog(
            transactions = transactions,
            accounts = accounts,
            onExportJson = { viewModel.exportJson() },
            onExportCsv = { viewModel.exportCsv() },
            onImportJson = { json -> viewModel.importJson(json) },
            onDismissRequest = { showExportImportDialog = false }
        )
    }
}
