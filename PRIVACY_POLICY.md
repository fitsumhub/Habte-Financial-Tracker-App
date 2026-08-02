# Privacy Policy — Habte Financial Tracker

**Last updated:** 2026-08-02 · Applies to the Habte (ሀብቴ) Android app

Welcome to Habte (ሀብቴ) — a personal finance tracker built for the Ethiopian market to help you understand your income, expenses, and financial activity automatically, without manual entry. Your privacy matters to us, and this policy explains exactly what the app can access, what happens to it, and what — if anything — is shared with anyone else. By using Habte, you agree to the practices described in this Privacy Policy.

> **In short:** Habte runs entirely on your phone. There is no account to create, no cloud sync, and no server of ours that your financial data is ever sent to. The only outside network connections the app makes are to show ads (Google AdMob) and to fetch bank logo images (Google's public favicon service) — neither of those ever receives your SMS content, transactions, balances, or any other financial data.

## Contents

1. [Data We Access and Why](#1-data-we-access-and-why)
2. [What Never Leaves Your Device](#2-what-never-leaves-your-device)
3. [Third-Party Services](#3-third-party-services)
4. [Your Choices & Control](#4-your-choices--control)
5. [Security](#5-security)
6. [Data Retention & Deletion](#6-data-retention--deletion)
7. [Children's Privacy](#7-childrens-privacy)
8. [International Users](#8-international-users)
9. [Changes to This Policy](#9-changes-to-this-policy)
10. [Contact](#10-contact)

## 1. Data We Access and Why

| Data | Why we need it | Where it goes |
|---|---|---|
| SMS messages (`READ_SMS`, `RECEIVE_SMS`) | To detect and parse transaction notifications from supported banks and mobile wallets, so you don't have to enter transactions manually. Only messages matching a known bank/wallet sender pattern are parsed into a transaction; everything else is ignored and never stored. | **Stays on your device only.** Parsed transactions are stored in a local database (Room/SQLite). Raw SMS content is never uploaded, copied, or transmitted anywhere. |
| Notification content (optional — Android Notification Access) | An alternative transaction-capture path for banks/wallets that push an app notification instead of (or in addition to) SMS. Off by default even after the system permission is granted; only notifications from apps you explicitly select in **Settings → Notification Capture** are ever inspected — every other notification on your device is left completely alone. | **Stays on your device only.** Parsed the same way as SMS, into the same local database. Never transmitted anywhere. |
| Biometric data (fingerprint/face, via `USE_BIOMETRIC`) | To lock the app behind your device's existing biometric authentication. | Handled entirely by Android's own Biometric API. The app never receives, stores, or has access to your biometric data itself — only a yes/no authentication result. |
| App PIN | An alternative/backup way to lock the app. | Stored on-device only, as a salted PBKDF2 hash (12,000 iterations) — never in plain text, never transmitted. |
| Account balances, transaction history, budgets, payment reminders, certificates | Core app functionality — showing you your financial picture, reminding you about upcoming bills, and generating achievement certificates from your real activity. | **Stays on your device only**, in a local Room database. |
| Optional profile name/email/photo you enter | Personalizing the app, labeling exported statements, and appearing on generated certificates. | **Stays on your device only.** Not transmitted anywhere; no account system or backend exists to send it to. |
| Crash reports | If the app crashes, the error is saved locally so the app can show you a recovery screen on next launch instead of silently failing again. | **Stays on your device only.** Not transmitted to us or anyone else. |

## 2. What Never Leaves Your Device

- Your SMS content and parsed transactions
- Any notification content read via Notification Capture
- Your account, bank, and balance data, including any account numbers you enter
- Your budgets, payment reminders, and generated certificates (including any photo used on one)
- Your PIN, biometric authentication result, and any profile info you enter
- Crash reports

**There is no cloud sync, no account/login system, and no backend server operated by us that any of this data is sent to.** Everything above is read and processed entirely on your device.

## 3. Third-Party Services

The app uses two third-party services, both involving limited, specific network requests:

### Google AdMob (advertising)

The app displays ads via Google's AdMob SDK (banner, interstitial, rewarded, and native ad formats) to support free use of the app. AdMob may collect an advertising identifier and request metadata (device/app information, approximate location derived from IP, etc.) to serve and measure ads, per [Google's advertising and measurement policies](https://policies.google.com/technologies/ads) and [Google's Privacy Policy](https://policies.google.com/privacy). **No SMS content, transaction data, or other financial data collected by the app is ever shared with AdMob or any advertiser** — the ad SDK operates independently and has no access to that data. Where required, a consent message is shown before any ad request is personalized.

You can go ad-free for 24 hours at a time from Settings by watching a short rewarded ad.

### Google Favicon Service (bank logos)

To display a bank's logo, the app requests an icon from Google's public favicon service (`google.com/s2/favicons`) using that bank's domain name. This request contains only the bank's domain — never any of your personal or financial data.

## 4. Your Choices & Control

- **SMS access** can be revoked at any time via Android Settings → Apps → Habte → Permissions. Without it, transactions won't be detected automatically, but the rest of the app continues to work.
- **Notification access** is off by default and entirely optional. You choose exactly which apps are monitored, and can revoke system-level notification access at any time via Android Settings → Apps → Special app access → Notification access.
- **Biometric/PIN lock** can be turned on or off at any time in Settings → Security.
- **Account numbers** you enter for sharing with customers are optional and can be edited or cleared at any time from an account's detail view.
- **Ads** can be paused for 24 hours at a time from Settings by watching a rewarded ad.

## 5. Security

Because everything stays on your device, there is no server of ours to be breached and no account credentials of yours for us to lose. The app itself can be locked behind your device's biometric authentication or a PIN (stored only as a salted PBKDF2 hash, never in plain text). We still recommend keeping your device's own lock screen enabled, since anyone with unlocked access to your phone could otherwise open any app on it, including this one if biometric/PIN lock is turned off.

## 6. Data Retention & Deletion

- Your data persists locally on your device for as long as the app is installed.
- Habte has no cloud account, so there's no separate "delete my account" request to make — removing your data means removing it from this device.
- To delete everything at once without uninstalling, use Android's own **Settings → Apps → Habte → Storage & cache → Clear storage**, which immediately wipes the local database.
- Uninstalling the app deletes its local database and all data with it, per standard Android app-storage behavior.

## 7. Children's Privacy

Habte is a personal-finance tool intended for general audiences and is not directed at children under 13. We do not knowingly collect data from children.

## 8. International Users

Habte is built primarily for users in Ethiopia, but the app itself imposes no location restriction. Regardless of where you are, the same fact applies: no personal or financial data is collected, stored, or processed on any server operated by us, because none exists — every byte of your financial data stays on your own device. If you are in a region with its own data protection law (for example, the EU/UK's GDPR), there is accordingly no personal data of yours held by us to request, export, or delete on our end — your device is the only place it ever lived.

## 9. Changes to This Policy

If this policy changes, the "Last updated" date above will be revised, and material changes will also be reflected in the app's own release notes. Continued use of the app after a change constitutes acceptance of the updated policy.

## 10. Contact

Questions about this policy or your data can be sent to the developer below.

**Developer:** Fitsum Enunu
**App:** Habte (ሀብቴ)
**Email:** fitsumenunu21@gmail.com

---

© 2026 Habte (ሀብቴ). All rights reserved.
