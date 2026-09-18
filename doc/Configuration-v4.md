# Configuration Parser Restructure: Method-Per-State Readers

Status: design / not yet implemented
Scope: `jing-common` `io.jingproject.common.conf` package only.
Follows `doc/Configuration-v3.md` (the parsing behavior, canonical forms, scanner
contract and error semantics described there remain authoritative). This
document only restructures the internal shape of `JsonCfgReader` and
`TomlCfgReader`; it changes no public API and no observable behavior.

## 1. Goals

1. **Readability**: replace the two ~150-line state-machine `parse()` bodies
   (a `for (;;)` loop with a big `switch (state)`) with a ~18-line driver loop
   plus a set of small single-responsibility methods, one per state.
2. **Modeling**: use a `record` as an intermediate value object to carry
   multi-field state where a model benefits — specifically a `Frame` record
   pairing each nested object with its pending key, replacing the two parallel
   `Deque`s that are always pushed and popped together.
3. **De-duplication**: the scalar value dispatch is duplicated between
   `EXPECT_VALUE` and `ARR_START` (~18 lines each) in both readers; unify it in
   one `readScalarValue(int firstByte)` method. The six "`putIfAbsent` duplicate
   check + clear `key` + set trailing state" tails become two shared methods
   `storeItem` / `storeList`.
4. **Behavior preserved**: byte-exact parsing, single `String` construction per
   token, no per-byte allocation, no regex, no recursion. All 71 existing tests
   keep passing unchanged.

## 2. Constraints

- Zero third-party dependencies (`jing-common` rule).
- No public API / signature change: `CfgReader.parse(int maxDepth)`, the four
  `CfgUtil` scanners, `readCfgKey` / `readCfgNestedKey` keep their v3 contracts.
- Project AGENTS.md rules: no recursion, no regex, comments `//` only, 4-space
  indent, 120 columns, no `import static`, lowercase sentence starts, `record`
  preferred for plain data.
- Byte-level single-pass parsing preserved: `PushbackInputStream` semantics
  (non-token byte `unread`, EOF not unread), `HeapWriteBuffer` reuse, `String`
  built exactly once per token (the v3 D6 sites must not grow).
- No per-token/per-byte allocation increase on the hot path; `Frame` records
  only on nested-object entry.
- Error behavior identical: same input, same failure (`CfgException` at the
  same byte). Tests assert only the exception type, not message text, but
  message strings stay byte-identical anyway.

## 3. Core Design Decisions

### D1. Method-per-state driver loop

Both readers keep an iterative state machine, but `parse` shrinks to a pure
driver:

```java
public CfgObject parse(int maxDepth) throws IOException {
    State state = State.INITIAL;
    while (state != State.DONE) {
        state = switch (state) {
            case INITIAL -> onInitial();
            // one branch per state; DONE is unreachable (exhaustive switch)
            case DONE -> throw new AssertionError();
        };
    }
    return new CfgObject(current);
}
```

- Each state handler reads the bytes it owns, does its work, and returns the
  next state. `IOException` / `CfgException` propagate naturally; the loop only
  exits via `DONE` or an exception.
- `DONE` is returned by the root-completion paths only:
  - JSON: `onAfterValue` at root `}` or root EOF;
  - TOML: `onValueEnd` / `onInitial` at EOF.
- The `DONE -> throw new AssertionError()` branch exists solely to satisfy
  enum-switch-expression exhaustiveness (matches the existing `AssertionError`
  style already used in `TomlCfgReader`).

### D2. `Frame` record replaces the parallel deques (JSON only)

Today `JsonCfgReader` keeps `Deque<Map<String, Cfg>> previous` and
`Deque<String> keys` which are always pushed and popped together — an implicit
pairing that can silently desync. Replace both with a single stack of a value
record:

```java
// in JsonCfgReader
private record Frame(Map<String, Cfg> parent, String key) {}

private final Deque<Frame> frames = new ArrayDeque<>(); // replaces previous + keys
```

- push on nested-object entry: `frames.addLast(new Frame(current, key))`,
  then `current = new HashMap<>(); key = null;`
- pop on `}`: `Frame frame = frames.pollLast();` — `null` means the root object
  closed (`DONE`); otherwise restore `current = frame.parent()` and store
  `new CfgObject(current)` under `frame.key()` with the same `putIfAbsent`
  duplicate check.
