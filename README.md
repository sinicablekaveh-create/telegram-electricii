# Telegram Electric

Telegram Electric is an Android app scaffold. The current alpha does **not** include TDLib's Java/JNI runtime, account authorization, a live chat list, or message delivery. The UI and core guards fail closed so this build cannot report a fake connection or successful send.

See [the architecture notes](docs/TELEGRAM_ELECTRIC_ARCHITECTURE.md) for the connection prerequisites and the next implementation steps.

## Build pipeline

GitHub Actions runs the Android checks and uploads a debug APK for development. That APK is not a functioning Telegram client and is not a public release.
