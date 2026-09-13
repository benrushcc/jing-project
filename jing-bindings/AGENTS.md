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

# Return Value Design

Native functions follow a fixed return value convention so the Java
side can consume them uniformly.

## Simple Returns: `-errno`

Most C standard library syscalls return a value >= 0 on success and a
negative value on failure, with the failure reason in `errno`. Since
`errno` is always positive, use it as the unified error convention:

- success: return a value >= 0
- failure: return `-errno`

The Java side treats a negative return as an error and takes the
absolute value to recover the actual `errno`.

```c
int jing_linux_epoll_ctl(int epfd, int socket, int op, int eventTypes,
                         int data) {
    if (epoll_ctl(epfd, op, socket, eventTypes, data) < 0) {
        return -errno;
    }
    return 0;
}
```

## Complex Returns: `jing_result`

When a function must return more than one value (for example a pointer
together with the length it points to), pass a `jing_result*`
out-parameter instead of returning a single value.

`jing_result` is defined in `src/jing_common.h` and is fixed at 16
bytes (`size_t len` plus a `jing_data` union), verified by a
`static_assert`. The `jing_data` union can carry byte/short/char/int/
long/float/double, a pointer, or an error code pair, so one
out-parameter accommodates different return data types.

Use the helper functions in `src/jing_common.h` to fill it:
- `jing_err_result(r, err)` / `jing_err_result_with_flag(r, err, flag)`
- `jing_byte_result` / `jing_short_result` / `jing_int_result` /
  `jing_long_result` / `jing_float_result` / `jing_double_result`
- `jing_ptr_result(r, ptr, len)` for pointer + length

Conventions:
- scalar results set `len` to `SIZE_MAX` (no length)
- pointer results set `len` to the real length
- errors set `len` to 0 and fill `err_val` (`err_code` + `err_flag`,
  with `JING_SYSTEM_ERROR_FLAG` = 0 for system errors)

The Java side reads this layout through
`io.jingproject.ffm.NativeSegmentAccess` in `jing-ffm`.

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