- Depth check becomes `frames.size() >= maxDepth` (identical to the old
  `previous.size() >= maxDepth`).
- TOML needs no frame: its nesting comes from `[table]` headers that navigate
  `current` directly; there is no inline nesting and no stack.

### D3. `readScalarValue` absorbs the string/number/bool dispatch

Today `EXPECT_VALUE` and `ARR_START` each implement the full string/number/bool
dispatch (>18 duplicated lines per reader). Extract one private method per
reader; the `"` branch makes the former `STR_START` state unnecessary:

```java
// JsonCfgReader — dispatch identical for values in objects and in arrays
private String readScalarValue(int firstByte) throws IOException {
    if (firstByte == '"') {
        readJsonStrValue(input, out);
        String str = new String(out.rawByteArray(), 0, out.intPosition(), StandardCharsets.UTF_8);
        out.reset();
        return str;
    }
    if (firstByte == '-' || (firstByte >= '0' && firstByte <= '9')) {
        String num = CfgUtil.readJsonNumber(input, firstByte, out);
        out.reset();
        return num;
    }
    if (firstByte == 't' || firstByte == 'f') {
        String bool = CfgUtil.readJsonBool(input, firstByte, out);
        out.reset();
        return bool;
    }
    throw new CfgException("Corrupted json configuration");
}
```

Key point: the `'"'` branch preserves today's exact semantics — the opening
quote has already been consumed by `ignoreTillEOF`, `readJsonStrValue`
consumes the closing quote internally, and the single `new String(...)` +
`out.reset()` are unchanged. The TOML version differs only in routing
(`readTomlNumber` / `readTomlWord` / `readTomlStrValue`) and the message
`"Corrupt toml configuration"`.

### D4. "Set + fall back" collaboration after a completed value

Every value path ends in one of exactly two moves:

```java
storeItem(readScalarValue(b)); // or storeList(onArray())
return State.AFTER_VALUE;      // JSON / TOML: VALUE_END — fall back to the
                               // single "value done" conjunction point

return State.OBJ_START;        // nested object — set, onObjStart decides
                               // whether a frame must be pushed
```

`AFTER_VALUE` (JSON) / `VALUE_END` (TOML) become the sole conjunction point:
whatever follows (`,` `}` `\n` `#` EOF) is handled in one method.

## 4. Unchanged Types

| type | reason |
|---|---|
| `Cfg` / `CfgItem` / `CfgList` / `CfgObject` | data model untouched |
| `CfgException` | error model untouched |
| `CfgUtil` (all public methods incl. the 4 scanners) | v3 contract; see section 7 |
| `HeapWriteBuffer` / `WriteBuffer` | buffering model untouched |
| `DefaultConfigurationFacade` / `ConfigurationFacade` / `ConfigurationFactory` | routing only |
| `PropertiesCfgReader` | no state machine, no buffer, already simple |
| `readJsonStrValue` / `readTomlStrValue` | already single-responsibility escape state machines (~40 lines each); not split |

## 5. JsonCfgReader Restructure

### 5.1 State model (9 -> 8 values)

```java
enum State {
    INITIAL,       // find the opening '{' of the root object
    OBJ_START,     // entering an object: push a frame if a key is pending, expect '}' or '"'
    AFTER_VALUE,   // a value just completed: expect ',' or '}' (root '}' -> DONE)
    EXPECT_KEY,    // expect the opening '"' of a key
    KEY_READ,      // reading the key content up to the closing '"'
    COLON,         // expect the ':' separator
    EXPECT_VALUE,  // dispatch on the first byte of a value
    ARRAY,         // parsing array elements
    DONE,          // root object finished, exit the loop
}
```

Renames: `STR_ARR_OBJ_END` -> `AFTER_VALUE`, `KEY_START` -> `KEY_READ`,
`KEY_END` -> `COLON`. Removed: `STR_START` (absorbed by `readScalarValue`).
Added: `DONE`.

### 5.2 Fields / construction

- `private final PushbackInputStream input;` (unchanged, constructor wraps)
- `private final HeapWriteBuffer out = new HeapWriteBuffer(64);` (unchanged)
- `previous` + `keys` -> `private final Deque<Frame> frames = new ArrayDeque<>();`

### 5.3 Method list (all private instance methods)

