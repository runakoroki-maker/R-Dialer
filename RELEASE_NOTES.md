# R Dialer v1.1.1 — Release Notes

**Status:** Ready for Owner Review (Awaiting manual review & publication approval)  
**Target Release Tag:** `v1.1.1`  
**Version Code:** `3`  
**Version Name:** `1.1.1`  
**Release Artifact:** `R-Dialer-1.1.1.apk` (`/app/build/outputs/apk/release/R-Dialer-1.1.1.apk`)

---

### Critical Fix in v1.1.1: Incoming Call Action Duplication

#### Problem Resolved
* In previous builds, when an incoming call arrived while the user was interacting with another application (or on the home screen), the system incoming-call notification displayed **three action buttons** (`Answer`, `Answer`, `Decline`).
* **Root Cause:** `NotificationCompat.CallStyle.forIncomingCall` automatically handles and renders standard Answer and Decline action buttons via Android's CallStyle engine. Redundant `.addAction(...)` invocations on the `NotificationCompat.Builder` caused a second `Answer` action to be appended to the notification action list.

#### Changes Implemented
* **Eliminated Redundant Action Registration:** Removed manual `.addAction(...)` calls from `CallNotificationManager.kt`. The incoming call notification now relies exclusively on `NotificationCompat.CallStyle.forIncomingCall(callerPerson, declinePendingIntent, answerPendingIntent)`.
* **Guaranteed 2-Action Model Across All Surfaces:**
  | Surface / State | Actions Displayed | Underlying Call Logic |
  | --------------- | ----------------- | --------------------- |
  | **Notification (Other App Open)** | 🟢 Answer • 🔴 Decline | Answers / Rejects incoming Telecom call |
  | **Compact Heads-Up Popup** | 🔴 Decline • 🟢 Answer | Answers / Rejects incoming Telecom call |
  | **Lock Screen UI** | 🟢 Answer • 🔴 Decline | Answers / Rejects incoming Telecom call |
  | **Full-Screen In-Call UI** | 🟢 Answer • 🔴 Decline | Answers / Rejects incoming Telecom call |
  | **R Dialer App Open** | 🟢 Answer • 🔴 Decline | Answers / Rejects incoming Telecom call |
* **Telecom Integrity Preserved:** Exactly one logical `answerPendingIntent` (launching `InCallActivity` with `ACTION_ANSWER_CALL` to call `CallManager.answer()`) and one logical `declinePendingIntent` (dispatching `CallActionReceiver` with `ACTION_DECLINE` to call `CallManager.disconnect()`).
* **Comprehensive Test Coverage:** Added unit test `incoming call notification does not have duplicate answer actions` to assert no duplicate actions exist on the notification.

---

### Previous Highlights (from v1.1.0)
* **Real Call Hold & Resume:** Telecom API-driven `Call.hold()` and `Call.unhold()`.
* **Multi-Call & Conference:** Swap calls, merge calls, add call flow, and conference participant list.
* **Premium Frosted Glass UI:** 3×2 grid layout with white frosted glass styling.
* **Emulator & Real Device Detection:** "R Dialer Emulator Login Detected" system preserved.

---

### Release Artifact Details
* **File:** `R-Dialer-1.1.1.apk`
* **Size:** ~17 MB
* **Build Variant:** Release
* **Application ID:** `com.aistudio.rdialer.mbfx`
* **Min SDK:** Android 8.0 (API 26)
* **Target SDK:** Android 16 (API 36)
* **Test Status:** 100% unit tests passing (`ExampleRobolectricTest`).

---

### Owner Review Checklist
- [ ] Source code verification (`CallNotificationManager.kt`, `InCallScreen.kt`, `IncomingCallCard.kt`)
- [ ] Physical device notification test (Receive call while in YouTube/Chrome: verify only 1 Answer + 1 Decline)
- [ ] Physical device lock-screen test (Verify 1 Answer + 1 Decline)
- [ ] Physical device compact popup test (Verify 1 Answer + 1 Decline)
- [ ] Release APK installation verification (`R-Dialer-1.1.1.apk`)
- [ ] Owner approval for GitHub release publication
