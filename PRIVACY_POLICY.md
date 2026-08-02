# Privacy Policy — Habte Financial Tracker

**Last updated:** 2026-07-30

Habte (ሀብቴ) ("the app", "we", "our") is developed for the Ethiopian market to help users track their finances automatically from bank and Telebirr SMS notifications. This policy explains what data the app accesses, what stays on your device, and what — if anything — is shared with third parties.

## 1. Data We Access and Why

| Data | Why we need it | Where it goes |
|---|---|---|
| SMS messages (`READ_SMS`, `RECEIVE_SMS`) | To detect and parse transaction notifications from supported banks and Telebirr, so you don't have to enter transactions manually. Only messages matching a known bank/Telebirr sender pattern are parsed into a transaction; everything else is ignored and never stored. | **Stays on your device only.** Parsed transactions are stored in a local database (Room/SQLite). Raw SMS content is never uploaded, copied, or transmitted anywhere. |
| Biometric data (fingerprint/face, via `USE_BIOMETRIC`) | To lock the app behind your device's existing biometric authentication. | Handled entirely by Android's own Biometric API. The app never receives, stores, or has access to your biometric data itself — only a yes/no authentication result. |
| App PIN | An alternative/backup way to lock the app. | Stored on-device only, as a salted PBKDF2 hash — never in plain text, never transmitted. |
| Account balances, transaction history, budgets, payment reminders | Core app functionality — showing you your financial picture and reminding you about upcoming bills. | **Stays on your device only**, in a local Room database. |
| Optional profile name/email you enter | Personalizing the app and labeling exported statements. | **Stays on your device only.** Not transmitted anywhere; no account system or backend exists to send it to. |
| Crash reports | If the app crashes, the error is saved locally so the app can show you a recovery screen on next launch instead of silently failing again. | **Stays on your device only.** Not transmitted to us or anyone else. |

## 2. What Never Leaves Your Device

- Your SMS content and parsed transactions
- Your account, bank, and balance data
- Your budgets and payment reminders
- Your PIN, biometric authentication result, and any profile info you enter
- Crash reports

**There is no cloud sync, no account/login system, and no backend server operated by us that any of this data is sent to.**

## 3. Third-Party Services

The app uses two third-party services, both of which involve limited network requests:

### Google AdMob (advertising)

The app displays ads via Google's AdMob SDK (banner, interstitial, rewarded, and native ad formats) to support free use of the app. AdMob may collect an advertising identifier and request metadata (device/app information, approximate location derived from IP, etc.) to serve and measure ads, per [Google's advertising and measurement policies](https://policies.google.com/technologies/ads) and [Google's Privacy Policy](https://policies.google.com/privacy). **No SMS content, transaction data, or other financial data collected by the app is ever shared with AdMob or any advertiser** — the ad SDK operates independently and has no access to that data.

You can go ad-free for 24 hours at a time from Settings by watching a short rewarded ad.

### Google Favicon Service (bank logos)

To display a bank's logo, the app requests an icon from Google's public favicon service (`google.com/s2/favicons`) using that bank's domain name. This request contains only the bank's domain — never any of your personal or financial data.

## 4. Data Retention & Deletion

- Your data persists locally on your device for as long as the app is installed.
- You can clear all locally stored financial data at any time via **Settings → Sign Out**.
- Uninstalling the app deletes its local database and all data with it, per standard Android app-storage behavior.

## 5. Children's Privacy

Habte is a personal-finance tool intended for general audiences and is not directed at children under 13. We do not knowingly collect data from children.

## 6. Changes to This Policy

If this policy changes, the "Last updated" date above will be revised. Continued use of the app after a change constitutes acceptance of the updated policy.

## 7. Contact

Questions about this policy or your data can be sent to: **fitsumenunu21@gmail.com**