| method | responsibility | approx. lines |
|---|---|---|
| `parse(int maxDepth)` | driver loop from D1 | ~18 |
| `State onInitial()` | find the root `{` | ~5 |
| `State onObjStart(int maxDepth)` | optional frame push, read `}` / `"` | ~12 |
| `State onAfterValue()` | read `,` / `}` / EOF, pop frame, return DONE | ~18 |
| `State onExpectKey()` | expect `"` | ~4 |
| `State onKeyRead()` | `search` the key content (buffer range decode + reset) | ~4 |
| `State onColon()` | expect `:` | ~4 |
| `State onExpectValue()` | dispatch `{` / `[` / scalar | ~8 |
| `State onArray()` | array element loop, `]` / `,` handling | ~14 |
| `String readScalarValue(int firstByte)` | string / number / bool dispatch (per D3) | ~15 |
| `void storeItem(String value)` | `putIfAbsent` + duplicate check + clear key | ~6 |
| `void storeList(List<String> value)` | same, stores `CfgList` | ~6 |

### 5.4 Pseudo-code

```java
private State onInitial() throws IOException {
    int b = CfgUtil.ignoreTillEOF(input, ' ', '\t', '\r', '\n');
    if (b != '{') {
        throw new CfgException("Corrupted json configuration");
    }
    return State.OBJ_START;
}

private State onObjStart(int maxDepth) throws IOException {
    if (key != null) {
        if (frames.size() >= maxDepth) {
            throw new CfgException("maximum nesting depth exceeded");
        }
        frames.addLast(new Frame(current, key));
        current = new HashMap<>();
        key = null;
    }
    int b = CfgUtil.ignoreTillEOF(input, ' ', '\t', '\r', '\n');
    if (b == '}') {
        return State.AFTER_VALUE;
    }
    if (b == '"') {
        return State.KEY_READ;
    }
    throw new CfgException("Corrupted json configuration");
}

private State onAfterValue() throws IOException {
    int b = CfgUtil.ignore(input, ' ', '\t', '\r', '\n'); // ignore: EOF returns -1
    if (b == ',') {
        return State.EXPECT_KEY;
    }
    if (b == '}') {
        Frame frame = frames.pollLast();
        if (frame == null) {
            return State.DONE; // root object closed
        }
        key = frame.key();
        if (frame.parent().putIfAbsent(key, new CfgObject(current)) != null) {
            throw new CfgException("Duplicate key: " + key);
        }
        current = frame.parent();
        key = null;
        return State.AFTER_VALUE;
    }
    if (b == -1) {
        if (frames.isEmpty()) {
            return State.DONE;
        }
        throw new CfgException("EOF reached");
    }
    throw new CfgException("Corrupted json configuration");
}

private State onExpectKey() throws IOException {
    int b = CfgUtil.ignoreTillEOF(input, ' ', '\t', '\r', '\n');
    if (b == '"') {
        return State.KEY_READ;
    }
    throw new CfgException("Corrupted json configuration");
}

private State onKeyRead() throws IOException {
    CfgUtil.search(input, out, '"');
    key = CfgUtil.readCfgKey(out.rawByteArray(), 0, out.intPosition());
    out.reset();
    return State.COLON;
}

private State onColon() throws IOException {
    int b = CfgUtil.ignoreTillEOF(input, ' ', '\t', '\r', '\n');
    if (b != ':') {
        throw new CfgException("Corrupted json configuration");
    }
    return State.EXPECT_VALUE;
}

private State onExpectValue() throws IOException {
    int b = CfgUtil.ignoreTillEOF(input, ' ', '\t', '\r', '\n');
    if (b == '{') {
        return State.OBJ_START;
    }
    if (b == '[') {
        return State.ARRAY;
    }
    storeItem(readScalarValue(b));
    return State.AFTER_VALUE;
}

private State onArray() throws IOException {
    List<String> list = new ArrayList<>();
    for (;;) {
        int b = CfgUtil.ignoreTillEOF(input, ' ', '\t', '\r', '\n');
        list.add(readScalarValue(b));
        b = CfgUtil.ignoreTillEOF(input, ' ', '\t', '\r', '\n');
        if (b == ']') {
            break;
        }
        if (b != ',') {
            throw new CfgException("Corrupted json configuration");
        }
    }
    storeList(list);
    return State.AFTER_VALUE;
}

private void storeItem(String value) {
    if (current.putIfAbsent(key, new CfgItem(value)) != null) {
        throw new CfgException("Duplicate key: " + key);
    }
    key = null;
}

private void storeList(List<String> value) {
    if (current.putIfAbsent(key, new CfgList(value)) != null) {
        throw new CfgException("Duplicate key: " + key);
    }
    key = null;
}
```

