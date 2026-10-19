# J.A.R.V.I.S. Android

An Android-native starter app inspired by Chi-King J.A.R.V.I.S., with Gemini chat, spoken replies, Android speech recognition, contacts picker, and email compose integration.

## Build the APK on GitHub using your phone
1. Open the **Actions** tab in this repository.
2. Select **Build J.A.R.V.I.S. Android APK**.
3. Tap **Run workflow** (or wait for a build after a commit).
4. Open the completed workflow run and download the `jarvis-android-debug` artifact.
5. Extract the ZIP on Android and install `app-debug.apk`. You may need to allow installs from your browser or file manager.

## Gemini
Get a Gemini API key at https://aistudio.google.com/apikey, open the app, enter the key, and tap **SAVE API KEY**. The key is stored in the app's private preferences on this device. Do not share your key or commit it to GitHub.

## Phone features
- **SPEAK:** Android speech recognition, then Gemini response and Android text-to-speech.
- **CONTACTS:** opens the Android contacts picker; permission is requested when needed.
- **COMPOSE EMAIL:** opens the installed email app to draft an email. This opens a draft; it does not read your inbox or send mail automatically.

## Notes
This is an initial Android implementation, not a direct APK conversion of the original Windows/Python application. It uses the Gemini REST API and Android's native speech and intent integrations. A debug APK is unsigned for Play Store release, but can be installed manually.
