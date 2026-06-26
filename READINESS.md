# Readiness

Last updated: 2026-06-25

## Status

**IMPLEMENTED BASELINE / RELEASE EVIDENCE PENDING**

Maxxed Gold Estimator has an offline six-angle visual-estimation workflow documented, but it is not production-ready until current-branch verification, signed artifacts, and real-device acceptance evidence are recorded.

## Current evidence

- Six-angle CameraX capture flow is documented.
- Image-quality rejection, known-size scale measurement, segmentation, cluster material review, visible-share/density/volume/weight/confidence ranges, local save/compare/copy/export flows, and conservative interpretation copy are documented.
- Privacy posture is local-first: no network permission, analytics SDK, advertising SDK, account, or cloud sync.
- Release build path requires external `MAXXED_RELEASE_PROPERTIES` and intentionally has no debug-signing fallback.

## Blocking launch gates

- Run `./gradlew testDebugUnitTest lintDebug assembleDebug` on the current release candidate.
- Run signed-release verification with production upload key: `./gradlew clean testDebugUnitTest lintRelease assembleRelease bundleRelease`.
- Record APK/AAB hashes and signer evidence.
- Run physical acceptance with controlled sample photos, known-size references, lighting variation, wet/dry or glare cases, and obvious look-alike materials.
- Confirm all store copy preserves the visual-estimate-only limitation and does not imply assay, purity, legal valuation, or guaranteed identification.
- Finalize Play listing, privacy policy URL, screenshots, data-safety declaration, and support URL.

## Ready when

Mark **READY** only after automated checks pass, signed artifacts are verified, physical sample acceptance is documented, conservative disclaimer copy is present in app and store materials, and Play Console legal/support metadata is complete.