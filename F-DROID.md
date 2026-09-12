# F-Droid packaging notes

This project is intended for inclusion in the official F-Droid repository.

## Build and licensing

- The app is built from source with Gradle.
- The app does not use Firebase, Google Play Services, advertising, analytics, or secret API keys.
- The application is licensed under MIT; see `LICENSE`.
- The current release is version `1.0` with versionCode `1`.
- The release should be tagged in Git (for example `v1.0`) before submitting packaging metadata.
- F-Droid should build from the tagged source commit and use its own signing key.

## Store metadata

F-Droid can consume the Fastlane-compatible metadata under `fastlane/metadata/android/` in this source repository. The official F-Droid build metadata is maintained separately in the `fdroiddata` repository and should reference this repository and a fixed release commit.

See the official F-Droid Build Metadata Reference and submission guide for the current requirements.
