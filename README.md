# Walking Buddy

A native Android virtual-pet pedometer inspired by the daily-goal and pet-care
loop of [Step Pals](https://play.google.com/store/apps/details?id=com.benleavitt.steppals).
Walk a little, care a little, and collect a club of original animated pixel companions.

![Original pixel buddies](art/buddies.png)

## Install on your Android phone

1. Download **[Walking-Buddy-1.1.0.apk](dist/Walking-Buddy-1.1.0.apk)**. On GitHub's file page, use **Download raw file**.
2. Open the downloaded APK on your phone.
3. If prompted, allow **Install unknown apps** for the browser or Files app you used, then tap **Install**.
4. Open **Walking Buddy**, pick a buddy and a goal, and enable step counting or choose manual entry.

Requires **Android 8.0 (API 26) or later**. No Google Play installation, account,
subscription, or network connection is needed. The APK is signed and is a
non-debuggable release. The private signing key stays outside version control.

## Google Play beta

The signed [Walking-Buddy-1.1.0.aab](dist/Walking-Buddy-1.1.0.aab) is the Play
Console upload (version code 2, target Android 16 / API 36). Use the APK above
for sideloading. Both retain the existing release certificate.

The [Play submission guide](docs/play/PLAY-CONSOLE.md) includes signing choices,
declaration answers, and the remaining publisher tasks. The [store listing](docs/play/STORE-LISTING.md),
[icon and feature graphic](docs/play/), [screenshots](docs/screenshots/), and
[privacy policy](docs/PRIVACY.md) are included. Google Play approval and rollout
still happen in the publisher's Console; a real-phone foreground-service demo,
support email, audience/rating choices, and account verification are required
where requested. No Play release has been submitted by this repository.

## What's inside

- Four starter pals: tabby cat, corgi, fox, and bunny. Unlock a frog, panda,
  penguin, and dragon at 20k, 40k, 60k, and 100k lifetime steps.
- Eight original four-frame pixel sprites, three pixel landscapes, and original icons.
- Custom daily goals, automatic background step counting, and editable manual entries.
- Feed, water, and clean your buddy. Each 500 steps earns a leaf; reaching your
  goal earns three bonus leaves. Snacks cost one leaf. Water and cleaning are free.
- Pet health, mood, gradual care decay, and a new-adoption/memorial loop.
  Reaching a goal restores 15 health; a missed daily goal costs 20 health.
- Daily streaks, weekly chart, milestone badges, personal bests, and history.
- Nicknames, scenery selection, reduced animation, progress sharing, CSV export,
  and JSON backup/restore through Android's file picker.
- Local-only storage. No ads, analytics, network permission, or third-party runtime SDKs.

This is an independent implementation of the core virtual-pet walking loop,
with a new interface and original artwork. It does not connect to Step Pals,
copy its assets, import its data, provide a shared online leaderboard, or use
Health Connect / watch data. The journal ranks your own best walking days.

## Step-counting behavior

Automatic counting uses Android's low-power hardware step counter in a health
foreground service, with a quiet persistent notification. Grant **Physical
activity** permission when asked. Notification permission is optional on
Android 13+; tracking can still operate when it is denied.

Counting begins when you enable tracking. It does not import steps taken before
installation or while tracking was paused. Carry the phone with you. The app
checks for a sensor and offers manual entry if none exists. Use manual entry
for steps from another tracker; avoid entering steps already counted by the app.

Sensor samples are rebaselined on install, reboot, pause/resume, and the first
sample after midnight. An ambiguous interval spanning midnight is deliberately
not assigned to either day, so a few steps around that boundary can be omitted.
Background restrictions, force-stop, and aggressive power saving can interrupt
tracking. Reopen the app to resume; keeping it unrestricted in your phone's
battery settings can help. These hardware behaviors need a real walking check
on your particular phone.

Progress survives ordinary app and phone restarts. Uninstalling clears it;
save a backup first. Restore validates the backup before replacing state and
pauses automatic tracking. Goal changes affect today and future days; history
keeps the goal originally used. Rewards cannot be repeated by lowering and
re-entering a manual total. Distance shown in the journal is an estimate.

## Build

Install JDK 17 and Android SDK platform 36/build-tools 35.0.0. Set `ANDROID_HOME`
or create `local.properties` containing `sdk.dir=/absolute/path/to/android-sdk`.
The checked-in Gradle wrapper downloads Gradle 8.13; Android Gradle Plugin is 8.13.2.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk` and installs under
`com.walkingbuddy.app.debug`, separate from the release app.

For a signed release, create a persistent key (do this once, and back it up):

```sh
mkdir -p signing
keytool -genkeypair -keystore signing/walking-buddy.jks \
  -alias walking-buddy -keyalg RSA -keysize 2048 -validity 10000 \
  -dname 'CN=Walking Buddy Developer, O=Walking Buddy, C=US'
# Follow keytool's prompts to choose a private password.
# Supply your WB_* environment variables, then:
./gradlew assembleRelease bundleRelease
```

For a release build, set `WB_KEYSTORE` (an absolute path), `WB_STORE_PASSWORD`,
`WB_KEY_ALIAS`, and `WB_KEY_PASSWORD` in your private environment. Alternatively,
create the ignored local file `signing/keystore.properties` with `storeFile`
(relative to `signing/`), `storePassword`, `keyAlias`, and `keyPassword` values.
There are no default signing passwords in this repository. The shipped APK's
signing key and local signing configuration are retained in the private,
ignored `signing/` directory. Reuse that key to publish updates that install over
this APK. A differently signed build requires uninstalling the existing app,
which erases its data; export a backup before doing so. Never commit a private key.

## Verification

The project includes 27 deterministic game-rule tests and a native device
integration runner. With an emulator or device connected:

```sh
./gradlew assembleDebug assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w \
  com.walkingbuddy.app.debug.test/com.walkingbuddy.app.SmokeInstrumentation
```

The integration runner resets **only the debug app** and exercises adoption,
manual step entry, goal rewards, care, navigation, validation, reduced animation,
and persistence. It writes screenshots to the debug app's external `files/qa`
directory. See [verification details](docs/VERIFICATION.md).

GitHub Actions uses Ubuntu 24.04 and Node 24 actions, runs unit tests and lint
for debug and release, and builds a debug APK plus an **unsigned** release bundle.
Reports and SDK/build logs are uploaded when they exist; missing required outputs
fail a successful build. No private signing material is available to CI. Use the
signed artifacts in `dist/` for distribution. Increment `versionCode` for each
new Play upload and regenerate `dist/SHA256SUMS` when replacing release files.

## Art and licensing

Source: [MIT](LICENSE). Original art: [CC0 1.0](art/LICENSE.md).
Regenerate assets with `python3 tools/generate_art.py` (requires Pillow).
The reference store screenshots were consulted for product research and are
not included in the app or repository.
