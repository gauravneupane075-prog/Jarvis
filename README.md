# JARVIS: voice assistant for Android

A native Android app (Kotlin) you control by voice. English and Nepali are supported; choose the speech-recognition language in Settings. Common Romanized Nepali and mixed English/Nepali phone commands are also recognized by the offline command parser.

## Install the app
1. Open the repository's **Actions** tab and select the latest successful **Build JARVIS APK** run.
2. Download the **JARVIS-debug-apk** artifact and extract `JARVIS-debug.apk`.
3. Transfer it to your Android phone, tap it, and allow your file manager to install unknown apps if Android asks.
4. Open JARVIS and grant only the permissions you want to use.

The debug APK is signed with Android's standard debug certificate so it can be installed for personal testing. It is not a Play Store release. If you install a different build signed with another key, Android may require you to uninstall the previous version first.

## Security and privacy
- Android backup is disabled for this app, and cleartext HTTP traffic is blocked. Configure your AI backend with an HTTPS URL.
- Small secrets are encrypted using Android Keystore-backed AES-GCM storage.
- Runtime permissions are requested by the app; deny any permission for features you do not want to use.
- Phone commands run locally where possible. AI questions are sent to the configured AI provider/backend.
- Never share your APK signing key, API key, or backend token. A debug build is for personal testing; Play Store distribution requires a protected release signing setup and Play Console review.
- Google Play Protect may still display a warning for a sideloaded or unfamiliar APK. This project does not disable, bypass, or suppress Play Protect, and no developer can guarantee a clean verdict without Google's scanning.

## What works (no backend needed)
Call a contact · prepare an SMS · prepare a WhatsApp message · open any app · search Google (opens browser) ·
alarms and timers · reminders (calendar) · flashlight · volume · battery · device info · time/date ·
read latest SMS · read notifications · find photos/videos by day · open maps · open Bluetooth/Wi-Fi/location/brightness settings ·
open a payment app after you confirm.

## What needs Claude (API key in Settings, or your backend)
Conversation, questions, news, weather, research, and natural phrasing ("could you maybe ring my brother"). JARVIS keeps listening for your follow-up after each answer. Also open links, email drafts and play music. See **SETUP.md**.

## Honest limits (Android rules, not bugs)
- JARVIS **cannot** send SMS/WhatsApp or pay silently. It opens the app and **you press send / approve**.
- Bluetooth/Wi-Fi cannot be toggled by apps on modern Android. JARVIS opens the settings page.
- "Hey Jarvis" hands-free is **experimental** and only works while the app is open on screen. There is no background wake word or microphone foreground service yet. Do not expect it to listen after you leave the app or restart the phone.
- Speech recognition uses the language selected in Settings (English or Nepali), not automatic live language detection. Common Romanized Nepali/mixed commands are parsed offline, but recognition accuracy depends on the installed speech service. Nepali speech output needs a Nepali voice installed in Google Text-to-speech.
- Reading SMS needs the SMS permission. Google Play restricts this, which is fine for a personal APK.

See TESTING.md for what has and has not been tested.
