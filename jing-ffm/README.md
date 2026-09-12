# jing-ffm

A runtime for FFM (Foreign Function & Memory) native bindings.

It works together with `jing-ffm-processor`.
Bindings are declared as Java interfaces with annotations.
The processor generates all the glue code at compile time.
The result is a safer, simpler way to downcall into native code.

## Motivation

Calling native code from the JVM used to be painful.

Before Project Panama, the standard way was JNI.
You wrote the binding yourself: C glue code, headers, symbol wiring.
It was verbose, repetitive, and easy to get wrong.

Project Panama brought a unified wrapper for FFI.
You can find a function pointer from a dynamic library.
Then you create a stub at runtime and call through it.
The VM can optimize these calls well.

The JDK also ships `jextract`.
It generates Java bindings directly from C header files.
But it generates glue code uniformly for every function.
It gives you no filtering or customization.
For multiple headers, or custom needs, it is not easy to use.

So we built this library.
It turns native binding into plain interface declarations.
`jing-ffm` provides the annotations and the runtime.
`jing-ffm-processor` generates the implementation code.
The result is a lightweight, purpose-driven alternative to `jextract`.

## Annotations

Bindings live on a Java interface.
The interface is annotated with `@FFM`.
Each method is annotated with `@Downcall`.

```java
@FFM(libraryName = "my")
public interface MyLib {
    @Downcall(methodName = "my_add", critical = true)
    int myAdd(int a, int b);
}
```

### `@FFM`

Type-level annotation. Marks an interface as a FFM binding.

```java
@FFM(libraryName = "my", supportedOS = {Os.WINDOWS, Os.LINUX})
public interface MyLib { ... }
```

- `libraryName()`: the shared library name.
  OS prefixes (`lib`) and extensions (`.so`, `.dll`, `.dylib`)
  are resolved at best effort via `System.mapLibraryName`.
  The default `jvm` selects the JVM's internal lookup.
  You should never name your own library `jvm`.
- `supportedOS()`: the operating systems this binding supports.
  Defaults to `{WINDOWS, LINUX, MACOS}`.
  Must stay consistent with the `Os` enum in `jing-common`.
  On an unsupported OS the binding is skipped.
  The program still starts.

### `@Downcall`

Method-level annotation. Names the native function to bind.

```java
@Downcall(methodName = "malloc", critical = true)
MemorySegment malloc(long size);

@Downcall(methodName = "MY_MACRO")
int myMacro();

@Downcall(methodName = "my_get_name")
MemorySegment myGetName(long id);
```

- `methodName()`: the exported symbol name.
  Snake-case is recommended.
- `constant()`: whether the return value is constant.
  When `true`, the function must have no arguments.
  `jing-ffm-processor` emits `MethodHandles.constant`.
  The result is cached and constant-folded.
  Used for native `#define` / `MACRO` values.
  Supported returns: the eight primitives and `MemorySegment`.
- `critical()`: whether the function returns immediately.
  Critical downcalls remove the safepoint check.
  This is dangerous for long-running functions.
  Use only for short, non-blocking calls like `malloc`.
  Critical calls can be disabled globally.
  See `jing.ffm.critical` below.

Constraints on the `@FFM` interface:
- must be public, non-sealed, top-level
- every non-default/static/private method needs `@Downcall`
- methods must not be var-args
- methods must not declare checked exceptions
- methods must not declare type parameters
- parameter and return types are primitives or `MemorySegment`

## Usage

User code has two supported entry points.
The typical one is the generated interface.
The lower-level one is `libDescriptor`.

### Through the Generated Interface

The processor generates `MyLibLibFacade`
(a `LibFacade` implementation, a `@Provider`).
`Libs` discovers it via `ServiceLoader` at startup.
`impl(MyLib.class)` returns the singleton implementation.
Every call forwards to the resolved native function.

```java
MyLib lib = Libs.impl(MyLib.class);
if (lib == null) {
    // null: library missing or unsupported on this OS
}
int sum = lib.myAdd(40, 2);
```

Store the implementation in a `static final` field.
That form can be constant-folded by the JIT,
and the calls optimize to the best possible shape.
Avoid fetching the implementation each time
into a local variable.

```java
private static final MyLib MY_LIB = Libs.impl(MyLib.class);

int sum = MY_LIB.myAdd(40, 2);
```

### Through `libDescriptor`

For lower-level work, ask `Libs` for the `LibDescriptor`.
This is how to inspect a loaded library:
the mapped library name,
the resolved file path,
the `SymbolLookup`,
the function address map,
and the implementation instance.

```java
LibDescriptor<MyLib> desc = Libs.libDescriptor(MyLib.class);
if (desc == null) {
    // null: library missing or unsupported on this OS
}
MemorySegment myAdd = desc.functions().get("my_add");
Path libPath = desc.libPath();
SymbolLookup lookup = desc.lookup();
```

### VM Functions

`addrFromVM` and `mhFromVM` target the JVM's default lookup.
Use them to reach C standard library functions,
like `malloc` and `free`.

```java
MemorySegment seg = Libs.addrFromVM("malloc");

MethodHandle mh = Libs.mhFromVM("malloc",
        List.of(MemorySegment.class, long.class), true, false);
```

