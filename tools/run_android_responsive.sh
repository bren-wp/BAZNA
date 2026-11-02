#!/usr/bin/env bash
# Real Android emulator test grid. Runs all 12 Compose screens at each size.
# The emulator is started by android-emulator-runner; this script never changes
# application metadata or touches the user's published app signing key.
set -euo pipefail

cd "$(dirname "$0")/../android"
mkdir -p ../artifacts/android-responsive

test -s app/build/outputs/apk/debug/app-debug.apk
test -s app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb wait-for-device
adb shell wm size reset >/dev/null
adb shell wm density reset >/dev/null
trap 'adb shell wm size reset >/dev/null 2>&1 || true; adb shell wm density reset >/dev/null 2>&1 || true; adb shell settings put system font_scale 1.0 >/dev/null 2>&1 || true' EXIT

adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk

# Widths in dp because Android density is fixed to 160 for deterministic tests.
# Includes a 240dp small phone, landscape tablet and one accessibility case.
cases=(
    "240x480:1.0:small-240"
    "320x568:1.0:small-320"
    "360x780:1.0:phone-360"
    "393x852:1.0:reference-393"
    "411x891:1.0:phone-411"
    "600x960:1.0:tablet-600"
    "840x1120:1.0:tablet-840"
    "1280x800:1.0:landscape-1280"
    "320x640:1.4:large-font-320"
)

for entry in "${cases[@]}"; do
    IFS=: read -r resolution scale label <<<"$entry"
    echo "::group::SIDRENA responsive emulator ${label} (${resolution}, font ${scale})"
    adb shell wm size "$resolution"
    adb shell wm density 160
    adb shell settings put system font_scale "$scale"
    sleep 2

    output="../artifacts/android-responsive/${label}"
    mkdir -p "$output"
    # am instrument may exit zero even when JUnit fails: validate runner output.
    adb shell am instrument -w -r \
        com.brendigo.sidrena.test/androidx.test.runner.AndroidJUnitRunner \
        | tee "$output/instrumentation.txt"
    grep -q 'OK (1 test)' "$output/instrumentation.txt"

    adb pull /sdcard/Android/data/com.brendigo.sidrena/files/responsive/. "$output/" >/dev/null
    COUNT="$(find "$output" -maxdepth 1 -type f -name '*.png' | wc -l)"
    test "$COUNT" -eq 12

    python3 - "$output" "$resolution" <<'PY'
import pathlib, struct, sys
folder = pathlib.Path(sys.argv[1])
expected = tuple(map(int, sys.argv[2].split("x")))
for path in sorted(folder.glob("*.png")):
    with path.open("rb") as f:
        header = f.read(24)
    assert header[:8] == b"\x89PNG\r\n\x1a\n", path
    dimensions = struct.unpack(">II", header[16:24])
    assert dimensions == expected, (path, dimensions, expected)
print("PASS:", len(list(folder.glob("*.png"))), "screenshots at", expected)
PY
    echo "::endgroup::"
done

echo "PASS: 9 real Android emulator configurations x 12 SIDRENA screens = 108 PNG screenshots"
