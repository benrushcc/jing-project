# Purpose

`jing-bindings` provides FFI (Foreign Function Interface) downcall
bindings for the native libraries needed by other project modules.
Java-side code lives in `src/main/java`, and native-side (C) code lives
in `src/main/native`. Project-wide conventions and Java build
instructions are described in the `AGENTS.md` at the repository root;
only module-specific details are documented here.

# Directory Layout

- `src/main/java`: a simple wrapper that allows the Java layer to call
  into the native layer.
- `src/main/native`: C sources and the CMake/Ninja build setup.
  - `src/*.c` / `src/*.h`: native implementation and headers.
  - `CMakePresets.json`: defines the `windows` / `linux` / `macos`
    configure presets.
  - `CMakeLists.txt`: builds the `jing_bindings` shared library (plus
    the optional `jing_demo` library used by tests).
  - `vs-env.ps1`: Windows-only; imports the Visual Studio developer
    environment into the current shell.
  - `clang-format.ps1` / `clang-format.sh`: format all C/C++ sources
    under `src`.

# Native Build

- Build system: CMake with the Ninja generator, driven by the presets
  in `CMakePresets.json`.
- Target platforms: Windows, Linux, and macOS, on both x64 and aarch64.
- Platform dispatch in C code is done via the `JING_OS_*` macros defined
  in `src/jing_common.h`: `JING_OS_WINDOWS`, `JING_OS_LINUX`,
  `JING_OS_MACOS`.
- Focus only on the code for the current platform. For example, on a
  Windows machine only work with the Windows implementation
  (`jing_win.c`, `jing_win.h`, `WinBindings.java`, etc.) and ignore
  Linux/macOS code; the corresponding code is validated on those
  platforms.
- Configure + build (run the preset matching the current OS):
  - Windows: `cmake --preset=windows && cmake --build build`
  - Linux: `cmake --preset=linux && cmake --build build`
  - macOS: `cmake --preset=macos && cmake --build build`
- Windows note: before configuring or building on Windows, run
  `vs-env.ps1` (dot-source it) so the current shell has the Visual
  Studio environment (cl.exe, INCLUDE/LIB, etc.). For example:
  `. .\vs-env.ps1; cmake --preset=windows && cmake --build build`

# Formatting

- Native code is formatted with `clang-format` using the config in
  `src/main/native/.clang-format`.
- After writing or editing native code, run the formatter for the
  current platform:
  - Windows: `.\clang-format.ps1`
  - Linux/macOS: `./clang-format.sh`

# Changes Tracking

After modifying any code in this module, append a Chinese description
of the change reason to `../diff.md` in the project root directory.