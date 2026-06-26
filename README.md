# Maxxed Gold Estimator

Maxxed Gold Estimator is an offline Android tool for producing conservative visual material estimates from a guided six-angle photo set. It does not identify chemical composition and is not a substitute for a laboratory assay, appraisal, purity test, or legal valuation.

## Website Summary

Use this copy when referencing the app externally:

> Maxxed Gold Estimator is an offline Android visual-estimation tool for photographed mineral or material batches. It guides six-angle capture, known-size calibration, on-device segmentation, material-cluster review, visible-share estimates, confidence ranges, local saved batches, comparison, and CSV export while clearly warning that results are visual estimates only.

## Readiness

Current release status is tracked in [`READINESS.md`](READINESS.md). Do not treat this app as production-ready until automated checks, signed artifacts, physical sample acceptance, and conservative store/legal copy are verified.

## Implemented Workflow

1. Capture top, front, right, back, left, and low-oblique views with CameraX.
2. Reject poor exposure, low contrast, blur, glare, missing subject presence, near-duplicate images, and insufficient device-angle separation.
3. Measure a known-size object by tapping its two endpoints; optionally enter measured total batch weight.
4. Segment the subject from its border background and cluster visible colors on device.
5. Adjust mask sensitivity and explicitly review or change every cluster's material assignment.
6. Calculate visible-share, density, volume, weight, and confidence ranges.
7. Save batches locally, compare two batches, copy a text summary, or explicitly export CSV through Android's document picker.

Results always place Gold first, followed by Quartz, Pyrite / mica, Black sand, Dirt / clay, and Unknown.

## Privacy And Safety Posture

- Analysis is deterministic and on-device.
- The app has no network permission, analytics SDK, advertising SDK, account, or cloud sync.
- Captures are held in app-private storage and excluded from backup.
- CSV leaves app-private storage only after the user chooses a destination.
- Results are visual estimates only and must not be presented as assay, purity, chemical-composition, legal-value, or guaranteed identification results.

## Build

Android Studio Ladybug or newer with JDK 17 and Android SDK 35:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

For a signed release, create a properties file outside the repository:

```properties
storeFile=/absolute/path/to/upload-key.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Then run:

```bash
MAXXED_RELEASE_PROPERTIES=/absolute/path/to/release.properties \
  ./gradlew clean testDebugUnitTest lintRelease assembleRelease bundleRelease
```

The release build intentionally has no debug-signing fallback.

## Release Gate

Before Play submission:

1. Run debug and signed-release checks on the release candidate.
2. Build signed APK/AAB artifacts and record hashes plus signer evidence.
3. Run physical sample acceptance with controlled sample photos, known-size references, lighting variation, glare/wetness cases, and look-alike materials.
4. Confirm in-app and store copy preserves the visual-estimate-only limitation.
5. Verify support/privacy URLs, screenshots, Play Console data-safety declaration, and release notes.

## Interpretation

Color, surface visibility, wetness, lighting, scale placement, masking, and look-alike minerals can substantially change an estimate. Use a qualified assay service for consequential identification or valuation.
