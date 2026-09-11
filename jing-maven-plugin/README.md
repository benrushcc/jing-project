# jing-maven-plugin

`jing-maven-plugin` is a Maven plugin that automates Java SPI (Service Provider Interface) registration. It is the build-time counterpart to `jing-common-processor`'s `ProviderProcessor`: it reads the `jing-providers.json` descriptor generated during annotation processing and produces the corresponding SPI files and `module-info` directives.

## How It Works

During compilation, `ProviderProcessor` (from `jing-common-processor`) collects all `@Provider`-annotated classes and writes a `jing-providers.json` file to the build output directory. At the build phase, this plugin consumes that JSON file and:

1. **Generates SPI descriptor files** -- writes `META-INF/services/<interface-fqn>` files listing the implementation classes, enabling `ServiceLoader.load()` at runtime.
2. **Updates `module-info.class`** -- parses the bytecode of `module-info.class` using the JDK classfile API and injects `provides ... with ...` directives, ensuring JPMS compatibility.
3. **Consumes the JSON** -- renames `jing-providers.json` to `jing-providers-consumed.json` to prevent duplicate processing.

> **Note:** Always use `mvn clean` before rebuilding. If the JSON file has already been consumed, subsequent builds without `clean` will fail because the plugin cannot find the descriptor.

## Plugin Goal

| Goal | Phase | Description |
|------|-------|-------------|
| `jing:process-jing-providers` | `process-classes` | Reads `jing-providers.json`, generates SPI files, updates `module-info.class` |

## Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `scope` | `String` | `main` | Which output directory to process: `main` for `target/classes`, `test` for `target/test-classes` |

## Configuration

### Main scope

For modules whose `@Provider` registrations are in production code:

```xml
<plugin>
    <groupId>io.jingproject</groupId>
    <artifactId>jing-maven-plugin</artifactId>
    <version>${jingproject.version}</version>
    <executions>
        <execution>
            <id>process-main-providers</id>
            <phase>process-classes</phase>
            <goals>
                <goal>process-jing-providers</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### Test scope

For modules whose `@Provider` registrations are only in test code:

```xml
<plugin>
    <groupId>io.jingproject</groupId>
    <artifactId>jing-maven-plugin</artifactId>
    <version>${jingproject.version}</version>
    <executions>
        <execution>
            <id>process-test-providers</id>
            <phase>process-test-classes</phase>
            <goals>
                <goal>process-jing-providers</goal>
            </goals>
            <configuration>
                <scope>test</scope>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## Prerequisites

- `jing-common-processor` must be on the annotation processor classpath so that `ProviderProcessor` runs during compilation and produces `jing-providers.json`.
- The `module-info.java` (if present) must not already contain `provides ... with ...` directives for the same interface -- the plugin will throw an error if duplicates are detected.

## How It Fits in the SPI Pipeline

```
Compile: ProviderProcessor writes jing-providers.json
    |
Build:   jing-maven-plugin reads jing-providers.json
    |
    +---> META-INF/services/<interface> files
    +---> module-info.class updated with provides ... with ...
    +---> jing-providers.json renamed to jing-providers-consumed.json
    |
Runtime: ServiceLoader.load(Interface.class) discovers Implementation.class
```