## Runtime: `Libs`

### Library Search Path

Priority order (first hit wins):
1. `jing.library.path` system property
2. `JING_LIBRARY_PATH` environment variable
3. `java.library.path` (the JVM default)

Only existing directories are collected.

### `jing.ffm.critical`

The global switch `-Djing.ffm.critical=false` (default `true`).
When `false`, every `critical = true` call
is built as a normal, safepoint-checked downcall.
This guarantees a safepoint on every native transition.

## Loading Behavior

Libraries follow a fixed, process-wide lifecycle.

Using any member of `Libs`
(triggering its class loading)
loads every `@FFM`-declared library at once.
The libraries stay loaded
for the whole JVM process.
They are never unloaded.

This is the most common usage.
It covers the jing project's own needs.
If a third-party library needs a custom lifecycle,
do not try to adapt it with the ffm module.
The module is not built for that.

Like the jing project,
the ffm module only supports Windows, Linux, and macOS.
If `supportedOS` in the `@FFM` annotation
does not include the current platform,
the library is silently ignored.

Library loading is eager;
stub generation is lazy.

Loading a library takes little time.
Generating a stub is expensive,
and many functions
are never called during the process.
So each stub is bound on its first call.
This strategy balances
startup performance and runtime performance.

## Downcall Restrictions

jing-ffm targets downcalls only.
It currently serves the jing project internally.

Compared with the JDK FFM API,
it deliberately adds restrictions at the design level.
The goal is safe, simple, and maintainable bindings.

Of the `Linker.Option` variants the JDK offers,
this module exposes only `critical`.
The others are intentionally unsupported.
They are documented here so the reasons stay clear.

### `critical`

The most important downcall option.
Official docs recommend it only for very short calls,
like fetching a constant.
It lowers the downcall overhead:
fewer thread state transitions,
less stack cleanup,
no safepoint check.

Not sure whether to use it on a non-constant call?
Mirror the JVM itself. That is the safest baseline.
The JVM source has many internal JNI call mechanisms:
`JVM_ENTRY` is a normal call,
`JVM_LEAF` is a critical call.
If a third-party function resembles a standard library function,
check whether the VM wraps it with `JVM_LEAF`.
`malloc` and `free` are the typical examples.
Even though `malloc` may do syscalls internally,
the VM still classifies it as "always completes quickly"
and minimizes the transition overhead.
Use this as a simple reference.

### `allowHeap` (not supported)

Only meaningful together with `critical`.
Considered useless in practice.

Its purpose is to reduce copying between
heap and off-heap memory.
The effect is noticeable only for a few MB or more.
But passing large heap memory means handling bigger data batches.
The call takes longer,
and blocks safepoint entry for the whole process.
The two effects pull in opposite directions.
A real application scenario is hard to find.

Passed heap memory is explicitly GC-pinned.
In G1, a region's pin count decides the pin (cheap).
In ZGC (a concurrent GC),
it is an atomic counter plus a lock,
checked globally.
So the behavior depends on the chosen GC.
This adds maintenance and tuning complexity.
Not supported.

### `firstVariadicArg` (not supported)

Used to link variadic functions,
such as `printf`.
It gives the position of the variadic argument
in the bound parameter list.
Variadic calling conventions in the OS ABI
often differ from normal functions.
They need special treatment.
A good FFI library should not expose
variadic functions to other languages at all.
Not supported.

### `captureCallState` (not supported)

Captures and returns call state,
mainly to receive `errno` and similar error codes.
Such codes must be read immediately after the call,
otherwise the VM may overwrite them.

The idea is good:
call OS APIs directly,
get accurate error codes,
no wrapper library needed.
But the implementation is opaque to developers.
When a wrapper is feasible,
a manual implementation is more direct
and more maintainable.
Not supported.

## Generated Binding

For the interface above, `jing-ffm-processor` generates roughly:

```java
@Provider(target = LibFacade.class)
@Generated
public final class MyLibLibFacade implements LibFacade {
    private static final AtomicBoolean GUARD = new AtomicBoolean(false);
    private static final Impl IMPL = new Impl();

    @Override
    public Class<?> target() { return MyLib.class; }

    @Override
    public List<Os> supportedOS() {
        return List.of(Os.WINDOWS, Os.LINUX, Os.MACOS);
    }

    @Override
    public String libName() { return "my"; }

    @Override
    public List<String> methodNames() { return List.of("my_add"); }

    @Override
    public Object impl() { return IMPL; }

    private static final class Impl implements MyLib {
        private static final List<MethodHandle> MHS =
                List.ofLazy(1, Impl::makeMHS);

        private static MethodHandle makeMHS(int index) {
            return switch (index) {
                case 0 -> Libs.mhFromLib(MyLib.class, "my_add",
                        List.of(int.class, int.class, int.class),
                        true, false);
                default -> throw new AssertionError();
            };
        }

        @Override
        public int myAdd(int p1, int p2) {
            try {
                return (int) MHS.get(0).invokeExact(p1, p2);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable t) {
                throw new UndeclaredThrowableException(t);
            }
        }
    }
}
```