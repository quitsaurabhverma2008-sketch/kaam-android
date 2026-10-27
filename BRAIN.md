# 🧠 BRAIN — Kaam Android (Native APK)

## 1. Project kya hai
**Kaam** ka **native Android app** (Java). Offline task tracker — no web wrapper, no WebView.
Same concept as web version, but real Android UI + SharedPreferences storage + APK.

## 2. Goal / Next steps
- [x] Native Java app (single Activity + ListView)
- [x] Task CRUD, priority, due date, filters
- [x] Daily progress + stats + streak
- [x] SharedPreferences JSON persistence
- [x] Share/export backup + delete completed
- [x] JVM unit tests for core logic
- [x] Build APK with aapt2 + d8 + apksigner
- [x] Verify APK (manifest, dex, signature)
- [ ] Install on device (no adb device connected yet)
- [ ] Optional: reminders/notifications, recurring tasks

## 3. Stack + free tools
- **Language:** Java 8 source, framework APIs only (no AndroidX, no Gradle)
- **Compile SDK:** Android API 34 (`android.jar` from Sable/android-platforms)
- **Build:** `aapt2` (resources) → `javac` → D8/R8 9.5.28 (dex) → `apksigner`
- **Min SDK:** 21 (Android 5.0) · **Target SDK:** 34
- **Storage:** SharedPreferences JSON — 100% offline, no INTERNET permission
- **Output:** `build/kaam.apk` (signed, installable)
- **Free hosting:** GitHub repo (source + APK release)

## 4. Parts + status
| Part | File | Status |
|---|---|---|
| Model | `src/com/kaam/app/Task.java` | ✅ |
| Core logic (dates, stats, filter, sort) | `src/com/kaam/app/TaskMath.java` | ✅ |
| Storage | `src/com/kaam/app/TaskStore.java` | ✅ |
| UI helpers | `src/com/kaam/app/Ui.java` | ✅ |
| List adapter | `src/com/kaam/app/TaskAdapter.java` | ✅ |
| Main screen | `src/com/kaam/app/MainActivity.java` | ✅ |
| Resources/manifest | `res/`, `AndroidManifest.xml` | ✅ |
| Build script | `build.sh` | ✅ |
| JVM tests | `tests/TaskMathTest.java` | ✅ |

## 5. Important decisions
- No Gradle/AndroidX → build on Termux without SDK manager, tiny APK.
- Programmatic UI (no XML layouts) → fewer build steps, full control of dark theme.
- `Task` + `TaskMath` are pure Java → unit-testable on JVM without Android.
- Task rows custom-built; checkbox = styled circle TextView (consistent look).
- Delete = confirm dialog (no undo across process death).
- Date format `yyyy-MM-dd` in storage, Hinglish labels in UI.
- No INTERNET permission → truly offline/private.

## 6. Build + test commands
```bash
cd ~/projects/kaam-android
./build.sh                 # builds build/kaam.apk + verifies
./test.sh                  # JVM core logic tests
adb install -r build/kaam.apk   # when device connected
```

## 7. Session log
- 2026-10-27: Native app created after user clarified "mobile app, not web". Toolchain: android.jar API 34 + R8 9.5.28 downloaded to ~/tools/android.
- 2026-10-27: Built APK, JVM tests passing, APK verified. No adb device attached for on-device test.
