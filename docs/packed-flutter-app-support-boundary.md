# DPIS Packed/Dynamic-Classloader Flutter App Support Boundary

> Investigation date: 2026-05-17
> Target: `com.mfcloudcalculate.networkdisk` (123 云盘)
> Module: DPIS modern101 (libxposed API 101)

## 1. Static Evidence

### 1.1 Manifest

| Field | Value |
|---|---|
| `android:name` (Application) | `s.h.e.l.l.S` |
| `android:appComponentFactory` | `s.h.e.l.l.A` |
| Real Application (from shell code) | `com.mfcloudcalculate.networkdisk.MyApplication` |

**Manifest Activities:**

| Activity | Notes |
|---|---|
| `com.mfcloudcalculate.networkdisk.activity.LauncherActivity` | Exported, main launcher |
| `com.mfcloudcalculate.networkdisk.MainActivity` | singleTask, handles deep links |
| `com.mfcloudcalculate.networkdisk.SplashActivity` | Not exported |
| `com.mfcloudcalculate.networkdisk.activity.EmptyActivity` | Dialog theme |
| `com.mfcloudcalculate.networkdisk.activity.OnlineServiceActivity` | WebView-like |
| `com.mfcloudcalculate.networkdisk.wxapi.WXEntryActivity` | WeChat integration |
| `com.mfcloudcalculate.networkdisk.wxapi.WXPayEntryActivity` | WeChat Pay |

All activity class names are **declared in manifest but NOT present in dex** — they exist only in the shell's encrypted payload.

### 1.2 Dex

| Metric | Value |
|---|---|
| Number of dex files in APK | 1 (`classes.dex`) |
| Total classes in dex | **6** |
| Manifest Activity classes present? | **NO** |
| Flutter Java embedding classes present? | **NO** |
| Shell/packer classes present? | **YES** |

**All 6 classes in `classes.dex`:**

| Class | Role |
|---|---|
| `com.mfcloudcalculate.networkdisk.R` | Android resource IDs |
| `kotlin.coroutines.jvm.internal.DebugProbesKt` | Kotlin coroutine debug |
| `s.h.e.l.l.A` | Shell `AppComponentFactory` — intercepts classloader creation |
| `s.h.e.l.l.C` | Shell helper |
| `s.h.e.l.l.N` | Shell native bridge — loads `libexec.so`, provides `al()` (classloader replacement) |
| `s.h.e.l.l.S` | Shell `Application` — delegates to real `MyApplication` after decryption |

### 1.3 Assets / Native Libraries

| Item | Present? | Path / Notes |
|---|---|---|
| `ijiami.dat` | ✅ | Encrypted app dex payload |
| `libsec` | ✅ | ijiami security data |
| `libijmDataEncryption_arm64.so` | ✅ | ijiami encryption library (in assets) |
| `libexec.so` | ✅ | Extracted to `files/` at runtime, core decryption engine |
| `libexecmain.so` | ✅ | Extracted to `files/` at runtime, main dex loader |
| `libflutter.so` | ✅ | Flutter engine (normal, in `lib/arm64/`) |
| `libapp.so` | ✅ | Flutter compiled Dart code (normal, in `lib/arm64/`) |
| Split APKs | ❌ | `pm path` shows only `base.apk` |

### 1.4 Static Conclusion

> **PACKED CONFIRMED** — ijiami (爱加密) commercial packer.
>
> The base.apk contains only a 6-class shell stub. All real application classes (including
> Flutter Java embedding, manifest Activities, and business logic) are encrypted in
> `assets/ijiami.dat` and decrypted at runtime by native `libexec.so` into anonymous
> in-memory DEX regions.

## 2. Runtime Evidence

### 2.1 libxposed Lifecycle Callbacks

| Callback | Fires in target process? | Notes |
|---|---|---|
| `onModuleLoaded` | ✅ | Earliest; no classloader available |
| `onPackageLoaded` | ✅ | `defaultClassLoader` is shell's PathClassLoader |
| `onPackageReady` | ❌ | Does NOT fire — shell's `AppComponentFactory` intercepts classloader creation |

### 2.2 ClassLoader State at `onPackageLoaded`

