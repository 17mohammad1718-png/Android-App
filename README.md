# DataGuard — مدیریت و مانیتورینگ مصرف اینترنت

اپ اندروید **local-first** برای نمایش مصرف اینترنت (وای‌فای و دیتای موبایل) به تفکیک اپ،
روزانه/هفتگی/ماهانه، با امکان تعیین سقف مصرف و هشدار. بدون بک‌اند، بدون سرویس ابری — همه‌چیز روی خود دستگاه.

## وضعیت پیاده‌سازی

| فاز | وضعیت |
|---|---|
| فاز ۰ — اسکلت پروژه (Compose + Hilt + Room + Onboarding) | ✅ |
| فاز ۱ — MVP (مصرف امروز + لیست اپ‌ها + pull-to-refresh) | ✅ |
| فاز ۲ — تاریخچه + WorkManager (snapshot دوره‌ای + نمودار) | ✅ |
| فاز ۳ — مدیریت سقف مصرف و هشدار | 🟡 سقف و پیش‌بینی انجام شده؛ Notification در آستانه‌ها باقی‌مانده |
| فاز ۴ — ویجت (Glance) | ⬜ |
| فاز ۵ — پالیش (زبان/تم/واحد نمایش، تست چند برند) | 🟡 بخشی (تم و واحد نمایش) انجام شده |
| فاز ۶ — کنترل پیشرفته با VPNService | ⬜ (اختیاری، ریسک بالا) |
| فاز ۷ — آماده‌سازی نهایی | ⬜ |

## استک فنی

- **زبان:** Kotlin 2.1.0
- **UI:** Jetpack Compose (Material 3، BOM 2025.01.00)
- **معماری:** MVVM + Clean Architecture (`data` / `domain` / `presentation`)
- **دیتابیس:** Room 2.6.1 (KSP)
- **کار پس‌زمینه:** WorkManager 2.10.0 + Hilt Worker
- **نمودار:** Vico 2.0.1
- **DI:** Hilt 2.53.1
- **API آماری:** `NetworkStatsManager` (اصلی) + چک `AppOpsManager` برای مجوز

## ساخت و اجرا

> این ریپو داخل یک sandbox بدون JDK/Gradle/Android SDK تولید شده، بنابراین
> **بیلد باید روی دستگاه خودتان (Android Studio) انجام شود.**

1. پوشه را در **Android Studio** باز کنید (نسخه‌ی Ladybug یا جدیدتر).
2. اگر فایل `gradle/wrapper/gradle-wrapper.jar` موجود نبود، Android Studio هنگام sync
   به‌صورت خودکار Gradle را دانلود و wrapper را می‌سازد (یا از `gradle wrapper --gradle-version 8.10.2` استفاده کنید).
3. یک دستگاه/امولاتور با **API 29+** انتخاب و Run کنید.
4. در صفحه‌ی Onboarding دکمه‌ی «اعطای دسترسی آمار مصرف» را بزنید و در تنظیمات سیستم،
   `DataGuard` را فعال کنید.

نیازمندی‌ها: JDK 17+، Android SDK 35.

## ساختار پکیج

```
com.dataguard.app
├── DataGuardApp.kt          # Application + HiltWorkerFactory
├── MainActivity.kt
├── di/                      # Hilt modules (Database / Repositories)
├── data/
│   ├── local/               # Room entities, DAOs, database
│   ├── networkstats/        # NetworkStatsManager wrapper + permission check
│   ├── repository/          # impl های repository
│   ├── settings/            # SharedPreferences settings
│   └── worker/              # SnapshotWorker + scheduler
├── domain/
│   ├── model/               # مدل‌های دامنه
│   ├── repository/          # interface های repository
│   ├── usecase/             # use case ها
│   └── util/                # ByteFormatter + DateUtils
└── presentation/
    ├── navigation/          # NavHost + bottom navigation
    ├── theme/               # تم Material 3
    ├── components/          # کامپوننت‌های مشترک
    └── screens/             # Onboarding / Dashboard / AppList / History / DataCap / Settings
```

