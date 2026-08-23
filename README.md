# Paw Pantry

Paw Pantry is a local-only Android app for tracking food inventory and expiry dates. There is
no server and no account: everything lives in an on-device Room database, and the only way
data leaves the device is if you explicitly export it yourself (see CSV export below).

Package name: `com.example.foodtracker`.

## What it does

- **Inventory tracking** — add food items with a name, category, quantity, and expiry date.
  Each item can have a photo (picked from the device gallery) or falls back to a category
  emoji icon when no photo is set.
- **Categories** — a small set of categories ships by default (Uncategorized, Produce, Meat,
  Dairy, Pantry, Frozen); you can add your own from Settings. Deleting a category reassigns
  its items to "Uncategorized" rather than orphaning them.
- **Expiry reminders and notifications** — new items get reminder tiers at 30/7/1 days before
  expiry. A daily `WorkManager` background job checks every item's reminders against today's
  date and posts a local notification when one is due (separate notification channels for
  "expired" vs. "upcoming", grouped into a single summary when several fire at once). This is
  a local notification only; nothing is sent anywhere.
- **Upcoming view** — items expiring within the next 14 days are grouped into Today, Tomorrow,
  Next Week, or Later, so what needs attention soonest is easy to see.
- **Search** — filter the inventory list by name.
- **CSV import/export** — back up your whole inventory to a CSV file (or restore from one)
  through the system file picker (Storage Access Framework). This is the one deliberate way
  data can leave or enter the device.
- **Quick recipe search** — each item has a "Recipe Ideas" shortcut that opens your browser to
  a web search for "`<item name>` recipe". This hands off to whatever browser/search app is
  installed on the device; the app itself does not call any recipe API or make network
  requests of its own.

There is no release build or app store distribution — this is a debug-only project you build
and install yourself (see below).

## Requirements

- **Android Studio** (or a standalone JDK 17+ and the Android SDK / command-line tools) to
  build the project.
- **compileSdk / targetSdk:** 35 (Android 15)
- **minSdk:** 24 (Android 7.0 Nougat)

These are declared in [`app/build.gradle.kts`](app/build.gradle.kts).

## Building

Clone the repo and run the Gradle wrapper from the project root:

```sh
./gradlew assembleDebug
```

This produces a debug APK at `app/build/outputs/apk/debug/app-debug.apk`, which you can install
with `adb install app/build/outputs/apk/debug/app-debug.apk` or by opening the project in
Android Studio and running it on a device or emulator.

If Android Studio doesn't already know where your SDK is, create a `local.properties` file in
the project root (it's gitignored, and never committed) with:

```properties
sdk.dir=/path/to/your/Android/sdk
```

## Project layout

```
app/src/main/java/com/example/foodtracker/
├── FoodApplication.kt        # Application subclass: builds the DB/repository, schedules
│                              the daily expiry-check work request
├── MainActivity.kt           # Single-activity host, bottom navigation between screens
├── MainViewModel.kt          # Shared ViewModel: inventory/category state, search, CRUD
├── data/                     # Room entities (FoodItem, Category), DAO, database,
│                              Gson<->Room type converters, repository
├── ui/screens/                # Inventory, Upcoming, Add Food, Settings screens (Compose)
├── ui/components/             # Reusable Compose components (e.g. the food item card)
├── ui/theme/                  # Compose Material 3 theme
├── util/                      # DateUtils (expiry-date math/formatting), CsvHelper
│                              (CSV import/export)
└── worker/                    # ExpiryWorker: the daily WorkManager job that posts
                                 expiry notifications
```

Unit tests live under `app/src/test/java/com/example/foodtracker/`; instrumented
(on-device/emulator) tests live under `app/src/androidTest/java/com/example/foodtracker/`.

## Running tests

Unit tests run on the host JVM — no device or emulator required:

```sh
./gradlew testDebugUnitTest
```

Instrumented tests need a connected device or running emulator:

```sh
./gradlew connectedAndroidTest
```

Static analysis (Android Lint):

```sh
./gradlew lintDebug
```

## Continuous integration

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) runs on every push and pull request: it
checks out the repo, sets up Temurin JDK 21 and Gradle (with dependency/build caching), then
runs `assembleDebug`, `testDebugUnitTest`, and `lintDebug`, uploading the lint HTML report as a
build artifact. It does not run instrumented tests, since those need a device/emulator that
CI doesn't provision here.
