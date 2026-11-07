# Pocket Ledger Android app

Native Android Studio starter project written in Kotlin and Jetpack Compose.

## Build and install

1. Open the `android` folder in Android Studio.
2. Allow Gradle sync to finish. The project uses Android Gradle Plugin 8.7.3, Kotlin 2.0.21, and compile SDK 35.
3. Connect an Android phone with USB debugging enabled, select it as the run target, and press **Run**. Android Studio will build and install the app.

You can also use **Build > Build Bundle(s) / APK(s) > Build APK(s)** to create an APK for manual installation.

## What works in this starter

- First-run setup asks for a name and monthly budget. The ledger starts empty; no demo transactions are preloaded.
- Home dashboard, live spending by category, recent transactions, and reports.
- Add, edit, and delete income and expenses with amount, date, category, payment method, note, and mandatory / non-mandatory / needs-review label.
- Tap a budget category to see its expense history, or tap its **+** button to add a new expense with that category preselected.
- Search transactions and view transaction details.
- Edit your name and monthly budget in Profile, and switch between light and dark mode.
- Local transaction storage using app-private preferences. No account or network permission is requested.

Receipt storage, editable category budgets, automatic recurring detection, reminders, and encrypted database storage are not implemented yet. The project disables Android cloud backup to avoid exporting financial entries through system backup.
