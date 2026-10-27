# Parivar Health – Android app

This folder is a complete Android project. It turns into an installable APK file in about 5 minutes on GitHub, which is free. You don't need to install anything on your computer.

## Get the APK (free, using GitHub)

1. Sign in at https://github.com (or create a free account).
2. Click **+** (top right) → **New repository**. Name it `parivar-health`, choose **Private**, and click **Create repository**.
3. On the new repository page, click **uploading an existing file**.
4. Unzip `ParivarHealth-android.zip` on your computer. Open the `ParivarHealth-android` folder, select **everything inside it** (including the `.github` folder), and drag it into the upload area. Then click **Commit changes**.
   - The `.github` folder can be hidden. On Windows: File Explorer → View → Show → Hidden items. On Mac: press Cmd+Shift+. in Finder.
5. Open the **Actions** tab. A run called **Build APK** starts by itself. If it doesn't start, click **Build APK** → **Run workflow**.
6. Wait for the green tick (about 4–6 minutes). Click the run, scroll down to **Artifacts**, and download **ParivarHealth-APK**. It downloads as a zip; unzip it to get `ParivarHealth.apk`.

## Get the APK (with Android Studio)

Open this folder in Android Studio, then choose **Build → Build App Bundle(s) / APK(s) → Build APK(s)**. The file is saved at `app/build/outputs/apk/debug/app-debug.apk`.

## Install it on the phone

1. Copy `ParivarHealth.apk` to the phone. You can send it to yourself on WhatsApp, use Google Drive, or use a USB cable.
2. Tap the file. Android asks you to allow installing from this source (WhatsApp, Files or Drive). Allow it, then tap **Install**.
3. Google Play Protect may warn that it doesn't recognise the app. That's normal for apps not on the Play Store. Tap **More details → Install anyway**.

## First use

- **AI features** read reports, check medicine interactions and answer questions. They need a Claude API key:
  1. Create a key at https://console.anthropic.com and add some credit to your account.
  2. In the app, tap the gear icon (top right) and paste the key.

  Everything else works without a key: records, lab trends, the medicine list and the built-in interaction list.
- **From WhatsApp:** open a report → **Share** → **Parivar Health**. The app opens and reads it.
- **Your data** stays only on this phone, inside the app. Uninstalling the app deletes it.

## What's inside

| Path | What it is |
|---|---|
| `app/src/main/assets/index.html` | The whole app screen (same as the web version) |
| `app/src/main/java/.../MainActivity.java` | Shows the screen, file picker, WhatsApp share, AI connection |
| `app/src/main/AndroidManifest.xml` | App name, permissions, "Share to Parivar Health" |
| `.github/workflows/build-apk.yml` | Builds the APK automatically on GitHub |
