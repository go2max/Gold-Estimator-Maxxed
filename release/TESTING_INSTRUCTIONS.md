# Release Testing Instructions

1. Run `./gradlew clean testDebugUnitTest lintRelease assembleRelease bundleRelease` with `MAXXED_RELEASE_PROPERTIES` set.
2. Verify the APK with `apksigner verify --verbose --print-certs` and the AAB with `jarsigner -verify -verbose -certs`.
3. Confirm the release manifest is not debuggable and requests Camera but no network or storage permission.
4. Install on the Samsung S22 Ultra and deny Camera once, retry, then grant it.
5. Complete every fixture in `release/VALIDATION.md`, including six genuinely different views.
6. Force-stop after capture, correction, save, copy, and export checkpoints; document what survives and what restarts safely.
7. Verify light/dark mode, large font, airplane mode, rotation lock, deletion, two-batch comparison, clipboard text, and CSV column order.
8. Confirm every results surface says visual estimate or equivalent and never implies chemical identification.
