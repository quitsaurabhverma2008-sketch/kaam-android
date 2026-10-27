# ✅ Kaam — Native Android Task Tracker

**Real native Android app** (Java) — no WebView, no React Native, no Cordova, no web wrapper.
Offline task tracker: *"Aaj ka kaam, aaj hi."*

**⬇️ Download APK:** https://github.com/quitsaurabhverma2008-sketch/kaam-android/releases/download/v1.0/kaam-v1.0.apk?download=1

**Release page:** https://github.com/quitsaurabhverma2008-sketch/kaam-android/releases/tag/v1.0

**Size:** ~58 KB · **Min:** Android 5.0 (API 21) · **Target:** Android 14 (API 34) · **No INTERNET permission**

---

## ✨ Features

- ➕ Add tasks with **priority** (Low / Medium / High) + **due date** (native date picker)
- ✅ Complete / reopen, ✏️ edit, 🗑️ delete (with confirm)
- 📅 Filters: **Aaj**, **Sab**, **Done** — live counts
- 📊 Daily progress bar, done-today, pending, 🔥 streak
- 🌙 Native dark theme (Material)
- 📤 Share/export tasks as text + delete completed in one tap
- 🔒 100% offline — data in `SharedPreferences`, no network code
- 🪶 Tiny APK, no libraries, no ads, no trackers

## 📲 Install

1. Download the APK from the link above.
2. Phone Settings → allow **Install unknown apps** for your browser/file manager.
3. Open the APK → Install → **Kaam** launcher icon.
4. Done. Data stays on the device.

> Updates ke liye same signing key chahiye. `keystore.jks` is machine-local and gitignored — ise safe rakho.

## 🧱 Tech

| | |
|---|---|
| Language | Java 8 (framework APIs only) |
| UI | Programmatic native views (no XML layouts, no AndroidX) |
| Storage | `SharedPreferences` + `org.json` |
| Build | `aapt2` → `javac` → D8/R8 → `apksigner` (no Gradle) |
| Min / Target | API 21 / API 34 |
| APK | 58 KB, signed v1 + v2 + v3 |

## 🗂 Structure

```
AndroidManifest.xml
src/com/kaam/app/
  Task.java          model
  TaskMath.java      pure date/stats/filter/sort logic (JVM-tested)
  TaskStore.java     SharedPreferences JSON storage
  TaskAdapter.java   custom ListView adapter
  Ui.java            colors / dp / rounded drawables
  MainActivity.java  single-screen UI + dialogs
res/                 strings, theme, launcher icon
tests/TaskMathTest.java
scripts/align_apk.py  resources.arsc 4-byte alignment (no zipalign on Termux)
build.sh             one-command APK build
test.sh              JVM logic tests
```

## 🛠 Build (Termux / Linux)

```bash
# 1. Get a free Android platform jar + R8 (D8)
mkdir -p ~/tools/android && cd ~/tools/android
curl -L -o android.jar https://raw.githubusercontent.com/Sable/android-platforms/master/android-34/android.jar
curl -L -o r8.jar https://dl.google.com/dl/android/maven2/com/android/tools/r8/9.5.28/r8-9.5.28.jar

# 2. Build
cd ~/projects/kaam-android
./test.sh        # 25 pure-JVM checks
./build.sh       # -> build/kaam.apk (signed)

# 3. Install (with a connected device)
adb install -r build/kaam.apk
```

Build needs: `aapt2`, `apksigner`, JDK 17+, Python 3, `keytool`.
`build.sh` generates a random keystore password in `keystore.properties` on first run (gitignored).

## 🧪 Verification

- **25/25** pure-JVM logic checks (dates, filters, sort, stats, streak)
- APK: manifest / minSdk / targetSdk / launcher verified with `aapt2`
- Dex decompiled successfully with `jadx` (all 5 app classes present)
- Signature verified: v1 + v2 + v3
- `resources.arsc` **stored + 4-byte aligned** (Android 11+ requirement)
- SHA-256 in the release notes

## ⚠️ Known limitations

- No cloud sync / reminders yet (v1 is local-only by design).
- On-device install test pending (no adb device attached during build).
- Play Store upload would need a newer targetSdk + Gradle/AAB.

## 📄 License

MIT
