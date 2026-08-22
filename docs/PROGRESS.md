# DataGuard — گزارش پیشرفت و نقشه راه

آخرین به‌روزرسانی: ۳ شهریور ۱۴۰۵ (۲۳ اوت ۲۰۲۶)

## خلاصه

اپ اندروید DataGuard (مانیتور مصرف اینترنت، local-first) از «کد کامپایل‌نشده روی کاغذ»
به **اولین APK واقعی نصب‌شده و تست‌شده روی گوشی** رسید. کل چرخه در یک روز:

```
CI گیت‌هاب راه افتاد → ۴ دور خطا → رفع → بیلد سبز → APK تحویل شد
```

- وضعیت CI: 🟢 سبز (run موفق: ۱ دقیقه و ۳۷ ثانیه)
- خروجی: `DataGuard-debug-apk` (artifact هر پوش + نسخه debug روی دسکتاپ)
- تست میدانی: ✅ نصب و اجرای موفق روی گوشی واقعی

## کارهای انجام‌شده (به ترتیب)

| # | کار | نتیجه |
|---|---|---|
| ۱ | افزودن GitHub Actions workflow (تست + lint + ساخت APK) | PR #4 → merge |
| ۲ | بیلد اول: ۵ خطای کامپایل Kotlin | شناسایی و فیکس |
| ۳ | `MainActivity`: نبودن `initialValue` برای `collectAsStateWithLifecycle` | فیکس با `AppSettings()` |
| ۴ | `NetworkStatsDataSource`: استفاده اشتباه از `hasNextBucket` (این API یک Bucket تکی برمی‌گرداند نه iterable) | فیکس |
| ۵ | `HistoryScreen`: ایمپورت جاافتاده Vico (`rememberStart`/`rememberBottom`) | فیکس |
| ۶ | بیلد دوم: ۱۲ خطای JVM signature clash (`private set` + تابع هم‌نام) | بازطراحی state در ۳ ViewModel |
| ۷ | بیلد سوم: یک property حذف‌شده در پچ قبلی | بازیابی |
| ۸ | بیلد چهارم: کامپایل پاس شد؛ فقط lint error (initializer دوبل WorkManager — باعث کرش واقعی می‌شد) | فیکس manifest |
| ۹ | **بیلد پنجم: سبز** → اولین APK | صحت‌سنجی + نصب موفق |

## درس‌های فنی (برای مراجعه بعدی)

1. `collectAsStateWithLifecycle()` بدون پارامتر فقط برای `StateFlow` کار می‌کند؛ برای `Flow` معمولی `initialValue` الزامی است.
2. `NetworkStatsManager.querySummaryForDevice` یک `Bucket` تجمیعی واحد برمی‌گرداند — حلقه‌زدن روش خطاست.
3. توابع axis کتابخانه Vico 2.x (`VerticalAxis.rememberStart`) توابع extension جدا هستند و ایمپورت مخصوص می‌خواهند.
4. پراپرتی Compose با `by mutableStateOf` + `private set` در JVM متد `setX()` می‌سازد؛ تعریف تابع هم‌نام = خطای Platform declaration clash. الگوی صحیح: فیلد خصوصی + getter عمومی فقط-خواندنی.
5. وقتی `Application` از `Configuration.Provider` استفاده می‌کند باید `WorkManagerInitializer` در manifest حذف شود؛ وگرنه lint fail و در اجرا کرش دوبار-initialize.

## فرآیند کاری که جواب داد (حفظ شود)

1. هر تغییر → commit مستقیم روی main → CI خودکار بیلد می‌گیرد
2. لاگ خطای CI ← استخراج ← دسته‌بندی ریشه‌ها ← فیکس دقیق ← push بعدی
3. فایل workflow را PAT بدون scope نمی‌تواند پوش کند → آن مورد خاص از طریق Arena/freebuff
4. نگهبان پس‌زمینه (poll هر ۳۰ ثانیه) نتیجه بیلد را گزارش می‌کند
5. APK artifact دانلود و قبل از تحویل صحت‌سنجی می‌شود (ZIP معتبر، manifest، dex)

## وضعیت فازها

| فاز | توضیح | وضعیت |
|---|---|---|
| ۰–۲ | اسکلت، MVP، تاریخچه | ✅ کامل و کامپایل‌شده |
| ۳ | سقف مصرف + پیش‌بینی | 🟡 نوتیفیکیشن آستانه مانده |
| ۴ | ویجت صفحه اصلی (Glance) | ⬜ |
| ۵ | پالیش (زبان درون‌اپ، UI/UX، تست چند برند) | 🟡 بخشی |
| ۶ | کنترل VPNService (اختیاری) | ⬜ |
| ۷ | آماده‌سازی انتشار (release امضاشده) | ⬜ |

## قدم بعدی

فاز ۳ — نوتیفیکیشن هشدار در آستانه‌های ۵۰/۸۰/۱۰۰٪ سقف مصرف
(جزئیات در Issue شماره ۵)

---

## دور بررسی کد (۳ شهریور ۱۴۰۵)

بازبینی کامل سورس انجام شد؛ یافته‌ها و فیکس‌ها:

| # | یافته | فیکس |
|---|---|---|
| ۱ | تنظیم «واحد نمایش» (MB/GB) ذخیره می‌شد ولی هیچ‌جا خوانده نمی‌شد (تنظیم مرده) | پارامتر `unit` به `ByteFormatter` اضافه شد؛ `LocalDisplayUnit` در تم ارائه و همه‌ی نقاط UI (داشبورد/لیست اپ/نمودار) از طریق `formatBytes` به آن وصل شدند |
| ۲ | رشد بی‌نهایت `usage_snapshot` (worker هر ۱۵ دقیقه snapshot کامل درج می‌کرد، ~۹۶× در روز) | درج snapshot خام حداکثر یک‌بار در ۱۲ ساعت + حذف ردیف‌های قدیمی‌تر از ۳۰ روز در هر اجرا |
| ۳ | `watch_ci2.py` / `watch_ci3.py` (اسکریپت وچر لوکال) در روت مانده بودند | حذف از ریپو |
| ۴ | نبود try/catch در `AppListViewModel` / `HistoryViewModel` / `DataCapViewModel` → کرش اپ در خطای غیرمنتظره | state خطا + شاخه‌ی error/retry در UI (هم‌الگوی DashboardViewModel) |
| ۵ | موارد جزئی: `resolveApp` دو بار به‌ازای هر اپ، `toInt()` به‌جای round در لیبل محور نمودار، `exportSchema=false`، نبود تست مسیر فارسی‌سازی/واحد اجباری | ادغام resolve، `roundToInt()`، فعال‌سازی export اسکیما (+ `ksp room.schemaLocation`)، تست‌های جدید |

تست‌های اضافه‌شده: واحد اجباری MB/GB، فارسی‌سازی ارقام/ممیز/واحدها.
