# Progress Log

## 2026-08-23 — Comprehensive Code Review & Fixes

### Issues Fixed

#### P0 — Critical
1. **Resource Leak** — `NetworkStats.queryDetails()` was never closed → Now uses `.use {}` for automatic close
2. **Main Thread I/O** — All `NetworkStatsManager` queries ran on Main dispatcher → Moved to `Dispatchers.IO` with `withContext`
3. **Release Build Not Minified** — `isMinifyEnabled = false` → Enabled with `isShrinkResources = true`
4. **Data Backup Enabled** — `allowBackup="true"` → Set to `false` for privacy
5. **Onboarding Not Persisted** — User saw onboarding on every process death → Added `OnboardingPrefs` with SharedPreferences flag

#### P1 — Important
6. **ViewModels Mixed with Screens** — Each ViewModel was defined in the same file as its Screen → Separated into independent files
7. **Ad-hoc Error Handling** — Errors were booleans/strings → Added `Result<T>` sealed class with structured error wrapping
8. **No Database Migration Strategy** — Schema changes would crash → Added `fallbackToDestructiveMigration()` (safe: data reconstructable from NetworkStatsManager)
9. **Minimal Test Coverage** — Only 2 test files → Expanded to 9 test files covering Result, DataCapCalculator, ByteFormatter, AppUsageRaw, and ViewModel UiStates
10. **Wrong OnConflictStrategy** — `UsageSnapshotDao.insertAll` used REPLACE on append-only table → Changed to IGNORE

#### P2 — Improvements
11. **No Data Cap Notifications** — Phase 3 incomplete → Added `DataCapNotificationHelper` with threshold notifications at 50/80/100%
12. **No Error Logging** — Exceptions silently swallowed → Added `Log.e`/`Log.w` with TAG throughout
13. **App List Without Icons** — Only first letter shown → Added `PackageManager.getApplicationIcon()` with fallback
14. **AppUsageRaw Mutable** — Used `var` fields → Converted to immutable `data class` with `withWifi`/`withMobile` builder methods
15. **resolveApp Not Cached** — `PackageManager` called for every UID → Added in-memory cache
16. **Duplicate Button Label** — "Set data cap" used for both label and button → Added separate "Edit data cap" / "ویرایش سقف مصرف"
17. **ProGuard Rules Empty** — No rules defined → Added comprehensive rules for Hilt, Room, WorkManager, Compose, Vico, Coroutines
18. **No coroutines-test dependency** — Added `kotlinx-coroutines-test` for future coroutine testing

---

## فاز ۳ کامل شد (۳ شهریور ۱۴۰۵)

PR #7 (Arena) + مرج مستقیم Hermes:

| # | مورد | نتیجه |
|---|---|---|
| ۱ | ۱۸ نقطه‌ضعف بازبینی (نشت NetworkStats، ANR، minify release، allowBackup، onboarding تکراری و...) | فیکس شد |
| ۲ | نوتیفیکیشن هشدار ۵۰/۸۰/۱۰۰٪ سقف مصرف | پیاده شد |
| ۳ | ایراد Hermes: هشدار هر ۱۵ دقیقه تکرار می‌شد | حافظه per-cycle اضافه شد — هر آستانه فقط یک بار در هر سیکل |
| ۴ | خطای کامپایل ResultTest (استنتاج Nothing) | فیکس شد |
| ۵ | تست‌ها: از ۲ فایل به ۹ فایل (۳۵ تست) | CI سبز |

Issue #5 بسته شد. وضعیت: فازهای ۰–۳ ✅ | باقی: ویجت (۴)، پالیش (۵)، release امضاشده (۷)
