# nxthyPR - Kiosk App for Phone Rental

This application is designed to lock an Android device into a kiosk mode until specific conditions are met (Admin password or validated internet on a specific Wi-Fi).

## Features
- **Kiosk Mode**: Uses Android's Lock Task Mode to pin the app.
- **Hardware Restrictions**: Blocks power menu, status bar, and safe boot.
- **Smart Unlock**: Automatically unlocks when connected to Wi-Fi `PHONE_01` with validated internet access.
- **Admin Bypass**: Unlock manually with password `nath261920`.
- **Safety Mechanism**: Long-press the "WELCOME USER" text to trigger the admin password prompt anytime.

## How to Build the APK Fast

Since building an APK requires a full Android SDK environment which is quite large, the fastest ways for you to get the APK are:

### Option 1: Use an Online Build Service (Easiest & Fastest)
1. Go to [Appetize.io](https://appetize.io/) or [GitHub Actions](https://github.com/features/actions).
2. If using GitHub:
   - Create a new repository and upload this source code.
   - Add a simple GitHub Action for Android (templates are available).
   - It will automatically build the APK for you in the cloud.

### Option 2: Use Android Studio (Recommended for Developers)
1. Download and install [Android Studio](https://developer.android.com/studio).
2. Open the `nxthyPR` folder as an existing project.
3. Click **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. The APK will be located in `app/build/outputs/apk/debug/app-debug.apk`.

## How to Provision as Device Owner
To enable the high-security kiosk features, the app **must** be set as the Device Owner via ADB.

1. Install the APK on the device.
2. Go to **Settings > Accounts** and remove all accounts (Google, etc.).
3. Enable **USB Debugging** on the phone.
4. Connect the phone to your computer and run the following command:

```bash
adb shell dpm set-device-owner com.nxthy.pr/.AdminReceiver
```

## Security & Safety Note
- **Don't Get Locked Out**: The app is designed to be very secure. Always remember the password `nath261920`.
- **Testing**: When you first install it, **do not** run the ADB command immediately. Open the app normally first to ensure you can use the long-press gesture on the welcome text to trigger the password prompt.
- **Provisioning**: Only run the `adb shell dpm set-device-owner` command once you are sure the app behaves as you want. Once provisioned as Device Owner, the app becomes much harder to remove without the admin password or a factory reset (if allowed).
