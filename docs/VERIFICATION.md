# Walking Buddy 1.1.0 verification

Verified October 4, 2026 (local time).

- Release APK and Android App Bundle built with JDK 17, Gradle 8.13, AGP 8.13.2,
  compile/target SDK 36, min SDK 26, version code 2.
- `testDebugUnitTest`: **27 tests, zero failures, zero errors**.
- `lintDebug` and `lintRelease`: **zero errors**. Remaining warnings concern
  English-only UI text, programmatically constructed views, small draw allocations,
  the selected supported AGP version, and a false-positive suggestion that the
  app's custom signature permission is a misspelled Internet permission.
- Native integration runner on Android 16 / API 36, Pixel 6 arm64 AOSP ATD emulator:
  **31 passing checks**, including complete deletion of recovery copies on reset,
  the offline privacy policy with its online link, and the wellness/about dialog.
  Rendering was enabled on the ATD device before the final run and screenshot capture.
- Tests also cover adoption, companion choice, nickname persistence, manual input,
  goal rewards, feeding, navigation, goal validation/editing, reduced animation,
  state serialization, and activity restart. Deterministic rules cover combined
  totals, counter baselines, reboot, midnight, duplicates, and delayed samples.
- Google bundletool 1.18.3 validation passed. A universal APK generated from the
  signed AAB successfully installed over the original 1.0.0 APK and launched.
  Package inspection confirmed `com.walkingbuddy.app`, version 1.1.0/code 2,
  target SDK 36, and no `DEBUGGABLE` flag.
- APK Signature Scheme v2 verification passed. The AAB's JAR signature verified;
  the expected self-signed development certificate is valid until 2054.
- The APK and bundle contain no native `.so` libraries, avoiding native ABI and
  native 16 KB page-size alignment dependencies. Java bytecode is universal.
- Original and updated APKs retain the signing certificate SHA-256:
  `d311d35cb5a14a280ec0bffb554c2fc3f67ab71819c011b8ff1de6180441f646`.
- Checksums for both release formats are recorded in `dist/SHA256SUMS`.
- The packaged offline policy matches `docs/PRIVACY.md` byte-for-byte. No Internet,
  advertising ID, location, body-sensor, or Health Connect permission is requested.
  Automatic backups and device transfers are excluded, including the legacy backup flag.
- The original 512 × 512 icon and 1024 × 500 feature graphic are opaque RGB PNGs.
  The phone screenshots are 1080 × 1920, opaque RGB PNGs captured from the Android 16 debug app
  with explicitly entered demo steps. The distributable app starts with no progress.

## GitHub build repair

The previous run failed before Gradle: `android-actions/setup-android@v3` requested
Google's removed `tools` SDK package. As a result, test/lint reports never existed.
The workflow now uses the Ubuntu 24.04 runner's SDK tools directly, installs API 36,
uses SHA-pinned Node 24 checkout/setup-java/upload actions, and explicitly runs Bash
with pipeline error propagation. It builds a debug APK and unsigned validation
bundle, tests and lints both variants, checks required report paths, and uploads
existing reports and diagnostic logs. CI does not receive the private release key.

## Remaining real-device and publisher checks

The emulator has no hardware `TYPE_STEP_COUNTER` sensor. Actual walking,
background counting, permission grants/denials on a sensor-equipped phone, reboot
behavior on a physical phone, and OEM power-management restrictions still need
real-device verification. The sensor-absent/manual fallback was exercised.
Android 8–14 are supported but were not device-tested for this release.

No Google Play submission, pre-launch report, or approval has occurred. The
publisher must supply a support email, choose audience/regions and content-rating
answers, finish account verification where required, select Play App Signing,
and provide the real-phone foreground-service demonstration video requested by
Play. See [the submission guide](play/PLAY-CONSOLE.md).

## Screens

![Adoption](screenshots/01-adoption.png)
![Today](screenshots/02-today.png)
![Buddies](screenshots/03-buddies.png)
![Journal](screenshots/04-journal.png)
![Settings](screenshots/05-settings.png)
