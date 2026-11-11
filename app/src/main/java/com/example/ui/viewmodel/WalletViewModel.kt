package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.WalletDatabase
import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.TransactionEntity
import com.example.data.repository.WalletRepository
import com.example.data.sync.CloudSyncManager
import com.example.data.sync.SyncState
import com.example.security.SecurityManager
import com.example.ui.theme.ArtisanThemeConfig
import com.example.ui.theme.ArtisanThemes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WalletViewModel(application: Application) : AndroidViewModel(application) {

    private val database = WalletDatabase.getDatabase(application)
    private val repository = WalletRepository(database.walletDao())
    val cloudSyncManager = CloudSyncManager(application)
    val securityManager = SecurityManager(application)

    private val prefs = application.getSharedPreferences("balatec_settings_prefs", Context.MODE_PRIVATE)

    // Themes
    private val _currentTheme = MutableStateFlow(
        ArtisanThemes.getThemeById(prefs.getString("theme_id", "brown_leather") ?: "brown_leather")
    )
    val currentTheme: StateFlow<ArtisanThemeConfig> = _currentTheme.asStateFlow()

    // Currency
    private val _baseCurrency = MutableStateFlow(prefs.getString("base_currency", "USD") ?: "USD")
    val baseCurrency: StateFlow<String> = _baseCurrency.asStateFlow()

    // Language (English & French only - default French)
    private val _currentLanguage = MutableStateFlow(prefs.getString("language", "fr") ?: "fr")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    val accounts: StateFlow<List<AccountEntity>> = repository.accountsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<TransactionEntity>> = repository.transactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contacts: StateFlow<List<com.example.data.model.ContactEntity>> = repository.contactsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurringTransactions: StateFlow<List<com.example.data.model.RecurringTransactionEntity>> = repository.recurringTransactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.processDueRecurringTransactions()
        }
    }

    // Metrics
    val totalBalance: StateFlow<Double> = accounts.combine(transactions) { accs, _ ->
        accs.sumOf { it.balance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalIncome: StateFlow<Double> = transactions.combine(accounts) { txs, _ ->
        txs.filter { it.type == com.example.data.model.TransactionType.INCOME && !it.isDeleted }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense: StateFlow<Double> = transactions.combine(accounts) { txs, _ ->
        txs.filter { it.type == com.example.data.model.TransactionType.EXPENSE && !it.isDeleted }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Security Lock State
    private val _isLocked = MutableStateFlow(securityManager.isLocked)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _isPinEnabled = MutableStateFlow(securityManager.isPinLockEnabled())
    val isPinEnabled: StateFlow<Boolean> = _isPinEnabled.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(securityManager.isBiometricEnabled())
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    // Cloud Sync State
    val syncState: StateFlow<SyncState> = cloudSyncManager.syncState

    fun setTheme(themeConfig: ArtisanThemeConfig) {
        _currentTheme.value = themeConfig
        prefs.edit().putString("theme_id", themeConfig.id).apply()
    }

    fun setBaseCurrency(code: String) {
        _baseCurrency.value = code
        prefs.edit().putString("base_currency", code).apply()
    }

    fun setLanguage(lang: String) {
        _currentLanguage.value = lang
        prefs.edit().putString("language", lang).apply()
    }

    // Security Actions
    fun unlockWithPin(pin: String): Boolean {
        val valid = securityManager.verifyPin(pin)
        if (valid) {
            _isLocked.value = false
        }
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

    // Account Actions
    fun addAccount(name: String, type: AccountType, balance: Double, currency: String, emoji: String) {
        viewModelScope.launch {
            val account = AccountEntity(
                name = name,
                type = type,
                balance = balance,
                currencyCode = currency,
                emojiAvatar = emoji
            )
            repository.addAccount(account)
            // Trigger auto-sync if enabled
            if (syncState.value.isSyncEnabled) {
                cloudSyncManager.performSync(repository)
            }
        }
    }

    fun updateAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.updateAccount(account)
            if (syncState.value.isSyncEnabled) {
                cloudSyncManager.performSync(repository)
            }
        }
    }

    fun deleteAccount(accountId: String) {
        viewModelScope.launch {
            repository.deleteAccount(accountId)
            if (syncState.value.isSyncEnabled) {
                cloudSyncManager.performSync(repository)
            }
        }
    }

    // Transaction Actions
    fun addTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.addTransaction(transaction)
            if (syncState.value.isSyncEnabled) {
                cloudSyncManager.performSync(repository)
            }
        }
    }

    fun updateTransaction(oldTx: TransactionEntity, newTx: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(oldTx, newTx)
            if (syncState.value.isSyncEnabled) {
                cloudSyncManager.performSync(repository)
            }
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            if (syncState.value.isSyncEnabled) {
                cloudSyncManager.performSync(repository)
            }
        }
    }

    fun deleteTransactions(transactions: List<TransactionEntity>) {
        viewModelScope.launch {
            transactions.forEach { repository.deleteTransaction(it) }
            if (syncState.value.isSyncEnabled) {
                cloudSyncManager.performSync(repository)
            }
        }
    }

    fun clearAllTransactions() {
        viewModelScope.launch {
            repository.clearAllTransactions()
            if (syncState.value.isSyncEnabled) {
                cloudSyncManager.performSync(repository)
            }
        }
    }

    // Recurring Transactions Management
    fun addRecurringTransaction(recurring: com.example.data.model.RecurringTransactionEntity) {
        viewModelScope.launch {
            repository.addRecurringTransaction(recurring)
            repository.processDueRecurringTransactions()
        }
    }

    fun updateRecurringTransaction(recurring: com.example.data.model.RecurringTransactionEntity) {
        viewModelScope.launch {
            repository.updateRecurringTransaction(recurring)
        }
    }

    fun deleteRecurringTransaction(id: String) {
        viewModelScope.launch {
            repository.deleteRecurringTransaction(id)
        }
    }

    fun toggleRecurringActive(id: String, isActive: Boolean) {
        viewModelScope.launch {
            repository.toggleRecurringTransactionActive(id, isActive)
        }
    }

    fun executeRecurringNow(recurringId: String) {
        viewModelScope.launch {
            repository.executeRecurringTransactionNow(recurringId)
        }
    }

    fun checkAndProcessDueRecurring() {
        viewModelScope.launch {
            repository.processDueRecurringTransactions()
        }
    }

    // Cloud Sync Operations
    fun onAuthSuccess() {
        viewModelScope.launch {
            cloudSyncManager.performSync(repository)
        }
    }

    fun signOutCloud(credentialManager: androidx.credentials.CredentialManager) {
        cloudSyncManager.signOut(credentialManager, viewModelScope)
    }

    fun syncNow() {
        viewModelScope.launch {
            cloudSyncManager.performSync(repository)
        }
    }

    // Export & Import
    suspend fun exportJson(): String = repository.exportToJson()
    suspend fun exportCsv(): String = repository.exportToCsv()
    suspend fun importJson(json: String): Pair<Int, Int> = repository.importFromJson(json)
}
