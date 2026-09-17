package com.example

import com.example.data.models.ActiveCallState
import com.example.data.models.CallLogEntry
import com.example.data.models.CallType
import com.example.data.models.ContactItem
import com.example.telecom.TelecomHelper
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testContactInitialExtraction() {
    val contact = ContactItem(id = 1, displayName = "Runa Koroki", phoneNumber = "+919876543210")
    assertEquals("R", contact.initial)

    val contactEmpty = ContactItem(id = 2, displayName = "", phoneNumber = "12345")
    assertEquals("?", contactEmpty.initial)
  }

  @Test
  fun testDurationFormatting() {
    assertEquals("00:08", TelecomHelper.formatDuration(8))
    assertEquals("01:42", TelecomHelper.formatDuration(102))
    assertEquals("1:00:15", TelecomHelper.formatDuration(3615))
  }

  @Test
  fun testActiveCallStateDisplayTitle() {
    val knownCall = ActiveCallState(
      number = "+919876543210",
      contactName = "Runa Koroki",
      telecomState = 4 // STATE_ACTIVE
    )
    assertEquals("Runa Koroki", knownCall.displayTitle)
    assertEquals("R", knownCall.initial)

    val unknownCall = ActiveCallState(
      number = "+15550199",
      contactName = null,
      telecomState = 2 // STATE_RINGING
    )
    assertEquals("+15550199", unknownCall.displayTitle)
    assertEquals("?", unknownCall.initial)
  }

  @Test
  fun testCallLogEntryProperties() {
    val entry = CallLogEntry(
      id = 10,
      number = "+919876543210",
      cachedName = "Atik Hasan",
      type = CallType.OUTGOING,
      timestamp = System.currentTimeMillis(),
      durationSeconds = 45
    )
    assertEquals("Atik Hasan", entry.displayName)
    assertEquals("A", entry.initial)
  }
}