### 5.5 State-by-state semantic mapping (behavioral equivalence)

| state today | becomes | behavioral difference |
|---|---|---|
| `INITIAL` | `onInitial` | none |
| `OBJ_START` | `onObjStart` | none (`frames.size()` == old `previous.size()`) |
| `STR_ARR_OBJ_END` | `onAfterValue` | `return` -> `return State.DONE`, driver loop returns |
| `EXPECT_KEY` | `onExpectKey` | none |
| `KEY_START` | `onKeyRead` | none |
| `KEY_END` | `onColon` | none |
| `EXPECT_VALUE` | `onExpectValue` + `readScalarValue` | none (`"` branch moves into `readScalarValue`) |
| `STR_START` | inline in `readScalarValue('"')` | none (only reachable from `EXPECT_VALUE`, now `${}`) |
| `ARR_START` | `onArray` + `readScalarValue` | none |

Today `parse` is ~149 lines; the driver loop becomes ~18, with 11 methods each
at most ~18 lines.

## 6. TomlCfgReader Restructure

### 6.1 State model (8 -> 7 values)

```java
enum State {
    INITIAL,       // skip whitespace, detect EOF / '#' / '[' / key start
    COMMENT,       // skip to EOL, validate control characters
    TABLE_START,   // read '[table.header]' and navigate `current`
    VALUE_END,     // after a value: expect newline, EOF, or '#'
    KEY_READ,      // read the key up to '='
    VALUE_START,   // dispatch on the first byte of a value
    ARRAY,         // parsing array elements
    DONE,          // EOF reached, exit the loop
}
```

Renames: `KEY_START` -> `KEY_READ`. Removed: `STR_START` (absorbed by
`readScalarValue`). Added: `DONE`.

### 6.2 Method list (all private instance methods)

| method | responsibility | approx. lines |
|---|---|---|
| `parse(int maxDepth)` | driver loop from D1 | ~18 |
| `State onInitial()` | skip whitespace, dispatch `#` / `[` / key first byte | ~8 |
| `State onComment()` | skip comment + control-char validation | ~9 |
| `State onTableStart(int maxDepth)` | read table header, `tables` de-dup, navigate `current` | ~18 |
| `State onValueEnd()` | newline / EOF / `#` (EOF -> DONE) | ~7 |
| `State onKeyRead()` | read key up to `=` | ~8 |
| `State onValueStart()` | dispatch `[` / scalar | ~6 |
| `State onArray()` | array element loop | ~14 |
| `String readScalarValue(int firstByte)` | string / number / word dispatch (per D3) | ~15 |
| `void storeItem(String value)` | `current.value().putIfAbsent` + clear key | ~6 |
| `void storeList(List<String> value)` | same, stores `CfgList` | ~6 |

No `Frame`: `[table]` headers navigate `current` directly; no inline nesting.

### 6.3 Pseudo-code

