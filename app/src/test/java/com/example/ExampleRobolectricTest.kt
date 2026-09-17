package com.example

import android.content.Context
import android.telecom.Call
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.models.ActiveCallState
import com.example.ui.components.IncomingCallCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("R Dialer", appName)
  }

  @Test
  fun `incoming call card displays saved contact and answers`() {
    var answered = false
    var declined = false
    var fullScreenOpened = false

    val incomingState = ActiveCallState(
      number = "+91 98765 43210",
      contactName = "Runa Koroki",
      photoUri = null,
      telecomState = Call.STATE_RINGING,
      isIncoming = true
    )

    composeTestRule.setContent {
      IncomingCallCard(
        callState = incomingState,
        onAnswer = { answered = true },
        onDecline = { declined = true },
        onOpenFullScreen = { fullScreenOpened = true }
      )
    }

    composeTestRule.onNodeWithTag("incoming_call_card").assertIsDisplayed()
    composeTestRule.onNodeWithText("Runa Koroki").assertIsDisplayed()
    composeTestRule.onNodeWithText("+91 98765 43210").assertIsDisplayed()
    composeTestRule.onNodeWithTag("incoming_call_answer_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("incoming_call_decline_button").assertIsDisplayed()

    // Tap Answer
    composeTestRule.onNodeWithTag("incoming_call_answer_button").performClick()
    assertTrue("Answer should be invoked", answered)
  }

  @Test
  fun `incoming call card displays unknown caller and declines`() {
    var declined = false

    val unknownState = ActiveCallState(
      number = "+91 99999 88888",
      contactName = null,
      photoUri = null,
      telecomState = Call.STATE_RINGING,
      isIncoming = true
    )

    composeTestRule.setContent {
      IncomingCallCard(
        callState = unknownState,
        onAnswer = {},
        onDecline = { declined = true },
        onOpenFullScreen = {}
      )
    }

    composeTestRule.onNodeWithText("+91 99999 88888").assertIsDisplayed()
    composeTestRule.onNodeWithText("Unknown Caller").assertIsDisplayed()

    // Tap Decline
    composeTestRule.onNodeWithTag("incoming_call_decline_button").performClick()
    assertTrue("Decline should be invoked", declined)
  }
}
