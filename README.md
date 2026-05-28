# Habte - Personal Finance (ሀብቴ) 📱💰

Habte (ሀብቴ) is a premium, secure, and highly automated personal finance tracker built specifically for the Ethiopian market. It leverages native Android capabilities to provide seamless synchronization with Telebirr and other local financial services via SMS parsing, all while maintaining absolute data privacy.

---

## ✨ Key Features

### 🚀 Automated Transaction Sync
*   **Telebirr Integration:** Real-time synchronization with Telebirr notifications (Sender ID: 127).
*   **Smart SMS Parsing:** Automatically extracts amounts, balances, and transaction types from bank SMS notifications (CBE, BOA, Awash, etc.).
*   **Automatic Categorization:** Transactions are intelligently grouped into categories like Food, Utilities, Transport, and Income.

### 🛡️ State-of-the-Art Security
*   **Biometric Lock:** Secure your financial data with Fingerprint or Face ID authentication on startup.
*   **Privacy Mode (Anti-Spy):** Protects your sensitive information with `FLAG_SECURE`, preventing screenshots and blurring content in the recent apps switcher.
*   **Balance Masking:** Instantly hide your total balance and transaction amounts with a single tap or auto-hide settings.

### 🎨 Premium User Experience
*   **Modern Aesthetics:** A stunning dark-mode interface built with Jetpack Compose and Material 3, featuring glassmorphism effects and smooth micro-animations.
*   **Dynamic Theming:** Switch between Light, Dark, and System Default themes instantly.
*   **Full Localization:** Native support for both **English** and **Amharic (ሀብቴ)**.

### 📊 Financial Insights
*   **Interactive Analytics:** Visualize your spending trends with dynamic sparkline charts.
*   **Data Portability:** Export your transaction history to CSV for deep analysis or backup.
*   **Habte AI (Beta):** Smart financial assistant to help you understand your spending habits (Coming Soon).

---

## 🛠️ Technology Stack

*   **Language:** Kotlin
*   **UI Framework:** Jetpack Compose (Material 3)
*   **Architecture:** Clean Architecture with Repository Pattern
*   **State Management:** Kotlin Coroutines & StateFlow
*   **Local Storage:** SharedPreferences (Settings) & In-memory Reactive Store
*   **Security:** Android Biometric API & Window Manager Security Flags

---

## 📚 Documentation

Full project documentation — architecture, setup, API reference, SMS parsing, security, and testing — is available in **[DOCUMENTATION.md](DOCUMENTATION.md)**.

---

## 📥 Installation

1.  **Clone the Repository:**
    ```bash
    git clone https://github.com/fitsumhub/Habte-Financial-Tracker-App.git
    ```
2.  **Open in Android Studio:**
    Open the project using the latest version of Android Studio (Hedgehog or newer recommended).
3.  **Build & Run:**
    Connect an Android device (API 26+) and run the `app` module.

---

## 🔒 Privacy & Data Policy

Habte is designed with a **Privacy-First** philosophy:
*   **Local Processing:** All SMS parsing and data storage happen 100% locally on your device.
*   **No Cloud Sync:** Your financial data is never uploaded to any external servers.
*   **Permissions:** The app only requests SMS and Biometric permissions to perform its core functions.

---

## 🤝 Contributing

Contributions are welcome! If you have suggestions for new SMS parsers for Ethiopian banks or UI improvements, feel free to open an issue or submit a pull request.

---

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

---
*Created by [Fitsumhub](https://github.com/fitsumhub)*
