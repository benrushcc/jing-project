# jing-marshall

A compile-time serialization metadata framework for Java.

## Motivation

Serialization in Java has long been a source of complexity and risk. Dynamically constructing objects and accessing fields at runtime inherently relies on reflection and privileged mechanisms like `sun.misc.Unsafe`, which break encapsulation, create security vulnerabilities, and make debugging difficult. Java's built-in serialization, in particular, suffers from these issues and has led to well-known deserialization attacks — objects are reconstructed through hidden backdoors that bypass constructors, ignore access controls, and accept untrusted data without validation.

The Java team has recognized these problems. Under Project Amber, a new approach to serialization is being designed (often referred to as Serialization 2.0). Unlike the original serialization which was tightly coupled to the `Serializable` interface and a specific byte stream format, Serialization 2.0 aims to establish a **unified object schema model** — a standard way for any class to describe its serial form, using language-level primitives (constructors, factories, and deconstruction patterns) rather than reflection or magic methods. The key goals include: bringing serialization into the object model as a first-class concept; making state extraction and reconstruction explicit, author-controlled, and safe; decoupling the serial form from both the in-memory representation and the wire encoding; and allowing any serialization framework to reuse the same metadata. Crucially, it is no longer about "implementing an interface" — it is about providing a standard schema that any serialization framework can consume.

However, Serialization 2.0 is a JDK-level effort focused on establishing this unified model for the entire Java ecosystem. It does not directly provide a ready-to-use serialization library for application developers, nor does it define specific wire formats. Third-party libraries still need to build on top of this model to provide concrete serialization formats, schemas, and interoperability.

jing-marshall takes a similar philosophy but applies it specifically to the problem of **serialization metadata generation**. Instead of relying on reflection or magic methods, jing-marshall uses compile-time annotation processing to generate dedicated metadata and accessor classes (`MarshallFacade`) for each `@Marshallable` type. This means:

- **No reflection** at runtime — for regular classes, field access goes through generated `VarHandle` calls, eliminating the need for getter/setter methods entirely; for records, the generated code directly calls the record's accessor methods.
- **No `Unsafe`** — every object is constructed through its normal constructor.
- **No hidden magic** — the generated code is plain Java source that you can read, debug, and step through.
- **Minimal stack frames** — the generated accessors are direct, inlined operations with no framework overhead.
- **Full control** — the class author decides exactly which fields are exposed and how they are named.

## Design Philosophy

jing-marshall is fundamentally different from most serialization frameworks. It does not define a wire format. It does not serialize or deserialize anything by itself. Instead, it provides **metadata** — type information, field descriptors, field accessors, and constructors — that other libraries can use to implement their own serialization formats.

This separation of concerns means:

- A single `@Marshallable` class can be serialized to JSON, binary, XML, or any other format, as long as a format-specific library is provided.
- The core library remains lightweight and format-agnostic.
- Format-specific implementations can evolve independently without changing the core metadata.

Currently, the jing project ships one such format implementation: **jing-marshall-json**, a high-performance JSON serializer/deserializer built on top of jing-marshall's metadata. For usage, refer to its README.

## Module Structure

- **jing-marshall**: Core annotations, runtime metadata APIs (`MarshallFacade`, `MarshallInfo`, `MarshallBuilder`), and type transformer support.
- **jing-marshall-processor**: Annotation processor that generates `MarshallFacade` implementations at compile time.
- **jing-marshall-json**: JSON serialization/deserialization format implementation built on jing-marshall.

## Quick Start

### Define a Marshallable Type

```java
@Marshallable
public class User {
    private int id;
    private String name;
    private long score;

    public int id() { return id; }
    public void setId(int id) { this.id = id; }
    public String name() { return name; }
    public void setName(String name) { this.name = name; }
    public long score() { return score; }
    public void setScore(long score) { this.score = score; }
}
```

Records are also supported:

```java
@Marshallable
public record User(int id, String name, long score) {}
```

### Generated MarshallFacade

The annotation processor generates a `MarshallFacade` implementation for each `@Marshallable` type. For the `User` class above, it would generate something equivalent to:

