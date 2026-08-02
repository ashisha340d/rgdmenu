# RGD Menu

A small native Android app (Kotlin) showing a scrollable menu list. It's a starting scaffold — swap the hard-coded items in `MainActivity.kt` for whatever content you want.

## Getting the APK on your phone

Every push triggers `.github/workflows/build-apk.yml`, which builds a debug APK and publishes it as a GitHub Release.

1. Push/merge to this repo (already wired to run on any branch).
2. Open the **Actions** tab to watch the build, or go straight to **Releases**.
3. On your phone, open the latest release in a browser and download `app-debug.apk`.
4. Allow "install unknown apps" for your browser the first time, then open the downloaded file to install.

## Local build (if you ever have Android Studio / SDK available)

```
./gradlew assembleDebug
```

Output APK: `app/build/outputs/apk/debug/app-debug.apk`