```java
private State onInitial() throws IOException {
    int b = CfgUtil.ignore(input, ' ', '\t', '\r', '\n');
    if (b == -1) {
        return State.DONE;
    }
    if (b == '#') {
        return State.COMMENT;
    }
    if (b == '[') {
        return State.TABLE_START;
    }
    out.writeByte((byte) b);
    return State.KEY_READ;
}

private State onComment() throws IOException {
    CfgUtil.search(input, out, '\r', '\n');
    String commentStr = new String(out.rawByteArray(), 0, out.intPosition(), StandardCharsets.UTF_8);
    out.reset();
    commentStr.chars().forEach(c -> {
        if (Character.getType(c) == Character.CONTROL && rejectCommentControlCharacter(c)) {
            throw new CfgException("Corrupt toml configuration");
        }
    });
    return State.INITIAL;
}

private State onTableStart(int maxDepth) throws IOException {
    current = root;
    CfgUtil.search(input, out, ']');
    int nestedKeyLen = out.intPosition();
    String nestedKeyStr = new String(out.rawByteArray(), 0, nestedKeyLen, StandardCharsets.US_ASCII);
    if (!tables.add(nestedKeyStr)) {
        throw new CfgException("Duplicate table: " + nestedKeyStr);
    }
    for (String nestedKey : CfgUtil.readCfgNestedKey(out.rawByteArray(), 0, nestedKeyLen, maxDepth)) {
        Map<String, Cfg> currentMap = current.value();
        Cfg currentObj = currentMap.get(nestedKey);
        if (currentObj == null) {
            CfgObject cm = new CfgObject(new HashMap<>());
            currentMap.put(nestedKey, cm);
            current = cm;
        } else if (currentObj instanceof CfgObject cm) {
            current = cm;
        } else {
            throw new CfgException("Duplicate table: " + nestedKeyStr);
        }
    }
    out.reset();
    return State.VALUE_END;
}

private State onValueEnd() throws IOException {
    int b = CfgUtil.ignore(input, ' ', '\t');
    if (b == -1) {
        return State.DONE;
    }
    if (b == '\r' || b == '\n') {
        return State.INITIAL;
    }
    if (b == '#') {
        return State.COMMENT;
    }
    throw new AssertionError(); // unreachable, kept from today
}

private State onKeyRead() throws IOException {
    int b = CfgUtil.search(input, out, ' ', '\t', '=');
    key = CfgUtil.readCfgKey(out.rawByteArray(), 0, out.intPosition());
    out.reset();
    if (b != '=') {
        b = CfgUtil.ignoreTillEOF(input, ' ', '\t');
        if (b != '=') {
            throw new CfgException("Corrupt toml configuration");
        }
    }
    return State.VALUE_START;
}

private State onValueStart() throws IOException {
    int b = CfgUtil.ignoreTillEOF(input, ' ', '\t');
    if (b == '[') {
        return State.ARRAY;
    }
    storeItem(readScalarValue(b));
    return State.VALUE_END;
}

private String readScalarValue(int firstByte) throws IOException {
    if (firstByte == '"') {
        readTomlStrValue(input, out);
        String str = new String(out.rawByteArray(), 0, out.intPosition(), StandardCharsets.UTF_8);
        out.reset();
        return str;
    }
    if (firstByte == '-' || firstByte == '+' || (firstByte >= '0' && firstByte <= '9')) {
        String num = CfgUtil.readTomlNumber(input, firstByte, out);
        out.reset();
        return num;
    }
    if (firstByte == 't' || firstByte == 'f' || firstByte == 'i' || firstByte == 'n') {
        String word = CfgUtil.readTomlWord(input, firstByte, out);
        out.reset();
        return word;
    }
    throw new CfgException("Corrupt toml configuration");
}

private State onArray() throws IOException {
    List<String> list = new ArrayList<>();
    for (;;) {
        int b = CfgUtil.ignoreTillEOF(input, ' ', '\t');
        list.add(readScalarValue(b));
        b = CfgUtil.ignoreTillEOF(input, ' ', '\t');
        if (b == ']') {
            break;
        }
        if (b != ',') {
            throw new CfgException("Corrupt toml configuration");
        }
    }
    storeList(list);
    return State.VALUE_END;
}

private void storeItem(String value) {
    if (current.value().putIfAbsent(key, new CfgItem(value)) != null) {
        throw new CfgException("Duplicate key: " + key);
    }
    key = null;
}
```

Behavior notes: today's `VALUE_START` routes `"` into `STR_START`; after the
restructure it falls into `readScalarValue('"')` — equivalent. The
`onValueEnd` `AssertionError` is today's unreachable branch, kept as-is. Today
`parse` is ~147 lines; the driver loop becomes ~18.

## 7. CfgUtil Scanners Stay Untouched

The four scanners are **not** restructured and receive no `record` intermediate
state. Rationale, scanner by scanner:

1. `readTomlNumber` decimal negative accumulation: the working state
   `(acc, overflow, lastWasUnderscore, buf)` is read/written inside one while
   loop, never crosses a method boundary; packing it into a record adds an
   `.acc()` / `.overflow()` accessor suffix to every site and replaces a
   readable `acc = acc * 10` flow with fan-in of getters. Zero readability gain.
2. `accumulateRadix`: its field set `(acc, hasDigit, lastWasUnderscore)` differs
   from the decimal path's. A shared record would have to be the union set,
   leaving half the fields idle at each use — worse.
3. `readTomlFloatContinue`: only `lastWasUnderscore` to track; nothing to pack.
4. `readJsonNumber` / `readJsonBool`: pure linear "write + read" flow, no
   accumulation state.

