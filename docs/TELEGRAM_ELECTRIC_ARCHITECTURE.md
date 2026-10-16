# Telegram Electric: connection and safety

## Telegram connection

- The app uses the official TDLib JSON/JNI interface. The Android build script pins upstream TDLib to commit a94b83d67ab4c8397c6245e541b31a4131ea2f79.
- CI builds the native libraries for Android ABIs with Android NDK 28.2.13676358 and OpenSSL 3.6.5, then puts them in the APK. NDK r28 aligns shared libraries for 16 KB page-size devices. Generated native binaries are not committed to the repository.
- The user enters their Telegram API ID and API hash from my.telegram.org in the app. Android Keystore encrypts those values and the random TDLib database key at rest. The app does not request the login code or 2-step verification password outside the app.
- Authorization follows TDLib update states for phone number, login code, 2-step verification password, and email verification.
- After authorization, the app loads the signed-in account's main and archived chat lists and displays groups visible to that account. It does not join chats or scrape members.

## Messaging safety

The current alpha does not send messages. The existing send screen and engine remain disabled. A later send feature must show the exact selected chats and message text and require a separate explicit confirmation before sending. It must respect Telegram limits, provide a stop control, and pause for review when delivery is uncertain.

## Build and verification

GitHub Actions runs unit tests, builds the pinned TDLib runtime, assembles a debug APK, and uploads it as a development artifact. Local Android builds must run scripts/build-tdlib-android.sh before Gradle assembles the APK.
