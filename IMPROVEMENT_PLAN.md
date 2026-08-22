# Paw Pantry — Remediation Plan

Merged output of six parallel read-only audits: data layer, UI/state, background work
and notifications, build and repo hygiene, adversarial crash hunt, and product/architecture.
Findings are de-duplicated and ordered so each phase unblocks the next.

**47 items · 6 critical · 16 high · 0 tests that assert anything · 0 CI pipelines**

All file claims were verified against the intact project tree extracted from
`Local_Food_Tracker_APK-master.zip`, since the repository itself is not currently buildable
(see Phase 0).

---

## Phase 0 — Restore the repository

*Gate: `./gradlew assembleDebug` runs from a fresh clone.*

Nothing else in this plan can be verified until this is done.

All three content commits were made through GitHub's web "Add files via upload" dialog
from a browser Downloads folder. The browser had already auto-renumbered colliding
filenames — `download (1)`, `build.gradle (13).kts`, `ic_launcher (5).webp` — so every
filename in the repo is detached from its actual contents. Verified by SHA-256 matching
all 45 root files against the intact tree.

| Repo filename | Actually contains |
|---|---|
| `MainActivity.kt` | `app/proguard-rules.pro` |
| `AndroidManifest.xml` | `app/.gitignore` — one line, `/build` |
| `Theme.kt` | `FoodApplication.kt` |
| `FoodApplication.kt` | `app/build.gradle.kts` |
| `gradle-wrapper.jar` | `ic_launcher_background.xml` (a vector drawable) |
| `gradlew.bat` | `mipmap-hdpi/ic_launcher.webp` |
| `ic_launcher (7).webp` | `ui/screens/AddFoodScreen.kt` |
| `Food Tracking App JS` | `worker/ExpiryWorker.kt` |

…and 37 more.

