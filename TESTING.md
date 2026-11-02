# Testing status (be honest about what is verified)

## Verified
- The project compiles and builds a debug APK on GitHub Actions (Kotlin 1.9.24, AGP 8.5.2, Gradle 8.7). Compilation is the only thing verified so far.

## NOT yet tested on a real phone (Samsung Galaxy F22)
Everything below was written carefully against the official Android APIs but has not been run on a device:
speech recognition (English and Nepali), text-to-speech, contact search and ambiguity dialog, calling, SMS and WhatsApp hand-off,
app launching, alarms/timers/reminders, flashlight, volume, notification reading, SMS reading, photo search,
payment hand-off, hands-free "Hey Jarvis", AI backend round trip, offline behaviour, denied-permission flows.

## Known limitations
- No background wake word. Hands-free works only with the app open.
- Cannot cancel alarms or list/cancel reminders yet.
- WhatsApp number assumes Nepal (+977) for 10-digit local numbers.
- Offline Nepali commands are very limited.
- Debug-signed APK: uninstall before installing a newer build if Android refuses to update.

## How to report a bug
Tell Claude exactly what you said, what JARVIS answered, and what you expected.
