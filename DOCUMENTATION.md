# Habte Financial Tracker — Full Documentation

**Habte (ሀብቴ)** is a premium, privacy-first personal finance tracker built for the Ethiopian market. It automatically syncs transactions from bank and Telebirr SMS notifications, provides analytics and budgeting tools, and includes an AI financial assistant powered by Google Gemini.

| Property | Value |
|---|---|
| **Package ID** | `com.mobile.habte` |
| **Version** | 1.0.0 (versionCode 1) |
| **Min SDK** | 26 (Android 8.0) |
| **Target SDK** | 34 (Android 14) |
| **Language** | Kotlin 1.9.10 |
| **UI** | Jetpack Compose + Material 3 |

---

## Table of Contents

1. [Overview](#1-overview)
2. [Key Features](#2-key-features)
3. [Architecture](#3-architecture)
4. [Project Structure](#4-project-structure)
5. [Technology Stack](#5-technology-stack)
6. [Setup & Installation](#6-setup--installation)
7. [Configuration](#7-configuration)
8. [Data Models](#8-data-models)
9. [Core Modules](#9-core-modules)
10. [UI Screens & Navigation](#10-ui-screens--navigation)
11. [Security](#11-security)
12. [Theming & Localization](#12-theming--localization)
13. [Testing](#13-testing)
14. [Build & Release](#14-build--release)
15. [Privacy & Data Handling](#15-privacy--data-handling)
16. [Troubleshooting](#16-troubleshooting)
17. [Contributing](#17-contributing)
18. [License](#18-license)

---

## 1. Overview

Habte is designed to help Ethiopian users track their finances without manual data entry. The app:

- **Reads SMS notifications** from major Ethiopian banks and Telebirr
- **Parses transaction details** (amount, balance, type, category) locally on-device
- **Displays accounts and balances** in a modern Material 3 interface
- **Provides analytics, budgeting, and export tools**
- **Offers Habte AI Pro** — a Gemini-powered financial coach that uses your live transaction data

All financial data processing happens **locally**. There is no cloud sync of transaction data. The only network call is to the Gemini API when using the AI chat feature (and only the summarized financial snapshot is sent, not raw SMS).

---

## 2. Key Features

### Automated Transaction Sync

| Feature | Description |
|---|---|
| **Telebirr Integration** | Parses SMS from sender IDs `127`, `telebirr`, `tele`, `*127#` |
| **Bank SMS Parsing** | Supports 16 Ethiopian banks (CBE, BOA, Awash, Dashen, etc.) |
| **Real-time Capture** | `SmsReceiver` listens for incoming SMS and adds transactions instantly |
| **Historical Sync** | On first launch, reads entire SMS inbox to backfill transactions |
| **Auto-categorization** | Classifies transactions into Food, Bills, Transport, Income, etc. |
| **Account Detection** | Extracts last 4 digits of account numbers from SMS body |

### Security

| Feature | Description |
|---|---|
| **Biometric Lock** | Fingerprint / Face ID on app startup via Android Biometric API |
| **Privacy Mode** | Enables `FLAG_SECURE` — blocks screenshots and blurs recents |
| **Balance Masking** | Tap to hide balances; optional auto-hide setting |
| **App PIN** | Configurable 4-digit PIN (default: `1234`) |
| **Sign Out** | Clears all in-memory financial data |

### User Experience

| Feature | Description |
|---|---|
| **Material 3 UI** | Dark/light themes with glassmorphism and micro-animations |
| **Dynamic Theming** | Light, Dark, or System Default |
| **Localization** | English and Amharic app name (`ሀብቴ`) |
| **Bottom Navigation** | 5-tab nav: Analytics, Budget, Home, Tools, Settings |
| **Haptic Feedback** | Tactile responses on key interactions |

### Financial Tools

| Feature | Description |
|---|---|
| **Analytics Dashboard** | Net worth, spending trends, category breakdown |
| **Budget Tracking** | Budget screen for spending limits |
| **CSV / JSON Export** | Export transaction history via Android Storage Access Framework |
| **Currency Converter** | Built-in ETB converter tool |
| **Loan & Tax Calculators** | EMI and income tax estimators |
| **Habte AI Pro** | Streaming Gemini chat with 12 preset financial prompts |

---

## 3. Architecture

Habte follows a **Repository Pattern** with **reactive state** via Kotlin `StateFlow`. The UI layer is fully Compose-based with no ViewModels — screens observe repository flows directly.

```
┌─────────────────────────────────────────────────────────────┐
│                        MainActivity                          │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────────────┐ │
│  │ Biometric   │  │ Privacy Mode │  │ SettingsRepository  │ │
│  │ Auth Gate   │  │ FLAG_SECURE  │  │ (SharedPreferences) │ │
│  └─────────────┘  └──────────────┘  └─────────────────────┘ │
└──────────────────────────┬──────────────────────────────────┘
                           │
                    ┌──────▼──────┐
                    │  RootLayout  │
                    │ ErrorBoundary│
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │ AppNavigation│
                    │  (Scaffold)  │
                    └──────┬──────┘
                           │
         ┌─────────────────┼─────────────────┐
         │                 │                 │
    ┌────▼────┐      ┌─────▼─────┐     ┌────▼────┐
    │ Screens │      │ Components │     │  Theme  │
    └────┬────┘      └───────────┘     └─────────┘
         │
    ┌────▼────────────────────────────────────────┐
    │              Data Layer                      │
    │  FinanceRepository  │  SettingsRepository   │
    │  SmsParser          │  GeminiService        │
    │  SmsReceiver        │  Data (models/seeds)  │
    └─────────────────────────────────────────────┘
         │
    ┌────▼────┐         ┌──────────┐
    │ SMS Inbox│         │ Gemini API│
    │ (local)  │         │ (AI only) │
    └──────────┘         └──────────┘
```

### Design Decisions

- **In-memory storage**: Banks and transactions live in `MutableStateFlow` — data is lost on app kill unless re-synced from SMS
- **No Room/SQLite**: Simplifies architecture; SMS inbox is the source of truth
- **Singleton repositories**: `FinanceRepository`, `SettingsRepository`, `GeminiService` are Kotlin `object`s
- **No Navigation Component graph**: Route state is a simple `String` in `AppNavigation`

---

## 4. Project Structure

```
Habte-Financial-Tracker-App-main/
├── app/
│   ├── build.gradle                 # App dependencies & BuildConfig
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/mobile/
│       │   │   ├── MainActivity.kt           # Entry point, biometric gate
│       │   │   ├── data/
│       │   │   │   ├── Data.kt               # Models & preset banks
│       │   │   │   ├── FinanceRepository.kt  # Banks & transactions state
│       │   │   │   ├── SettingsRepository.kt # User preferences
│       │   │   │   ├── SmsParser.kt          # SMS → Transaction parser
│       │   │   │   ├── SmsReceiver.kt        # BroadcastReceiver for SMS
│       │   │   │   └── GeminiService.kt      # AI chat integration
│       │   │   └── ui/
│       │   │       ├── RootLayout.kt           # Error boundary wrapper
│       │   │       ├── navigation/
│       │   │       │   └── AppNavigation.kt  # Bottom nav & routing
│       │   │       ├── screens/              # All app screens (15)
│       │   │       ├── components/           # Reusable UI components
│       │   │       └── theme/                  # Colors, theme, extended colors
│       │   └── res/
│       │       ├── drawable/                   # Bank logos, launcher icons
│       │       ├── values/                     # strings, themes
│       │       └── values-am/                  # Amharic strings
│       ├── test/                               # Unit tests
│       └── androidTest/                        # Instrumented / E2E tests
├── build.gradle                     # Root Gradle config
├── settings.gradle
├── local.properties.example         # SDK path & Gemini API key template
├── README.md
└── DOCUMENTATION.md                 # This file
```

---

## 5. Technology Stack

| Category | Technology | Version |
|---|---|---|
| Language | Kotlin | 1.9.10 |
| Build System | Gradle (AGP) | 8.2.2 |
| UI Framework | Jetpack Compose | BOM 2024.02.00 |
| Design System | Material 3 | — |
| Navigation | Custom (Compose state) | — |
| Async | Kotlin Coroutines | — |
| State | StateFlow / MutableStateFlow | — |
| Image Loading | Coil Compose | 2.5.0 |
| Security | Biometric KTX | 1.2.0-alpha05 |
| AI | Google Generative AI SDK | 0.9.0 |
| Testing | JUnit 4, Espresso, Compose UI Test | — |

### Key Dependencies

```gradle
// Compose BOM
implementation platform('androidx.compose:compose-bom:2024.02.00')

// Navigation
implementation 'androidx.navigation:navigation-compose:2.7.7'

// Biometric
implementation 'androidx.biometric:biometric-ktx:1.2.0-alpha05'

// Gemini AI
implementation 'com.google.ai.client.generativeai:generativeai:0.9.0'
```

---

## 6. Setup & Installation

### Prerequisites

- **Android Studio** Hedgehog (2023.1.1) or newer
- **JDK 17**
- **Android SDK 34**
- Physical Android device or emulator (API 26+)
- **Gemini API key** (optional, for AI chat)

### Steps

1. **Clone the repository**

   ```bash
   git clone https://github.com/fitsumhub/Habte-Financial-Tracker-App.git
   cd Habte-Financial-Tracker-App
   ```

2. **Configure local properties**

   Copy `local.properties.example` to `local.properties`:

   ```properties
   sdk.dir=C\:\\Users\\YOUR_USERNAME\\AppData\\Local\\Android\\Sdk
   GEMINI_API_KEY=your_gemini_api_key_here
   ```

3. **Open in Android Studio**

   Open the project root folder. Gradle sync should complete automatically.

4. **Build and run**

   ```bash
   ./gradlew assembleDebug
   ```

   Or use **Run ▶** in Android Studio with a connected device.

5. **Grant SMS permissions**

   On first launch, the app requests `READ_SMS` and `RECEIVE_SMS`. Grant these for automatic transaction sync.

---

## 7. Configuration

### Gemini API Key

The AI chat feature requires a Google Gemini API key:

1. Get a key from [Google AI Studio](https://aistudio.google.com/)
2. Add it to `local.properties`:

   ```properties
   GEMINI_API_KEY=AIza...
   ```

3. Rebuild the app. The key is injected via `BuildConfig.GEMINI_API_KEY`.

> **Security note**: Never commit `local.properties` to version control. It is listed in `.gitignore`.

### Android Permissions

Defined in `AndroidManifest.xml`:

| Permission | Purpose |
|---|---|
| `INTERNET` | Gemini AI API calls |
| `USE_BIOMETRIC` | Fingerprint / Face authentication |
| `READ_SMS` | Historical SMS sync from inbox |
| `RECEIVE_SMS` | Real-time SMS capture via BroadcastReceiver |
| `RECEIVE_BOOT_COMPLETED` | Reserved for future boot-time sync |

### Settings (SharedPreferences)

Stored under `habte_settings` preference file:

| Key | Default | Description |
|---|---|---|
| `biometric` | `true` | Enable biometric login |
| `auto_hide` | `false` | Auto-hide balances |
| `privacy_mode` | `false` | Anti-screenshot mode |
| `notifications` | `true` | Push notifications |
| `email_updates` | `false` | Email update preference |
| `sms_alerts` | `true` | SMS alert preference |
| `currency` | `ETB` | Display currency |
| `language` | `English` | UI language |
| `date_format` | `MM/DD/YYYY` | Date display format |
| `theme` | `Dark` | Light / Dark / System Default |
| `app_pin` | `1234` | App PIN code |

---

## 8. Data Models

### AccountType

```kotlin
enum class AccountType { SAVINGS, CURRENT, MOBILE }
```

### Account

```kotlin
data class Account(
    val id: String,
    val bankId: String,
    val accountNumber: String,   // e.g. "•••• 1234"
    val label: String,           // e.g. "CBE Account (*1234)"
    val balance: Double,
    val currency: String,        // "ETB"
    val type: AccountType
)
```

### Bank

```kotlin
data class Bank(
    val id: String,
    val name: String,            // "Commercial Bank of Ethiopia"
    val shortName: String,       // "CBE"
    val accounts: List<Account>,
    val colorFrom: String,       // Hex gradient start
    val colorTo: String,         // Hex gradient end
    val logoText: String,        // Fallback logo text
    val logoResId: Int? = null,  // Drawable resource ID
    val domain: String? = null   // For Clearbit logo fetching
)
```

### Transaction

```kotlin
data class Transaction(
    val id: String,
    val title: String,
    val amount: Double,          // Always positive; sign determined by type
    val date: String,            // "MMM dd, yyyy"
    val type: String,            // "credit" or "debit"
    val bankShortName: String,   // "CBE", "TEL", etc.
    val category: String,        // "Food & Dining", "Income", etc.
    val balance: Double? = null,
    val accountSuffix: String? = null  // Last 4 digits
)
```

### Supported Banks (Preset List)

| ID | Bank | Short Name |
|---|---|---|
| `cbe` | Commercial Bank of Ethiopia | CBE |
| `boa` | Bank of Abyssinia | BOA |
| `awash` | Awash Bank | AWA |
| `dashen` | Dashen Bank | DAS |
| `hibret` | Hibret Bank | HIB |
| `zemen` | Zemen Bank | ZEM |
| `nib` | Nib International Bank | NIB |
| `coop` | Cooperative Bank of Oromia | COO |
| `abay` | Abay Bank | ABY |
| `berhan` | Berhan Bank | BER |
| `bunna` | Bunna Bank | BUN |
| `wegagen` | Wegagen Bank | WEG |
| `oromia` | Oromia Bank | ORO |
| `lion` | Lion Bank | LIO |
| `enat` | Enat Bank | ENA |
| `tele` | Telebirr | TEL |

### Transaction Categories

| Category | Trigger Keywords |
|---|---|
| Income | All credit transactions |
| Bills & Utilities | airtime, electric, water, utility |
| Food & Dining | supermarket, restaurant, cafe, hotel |
| Transport | fuel, petrol |
| Cash | ATM, withdraw |
| Transfers | transfer |
| Shopping | purchase |
| General | Default for unmatched debits |

---

## 9. Core Modules

### 9.1 SmsParser

**File**: `app/src/main/java/com/mobile/data/SmsParser.kt`

Parses incoming SMS messages into `Transaction` objects.

**Flow**:

```
SMS (sender, body, timestamp)
    │
    ▼
Detect bank from sender ID
    │
    ▼
Determine type (credit/debit) from keywords
    │
    ▼
Extract amount via regex
    │
    ▼
Extract balance (optional)
    │
    ▼
Extract account suffix (last 4 digits)
    │
    ▼
Categorize title & category
    │
    ▼
Return Transaction or null
```

**Sender ID Mapping**:

| Sender Pattern | Bank |
|---|---|
| `cbe`, `1000`, `8008` | CBE |
| `boa`, `abyssinia` | BOA |
| `telebirr`, `tele`, `127`, `*127#` | TEL (Telebirr) |
| `awash` | AWA |
| `dashen` | DAS |
| `hibret`, `united` | HIB |
| `zemen` | ZEM |
| `nib` | NIB |
| `coop`, `coopbank` | COO |
| `abay` | ABY |
| `berhan` | BER |
| `bunna` | BUN |
| `wegagen` | WEG |
| `oromia` | ORO |
| `lion` | LIO |
| `enat` | ENA |

**Amount Regex Patterns**:

- `(?:etb|birr|amount...) ([0-9,]+\.?[0-9]*)`
- `([0-9,]+\.?[0-9]*) (?:etb|birr|br\.)`

**Special Rules**:

- Returns `null` for unknown senders, missing amounts, zero amounts, or OTP messages
- Telebirr forces `accountSuffix = null` (single wallet account)
- Transaction IDs: `sms-{bankShortName}-{timestamp}`

### 9.2 SmsReceiver

**File**: `app/src/main/java/com/mobile/data/SmsReceiver.kt`

A `BroadcastReceiver` registered in the manifest with priority 999 for `SMS_RECEIVED`.

```kotlin
override fun onReceive(context: Context, intent: Intent) {
    // For each SMS message:
    //   1. Extract address, body, timestamp
    //   2. Call SmsParser.parseMessage()
    //   3. If valid, FinanceRepository.addTransaction()
}
```

### 9.3 FinanceRepository

**File**: `app/src/main/java/com/mobile/data/FinanceRepository.kt`

Central state manager for banks and transactions.

**Public API**:

| Method | Description |
|---|---|
| `banks: StateFlow<List<Bank>>` | Observable bank list |
| `transactions: StateFlow<List<Transaction>>` | Observable transaction list |
| `addBank(bank)` | Add a bank manually |
| `removeBank(bankId)` | Remove bank and its transactions |
| `updateBankColors(bankId, from, to)` | Update bank card gradient |
| `addTransaction(transaction)` | Add a single transaction (sorted by date desc) |
| `updateTransactionCategory(id, category)` | Recategorize a transaction |
| `clearAll()` | Wipe all banks and transactions |
| `syncHistoricalSms(context)` | Read SMS inbox and populate state (IO dispatcher) |

**Historical SMS Sync Logic**:

1. Query `content://sms/inbox` sorted by date descending
2. Parse each message with `SmsParser`
3. Group transactions by `(bankShortName, accountSuffix)`
4. Auto-create banks from `PRESET_BANKS` metadata
5. Auto-create accounts with masked account numbers
6. Calibrate account balances from latest SMS balance field

### 9.4 SettingsRepository

**File**: `app/src/main/java/com/mobile/data/SettingsRepository.kt`

Manages user preferences via `SharedPreferences` with reactive `StateFlow` exposure.

**Initialization**: Called in `MainActivity.onCreate()` before `super.onCreate()`.

**Theme modes**: `Light`, `Dark`, `System Default` — applied via `AppCompatDelegate.setDefaultNightMode()`.

### 9.5 GeminiService

**File**: `app/src/main/java/com/mobile/data/GeminiService.kt`

Integrates Google Gemini for the Habte AI Pro chat feature.

| Property | Value |
|---|---|
| Model | `gemini-2.0-flash-lite` |
| Max history turns | 12 |
| Max output tokens | 768 |
| Temperature | 0.55 |

**Key Methods**:

| Method | Description |
|---|---|
| `buildSnapshot(transactions, banks)` | Creates a cached financial context digest |
| `streamMessage(userMessage, history, snapshot)` | Returns `Flow<String>` of streaming AI response |
| `resetSession()` | Clears chat session and cached model |

**Financial Snapshot Contents**:

- Account summaries (top 5 banks)
- Spend / income / net flow totals
- Top 5 spending categories
- Top 3 bank outflows
- 8 most recent transactions

**System Prompt Rules**:

- ETB currency only
- Never invent data not in the snapshot
- Structured response: Headline → Analysis → Action
- Max ~220 words unless deep audit requested

---

## 10. UI Screens & Navigation

### Navigation Map

```
Bottom Navigation (always visible except AI Chat):
├── Analytics          → AnalyticsScreen
├── Budget             → BudgetScreen
├── Home (default)     → HomeScreen
├── Tools              → ToolsScreen
└── Settings           → SettingsScreen

Secondary Routes (no bottom nav):
├── ai_chat            → AiChatScreen
├── profile            → ProfileScreen
├── transaction_history → TransactionHistoryScreen
├── alerts             → AlertsScreen
├── security           → SecurityScreen
├── export_data        → ExportDataScreen
└── support            → SupportScreen
```

### Screen Descriptions

#### HomeScreen

The main dashboard. Features:

- **Summary / Today / per-bank tabs** via `TopTabBar`
- **Total balance card** with show/hide toggle
- **Bank cards** with gradient backgrounds and account details
- **Recent transactions** list with detail sheet
- **Add bank modal** from preset list
- **SMS permission request** and historical sync on launch
- **Navigation** to AI chat, profile, and transaction history

#### AnalyticsScreen

Financial insights dashboard:

- Net worth summary card
- Spending trend sparkline (derived from recent transactions)
- Category breakdown
- Per-bank analytics
- Quick link to Habte AI

#### BudgetScreen

Budget tracking and spending limits interface.

#### ToolsScreen

Grid of financial utilities:

| Tool | Function |
|---|---|
| Transfer History | View all transactions |
| Statement | Download statements |
| Converter | Currency exchange rates |
| Loan Calc | EMI calculator |
| Tax Calc | Income tax estimator |
| Alerts | Balance alert configuration |
| Security | Navigate to security settings |
| Export Data | CSV export |

#### SettingsScreen

App configuration hub:

- Profile management
- Theme, language, currency, date format
- Security toggles (biometric, privacy, auto-hide)
- SMS sync trigger
- Data export
- Cache management
- Sign out (clears all data)

#### AiChatScreen

Habte AI Pro chat interface:

- Streaming Gemini responses with markdown-style bold
- 12 preset suggestion chips (spending summary, cost cutting, budget forecast, etc.)
- Live financial snapshot banner
- Copy message, regenerate response
- Ideas panel toggle

#### SecurityScreen

Dedicated security settings:

- Biometric login toggle
- Auto-hide balances
- Privacy mode (anti-screenshot)
- Change PIN modal

#### ExportDataScreen

Data export options:

- **CSV**: Date, Title, Amount, Type, Category, Bank
- **JSON**: Full transaction objects with formatting

Uses Android Storage Access Framework (`CreateDocument`).

#### Other Screens

| Screen | Purpose |
|---|---|
| `ProfileScreen` | User profile display and editing |
| `TransactionHistoryScreen` | Full transaction list with filtering |
| `AlertsScreen` | Balance and spending alert configuration |
| `SupportScreen` | Help and contact information |
| `NotFoundScreen` | 404 fallback (unused in current nav) |

### Reusable Components

| Component | Purpose |
|---|---|
| `BalanceCard` | Total balance display with masking |
| `BankCard` | Individual bank account card |
| `BankLogo` | Bank logo (drawable or Clearbit fallback) |
| `AddBankModal` | Modal to add bank from presets |
| `AddAccountCard` | Card prompting account addition |
| `AccountDetailSheet` | Bottom sheet for account details |
| `TransactionDetailSheet` | Bottom sheet for transaction details |
| `TopTabBar` | Horizontal tab selector |
| `ErrorBoundary` | Catches Compose rendering errors |
| `ErrorFallback` | Error display UI |
| `KeyboardAwareColumn` | Keyboard-aware scroll container |

---

## 11. Security

### Biometric Authentication

Implemented in `MainActivity`:

1. On launch, check `SettingsRepository.biometricEnabled`
2. If enabled, verify device supports `BIOMETRIC_STRONG`
3. Show `BiometricPrompt` with title "Biometric login for Habte"
4. App content hidden behind "App Locked" screen until authenticated

### Privacy Mode

When enabled via Settings or Security screen:

```kotlin
window.setFlags(
    WindowManager.LayoutParams.FLAG_SECURE,
    WindowManager.LayoutParams.FLAG_SECURE
)
```

This prevents:
- Screenshots
- Screen recording
- Content visibility in recent apps overview

### Balance Masking

- Manual toggle on home screen balance card
- Auto-hide option in settings (`autoHideBalances`)
- Masked values show `••••••` instead of amounts

### Data Clearing

`FinanceRepository.clearAll()` removes all banks and transactions from memory. Triggered by sign-out in Settings.

### ProGuard (Release)

Release builds enable minification:

```gradle
release {
    minifyEnabled true
    proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
}
```

---

## 12. Theming & Localization

### Theme System

**File**: `app/src/main/java/com/mobile/ui/theme/Theme.kt`

| Mode | Background | Surface | Primary |
|---|---|---|---|
| Dark | `#070912` | `#0E1527` | `#6366F1` (Indigo) |
| Light | `#F5F5F5` | `#FFFFFF` | `#6366F1` (Indigo) |

Extended colors (`appExtendedColors()`) provide additional semantic tokens:
- `subtleText`, `placeholder`, `successGreen`, `dangerRed`, etc.

Theme selection flow:
1. User picks theme in Settings → `SettingsRepository.setTheme()`
2. Persists to SharedPreferences
3. `AppCompatDelegate.setDefaultNightMode()` applied
4. `AppTheme` composable reads theme mode and selects color scheme
5. Status bar and navigation bar colors synced

### Localization

| Resource | English | Amharic |
|---|---|---|
| App name | `Habte - Personal Finance` | `ሀብቴ` |

Amharic strings: `app/src/main/res/values-am/strings.xml`

Language setting stored in preferences (UI-level; full string localization is partial).

---

## 13. Testing

### Unit Tests

Located in `app/src/test/java/com/mobile/data/`.

#### SmsParserTest (40+ tests)

Covers:
- All 16 bank sender detections
- Credit and debit parsing
- Amount format variations (ETB, Birr, with/without decimals)
- Category classification
- Edge cases (unknown sender, OTP, zero amount, missing amount)
- Transaction ID uniqueness
- Telebirr account suffix nullification

Run:

```bash
./gradlew test
```

#### FinanceRepositoryTest

Covers:
- Add/remove banks
- Transaction add and sort order
- Remove bank cascades to transactions
- Category updates
- Clear all data
- Bank color updates

#### DataTest

Validates data model utilities and preset bank list integrity.

### Instrumented Tests

Located in `app/src/androidTest/java/com/mobile/ui/`.

#### NavigationE2ETest

End-to-end Compose UI tests:
- Default screen is Home
- Bottom nav navigation to all 5 tabs
- Secondary route navigation (AI chat, profile, tools sub-screens)
- Back navigation

#### ScreenRenderingTest

Validates that all screens render without crashes.

Run:

```bash
./gradlew connectedAndroidTest
```

---

## 14. Build & Release

### Debug Build

```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release Build

```bash
./gradlew assembleRelease
```

Requires signing configuration (not included in repo). Release builds have ProGuard enabled.

### Gradle Tasks

| Task | Description |
|---|---|
| `assembleDebug` | Build debug APK |
| `assembleRelease` | Build release APK |
| `test` | Run unit tests |
| `connectedAndroidTest` | Run instrumented tests |
| `clean` | Delete build directory |

### Build Configuration

| Setting | Value |
|---|---|
| `compileSdk` | 34 |
| `minSdk` | 26 |
| `targetSdk` | 34 |
| `jvmTarget` | 17 |
| `applicationId` | `com.mobile.habte` |
| Compose Compiler | 1.5.3 |

---

## 15. Privacy & Data Handling

### What Stays on Device

- All SMS content and parsed transactions
- Bank and account data
- User settings and preferences
- Balance and category information

### What Leaves the Device

- **Gemini AI requests only**: When using Habte AI Pro, a summarized financial snapshot (not raw SMS) is sent to Google's Gemini API
- No analytics, crash reporting, or cloud sync is implemented

### Permissions Justification

| Permission | Why |
|---|---|
| `READ_SMS` | Parse historical bank notifications |
| `RECEIVE_SMS` | Capture new transactions in real time |
| `USE_BIOMETRIC` | Secure app access |
| `INTERNET` | AI chat feature only |

### Data Retention

- Financial data exists only in memory (`StateFlow`) during app session
- Re-populated from SMS inbox on each launch via `syncHistoricalSms()`
- Settings persist in SharedPreferences across sessions
- Sign out clears all financial data from memory

---

## 16. Troubleshooting

### SMS Not Syncing

1. Verify SMS permissions are granted in Android Settings → Apps → Habte → Permissions
2. Ensure bank SMS exist in the device's SMS inbox
3. Check sender ID matches supported patterns (see [SmsParser mapping](#sender-id-mapping))
4. OTP and promotional messages are intentionally ignored

### AI Chat Not Working

1. Verify `GEMINI_API_KEY` is set in `local.properties`
2. Rebuild after adding the key (`BuildConfig` is generated at build time)
3. Check internet connectivity
4. Error message: "Gemini API key missing" → key not configured

### Biometric Lock Issues

1. Ensure device has biometric hardware enrolled
2. If biometrics unavailable, app auto-unlocks
3. Disable via Settings → Security → Biometric Login

### App Shows No Data After Restart

Expected behavior — data is in-memory only. The app re-syncs from SMS inbox on each launch when permissions are granted.

### Build Failures

| Issue | Solution |
|---|---|
| SDK not found | Set `sdk.dir` in `local.properties` |
| Compose version mismatch | Use the Compose BOM; don't pin individual Compose lib versions |
| JDK version error | Use JDK 17 |
| Gradle sync failed | Use Android Studio Hedgehog+ with AGP 8.2.2 |

---

## 17. Contributing

Contributions are welcome. Priority areas:

1. **New SMS parsers** — Add support for additional Ethiopian banks or updated SMS formats
2. **Persistent storage** — Room database for offline data retention
3. **Full Amharic localization** — Translate all UI strings
4. **Budget features** — Enhanced budget tracking with alerts
5. **Widget support** — Home screen balance widget

### Development Guidelines

- Follow existing Kotlin and Compose conventions
- Match Material 3 theming via `appExtendedColors()`
- Add unit tests for SMS parser changes
- Keep changes focused — minimal diffs preferred
- Never commit secrets (`local.properties`, API keys)

### Adding a New Bank Parser

1. Add bank to `Data.PRESET_BANKS` in `Data.kt`
2. Add sender detection in `SmsParser.parseMessage()` `when` block
3. Add unit tests in `SmsParserTest.kt`
4. Test with real SMS samples

---

## 18. License

This project is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for details.

---

## Appendix A: Quick Reference — File to Feature Map

| Feature | Primary File(s) |
|---|---|
| App entry & biometric gate | `MainActivity.kt` |
| Navigation & routing | `AppNavigation.kt` |
| SMS parsing | `SmsParser.kt` |
| Real-time SMS capture | `SmsReceiver.kt` |
| Financial state | `FinanceRepository.kt` |
| User preferences | `SettingsRepository.kt` |
| AI chat | `GeminiService.kt`, `AiChatScreen.kt` |
| Home dashboard | `HomeScreen.kt` |
| Analytics | `AnalyticsScreen.kt` |
| Data export | `ExportDataScreen.kt` |
| Security settings | `SecurityScreen.kt` |
| Theme & colors | `Theme.kt`, `Color.kt` |
| Bank presets | `Data.kt` |

## Appendix B: Environment Variables

| Variable | Location | Required | Description |
|---|---|---|---|
| `sdk.dir` | `local.properties` | Yes | Android SDK path |
| `GEMINI_API_KEY` | `local.properties` | No | Google Gemini API key for AI chat |

---

*Documentation generated for Habte Financial Tracker v1.0.0*
*Created by [Fitsumhub](https://github.com/fitsumhub)*
