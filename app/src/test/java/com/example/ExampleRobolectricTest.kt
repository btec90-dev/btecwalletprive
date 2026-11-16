package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
}