```java
@Provider(target = MarshallFacade.class)
public final class UserMarshallFacade implements MarshallFacade {
    private static final List<MarshallInfo> MARSHALL_INFOS;
    private static final MarshallHashInfo HASH_INFO;
    private static final List<VarHandle> VHS;

    static {
        MarshallInfo mi0 = new MarshallInfo(int.class, null, null, 0, "id", "id", false, false);
        MarshallInfo mi1 = new MarshallInfo(String.class, null, null, 1, "name", "name", false, false);
        MarshallInfo mi2 = new MarshallInfo(long.class, null, null, 2, "score", "score", false, false);
        MARSHALL_INFOS = List.of(mi0, mi1, mi2);
        HASH_INFO = new MarshallHashInfo(MARSHALL_INFOS);
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandles.Lookup lookup0 = MethodHandles.privateLookupIn(User.class, lookup);
            VarHandle vh0 = lookup0.findVarHandle(User.class, "id", int.class);
            VarHandle vh1 = lookup0.findVarHandle(User.class, "name", String.class);
            VarHandle vh2 = lookup0.findVarHandle(User.class, "score", long.class);
            VHS = List.of(vh0, vh1, vh2);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    // ... field accessors, builder, constructor ...
}
```

This is real, debuggable Java code — no reflection, no magic.

For regular classes, the generated code uses `VarHandle` to read and write fields directly, so the class does not need to provide getter/setter methods. For records, the generated code calls the record's accessor methods directly (e.g., `entity.id()`), which are already part of the record's public API.

## Metadata

### MarshallInfo

Each field in a `@Marshallable` type is described by a `MarshallInfo` record:

```java
public record MarshallInfo(
    Class<?> rawType,           // the Java type of the field
    Class<?> firstGenericType,  // first generic type argument (if any)
    Class<?> secondGenericType, // second generic type argument (if any)
    int index,                  // sequential field index (0-based)
    String fieldName,           // original Java field name
    byte[] fieldNameUtf8Bytes,  // UTF-8 encoding of fieldName
    String mappedName,          // name used in serialized form
    byte[] mappedNameUtf8Bytes, // UTF-8 encoding of mappedName
    byte type,                  // encoded type descriptor
    byte flags                  // bitmask: skip serializing, skip deserializing, name simplicity
)
```

- `index`: The ordinal position of this field. Used for indexed access via `MarshallFacade`.
- `fieldName`: The original name in the Java source.
- `fieldNameUtf8Bytes`: Pre-computed UTF-8 encoding of `fieldName`, used for binary lookup without string allocation.
- `mappedName`: The name used during serialization. This solves the common case where the wire format requires a different field name than the Java class field name. For example, a Java field `userName` might need to map to `"user_name"` in JSON, or `"USERNAME"` in another format. The `mappedName` can be configured via `@MarshallAttr.mappedName` or automatically derived from `NamingConvention` conversion.
- `mappedNameUtf8Bytes`: Pre-computed UTF-8 encoding of `mappedName`.
- `type`: An encoded type descriptor optimized for fast dispatch and lookup table access. It specifically targets common JDK standard library types with dedicated codes (e.g., `int`, `String`, `LocalDateTime`, `List`, `Map`, enums, etc.), so that format-specific implementations can use `switch` statements or lookup tables instead of `Class.equals()` comparisons. This is a deliberate design choice — rather than encoding an arbitrary type hierarchy, it focuses on the types that serialization frameworks actually need to handle differently.
- `flags`: Bitmask indicating whether the field should be skipped during serialization/deserialization, and whether the field/mapped names contain only simple ASCII characters (used to optimize hash computation).

### Generic Type Handling

One of the key differences between jing-marshall and reflection-based serialization frameworks is how generic types are handled.

Traditional frameworks that rely on runtime reflection suffer from **generic erasure** — at runtime, `List<A>` and `List<B>` are both just `List.class`. The framework cannot know what type `A` or `B` is without explicit type information (e.g., `TypeReference`, `ParameterizedType`). This leads to two problems:

- **Serialization**: Each element must be inspected at runtime to determine its actual type before dispatch.
- **Deserialization**: The framework must use mechanisms like `TypeReference` to capture and pass generic type information, which is both error-prone and verbose.

