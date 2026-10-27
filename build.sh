#!/data/data/com.termux/files/usr/bin/bash
# Build a signed native APK without Gradle.
set -euo pipefail
cd "$(dirname "$0")"

SDK="${ANDROID_SDK:-$HOME/tools/android}"
ANDROID_JAR="$SDK/android.jar"
R8_JAR="$SDK/r8.jar"
BUILD="build"
KEYSTORE="keystore.jks"
PROPS="keystore.properties"
ALIAS="kaam"

for f in "$ANDROID_JAR" "$R8_JAR"; do
  [ -f "$f" ] || { echo "❌ Missing $f"; exit 1; }
done

echo "▶ Clean"
rm -rf "$BUILD"
mkdir -p "$BUILD/gen" "$BUILD/classes" "$BUILD/dex"

echo "▶ Compile resources (aapt2)"
aapt2 compile --dir res -o "$BUILD/res.zip"
aapt2 link \
  -o "$BUILD/unsigned.apk" \
  -I "$ANDROID_JAR" \
  --manifest AndroidManifest.xml \
  -R "$BUILD/res.zip" \
  --java "$BUILD/gen" \
  --auto-add-overlay \
  --min-sdk-version 21 \
  --target-sdk-version 34

echo "▶ Compile Java (javac)"
find src "$BUILD/gen" -name '*.java' > "$BUILD/sources.txt"
javac -source 8 -target 8 \
  -bootclasspath "$ANDROID_JAR" \
  -d "$BUILD/classes" \
  @"$BUILD/sources.txt"

echo "▶ Dex (D8)"
find "$BUILD/classes" -name '*.class' > "$BUILD/classes.txt"
java -cp "$R8_JAR" com.android.tools.r8.D8 \
  --release \
  --lib "$ANDROID_JAR" \
  --min-api 21 \
  --output "$BUILD/dex" \
  @"$BUILD/classes.txt"

echo "▶ Package classes.dex into APK"
python3 - "$BUILD/unsigned.apk" "$BUILD/dex/classes.dex" <<'PY'
import sys, zipfile
apk, dex = sys.argv[1], sys.argv[2]
if not zipfile.is_zipfile(apk):
    raise SystemExit("not a zip/apk: " + apk)
with zipfile.ZipFile(apk, 'a') as z:
    names = z.namelist()
    if 'classes.dex' in names:
        raise SystemExit("classes.dex already exists")
    z.write(dex, 'classes.dex', compress_type=zipfile.ZIP_STORED)
print("   classes.dex added uncompressed")
PY

echo "▶ Align stored entries (resources.arsc must be 4-byte aligned)"
python3 scripts/align_apk.py "$BUILD/unsigned.apk" "$BUILD/aligned.apk"
mv "$BUILD/aligned.apk" "$BUILD/unsigned.apk"

echo "▶ Signing key"
if [ ! -f "$PROPS" ]; then
  PASS=$(python3 -c "import secrets; print(secrets.token_urlsafe(18))")
  printf 'storePass=%s\n' "$PASS" > "$PROPS"
  chmod 600 "$PROPS"
  echo "   generated random keystore password -> $PROPS (gitignored)"
fi
# shellcheck disable=SC1090
source "$PROPS"

if [ ! -f "$KEYSTORE" ]; then
  keytool -genkeypair -v \
    -keystore "$KEYSTORE" \
    -alias "$ALIAS" \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass "$storePass" -keypass "$storePass" \
    -dname "CN=Kaam, OU=Mobile, O=Kaam, L=India, C=IN" >/dev/null
  echo "   keystore created: $KEYSTORE (keep it safe for updates)"
fi

echo "▶ apksigner"
apksigner sign \
  --ks "$KEYSTORE" \
  --ks-key-alias "$ALIAS" \
  --ks-pass "pass:$storePass" \
  --key-pass "pass:$storePass" \
  --out "$BUILD/kaam.apk" \
  "$BUILD/unsigned.apk"

apksigner verify --verbose --print-certs "$BUILD/kaam.apk" | head -20

echo "▶ Verify manifest"
aapt2 dump badging "$BUILD/kaam.apk" | grep -E "^(package|sdkVersion|targetSdkVersion|application-label|launchable-activity)" || true

echo "▶ Check resources.arsc compression"
python3 - "$BUILD/kaam.apk" <<'PY'
import sys, zipfile
with zipfile.ZipFile(sys.argv[1]) as z:
    info = z.getinfo('resources.arsc')
    print("   resources.arsc: stored" if info.compress_type == zipfile.ZIP_STORED else "   resources.arsc: DEFLATED (may break Android 11+)")
    print("   classes.dex: stored" if z.getinfo('classes.dex').compress_type == zipfile.ZIP_STORED else "   classes.dex: deflated")
PY

echo
ls -lh "$BUILD/kaam.apk"
echo "✅ APK ready: $PWD/$BUILD/kaam.apk"
