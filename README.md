# Sunbeam

Android game-stream client for [Sunshine](https://github.com/LizardByte/Sunshine). Targets the official server only.

Fork lineage: [moonlight-stream/moonlight-android](https://github.com/moonlight-stream/moonlight-android)
→ [ClassicOldSong/moonlight-android](https://github.com/ClassicOldSong/moonlight-android) (Artemis) → Sunbeam.
Redesigned in modular Kotlin and Jetpack Compose (`core/*`, `features/*`).

✅ = implemented · ⬜ = planned

## Features

### Streaming
- ✅ H.264 / HEVC / AV1 / HDR via `MediaCodec`
- ✅ Custom resolution and bitrate
- ✅ Frame-pacing modes
- ✅ Video scale: fit / fill / stretch, pan and zoom
- ✅ Qualcomm SGSR1 upscaling (experimental)
- ✅ Portrait mode, in-game rotation
- ✅ External display mode, Samsung DeX
- ⬜ Surface producer-throttling off, SPS-patch removal (see [upstream triage](docs/UPSTREAM_TRIAGE.md))

### Input
- ✅ Mouse modes: normal, multi-touch, touchpad, local cursor, disabled
- ✅ Touchpad sensitivity and virtual touchpad space
- ✅ Customizable on-screen gamepad, import/export, joystick skins
- ✅ Gamepad: rumble, gyro, LED, device-vibration override, debug page
- ✅ Physical-keyboard support, non-QWERTY layouts, soft-keyboard toggle
- ✅ Custom shortcut commands, in-game menu
- ⬜ Android 16.1 native keyboard capture
- ⬜ Xbox Series S/X USB driver init, F13-F24, PrtSc

### App
- ✅ Host discovery (mDNS + coroutines), manual add, PIN pairing, Wake-on-LAN
- ✅ Stream profiles, live Gamepad Tester, and On-Screen Control layout preview
- ✅ Compose UI, Material 3, dynamic color, and OLED Black mode
- ✅ Kotlin, coroutines, modular architecture (`core/*`, `features/*`), Metro DI, Coil 3
- ✅ Settings store on DataStore (`kotlinx-serialization-json`)
- ✅ Unit and Roborazzi screenshot tests in CI
- ⬜ Up-to-date translations from upstream

## Release screenshots

Each tagged release includes curated emulator screenshots of the Hosts, Apps, Profiles, and Settings
screens as release assets. They use deterministic sample content and can be reviewed and added to
this README when a release includes UI changes.

## Build

```sh
git submodule update --init --recursive
echo "ndk.dir=/path/to/ndk" >> local.properties
./gradlew :app:assembleDebug
```

Flavors and test setup: [android_test_setup.md](android_test_setup.md). Contributor and agent rules: [AGENTS.md](AGENTS.md).

## Legal

Sunbeam is a modified version of Moonlight Android and of its fork Artemis, released under GPLv3
([LICENSE.txt](LICENSE.txt)). It is not affiliated with those projects, LizardByte or NVIDIA.
Modifications are recorded in the git history, which is kept intact. Original Moonlight authors:
Cameron Gutman, Diego Waxemberg, Aaron Neyer, Andrew Hennessy. Artemis: ClassicOldSong.

## License

GPLv3, see [LICENSE.txt](LICENSE.txt).