jing-marshall takes a fundamentally different approach. Since the annotation processor runs at compile time, it has full access to generic type information. When it encounters a field like `List<A>`, it knows exactly what `A` is — no erasure has occurred yet. The generated `MarshallInfo` captures this information in `firstGenericType` and `secondGenericType`. The dispatch during serialization and deserialization is based entirely on compile-time type information, eliminating the need for runtime type inspection.

To keep the type system manageable, jing-marshall captures at most **2 generic type parameters at the first level**. This covers the most common cases:

- `List<String>` — 1 generic parameter
- `Map<String, Integer>` — 2 generic parameters
- `Collection<SomeEntity>` — 1 generic parameter

Nested generic types beyond this level (e.g., `Map<List<X>, Y>` or `List<Map<String, List<Integer>>>`) are **not supported** and will produce a compile-time error. If your data model requires deeply nested generic structures, you should refactor the code to use non-generic bean types as nested wrappers instead.

### Generic Beans Are Not Supported

Due to generic erasure, a generic class like `Result<T>` cannot be used with jing-marshall. When the processor encounters `Result<User>`, the field type of `T` is erased to `Object` at runtime, and the compile-time type information is lost in the generated facade. This means the framework cannot determine what `T` actually is when processing fields of the generic bean.

If you need to wrap results in a generic container, define a concrete, non-generic type instead:

```java
// Not supported
public class Result<T> {
    private int code;
    private T data;
}

// Supported — use a concrete type
public class UserResult {
    private int code;
    private User data;
}
```

### MarshallFacade

The central API for working with marshallable types:

```java
public interface MarshallFacade {
    // the type this facade describes
    Class<?> marshallableType();

    // all field metadata
    List<MarshallInfo> marshallInfos();

    // look up field metadata by original Java name
    MarshallInfo marshallInfoByFieldName(String fieldName);

    // look up field metadata by serialized name
    MarshallInfo marshallInfoByMappedName(String mappedName);

    // read a field value by index (typed accessors)
    boolean readBoolean(Object instance, int index);
    byte readByte(Object instance, int index);
    short readShort(Object instance, int index);
    char readChar(Object instance, int index);
    int readInt(Object instance, int index);
    long readLong(Object instance, int index);
    float readFloat(Object instance, int index);
    double readDouble(Object instance, int index);
    Object readObject(Object instance, int index);

    // create a builder for constructing a new instance
    MarshallBuilder newBuilder();

    // construct a new instance from a builder
    Object construct(MarshallBuilder builder);
}
```

### Field Lookup

Field lookup supports both `String` and binary (`byte[]` / `MemorySegment`) inputs. Binary lookup uses a hash-based strategy for O(1) average-case performance:

```java
MarshallFacade facade = Marshalls.beanMarshallFacade(User.class);

// lookup by Java field name
MarshallInfo info = facade.marshallInfoByFieldName("id");

// lookup by serialized name
MarshallInfo info2 = facade.marshallInfoByMappedName("id");

// lookup from raw UTF-8 bytes (e.g., during JSON parsing)
byte[] utf8 = "id".getBytes(StandardCharsets.UTF_8);
MarshallInfo info3 = facade.marshallInfoByFieldName(utf8);
```

### Field Access

Fields are accessed by their index (the `index` field in `MarshallInfo`):

```java
MarshallFacade facade = Marshalls.beanMarshallFacade(User.class);
User user = ...;

// read by index
int id = facade.readInt(user, 0);
String name = (String) facade.readObject(user, 1);
long score = facade.readLong(user, 2);
```

### Object Construction

Objects are constructed through `MarshallBuilder`, which writes field values by index, then passes the builder to `construct()`:

```java
MarshallFacade facade = Marshalls.beanMarshallFacade(User.class);
MarshallBuilder builder = facade.newBuilder();
builder.writeInt(0, 42);
builder.writeObject(1, "alice");
builder.writeLong(2, 100L);
User user = (User) facade.construct(builder);
```

For classes, the builder wraps a pre-constructed instance (via no-arg constructor) and sets fields through `VarHandle`. For records, the builder accumulates values and calls the canonical constructor in `construct()`.

## Annotations

### `@Marshallable`

Marks a class, record, or enum as marshallable. Triggers annotation processing to generate a `MarshallFacade` implementation.

```java
@Marshallable(from = NamingConvention.CAMEL_CASE, to = NamingConvention.SNAKE_CASE)
```

