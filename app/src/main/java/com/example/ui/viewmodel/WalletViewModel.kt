package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.WalletDatabase
import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.Currencies
import com.example.data.model.RecurringTransactionEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.TwtAccountHelper
import com.example.data.repository.WalletRepository
import com.example.data.sync.CloudSyncManager
import com.example.data.sync.SyncState
import com.example.security.AccessControlManager
import com.example.security.AccessSession
import com.example.security.PasscodeLoginResult
import com.example.security.SecurityManager
import com.example.ui.theme.ArtisanThemeConfig
import com.example.ui.theme.ArtisanThemes
import com.example.util.AppDesignSpecBuilder
import com.example.util.AppSpecSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WalletViewModel(application: Application) : AndroidViewModel(application) {

    private val database = WalletDatabase.getDatabase(application)
    private val repository = WalletRepository(database.walletDao(), application)
    val accessControlManager = AccessControlManager(application)
    val cloudSyncManager = CloudSyncManager(application, accessControlManager)
    val securityManager = SecurityManager(application)

    private val prefs = application.getSharedPreferences("balatec_settings_prefs", Context.MODE_PRIVATE)

    // Access Session (Primary Admin vs Secondary Shared User vs Signed Out)
    val accessSession: StateFlow<AccessSession> = accessControlManager.session

    // Themes
    private val _currentTheme = MutableStateFlow(
        ArtisanThemes.getThemeById(prefs.getString("theme_id", "espresso_brown") ?: "espresso_brown")
    )
    val currentTheme: StateFlow<ArtisanThemeConfig> = _currentTheme.asStateFlow()

    // Base Currency (default CFA for general wallet; TWT strictly uses USD)
    private val _baseCurrency = MutableStateFlow(
        prefs.getString("base_currency", "CFA").let {
            if (it.equals("FCFA", ignoreCase = true) || it.isNullOrBlank()) "CFA" else it
        }
    )
    val baseCurrency: StateFlow<String> = _baseCurrency.asStateFlow()

    // Language (English & French only - default French)
    private val _currentLanguage = MutableStateFlow(prefs.getString("language", "fr") ?: "fr")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    // Raw DB flows (private so UI never sees unisolated TWT data)
    private val rawAccountsFlow = repository.accountsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val rawTransactionsFlow = repository.transactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val rawRecurringFlow = repository.recurringTransactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Role-Filtered Visible Accounts (strict TWT exclusion unless Primary Admin)
    val accounts: StateFlow<List<AccountEntity>> = combine(
        rawAccountsFlow,
        accessSession
    ) { allAccounts, session ->
        if (session.isAdminTwtVisible) {
            allAccounts
        } else {
            allAccounts.filter { !TwtAccountHelper.isTwtAccount(it) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Role-Filtered Visible Transactions (strict TWT exclusion unless Primary Admin)
    val transactions: StateFlow<List<TransactionEntity>> = combine(
        rawTransactionsFlow,
        rawAccountsFlow,
        accessSession
    ) { allTx, allAccs, session ->
        if (session.isAdminTwtVisible) {
            allTx
        } else {
            val twtIds = allAccs.filter { TwtAccountHelper.isTwtAccount(it) }.map { it.id }.toSet()
            allTx.filter { tx -> tx.accountId !in twtIds }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Role-Filtered Visible Recurring Schedules
    val recurringTransactions: StateFlow<List<RecurringTransactionEntity>> = combine(
        rawRecurringFlow,
        rawAccountsFlow,
        accessSession
    ) { allRec, allAccs, session ->
        if (session.isAdminTwtVisible) {
            allRec
        } else {
            val twtIds = allAccs.filter { TwtAccountHelper.isTwtAccount(it) }.map { it.id }.toSet()
            allRec.filter { rec -> rec.accountId !in twtIds }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contacts: StateFlow<List<com.example.data.model.ContactEntity>> = repository.contactsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customCategories: StateFlow<List<Pair<String, String>>> = repository.customCategoriesFlow

    // Separate CFA vs TWT(USD) Metrics (never mix USD with CFA)
    val totalBalance: StateFlow<Double> = accounts.combine(transactions) { accs, _ ->
        accs.filter { !TwtAccountHelper.isTwtAccount(it) && Currencies.effectiveCurrencyCode(it) == "CFA" }
            .sumOf { it.balance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalIncome: StateFlow<Double> = combine(transactions, accounts) { txs, accs ->
        val accMap = accs.associateBy { it.id }
        txs.filter {
            it.type == TransactionType.INCOME &&
                !it.isDeleted &&
                !TwtAccountHelper.isTwtAccount(accMap[it.accountId]) &&
                Currencies.effectiveCurrencyCode(accMap[it.accountId], "CFA") == "CFA"
        }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense: StateFlow<Double> = combine(transactions, accounts) { txs, accs ->
        val accMap = accs.associateBy { it.id }
        txs.filter {
            it.type == TransactionType.EXPENSE &&
                !it.isDeleted &&
                !TwtAccountHelper.isTwtAccount(accMap[it.accountId]) &&
                Currencies.effectiveCurrencyCode(accMap[it.accountId], "CFA") == "CFA"
        }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Dedicated TWT USD ($) balance for Primary Admin hero display
    val twtUsdBalance: StateFlow<Double?> = combine(rawAccountsFlow, accessSession) { accs, session ->
        if (!session.isAdminTwtVisible) {
            null
        } else {
            val twtAccs = accs.filter { TwtAccountHelper.isTwtAccount(it) }
            if (twtAccs.isEmpty()) null else twtAccs.sumOf { it.balance }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Security Lock State
    private val _isLocked = MutableStateFlow(securityManager.isLocked)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _isPinEnabled = MutableStateFlow(securityManager.isPinLockEnabled())
    val isPinEnabled: StateFlow<Boolean> = _isPinEnabled.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(securityManager.isBiometricEnabled())
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    // Cloud Sync State
    val syncState: StateFlow<SyncState> = cloudSyncManager.syncState

    // Live auto-updating App Details & Design (.txt) for Primary Admin (410bala@gmail.com)
    private data class SettingsSpecState(
        val theme: ArtisanThemeConfig,
        val baseCurrency: String,
        val language: String,
        val isPinEnabled: Boolean,
        val isBiometricEnabled: Boolean
    )

    private data class DataSpecState(
        val accounts: List<AccountEntity>,
        val transactions: List<TransactionEntity>,
        val recurring: List<RecurringTransactionEntity>,
        val customCategories: List<Pair<String, String>>
    )

    private val settingsSpecFlow = combine(
        _currentTheme,
        _baseCurrency,
        _currentLanguage,
        _isPinEnabled,
        _isBiometricEnabled
    ) { theme, currency, lang, pin, bio ->
        SettingsSpecState(theme, currency, lang, pin, bio)
    }

    private val dataSpecFlow = combine(
        accounts,
        transactions,
        recurringTransactions,
        customCategories
    ) { accs, txs, recs, cats ->
        DataSpecState(accs, txs, recs, cats)
    }

    val adminDesignSpecText: StateFlow<String> = combine(
        accessSession,
        syncState,
        settingsSpecFlow,
        dataSpecFlow
    ) { session, sync, settings, data ->
        if (!AppDesignSpecBuilder.isEligibleForAdminSpec(session)) {
            ""
        } else {
            AppDesignSpecBuilder.buildText(
                AppSpecSnapshot(
                    accessSession = session,
                    syncState = sync,
                    currentTheme = settings.theme,
                    baseCurrency = settings.baseCurrency,
                    currentLanguage = settings.language,
                    isPinEnabled = settings.isPinEnabled,
                    isBiometricEnabled = settings.isBiometricEnabled,
                    accounts = data.accounts,
                    transactions = data.transactions,
                    recurringTransactions = data.recurring,
                    customCategories = data.customCategories
                )
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    init {
        viewModelScope.launch {
            if (!prefs.getBoolean("transactions_cleaned_v6", false)) {
                repository.clearAllTransactions()
                cloudSyncManager.clearAllTransactionsRemote()
                prefs.edit().putBoolean("transactions_cleaned_v6", true).apply()
            }
            // Migrate legacy TWT naming & FCFA labels cleanly
            repository.normalizeAccountsOnStartup()

            if (cloudSyncManager.syncState.value.isAuthenticated) {
                cloudSyncManager.startRealtimeSync(repository, viewModelScope)
                cloudSyncManager.performSync(repository)
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            adminDesignSpecText.collectLatest { specText ->
                AppDesignSpecBuilder.syncSpecFileOnDisk(
                    context = getApplication(),
                    session = accessSession.value,
                    specText = specText
                )
            }
        }
    }

    fun setTheme(themeConfig: ArtisanThemeConfig) {
        _currentTheme.value = themeConfig
        prefs.edit().putString("theme_id", themeConfig.id).apply()
    }

    fun setBaseCurrency(code: String) {
        val clean = if (code.equals("FCFA", ignoreCase = true)) "CFA" else code
        _baseCurrency.value = clean
        prefs.edit().putString("base_currency", clean).apply()
    }

    fun setLanguage(lang: String) {
        _currentLanguage.value = lang
        prefs.edit().putString("language", lang).apply()
    }

    // Security Actions
    fun unlockWithPin(pin: String): Boolean {
        val valid = securityManager.verifyPin(pin)
        if (valid) _isLocked.value = false
        return valid
    }

    fun unlockWithBiometric() {
        securityManager.unlockByBiometric()
        _isLocked.value = false
    }

    fun setPin(pin: String): Boolean {
        val ok = securityManager.setPin(pin)
        if (ok) {
            _isPinEnabled.value = true
            _isLocked.value = false
        }
        return ok
    }

    fun togglePin(enable: Boolean) {
        if (!enable) {
            securityManager.removePin()
            _isPinEnabled.value = false
            _isLocked.value = false
        }
    }

    fun toggleBiometric(enable: Boolean) {
        securityManager.setBiometricEnabled(enable)
        _isBiometricEnabled.value = enable
    }

    // Access Control (Passcode Switch / Login)
    fun verifyPasscodeLogin(email: String, passcode: String): PasscodeLoginResult {
        val result = accessControlManager.verifyAndSwitchWithPasscode(email, passcode)
        if (result is PasscodeLoginResult.Success && cloudSyncManager.syncState.value.isAuthenticated) {
            cloudSyncManager.startRealtimeSync(repository, viewModelScope)
        }
        return result
    }

    // Custom Categories
    fun addCustomCategory(name: String, icon: String) {
        repository.addCustomCategory(name, icon)
        triggerSync()
    }

    fun deleteCustomCategory(name: String) {
        repository.deleteCustomCategory(name)
        triggerSync()
    }

    // Account Actions
    fun addAccount(
        name: String,
        type: AccountType,
        balance: Double,
        currency: String,
        emoji: String,
        onCreated: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val rawAccount = AccountEntity(
                name = name,
                type = type,
                balance = balance,
                currencyCode = if (TwtAccountHelper.isTwtAccountName(name)) "USD" else currency,
                emojiAvatar = emoji
            )
            val normalized = TwtAccountHelper.normalizeTwtAccount(rawAccount)
            repository.addAccount(normalized)
            onCreated?.invoke(normalized.id)
            triggerSync()
        }
    }

    fun updateAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.updateAccount(account)
            triggerSync()
        }
    }

    fun deleteAccount(accountId: String) {
        viewModelScope.launch {
            repository.deleteAccount(accountId)
            triggerSync()
        }
    }

    // Transaction Actions
    fun addTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.addTransaction(transaction)
            triggerSync()
        }
    }

    fun updateTransaction(oldTx: TransactionEntity, newTx: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(oldTx, newTx)
            triggerSync()
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            val acc = rawAccountsFlow.value.firstOrNull { it.id == transaction.accountId }
            val isTwt = TwtAccountHelper.isTwtAccount(acc)
            repository.deleteTransaction(transaction)
            cloudSyncManager.deleteTransactionRemote(transaction.id, isTwtTransaction = isTwt)
            triggerSync()
        }
    }

    fun deleteTransactions(transactions: List<TransactionEntity>) {
        viewModelScope.launch {
            val accMap = rawAccountsFlow.value.associateBy { it.id }
            transactions.forEach { tx ->
                val isTwt = TwtAccountHelper.isTwtAccount(accMap[tx.accountId])
                repository.deleteTransaction(tx)
                cloudSyncManager.deleteTransactionRemote(tx.id, isTwtTransaction = isTwt)
            }
            triggerSync()
        }
    }

    fun clearAllTransactions() {
        viewModelScope.launch {
            repository.clearAllTransactions()
            cloudSyncManager.clearAllTransactionsRemote()
            triggerSync()
        }
    }

    // Recurring Transactions Management
    fun addRecurringTransaction(recurring: RecurringTransactionEntity) {
        viewModelScope.launch {
            repository.addRecurringTransaction(recurring)
            repository.processDueRecurringTransactions()
            triggerSync()
        }
    }

    fun updateRecurringTransaction(recurring: RecurringTransactionEntity) {
        viewModelScope.launch {
            repository.updateRecurringTransaction(recurring)
            triggerSync()
        }
    }

    fun deleteRecurringTransaction(id: String) {
        viewModelScope.launch {
            repository.deleteRecurringTransaction(id)
            triggerSync()
        }
    }

    fun toggleRecurringActive(id: String, isActive: Boolean) {
        viewModelScope.launch {
            repository.toggleRecurringTransactionActive(id, isActive)
            triggerSync()
        }
    }

    fun executeRecurringNow(recurringId: String) {
        viewModelScope.launch {
            repository.executeRecurringTransactionNow(recurringId)
            triggerSync()
        }
    }

    fun checkAndProcessDueRecurring() {
        viewModelScope.launch {
            val count = repository.processDueRecurringTransactions()
            if (count > 0) {
                triggerSync()
            }
        }
    }

    // Cloud Sync Operations
    fun onAuthSuccess() {
        viewModelScope.launch {
            cloudSyncManager.startRealtimeSync(repository, viewModelScope)
            cloudSyncManager.performSync(repository)
        }
    }

    fun signOutCloud(credentialManager: CredentialManager) {
        viewModelScope.launch {
            cloudSyncManager.signOut(credentialManager, viewModelScope)
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            cloudSyncManager.performSync(repository)
        }
    }

    private fun triggerSync() {
        viewModelScope.launch {
            if (syncState.value.isSyncEnabled && syncState.value.isAuthenticated) {
                cloudSyncManager.performSync(repository)
            }
        }
    }

    // Export & Import (role-scoped TWT exclusion)
    suspend fun exportJson(): String = repository.exportToJson(includeTwt = accessSession.value.isAdminTwtVisible)
    suspend fun exportCsv(): String = repository.exportToCsv(includeTwt = accessSession.value.isAdminTwtVisible)
    suspend fun importJson(json: String): Pair<Int, Int> {
        val res = repository.importFromJson(json, includeTwt = accessSession.value.isAdminTwtVisible)
        triggerSync()
        return res
    }
}
