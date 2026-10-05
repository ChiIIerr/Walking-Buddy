# Uploading the first Walking Buddy beta

Prepared October 4, 2026. This packet prepares the app and supporting material; it does not mean Google has reviewed or approved it.

## Release identity and signing

Upload **dist/Walking-Buddy-1.1.0.aab** to an Internal testing release. The APK alongside it is for sideloading; it is not the Play upload. The bundle uses package `com.walkingbuddy.app`, version code **2**, version name **1.1.0**, minimum API **26**, target API **36**. Package names are permanent after the first upload. If this package name is unavailable in your account, stop and change it consistently before the first Play release.

The AAB is signed with the existing Walking Buddy certificate, matching the sideloaded release. Keep its private keystore and passwords backed up securely; neither is in GitHub. The local key is under the ignored `signing/` folder in the development checkout.

Enroll in Play App Signing. To preserve direct upgrades from the original sideloaded APK, choose the option to provide the existing app-signing key and follow Play's encrypted PEPK export instructions. Never upload the raw keystore to GitHub. If you let Google generate a new app-signing key, the Play-installed app will have a different certificate from the sideloaded app: export progress, uninstall the sideloaded copy, install from Play, and restore the backup. The key signing the uploaded AAB can serve as the upload key; Google signs the APKs delivered to users with the app-signing key.

GitHub CI intentionally has no release key. Its `unsigned-validation-bundle` artifact is a build check, not the signed Play upload. Use the signed bundle in `dist/`.

## App content answers based on this release

Review the live Console wording before submitting. Update these declarations whenever app behavior or SDKs change.

| Form | Current behavior / answer |
| --- | --- |
| App access | All functionality available without an account, login, payment, or reviewer credentials. Manual step entry works on devices without a step sensor. |
| Ads | No ads or advertising SDKs. |
| Health apps | **Activity and Fitness**: on-device step tracking and walking goals. It is not a medical device. No Health Connect integration or medical claims. |
| Data safety: collection | No app data collected off-device by the developer. Sensor readings and history are processed only locally. No Internet permission or analytics SDK. |
| Data safety: sharing | No automatic sharing. User-initiated Android sharing and chosen document exports fall under the applicable user-initiated transfer exclusions; assess the current form instructions. |
| Account creation/deletion | No accounts. Reset all progress deletes local records and recovery copies; exported files must be deleted separately. |
| Advertising ID / location | Neither permission is requested. |
| Foreground service | Declare **health** (`FOREGROUND_SERVICE_HEALTH`), on-device step counting. See the text and video requirements below. |
| Target audience / content rating | Publisher must choose intended age groups and complete the questionnaire, including the pet-death mechanic where relevant. Do not assume that cartoon pets make the app child-directed. |

The app reads fitness information locally even though no information is collected off-device under the Data safety definition. Declare its fitness functionality accurately in the Health apps form. The privacy policy is available offline from onboarding and Settings, and publicly at the URL in STORE-LISTING.md. It identifies Walking Buddy and offers a contact mechanism. Supply the publisher's monitored support email in the Play listing.

## Foreground-service declaration text

**Feature:** The user taps Enable step tracking and grants Physical activity permission. Walking Buddy then registers the phone's low-power step-counter sensor in a health foreground service, showing current step progress in an ongoing notification. It updates locally stored daily walking totals, goal progress, and virtual-pet rewards while the app is in the background. The user can pause it from Settings or the notification. Tracking may resume after reboot/update only if it was previously enabled.

**Why immediate and continuous:** Delaying the service prevents counting from beginning when the user enables it. Interrupting sensor registration can omit steps or introduce ambiguous gaps, especially across reboots or day boundaries, producing incomplete daily goals and rewards. Counting uses the phone's low-power hardware counter and does not send data to a server.

**Use case:** Health / on-device activity tracking (the Console may label this Health Data Sync and include step counters in its examples).

**Required demonstration video — record on a phone with a step-counter sensor:**
1. Open Walking Buddy; show onboarding or Settings and the enable-tracking control.
2. Enable tracking and show the Physical activity permission prompt; allow it. Allow notifications for a visible demonstration.
3. Walk a short distance with the phone; show the total increasing.
4. Press Home, open the notification shade, and show the ongoing Walking Buddy progress notification while the app is in the background.
5. Use Pause tracking from the notification; reopen the app and show tracking is paused.
6. Upload the recording to a reviewer-accessible, unlisted video URL and paste that URL into the foreground-service declaration.

Do not substitute manual entries or simulated steps for the hardware demonstration. The local emulator tests do not validate real walking accuracy or OEM battery restrictions.

## Internal beta rollout

1. Create the app in your verified Play Console account as Walking Buddy, free, English (United States). Complete any account verification/device verification tasks.
2. In Testing > Internal testing, create a tester email list or Google Group (up to 100 testers).
3. Create a release, enroll in Play App Signing as described above, upload the signed AAB, and add the release notes from STORE-LISTING.md.
4. Complete any blocking setup tasks shown by your account and release. Internal testing can start before the full public listing; internal-only apps are exempt from the Data safety section, but closed/open/production tracks are not.
5. Review and roll out the internal release, then share the opt-in link. Testers must join with the same Google account used in Play Store.
6. Run Play's pre-launch report, inspect crashes and accessibility findings, and do a real-phone walking/background/reboot/pause check before expanding the beta.
7. For a closed beta, complete the listing, privacy/Data safety/health/foreground-service declarations, rating, intended audience, regions, and tester selection before rollout.

For personal developer accounts created after November 13, 2023, production access requires a closed test with at least 12 continuously opted-in testers for 14 consecutive days and an application for production access. Internal testing does not satisfy that requirement. Open testing may also require production access on these accounts.

## Official references

- [Target API requirements](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en)
- [Testing tracks](https://support.google.com/googleplay/android-developer/answer/9845334?hl=en)
- [New personal account testing requirements](https://support.google.com/googleplay/android-developer/answer/14151465?hl=en)
- [Play App Signing](https://support.google.com/googleplay/android-developer/answer/9842756?hl=en)
- [Data safety definitions and exclusions](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en)
- [Health apps declarations](https://support.google.com/googleplay/android-developer/answer/14738291?hl=en)
- [Foreground service declarations](https://support.google.com/googleplay/android-developer/answer/13392821?hl=en)
- [Listing asset requirements](https://support.google.com/googleplay/android-developer/answer/9866151?hl=en)
