# Setup

## 1. Get the app
Install JARVIS.apk from **Releases** (see README).

## 2a. Easiest: chat with your own Claude API key
JARVIS → Settings → paste your Claude API key (from console.anthropic.com) → **Save and test connection**.
The key is stored encrypted on your phone (Android Keystore) and is never in the code or on GitHub.
Trade-off: the key lives on your phone, so use a key with a spending limit and remove it if you lose the phone.
After JARVIS answers, it listens again for your follow-up, so you can talk back and forth.

## 2b. Safer: set up the AI backend (once, about 15 minutes, free tier)
You need: a free Cloudflare account, Node.js on a computer, and a Claude API key from console.anthropic.com.

```
cd backend
npm install -g wrangler
wrangler login
wrangler secret put ANTHROPIC_API_KEY     # paste your Claude key when asked
wrangler secret put APP_TOKEN             # invent a long random password and paste it
wrangler deploy
```
Wrangler prints your URL, like `https://jarvis-backend.YOURNAME.workers.dev`.

## 3. Connect the app
JARVIS → Settings → paste the URL and the APP_TOKEN → **Save and test connection**.

Optional: `wrangler secret put MODEL` to choose a Claude model, or set variable `WEB_SEARCH` to `off`.
Check the Claude API pricing page for current costs. Personal use is usually small.

## 4. Turn on extras
- **Notifications:** Settings → Notification access → turn on JARVIS.
- **Contacts, calls, SMS, photos:** JARVIS asks the first time you use each one.

## Build it yourself
Pushing to `main` triggers GitHub Actions (`.github/workflows/build.yml`), which builds the APK and publishes it under Releases.
To build in Android Studio: open this folder; if it asks for a Gradle version, choose 8.7.

## Security notes
- Never put your Claude API key or GitHub token in this repo or in the app.
- The app stores the backend token encrypted with the Android Keystore.
