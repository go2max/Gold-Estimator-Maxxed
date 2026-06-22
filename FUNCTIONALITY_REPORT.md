# Functionality Report

Date: June 21, 2026

## Implemented

- CameraX preview, permission retry, app-private JPEG capture, and six guided views.
- Rotation-vector pose tracking with 12-degree minimum separation, plus image-fingerprint duplicate rejection.
- Exposure, contrast, sharpness, glare, and center-subject checks before a view is accepted.
- Two-point known-size calibration and optional measured total batch weight.
- Offline border-derived foreground segmentation and deterministic six-cluster RGB analysis.
- User mask-sensitivity correction and explicit material reassignment for every cluster.
- Gold-first ordered visual-share, density, volume, weight, and confidence ranges.
- Local saved batches, deletion, two-batch comparison, clipboard summary, and system-picker CSV export.
- Camera-only permission model, app-private storage, no network dependency, and safe field reference.

## Calculation limits

Surface pixels do not establish internal composition. Without a measured total weight, volume uses calibrated projected area and a broad 3-12 mm depth assumption. Density ranges are reference assumptions, not measured purity. Bright gold-colored clusters default away from Gold, and any Gold label remains deliberately low confidence.

## Verification result

Synthetic fixture tests were authored for poor light, missing scale/mass, bright gold-colored look-alikes, foreground masking, pose duplication, and range ordering. They could not be executed here because Gradle 8.9 download returned HTTP 403 and the workspace has no cached distribution or Android SDK.

No target phone or production upload key was available. No APK/AAB, hashes, signer verification, release lint result, or physical fixture pass is claimed. See `RELEASE_READINESS.json` for the exact release gate.