- `from`: Source naming convention of Java field names (default: `ORIGINAL`).
- `to`: Target naming convention for the serialized form (default: `ORIGINAL`).
- If both are `ORIGINAL`, no name conversion is performed.

Supported naming conventions: `ORIGINAL`, `CAMEL_CASE`, `SNAKE_CASE`, `KEBAB_CASE`, `PASCAL_CASE`, `UPPER_SNAKE_CASE`, `UPPER_KEBAB_CASE`.

### `@MarshallAttr`

Customizes individual field behavior within a `@Marshallable` type.

```java
@MarshallAttr(mappedName = "user_name", skipDeserializing = true)
private String name;
```

- `mappedName`: Override the serialized field name.
- `skipSerializing`: Skip this field during serialization.
- `skipDeserializing`: Skip this field during deserialization.

## Type Transformer

`MarshallTransformer<CustomType, BuiltinType>` is a bidirectional converter between a custom type the user wants to support and a type natively supported by the serialization format. It is loaded and used uniformly in both serialization and deserialization.

- `BuiltinType` — the type natively supported by the specific wire format (e.g., for JSON, this must be a `JsonPrimitiveType` subtype; for a binary format, it might be a raw byte representation).
- `CustomType` — the user's desired type that needs to be converted to/from the builtin type.

To create a transformer, implement `MarshallTransformer` and annotate the class with `@Transformable`:

```java
@Transformable
public final class TimeTransformer implements MarshallTransformer<LocalDateTime, String> {
    private static final DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String toBuiltin(LocalDateTime ct) {
        return formatter.format(ct);
    }

    @Override
    public LocalDateTime toCustom(String bt) {
        return formatter.parse(bt, LocalDateTime::from);
    }
}
```

The `MarshallTransformerProcessor` annotation processor automatically detects `@Transformable`-annotated classes and generates a corresponding `MarshallTransformerFacade` implementation (e.g., `TimeTransformerFacade`). The generated facade is registered via SPI and can be loaded at runtime.

Requirements for `@Transformable` classes:
- Must be a public, non-abstract, top-level class or record.
- Must have a public no-arg constructor.
- Must implement `MarshallTransformer<CustomType, BuiltinType>`.
- `CustomType` and `BuiltinType` must be different types.

## Inheritance

Class-based marshallable types support inheritance. The parent class must also be annotated with `@Marshallable` and reside in the same module. Fields are collected from the root class down to the leaf class.

```java
@Marshallable
public class BaseEntity {
    private int id;
    private String name;
}

@Marshallable
public final class ExtendedEntity extends BaseEntity {
    private long score;
}
```

The generated facade for `ExtendedEntity` will handle all fields from both classes, with field indices assigned in declaration order from root to leaf.

## Enums

Enums annotated with `@Marshallable` are handled specially. Each enum constant is treated as a "field", and lookup is done by name via a generated `switch` expression.

```java
@Marshallable
public enum Status {
    ACTIVE,
    INACTIVE,
    PENDING
}
```

For enums, the enum constants themselves **are** the fields — there is no separate field declaration to annotate. Therefore, `@MarshallAttr.skipSerializing` and `@MarshallAttr.skipDeserializing` are **not supported on individual enum constants**. If you need to skip serialization or deserialization of the entire enum, that decision is made at the field level where the enum type is used, not on the constants themselves.

## Constraints

- Abstract classes cannot be annotated with `@Marshallable`.
- Non-abstract classes must have a public no-arg constructor.
- Fields in regular classes must be non-final.
- Generic fields are limited to at most 2 type parameters at the first level (e.g., `List<String>`, `Map<String, Integer>`). Nested generic types (e.g., `Map<List<X>, Y>`) are not supported.
- Generic bean types (`Result<T>`, `Response<V>`, etc.) are not supported due to generic erasure. Use concrete, non-generic types instead.
- Parent classes in an inheritance chain must be annotated with `@Marshallable`.
- Parent classes must reside in the same module as the child class. This is because the generated `MarshallFacade` uses `MethodHandles.privateLookupIn` to obtain `VarHandle` for each field, and the Java module system restricts `privateLookupIn` to the same module — cross-module private lookup is not permitted.
- Enum constants do not support `skipSerializing` / `skipDeserializing`.