The general counter-argument: value records pay off for multi-return values or
state crossing method boundaries. The only way to create such a boundary here
would be extracting the digit loop into a helper returning a record — which
either breaks the single-pass property (two scans, violates constraint 4/v3 D3)
or forces passing `PushbackInputStream` + `HeapWriteBuffer` into the helper
(more parameters than the record saves). Cost exceeds benefit; keep local
variables.

Optional, out of scope: `readCfgKey` and `readCfgNestedKey` share a validation
predicate variant (`rejectKey` vs `rejectKey || '.'`) — a future micro-cleanup
candidate only.

## 8. Explicit Non-Goals (YAGNI)

| # | not done | reason |
|---|---|---|
| 1 | abstract base class / shared parent for the readers | JSON and TOML differ in nesting model (frame stack vs tree navigation), key separator (`:` vs `=`), comments, escapes and number routing; a shared base ends up as "abstract methods + two half-subclasses", harder to read than two self-contained classes |
| 2 | generic state-machine framework (transition tables / config-driven) | two readers, 15 states total; config-driven dispatch is over-engineering |
| 3 | `ParseContext` aggregate object (input + out + frames) | fields belong naturally to the reader instance; single-use, no concurrency; packing merely renames `this.` |
| 4 | `record`-packed scanner intermediate state | section 7 |
| 5 | change the four `CfgUtil` scanner signatures / returned `String` | contract of v3 doc and of the test suite |
| 6 | split `readJsonStrValue` / `readTomlStrValue` | already single-responsibility escape state machines; the two-state `escaping` boolean would be fragmented by splitting |
| 7 | touch `PropertiesCfgReader` | no state machine, no buffer, already simple |
| 8 | extract the TOML `onTableStart` tree navigation into a shared `CfgUtil` method with `PropertiesCfgReader` | ~6 similar lines, different error messages; a shared helper needs a context-name parameter for only two call sites |
| 9 | unify JSON/TOML `readScalarValue` via interface/lambda | dispatch branches, messages and underlying scanners all differ; unification means 4 function parameters or a strategy interface |
| 10 | split the dual role of the `key` field (current key + "frame pending" flag) | it is the implicit protocol of the frame stack (`onObjStart` pushes iff `key != null`); an explicit boolean touches every state handler for a benefit below the risk |
| 11 | move exception messages or throw points | tests assert only the type, but constraint "same input, same failure" requires byte-for-byte equivalence anyway |
| 12 | split the readers into separate files (`JsonCfgReader.java` etc.) | pure file-layout change, no behavioral gain; nested sealed-interface style matches the project; an independent decision for after v4 if ever desired |
| 13 | per-method unit tests for the new private methods | private methods are not directly testable; 71 black-box tests cover every path, a behavior-preserving restructure needs no new tests |

## 9. Trade-offs

### 9.1 Benefits

1. **Readability**: two ~150-line blocks become one ~18-line driver + 11/10
   methods of at most ~18 lines. Reading is "read the state name + only that
   handler"; the collaboration
   `EXPECT_VALUE -> readScalarValue -> storeItem -> AFTER_VALUE` is visible at a
   glance.
2. **Testability (indirect)**: each state handler is an addressable black-box
   path; a failing test narrows the suspect to a single method.
3. **Extensibility**: a new value kind (e.g. a future `null`) touches only
   `readScalarValue` + the dispatcher; a new state adds one enum value + one
   switch branch + one method. The driver loop is never edited for features.
4. **De-duplication**: two duplicated scalar dispatch blocks (each ~18 lines)
   merge into one; six store tails merge into two methods; the parallel-deque
   implicit pairing (a desync hazard) disappears with `Frame`.

### 9.2 Costs

1. **Indirection**: one extra method call per state transition. The handlers are
   small single-call-site methods that C2 inlines; the 71 tests are unaffected,
   and for real configs the bottleneck is I/O and `HashMap` work, not dispatch.
2. **Allocation**: the only new allocation is `Frame` (one per nesting level),
   whereas the old code already allocated one `HashMap` per level plus two
   `Deque` nodes; the increment is negligible and sits on the nesting path, not
   the hot per-token path.
3. **Line count**: net line count stays roughly flat or slightly grows (method
   signatures). The gain is structural, not numeric — that is the point of v4.
4. **switch statement -> switch expression**: free on JDK 28; the exhaustive
   enum switch needs the one unreachable `DONE` branch (cost ~1 line).