## مدل داده (Room)

- `UsageSnapshot` — اسنپ‌شات خام دوره‌ای (برای audit و محاسبات delta در فازهای بعد).
- `DataCapConfig` — تک‌رکورد (id=1) تنظیمات سقف مصرف.
- `AppDailyAggregate` — مجموع مصرف روزانه/اپ؛ کلید ترکیبی `(date, appPackageName)`.
  `date` به‌صورت `LocalDate.toEpochDay()` ذخیره می‌شود تا نسبت به تغییر تایم‌زون مقاوم باشد.

## نکات فنی مهم (Gotchas)

- **مجوز Usage Access اجباری است:** `NetworkStatsManager.querySummaryForDevice` و
  `queryDetails*` بدون `PACKAGE_USAGE_STATS` (که فقط از تنظیمات سیستم گرفته می‌شود)
  `SecurityException` پرتاب می‌کنند. قبل از هر query چک `AppOpsManager` انجام می‌شود.
- **UID ≠ package:** `queryDetails` بر اساس UID کار می‌کند؛ نگاشت به نام پکیج با
  `PackageManager.getPackagesForUid()` انجام می‌شود. UIDهای سیستمی/منفی (مثلاً tethering
  یا اپ‌های حذف‌شده) فعلاً به‌صورت `uid_…` نمایش داده می‌شوند.
- **تأخیر sync:** داده‌های `NetworkStatsManager` گاهی چند دقیقه تأخیر دارند؛ انتظار دقت لحظه‌ای ۱۰۰٪ نداشته باشید.
- **WorkManager بازه‌ی حداقل ۱۵ دقیقه دارد** و در Doze/اندروید ۱۴+ ممکن است تأخیر بخورد؛
  هنگام باز شدن اپ یک `refreshSnapshot()` برای backfill بازه‌های از دست رفته اجرا می‌شود.
- **Battery optimization:** در MIUI/EMUI/One UI سرویس پس‌زمینه ممکن است کشته شود؛
  صفحه‌ی Onboarding و Settings دکمه‌ی غیرفعال‌کردن آن را دارند.
- **Vico 2.x:** اگر هنگام کامپایل خطای API دیدید (چون اینجا بیلد نگرفته‌ایم)، مستندات
  نسخه‌ی 2.x را در `guide.vico.patrykandpatrick.com` چک کنید. (نسخه‌ی 3.x به Kotlin 2.4 وابسته است و اینجا عمداً استفاده نشده.)

## نکات طراحی / انحراف‌های آگاهانه از پلن

- `TrafficStats` به‌عنوان fallback تاریخی استفاده **نشده** (کانترهایش از بوت ریست می‌شوند و
  تفکیک تاریخی wifi/mobile ندارند)؛ در صورت نیاز می‌توان بعداً به‌عنوان «نمایش مجموع از آخرین بوت» اضافه کرد.
- جداول `rx/tx` در `UsageSnapshot` جدا ذخیره می‌شوند؛ ولی aggregate روزانه و لیست اپ‌ها
  فعلاً مجموع (rx+tx) را نشان می‌دهند.
- `AppDailyAggregate` به‌جای `id` از کلید ترکیبی `(date, appPackageName)` استفاده می‌کند تا
  `@Upsert` Room درست کار کند.

## قدم‌های بعدی پیشنهادی

1. **فاز ۳ (تکمیل):** Notification در آستانه‌های ۵۰/۸۰/۱۰۰٪ (نیازمند `POST_NOTIFICATIONS` در اندروید ۱۳+).
2. **فاز ۴:** ویجت Jetpack Glance (نمایش درصد و باقی‌مانده).
3. **فاز ۵:** انتخاب زبان درون‌اپ (Per-app locale)، بهبود UI/UX، تست چند برند.
4. **فاز ۶ (اختیاری):** `VpnService` محلی برای بلاک اپ — فقط در صورت نیاز واقعی.
