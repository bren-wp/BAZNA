# BAZNA Android — Google Play release gate (studeni 2026.)

This checklist covers the Android app only. **Passing CI does not imply Play approval**.

## Build prerequisites
- Android package ID: `com.brendigo.sidrena` (DO NOT change for an existing Play listing).
- `minSdk = 26`, `targetSdk = 36` (Google Play minimum for new apps and updates after 31 August 2026).
- Java 17 / Gradle 9.6 / AGP 9.4; Android SDK 36 for compilation, target remains Android 16.
- Stable Compose BOM, AndroidX Activity/Lifecycle, Kotlin Compose compiler, coroutines and OkHttp.
- Keep R8/shrinking on. Verify API 26–37 execution and large-screen/large-font behavior.
- Build with `cd android && gradle --no-daemon :app:testDebugUnitTest :app:lintDebug :app:bundleRelease`.
- CI-produced `BAZNA-Android-unsigned.aab` is **not uploadable until signed** with the authorized upload key.

## Signing and store delivery
1. Retain the existing `applicationId` and use a `versionCode` greater than the latest uploaded Play artifact.
2. In Android Studio choose **Build → Generate Signed Bundle / APK → Android App Bundle**.
3. Select the correct **upload key** previously registered for this app; never commit a keystore or passwords.
4. Confirm the resulting AAB is signed with the intended upload certificate.
5. Upload the AAB to Play Console (internal test first), where **Play App Signing** manages distribution signatures.
6. Inspect the **Android vitals**, pre-launch report, permission declarations, crash/ANR results and Play warnings.

## Privacy and store metadata — manual requirements
- The app reads public prices directly from a site chosen by the user, over HTTPS. The site can see the client's network metadata, including its IP address, when serving data/images.
- The app stores the chosen site URL and outlet selection in local preferences; no admin credentials are required or collected by the Android app.
- A visible in-app link opens the existing [Brendigo privacy policy](https://brendigo.com/politika-privatnosti/).
- **Before release**, add BAZNA-Android-specific disclosures to that public HTML privacy policy; the current generic website policy alone has not been confirmed sufficient.
- Complete Play Console **Data safety** accurately based on an SDK, network and server-side data flow review. Do not claim "no data collected" without auditing the remote publisher and SDKs.
- Provide store description, icon, feature graphic, real phone screenshots, contact details, content rating, app-access information, ads declaration, country availability and applicable declarations.
- The app is public-catalog **read-only**; do not advertise mobile price editing or automatic bidirectional sync.
- For future native SDK additions, verify 16 KB page compatibility (if native `.so` files are present).
- Validate high font scale, TalkBack, keyboard navigation, gesture insets and foldable/tablet layouts with real screenshots; 1:1 pixel parity needs original per-screen PNG references and measured pixel differences.

## Sources
- https://support.google.com/googleplay/android-developer/answer/11926878
- https://support.google.com/googleplay/android-developer/answer/10144311
- https://developer.android.com/studio/publish/app-signing
- https://developer.android.com/guide/practices/page-sizes
