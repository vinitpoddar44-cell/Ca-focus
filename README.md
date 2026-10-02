# CA Focus (Android, v0.1)

Blocks chosen apps during a study session. The Phone app is never blocked, and an emergency contact can be dialled from the focus screens.

## Option A: build the APK in the cloud (no Android Studio needed)
1. Create a free account at github.com and a new repository (private is fine).
2. Upload everything inside this folder (including the hidden .github folder) to the repository root.
3. Open the Actions tab, run "Build APK", and wait about 5 minutes.
4. Open the finished run, download the "CAFocus-apk" artifact, unzip it, and install app-debug.apk on your phone (allow "install unknown apps" when asked).
5. If the build fails, copy the red error text from the log and send it to Claude to fix.

## Option B: build on your computer

1. Install Android Studio. Choose File > Open and select this folder. Let Gradle sync finish.
2. On your phone: Settings > About phone > tap Build number 7 times, then enable USB debugging. Connect by USB.
3. Press Run in Android Studio. For a shareable APK: Build > Build APK(s).
4. In the app, allow "usage access" and "display over other apps", pick apps to block, set an emergency number, and start.

## Notes
- Untested: written without Android build tools. Expect to fix small compile or device issues.
- Needs Android 8 or newer. Some phones (Xiaomi, Oppo, Vivo) also need battery optimisation turned off for the app.
- Google Play restricts usage-access and overlay permissions, so a Play Store release needs a permissions declaration and review.
- Emergency button opens the dialler with the number filled in, so the student presses call.
