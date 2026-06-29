# Field Value UI Follow-up

This branch contains the core field-value domain, persistence, ViewModel, CSV, tests, and release documentation changes.

The matching Compose UI polish still needs to land in `app/src/main/java/com/maxxed/goldestimator/MainActivity.kt` before the PR is ready for review.

## UI changes to apply

- Home copy should identify the app as a field gold value estimator for panning, sluice cleanup, paydirt, tailings, and digging samples.
- Reference/setup screen should expose:
  - sample type picker
  - site/claim/bucket/cleanup label
  - optional gold price per gram
  - expected recovery percentage
  - 70%, 85%, and 95% recovery presets
  - last field setup prefill from `GoldUiState.lastFieldContext`
- Reference submit should call `setReference(referenceMm, measuredPixels, weightG, FieldContext(...))`.
- Correction screen should prefill the batch name with `GoldViewModel.suggestedBatchName`.
- Results screen should show a Gold-first field value card with:
  - visual Gold grams
  - recoverable grams
  - troy ounce equivalent
  - pennyweight equivalent
  - estimated value
  - price/recovery assumption
- History rows should show recoverable Gold grams and value.
- Compare rows should include recoverable Gold and value for Gold material rows.

## Validation after UI lands

Run:

```bash
./gradlew clean testDebugUnitTest lintDebug assembleDebug
```

Then device-test:

- camera deny/retry/grant
- six-angle capture
- duplicate angle rejection
- scale reference tap
- sample type, label, price, and recovery entry
- recovery preset buttons
- batch save/reopen
- compare selected batches
- copy summary
- CSV export columns

Keep the PR draft until the UI file and Android checks are complete.
