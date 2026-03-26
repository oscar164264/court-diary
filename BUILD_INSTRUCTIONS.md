# Court Diary – Build & Run Instructions

## Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17 (bundled with Android Studio)
- Android SDK 34 installed
- Android device or emulator running Android 8.0+ (API 26+)

---

## Step 1 – Open the Project
1. Launch **Android Studio**
2. Click **File → Open**
3. Navigate to and select the `mini_court_diary` folder
4. Wait for Gradle sync to complete (first sync downloads dependencies ~5 min)

---

## Step 2 – Build the Project
In the Android Studio menu:
```
Build → Make Project   (Ctrl+F9 / Cmd+F9)
```
Or via terminal inside Android Studio:
```bash
./gradlew assembleDebug
```

---

## Step 3 – Run on a Device / Emulator

### Option A – Android Studio (recommended)
1. Connect your Android phone via USB (enable USB Debugging in Developer Options)
   **OR** create an emulator: **Tools → Device Manager → Create Virtual Device**
2. Select your device from the toolbar dropdown
3. Click the green **Run** button (▶) or press **Shift+F10**

### Option B – Command line
```bash
# Install debug APK to connected device
./gradlew installDebug

# Then launch the app
adb shell am start -n com.courtdiary.debug/.MainActivity
```

---

## Step 4 – Build Release APK

### Debug APK (signed with debug key – can be installed directly)
```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release APK
1. **Build → Generate Signed Bundle / APK**
2. Choose **APK** → Next
3. Create or select a keystore file
4. Choose **release** build variant → Finish

Output: `app/build/outputs/apk/release/app-release.apk`

---

## Installing the APK on a Phone
1. Enable **Install from Unknown Sources** on the phone
   *(Settings → Apps → Special App Access → Install Unknown Apps)*
2. Transfer `app-debug.apk` to the phone (USB, email, cloud)
3. Tap the APK file to install

---

## Project Structure
```
app/src/main/java/com/courtdiary/
├── CourtDiaryApp.kt          – Application class (notification channel, WorkManager)
├── MainActivity.kt           – Entry point, permission request, Compose host
│
├── model/
│   └── Case.kt               – Room entity
│
├── database/
│   ├── CaseDao.kt            – All SQL queries
│   └── CaseDatabase.kt       – Room database singleton
│
├── repository/
│   └── CaseRepository.kt     – Data layer abstraction
│
├── viewmodel/
│   └── CaseViewModel.kt      – All UI state, CRUD, settings, backup
│
├── notification/
│   ├── NotificationHelper.kt     – Creates channel, posts notifications
│   ├── CaseReminderWorker.kt     – WorkManager worker (daily check)
│   └── NotificationScheduler.kt  – Schedules/cancels WorkManager job
│
└── ui/
    ├── theme/          – Colors, Typography, Material3 theme
    ├── navigation/     – NavHost + bottom navigation
    └── screens/
        ├── DashboardScreen.kt    – Today + Upcoming cases
        ├── AllCasesScreen.kt     – Search, filter, full list
        ├── CalendarScreen.kt     – Monthly calendar + day cases
        ├── AddEditCaseScreen.kt  – Add / Edit case form
        ├── CaseDetailScreen.kt   – Full details + Call/WhatsApp
        └── SettingsScreen.kt     – Notifications, dark mode, backup
```

---

## Features Implemented
| Feature | Status |
|---------|--------|
| Case CRUD (add, edit, delete) | ✅ |
| Dashboard with Today + Upcoming | ✅ |
| Urgency colour coding (RED/ORANGE/GREEN) | ✅ |
| Search by case number / client / phone | ✅ |
| Filter: All / Today / Upcoming / Past | ✅ |
| Monthly calendar with case highlights | ✅ |
| Calendar day tap → show cases | ✅ |
| Case detail screen | ✅ |
| Call Client intent (ACTION_DIAL) | ✅ |
| WhatsApp deep link (wa.me) | ✅ |
| WorkManager daily notifications | ✅ |
| Notification for today + tomorrow | ✅ |
| Room database with unique case number | ✅ |
| DataStore settings (notifications, dark mode) | ✅ |
| JSON backup (export via share sheet) | ✅ |
| JSON restore (import from file picker) | ✅ |
| Dark / Light mode toggle | ✅ |
| Sample data on first launch | ✅ |
| Material Design 3 theme | ✅ |
| Bottom navigation bar (5 tabs) | ✅ |
| Form validation (required fields, phone, unique case#) | ✅ |
| DatePicker dialogs | ✅ |
| Smooth navigation animations | ✅ |

---

## Tech Stack
- **Language**: Kotlin 1.9.22
- **UI**: Jetpack Compose + Material3
- **Architecture**: MVVM (AndroidViewModel + StateFlow)
- **Database**: Room 2.6.1 (SQLite)
- **Background**: WorkManager 2.9.0
- **Settings**: DataStore Preferences
- **Navigation**: Navigation Compose 2.7.7
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
