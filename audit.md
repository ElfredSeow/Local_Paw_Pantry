# Local Paw Pantry — Audit & Fix Register

Audit of the Kotlin / Jetpack Compose Android app (a port of the "FreshTrack" React MVP),
with the fix applied for each finding. Grouped by area; the reported user complaint
("non-stop notifications") is **Group B**.

## Verification status

- **Repository restructured** so the app builds from Git (was previously a scrambled, mislabeled
  dump; the only intact project lived inside `Local_Food_Tracker_APK-master.zip`).
- **Code changes are written and statically reviewed** (imports, references, and API usage checked
  by hand). They were **not machine-compiled here**: this environment has no network and the
  Android Gradle Plugin / dependencies are not in the offline cache, so `./gradlew` cannot resolve.
  Build + manual QA in Android Studio is the remaining verification step — see *How to verify* below.
- 23 of 25 findings are fully fixed; **H1** (i18n) and **H2** (tests) are partially done and noted.

## Summary

| Severity | Count | Status |
|---|---|---|
| Blocker | 1 | Fixed |
| Critical | 2 | Fixed |
| High | 6 | Fixed |
| Medium | 12 | Fixed |
| Low | 4 | 2 Fixed, 2 Partial |

---

## A. Build & Repository

| ID | Sev | Status | Fix |
|----|-----|--------|-----|
| B1 | Blocker | ✅ Fixed | Replaced the scrambled root with the real project tree (`app/`, `gradle/`, `settings.gradle.kts`, …). Deleted the 21 MB `app-debug.apk`, the source zip, `.idea/`, and all duplicate/junk files. Added a proper `.gitignore`. |

## B. Notification Flood (the reported complaint)

| ID | Sev | Status | Fix |
|----|-----|--------|-----|
| N1 | Critical | ✅ Fixed | `FoodApplication` now uses `enqueueUniquePeriodicWork("expiry_check", KEEP, …)` instead of `enqueue()` on every launch, so only **one** worker ever exists. A one-time `cancelAllWork()` (guarded by a pref) clears the duplicates already accumulated on users' devices. Cadence changed 15 min → **1 day** (appropriate for expiry). |
| N2 | Critical | ✅ Fixed | `ExpiryWorker` uses a **stable** notification id (`item.id.hashCode()`) so re-sends replace instead of stacking. With a single worker (N1) the read-modify-write race that defeated the `isNotified` guard no longer occurs. |
| N3 | High | ✅ Fixed | New items no longer auto-get a blanket `[30, 7, 1]`-day reminder set; the default is a single editable 3-day reminder (see F2), and the check runs daily rather than every 15 min. |
| N4 | High | ✅ Fixed | `MainActivity` now handles the `POST_NOTIFICATIONS` result; if denied it shows an in-app banner with a "Turn on" action that opens system notification settings. |
| N5 | Medium | ✅ Fixed | The Settings "Enable notifications" button (previously a no-op) now opens the app's notification settings via a shared `openAppNotificationSettings()` helper. |
| N6 | Low | ✅ Fixed | Notifications now carry a `PendingIntent` (`setContentIntent` + `setAutoCancel`) so tapping one opens the app. |

## C. Data Integrity & Correctness

| ID | Sev | Status | Fix |
|----|-----|--------|-----|
| D1 | High | ✅ Fixed | Default categories are seeded **only when the table is empty**, so deleting a default no longer resurrects it on next launch. |
| D2 | High | ✅ Fixed | New DAO `@Transaction deleteCategoryAndReassign` moves a deleted category's items to "Uncategorized" atomically; the repository calls it. |
| D3 | High | ✅ Fixed | CSV export/import now run on `Dispatchers.IO` (via the previously-unused `rememberCoroutineScope`), with the Toast posted back on the main thread — no more ANR risk. |

## D. Missing Features (vs. MVP)

