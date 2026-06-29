# Validation Matrix

All outputs must be described as visual estimates, never as chemical assay results.

| Fixture | Automated expectation | Physical-device expectation | Current evidence |
|---|---|---|---|
| Poor light | Exposure/contrast gate rejects fixture | Dark capture cannot advance | Unit fixture authored; execution blocked in this workspace |
| Missing scale | No volume or weight is invented | Analysis cannot start without a measured reference | Unit fixture authored and UI gate implemented; physical test pending |
| Bright gold-colored material | Defaults to Pyrite / mica, not Gold | Warning remains visible and user can assign Unknown | Unit fixture authored; physical fixture pending |
| Saturated gold-like material | Gold remains low-confidence visual label | Result states look-alike risk and assay limitation | Unit fixture authored; physical fixture pending |
| Plain background | Center subject is separated from border | Mask slider visibly changes included subject share | Unit fixture authored; physical test pending |
| Duplicate angle | Similar fingerprint or less than 12 degrees of pose separation is rejected | Repeating the same view cannot advance | Unit pose fixture authored and pipeline implemented; physical test pending |
| Known total weight | Density-weighted ranges are finite and ordered | Entered total drives material mass allocation | Unit fixture authored; physical test pending |
| No total weight | Scale-derived broad volume/depth range is used | UI explains broad uncertainty | Pipeline implemented; physical test pending |
| Field value | Recoverable gold value uses only Gold estimate, user price, and recovery percentage | Price/recovery edits visibly change Gold value and not non-gold rows | Unit fixture authored; physical test pending |
| Field units | Troy-ounce and pennyweight helpers convert from recoverable grams | Results and CSV show gram-first equivalents without changing source calculation | Unit fixture authored; physical test pending |
| Field input bounds | Blank/invalid price is ignored and recovery is clamped to 1-100% | Bad field inputs do not crash or produce negative value estimates | Unit fixture authored; physical test pending |
| Last setup prefill | Last sample type, label, price, and recovery are stored app-private | Starting another batch preloads the prior setup and remains editable | Pipeline implemented; physical test pending |

## Required physical set

Use dry, non-valuable fixtures under diffuse normal light and deliberately poor light. Include quartz, dark sand, ordinary soil/clay, mica or pyrite-like material, and at least one ambiguous gold-colored non-gold object. Record acceptance/rejection, mask corrections, assignments, CSV output, process restart behavior, and comparison behavior.

No READY claim is permitted until the automated suite and physical matrix have actually passed on the target Samsung device.
