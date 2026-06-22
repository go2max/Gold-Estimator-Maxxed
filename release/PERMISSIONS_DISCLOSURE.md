# Permissions Disclosure

## Camera

The app requests `android.permission.CAMERA` only to create the six user-initiated sample photographs used by the offline visual-estimation workflow. Denial leaves the rest of the app available and provides an in-app retry action.

No storage permission is requested. CSV export uses Android's system document picker. No network permission is requested.
