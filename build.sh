#!/bin/bash
# Build QbiliPlili LSPosed module (pure-java, no aapt2/gradle needed)
set -u
cd "$(dirname "$0")"
JDK=/opt/tools/jdk
TOOLS=/opt/tools
PKG=io.op.qbiliplili
OUT=out

echo "== [1/6] compile resources with aapt (icon: gen/ic_launcher.png) =="
cd gen
mkdir -p ab/res/drawable
cp ic_launcher.png ab/res/drawable/
cp manifest_src.xml ab/AndroidManifest.xml
( cd ab && aapt package -f -M AndroidManifest.xml -S res -I "$TOOLS/android.jar" -F ../out_base.apk ) 2>&1 || { echo "aapt FAILED"; exit 1; }
cd ..
rm -rf "$OUT"; mkdir -p "$OUT"
mv gen/out_base.apk "$OUT"/base.apk
echo "base.apk OK"

echo "== [2/6] compile api stubs =="
rm -rf "$OUT"/api_classes; mkdir -p "$OUT"/api_classes
find api -name '*.java' > "$OUT"/api_srcs.txt
"$JDK/bin/javac" -cp "$TOOLS/android.jar" -d "$OUT"/api_classes @"$OUT"/api_srcs.txt 2>&1 || { echo "api compile FAILED"; exit 1; }
"$JDK/bin/jar" cf "$OUT"/api.jar -C "$OUT"/api_classes . 2>&1

echo "== [3/6] compile module =="
rm -rf "$OUT"/main_classes; mkdir -p "$OUT"/main_classes
find src -name '*.java' > "$OUT"/main_srcs.txt
"$JDK/bin/javac" -cp "$TOOLS/android.jar:$OUT/api.jar" -d "$OUT"/main_classes @"$OUT"/main_srcs.txt 2>&1 || { echo "module compile FAILED"; exit 1; }

echo "== [4/6] dex with r8 =="
rm -rf "$OUT"/dex; mkdir -p "$OUT"/dex
"$JDK/bin/jar" cf "$OUT"/main.jar -C "$OUT"/main_classes . 2>&1
cat > "$OUT"/keep.pro <<'EOF'
-keep class io.op.qbiliplili.** { *; }
-keep class * implements de.robv.android.xposed.IXposedHookLoadPackage { *; }
-dontwarn **
-dontnote **
EOF
"$JDK/bin/java" -cp "$TOOLS/r8.jar" com.android.tools.r8.R8 \
  --release --no-minification \
  --lib "$TOOLS/android.jar" \
  --lib "$OUT/api.jar" \
  --pg-conf "$OUT/keep.pro" \
  --output "$OUT/dex" \
  "$OUT"/main.jar 2>&1 | grep -v -E "Warning:|warning:|Missing class" || true
ls -la "$OUT"/dex/classes*.dex 2>&1 || { echo "dex FAILED"; exit 1; }

echo "== [5/6] package apk (add dex + assets to aapt base) =="
rm -rf "$OUT"/apk; mkdir -p "$OUT"/apk
cp "$OUT"/base.apk "$OUT"/apk/
mkdir -p "$OUT"/apk/assets
printf 'io.op.qbiliplili.MainHook' > "$OUT"/apk/assets/xposed_init
cp "$OUT"/dex/classes*.dex "$OUT"/apk/
( cd "$OUT"/apk && python3 - <<'PY'
import zipfile, os
zin = zipfile.ZipFile('base.apk')
zout = zipfile.ZipFile('../unsigned.apk', 'w', zipfile.ZIP_DEFLATED)
for i in zin.infolist():
    zout.writestr(i, zin.read(i.filename))
for p in os.listdir('.'):
    if p.endswith('.dex'):
        zout.write(p)
zout.write('assets/xposed_init')
zout.close(); zin.close()
print("PACKAGED")
PY
) 2>&1

echo "== [6/6] sign (v1, jarsigner, persistent keystore) =="
KEYSTORE="$OUT"/../debug.keystore
if [ ! -f "$KEYSTORE" ]; then
  "$JDK/bin/keytool" -genkey -keystore "$KEYSTORE" -alias androiddebugkey \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass android -keypass android -dname "CN=Android Debug,O=Android,C=US" 2>&1
fi
rm -f "$OUT"/signed.apk
"$JDK/bin/jarsigner" -keystore "$KEYSTORE" -storepass android -keypass android \
  -signedjar "$OUT"/signed.apk "$OUT"/unsigned.apk androiddebugkey 2>&1

echo ""
echo "== DONE =="
ls -la "$OUT"/signed.apk
cp "$OUT"/signed.apk /sdcard/QbiliPlili.apk
echo "APK copied to /sdcard/QbiliPlili.apk"