### 9.3 Alternatives compared

| alternative | description | verdict |
|---|---|---|
| **chosen design** | method-per-state + `readScalarValue`/`storeItem`/`storeList` extraction + `Frame` record | best structural consistency; JSON/TOML are isomorphic, one pattern to learn |
| A: minimal extraction | only extract `readScalarValue` + `onArray`, keep the rest inline in the switch | smallest diff, but the main loop still weighs ~80 lines and frame logic stays tangled with flow — treats symptoms |
| B: event/turn object driving | a "current event + result" object drives the loop | extra abstraction for two one-way readers, and per-token event-object allocation violates the no-hot-path-allocation constraint |
| C: merged readers | one shared state table for JSON + TOML | nesting models and line semantics differ; every merged state needs a format `if` — worse than two separate classes |

Conclusion: no `CfgUtil` change (section 7); the entire gain comes from the
internal restructure of the two readers.

## 10. Migration and Validation

### 10.1 Steps (each atomic and independently reviewable)

**Step 1 — JsonCfgReader restructure (one commit)**
1. add `Frame`, replace `previous` + `keys` with `frames`;
2. split per section 5.4, rewrite the driver loop, rename states
   (`AFTER_VALUE` / `KEY_READ` / `COLON` / `DONE`), delete `STR_START`.
Verify: `mvn -o test -pl jing-common -am` — all 71 tests green. If red, check
in order: `frames.size()` vs `previous.size()` at the `maxDepth` boundary,
`onAfterValue` keeps `ignore` (not `ignoreTillEOF`), `readScalarValue('"')`
still consumes the closing quote.

**Step 2 — TomlCfgReader restructure (one commit)**
Split per section 6.3 + state renames + delete `STR_START`. Verify: same
command, 71 tests green.

**Step 3 — behavioral equivalence review (white-box)**
- state-by-state mapping tables (5.5 / 6.x) reviewed pairwise;
- diff review: no `StringBuilder`, no regex, no per-char concatenation, no
  `import static`, number of `new String` sites unchanged (still exactly one
  per token), no `System.out`, comments all `//`, exception messages verbatim.

**Step 4 — documentation and change log**
1. `doc/Configuration-v4.md` (this design);
2. append the mandatory Chinese `diff.md` entry with timestamp per AGENTS.md.

### 10.2 New tests (only if gaps found — optional)

All 71 existing tests cover the string/number canonicalization (incl.
`Long.MIN_VALUE`, radix, underscores, inf/nan), booleans, mixed arrays,
duplicate keys, depth limit and EOF truncations. Candidate gap checks:

- TOML empty input -> `root` (the `onInitial` `DONE` path) — confirm the empty
  case exists in `TomlCfgReaderTest`, add one if missing;
- JSON root `}` after EOF (`onAfterValue` `DONE` branch) — usually covered by
  the normal cases; confirm only.

### 10.3 Risks and rollback

- Main risk: Step 1 moving the string path from a dedicated state into
  `readScalarValue`; if the closing quote is not consumed, every following `,` /
  `}` parse breaks — multi-key tests (`{"a": "x", "b": "y"}` style) expose this
  immediately.
- Each step is an atomic commit; on failure, revert that step only (note:
  AGENTS.md forbids git operations, so rollback is done manually by the
  implementer).
- Behavior-preserving restructure: no new tests are required; the 71 black-box
  tests are the complete regression net.

## 11. Estimated Diff Size

| file | approx. changed lines | note |
|---|---|---|
| `CfgReader.java` JsonCfgReader | restructure of ~150-line `parse` into 12 methods | net lines roughly flat |
| `CfgReader.java` TomlCfgReader | restructure of ~147-line `parse` into 11 methods | net lines roughly flat |
| tests | 0 | optionally 1-2 gap cases if found |
| `CfgUtil.java` / other files | 0 | not touched |

## 12. Open Questions / Decisions Taken

- `Frame` per nesting level is an acceptable allocation: it replaces an
  implicit pairing of two `Deque`s whose nodes were already allocated per level.
- Records for scanner intermediate state: rejected (section 7).
- Shared abstraction between the two readers / a generic state-machine
  framework: rejected (section 8 items 1-3, 9, and 9.3).
- `key` keeps its dual role (current key + frame-pending flag): decision taken
  (section 8 item 10).
- Pure file-layout split of the readers: deferred, independent decision (8-12).