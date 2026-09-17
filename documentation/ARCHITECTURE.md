# Architecture Overview — R Dialer (Phase 1)

R Dialer is built following Google's official Android application architecture guidelines with MVVM (Model-View-ViewModel), unidirectional data flow (UDF), and Jetpack Compose.

## 1. High-Level Architecture

```text
┌────────────────────────────────────────────────────────┐
│                        UI Layer                        │
│   DialerScreen       RecentsScreen      ContactsScreen │
│            \               |               /           │
│             InCallScreen (InCallActivity)              │
└───────────────────────────┬────────────────────────────┘
                            │ StateFlow / Events
┌───────────────────────────▼────────────────────────────┐
│                    ViewModel Layer                     │
│    DialerViewModel   RecentsViewModel   ContactsVM     │
└───────────────────────────┬────────────────────────────┘
                            │ Coroutines
┌───────────────────────────▼────────────────────────────┐
│                    Repository Layer                    │
│      ContactRepository         CallLogRepository       │
└─────────────┬───────────────────────────┬──────────────┘
              │                           │
┌─────────────▼───────────────┐ ┌─────────▼──────────────┐
│  Contacts Provider (Device) │ │   CallLog Provider     │
└─────────────────────────────┘ └────────────────────────┘
              │
┌─────────────▼──────────────────────────────────────────┐
│                    Telecom Subsystem                   │
│   RDialerInCallService ──> CallManager (Singleton)     │
│   CallNotificationManager (Heads-Up Multitasking)      │
└────────────────────────────────────────────────────────┘
```

## 2. Core Components

### Telecom Integration (`com.example.telecom`)
- **`RDialerInCallService`**: Extends Android's `InCallService`. Binds to the OS when R Dialer is the default phone app. Captures incoming and outgoing calls and forwards state updates to `CallManager`.
- **`CallManager`**: Singleton coordinator holding the active `android.telecom.Call` instance. Tracks state (RINGING, DIALING, ACTIVE, HOLDING, DISCONNECTED), manages audio routing (Speaker vs. Earpiece), microphone mute state, and DTMF tones.
- **`CallNotificationManager`**: Coordinates high-priority Heads-Up Notifications (HUN) for incoming calls with Answer and Decline actions, plus ongoing call status notifications with duration timers.
- **`TelecomHelper`**: Manages default dialer checks, `RoleManager` requests on Android 10+ (API 29+), and fallback call initiation.

### Data Layer (`com.example.data`)
- **`ContactRepository`**: Queries `ContactsContract.CommonDataKinds.Phone` and `ContactsContract.PhoneLookup`. Provides fast asynchronous phone number lookup for incoming caller recognition and search queries.
- **`CallLogRepository`**: Queries `android.provider.CallLog.Calls` for real cellular call history, parsing durations, timestamps, and call types.

### UI Layer (`com.example.ui`)
- **`MainActivity`**: Hosts the bottom navigation with tabs: **Dialer**, **Recents**, and **Contacts**. Shows a floating active call banner when a call continues in the background.
- **`InCallActivity`**: Dedicated lightweight activity for the incoming and active call experience with full-screen intent support and lockscreen dismissal flags.
- **`ContactAvatar`**: Reusable avatar component adhering to the 3-tier identity priority: Photo URI → First letter initial → Unknown icon.
- **`DialerKeypad`**: Custom keypad with haptic feedback, DTMF audio tone generation, and long-press zero support.
