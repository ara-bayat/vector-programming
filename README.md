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
├── run-lesson01.ps1
├── run-lesson02.ps1
├── run-lesson03.ps1
└── src/main/java/ir/vector/
    ├── common/Benchmark.java
    ├── lesson01/ScalarVsVectorAdd.java
    ├── lesson02/ComplexKernelBenchmark.java
    └── lesson03/ByteKernelBenchmark.java
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

## درس ۲ — کرنل پیچیده‌تر

فرمول هر عنصر:

```text
poly   = ((c3*x + c2)*x + c1)*x + c0
energy = wX*x*x + wY*y*y + wZ*z*z
seed   = sqrt(energy)*gain + poly
out    = NewtonRefine(seed, energy, poly)   // چند ده iteration
```

```powershell
.\run-lesson02.ps1
```

در IntelliJ: **Lesson02 ComplexKernelBenchmark**  
(VM options همان `--add-modules jdk.incubator.vector`)

اینجا از `mul` / `fma` / `sqrt` / `broadcast` استفاده می‌شود.  
چون FLOP به‌ازای هر load بیشتر است، معمولاً تفاوت SIMD واضح‌تر از درس ۱ دیده می‌شود.

## درس ۳ — کرنل سنگین روی byte (۸بیت)

روی AVX-256 معمولاً `32` تا lane برای byte داریم (`256/8`).

```powershell
.\run-lesson03.ps1
```

در IntelliJ: **Lesson03 ByteKernelBenchmark**

## مسیر یادگیری پیشنهادی

1. **Lesson 01** — Scalar vs Vector add
2. **Lesson 02** — کرنل سنگین float با `mul` / `fma` / `sqrt`
3. **Lesson 03** — کرنل سنگین byte (۸بیت)
4. Lesson 04 — reduction (sum / max)
5. Lesson 05 — مقایسه با HotSpot auto-vectorization

## نکته معماری

درس‌ها جدا نگه داشته شده‌اند تا هر مفهوم SIMD بدون درهم‌ریختگی قابل اجرای مستقل باشد.  
منطق مشترک زمان‌سنجی فقط در `common.Benchmark` است (DRY بدون پیچیدگی اضافه).
