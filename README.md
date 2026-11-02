# JARVIS: voice assistant for Android

A native Android app (Kotlin) you control by voice, in English and Nepali. Phone commands work offline.
Questions, news and research use Claude through a small backend that keeps your API key off the phone.

## Install the app (no coding needed)
1. On your phone open this repository → **Releases** → latest release.
2. Download **JARVIS.apk** and open it. Allow "Install unknown apps" for your browser when asked.
3. Open JARVIS, tap the mic, allow the microphone, and speak.

Each new build is a new release. If an update refuses to install, uninstall the old JARVIS first (builds are debug-signed).

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
- "Hey Jarvis" hands-free is **experimental** and only works while the app is open on screen. There is no background wake word.
- Nepali commands: only a few offline phrases. Everything else in Nepali goes through the AI backend. Nepali speech output needs a Nepali voice installed in Google Text-to-speech.
- Reading SMS needs the SMS permission. Google Play restricts this, which is fine for a personal APK.

See TESTING.md for what has and has not been tested.
