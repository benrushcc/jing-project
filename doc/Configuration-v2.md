# Configuration (cfg) Design

This document describes the configuration subsystem of `jing-common`, derived from the
current source code. The old `doc/Configuration.md` is kept for historical reference but
may not match the implementation; this document is authoritative.

## 1. Overview

The configuration subsystem is a pluggable, SPI-based facility. Its design goals are:

- **Pluggable**: users may replace the whole configuration source through the Java
  `ServiceLoader` SPI, e.g. to read from a remote config center, a database, or any
  custom source.
- **Recommended default**: the bundled default implementation reads a configuration
  file from the classpath and is intended to be used directly in most cases.
- **Three file formats**: the default implementation supports `toml`, `json` and
  `properties` configuration files.
- **Zero third-party dependencies**: all parsers are hand-written, consistent with the
  project-wide rule that modules other than `jing-bench` must not depend on external
  libraries.

The configuration model is a tree of string values: the only first-class value types are
a single string, an array of strings, and a nested object. There are no numbers,
booleans, or nulls inside the model; typed access is provided as a convenience layer on
top.

## 2. Architecture

```
ConfigurationFactory            (io.jingproject.common)   static user-facing entry
        |
        |  Anchor.compute + Holder (lazy singleton)
        |  ServiceLoader.findFirst() -> user SPI impl, else DefaultConfigurationFacade
        v
ConfigurationFacade             (io.jingproject.common)   SPI interface
        ^
        |
DefaultConfigurationFacade      (io.jingproject.common.conf)   default impl
        |
        |  reads classpath resource via TCCL, caches parsed result
        v
TomlCfgReader / JsonCfgReader / PropertiesCfgReader       hand-written parsers
        |
        v
CfgObject -> CfgItem / CfgList / CfgObject                sealed data model
```

### 2.1 SPI interface: `ConfigurationFacade`

```java
public interface ConfigurationFacade {
    String conf(String key);

    List<String> confList(String key);
}
```

- `conf(key)` returns the string value for `key`, or `null` if the key does not exist.
- `confList(key)` returns the string array for `key`, or `null` if the key does not exist.
- `jing.common` declares `uses io.jingproject.common.ConfigurationFacade` in its
  `module-info.java`, so any module on the module path may provide an implementation.

### 2.2 Static entry: `ConfigurationFactory`

`ConfigurationFactory` is the only class users normally touch. It resolves the
`ConfigurationFacade` implementation once and caches it:

```java
private static ConfigurationFacade instance() {
    class Holder {
        static final ConfigurationFacade INSTANCE = Anchor.compute(ConfigurationFacade.class, () -> {
            Optional<ConfigurationFacade> cf = ServiceLoader.load(ConfigurationFacade.class).findFirst();
            return cf.orElseGet(DefaultConfigurationFacade::new);
        });
    }
    return Holder.INSTANCE;
}
```

Resolution order:

1. `ServiceLoader.load(ConfigurationFacade.class).findFirst()` — a user-provided SPI
   implementation wins if present.
2. Otherwise fall back to `DefaultConfigurationFacade`.

`Anchor.compute` is a small global registry (`Map<Class<?>, Object>` guarded by a
`ReentrantLock`); combined with the `Holder` idiom this gives a thread-safe lazy
singleton. The implementation is resolved exactly once per JVM.

Public API:

| method | behavior |
|---|---|
| `conf(String key)` | raw string value, `null` if absent |
| `conf(String key, String defaultValue)` | `defaultValue` when value is `null` or blank |
| `confList(String key)` | raw list value, `null` if absent |
| `confAsBoolean(key, default)` | `true`/`false` (case-insensitive), else `default` |
| `confAsInt/Long/Float/Double(key, default)` | parsed value, `default` when value is `null` |
| `valueAsBoolean/Int/Long/Float/Double(value, default)` | standalone converters, usable on any string |

Note: `confAsInt` and friends throw `NumberFormatException` on a non-null but
unparseable value; only `null` falls back to the default. `valueAsBoolean` is lenient
and returns the default for anything that is not exactly `true`/`false`.

### 2.3 Pluggability

To override the configuration source, a user module:

1. Implements `ConfigurationFacade`.
2. Declares `provides io.jingproject.common.ConfigurationFacade with <impl>;` in its
   `module-info.java` (or registers the implementation as a service in a classpath
   setup).

The user implementation then completely replaces the default file-based behavior.

## 3. Data model

### 3.1 `Cfg` sealed hierarchy

```java
public sealed interface Cfg permits CfgItem, CfgList, CfgObject {
    String type();
}
```

