# Android Permissions & Privacy Guide — R Dialer

R Dialer is an offline-first, privacy-focused telephony application. No contact records, call histories, or audio signals are ever transmitted over the network or saved to external servers.

## Permission Declarations

### 1. Telephony & Calling
* **`android.permission.CALL_PHONE`**
  * *Type*: Dangerous / Runtime
  * *Justification*: Required to initiate cellular voice phone calls without displaying an extra intermediate system dialer prompt.
* **`android.permission.READ_PHONE_STATE`**
  * *Type*: Dangerous / Runtime
  * *Justification*: Necessary for detecting incoming phone calls, cellular carrier state, and connection status.
* **`android.permission.BIND_INCALL_SERVICE`**
  * *Type*: Signature / System Permission
  * *Justification*: Required for R Dialer to implement `InCallService` and present the official in-call user interface when set as the default dialer.

### 2. Contacts & Address Book
* **`android.permission.READ_CONTACTS`**
  * *Type*: Dangerous / Runtime
  * *Justification*: Required to display the user's contacts list, perform instant contact search, and resolve unknown incoming phone numbers to known names and contact photos.
* **`android.permission.WRITE_CONTACTS`**
  * *Type*: Dangerous / Runtime
  * *Justification*: Prepares R Dialer for future contact creation and editing capabilities.

### 3. Call History
* **`android.permission.READ_CALL_LOG`**
  * *Type*: Dangerous / Runtime
  * *Justification*: Reads the device's call log history to present incoming, outgoing, and missed call logs with accurate timestamps and call durations.
* **`android.permission.WRITE_CALL_LOG`**
  * *Type*: Dangerous / Runtime
  * *Justification*: Allows management of call records if requested by the user.

### 4. Audio & Hardware Controls
* **`android.permission.MODIFY_AUDIO_SETTINGS`**
  * *Type*: Normal / Install-time
  * *Justification*: Switches call audio between the earpiece and speakerphone.
* **`android.permission.VIBRATE`**
  * *Type*: Normal / Install-time
  * *Justification*: Delivers haptic feedback during dial pad touches and incoming call ring alerts.
* **`android.permission.POST_NOTIFICATIONS`**
  * *Type*: Dangerous / Runtime (Android 13+)
  * *Justification*: Displays the incoming call heads-up banner and persistent in-call duration notifications.
* **`android.permission.USE_FULL_SCREEN_INTENT`**
  * *Type*: Normal (Special App Access)
  * *Justification*: Allows the in-call screen to wake and display over the lockscreen when a phone call is received while the device is sleeping.
