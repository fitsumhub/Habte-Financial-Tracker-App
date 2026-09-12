# HABTE — Google Play Store Deployment Guide (v1.0.1)

This complete step-by-step guide covers everything required to deploy **HABTE (ሀብቴ)** to the Google Play Store, from release signing and building the production App Bundle (`.aab`) to Play Console setup, SMS permission declarations, privacy compliance, and store rollout.

---

## Table of Contents
1. [Pre-Deployment Checklist](#1-pre-deployment-checklist)
2. [Step 1: Keystore & Release Signing Setup](#step-1-keystore--release-signing-setup)
3. [Step 2: Configure AdMob Production Unit IDs](#step-2-configure-admob-production-unit-ids)
4. [Step 3: Build Production Android App Bundle (.AAB)](#step-3-build-production-android-app-bundle-aab)
5. [Step 4: Create App in Google Play Console](#step-4-create-app-in-google-play-console)
6. [Step 5: Store Listing & Assets](#step-5-store-listing--assets)
7. [Step 6: Privacy Policy & Data Safety Form](#step-6-privacy-policy--data-safety-form)
8. [Step 7: Google Play SMS Permission Declaration](#step-7-google-play-sms-permission-declaration)
9. [Step 8: Content Rating & App Declarations](#step-8-content-rating--app-declarations)
10. [Step 9: Upload AAB & Release Rollout](#step-9-upload-aab--release-rollout)

---

## 1. Pre-Deployment Checklist

Before building your release package, verify that all release checks pass:

- [x] **Unit & Integration Tests**: Executed `.\gradlew.bat test` — 100% passing.
- [x] **Target SDK**: Configured to `compileSdk 36` and `targetSdk 36` (Android 14+ Play Store requirement).
- [x] **Version Code & Name**: `versionCode 2`, `versionName "1.0.1"` in `app/build.gradle`.
- [x] **R8 / Proguard Rules**: Verified Room, Kotlin Serialization, and Jetpack Compose keep rules in `proguard-rules.pro`.
- [x] **Manifest Exports**: `NotificationCaptureListenerService` exported (`android:exported="true"`).

---

## Step 1: Keystore & Release Signing Setup

Google Play requires all release builds to be signed with a production keystore certificate.

### 1.1 Generate Keystore File
Open PowerShell or Terminal at your project root (`c:\Habte-Financial-Tracker`) and run:

```bash
keytool -genkeypair -v -keystore keystore/habte-release.jks -alias habte-key -keyalg RSA -keysize 2048 -validity 10000
```

> [!CAUTION]
> Store the generated `keystore/habte-release.jks` and its passwords in a secure location (e.g. 1Password/KeePass). **If you lose this keystore, you will never be able to update HABTE on Google Play.**

### 1.2 Create `keystore.properties`
Create a file named `keystore.properties` in the project root directory (`c:\Habte-Financial-Tracker\keystore.properties`):

```properties
storeFile=keystore/habte-release.jks
storePassword=YOUR_KEYSTORE_PASSWORD
keyAlias=habte-key
keyPassword=YOUR_KEY_PASSWORD
```

*(Note: `keystore.properties` and `.jks` files are ignored by git in `.gitignore` to prevent secret leaks).*

---

## Step 2: Configure AdMob Production Unit IDs

Currently, the app uses Google's public test AdMob unit IDs. For release, swap them to your live AdMob IDs.

1. Log in to [Google AdMob Console](https://admob.google.com/).
2. Create an App for **HABTE (Android)**.
3. Create the required Ad Units: Banner, Interstitial, Rewarded, and Native Advanced.
4. Update `app/src/main/java/com/mobile/ads/AdMobConfig.kt`:
   - Set `USE_TEST_ADS = false`.
   - Update `bannerAdUnitId`, `interstitialAdUnitId`, `rewardedAdUnitId`, and `nativeAdUnitId` with your real IDs.
5. Update `app/src/main/AndroidManifest.xml`:
   - Replace `com.google.android.gms.ads.APPLICATION_ID` (`ca-app-pub-3940256099942544~3347511713`) with your real AdMob Application ID.

---

## Step 3: Build Production Android App Bundle (.AAB)

Google Play requires the **Android App Bundle (`.aab`)** format for all new app submissions.

Run the release bundle build command in terminal:

```powershell
.\gradlew.bat clean bundleRelease
```

### Verification:
Upon successful build, the production bundle will be located at:
`app/build/outputs/bundle/release/app-release.aab`

---

## Step 4: Create App in Google Play Console

1. Go to [Google Play Console](https://play.google.com/console).
2. Click **Create app**.
3. Fill in basic details:
   - **App name**: `
   - **Default language**: `English (United States)` or `Amharic`
   - **App or Game**: `App`
   - **Free or Paid**: `Free`
   - **Declarations**: Accept Developer Program Policies and US export laws.
4. Click **Create app**.

---

## Step 5: Store Listing & Assets

Navigate to **Grow > Store presence > Main store listing** in Play Console.

### 5.1 Text Content
- **Short description** *(Max 80 chars)*:
  ```text
  Track Ethiopian bank & wallet spending automatically from your SMS. Private.
  ```
- **Full description** *(Max 4000 chars)*:
  ```text
  Habte automatically tracks your money across every major Ethiopian bank and mobile wallet — no manual entry, no spreadsheets, no bank logins required.

  HOW IT WORKS
  Habte reads the transaction SMS your bank and wallet apps send you, parses the amount, balance, and merchant, and turns it into a clean, organized transaction history — entirely on your device. Supports Commercial Bank of Ethiopia (CBE), Bank of Abyssinia, Awash Bank, Dashen Bank, Hibret Bank, telebirr, and many more Ethiopian banks and wallets.

  WHY HABTE

  📊 Automatic Transaction Tracking
  No manual entry. Every SMS-notified transaction is captured, categorized, and added to your history automatically.

  💰 Real Budgets & Analytics
  See income vs. expenses by week, month, or year. Break spending down by category or by bank. Track your net worth across every account in one place.

  🔔 Payment Reminders
  Never miss a bill. Set up recurring reminders for rent, subscriptions, and payments with custom lead times.

  🔒 Private by Design
  Your financial data never leaves your device. There is no cloud account, no login, and no server — everything is stored locally with PIN and biometric lock protecting the app itself.

  🏆 Achievement Certificates
  Celebrate real progress with Weekly, Monthly, and Yearly certificates generated from your actual savings — downloadable and shareable.

  📁 Export & Backup
  Export your transaction history as PDF, CSV, or JSON anytime, and restore from a backup whenever you need to.

  🧮 Built-in Financial Tools
  Loan (EMI) calculator, income tax estimator, currency converter, and a duplicate-transaction checker to keep your records clean.

  🇪🇹 Built for Ethiopia
  Optional Ethiopian calendar support alongside Gregorian, and full support for Ethiopian Birr (ETB) throughout.
  ```

### 5.2 Graphics & Media Assets
- **App Icon**: `512 x 512 px` (PNG/JPEG, max 1MB) — located at `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png`.
- **Feature Graphic**: `1024 x 500 px` (PNG/JPEG, max 1MB).
- **Phone Screenshots**: Minimum 2 screenshots (16:9 or 9:16 aspect ratio, e.g. `1080 x 1920 px`).
- **7-inch & 10-inch Tablet Screenshots**: Minimum 1 screenshot for each tablet format.

---

## Step 6: Privacy Policy & Data Safety Form

### 6.1 Privacy Policy URL
Google Play requires a publicly accessible HTTPS Privacy Policy link.

1. Host the included `docs/privacy-policy.html` on GitHub Pages or your custom domain:
   - Example: `https://yourusername.github.io/Habte-Financial-Tracker/privacy-policy.html`
2. Enter this URL in **App content > Privacy policy** in Play Console.

### 6.2 Data Safety Form Setup
Navigate to **Policy and programs > App content > Data safety**:

- **Does your app collect or share user data?**: Select `Yes` (due to AdMob SDK advertising ID collection).
- **Is all of the user data collected by your app encrypted in transit?**: `Yes`.
- **Do you provide a way for users to request that their data be deleted?**: `Yes` (User can clear all data in Settings > Clear All Data).

#### Data Types Selection:
1. **Financial Info (Financial Account Info / Transaction History)**:
   - Collected? `No` (Data stays 100% on-device local Room DB, never collected or uploaded to any server).
2. **Device or other IDs (Advertising ID)**:
   - Collected? `Yes` (Collected by Google Mobile Ads SDK for ad serving/fraud prevention).
   - Ephemeral? `No`.
   - Required or Optional? `Required`.
   - Purpose: `Advertising or marketing`, `Analytics`.

---

## Step 7: Google Play SMS Permission Declaration

HABTE requests `READ_SMS` and `RECEIVE_SMS` permissions to automatically parse bank transaction alerts.

Navigate to **Policy and programs > App content > Sensitive permissions > SMS and Call Log permissions**:

1. **Core Functionality Selection**: Select **Financial Management / Personal Finance**.
2. **Declaration Justification Text**:
   ```text
   Habte is an automatic personal finance tracker designed for Ethiopian financial institutions (CBE, BOA, Telebirr, Awash, Dashen). Ethiopian banks communicate completed deposits, withdrawals, and balance updates exclusively via SMS. The app's core value proposition relies on reading and parsing incoming bank SMS messages locally on the user's device to automatically construct transaction logs, budget analytics, and net worth overview without requiring manual data entry or online bank login credentials. No SMS data is ever transmitted off the user's device.
   ```
3. **Proof of Functionality (Demo Video)**:
   - Record a 30-second screen recording showing an incoming bank SMS automatically generating a transaction entry on HABTE's Home screen.
   - Provide a YouTube or Google Drive video link to the Google review team.

---

## Step 8: Content Rating & App Declarations

### 8.1 Content Rating (IARC)
1. Go to **Policy and programs > App content > Content ratings**.
2. Complete questionnaire:
   - Category: `Utility, Productivity, Communication, or Other`
   - Violence, Sex, Language, Controlled Substances: Select `No` for all.
   - Resulting Rating: **Everyone (3+) / PEGI 3**.

### 8.2 Target Audience & Ads Declaration
- **Target Audience**: `18 and over` (General audience).
- **Contains Ads**: Select `Yes, my app contains ads` (due to AdMob integration).
- **Financial Features**: Select `Personal Financial Management`.

---

## Step 9: Upload AAB & Release Rollout

1. In Play Console, navigate to **Testing > Closed testing** (recommended for initial verification) or **Production**.
2. Click **Create new release**.
3. Upload `app/build/outputs/bundle/release/app-release.aab`.
4. Enter **Release notes** for `v1.0.1`:
   ```text
   - Initial release of Habte (ሀብቴ) Personal Financial Tracker.
   - Automatic transaction tracking for CBE, BOA, Telebirr, Awash, Dashen, and more.
   - Multi-bank account balance overview and net worth analytics.
   - Custom payment reminders and downloadable savings achievement certificates.
   - 100% private, on-device local storage with biometric lock protection.
   ```
5. Click **Save** -> **Review release** -> **Start rollout to Production**.

---

### 🎉 Deployment Complete!

Once submitted, Google Play review typically takes **1 to 3 business days**. You can track review status under **Dashboard > App status** in Play Console.

