# Testing status (be honest about what is verified)

## Verified
- (updated after the first CI build)

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