**All nine Gradle bootstrap files are absent** outside the committed zip: root and app
`build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `libs.versions.toml`, both
wrapper files, and `gradlew`. Also missing: the unit test, nine mipmap density variants,
and every file under `res/values/`.

| Sev | Item | Fix |
|---|---|---|
| Critical | Repository cannot be cloned and built by anyone | Extract the zip in place, delete the 45 scrambled root files, commit the real tree from a local git client |
| High | 21 MB unsigned debug APK + zip committed to git | `git filter-repo` both blobs, gitignore `*.apk`/`*.zip`, ship via GitHub Releases |
| High | No CI at all | `.github/workflows/ci.yml` running `assembleDebug testDebugUnitTest lintDebug` |
| Medium | README is three sentences | Build instructions, SDK levels, feature list, license |

---

## Phase 1 — Stop losing user data

*Gate: CSV round-trip and DB upgrade tests pass.*

| Sev | Item | Fix |
|---|---|---|
| Critical | `Converters.toReminderList` has no try/catch and runs on every read of the food table. One malformed `reminders` value crashes Inventory, Upcoming and ExpiryWorker on every launch, forever, with no in-app recovery | Return `emptyList()` on failure, matching `DateUtils` |
| Critical | Gson returns `null` for an empty reminders cell *without throwing*, slipping past the existing catch into a non-null Kotlin `List`. Kotlin's null check fires inside an unguarded `viewModelScope.launch` | `?: emptyList()`, plus a `CoroutineExceptionHandler` |
| Critical | No Room migration strategy: `version = 1`, `exportSchema = false`, no migrations, no destructive fallback. First schema change crashes every existing user at launch | `exportSchema = true`, commit schema JSON, write explicit migrations |
| Critical | DatePicker returns UTC midnight but both conversions use `ZoneId.systemDefault()`, so expiry dates save one day early across the Americas | Use `ZoneOffset.UTC` both directions |
| High | A newline in a food name corrupts the CSV: the name field isn't `singleLine`, `escapeCsv` doesn't escape newlines, and import uses `readLine()` before CSV-aware parsing | `singleLine = true`; parse across physical lines, splitting only outside quotes |
| High | One bad row aborts the import loop and silently discards the rest; the toast still reports success | Catch per row, continue, report skipped rows |
| High | Import overwrites by ID with no confirmation and no transaction | One `@Transaction`; confirm overwrites; decide restore-vs-merge |
| High | CSV read and write run on the main thread. `rememberCoroutineScope()` is declared and never used | `scope.launch(Dispatchers.IO)` |
| High | Deleting a category orphans its items forever — no FK, no reassignment (the code comment says it should), no edit screen to fix them, no confirmation | Reassign in one transactional DAO query; add confirmation |
| High | Default categories re-seed on every launch, so deleting one silently reverts | Gate seeding on a first-run flag or empty-table check |
| Low | Import bypasses the quantity floor the UI enforces | Clamp on import |
| Low | `id` and `expiryDate` exported unescaped | Escape every field uniformly |

The category-orphaning bug was reported independently by four of the six audits.

---

## Phase 2 — Make notifications trustworthy

*Gate: one work chain, one notification per reminder.*

| Sev | Item | Fix |
|---|---|---|
| Critical | `scheduleExpiryCheck()` builds a fresh UUID request and calls plain `enqueue()` on every `Application.onCreate()` — every cold start, reboot, and WorkManager-spawned process. Chains accumulate permanently | `enqueueUniquePeriodicWork(..., KEEP, ...)` |
| High | `isNotified` is set even when `notify()` silently no-ops under denied permission, consuming that reminder tier forever | Check `areNotificationsEnabled()` first; only persist on real delivery |
| High | Notification IDs from `System.currentTimeMillis().toInt()` collide and overwrite each other | Stable ID per reminder |
| High | 15-minute polling for a value that changes once per calendar day; Doze defers it anyway | Daily period with an initial delay to a sensible local hour |
| Medium | Permission result discarded, re-requested on every activity recreation, no path to system settings | Capture result, gate on rationale, offer Settings deep link |
| Medium | No `setContentIntent`, `setAutoCancel`, grouping, or real icon | Deep link to item, auto-cancel, group with summary |
| Medium | `(applicationContext as FoodApplication)` cast breaks all worker testing | Inject via `WorkerFactory` / `HiltWorker` |
| Low | `allowBackup="true"` with template rules cloud-backs-up the DB and every photo, contradicting the "fully local" positioning | Decide deliberately and make code and copy agree |

---

## Phase 3 — Fix what users touch

*Gate: no dead controls, no silent failures.*

| Sev | Item | Fix |
|---|---|---|
| High | Two shipped buttons have empty click handlers: "Recipe Ideas" on every card and "Enable Push Notifications" in Settings | Wire both or remove both |
| High | No way to edit an existing item; any typo requires delete and re-add | Add an `add/{itemId}` route reusing the add screen |
| High | Add form uses plain `remember`, so a single tab tap wipes it including the picked photo | Hoist to `SavedStateHandle`, or `rememberSaveable` |
| High | No confirmation or undo on any destructive action; delete target is 24 dp against a 48 dp minimum | Confirm dialog or snackbar with Undo |
| High | Blank name makes Save a silent no-op; quantity accepts `-5` and silently rewrites bad input to `1` | Numeric keyboard, clamp, inline errors, disable Save |
| Medium | Duplicates always created; `REPLACE` is unreachable on the add path since every item gets a fresh UUID | Match on name + category + expiry, offer merge |
| Medium | No snackbar host anywhere; failed export produces no feedback at all | Add a SnackbarHost, route errors through it |
| Medium | `darkTheme` is computed and discarded; ~30 hardcoded `Color.*` literals will render wrong once it's honored | Fix literals first, then branch on `darkTheme` |
| Medium | `UpcomingScreen` missing `key = { it.id }` that `InventoryScreen` has | One line |
| Low | "EXPIRING SOON" is white on yellow (~1.6:1); null content descriptions; `strings.xml` holds only `app_name` | Dark text on yellow, real descriptions, extract strings |
| Low | Empty state flashes before the first DB emission | Explicit loading flag |

---

## Phase 4 — Earn the right to add features

*Gate: a feature can be added without touching `Application`.*

Not defects — the structural work that makes everything above affordable.

- **Put a seam behind the README's promise.** It invites users to "connect to other
  databases," but `FoodRepository` is concrete and constructed inline in `Application`.
  Extract an interface + Room implementation, introduce Hilt (also fixes the worker cast).
- **Split the single app-wide ViewModel** into per-screen ViewModels sharing the repository
  through DI. Sort/filter state, edit drafts and batch selection all land here otherwise.
- **Expiry rules are implemented three times** — `UpcomingScreen`, `FoodItemCard`,
  `ExpiryWorker`. Extract one use-case layer consumed by UI and worker.
- **Write the first real tests.** Both files are templates asserting `2 + 2 == 4`. Order by
  value: `DateUtils` boundaries (0/1/7/14, unparseable), `CsvHelper` round-trips including
  the newline and comma cases, `Converters`, then `FoodDao` on an in-memory DB.
- **Make a release build possible.** `isMinifyEnabled = false`, no signing config,
  fully-commented-out ProGuard template. R8 would break Gson today — `Converters` uses
  `TypeToken<List<Reminder>>` and needs keep rules for the generic signature.
- **Refresh dependencies.** Compose BOM pinned to Feb 2024, Kotlin 2.0.0, Room 2.6.1. More
  urgently, the wrapper pins Gradle 9.4.1 against AGP 8.8.0 — verify that pairing before
  assuming a clean sync. `appcompat` and `material` are declared but never imported.
- **Add missing indices** on `category` and `expiryDate`, and a real foreign key — which is
  why the Phase 1 orphaning bug has nowhere natural to live.
- **Settle the app's identity.** `app_name` is "Food Tracker", the in-app header says
  "FreshTrack", the repo says Paw Pantry. Application ID is still `com.example.foodtracker`,
  which cannot be published to Play.

---

## Four decisions to settle before Phase 1

1. **Is this a pet app or a pantry app?** Nothing in the code is pet-specific. Committing to
   "Paw Pantry" literally means a data-model fork (pet profiles, feeding schedules, portions,
   allergy notes). Committing to a generic pantry means renaming. Both are fine; drifting isn't.
2. **Does CSV import restore or merge?** Today it's an unannounced overwrite-by-ID —
   defensible as *restore*, indefensible as *import*. Determines whether imported rows keep
   their IDs and what the confirmation says.
3. **Is the data actually local-only?** The README says local; the manifest ships Google Auto
   Backup for the database and every photo.
4. **Sideloaded APK, or Play Store?** Play requires a real application ID, signing config, and
   release build — all Phase 4. If sideloading stays, at least move to signed release builds
   published through GitHub Releases.
