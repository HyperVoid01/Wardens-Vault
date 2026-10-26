# Warden's Vault

A gamified personal budgeting app for Android. You play as a Warden guarding your dungeon: recurring bills become enemies you defeat by paying them, good financial habits earn XP and Gold, and Gold buys cosmetics for your knight and bedroom. The budgeting tools come first; the fantasy theme is there to make them worth opening every day.

| Team member | Student number |
|---|---|
| Ethan Ramsumair | ST10439085 |
| Sooriyan Gramani | ST10443023 |
| Kairav Naicker | ST10440071 |

---

## Table of contents

- [Features](#features)
  - [Accounts and setup](#accounts-and-setup)
  - [Expenses](#expenses)
  - [Budgets and limits](#budgets-and-limits)
  - [Reports](#reports)
  - [Income, net worth and forecasting](#income-net-worth-and-forecasting)
  - [Bills and the enemy system](#bills-and-the-enemy-system)
  - [Gamification](#gamification)
  - [Customisation](#customisation)
  - [Alerts and notifications](#alerts-and-notifications)
  - [Data validation](#data-validation)
- [Innovative features](#innovative-features)
- [Navigation](#navigation)
- [Tech stack](#tech-stack)
- [Data model](#data-model)
- [Getting started](#getting-started)
- [Project structure](#project-structure)

---

## Features

### Accounts and setup

- **Registration** with full name, username, email, password and password confirmation. Usernames must be unique, and passwords must be at least 8 characters with a letter and a number.
- **Login** with either a username or an email address. Clear error messages are shown for empty fields or wrong details.
- **Secure passwords** - authentication is handled by Firebase Authentication, so the app never stores passwords itself.
- **Persistent sessions** - users stay signed in across app restarts.
- **Initial setup** after first login to enter monthly income, current savings and a monthly spending budget. Every field is optional and the whole step can be skipped.
- **Default categories** (Groceries, Transport, Entertainment, Shopping, Healthcare, Education, Utilities) are created automatically for new users.

### Expenses

- **Add expenses** with amount, date, category and description. Start and end times are optional.
- **Recurring expenses** with a start date, optional end date and a weekly, monthly or yearly frequency. They are saved once and each repeat is counted automatically in the period being viewed.
- **Receipt photos** - take a photo with the camera or pick an existing image, then view, replace or remove it.
- **Built-in calculator** - a pixel-art calculator for adding up receipts and sending the result straight into the amount field.
- **Manage categories** - add, rename and delete. A category in use cannot simply be deleted; its records must be moved to another category first.
- **Expense history** for a chosen start and end date, filterable by category. Each entry shows its date, description, category, amount and whether a receipt is attached. Tap to view the receipt, long-press to delete (with confirmation).
- **Calendar view** - days with expenses are highlighted, and tapping a day lists what was spent.

### Budgets and limits

- **Monthly budget** with total, amount spent, amount remaining and percentage used. Each month starts a fresh period while past months' records are kept.
- **Category spending limits** with a status for each category: *Safe*, *Approaching* (75% or more) or *Exceeded*.
- **Budget progress dashboard** on the Home screen showing the monthly budget, category progress, upcoming bills, savings goal progress, net worth and a 14-day balance estimate.

### Reports

- **Daily spending graph** - a stacked bar chart for a weekly, monthly or custom date range, with each category colour-coded. A short summary points out the busiest day, the biggest category and weekend vs weekday spending.
- **Category spending totals** for any selected date range.

### Income, net worth and forecasting

- **Income tracking** - add, edit, delete and view income entries with amount, source, date and type (Salary, Freelance, Bonus, Other).
- **Net worth** calculated as *Total Assets - Total Liabilities*.
  - Assets: Cash, Savings, Investments, Other assets
  - Liabilities: Loans, Credit card debt, Other debts
  - A snapshot is saved each day so changes can be tracked over time.
- **Cash flow forecast** for 7, 14 or 30 days. It combines current Cash and Savings, average income from the last 90 days, unpaid bills due in the window, and average spending from the last 30 days (or the monthly budget if there is no history yet). A warning appears when the estimate drops below R500 or goes negative.

### Bills and the enemy system

- **Recurring bills** with name, amount, due date, frequency (weekly, monthly or yearly), category and payment status.
- **Every bill is an enemy** - Rent becomes an Ogre, Electricity a Beast, Water a Monster, Internet a Wraith, Insurance a Skeleton.
- **Health bars** - a partial payment wounds the enemy; paying the full amount defeats it.
- **Rewards** - defeating an enemy gives XP and Gold, with bonus Gold for paying on or before the due date.
- **Respawning** - a defeated enemy returns once its due date passes, ready for the next billing cycle.
- Every bill payment is also saved as a normal expense, so budgets stay accurate.

### Gamification

- **XP** for logging expenses, paying bills, staying within the monthly budget, completing goals, unlocking achievements and daily logins.
- **Gold** as a cosmetic-only currency. It never affects any financial calculation.
- **Levels** earned from XP, each with a title from *Squire* up to *Vault Overlord*.
- **Daily login rewards** with a streak counter. Rewards grow each day and every 7th day adds a bonus. Each day can only be claimed once, and missing a day resets the streak.
- **Savings goals** with a name, target amount, current amount and optional deadline. Add savings over time and track progress; completing a goal gives a reward.
- **Achievements** such as *First Coin Spent*, *First Blood* (first bill defeated), *Monster Slayer*, *Goal Getter* and *Week Warrior*, each with its own XP and Gold reward.

### Customisation

- **Armory** - spend Gold on helmets, armour, weapons, shields, Warden colours, bedroom themes, titles and bedroom decorations. Higher-tier items unlock at higher levels.
- **Bedroom** - the Warden's room shows the knight wearing the equipped gear in the chosen colour, the selected room theme, equipped decorations and the current title.
- **Profile** - username, level and title, XP, Gold, equipped items, expenses logged and bills defeated, with links to achievements and settings.

### Alerts and notifications

- **Notice board** on the Home screen listing current alerts.
- **System notifications** for:
  - monthly budget at 75%, 90% and over 100%
  - category limits approaching or exceeded
  - bills due within 3 days, due today or overdue
  - cash flow warnings
  - achievements unlocked
- Each alert is only sent once.
- **Settings** let users turn all notifications, or each type, on and off.

### Data validation

- Amounts, budgets and limits cannot be negative, and required fields cannot be left empty.
- Duplicate category names are rejected.
- Date ranges must have a start date before the end date.
- Deleting expenses, categories, bills, goals and income asks for confirmation first.

---

## Innovative features

These features were chosen from our research into existing budgeting apps:

| Feature | Inspired by | How it fits the theme |
|---|---|---|
| Daily login rewards | Fortune City | Gold and XP each day, with a 7-day streak bonus |
| Budget alerts | Wallet | Notice board and notifications at 75%, 90% and 100% |
| Goal-based budgeting | Wallet | Savings goals that reward XP and Gold when completed |
| Net worth calculation | Vault22 | Assets minus liabilities, tracked daily |
| Cash flow forecasting | Vault22 | 7, 14 or 30-day balance estimate with low-balance warnings |

---

## Navigation

The bottom navigation bar has three tabs: **Home | Map | Profile**.

The **Map** is the main hub. Users tap locations instead of moving a character:

| Location | Purpose | Contains |
|---|---|---|
| Bedroom | Your Warden | Knight with equipped gear, decorations and title |
| Castle | Budgets & Goals | Monthly budget, category limits, savings goals |
| Dungeon | Bills | Recurring bill enemies |
| Treasury | Income & Net Worth | Income, net worth, cash flow forecast |
| Market | Expenses | Add expense, history, calendar, categories |
| Reports | Analytics | Spending graph, category totals, history, calendar |
| Armory | Customisation | Cosmetic shop |
| Profile | Account | Stats, achievements, settings, log out |

---

## Tech stack

| Area | Technology |
|---|---|
| Language | Kotlin |
| IDE | Android Studio |
| Min SDK | 34 |
| Target / Compile SDK | 37 |
| Authentication | Firebase Authentication |
| Database | Cloud Firestore |
| Async | Kotlin coroutines (`kotlinx-coroutines-play-services` for `await()` on Firebase tasks) |
| UI | Android Views (XML and code-built screens), Material Components |
| Architecture | `AndroidViewModel` + DAO classes over Firestore; `SharedPreferences` for settings and flags |

---

## Data model

All data belongs to a user and is stored under their own Firestore document, so one account can never see another's data.

```
users/{uid}                          User profile
users/{uid}/categories/{id}          Category
users/{uid}/expenses/{id}            Expense
users/{uid}/budget_goals/goal        Monthly budget
users/{uid}/player_stats/stats       XP, Gold, streaks, expenses logged
users/{uid}/category_limits/{id}     Category spending limits
users/{uid}/incomes/{id}             Income entries
users/{uid}/bills/{id}               Recurring bills (enemies)
users/{uid}/bill_payments/{id}       Defeated enemy history
users/{uid}/savings_goals/{id}       Savings goals
users/{uid}/net_worth_items/{id}     Assets and liabilities
users/{uid}/net_worth_snapshots/{d}  One net worth value per day
users/{uid}/unlocked_achievements/   Achievements earned
users/{uid}/owned_items/{key}        Cosmetics owned and equipped
usernames/{username}                 Username to account lookup for login
```

Receipt photos are stored on the device in app-private storage, with only the file path saved in the database. A `FileProvider` shares the file with the camera app.

---


## Project structure

```
app/src/main/java/com/soo/wardensvault/
|-- Activities          Login, Registration, Setup, Home, MapMenu, Hub, Bedroom, Profile,
|                       Settings, CreateExpense, ExpenseList, Calendar, CreateCategory,
|                       CategoryTotals, CategoryLimits, BudgetGoal, Goals, Income,
|                       NetWorth, Forecast, Bills, Armory, Achievements, SpendingGraph
|-- Managers            GamificationManager, BillManager, AchievementManager,
|                       NetWorthManager, Forecaster, AlertEngine, NotificationHelper
|-- Catalogs            EnemyCatalog, CosmeticCatalog, CategorySeeder
|-- Data                AppDatabase, Daos, entity classes (User, Expense, Category, Bill ...)
|-- UI helpers          ScreenActivity, UiKit, BottomNav, WardenView, SpendingChartView,
                        CalculatorDialog, WallDrawable
```