| type | record | `type()` | value |
|---|---|---|---|
| scalar | `CfgItem(String value)` | `"Item"` | a single string |
| array | `CfgList(List<String> value)` | `"List"` | an array of strings |
| object | `CfgObject(Map<String, Cfg> value)` | `"Object"` | a nested map |

- The whole configuration file is parsed into one `CfgObject` (the root).
- `CfgObject.asImmutable()` deep-copies the whole subtree (`Map.copyOf` + recursive
  copy); `CfgItem.asImmutable()` returns `this` (strings are immutable);
  `CfgList.asImmutable()` wraps with `List.copyOf`. The immutable copies are available
  for defensive sharing.
- `CfgException extends RuntimeException` is the single error type for malformed
  configuration, duplicate keys, invalid keys, and missing files.

### 3.2 Key rules

Keys are restricted to `[a-zA-Z0-9_-]` (checked byte-wise by `CfgUtil.rejectKey`).
Nested keys use `.` as the separator, e.g. `server.port`. Nesting depth is capped at
`MAX_DEPTH = 128` segments. Keys are ASCII.

## 4. Default implementation

### 4.1 Loading

`DefaultConfigurationFacade` loads the configuration from a classpath resource:

- File name: system property `jing.config.file`, default `jing-config`.
- Extension: system property `jing.config.ext`, default empty.
  - If empty, all supported extensions are tried in order: `toml`, `json`, `properties`.
  - If set, only that extension is tried.
- The resource is resolved with the thread context class loader
  (`Thread.currentThread().getContextClassLoader().getResourceAsStream(fullName)`).
- The first resource found is parsed and the result is cached in a `Holder` lazy
  singleton; parsing happens exactly once.
- If no resource is found, a `CfgException` is thrown:
  `configuration file not found, filename : <name> , exts: <exts>`.

### 4.2 Lookup

`conf`/`confList` split the requested key on `.` and walk the tree:

- A missing intermediate or final segment yields `null` (no exception).
- A type mismatch (e.g. asking `conf` for a `CfgList`, or `confList` for a `CfgItem`)
  throws `CfgException` describing the offending key and the actual type.

## 5. File format subsets

The parsers do not implement full TOML/JSON/Properties. They implement a deliberate
subset that only supports the string model above. This keeps the parsers small and
avoids surprising value conversions.

### 5.1 TOML (`TomlCfgReader`)

Supported:

- table headers `[a.b.c]` (dotted bare keys), which switch the current table;
- bare keys only (`[a-zA-Z0-9_-]`), optionally dotted;
- string values in double quotes, single line;
- string arrays `["a", "b"]`;
- comments starting with `#`;
- escapes in strings: `\b \t \n \f \r \e \" \\ \xXX \uXXXX \UXXXXXXXX`;
- duplicate table and duplicate key detection (both rejected).

Not supported (rejected): numbers, booleans, dates, quoted keys, multi-line strings,
literal strings `'...'`, arrays of tables `[[...]]`, inline tables.

### 5.2 JSON (`JsonCfgReader`)

Supported:

- a single top-level object `{ ... }`;
- string keys and string values;
- string arrays `["a", "b"]`;
- nested objects;
- escapes in strings: `\" \\ \/ \b \f \n \r \t \uXXXX`, including surrogate pairs
  (`\uD800\uDC00` combined into a single code point);
- duplicate key detection (rejected).

Not supported (rejected): numbers, booleans, `null`, arrays at the top level, arrays of
objects.

### 5.3 Properties (`PropertiesCfgReader`)

Supported:

- standard `java.util.Properties.load` semantics (UTF-8 reader), including its escape
  handling and comment lines;
- flat keys with `.` separators are expanded into a nested `CfgObject` tree;
- a value wrapped in `[` ... `]` is split on `,` into a `CfgList` (blank items are
  dropped);
- duplicate key detection (rejected).

## 6. Known issues (recorded, not yet fixed)

These observations were found while reviewing the code against this document. They are
recorded here for later triage; the document above describes the intended design.

1. `DefaultConfigurationFacade.createConfiguration()` validates
   `SUPPORTED_FILE_EXT.contains(fileExt)` (the system property value) instead of the
   loop variable `ext`. With the default empty `jing.config.ext`, the first iteration
   throws `unsupported configuration file extension: toml`, so the default loading path
   is currently broken.
2. `conf()`/`confList()` traverse nested keys with
   `nestedkeys.subList(1, size - 1)`, which skips the first segment. A key like
   `server.port` resolves against the root instead of the `server` table. The
   `PropertiesCfgReader` uses the correct `subList(0, size - 1)` form.
3. `DefaultConfigurationFacade.MAX_DEPTH = 128` is declared but unused; the depth check
   lives in `CfgUtil`.