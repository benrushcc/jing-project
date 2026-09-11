# jing-common-processor

`jing-common-processor` is the foundational annotation processing module for the jing project. It provides two core capabilities:

1. **SPI registration** -- `ProviderProcessor` scans `@Provider`-annotated classes, collects interface-to-implementation mappings, and writes a `jing-providers.json` descriptor. Combined with `jing-maven-plugin`, this achieves automatic SPI registration without manual `META-INF/services` file management.

2. **Code generation utilities** -- A set of builder classes (`GeneratorSource`, `GeneratorBlock`, `GeneratorLine`) and validation helpers (`AnnoUtil`) for programmatically constructing Java source files during annotation processing.

## Design Philosophy

### Source Code Generation

Source code generation is inherently fragile. Implementing a complete source code generation system is extremely difficult -- it would require parsing and producing every syntax structure that Java supports (generics, annotations, inner classes, enums, records, sealed hierarchies, etc.). This complexity is unnecessary for the vast majority of annotation processors, since generated classes typically have simple structures: a class declaration, a few field assignments, method overrides, and static factory methods.

`jing-common-processor` therefore takes a deliberately minimal approach: source code is built via string concatenation using `GeneratorSource` and `GeneratorBlock`. This avoids the overhead of a full AST model while still providing reusable utilities for the non-trivial parts -- import management, name conflict resolution, indentation control, and writing to the annotation processing filer.

### SPI Registration with `@Provider`

Generated code must be discoverable by user code at runtime. The standard Java mechanism for this is the ServiceLoader SPI, which requires:

1. A descriptor file under `META-INF/services/<interface-fqn>` listing implementation classes.
2. A `provides ... with ...` directive in `module-info.java` for JPMS compatibility.

Manually maintaining these files is error-prone and tedious, especially when multiple modules contribute providers. The `@Provider` annotation solves this: a developer annotates a generated class with `@Provider(target = MyInterface.class)`, and the `ProviderProcessor` (running in the next annotation processing round) collects these mappings into `jing-providers.json`.

However, annotation processing alone cannot complete the pipeline. APT runs at compile time and can only produce source files and resources -- it cannot modify the compiled `module-info.class` bytecode. Since `module-info` is fixed once compilation finishes, the `provides ... with ...` directive for generated classes must be injected after compilation.

This is why `jing-maven-plugin` is essential. It runs at the build phase, reads `jing-providers.json`, and:

- Writes `META-INF/services/` files for traditional (classpath) SPI support.
- Parses `module-info.class` at the bytecode level using the JDK classfile API and injects the necessary `provides ... with ...` entries.

The combination of `@Provider` (compile-time annotation collection) and `jing-maven-plugin` (build-time bytecode modification) achieves fully automatic SPI registration with zero manual configuration.

## Dependency Rules

- Depends only on `jing-common` (no third-party libraries).
- All public API classes are annotated with `@ProcessorApi`, indicating they are referenced as strings in annotation processors (e.g., fully-qualified class names in `getSupportedAnnotationTypes()`, method names in reflective lookups). Renaming or refactoring these classes, their methods, or method parameters requires corresponding changes in the APT code that references them.
- Built with `<proc>none</proc>` to disable annotation processing during its own compilation.

## Module Declaration

```java
module jing.commonprocessor {
    requires transitive jing.common;
    requires transitive java.compiler;

    exports io.jingproject.commonprocess;
    provides javax.annotation.processing.Processor with io.jingproject.commonprocess.ProviderProcessor;
}
```

## SPI Registration Pipeline

The full data flow from annotation to runtime SPI:

```
@Provider(target = MyInterface.class)
final class MyInterfaceImpl implements MyInterface { ... }
        |
        v
ProviderProcessor (compile-time, in jing-common-processor)
        |
        | writes
        v
jing-providers.json  (in target/classes/)
        |
        | consumed by
        v
jing-maven-plugin (process-jing-providers goal)
        |
        | produces:
        | 1. META-INF/services/<interface> files
        | 2. Updated module-info.class (provides ... with ...)
        v
Runtime: ServiceLoader.load(MyInterface.class) discovers MyInterfaceImpl
```

### Step 1: APT generates a class annotated with `@Provider`

An upstream annotation processor (e.g., `jing-marshall-processor`) generates a class and marks it with `@Provider`:

```java
import io.jingproject.common.anno.Provider;

@Provider(target = MyInterface.class)
public final class MyInterfaceImpl implements MyInterface {
    // ... generated code ...
}
```

Constraints on the generated class:
- Must be `final`.
- Must be a top-level `public` class (no inner classes, no anonymous classes).
- `@Provider.target()` must point to an interface, not a class.

### Step 2: `ProviderProcessor` generates the JSON descriptor

During compilation, `ProviderProcessor` (from this module) runs in a subsequent annotation processing round, discovers all `@Provider`-annotated classes, and writes a `jing-providers.json` descriptor to `target/classes/`:

```json
{
  "com.example.MyInterface": [
    "com.example.MyInterfaceImpl"
  ]
}
```

### Step 3: `jing-maven-plugin` consumes the JSON

At the build phase, `jing-maven-plugin` reads `jing-providers.json` and produces SPI descriptor files and `module-info` directives. See [jing-maven-plugin/README.md](../jing-maven-plugin/README.md) for configuration details.

