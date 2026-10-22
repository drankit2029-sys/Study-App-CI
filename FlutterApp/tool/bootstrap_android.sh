#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if [ ! -d android ]; then
  tmp="$(mktemp -d)"
  trap 'rm -rf "$tmp"' EXIT
  flutter create \
    --platforms=android \
    --org com.studyapp.pomodoro \
    --project-name flutterapp \
    --no-pub \
    "$tmp/project" > /dev/null
  cp -R "$tmp/project/android" ./android
fi

res="android/app/src/main/res"
mkdir -p "$res/values" "$res/drawable" "$res/mipmap-anydpi-v26"
cp android_assets/app_colors.xml "$res/values/app_colors.xml"
cp android_assets/ic_launcher_foreground.xml "$res/drawable/ic_launcher_foreground.xml"
cp android_assets/ic_launcher.xml "$res/mipmap-anydpi-v26/ic_launcher.xml"
for density in mdpi hdpi xhdpi xxhdpi xxxhdpi; do
  mkdir -p "$res/mipmap-$density"
  cp "android_assets/mipmap-$density/ic_launcher.png" "$res/mipmap-$density/ic_launcher.png"
done

python3 - <<'PY'
from pathlib import Path
import re

root = Path("android/app/src/main")
manifest = root / "AndroidManifest.xml"
text = manifest.read_text()
text = re.sub(r'android:label="[^"]*"', 'android:label="Pomodoro"', text, count=1)
if "android:screenOrientation=" not in text:
    text = text.replace(
        'android:name=".MainActivity"',
        'android:name=".MainActivity"\n            android:screenOrientation="portrait"',
        1,
    )
manifest.write_text(text)

for path in (root / "res").rglob("*.xml"):
    content = path.read_text()
    updated = (
        content.replace("@android:color/white", "@color/ivory")
        .replace("@android:color/black", "@color/ivory")
        .replace("?android:colorBackground", "@color/ivory")
        .replace("Theme.Black.NoTitleBar", "Theme.Light.NoTitleBar")
    )
    if updated != content:
        path.write_text(updated)
PY

echo "Flutter Android scaffold is ready."
