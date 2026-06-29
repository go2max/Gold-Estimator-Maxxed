# Functionality Report

Date: June 28, 2026

## Implemented

- CameraX preview, permission retry, app-private JPEG capture, and six guided views.
- Rotation-vector pose tracking with 12-degree minimum separation, plus image-fingerprint duplicate rejection.
- Exposure, contrast, sharpness, glare, and center-subject checks before a view is accepted.
- Two-point known-size calibration and optional measured total batch weight.
- Field sample context for pan concentrate, sluice concentrate, paydirt, hard-rock specimens, tailings, or generic field samples.
- Optional user-entered gold price per gram and expected recovery percentage for rough recoverable-value ranges, with conservative/normal/careful recovery presets planned for the UI.
- Field input normalization for site labels, positive price values, and 1-100% recovery bounds.
- App-private last-field-setup prefill for repeated pan, cleanup, or bucket checks.
- Offline border-derived foreground segmentation and deterministic six-cluster RGB analysis.
- User mask-sensitivity correction and explicit material reassignment for every cluster.
- Gold-first ordered visual-share, density, volume, weight, recoverable gold, troy-ounce/pennyweight equivalents, value, and confidence ranges.
- Local saved batches, deletion, two-batch comparison, clipboard summary, and system-picker CSV export.
- Camera-only permission model, app-private storage, no network dependency, and safe field reference.

## Calculation limits

Surface pixels do not establish internal composition. Without a measured total weight, volume uses calibrated projected area and a broad 3-12 mm depth assumption. Density ranges are reference assumptions, not measured purity. Field value depends on user-entered price and recovery rate, and should be treated as a rough planning number. Bright gold-colored clusters default away from Gold, and any Gold label remains deliberately low confidence.

## Verification result

Synthetic fixture tests were authored for poor light, missing scale/mass, bright gold-colored look-alikes, foreground masking, pose duplication, range ordering, recoverable field value, unit conversions, generated field batch names, and field input normalization. They could not be executed here because Gradle 8.9 was not cached and the workspace could not reach `services.gradle.org`.

No target phone or production upload key was available. No APK/AAB, hashes, signer verification, release lint result, or physical fixture pass is claimed. See `RELEASE_READINESS.json` for the exact release gate.