| ID | Sev | Status | Fix |
|----|-----|--------|-----|
| F1 | Medium | ✅ Fixed | Added the horizontal icon-picker row (11 food emojis) that writes `iconId`; shown when no photo is chosen. |
| F2 | Medium | ✅ Fixed | Added a reminder editor (add / edit days-before / remove) on the Add screen. Also resolves N3. |
| F3 | Medium | ✅ Fixed | Upcoming screen now has a search bar and an "All caught up!" empty state. |
| F4 | Medium | ✅ Fixed | "Recipe Ideas" button now launches a web recipe search (`ACTION_VIEW`). |
| F5 | Medium | ✅ Fixed | CSV export embeds the image as base64 (portable); import decodes it to a fresh internal file. Legacy on-device paths are still read for backward compatibility. |

## E. Storage & Persistence

| ID | Sev | Status | Fix |
|----|-----|--------|-----|
| S1 | Medium | ✅ Fixed | Images are downsampled (≤ 500 px, JPEG 85%) on save, and the file is deleted when the item is deleted. |
| S2 | Medium | ✅ Fixed | `exportSchema = true` with `room.schemaLocation` configured; migration policy documented (no destructive fallback, so a missing migration fails loudly in dev rather than wiping data). |

## F. UI & Accessibility

| ID | Sev | Status | Fix |
|----|-----|--------|-----|
| U1 | High | ✅ Fixed | `TextFieldDefaults.textFieldColors(...)` (removed in Material3 1.2) replaced with `TextFieldDefaults.colors(...)` on both search fields. |
| U2 | Medium | ✅ Fixed | Dark text on the yellow "Expiring Soon" tag; low-contrast `LightGray` body text replaced with a theme `onSurfaceVariant` (~6:1 on white). |
| U3 | Medium | ✅ Fixed | Meaningful `contentDescription`s on actionable icons (delete, search, category, date, dismiss). |
| U4 | Medium | ✅ Fixed | Real `darkColorScheme` wired to the system setting; hardcoded `Color.White/Gray/LightGray` on cards/text swapped for `MaterialTheme.colorScheme.*`. |
| U5 | Medium | ✅ Fixed | Quantity field uses a numeric keyboard, accepts digits only, and Save is disabled until name + quantity are valid. |

## G. Release Readiness & Hygiene

| ID | Sev | Status | Fix |
|----|-----|--------|-----|
| H1 | Low | ◑ Partial | `applicationId` changed to `com.localpawpantry.foodtracker`; release build enables `minify` + `shrinkResources` with Gson/Room keep rules added; `app_name` set to "Local Paw Pantry". **Follow-up:** most in-code UI strings are still hardcoded — full `strings.xml` extraction for localization remains. |
| H2 | Low | ◑ Partial | Added `DateUtilsTest` covering the trickiest logic (day math + timeline grouping). **Follow-up:** ViewModel and CSV round-trip tests need Robolectric/instrumentation (Context dependencies) and are not yet added. |
| H3 | Low | ✅ Fixed | Removed the dead `myMutableStateOf` helper; junk/duplicates removed as part of B1. |

---

## Suggested fix order (as applied)

0. **Unbreak & verify** — B1 (+ U1 confirmed on build)
1. **Stop the flood** — N1, N2, N4, N5, N6
2. **Data correctness** — D1, D2, D3
3. **Feature parity** — F1, F2 (closes N3), F3, F4, F5
4. **Storage & a11y** — S1, S2, U2, U3, U4, U5
5. **Hardening** — H1, H2, H3

## How to verify (build wasn't possible in this sandbox)

1. Open the project in Android Studio (it will fetch the AGP/dependencies and the JDK 21 toolchain
   this offline sandbox couldn't).
2. `./gradlew testDebugUnitTest` — runs `DateUtilsTest`.
3. `./gradlew assembleDebug` — confirms compilation, including the U1 Material3 API fix and the
   Room `@Transaction` codegen.
4. `./gradlew assembleRelease` — confirms minify/shrink + ProGuard keep rules don't break Gson/Room.
5. Manual QA of the flood fix: add several items, confirm you get at most one notification per
   reminder and that re-opening the app doesn't multiply them.

*Read-only findings were produced first; this document records the fixes subsequently applied on
branch `feat/bug-fixes`.*