| Property | Value |
|---|---|
| `defaultClassLoader` type | `dalvik.system.PathClassLoader` |
| Points to | `base.apk` (shell-only dex) |
| Can load manifest Activities? | **NO** — `ClassNotFoundException` for all |
| Can load Flutter Java classes? | **NO** — `ClassNotFoundException` for all |
| Can load shell classes (`s.h.e.l.l.*`)? | **YES** (present in base.apk dex) |

### 2.3 Java Hook Callback Results

| Hook target | Class origin | Install result | Callback fires? |
|---|---|---|---|
| `Activity.onResume` | Boot/framework | ✅ install ok | ❌ No callback |
| `Handler.dispatchMessage(Message)` | Boot/framework | ✅ install ok | ❌ No callback |
| `Instrumentation.callActivityOnResume(Activity)` | Boot/framework | ✅ install ok | ❌ No callback |
| `Instrumentation.callActivityOnCreate(Activity, Bundle)` | Boot/framework | ✅ install ok | ❌ No callback |
| `s.h.e.l.l.A.instantiateClassLoader` | App dex (shell) | ✅ class found, install ok | ❌ No callback |
| `s.h.e.l.l.S.attachBaseContext` | App dex (shell) | ✅ class found, install ok | ❌ No callback |
| `AppComponentFactory.instantiateClassLoader` (framework) | Boot/framework | ✅ install ok | ❌ No callback |
| `Application.attachBaseContext` (framework) | Boot/framework | ✅ install ok | ❌ No callback |

### 2.4 Runtime `/proc/maps`

| Mapping | Present? | Notes |
|---|---|---|
| `[anon:dalvik-DEX data]` | ✅ | Many large anonymous mappings — decrypted app dex |
| `libexec.so` | ✅ | `/data/data/.../files/libexec.so` — shell decryption engine |
| `libexecmain.so` | ✅ | `/data/data/.../files/libexecmain.so` — shell main dex loader |
| `libflutter.so` | ✅ | Normal Flutter engine from APK lib/ |
| `libapp.so` | ✅ | Normal Flutter compiled Dart from APK lib/ |
| `lywm/patch/wmdevcal_7.3.8.0` | ✅ | Hot-patch dex (dynamic code update) |
| `pangle_p/` ad SDK dex | ✅ | Multiple ad SDK dex files loaded dynamically |
| `base.apk` dex | ✅ | Shell-only dex (6 classes) |

## 3. DPIS Route Viability

### 3.1 Routes That Do NOT Work

| Route | Why |
|---|---|
| **Java semantic hooks from `onModuleLoaded`** | Too early; no classloader; boot class hooks "install ok" but no callback |
| **Java semantic hooks from `onPackageLoaded`** | `defaultClassLoader` can't see app/Flutter classes; boot class hooks no callback |
| **Java semantic hooks from `onPackageReady`** | `onPackageReady` never fires in this process (shell bypasses it) |
| **Direct `Class.forName` for app/Flutter classes** | Not in `defaultClassLoader`'s dex — all encrypted |
| **Framework class hooks (Activity, Handler, Instrumentation)** | "install ok" but callbacks never fire in this packed app process |

### 3.2 Routes That Also Do NOT Work (Tested)

| Route | Result |
|---|---|
| Hook `s.h.e.l.l.A.instantiateClassLoader` | Class found, install ok, **no callback** |
| Hook `s.h.e.l.l.S.attachBaseContext` | Class found, install ok, **no callback** |

This confirms that **ALL Java method hooks** — boot, framework, and app-dex shell classes — fail to callback in this packed app process. The ijiami packer's native runtime manipulation makes libxposed hook callbacks entirely non-functional.

### 3.3 Routes That Could Work (Not Tested Yet)

| Route | Feasibility | Notes |
|---|---|---|
| **Native Flutter engine hooking** | High | `libflutter.so` is NOT encrypted; loaded normally from `lib/arm64/`. Can hook text rendering functions directly. Independent of Java classloader state. |
| **compat100 legacy Xposed API** | Unknown | `handleLoadPackage` receives `LoadPackageParam` with classloader and fires later than `onModuleLoaded`. May fire after shell decryption. Needs testing. |

## 4. Recommendations

### 4.1 Product Decision

