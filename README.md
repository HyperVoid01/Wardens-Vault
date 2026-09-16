# Warden's Vault

A fantasy-themed personal budget tracker for Android, built with Kotlin and Room. Track spending against categories, set monthly goals, and review your history. The app is themed around a pixel-art RPG where your budget is your castle.

## Features

- **Account system** - register and log in with a username/password; sessions persist across app restarts.
- **Categories** - create custom spending categories.
- **Expense entries** - log an expense with a category, date, start/end time, description, and amount.
- **Photo attachments** - optionally attach a photo to an expense entry, captured directly from the device camera.
- **Monthly budget goals** - set a minimum and maximum monthly spending goal; the Home screen shows live progress against it.
- **Expense history** - view all expenses for a user-selectable date range, with photos viewable inline.
- **Category totals** - view total spend per category for a user-selectable date range.
- **Offline storage** - all data is stored locally via Room (SQLite); no network/account sync.

## The Warden's Vault theme

The UI is skinned as a fantasy dashboard. The bottom navigation bar (bed / map / helmet icons) and the in-app map menu translate directly to app features:

Castle -> Home dashboard
Dungeon -> Log a new expense
Treasury -> Category totals
Market -> Set monthly budget goals
Reports -> Expense history
Armory -> Manage categories
Profile -> Account info / log out

The Home screen's "Gold" figure and progress bar reflect real data: Gold shows remaining budget (max goal minus spent this month, or total spent if no goal is set yet), and the bar fills based on spend and your monthly max goal.

## Tech stack

- **Language**: Kotlin
- **Min SDK**: 34 
- **Target/Compile SDK**: 37.1
- **Persistence**: Room 2.8.4 (SQLite), KSP for annotation processing
- **UI**: Android Views (XML layouts), Material Components (`BottomNavigationView`)
- **Architecture**: `AndroidViewModel` + Kotlin coroutines for async DB access, `SharedPreferences`-backed session management

## Data model

- `User` - id, fullName, username, password
- `Category` - id, userId, name
- `Expense` - id, userId, categoryId, amount, date, startTime, endTime, description, photoPath (nullable)
- `BudgetGoal` - id, userId, minGoal, maxGoal

All entities are scoped to a `userId` foreign key, so each account's data is isolated. Expense photos are stored on device (app-private external storage) with only the file path saved in the database; a `FileProvider` handles camera-intent file sharing.

## Setup

1. Clone the repo and open it in Android Studio.
2. Let Gradle sync (requires Android SDK Platform 37 installed via SDK Manager).
3. Run on an emulator or device running API 34+.