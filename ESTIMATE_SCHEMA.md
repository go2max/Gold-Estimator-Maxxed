# Saved Estimate Schema

`BatchResult` is the app-private persisted record for schema version 1. The JSON store contains:

- stable ID, user name, and creation timestamp
- accepted angle count
- reference length in millimeters and calibrated pixels per millimeter
- optional measured total weight in grams
- mask sensitivity
- visible clusters with mean RGB, share, suggested material, and visual-label confidence
- user material assignment for every cluster
- ordered material estimates with visible-share, density, volume, weight, and confidence ranges
- overall confidence and interpretation warnings

The saved record intentionally does not call any visual label a chemical identification. CSV exports use explicit unit-bearing columns and the same estimate disclaimer.

Captured JPEGs remain app-private working files and are excluded from Android backup. Saved results do not require the JPEGs to reopen, compare, copy, or export.
