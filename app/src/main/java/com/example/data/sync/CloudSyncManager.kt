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
import com.example.data.model.Currencies
import com.example.data.model.RecurrenceFrequency
import com.example.data.model.RecurringTransactionEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.TwtAccountHelper
import com.example.data.repository.WalletRepository
import com.example.security.AccessControlManager
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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

class CloudSyncManager(
    private val context: Context,
    private val accessControl: AccessControlManager = AccessControlManager(context),
    firestoreOverride: FirebaseFirestore? = null,
    authOverride: FirebaseAuth? = null
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("balatec_sync_prefs", Context.MODE_PRIVATE)

    private val firestore: FirebaseFirestore by lazy {
        firestoreOverride ?: run {
            val databaseId = try {
                context.getString(R.string.firestore_database_id)
            } catch (_: Exception) {
                "ai-studio-android-balatecs-4baf2e8b-4fe1-4d7a-a144-fd45aa8e45ca"
            }
            FirebaseFirestore.getInstance(databaseId)
        }
    }

    private val auth: FirebaseAuth by lazy { authOverride ?: Firebase.auth }

    private val listenerRegistrations = mutableListOf<ListenerRegistration>()

    private val _syncState = MutableStateFlow(
        SyncState(
            isSyncEnabled = prefs.getBoolean("is_sync_enabled", true),
            isAuthenticated = auth.currentUser != null && accessControl.isEmailAuthorized(auth.currentUser?.email),
            userEmail = auth.currentUser?.email,
            userName = auth.currentUser?.displayName ?: auth.currentUser?.email?.substringBefore("@"),
            status = if (auth.currentUser != null && accessControl.isEmailAuthorized(auth.currentUser?.email)) {
                SyncStatus.SYNCED
            } else {
                SyncStatus.LOCAL_ONLY
            },
            lastSyncTime = prefs.getLong("last_sync_time", 0L).takeIf { it > 0 }
        )
    )
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            val email = user?.email
            val authorized = user != null && accessControl.isEmailAuthorized(email)
            if (user != null && !authorized) {
                // Reject any email outside 410bala@gmail.com / traore200@gmail.com
                firebaseAuth.signOut()
                accessControl.clearSession()
                _syncState.value = SyncState(
                    isSyncEnabled = false,
                    isAuthenticated = false,
                    status = SyncStatus.ERROR,
                    statusMessage = "Compte non autorisé : $email"
                )
                return@addAuthStateListener
            }

            if (authorized && email != null) {
                accessControl.onGoogleAuthenticated(email)
            }

            val name = user?.displayName ?: email?.substringBefore("@")
            _syncState.value = _syncState.value.copy(
                isAuthenticated = authorized,
                userEmail = if (authorized) email else null,
                userName = if (authorized) name else null,
                status = if (authorized) SyncStatus.SYNCED else SyncStatus.LOCAL_ONLY,
                statusMessage = if (authorized) "Connecté en tant que $email" else "Mode hors-ligne local actif"
            )
        }
    }

    private fun resolveWebClientId(): String? {
        return try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) context.getString(resId) else null
        } catch (_: Exception) {
            null
        }
    }

    fun attemptAutoSignIn(
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onAuthSuccess: () -> Unit = {},
        onUnauthenticated: () -> Unit = {}
    ) {
        val current = auth.currentUser
        if (current != null && accessControl.isEmailAuthorized(current.email)) {
            accessControl.onGoogleAuthenticated(current.email)
            onAuthSuccess()
            return
        }

        val clientId = resolveWebClientId()
        if (clientId.isNullOrBlank()) {
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
                    val authResult = auth.signInWithCredential(authCredential).await()
                    val signedEmail = authResult.user?.email
                    if (accessControl.onGoogleAuthenticated(signedEmail)) {
                        enableSync()
                        onAuthSuccess()
                    } else {
                        auth.signOut()
                        onUnauthenticated()
                    }
                } else {
                    onUnauthenticated()
                }
            } catch (_: Exception) {
                onUnauthenticated()
            }
        }
    }

    fun signInWithGoogle(
        activity: Activity,
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancelled: () -> Unit = {}
    ) {
        val clientId = resolveWebClientId()
        if (clientId.isNullOrBlank()) {
            onError("Configuration Google Sign-In manquante (default_web_client_id).")
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
                    val authResult = auth.signInWithCredential(authCredential).await()
                    val signedEmail = authResult.user?.email
                    if (accessControl.onGoogleAuthenticated(signedEmail)) {
                        enableSync()
                        onSuccess()
                    } else {
                        try {
                            credentialManager.clearCredentialState(ClearCredentialStateRequest())
                        } catch (_: Exception) { }
                        auth.signOut()
                        onError("Accès refusé pour $signedEmail. Seuls ${AccessControlManager.ADMIN_EMAIL} et ${AccessControlManager.SECONDARY_EMAIL} sont autorisés.")
                    }
                } else {
                    onError("Type d'identifiant Google inattendu.")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w("Auth", "Google Sign-In cancelled", e)
                onCancelled()
            } catch (e: Exception) {
                Log.e("Auth", "Google Sign-In failed", e)
                onError(e.localizedMessage ?: "Échec de connexion Google")
            }
        }
    }

    fun startRealtimeSync(repository: WalletRepository, scope: CoroutineScope) {
        stopRealtimeSync()
        val user = auth.currentUser ?: return
        val email = user.email?.trim()?.lowercase() ?: return
        if (!accessControl.isEmailAuthorized(email)) return

        val sharedRoot = firestore.collection("shared_wallets").document(SHARED_WALLET_ID)

        listenerRegistrations += sharedRoot.collection("accounts").addSnapshotListener { snap, err ->
            if (err != null) {
                handleFirestoreError(err, OperationType.LIST, "shared_wallets/$SHARED_WALLET_ID/accounts")
                return@addSnapshotListener
            }
            val docs = snap?.documents ?: return@addSnapshotListener
            scope.launch(Dispatchers.IO) {
                val remoteAccounts = docs.mapNotNull { docToAccount(it.id, it.data) }
                    .filter { !TwtAccountHelper.isTwtAccount(it) }
                if (remoteAccounts.isNotEmpty()) {
                    repository.mergeRemoteData(remoteAccounts, emptyList())
                }
            }
        }

        listenerRegistrations += sharedRoot.collection("transactions").addSnapshotListener { snap, err ->
            if (err != null) {
                handleFirestoreError(err, OperationType.LIST, "shared_wallets/$SHARED_WALLET_ID/transactions")
                return@addSnapshotListener
            }
            val docs = snap?.documents ?: return@addSnapshotListener
            scope.launch(Dispatchers.IO) {
                val remoteTxs = docs.mapNotNull { docToTransaction(it.id, it.data) }
                if (remoteTxs.isNotEmpty()) {
                    repository.mergeRemoteData(emptyList(), remoteTxs)
                }
            }
        }

        listenerRegistrations += sharedRoot.collection("categories").addSnapshotListener { snap, err ->
            if (err != null) {
                handleFirestoreError(err, OperationType.LIST, "shared_wallets/$SHARED_WALLET_ID/categories")
                return@addSnapshotListener
            }
            val docs = snap?.documents ?: return@addSnapshotListener
            val cats = docs.mapNotNull { doc ->
                val name = doc.getString("name") ?: return@mapNotNull null
                val icon = doc.getString("icon") ?: "🏷️"
                name to icon
            }
            repository.mergeRemoteCategories(cats)
        }

        // Strict TWT Isolation: ONLY Primary Admin gets realtime TWT listeners
        if (email == AccessControlManager.ADMIN_EMAIL) {
            val twtRoot = firestore.collection("private_twt").document(ADMIN_TWT_DOC)
            listenerRegistrations += twtRoot.collection("accounts").addSnapshotListener { snap, err ->
                if (err != null) {
                    handleFirestoreError(err, OperationType.LIST, "private_twt/$ADMIN_TWT_DOC/accounts")
                    return@addSnapshotListener
                }
                val docs = snap?.documents ?: return@addSnapshotListener
                scope.launch(Dispatchers.IO) {
                    val twtAccounts = docs.mapNotNull { docToAccount(it.id, it.data) }
                        .map { TwtAccountHelper.normalizeTwtAccount(it) }
                    if (twtAccounts.isNotEmpty()) {
                        repository.mergeRemoteData(twtAccounts, emptyList())
                    }
                }
            }
            listenerRegistrations += twtRoot.collection("transactions").addSnapshotListener { snap, err ->
                if (err != null) {
                    handleFirestoreError(err, OperationType.LIST, "private_twt/$ADMIN_TWT_DOC/transactions")
                    return@addSnapshotListener
                }
                val docs = snap?.documents ?: return@addSnapshotListener
                scope.launch(Dispatchers.IO) {
                    val twtTxs = docs.mapNotNull { docToTransaction(it.id, it.data) }
                    if (twtTxs.isNotEmpty()) {
                        repository.mergeRemoteData(emptyList(), twtTxs)
                    }
                }
            }
        }
    }

    fun stopRealtimeSync() {
        listenerRegistrations.forEach { it.remove() }
        listenerRegistrations.clear()
    }

    suspend fun deleteTransactionRemote(id: String, isTwtTransaction: Boolean = false) {
        val user = auth.currentUser ?: return
        val email = user.email?.trim()?.lowercase() ?: return
        if (!accessControl.isEmailAuthorized(email)) return
        if (isTwtTransaction && email != AccessControlManager.ADMIN_EMAIL) return

        val path = if (isTwtTransaction) {
            "private_twt/$ADMIN_TWT_DOC/transactions/$id"
        } else {
            "shared_wallets/$SHARED_WALLET_ID/transactions/$id"
        }

        try {
            withContext(Dispatchers.IO) {
                firestore.document(path).delete().await()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
        }
    }

    suspend fun clearAllTransactionsRemote() {
        val user = auth.currentUser ?: return
        val email = user.email?.trim()?.lowercase() ?: return
        if (!accessControl.isEmailAuthorized(email)) return

        try {
            withContext(Dispatchers.IO) {
                val sharedRoot = firestore.collection("shared_wallets").document(SHARED_WALLET_ID)
                sharedRoot.collection("transactions").get().await().documents.forEach { it.reference.delete().await() }
                sharedRoot.collection("recurring").get().await().documents.forEach { it.reference.delete().await() }

                if (email == AccessControlManager.ADMIN_EMAIL) {
                    val twtRoot = firestore.collection("private_twt").document(ADMIN_TWT_DOC)
                    twtRoot.collection("transactions").get().await().documents.forEach { it.reference.delete().await() }
                    twtRoot.collection("recurring").get().await().documents.forEach { it.reference.delete().await() }
                }
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "shared_wallets/$SHARED_WALLET_ID")
        }
    }

    fun signOut(credentialManager: CredentialManager, scope: CoroutineScope, onComplete: () -> Unit = {}) {
        stopRealtimeSync()
        scope.launch {
            withContext(NonCancellable) {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w("Auth", "Failed to clear credential state: ${e.message}")
                } finally {
                    auth.signOut()
                    accessControl.clearSession()
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
        stopRealtimeSync()
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

    suspend fun performSync(repository: WalletRepository): String {
        val user = auth.currentUser
        val email = user?.email?.trim()?.lowercase()
        if (user == null || email == null || !accessControl.isEmailAuthorized(email) || !_syncState.value.isSyncEnabled) {
            return "Mode hors-ligne actif (base locale sécurisée)."
        }

        val isPrimaryAdmin = email == AccessControlManager.ADMIN_EMAIL
        _syncState.value = _syncState.value.copy(status = SyncStatus.SYNCING)

        return try {
            withContext(Dispatchers.IO) {
                val sharedRoot = firestore.collection("shared_wallets").document(SHARED_WALLET_ID)
                val sharedAccountsCol = sharedRoot.collection("accounts")
                val sharedTxCol = sharedRoot.collection("transactions")
                val sharedRecCol = sharedRoot.collection("recurring")
                val sharedCatCol = sharedRoot.collection("categories")

                val localAccounts = repository.getAccountsList()
                val localTwtAccounts = localAccounts.filter { TwtAccountHelper.isTwtAccount(it) }
                val twtAccountIds = localTwtAccounts.map { it.id }.toSet()
                val localSharedAccounts = localAccounts.filter { it.id !in twtAccountIds }

                // 1A. Sync Shared Non-TWT Accounts
                val remoteSharedAccSnap = sharedAccountsCol.get().await()
                val remoteSharedAccs = remoteSharedAccSnap.documents.mapNotNull { docToAccount(it.id, it.data) }
                    .filter { !TwtAccountHelper.isTwtAccount(it) }

                for (acc in localSharedAccounts) {
                    val map = accountToMap(acc, user.uid, isTwt = false)
                    sharedAccountsCol.document(acc.id).set(map).await()
                }

                // 1B. Sync Private TWT Accounts (Primary Admin ONLY)
                val remoteTwtAccs = mutableListOf<AccountEntity>()
                if (isPrimaryAdmin) {
                    val twtAccountsCol = firestore.collection("private_twt").document(ADMIN_TWT_DOC).collection("accounts")
                    val remoteTwtAccSnap = twtAccountsCol.get().await()
                    remoteTwtAccs.addAll(
                        remoteTwtAccSnap.documents.mapNotNull { docToAccount(it.id, it.data) }
                            .map { TwtAccountHelper.normalizeTwtAccount(it) }
                    )
                    for (acc in localTwtAccounts) {
                        val normalized = TwtAccountHelper.normalizeTwtAccount(acc)
                        val map = accountToMap(normalized, user.uid, isTwt = true)
                        twtAccountsCol.document(normalized.id).set(map).await()
                    }
                }

                // 2. Sync Deleted Transactions
                val deletedLocalTxs = repository.getDeletedTransactionsList()
                for (delTx in deletedLocalTxs) {
                    val isTwt = delTx.accountId in twtAccountIds
                    if (!isTwt) {
                        sharedTxCol.document(delTx.id).delete().await()
                        repository.hardDeleteTransaction(delTx.id)
                    } else if (isPrimaryAdmin) {
                        firestore.collection("private_twt").document(ADMIN_TWT_DOC)
                            .collection("transactions").document(delTx.id).delete().await()
                        repository.hardDeleteTransaction(delTx.id)
                    }
                }

                // 3. Sync Shared & Private Transactions
                val localTxList = repository.getTransactionsList()
                val accountsById = localAccounts.associateBy { it.id }

                val remoteSharedTxSnap = sharedTxCol.get().await()
                val remoteSharedTxs = remoteSharedTxSnap.documents.mapNotNull { docToTransaction(it.id, it.data) }

                for (tx in localTxList) {
                    val acc = accountsById[tx.accountId]
                    val isTwt = TwtAccountHelper.isTwtAccount(acc)
                    if (!isTwt) {
                        val cur = Currencies.effectiveCurrencyCode(acc, "CFA")
                        sharedTxCol.document(tx.id).set(transactionToMap(tx, cur, user.uid, isTwt = false)).await()
                    } else if (isPrimaryAdmin) {
                        firestore.collection("private_twt").document(ADMIN_TWT_DOC)
                            .collection("transactions").document(tx.id)
                            .set(transactionToMap(tx, "USD", user.uid, isTwt = true)).await()
                    }
                }

                val remoteTwtTxs = mutableListOf<TransactionEntity>()
                if (isPrimaryAdmin) {
                    val twtTxCol = firestore.collection("private_twt").document(ADMIN_TWT_DOC).collection("transactions")
                    val remoteTwtTxSnap = twtTxCol.get().await()
                    remoteTwtTxs.addAll(remoteTwtTxSnap.documents.mapNotNull { docToTransaction(it.id, it.data) })
                }

                repository.mergeRemoteData(
                    remoteAccounts = remoteSharedAccs + remoteTwtAccs,
                    remoteTransactions = remoteSharedTxs + remoteTwtTxs
                )

                // 4. Sync Shared Categories
                val localCategories = repository.customCategoriesFlow.value
                val remoteCatSnap = sharedCatCol.get().await()
                val remoteCats = remoteCatSnap.documents.mapNotNull { doc ->
                    val name = doc.getString("name") ?: return@mapNotNull null
                    val icon = doc.getString("icon") ?: "🏷️"
                    name to icon
                }
                for ((catName, catIcon) in localCategories) {
                    val docId = catName.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(64).ifBlank { "cat_${catName.hashCode()}" }
                    val catMap = hashMapOf<String, Any?>(
                        "id" to docId,
                        "name" to catName,
                        "icon" to catIcon,
                        "updatedBy" to user.uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ).filterValues { it != null }
                    sharedCatCol.document(docId).set(catMap).await()
                }
                repository.mergeRemoteCategories(remoteCats)

                // 5. Sync Recurring Schedules (shared vs private TWT)
                val localRecList = repository.getRecurringTransactionsList()
                val remoteRecSnap = sharedRecCol.get().await()
                for (rec in localRecList) {
                    val acc = accountsById[rec.accountId]
                    val isTwt = TwtAccountHelper.isTwtAccount(acc)
                    val cur = if (isTwt) "USD" else Currencies.effectiveCurrencyCode(acc, "CFA")
                    val targetCol = if (isTwt) {
                        if (!isPrimaryAdmin) continue
                        firestore.collection("private_twt").document(ADMIN_TWT_DOC).collection("recurring")
                    } else {
                        sharedRecCol
                    }
                    targetCol.document(rec.id).set(recurringToMap(rec, cur, user.uid)).await()
                }
                for (doc in remoteRecSnap.documents) {
                    if (localRecList.none { it.id == doc.id }) {
                        docToRecurring(doc.id, doc.data)?.let { repository.addRecurringTransaction(it) }
                    }
                }

                val now = System.currentTimeMillis()
                prefs.edit().putLong("last_sync_time", now).apply()

                _syncState.value = _syncState.value.copy(
                    status = SyncStatus.SYNCED,
                    lastSyncTime = now,
                    statusMessage = "Synchronisé ($email)"
                )
                "Synchronisation Firestore terminée."
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "shared_wallets/$SHARED_WALLET_ID")
            _syncState.value = _syncState.value.copy(
                status = SyncStatus.ERROR,
                statusMessage = "Erreur de synchronisation: ${e.localizedMessage}"
            )
            "Erreur lors de la synchronisation: ${e.localizedMessage}"
        }
    }

    private fun accountToMap(acc: AccountEntity, uid: String, isTwt: Boolean): Map<String, Any> {
        return hashMapOf<String, Any?>(
            "id" to acc.id,
            "name" to acc.name,
            "type" to acc.type.name,
            "balance" to acc.balance,
            "currencyCode" to if (isTwt) "USD" else Currencies.effectiveCurrencyCode(acc, "CFA"),
            "emojiAvatar" to acc.emojiAvatar,
            "sortOrder" to acc.sortOrder,
            "isTwt" to isTwt,
            "updatedBy" to uid,
            "clientUpdatedAt" to acc.updatedAt,
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }.mapValues { it.value!! }
    }

    private fun transactionToMap(tx: TransactionEntity, currencyCode: String, uid: String, isTwt: Boolean): Map<String, Any> {
        return hashMapOf<String, Any?>(
            "id" to tx.id,
            "accountId" to tx.accountId,
            "type" to tx.type.name,
            "amount" to tx.amount,
            "payee" to tx.payee,
            "category" to tx.category,
            "timestamp" to tx.timestamp,
            "notes" to tx.notes,
            "currencyCode" to currencyCode,
            "isTwt" to isTwt,
            "updatedBy" to uid,
            "clientUpdatedAt" to tx.updatedAt,
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }.mapValues { it.value!! }
    }

    private fun recurringToMap(rec: RecurringTransactionEntity, currencyCode: String, uid: String): Map<String, Any> {
        return hashMapOf<String, Any?>(
            "id" to rec.id,
            "accountId" to rec.accountId,
            "type" to rec.type.name,
            "amount" to rec.amount,
            "payee" to rec.payee,
            "category" to rec.category,
            "currencyCode" to currencyCode,
            "frequency" to rec.frequency.name,
            "startDate" to rec.startDate,
            "lastExecutedDate" to rec.lastExecutedDate,
            "nextDueDate" to rec.nextDueDate,
            "isActive" to rec.isActive,
            "notes" to rec.notes,
            "updatedBy" to uid,
            "clientUpdatedAt" to rec.updatedAt,
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }.mapValues { it.value!! }
    }

    private fun docToAccount(id: String, data: Map<String, Any>?): AccountEntity? {
        if (data == null) return null
        val name = (data["name"] as? String) ?: return null
        val typeStr = (data["type"] as? String) ?: AccountType.CUSTOM.name
        val type = runCatching { AccountType.valueOf(typeStr) }.getOrDefault(AccountType.CUSTOM)
        val balance = (data["balance"] as? Number)?.toDouble() ?: 0.0
        val cur = (data["currencyCode"] as? String) ?: "CFA"
        val emoji = (data["emojiAvatar"] as? String) ?: "💳"
        val sortOrder = (data["sortOrder"] as? Number)?.toInt() ?: 0
        val clientUpdatedAt = (data["clientUpdatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        return AccountEntity(
            id = id,
            name = name,
            type = type,
            balance = balance,
            currencyCode = cur,
            emojiAvatar = emoji,
            sortOrder = sortOrder,
            updatedAt = clientUpdatedAt
        )
    }

    private fun docToTransaction(id: String, data: Map<String, Any>?): TransactionEntity? {
        if (data == null) return null
        val amount = (data["amount"] as? Number)?.toDouble() ?: return null
        val typeStr = (data["type"] as? String) ?: TransactionType.EXPENSE.name
        val type = runCatching { TransactionType.valueOf(typeStr) }.getOrDefault(TransactionType.EXPENSE)
        val cat = (data["category"] as? String) ?: "Other"
        val accId = (data["accountId"] as? String) ?: ""
        val payee = (data["payee"] as? String) ?: ""
        val notes = (data["notes"] as? String) ?: ""
        val timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
        val clientUpdatedAt = (data["clientUpdatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        return TransactionEntity(
            id = id,
            accountId = accId,
            type = type,
            amount = amount,
            payee = payee,
            category = cat,
            timestamp = timestamp,
            notes = notes,
            updatedAt = clientUpdatedAt
        )
    }

    private fun docToRecurring(id: String, data: Map<String, Any>?): RecurringTransactionEntity? {
        if (data == null) return null
        val amount = (data["amount"] as? Number)?.toDouble() ?: return null
        val typeStr = (data["type"] as? String) ?: TransactionType.EXPENSE.name
        val type = runCatching { TransactionType.valueOf(typeStr) }.getOrDefault(TransactionType.EXPENSE)
        val cat = (data["category"] as? String) ?: "Other"
        val accId = (data["accountId"] as? String) ?: ""
        val payee = (data["payee"] as? String) ?: ""
        val freqStr = (data["frequency"] as? String) ?: RecurrenceFrequency.MONTHLY.name
        val freq = runCatching { RecurrenceFrequency.valueOf(freqStr) }.getOrDefault(RecurrenceFrequency.MONTHLY)
        val start = (data["startDate"] as? Number)?.toLong() ?: System.currentTimeMillis()
        val lastExec = (data["lastExecutedDate"] as? Number)?.toLong() ?: 0L
        val nextDue = (data["nextDueDate"] as? Number)?.toLong() ?: System.currentTimeMillis()
        val active = (data["isActive"] as? Boolean) ?: true
        val notes = (data["notes"] as? String) ?: ""
        val clientUpdatedAt = (data["clientUpdatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        return RecurringTransactionEntity(
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
            updatedAt = clientUpdatedAt
        )
    }

    companion object {
        const val SHARED_WALLET_ID = "balatec_shared"
        const val ADMIN_TWT_DOC = "admin_only"
    }
}
