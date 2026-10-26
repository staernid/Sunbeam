# Sunbeam

Android game-stream client for [Sunshine](https://github.com/LizardByte/Sunshine). Targets the official server only.

Fork lineage: [moonlight-stream/moonlight-android](https://github.com/moonlight-stream/moonlight-android)
→ [ClassicOldSong/moonlight-android](https://github.com/ClassicOldSong/moonlight-android) (Artemis) → Sunbeam.
Direction: a full redesign in Kotlin and Compose. See [docs/ROADMAP.md](docs/ROADMAP.md).

✅ = implemented today (inherited from the lineage, not yet re-audited) · ⬜ = planned

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

### Experimental
- ✅ SBS 3D for external displays (MiDaS v2 Lite), under review

### App
- ✅ Host discovery (mDNS), manual add, PIN pairing, Wake-on-LAN
- ✅ Per-host profiles
- ⬜ Compose UI, Material 3, dynamic color
- ⬜ Kotlin, coroutines, modular architecture
- ⬜ Settings store on DataStore
- ⬜ Unit and screenshot tests in CI
- ⬜ Up-to-date translations from upstream

## Build

```sh
git submodule update --init --recursive
echo "ndk.dir=/path/to/ndk" >> local.properties
./gradlew :app:assembleNonRoot_gameDebug
```

Flavors and test setup: [android_test_setup.md](android_test_setup.md). Contributor and agent rules: [AGENTS.md](AGENTS.md).

## Legal

Sunbeam is a modified version of Moonlight Android and of its fork Artemis, released under GPLv3
([LICENSE.txt](LICENSE.txt)). It is not affiliated with those projects, LizardByte or NVIDIA.
Modifications are recorded in the git history, which is kept intact. Original Moonlight authors:
Cameron Gutman, Diego Waxemberg, Aaron Neyer, Andrew Hennessy. Artemis: ClassicOldSong.

## License

GPLv3, see [LICENSE.txt](LICENSE.txt).
