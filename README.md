# Delhi Metro Pink Line - Passenger Assistance Alert System

An Android application for Delhi Metro Pink Line operations (Majlis Park to Shiv Vihar), providing real-time passenger assistance requests, multi-device cloud synchronization, station-specific audible alarms, and station lifecycle tracking.

---

## 🎯 Inter-Station Alert Flow (Example: Anand Vihar to Trilokpuri Sanjay Lake)

1. **Mobile 1 (Logged in as Anand Vihar Station)**:
   - Station staff creates an alert for a passenger (e.g., Wheelchair, Coach C1) traveling to **Trilokpuri Sanjay Lake**.
   - Upon submitting, **no alarm sounds on Anand Vihar**. Anand Vihar sees the status: *"Waiting for Trilokpuri to acknowledge..."*.

2. **Mobile 2 (Logged in as Trilokpuri Sanjay Lake Station)**:
   - **Immediately triggers a high-intensity dual-tone siren, device vibration, and Text-to-Speech voice alert**:
     > *"Attention Trilokpuri station staff! Incoming special assistance passenger from Anand Vihar. Category: Wheelchair, Coach: C1. Please acknowledge alert!"*
   - Trilokpuri staff taps **"Acknowledge Alert (Stop Alarm)"**.
   - The siren stops and both phones update in real time to **"Staff Acknowledged"**.

---

## 📱 Pre-built APK Download

The compiled APK is included directly in this repository:
- **[`PinkLineAlert.apk`](./PinkLineAlert.apk)** (root directory)
- **[`apk/PinkLineAlert.apk`](./apk/PinkLineAlert.apk)**
- **[`PinkLineAlert_APK_Only.zip`](./PinkLineAlert_APK_Only.zip)**

---

## 🛠️ Tech Stack & Architecture
- **Language**: Kotlin
- **UI Toolkit**: Jetpack Compose with Material 3 (M3)
- **Architecture**: MVVM with Kotlin Coroutines & Flow
- **Local Persistence**: Room Database
- **Inter-Device Cloud Sync**: Real-time cloud synchronization service (`CloudSyncService`)
- **Audio & Haptics**: Native AudioTrack tone synthesizer (960 Hz / 770 Hz) + Android Vibrator
- **TTS Engine**: Android TextToSpeech for automated station announcements