> **Note:** Always use `mvn clean` before rebuilding. If `jing-providers.json` is already consumed, subsequent `mvn test` without `clean` will fail because the plugin cannot find the descriptor file.

## Code Generation Utilities

### GeneratorSource

The main entry point for generating a Java source file. It manages:

- **Package and class name derivation** -- automatically derives the output class name from the source `TypeElement` and a `tag` string via `Utils.generateClassName(simpleName, tag)`.
- **Import management** -- tracks all referenced types, resolves simple names, detects conflicts, and uses fully-qualified names when necessary. Automatically skips `java.lang` imports.
- **Indentation** -- tracks and applies consistent indentation across blocks.
- **Writing** -- outputs the complete source file (package declaration, sorted imports, indented code) to the annotation processing filer.

```java
GeneratorSource source = new GeneratorSource(targetTypeElement, "Provider");
source.register(MyClass.class);       // import MyClass
source.register(otherSource);         // import from another GeneratorSource
String resolved = source.registerFieldElement(fieldElement);  // import and resolve field type

GeneratorBlock block = new GeneratorBlock();
block.addLine("public class " + source.className() + " implements " + resolved + " {");
block.indent();
block.addLine("// ...");
block.unindent();
block.addLine("}");

source.addBlock(block);
source.writeToFiler(processingEnv);
```

#### Type Registration

`GeneratorSource` provides multiple `register(...)` overloads to handle different input types:

| Method | Input | Description |
|--------|-------|-------------|
| `register(Class<?>)` | Runtime class | Registers a class, returns simple name |
| `register(TypeElement)` | Compile-time type | Registers a type element, returns simple name |
| `register(GeneratorSource)` | Another source | Registers the generated class from another source |
| `registerFieldElement(Element)` | Field or record component | Registers all type components (including generic args) and returns the resolved type string |
| `registerRawFieldElement(Element)` | Field or record component | Registers only the raw (non-generic) type |

Name conflict resolution:
- If a simple name is not yet imported, it is imported and the simple name is returned.
- If the same simple name is already imported from the same package, the simple name is returned directly.
- If the same simple name is already imported from a different package, the fully-qualified name is returned.

### GeneratorBlock

A builder for a block of generated source code lines. Supports fluent chaining:

```java
GeneratorBlock block = new GeneratorBlock();
block.addLine("public void doSomething() {")
     .indent()
     .addLine("int x = 42;")
     .addLine("System.out.println(x);")
     .unindent()
     .addLine("}")
     .newLine();
```

Methods:
- `addLine(content)` -- appends a line at the current indent level.
- `prependLine(content)` -- inserts a line at the front of the block (must be non-empty).
- `newLine()` -- adds a blank line.
- `indent()` / `unindent()` -- increment / decrement the indent level.
- `lines()` -- returns the list of `GeneratorLine` records.
- `isEmpty()` -- checks whether the block has no lines.

### GeneratorLine

A record representing a single line of generated source code:

```java
public record GeneratorLine(String content, int indent) {}
```

Each indent level equals 4 spaces.

### AnnoUtil

Utility class for annotation processor development. Provides:

**String escaping:**
- `javaStringLiteral(str)` -- wraps a string in double quotes.
- `escapeJavaStringLiteral(str, builder)` -- escapes special characters (`"`, `\`, `\b`, `\f`, `\n`, `\r`, `\t`) and control characters below `0x20` using `\uXXXX` notation.

**Name manipulation:**
- `packageName(fullName)` / `simpleName(fullName)` -- split fully-qualified class names.
- `buildClassName(packageName, className)` / `buildClassName(moduleName, packageName, className)` -- construct qualified names.

**Safe casting** (using pattern-matching `instanceof`):
- `castTypeElement(Element)`, `castVariableElement(Element)`, `castRecordComponentElement(Element)`, `castExecutableElement(Element)`
- `castDeclaredType(TypeMirror)`, `castArrayType(TypeMirror)`

**Validation for type registration:**
- `checkClassForRegister(Class<?>)` -- rejects null, primitive, anonymous, member, and hidden classes.
- `checkTypeElementForRegister(TypeElement)` -- rejects null, primitive, non-top-level, non-public, and generic types.
- `checkFieldElementForRegister(Element)` -- validates fields and record components, checks enclosing type, validates array and generic type arguments, rejects `super`/`extends` wildcards.
- `validateTypeArgs(List<TypeMirror>)` -- ensures generic type arguments are non-generic declared types.
- `validateArray(ArrayType)` -- ensures array components are valid, rejects multi-dimensional arrays.

## For Downstream APT Modules

When building a new annotation processor module that needs to generate source code or register providers, depend on `jing-common-processor`:

```xml
<dependency>
    <groupId>io.jingproject</groupId>
    <artifactId>jing-common-processor</artifactId>
    <version>${jingproject.version}</version>
</dependency>
```

Then declare the processor in your `module-info.java`:

```java
requires transitive jing.commonprocessor;
```

And register your processor via SPI:

```java
provides javax.annotation.processing.Processor with io.jingproject.yourmodule.YourProcessor;
```

Use `GeneratorSource`, `GeneratorBlock`, and `AnnoUtil` from your processor to generate source code, and use `ProviderProcessor`'s `@Provider` scanning to register implementations.
