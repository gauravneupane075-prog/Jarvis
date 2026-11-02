# Testing status (be honest about what is verified)

## Repository review
- Reviewed the Android manifest, app module configuration, voice recognition, speech output, command parser, activity lifecycle, permissions, executor, settings, README and setup instructions.
- Added offline command parsing for a small set of common Romanized Nepali and mixed English/Nepali phrases.
- Updated the documentation to distinguish language selection from automatic language detection and to state the current background-listening limitation.

## Verified
- The repository's existing GitHub Actions workflow is configured to build a debug APK. A successful build is not claimed for this feature branch until its workflow completes.
- No physical-device test has been performed as part of this update.

## NOT yet tested on a real phone (Samsung Galaxy F22, Android 13)
Speech recognition (English, Nepali and mixed phrases), text-to-speech, contact search and ambiguity dialog, calling, SMS and WhatsApp hand-off,
app launching, alarms/timers/reminders, flashlight, volume, notification reading, SMS reading, photo search,
payment hand-off, AI backend round trip, offline behaviour, denied-permission flows and battery impact.

## Known limitations / not implemented
- No background microphone service and no reliable always-on wake word. Hands-free works only while the app is open.
- Speech recognition uses one selected language at a time; automatic English/Nepali language switching is not implemented.
- Android does not allow an ordinary app to control every other app or silently operate protected features. Payment approval, sending messages, passwords and protected settings remain user-controlled.
- Cannot cancel alarms or list/cancel reminders yet.
- WhatsApp number assumes Nepal (+977) for 10-digit local numbers.
- Nepali voice output requires a Nepali TTS voice installed on the phone.
- Debug-signed APK: uninstall before installing a newer build if Android refuses to update.

## Suggested device test checklist
1. In Settings, select English; try: "open YouTube", "call [contact]", "turn on flashlight", and "search Google for weather".
2. Select Nepali; try recognized Nepali phrases supported by the installed speech service.
3. Try Romanized phrases: "YouTube khola", "mero battery kati cha", "awaj badhau", "torch bala", "photo dekha".
4. Deny a permission and confirm JARVIS explains the missing permission without crashing.
5. Turn hands-free on, leave JARVIS visible, and confirm it stops listening when the app is backgrounded.

## How to report a bug
Tell Claude exactly what you said, what JARVIS answered, what you expected, and attach the Android version plus a screenshot/log if possible.
