package com.example

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.data.sync.SyncStatus
import com.example.security.BiometricHelper
import com.example.ui.components.artisanNavigationBarLeather
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.AuthGateBottomSheet
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.ExportImportDialog
import com.example.ui.screens.LedgerScreen
import com.example.ui.screens.LockScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.BalatecWalletTheme
import com.example.ui.theme.LocalArtisanTheme
import com.example.ui.viewmodel.WalletViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
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
            var showSplash by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                delay(650L)
                showSplash = false
            }

            // Dynamic locale support for English and French (default French) while preserving Activity owners
            val locale = if (currentLanguage.startsWith("en", ignoreCase = true)) Locale.ENGLISH else Locale.FRENCH
            val activityContext = this@MainActivity
            val localizedContext = remember(activityContext, locale) {
                Locale.setDefault(locale)
                val configuration = Configuration(activityContext.resources.configuration).apply {
                    setLocale(locale)
                    setLayoutDirection(locale)
                }
                val configContext = activityContext.createConfigurationContext(configuration)
                object : ContextWrapper(activityContext) {
                    override fun getResources(): Resources = configContext.resources
                    override fun getAssets(): AssetManager = configContext.assets
                    override fun createConfigurationContext(overrideConfiguration: Configuration): Context {
                        return configContext.createConfigurationContext(overrideConfiguration)
                    }
                }
            }

            CompositionLocalProvider(
                LocalConfiguration provides localizedContext.resources.configuration,
                LocalContext provides localizedContext,
                LocalActivityResultRegistryOwner provides this@MainActivity,
                LocalOnBackPressedDispatcherOwner provides this@MainActivity
            ) {
                BalatecWalletTheme(themeConfig = currentThemeConfig) {
                    Box(modifier = Modifier.fillMaxSize()) {
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

                        // Branded Balatec Gold Medallion Splash Overlay (#170E07)
                        AnimatedVisibility(
                            visible = showSplash,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF170E07)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.ic_balatec_coin),
                                        contentDescription = stringResource(R.string.app_name),
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(CircleShape)
                                            .border(2.5.dp, Color(0xFFD4AF37), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "BALATEC SECURE WALLET",
                                        color = Color(0xFFE6C875),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.6.sp
                                    )
                                }
                            }
                        }
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
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Ledger, 1: Accounts, 2: Categories, 3: Settings

    val accounts by viewModel.accounts.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val recurringTransactions by viewModel.recurringTransactions.collectAsState()
    val customCategories by viewModel.customCategories.collectAsState()
    val totalBalance by viewModel.totalBalance.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val twtUsdBalance by viewModel.twtUsdBalance.collectAsState()
    val baseCurrency by viewModel.baseCurrency.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val accessSession by viewModel.accessSession.collectAsState()
    val adminDesignSpecText by viewModel.adminDesignSpecText.collectAsState()
    val currentThemeConfig by viewModel.currentTheme.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val isPinEnabled by viewModel.isPinEnabled.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val isTwtVisibleEnabled by viewModel.isTwtVisibleEnabled.collectAsState()
    val includeSensitiveIdsInSpec by viewModel.includeSensitiveIdsInSpec.collectAsState()

    var showAuthGateSheet by remember { mutableStateOf(false) }
    var showExportImportDialog by remember { mutableStateOf(false) }

    val syncTimeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.ic_balatec_coin),
                            contentDescription = stringResource(R.string.app_name),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, theme.metallicAccent, CircleShape)
                                .testTag("header_gold_logo")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = theme.primaryColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    // Permanent Account Sync Indicator Badge (Green dot + Cloud + Status + Timestamp)
                    Box(
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                when (syncState.status) {
                                    SyncStatus.SYNCED -> Color(0xFF2E7D32).copy(alpha = 0.14f)
                                    SyncStatus.SYNCING -> Color(0xFF1976D2).copy(alpha = 0.14f)
                                    else -> theme.surfaceElevatedColor
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
                            .testTag("header_sync_badge")
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
                            Spacer(modifier = Modifier.width(5.dp))
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
                                    else -> theme.secondaryTextColor
                                },
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            val shortSyncLabel = when (syncState.status) {
                                SyncStatus.SYNCING -> "Sync..."
                                SyncStatus.SYNCED -> {
                                    val timePart = syncState.lastSyncTime?.let { " ${syncTimeFormat.format(Date(it))}" }.orEmpty()
                                    "Synced$timePart"
                                }
                                else -> "Local"
                            }
                            Text(
                                text = shortSyncLabel,
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
                modifier = Modifier.artisanNavigationBarLeather(theme),
                containerColor = Color.Transparent,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = stringResource(R.string.nav_ledger)) },
                    label = { Text(stringResource(R.string.nav_ledger)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = theme.primaryColor,
                        selectedTextColor = theme.primaryColor,
                        indicatorColor = theme.primaryContainer,
                        unselectedIconColor = theme.secondaryTextColor,
                        unselectedTextColor = theme.secondaryTextColor
                    ),
                    modifier = Modifier.testTag("nav_item_ledger")
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = stringResource(R.string.nav_accounts)) },
                    label = { Text(stringResource(R.string.nav_accounts)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = theme.primaryColor,
                        selectedTextColor = theme.primaryColor,
                        indicatorColor = theme.primaryContainer,
                        unselectedIconColor = theme.secondaryTextColor,
                        unselectedTextColor = theme.secondaryTextColor
                    ),
                    modifier = Modifier.testTag("nav_item_accounts")
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Category, contentDescription = stringResource(R.string.nav_categories)) },
                    label = { Text(stringResource(R.string.nav_categories)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = theme.primaryColor,
                        selectedTextColor = theme.primaryColor,
                        indicatorColor = theme.primaryContainer,
                        unselectedIconColor = theme.secondaryTextColor,
                        unselectedTextColor = theme.secondaryTextColor
                    ),
                    modifier = Modifier.testTag("nav_item_categories")
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.nav_settings)) },
                    label = { Text(stringResource(R.string.nav_settings)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = theme.primaryColor,
                        selectedTextColor = theme.primaryColor,
                        indicatorColor = theme.primaryContainer,
                        unselectedIconColor = theme.secondaryTextColor,
                        unselectedTextColor = theme.secondaryTextColor
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
                    customCategories = customCategories,
                    baseCurrency = baseCurrency,
                    isAdminTwtVisible = accessSession.isAdminTwtVisible && isTwtVisibleEnabled,
                    onAddTransaction = { viewModel.addTransaction(it) },
                    onSaveRemittanceFx = { usdAccId, cfaAccId, amtUsd, feeUsd, rate, payee, cat, ts, notes ->
                        viewModel.addRemittanceFx(usdAccId, cfaAccId, amtUsd, feeUsd, rate, payee, cat, ts, notes)
                    },
                    onUpdateTransaction = { oldTx, newTx -> viewModel.updateTransaction(oldTx, newTx) },
                    onDeleteTransaction = { viewModel.deleteTransaction(it) },
                    onDeleteTransactions = { viewModel.deleteTransactions(it) },
                    onQuickCreateAccount = { name, type, balance, currency, emoji, onCreated ->
                        viewModel.addAccount(name, type, balance, currency, emoji, onCreated)
                    },
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
                    twtUsdBalance = twtUsdBalance,
                    activity = activity,
                    securityManager = viewModel.securityManager,
                    onAddAccount = { name, type, balance, cur, emoji ->
                        viewModel.addAccount(name, type, balance, cur, emoji)
                    },
                    onUpdateAccount = { viewModel.updateAccount(it) },
                    onDeleteAccount = { viewModel.deleteAccount(it) }
                )
                2 -> CategoriesScreen(
                    customCategories = customCategories,
                    onAddCategory = { name, icon -> viewModel.addCustomCategory(name, icon) },
                    onDeleteCategory = { name -> viewModel.deleteCustomCategory(name) }
                )
                3 -> SettingsScreen(
                    currentThemeConfig = currentThemeConfig,
                    baseCurrency = baseCurrency,
                    currentLanguage = currentLanguage,
                    syncState = syncState,
                    accessSession = accessSession,
                    adminDesignSpecText = adminDesignSpecText,
                    isTwtVisibleEnabled = isTwtVisibleEnabled,
                    includeSensitiveIdsInSpec = includeSensitiveIdsInSpec,
                    isPinEnabled = isPinEnabled,
                    isBiometricEnabled = isBiometricEnabled,
                    activity = activity,
                    securityManager = viewModel.securityManager,
                    onThemeSelected = { viewModel.setTheme(it) },
                    onCurrencySelected = { viewModel.setBaseCurrency(it) },
                    onLanguageSelected = { viewModel.setLanguage(it) },
                    onOpenAuthGate = { showAuthGateSheet = true },
                    onToggleSync = { enabled ->
                        if (enabled && !syncState.isAuthenticated) {
                            showAuthGateSheet = true
                        } else {
                            viewModel.toggleSync(enabled)
                        }
                    },
                    onSyncNow = { viewModel.syncNow() },
                    onSignOut = {
                        val credentialManager = androidx.credentials.CredentialManager.create(context)
                        viewModel.signOutCloud(credentialManager)
                    },
                    onToggleTwtVisibility = { viewModel.setTwtVisibleEnabled(it) },
                    onToggleIncludeSensitiveIds = { viewModel.setIncludeSensitiveIdsInSpec(it) },
                    onTogglePin = { viewModel.togglePin(it) },
                    onSetPin = { viewModel.setPin(it) },
                    onToggleBiometric = { viewModel.toggleBiometric(it) },
                    onOpenExportImport = { showExportImportDialog = true },
                    onClearTransactions = { viewModel.clearAllTransactions() }
                )
            }
        }
    }

    // Account Wall / Multi-User Auth Gate Bottom Sheet
    if (showAuthGateSheet) {
        AuthGateBottomSheet(
            activity = activity,
            cloudSyncManager = viewModel.cloudSyncManager,
            currentLanguage = currentLanguage,
            onPasscodeLogin = { email, passcode -> viewModel.verifyPasscodeLogin(email, passcode) },
            onDismissRequest = { showAuthGateSheet = false },
            onAuthSuccess = {
                viewModel.onAuthSuccess()
            }
        )
    }

    // Flexible PDF / JSON / CSV Export & Import Dialog (role-filtered TWT isolation)
    if (showExportImportDialog) {
        ExportImportDialog(
            transactions = transactions,
            accounts = accounts,
            customCategories = customCategories,
            onExportJson = { viewModel.exportJson() },
            onExportCsv = { viewModel.exportCsv() },
            onImportJson = { json -> viewModel.importJson(json) },
            onDismissRequest = { showExportImportDialog = false }
        )
    }
}

