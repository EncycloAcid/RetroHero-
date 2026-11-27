# RetroHero

An all-in-one retro game emulator for Android. RetroHero is a native **libretro frontend**: one app with one library, one controller setup and one save system, running 20+ classic consoles through open-source emulator "cores" that it downloads when needed.

## Features

- **One library for every system.** Pick your ROM folders and RetroHero scans them, works out each file's system and shows box art from libretro-thumbnails.
- **Cores download on demand.** The first time you play a system, RetroHero offers to download the right core for your device's CPU.
- **Touch controls built for each console.** Each system gets its own layout (SNES diamond, Genesis A/B/C, PlayStation shapes, arcade 6-button and so on), with multi-touch, D-pad diagonals, sliding between buttons and haptics. The layout changes for portrait and landscape.
- **Physical controllers.** Bluetooth and USB gamepads plus keyboards, with up to 4 players. The analog sticks, triggers and hat switch are mapped, and the touch overlay hides when a controller is used.
- **Saves.** Battery saves (SRAM) are written automatically, with 4 save-state slots and auto-save/resume on quit.
- **Fast-forward** (hold or toggle, 2–8×), reset, and per-core options editable in-game.
- **Sharp pixels.** Optional integer nearest-neighbour pre-scaling keeps pixel art crisp.
- **BIOS manager.** Import BIOS files or download them from a link. Each file is identified by MD5 checksum, so it's saved under the exact name the core expects and marked as verified.
- **Game downloader.** Paste a direct, Google Drive or Dropbox link to a game file (for example your own backup or a homebrew release). It's saved in the app's `Android/data/com.retrohero.app/files/Games` folder and added to your library straight away.
- Each game runs in its **own process**, so every core starts from a clean state.

## Supported systems

| System | Default core | Alternatives |
|---|---|---|
| NES / Famicom (+FDS) | FCEUmm | Nestopia UE |
| Super Nintendo | Snes9x | |
| Game Boy / Game Boy Color | Gambatte | mGBA |
| Game Boy Advance | mGBA | VBA Next |
| Nintendo DS (touch screen supported) | DeSmuME | melonDS |
| Virtual Boy | Beetle VB | |
| Genesis / Mega Drive, Master System, Game Gear, Sega CD | Genesis Plus GX | PicoDrive |
| Sega 32X | PicoDrive | |
| PlayStation | PCSX ReARMed | SwanStation |
| PC Engine / TurboGrafx-16 (+CD) | Beetle PCE Fast | |
| Neo Geo Pocket (Color) | Beetle NeoPop | |
| WonderSwan (Color) | Beetle WonderSwan | |
| Atari Lynx / 2600 / 7800 | Handy / Stella / ProSystem | |
| Arcade (FBNeo / MAME 2003-Plus romsets) | FinalBurn Neo | MAME 2003-Plus |

You can change a system's core by long-pressing any of its games.

**File detection tips:** files with unambiguous extensions (`.sfc`, `.gba`, `.nes` …) are detected anywhere. Shared extensions (`.cue`, `.chd`, `.bin`, `.zip`) are resolved from the folder name, e.g. `roms/psx/`, `roms/segacd/`, `roms/arcade/`, so it's best to keep one folder per system. A zip holding a single console game is extracted automatically. Any other zip is treated as an arcade romset.

## Architecture

```
app/src/main/
├── cpp/                     Native libretro frontend (C++17, NDK)
│   ├── frontend.cpp         dlopen()s a core, implements the libretro environment
│   │                        callbacks, runs the paced emulation thread, save states/SRAM
│   ├── video_output.cpp     Blits frames into the SurfaceView's ANativeWindow
│   │                        (RGB565 / XRGB8888 / 0RGB1555, optional integer pre-scale)
│   ├── audio_output.cpp     AAudio output with a lock-free ring buffer, linear
│   │                        resampler and dynamic rate control
│   ├── jni_bridge.cpp       JNI surface for NativeBridge.kt
│   └── libretro/libretro.h  Official libretro API header (MIT)
└── java/com/retrohero/app/
    ├── data/                Systems registry, ROM scanner (Storage Access Framework),
    │                        ROM cache (.cue/.m3u aware), core downloader, BIOS, settings
    ├── emulator/            EmulatorActivity (":game" process), virtual gamepad,
    │                        controller input mapping, JNI bindings
    └── ui/                  Jetpack Compose app: Games, Systems, Cores, Settings
```

**Cores** are loaded from `src/main/jniLibs/<abi>/lib<core>_libretro_android.so` when they are bundled in the APK, and otherwise downloaded from `buildbot.libretro.com/nightly/android/latest/<abi>/`.

## Building

Requirements: JDK 17+, Android SDK 37.1, NDK 27.2.12479018 and CMake 3.22.1 (Android Studio installs these automatically).

```bash
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-<abi>-debug.apk
./gradlew testDebugUnitTest    # also renders UI screenshots to app/build/screenshots/
```

Builds produce one APK per CPU type plus a universal one: `app-arm64-v8a-*.apk` (almost every phone), `app-armeabi-v7a-*.apk` (old 32-bit phones), `app-x86_64-*.apk` (emulators and Chromebooks) and `app-universal-*.apk` (runs anywhere, about twice the size). They share a version code and signing key, so any of them updates any other in place. CI runs the unit tests before building, and a failing test stops the APKs from being published.

**Crash reports.** Crashes in the app or in a game (including native core crashes and ANRs on Android 11+) are saved as text files in the app's private storage. After a crash the app offers to share the report, and *Settings → Crash reports* lists, shares or deletes them. Nothing is sent anywhere unless the player shares it.

`assembleRelease` is signed with the debug key so you can test a minified build. Set up your own signing config before you publish.

## Current limitations / roadmap

- **Hardware-rendered cores are not supported yet.** Cores that need an OpenGL ES or Vulkan context (N64, PSP, Dreamcast, 3DS and the like) refuse to start. The next big step is a GLES context with `RETRO_ENVIRONMENT_SET_HW_RENDER`, followed by a shader pipeline (CRT and LCD filters).
- Planned: rewind, multi-disc (`.m3u`) disc swapping UI, remappable controls, custom touch layouts, cheats, RetroAchievements, netplay and cloud save sync.
- **Google Play policy** doesn't allow apps to download executable code. To publish on Play, bundle the cores under `jniLibs` (or ship them as dynamic feature modules) instead of using the downloader. Sideloading and F-Droid-style distribution can use the downloader as it is.

## Legal

RetroHero contains no games, BIOS files or copyrighted console firmware. Only play games you legally own. Emulator cores are independent open-source projects with their own licenses (GPL, MPL, and several non-commercial ones such as Snes9x, Genesis Plus GX, PicoDrive, FinalBurn Neo and MAME 2003-Plus). Check those licenses before you distribute cores with the app, especially commercially.
