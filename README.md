# Telegram Electric

Telegram Electric is an Android app with a real TDLib connection path. CI builds the official TDLib Android JSON/JNI runtime from a pinned upstream commit, packages it into the debug APK, and the app supports Telegram authorization and group chat sync.

The app asks for the API ID and API hash from [my.telegram.org](https://my.telegram.org), then requests the phone number and the verification code inside the app. API credentials and the TDLib database key are encrypted with Android Keystore on the device. Do not paste login codes or account passwords into source control or chat.

The current alpha displays groups available to the signed-in account. It does not send messages, join chats, or collect group members. Messaging remains disabled until a separate user-confirmed send flow is implemented.

## Local build

Install Docker with Buildx, JDK 17, and Gradle 8.9. Run:

1. Build TDLib: ./scripts/build-tdlib-android.sh
2. Build the APK: gradle --no-daemon assembleDebug

The script builds TDLib commit a94b83d67ab4c8397c6245e541b31a4131ea2f79 and places its native libraries under app/src/main/jniLibs. GitHub Actions runs the same script and uploads a debug APK artifact. That APK is for development and is not a public release.
