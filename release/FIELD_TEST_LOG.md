# Field Test Log Template

Use one row per fixture or real-world dry sample. Do not use valuable material for first-pass validation.

| Date | Device | Android | Sample type | Fixture/material | Lighting | Dry/wet | Reference object | Weight entered | Price entered | Recovery % | Expected behavior | Actual result | Pass/fail | Notes |
|---|---|---|---|---|---|---|---|---:|---:|---:|---|---|---|---|
|  |  |  | Pan concentrate | Black sand | Outdoor shade | Dry |  |  |  | 85 | Should not overstate Gold |  |  |  |
|  |  |  | Paydirt sample | Quartz + soil | Indoor diffuse | Dry |  |  |  | 85 | Should separate light/dirt clusters |  |  |  |
|  |  |  | Field sample | Pyrite/mica look-alike | Outdoor shade | Dry |  |  |  | 85 | Bright gold-colored material should warn/avoid overclaiming |  |  |  |
|  |  |  | Tailings / cleanup | Plain dark material | Low light | Dry |  |  |  | 85 | Poor light should be rejected or low confidence |  |  |  |
|  |  |  | Sluice concentrate | Mixed non-valuable sample | Direct sun/glare | Wet/glare |  |  |  | 85 | Glare should be rejected or warned |  |  |  |

## Required workflow notes

- Confirm camera permission deny, retry, and grant behavior.
- Confirm six genuinely different views are required.
- Confirm duplicate angle rejection.
- Confirm known-size reference tap is required before analysis.
- Confirm last field setup preloads into the next batch.
- Confirm CSV includes field label, sample type, recovery, price, recoverable grams, ozt, dwt, value, confidence, and disclaimer.
- Confirm copied summary includes visual-estimate-only language.
