# Vector Programming (SIMD) — Java

این پروژه برای یادگیری **برنامه‌نویسی برداری / SIMD** است:

> یک دستور پردازنده، روی چند داده هم‌زمان کار می‌کند و سرعت را بالا می‌برد.

منظور ما **`java.util.Vector`** (ساختار داده) نیست.  
منظور **Java Vector API** است: `jdk.incubator.vector` روی JDK 17.

## پیش‌نیاز

- JDK 17
- Maven 3.x

## ساختار

```
vector-programming/
├── pom.xml
└── src/main/java/ir/vector/
    ├── common/Benchmark.java
    └── lesson01/ScalarVsVectorAdd.java
```

## اجرای درس ۱

روی JDK 17 ماژول Vector API incubator است. بدون این فلگ، خطای زیر می‌آید:

`NoClassDefFoundError: jdk/incubator/vector/Vector`

### راه درست (Maven)

```powershell
cd "C:\Users\HO\Desktop\java codes\vector-programming"
.\run-lesson01.ps1
```

یا:

```powershell
mvn -q compile exec:exec "-Dexec.mainClass=ir.vector.lesson01.ScalarVsVectorAdd"
```

### اگر از IntelliJ اجرا می‌کنی

Run Configuration از قبل ساخته شده: **Lesson01 ScalarVsVectorAdd**  
VM options باید این باشد:

```text
--add-modules jdk.incubator.vector
```

اگر Run عادی کلاس را زدی و خطا دیدی:
1. Run → Edit Configurations
2. روی همان Application برو
3. در **VM options** همین فلگ را بگذار

### اجرای مستقیم با java

```powershell
mvn -q compile
java --add-modules jdk.incubator.vector -cp target\classes ir.vector.lesson01.ScalarVsVectorAdd
```

## ایدهٔ اصلی درس ۱

| حالت | چه اتفاقی می‌افتد |
|------|-------------------|
| Scalar | در هر سیکل/تکرار، معمولاً یک جمع `a[i] + b[i]` |
| SIMD / Vector | چند float هم‌زمان داخل یک register برداری جمع می‌شوند |

روی CPU فعلی‌ات، `SPECIES_PREFERRED` معمولاً چند lane می‌دهد (مثلاً 8 برای AVX).  
یعنی تقریباً ۸ جمع float در یک عملیات برداری.

## مسیر یادگیری پیشنهادی

1. **Lesson 01** — Scalar vs Vector add (همین الان)
2. Lesson 02 — `mul` / `fma` و عملیات‌های بیشتر
3. Lesson 03 — mask و باقیماندهٔ آرایه
4. Lesson 04 — reduction (sum / max)
5. Lesson 05 — مقایسه با HotSpot auto-vectorization

## نکته معماری

درس‌ها جدا نگه داشته شده‌اند تا هر مفهوم SIMD بدون درهم‌ریختگی قابل اجرای مستقل باشد.  
منطق مشترک زمان‌سنجی فقط در `common.Benchmark` است (DRY بدون پیچیدگی اضافه).