**Do NOT productize ijiami-specific support.** The shell is a commercial packer with:
- Obfuscated class names (`s.h.e.l.l.*`)
- Native classloader replacement
- Anti-tampering measures
- Version-specific behavior

Depending on shell internals (class names, method signatures, decryption timing) creates:
- Fragile code that breaks when the packer updates
- Maintenance burden with no user-visible benefit boundary
- Security/legal concerns around packer circumvention

### 4.2 User-Facing Limitation

Suggested user-visible explanation:

> **某些加壳应用不支持字体缩放**
>
> 部分应用使用了商业加壳保护（如爱加密、梆梆加固等），其运行时类加载机制与
> Xposed 模块不兼容。这类应用的 Flutter 字体缩放功能可能无法生效。
>
> 如遇此情况，建议联系应用开发者反馈字体可访问性需求。

### 4.3 General Improvements for DPIS

1. **Packer detection at install time**: When `installAppProcessHooksIfConfigured` runs, check if `classes.dex` has suspiciously few classes, or if `AppComponentFactory` / `Application` class names match known packer patterns. Log a warning and skip complex hook installation.

2. **`onPackageReady` availability check**: If `onPackageReady` doesn't fire within a reasonable lifecycle window, log that the app may use a custom `AppComponentFactory` that bypasses standard classloader initialization.

3. **ClassLoader capability probe**: Before installing Flutter hooks, verify that the available classloader can actually resolve at least one Flutter embedding class. If not, skip Flutter hook installation with a clear log message.

4. **compat100 fallback path**: For packed apps where libxposed API 101 `onPackageReady` doesn't fire, investigate whether the compat100 legacy API's `handleLoadPackage` fires after shell decryption. This could be a generic fallback without depending on shell internals.

5. **Native Flutter route independence**: The `libflutter.so` native hooking path should work regardless of Java-level packing. Prioritize making this route robust and generic (symbol-based offset resolution rather than hardcoded offsets) as it's the only viable path for packed Flutter apps.

## 5. Stop Conditions Met

| Condition | Status |
|---|---|
| Boot/framework hooks don't callback | ✅ Confirmed — all tested (Activity, Handler, Instrumentation) |
| `defaultClassLoader` can't load target classes | ✅ Confirmed — only 6 shell classes visible |
| `onPackageReady` doesn't fire | ✅ Confirmed — shell bypasses standard lifecycle |
| Need to enter native shell functions to continue | ✅ Would require hooking `N.al()` / `libexec.so` decryption |
| Need shell-brand-specific logic | ✅ `s.h.e.l.l.*` class names are ijiami-specific |

Additional stop conditions confirmed:

| Condition | Status |
|---|---|
| Shell class hooks (`s.h.e.l.l.A/S`) don't callback | ✅ Confirmed — class found, install ok, no callback |
| No real ClassLoader captured | ✅ No `retryWithAppClassLoader` reached |
| Java hooks entirely non-functional in packed process | ✅ **ALL** hook types (boot, framework, app-dex) fail |

**Resolution**: Investigation complete. The boundary is fully established: **libxposed API 101 Java method hooks are entirely non-functional in ijiami-packed app processes**. All probe code has been removed from the codebase. DPIS should treat packed/dynamic-classloader apps as unsupported for Java semantic hooks. Generic Android Resources/WebView/native routes that operate independently may still work.

## Appendix: Shell Mechanism Detail

```
Boot sequence:
1. Zygote forks → process starts
2. ART loads base.apk → only s.h.e.l.l.{A,C,N,S} + R + DebugProbesKt
3. s.h.e.l.l.A.instantiateClassLoader() called by framework
   → N.al() [native] → libexec.so decrypts ijiami.dat
   → Creates new ClassLoader with decrypted dex (in-memory)
   → Returns replaced ClassLoader
4. s.h.e.l.l.S.attachBaseContext() called
   → N.l() [native] → additional init
   → N.r() → replaces Application reference to real MyApplication
5. Real MyApplication.onCreate() called via delegated Application
6. Activities, Flutter, etc. all use the decrypted ClassLoader

DPIS libxposed API 101 lifecycle:
- onModuleLoaded: fires at step 1 (too early)
- onPackageLoaded: fires at step 2 (only shell classes visible)
- onPackageReady: NEVER fires (step 3 bypasses standard path)
```
