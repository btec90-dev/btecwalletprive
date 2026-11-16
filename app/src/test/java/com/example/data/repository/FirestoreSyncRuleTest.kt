package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FirestoreSyncRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun authorizedSharedUsers_shareNonTwtAccountsAndTransactions() {
        runBlocking {
            signInTestUser("410bala@gmail.com")
            val sharedAccRef = firestore.collection("shared_wallets")
                .document("balatec_shared")
                .collection("accounts")
                .document("acc_taptap")

            val accountData = hashMapOf(
                "id" to "acc_taptap",
                "name" to "Tap Tap Send",
                "type" to "CUSTOM",
                "balance" to 150000.0,
                "currencyCode" to "CFA",
                "emojiAvatar" to "📲",
                "sortOrder" to 0,
                "isTwt" to false,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            withTimeout(5000L) {
                sharedAccRef.set(accountData).await()
            }

            // Switch to secondary authorized user
            signInTestUser("traore200@gmail.com")
            val snapshot = withTimeout(5000L) {
                sharedAccRef.get().await()
            }

            assertTrue(snapshot.exists())
            assertEquals("Tap Tap Send", snapshot.getString("name"))
            assertEquals("CFA", snapshot.getString("currencyCode"))
            assertEquals(150000.0, snapshot.getDouble("balance") ?: 0.0, 0.001)

            // Secondary user can write non-TWT transaction in shared wallet
            val sharedTxRef = firestore.collection("shared_wallets")
                .document("balatec_shared")
                .collection("transactions")
                .document("tx_shared_1")
            withTimeout(5000L) {
                sharedTxRef.set(
                    hashMapOf(
                        "id" to "tx_shared_1",
                        "accountId" to "acc_taptap",
                        "type" to "EXPENSE",
                        "amount" to 25000.0,
                        "payee" to "Drissa Traoré",
                        "category" to "Famille",
                        "timestamp" to System.currentTimeMillis(),
                        "currencyCode" to "CFA",
                        "isTwt" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }
    }

    @Test
    fun secondaryUser_cannotAccessPrivateTwtCollection() {
        runBlocking {
            signInTestUser("410bala@gmail.com")
            val twtDoc = firestore.collection("private_twt")
                .document("admin_only")
                .collection("accounts")
                .document("acc_twt_2026")

            withTimeout(5000L) {
                twtDoc.set(
                    hashMapOf(
                        "id" to "acc_twt_2026",
                        "name" to "TWT 2026",
                        "type" to "SAVINGS",
                        "balance" to 850.0,
                        "currencyCode" to "USD",
                        "emojiAvatar" to "🪙",
                        "isTwt" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // Switch to secondary authorized user
            signInTestUser("traore200@gmail.com")

            try {
                withTimeout(5000L) {
                    twtDoc.get().await()
                }
                fail("Expected PERMISSION_DENIED when secondary user attempts to read private_twt")
            } catch (e: Exception) {
                val root = e.unwrapFirestoreException()
                assertNotNull(root)
                assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, root?.code)
            }
        }
    }

    @Test
    fun outsiderEmail_isDeniedSharedAndPrivateAccess() {
        runBlocking {
            signInTestUser("outsider@example.com")
            val sharedAccRef = firestore.collection("shared_wallets")
                .document("balatec_shared")
                .collection("accounts")
                .document("acc_sneak")

            try {
                withTimeout(5000L) {
                    sharedAccRef.get().await()
                }
                fail("Expected PERMISSION_DENIED for unauthorized email")
            } catch (e: Exception) {
                val root = e.unwrapFirestoreException()
                assertNotNull(root)
                assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, root?.code)
            }
        }
    }
}
