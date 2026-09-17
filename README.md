# R Dialer

A modern Android dialer focused on simple, clean, and reliable phone calling.

R Dialer provides a clean, minimal white/light interface for day-to-day telephony with real Android Telecom framework integration, contact recognition, call history, and multitasking support.

---

## Phase 1 Features

* **Real Phone Calls**: Direct dialing and cellular calling via Android Telecom framework and system call intents.
* **Incoming Calls**: Real incoming call detection with answer and decline controls.
* **Outgoing Calls**: Modern dialer keypad with contact matching as you type, tactile/tone feedback, and one-tap calling.
* **Contacts Integration**: Real device contact querying with instant search and letter-based or photo avatars.
* **Call History**: Real device call log inspection with incoming, outgoing, and missed call badges and durations.
* **Contact Avatars**: Consistent 3-tier avatar system across the entire application (Profile photo → Name initial → Unknown caller placeholder).
* **Runtime Permissions**: Transparent, standard Android permission flows for Contacts, Call Logs, and Phone state.
* **Android Telecom Integration**: Full implementation of `InCallService` to manage active call lifecycle and audio routes.
* **Default Phone App Support**: Built-in prompt to register R Dialer as the system's default dialer using official Android `RoleManager` APIs.
* **Multitasking-Friendly Call Experience**: When a call arrives while using YouTube, Chrome, or other apps, high-priority heads-up notifications allow answering or declining without force-closing foreground tasks.

---

## Technical Specifications

* **Minimum Android Version**: Android 8.0 (API level 26)
* **Target SDK**: Android 16 (API level 36)
* **Language**: Kotlin 2.2+
* **UI Toolkit**: Jetpack Compose with Material Design 3 (M3)
* **Architecture**: MVVM (Model-View-ViewModel) + Clean Architecture
* **Asynchronous Operations**: Kotlin Coroutines & StateFlow

---

## Required Permissions

| Permission | Purpose |
|---|---|
| `android.permission.CALL_PHONE` | Initiates cellular phone calls directly from the dialer |
| `android.permission.READ_PHONE_STATE` | Monitors call states and cellular connection status |
| `android.permission.READ_CONTACTS` | Reads local device contacts to match numbers with names and photos |
| `android.permission.WRITE_CONTACTS` | Supports contact management operations |
| `android.permission.READ_CALL_LOG` | Reads recent call logs (incoming, outgoing, missed) |
| `android.permission.WRITE_CALL_LOG` | Allows clearing or managing call log entries |
| `android.permission.BIND_INCALL_SERVICE` | Connects with the Android Telecom subsystem to manage in-call states |
| `android.permission.POST_NOTIFICATIONS` | Displays heads-up incoming call and ongoing call notifications (Android 13+) |
| `android.permission.USE_FULL_SCREEN_INTENT` | Displays full-screen incoming call UI when device screen is locked |
| `android.permission.VIBRATE` | Provides tactile feedback when tapping keypad digits and during calls |
| `android.permission.MODIFY_AUDIO_SETTINGS` | Toggles speakerphone and earpiece audio routing during active calls |

---

## How to Build

### Prerequisites
* Android Studio Ladybug (or newer)
* JDK 17 or JDK 21
* Android SDK 36

### Build via Command Line
```bash
# Debug build
gradle :app:assembleDebug

# Run unit and Robolectric tests
gradle :app:testDebugUnitTest
```

---

## How to Set R Dialer as Default Phone App

1. Launch **R Dialer**.
2. If R Dialer is not yet the default phone app, a prompt banner appears at the top of the dialer screen: *"Set as default phone app for full in-call UI"*.
3. Tap **Enable**.
4. In the Android system dialog that appears, select **R Dialer** and tap **Set as default**.

*Alternative (Manual System Settings):*
1. Open device **Settings** → **Apps** → **Default apps** (or **Advanced** → **Default apps**).
2. Tap **Phone app**.
3. Select **R Dialer**.

---

## Known Android / OEM Limitations

* **Call Recording**: Third-party call recording is restricted on modern Android versions (Android 9+) by Google security policy; R Dialer strictly avoids call recording to maintain user privacy.
* **Background Start Restrictions**: On certain aggressive OEM battery savers (e.g. Xiaomi MIUI/HyperOS, Huawei EMUI, Samsung OneUI background limits), users should ensure "Autostart" or "Ignore battery optimizations" is enabled to allow high-priority incoming call alerts when the screen is locked.
* **Dual SIM Selection**: Default Android Telecom handles SIM selection based on the user's preferred calling SIM configured in system settings. Multi-SIM selection prompts are subject to OEM carrier settings.

---

## Roadmap

* **Phase 1 (Current)**: Core Android Dialer, Telecom Framework, Contacts, Call History, Avatars, Multitasking UX.
* **Phase 2**: Real-time Caller ID & Directory Lookup.
* **Phase 3**: Offline & Community Spam Detection, Call Blocking.
* **Phase 4**: Advanced Personalization, Custom Dial Tones, and Audio Enhancements.
