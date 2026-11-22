#!/usr/bin/env bash
set -euo pipefail

sdk_root="${ANDROID_HOME:?Set ANDROID_HOME to the Android SDK directory}"
export PATH="$sdk_root/platform-tools:$PATH"
export ANDROID_SERIAL=emulator-5554
export ANDROID_USER_HOME="${RUNNER_TEMP:-${TMPDIR:-/tmp}}/walking-buddy-android"
export ANDROID_AVD_HOME="$ANDROID_USER_HOME/avd"
avd_name=walking_buddy_ci
results_dir="$PWD/ci-logs/ui"
mkdir -p "$results_dir" "$ANDROID_AVD_HOME"

emulator_pid=
cleanup() {
    if [[ -n "$emulator_pid" ]]; then
        timeout 10 adb logcat -d > "$results_dir/logcat.log" 2>&1 || true
        timeout 10 adb emu kill >/dev/null 2>&1 || true
        kill "$emulator_pid" 2>/dev/null || true
        wait "$emulator_pid" || true
    fi
}
trap cleanup EXIT

# GitHub's Ubuntu runner provides KVM. Fail with its diagnostic if unavailable.
if [[ -e /dev/kvm && ! -w /dev/kvm ]]; then
    sudo chmod a+rw /dev/kvm
fi
"$sdk_root/cmdline-tools/latest/bin/sdkmanager" \
    'emulator' 'system-images;android-35;default;x86_64' 2>&1 | tee "$results_dir/sdk.log"
"$sdk_root/emulator/emulator" -accel-check
printf 'no\n' | "$sdk_root/cmdline-tools/latest/bin/avdmanager" create avd \
    --force --name "$avd_name" --path "$ANDROID_AVD_HOME/$avd_name.avd" \
    --package 'system-images;android-35;default;x86_64' --device pixel_6
"$sdk_root/emulator/emulator" -list-avds
"$sdk_root/emulator/emulator" -avd "$avd_name" -port 5554 -accel on \
    -no-window -no-audio -no-snapshot -no-boot-anim -gpu swiftshader \
    -memory 2048 -cores 2 > "$results_dir/emulator.log" 2>&1 &
emulator_pid=$!

boot_deadline=$((SECONDS + 240))
until [[ "$(timeout 10 adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == 1 ]]; do
    if ! kill -0 "$emulator_pid" 2>/dev/null || ((SECONDS >= boot_deadline)); then
        cat "$results_dir/emulator.log"
        printf 'Android test device failed to boot.\n' >&2
        exit 1
    fi
    sleep 2
done
adb shell wm dismiss-keyguard
adb shell wm size 1080x1920
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb install -t app/build/outputs/apk/debug/app-debug.apk
adb install -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk

for font_scale in 1.0 1.5; do
    destination="$results_dir/font-$font_scale"
    mkdir -p "$destination"
    adb shell settings put system font_scale "$font_scale"
    adb logcat -c
    timeout 180 adb shell am instrument -w \
        com.walkingbuddy.app.debug.test/com.walkingbuddy.app.SmokeInstrumentation \
        2>&1 | tee "$destination/instrumentation.log"
    # am instrument can return success even when a custom runner reports failure.
    grep -Eq 'PASS: [0-9]+ Walking Buddy integration checks' "$destination/instrumentation.log"
    adb pull /sdcard/Android/data/com.walkingbuddy.app.debug/files/qa "$destination/screenshots"
done
