# Ticket 05: Remove Legacy MiDaS SBS 3D Renderer & Native AI/OpenCV Bloat

## Context

`app/build.gradle.kts` currently pulls in `com.google.ai.edge.litert:litert`, `litert-gpu`, and `org.opencv:opencv` (~50+ MB of native libraries) solely for `Stereo3DRenderer.java`, an inherited experimental feature that ran monocular depth estimation on 2D video frames. Removing this cuts APK size dramatically and simplifies `Game.java` and `MediaCodecDecoderRenderer.java`.

## Files to Delete / Modify

- **Delete**:
  - `app/src/main/java/io/github/staernid/sunbeam/utils/Stereo3DRenderer.java`
  - Any MiDaS `.tflite` model or stereo 3D shader files in `app/src/main/assets/`
- **Modify**:
  - `app/src/main/java/io/github/staernid/sunbeam/Game.java`
  - `app/src/main/java/io/github/staernid/sunbeam/preferences/PreferenceConfiguration.java`
  - `app/build.gradle.kts`
  - `gradle/libs.versions.toml`
  - `README.md`

## Steps

1. Delete `Stereo3DRenderer.java` and any associated assets in `app/src/main/assets/`.
2. Remove all references to `Stereo3DRenderer` and 3D display mode in `Game.java`, `PreferenceConfiguration.java`, and string/preference resources.
3. Remove `litert`, `litert.gpu`, and `opencv` from `app/build.gradle.kts` and `gradle/libs.versions.toml`.
4. Remove the "SBS 3D for external displays (MiDaS v2 Lite)" line from `README.md`.

## Verification & Completion Criteria

Run the following commands and ensure every single one exits with code `0`:

```bash
./gradlew testDebugUnitTest
./gradlew ktlintCheck detekt :app:lintDebug
./gradlew :app:assembleDebug
```
