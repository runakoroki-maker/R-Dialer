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

import com.example.ui.components.DeviceEnvironmentDialog
import com.example.ui.components.EmulatorLoginDetectedDialog
import com.example.util.EmulatorDetector

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
  fun `emulator detector evaluation and overrides`() {
    // Under Robolectric, Build.FINGERPRINT contains "robolectric" which triggers emulator detection
    val env = EmulatorDetector.refresh()
    assertTrue("Should detect virtual/emulator environment under Robolectric", env.isEmulator)
    assertEquals("Demo", env.callMode)

    // Verify manual override for physical device testing
    EmulatorDetector.setTestOverride(false)
    val physicalEnv = EmulatorDetector.environmentInfo.value
    assertEquals(false, physicalEnv.isEmulator)
    assertEquals("Real Device", physicalEnv.callMode)

    // Reset override
    EmulatorDetector.setTestOverride(null)
  }

  @Test
  fun `emulator login detected dialog displays and dismisses`() {
    var dismissed = false
    var openedDiagnostics = false

    composeTestRule.setContent {
      EmulatorLoginDetectedDialog(
        onDismiss = { dismissed = true },
        onViewDiagnostics = { openedDiagnostics = true }
      )
    }

    composeTestRule.onNodeWithTag("emulator_detected_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithText("R Dialer Emulator Login Detected").assertIsDisplayed()
    composeTestRule.onNodeWithText("OK").assertIsDisplayed()

    composeTestRule.onNodeWithTag("emulator_dialog_ok_button").performClick()
    assertTrue("Dialog OK button should trigger dismiss", dismissed)
  }

  @Test
  fun `device environment diagnostics dialog renders details`() {
    var closed = false
    val envInfo = EmulatorDetector.refresh()

    composeTestRule.setContent {
      DeviceEnvironmentDialog(
        environmentInfo = envInfo,
        onDismiss = { closed = true }
      )
    }

    composeTestRule.onNodeWithTag("device_environment_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithText("Device Environment").assertIsDisplayed()
    composeTestRule.onNodeWithTag("device_env_close_button").performClick()
    assertTrue("Close button should dismiss dialog", closed)
  }

  @Test
  fun `incoming call card displays demo badge in emulator mode`() {
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
        onAnswer = {},
        onDecline = {},
        onOpenFullScreen = {},
        isDemoModeOverride = true
      )
    }

    composeTestRule.onNodeWithTag("demo_incoming_call_badge").assertIsDisplayed()
    composeTestRule.onNodeWithText("DEMO INCOMING CALL").assertIsDisplayed()
    composeTestRule.onNodeWithText("EMULATOR").assertIsDisplayed()
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

  @Test
  fun `conference call and multi call state models`() {
    val participant1 = com.example.data.models.ConferenceParticipant(
      id = "part_1",
      displayName = "Runa Koroki",
      phoneNumber = "+1 555-0100",
      isHeld = false
    )
    val participant2 = com.example.data.models.ConferenceParticipant(
      id = "part_2",
      displayName = "Alex Vance",
      phoneNumber = "+1 555-0199",
      isHeld = false
    )

    val confState = ActiveCallState(
      number = "Conference",
      contactName = "Conference Call",
      isConference = true,
      conferenceParticipants = listOf(participant1, participant2),
      canMergeCalls = false,
      isVideoCall = false
    )

    assertTrue(confState.isConference)
    assertEquals(2, confState.conferenceParticipants.size)
    assertEquals("Runa Koroki", confState.conferenceParticipants[0].displayName)
    assertEquals("Alex Vance", confState.conferenceParticipants[1].displayName)
  }

  @Test
  fun `video call state and privacy toggling`() {
    val videoState = ActiveCallState(
      number = "+1 555-0155",
      contactName = "Dr. Elena",
      isVideoCall = true,
      isLocalCameraEnabled = false,
      videoStatusMessage = "Camera Off"
    )

    assertTrue(videoState.isVideoCall)
    assertEquals(false, videoState.isLocalCameraEnabled)
    assertEquals("Camera Off", videoState.videoStatusMessage)
  }

  @Test
  fun `contact item model with professional business name and address`() {
    val contact = com.example.data.models.ContactItem(
      id = 101L,
      lookupKey = "key_101",
      displayName = "Sarah Jenkins",
      phoneNumber = "+1 555-0122",
      businessName = "Starlight Technologies",
      address = "450 Innovation Way, Suite 300"
    )

    assertEquals("Sarah Jenkins", contact.displayName)
    assertEquals("Starlight Technologies", contact.businessName)
    assertEquals("450 Innovation Way, Suite 300", contact.address)
    assertEquals("S", contact.initial)
  }

  @Test
  fun `call hold and unhold state transitions`() {
    val activeCall = ActiveCallState(
      number = "+91 98765 43210",
      contactName = "Runa Koroki",
      telecomState = Call.STATE_ACTIVE,
      durationSeconds = 120L
    )
    assertEquals(Call.STATE_ACTIVE, activeCall.telecomState)

    // Transition to HOLDING
    val heldCall = activeCall.copy(telecomState = Call.STATE_HOLDING)
    assertEquals(Call.STATE_HOLDING, heldCall.telecomState)

    // Resume to ACTIVE
    val resumedCall = heldCall.copy(telecomState = Call.STATE_ACTIVE)
    assertEquals(Call.STATE_ACTIVE, resumedCall.telecomState)
  }

  @Test
  fun `multi-call state with primary held and secondary active`() {
    val secondCall = ActiveCallState(
      number = "+91 91234 56789",
      contactName = "Atik Hasan",
      telecomState = Call.STATE_ACTIVE,
      durationSeconds = 15L
    )

    val primaryCall = ActiveCallState(
      number = "+91 98765 43210",
      contactName = "Runa Koroki",
      telecomState = Call.STATE_HOLDING,
      durationSeconds = 222L,
      secondCall = secondCall,
      canMergeCalls = true,
      canSwapCalls = true
    )

    assertEquals(Call.STATE_HOLDING, primaryCall.telecomState)
    assertEquals("Runa Koroki", primaryCall.displayTitle)
    assertEquals(Call.STATE_ACTIVE, primaryCall.secondCall?.telecomState)
    assertEquals("Atik Hasan", primaryCall.secondCall?.displayTitle)
    assertTrue(primaryCall.canMergeCalls)
    assertTrue(primaryCall.canSwapCalls)
  }

  @Test
  fun `incoming call notification does not have duplicate answer actions`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val incomingState = ActiveCallState(
      number = "+91 98765 43210",
      contactName = "Runa Koroki",
      telecomState = Call.STATE_RINGING,
      isIncoming = true
    )

    com.example.telecom.CallNotificationManager.showIncomingCallNotification(context, incomingState)

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    val activeNotifications = notificationManager.activeNotifications
    val callNotif = activeNotifications.find { it.id == com.example.telecom.CallNotificationManager.NOTIFICATION_ID_CALL }?.notification
    org.junit.Assert.assertNotNull("Call notification should be posted", callNotif)

    // Check actions array on the built notification
    val actions = callNotif?.actions ?: emptyArray()
    val answerActions = actions.filter { it.title?.toString()?.equals("Answer", ignoreCase = true) == true }
    val declineActions = actions.filter { it.title?.toString()?.equals("Decline", ignoreCase = true) == true }

    // Under CallStyle on Android 12+, CallStyle manages actions and builder actions must not duplicate
    assertTrue("There should be at most 1 Answer action (never duplicated)", answerActions.size <= 1)
    assertTrue("There should be at most 1 Decline action (never duplicated)", declineActions.size <= 1)
  }
}
