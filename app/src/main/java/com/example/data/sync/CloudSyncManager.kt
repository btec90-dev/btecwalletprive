package com.example.data.sync

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.RecurrenceFrequency
import com.example.data.model.RecurringTransactionEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.repository.WalletRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

enum class SyncStatus {
    LOCAL_ONLY,
    SYNCING,
    SYNCED,
    ERROR
}

data class SyncState(
    val isSyncEnabled: Boolean = false,
    val isAuthenticated: Boolean = false,
    val userEmail: String? = null,
    val userName: String? = null,
    val status: SyncStatus = SyncStatus.LOCAL_ONLY,
    val lastSyncTime: Long? = null,
    val statusMessage: String? = null
)

class CloudSyncManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("balatec_sync_prefs", Context.MODE_PRIVATE)

    // Custom database instance strictly resolved from R.string.firestore_database_id
    private val firestore: FirebaseFirestore by lazy {
        val databaseId = try {
            context.getString(R.string.firestore_database_id)
        } catch (e: Exception) {
            "ai-studio-android-balatecs-4baf2e8b-4fe1-4d7a-a144-fd45aa8e45ca"
        }
        FirebaseFirestore.getInstance(databaseId)
    }

    private val auth: FirebaseAuth get() = Firebase.auth

    private val _syncState = MutableStateFlow(
        SyncState(
            isSyncEnabled = prefs.getBoolean("is_sync_enabled", true),
            isAuthenticated = auth.currentUser != null,
            userEmail = auth.currentUser?.email,
            userName = auth.currentUser?.displayName ?: auth.currentUser?.email?.substringBefore("@"),
            status = if (auth.currentUser != null) SyncStatus.SYNCED else SyncStatus.LOCAL_ONLY,
            lastSyncTime = prefs.getLong("last_sync_time", 0L).takeIf { it > 0 }
        )
    )
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    init {
        // Observe real Firebase Authentication state changes
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            val isAuth = user != null
            val email = user?.email
            val name = user?.displayName ?: email?.substringBefore("@")

            _syncState.value = _syncState.value.copy(
                isAuthenticated = isAuth,
                userEmail = email,
                userName = name,
                status = if (isAuth) SyncStatus.SYNCED else SyncStatus.LOCAL_ONLY,
                statusMessage = if (isAuth) "Connecté en tant que $email" else "Mode hors-ligne local actif"
            )
        }
    }

    /**
     * Silent auto sign-in at application start via GetGoogleIdOption
     */
    fun attemptAutoSignIn(
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onAuthSuccess: () -> Unit = {},
        onUnauthenticated: () -> Unit = {}
    ) {
        if (auth.currentUser != null) {
            onAuthSuccess()
            return
        }

        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            onUnauthenticated()
            return
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    auth.signInWithCredential(authCredential).await()
                    onAuthSuccess()
                } else {
                    onUnauthenticated()
                }
            } catch (e: Exception) {
                onUnauthenticated()
            }
        }
    }

    /**
     * Interactive Google Sign-In via GetSignInWithGoogleOption
     */
    fun signInWithGoogle(
        activity: Activity,
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancelled: () -> Unit = {}
    ) {
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            onError("Configuration Google Sign-In introuvable: default_web_client_id manquant")
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    auth.signInWithCredential(authCredential).await()
                    onSuccess()
                } else {
                    onError("Type de justificatif inattendu")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
                onCancelled()
            } catch (e: Exception) {
                Log.e("Auth", "Google Sign-In failed", e)
                onError(e.localizedMessage ?: "Échec de connexion")
            }
        }
    }

    /**
     * Complete sign-out clearing CredentialManager before signing out of Firebase Auth
     */
    fun signOut(credentialManager: CredentialManager, scope: CoroutineScope, onComplete: () -> Unit = {}) {
        scope.launch {
            withContext(NonCancellable) {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w("Auth", "Failed to clear credential state: ${e.message}")
                } finally {
                    auth.signOut()
                    prefs.edit()
                        .remove("user_email")
                        .remove("user_name")
                        .apply()
                    _syncState.value = SyncState(
                        isSyncEnabled = false,
                        isAuthenticated = false,
                        userEmail = null,
                        userName = null,
                        status = SyncStatus.LOCAL_ONLY,
                        lastSyncTime = null,
                        statusMessage = "Déconnecté. Mode hors-ligne local actif."
                    )
                    onComplete()
                }
            }
        }
    }

    fun disableSync() {
        prefs.edit().putBoolean("is_sync_enabled", false).apply()
        _syncState.value = _syncState.value.copy(
            isSyncEnabled = false,
            status = SyncStatus.LOCAL_ONLY
        )
    }

    fun enableSync() {
        prefs.edit().putBoolean("is_sync_enabled", true).apply()
        _syncState.value = _syncState.value.copy(isSyncEnabled = true)
    }

    /**
     * Executes real cloud synchronization with Cloud Firestore
     * scoped to authenticated user (/users/{uid}/...)
     */
    suspend fun performSync(repository: WalletRepository): String {
        val user = auth.currentUser
        if (user == null || !_syncState.value.isSyncEnabled) {
            return "Mode hors-ligne actif (base locale sécurisée)."
        }

        val uid = user.uid
        _syncState.value = _syncState.value.copy(status = SyncStatus.SYNCING)

        return try {
            withContext(Dispatchers.IO) {
                val userDoc = firestore.collection("users").document(uid)
                val accountsCol = userDoc.collection("accounts")
                val transactionsCol = userDoc.collection("transactions")
                val recurringCol = userDoc.collection("recurring")

                // 1. Sync Accounts
                val localAccounts = repository.getAccountsList()
                val remoteAccountsSnap = accountsCol.get().await()

                // Push local accounts to Firestore
                for (acc in localAccounts) {
                    val map = hashMapOf(
                        "name" to acc.name,
                        "type" to acc.type.name,
                        "balance" to acc.balance,
                        "currencyCode" to acc.currencyCode,
                        "emojiAvatar" to acc.emojiAvatar,
                        "sortOrder" to acc.sortOrder,
                        "updatedAt" to acc.updatedAt
                    )
                    accountsCol.document(acc.id).set(map).await()
                }

                // Pull remote accounts from Firestore into Room
                for (doc in remoteAccountsSnap.documents) {
                    val id = doc.id
                    if (localAccounts.none { it.id == id }) {
                        val name = doc.getString("name") ?: "Compte"
                        val typeStr = doc.getString("type") ?: AccountType.CUSTOM.name
                        val type = runCatching { AccountType.valueOf(typeStr) }.getOrDefault(AccountType.CUSTOM)
                        val balance = doc.getDouble("balance") ?: 0.0
                        val cur = doc.getString("currencyCode") ?: "CFA"
                        val emoji = doc.getString("emojiAvatar") ?: "💳"
                        val sortOrder = doc.getLong("sortOrder")?.toInt() ?: 0
                        val updated = doc.getLong("updatedAt") ?: System.currentTimeMillis()

                        repository.addAccount(
                            AccountEntity(
                                id = id,
                                name = name,
                                type = type,
                                balance = balance,
                                currencyCode = cur,
                                emojiAvatar = emoji,
                                sortOrder = sortOrder,
                                updatedAt = updated
                            )
                        )
                    }
                }

                // 2. Sync Transactions
                val localTxList = repository.getTransactionsList()
                val remoteTxSnap = transactionsCol.get().await()

                // Push local transactions to Firestore
                for (tx in localTxList) {
                    val map = hashMapOf(
                        "accountId" to tx.accountId,
                        "type" to tx.type.name,
                        "amount" to tx.amount,
                        "payee" to tx.payee,
                        "category" to tx.category,
                        "timestamp" to tx.timestamp,
                        "notes" to tx.notes,
                        "updatedAt" to tx.updatedAt
                    )
                    transactionsCol.document(tx.id).set(map).await()
                }

                // Pull remote transactions into Room
                for (doc in remoteTxSnap.documents) {
                    val id = doc.id
                    if (localTxList.none { it.id == id }) {
                        val amount = doc.getDouble("amount") ?: 0.0
                        val typeStr = doc.getString("type") ?: TransactionType.EXPENSE.name
                        val type = runCatching { TransactionType.valueOf(typeStr) }.getOrDefault(TransactionType.EXPENSE)
                        val cat = doc.getString("category") ?: "Divers"
                        val accId = doc.getString("accountId") ?: ""
                        val payee = doc.getString("payee") ?: ""
                        val notes = doc.getString("notes") ?: ""
                        val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        val updated = doc.getLong("updatedAt") ?: System.currentTimeMillis()

                        repository.addTransaction(
                            TransactionEntity(
                                id = id,
                                accountId = accId,
                                type = type,
                                amount = amount,
                                payee = payee,
                                category = cat,
                                timestamp = timestamp,
                                notes = notes,
                                updatedAt = updated
                            )
                        )
                    }
                }

                // 3. Sync Recurring Schedules
                val localRecList = repository.getRecurringTransactionsList()
                val remoteRecSnap = recurringCol.get().await()

                for (rec in localRecList) {
                    val map = hashMapOf(
                        "accountId" to rec.accountId,
                        "type" to rec.type.name,
                        "amount" to rec.amount,
                        "payee" to rec.payee,
                        "category" to rec.category,
                        "frequency" to rec.frequency.name,
                        "startDate" to rec.startDate,
                        "lastExecutedDate" to rec.lastExecutedDate,
                        "nextDueDate" to rec.nextDueDate,
                        "isActive" to rec.isActive,
                        "notes" to rec.notes,
                        "updatedAt" to rec.updatedAt
                    )
                    recurringCol.document(rec.id).set(map).await()
                }

                for (doc in remoteRecSnap.documents) {
                    val id = doc.id
                    if (localRecList.none { it.id == id }) {
                        val amount = doc.getDouble("amount") ?: 0.0
                        val typeStr = doc.getString("type") ?: TransactionType.EXPENSE.name
                        val type = runCatching { TransactionType.valueOf(typeStr) }.getOrDefault(TransactionType.EXPENSE)
                        val cat = doc.getString("category") ?: "Divers"
                        val accId = doc.getString("accountId") ?: ""
                        val payee = doc.getString("payee") ?: ""
                        val freqStr = doc.getString("frequency") ?: RecurrenceFrequency.MONTHLY.name
                        val freq = runCatching { RecurrenceFrequency.valueOf(freqStr) }.getOrDefault(RecurrenceFrequency.MONTHLY)
                        val start = doc.getLong("startDate") ?: System.currentTimeMillis()
                        val lastExec = doc.getLong("lastExecutedDate") ?: 0L
                        val nextDue = doc.getLong("nextDueDate") ?: System.currentTimeMillis()
                        val active = doc.getBoolean("isActive") ?: true
                        val notes = doc.getString("notes") ?: ""
                        val updated = doc.getLong("updatedAt") ?: System.currentTimeMillis()

                        repository.addRecurringTransaction(
                            RecurringTransactionEntity(
                                id = id,
                                accountId = accId,
                                type = type,
                                amount = amount,
                                payee = payee,
                                category = cat,
                                frequency = freq,
                                startDate = start,
                                lastExecutedDate = lastExec,
                                nextDueDate = nextDue,
                                isActive = active,
                                notes = notes,
                                updatedAt = updated
                            )
                        )
                    }
                }

                val now = System.currentTimeMillis()
                prefs.edit().putLong("last_sync_time", now).apply()

                _syncState.value = _syncState.value.copy(
                    status = SyncStatus.SYNCED,
                    lastSyncTime = now,
                    statusMessage = "Synchronisation Firestore réussie (${localAccounts.size} comptes, ${localTxList.size} transactions)"
                )
                "Synchronisation Firestore terminée avec succès."
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid")
            _syncState.value = _syncState.value.copy(
                status = SyncStatus.ERROR,
                statusMessage = "Erreur de synchronisation: ${e.localizedMessage}"
            )
            "Erreur lors de la synchronisation: ${e.localizedMessage}"
        }
    }
}
