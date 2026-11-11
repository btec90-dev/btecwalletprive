package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
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
    fun authenticatedUser_canCreateAndReadAccount() = runBlocking {
        val aliceUid = signInTestUser("alice@balatec.com")
        val accountsCol = firestore.collection("users").document(aliceUid).collection("accounts")

        val accountData = hashMapOf(
            "name" to "Taptap Send",
            "type" to "CUSTOM",
            "balance" to 150000.0,
            "currencyCode" to "CFA",
            "emojiAvatar" to "📲",
            "sortOrder" to 0,
            "updatedAt" to System.currentTimeMillis()
        )

        val docRef = accountsCol.document("acc_taptap")
        withTimeout(5000L) {
            docRef.set(accountData).await()
        }

        val snapshot = withTimeout(5000L) {
            docRef.get().await()
        }

        assertTrue(snapshot.exists())
        assertEquals("Taptap Send", snapshot.getString("name"))
        assertEquals("CFA", snapshot.getString("currencyCode"))
        assertEquals(150000.0, snapshot.getDouble("balance") ?: 0.0, 0.001)
    }

    @Test
    fun crossUserAccess_isDenied() = runBlocking {
        val aliceUid = signInTestUser("alice_owner@balatec.com")
        val aliceDoc = firestore.collection("users").document(aliceUid).collection("accounts").document("secret_acc")

        aliceDoc.set(
            hashMapOf(
                "name" to "Alice Vault",
                "type" to "SAVINGS",
                "balance" to 999999.0,
                "currencyCode" to "CFA"
            )
        ).await()

        // Switch to Bob
        signInTestUser("bob_attacker@balatec.com")

        try {
            withTimeout(3000L) {
                firestore.collection("users").document(aliceUid).collection("accounts").document("secret_acc").get().await()
            }
            fail("Expected PERMISSION_DENIED when Bob attempts to read Alice's account")
        } catch (e: Exception) {
            val root = e.unwrapFirestoreException()
            assertNotNull(root)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, root?.code)
        }
    }

    @Test
    fun unauthenticatedAccess_isDenied() = runBlocking {
        val aliceUid = signInTestUser("alice_temp@balatec.com")
        val docRef = firestore.collection("users").document(aliceUid).collection("transactions").document("tx_1")

        // Sign out
        auth.signOut()

        try {
            withTimeout(3000L) {
                docRef.set(
                    hashMapOf(
                        "amount" to 5000.0,
                        "currencyCode" to "CFA",
                        "type" to "EXPENSE",
                        "category" to "Famille",
                        "accountId" to "acc1",
                        "timestamp" to System.currentTimeMillis()
                    )
                ).await()
            }
            fail("Expected PERMISSION_DENIED when unauthenticated")
        } catch (e: Exception) {
            val root = e.unwrapFirestoreException()
            assertNotNull(root)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, root?.code)
        }
    }
}
