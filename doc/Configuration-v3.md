# Configuration Parser Redesign: Byte-Level Buffering and Number/Boolean Support

Status: design / not yet implemented
Scope: `jing-common` `io.jingproject.common.conf` package only.
Supersedes the parser details in `doc/Configuration-v2.md` (the data model and
the facade described there remain authoritative).

## 1. Goals

1. **Performance**: replace `ByteArrayOutputStream` with `HeapWriteBuffer`
   (reusable `byte[]` backing array, no per-token allocation, no implicit
   `Object[]`-free growth copying via `Arrays.copyOf` only when needed).
2. **Value support**: JSON and TOML parsers accept **numbers** and **booleans**
   in addition to strings. TOML additionally accepts `inf`/`nan` (± prefix).
3. **Data model unchanged**: `Cfg` keeps only `CfgItem(String)`,
   `CfgList(List<String>)`, `CfgObject(Map<String, Cfg>)`. Numbers/booleans are
   **canonicalized to String at parse time**.
4. **Byte-level validation**: every syntactic check happens on raw bytes, one
   byte at a time, before any `String` is constructed.
5. **String is a terminal state**: no code traverses, slices, replaces or scans
   the produced `String`. The only operations on `String` after its single
   construction are storing it into `Cfg` and (by the caller) JDK converters
   such as `Integer.parseInt` / `Double.parseDouble`.
6. **No regex** anywhere.
7. **JDK-compatible canonical forms**: every stored number string must be
   directly convertible by the JDK number parsers without extra preprocessing
   (see section 5.3).

## 2. Constraints

- Zero third-party dependencies (`jing-common` rule).
- Comments only with `//`, lowercase sentence start, 120 col, 4-space indent
  (project AGENTS.md).
- All comment/exception text in English starting lowercase.
- `@Fragile` classes: minimal defensive programming.
- No recursion (state machines are iterative; use `Deque` where needed).

## 3. Core Design Decisions

### D1. HeapWriteBuffer replaces ByteArrayOutputStream

One `HeapWriteBuffer` instance per reader, constructed once, reused for every
token (keys, string values, number tokens). `reset()` (position = 0) replaces
`output.reset()`. Between tokens the buffer must be reset; building a `String`
from the buffer always copies (`new String(byte[], off, len, cs)`), so resetting
afterwards is always safe even when the buffer later grows.

API mapping (current `ByteArrayOutputStream` usage -> `HeapWriteBuffer`):

| current | replacement | note |
|---|---|---|
| `new ByteArrayOutputStream()` | `new HeapWriteBuffer(INITIAL, LIMIT)` | see D5 buffer sizing |
| `output.write(b)` | `output.writeByte((byte) b)` | int -> byte cast required |
| `output.toString(StandardCharsets.UTF_8)` | `new String(output.rawByteArray(), 0, output.intPosition(), StandardCharsets.UTF_8)` | single terminal decode |
| `output.toByteArray()` | `output.rawByteArray()` + `intPosition()` range | zero-copy range access |
| `output.reset()` | `output.reset()` | same semantics |
| `CfgUtil.writeUnicodeInUtf8(output, cp)` | `output.writeCodePointInUtf8(cp)` | identical logic, already a `WriteBuffer` default method |

### D2. PushbackInputStream for one-byte lookahead

Token readers (number / boolean / inf-nan) must stop AT a delimiter without
consuming it (the surrounding state machine then reads the delimiter itself,
e.g. `,` `]` `}` `}` `\n`).

- Wrap the incoming `InputStream` with `PushbackInputStream` **inside the
  reader constructors** (`JsonCfgReader`, `TomlCfgReader`).
- Token readers read one byte; if it belongs to the token character class, they
  write it / accumulate it, otherwise they call `pushback.unread(b)` and return.
- All existing `CfgUtil` stream helpers (`ignore`, `ignoreTillEOF`, `search`,
  `assume`, `readUnicode`) keep working unchanged on the wrapped stream.

Rationale: `InputStream` has no native pushback; `PushbackInputStream` is JDK,
zero dependency, and exactly one pushed-back byte per token is enough.

### D3. In-band byte validation state machines

Every token scanner is a small byte-level state machine. Validation happens
while bytes are read; the buffer receives **only bytes that already passed**.
On any invalid byte sequence the scanner throws `CfgException` immediately
(no `String` is built). See section 6 for the exact FSMs.

### D4. Canonicalization at parse time

Numbers are normalized while scanning so the stored `String` is the canonical
JDK-parseable form:

| source | canonical stored String |
|---|---|
| TOML decimal int `42`, `+3`, `-7`, `1_000_000` | `42`, `3`, `-7`, `1000000` (underscores removed, sign applied) |
| TOML hex `0xFF`, `0xF_F` | `255` (value accumulated in base 16, emitted decimal) |
| TOML octal `0o77`, `0o7_7` | `63` |
| TOML binary `0b1010`, `0b10_10` | `10` |
| TOML float `3.14`, `1e10`, `1_0.5` | `3.14`, `1e10`, `10.5` (underscores stripped; digits/exponent preserved) |
| TOML `+inf` / `-inf` / `inf` | `Infinity` / `-Infinity` / `Infinity` |
| TOML `+nan` / `-nan` / `nan` | `NaN` (sign of NaN dropped) |
| JSON int `42`, `-7` | `42`, `-7` (kept as written) |
| JSON float `3.14`, `1e10`, `2.5E-3` | `3.14`, `1e10`, `2.5E-3` (kept as written) |
| boolean `true` / `false` | `true` / `false` |

Integer canonicalization is done via value accumulation (`long`) and a single
`Long.toString(value)` at the end; float/JSON forms are copied byte-for-byte
(with underscores skipped) into the buffer and decoded once at the end.

### D5. Buffer sizing

Construct the reader buffer as `new HeapWriteBuffer(64)`.

- minimum initial size enforced by `HeapWriteBuffer.MIN_INITIAL_SIZE = 4`; 64
  covers typical keys/values without growth.
- default `limit = Integer.MAX_VALUE` (single-arg constructor), so legitimate
  long values never trip `SizeLimitExceededException`. Config files originate
  from trusted classpath resources; unbounded growth to `Integer.MAX_VALUE` is
  acceptable (a hostile resource is out of scope for this parser).
- `Marshaler`-style max-length guard is intentionally NOT added (see @Fragile,
  minimalism).

### D6. String construction points (the only places `String` is built)

After this redesign, `new String(...)` / string literals appear exactly at:

| site | form | traversal? |
|---|---|---|
| string value ends (JSON/TOML) | `new String(buf.rawByteArray(), 0, buf.intPosition(), UTF_8)` | none (single decode) |
| key ends (JSON/TOML) | `CfgUtil.readCfgKey(buf.rawByteArray(), 0, buf.intPosition())` -> `new String(data, off, len, US_ASCII)` | none (single decode) |
| TOML integer canonical | `Long.toString(accumulated)` (after byte-level accumulation) | none |
| JSON number / TOML float | `new String(buf.rawByteArray(), 0, buf.intPosition(), US_ASCII)` | none |
| boolean / inf / nan | string literals `"true"`, `"false"`, `"Infinity"`, `"-Infinity"`, `"NaN"` | none |

No other `String` is created for values. In particular, the raw token is never
converted to `String` first and then scanned/cleaned.

## 4. Data Model (unchanged)

`Cfg.java` stays as is:

```java
sealed interface Cfg permits CfgItem, CfgList, CfgObject { ... }
record CfgItem(String value)      // scalar, stores canonical string
record CfgList(List<String> value) // array of canonical strings
record CfgObject(Map<String, Cfg> value)
```

Nested arrays of objects remain unsupported (existing behavior). Arrays may now
contain **mixed** element types; every element becomes a canonical String:

- JSON `[1, "two", true]` -> `CfgList(["1", "two", "true"])`
- TOML `[1, "two", true]` -> `CfgList(["1", "two", "true"])`

## 5. Value Grammar and Canonical Forms

### 5.1 JSON numbers (strict per RFC 8259 grammar)

```
number    = [ "-" ] int [ frac ] [ exp ]
int       = "0" / digit1-9 *digit
frac      = "." 1*digit
exp       = ( "e" / "E" ) [ "-" / "+" ] 1*digit
```

Rules enforced byte-wise:
- leading `+` is rejected (JSON does not allow it);
- no leading zeros (except the literal `0`);
- at least one digit required; `.` and `e/E` require digits on both sides
  (`.5`, `5.`, `1e` are rejected);
- no underscores, no `x/o/b` characters (they are not even routed to the
  scanner by the dispatcher, but the scanner rejects them defensively);
- canonical form = the input bytes verbatim (digit/sign/dot/exponent), written
  into the buffer as validated; decoded once to String at the end.

### 5.2 TOML numbers (per toml.io 1.1; strict subset)

Routing by prefix (checked byte-wise after optional sign for decimal):

| prefix | kind | digit set | leading-zero rule |
|---|---|---|---|
| `0x` / `0X` | hex integer | `0-9a-fA-F` | no leading zeros; `0x0` allowed |
| `0o` / `0O` | octal integer | `0-7` | `0o0` allowed |
| `0b` / `0B` | binary integer | `01` | `0b0` allowed |
| digits, optional sign | decimal integer | `0-9` | no leading zeros; `0`, `+0`, `-0` allowed |
| digits [`.` digits] [`e`/`E` ...] | float | `0-9`, `.`, `e/E` signs | no leading zeros in int part; integer part required; fraction/exponent require digits |
| `inf` / `+inf` / `-inf` | infinity | literal word | n/a |
| `nan` / `+nan` / `-nan` | not-a-number | literal word | n/a |

Underscore rules (byte-level):
- allowed only **between** digits of decimal/hex/octal/binary integer words and
  of the int/fraction part of floats;
- never leading, never trailing, never adjacent;
- **never** inside the exponent (`1e1_0` rejected);
- hex/octal/binary literals are **unsigned** per TOML spec (a `-` or `+`
  directly before `0x`/`0o`/`0b` is rejected).

Overflow:
- decimal/hex/octal/binary magnitudes are accumulated into `long`. If the
  accumulated magnitude overflows the 64-bit signed range, throw
  `CfgException` (TOML integers are specified as signed 64-bit).
- `Long.MIN_VALUE` (`-9223372036854775808`) is handled via negative
  accumulation (see section 6.2) and produces the exact canonical string.

Canonical forms (D4): decimal integers and hex/octal/binary emit
`Long.toString(value)`; floats emit copied bytes with underscores stripped;
inf/nan emit the fixed literals.

### 5.3 JDK conversion contract

Every stored number string must be directly convertible, **without any
pre-processing by the caller**, as follows:

| stored canonical string | direct JDK conversion |
|---|---|
| `42`, `-7`, `1000000` | `Integer.parseInt(s)` then `Long.parseLong(s)` (each iff it fits) |
| `255` (from hex/octal/binary) | `Integer.parseInt(s)` / `Long.parseLong(s)` |
| `3.14`, `1e10`, `2.5E-3`, `10.5` | `Double.parseDouble(s)` |
| `Infinity`, `-Infinity`, `NaN` | `Double.parseDouble(s)` |
| `true`, `false` | `Boolean.parseBoolean(s)` |

This is why `0xFF`-style raw text is never stored as-is, and why underscores
are stripped at parse time (JDK converters reject both).

## 6. Byte-Level State Machines (specification)

Conventions:
- `b` is the current byte (`int` from `input.read()`, `-1` = EOF).
- token scanners are `static` helpers in `CfgUtil`; they take the wrapped
  `PushbackInputStream`, the `firstByte` already consumed by the dispatcher,
  and the `HeapWriteBuffer` for output.
- on hitting a non-token byte or EOF, scanners `pushback.unread(b)` (EOF -> no
  unread) and return their result; throwing when the token is incomplete.

### 6.1 JSON number scanner: `readJsonNumber(input, firstByte, buf) -> String`

```
states: SIGN? -> INT -> (FRAC)? -> (EXP)?
INT: if '0': accept, then mustContain '.'/'e'/'E' else END
     else digit1-9, then 0-9 digits
FRAC: '.', then >=1 digit
EXP: 'e'/'E', optional sign, then >=1 digit
at any invalid character -> CfgException("invalid json number: ...")
on END: decode buf range once -> String
```

Scanner writes every accepted byte into `buf` (verbatim copy).

### 6.2 TOML number scanner: `readTomlNumber(input, firstByte, buf) -> String`

```
consume optional sign (record it, do NOT emit — decimal canonical form is
  produced from the accumulated value).
peek next byte:
  if 'i' or 'n' -> inf/nan word reader (6.4), apply sign to form
    "+inf"/"-inf"/"+nan"/"-nan" handling; emit literal; return.
  if '0' and next is 'x'/'X'/'o'/'O'/'b'/'B' -> radix reader (base 16/8/2);
    unsigned per spec (a recorded sign here -> CfgException).
  else -> decimal reader.

decimal/radix accumulation (shared):
  acc = 0 (accumulate as negative to cover Long.MIN_VALUE):
  for each digit d (radix r): acc = acc*r - digitValue(d)
  after end: if sign negative -> value = acc
             else: if acc == Long.MIN_VALUE -> overflow CfgException
                   else value = -acc
  emit Long.toString(value) — this is the terminal String; no byte copy.
  (decimals and radix forms both land here; underscore bytes are consumed and
   validated but never emitted.)
```

Float handling (TOML), when a `.` or `e/E` appears in the decimal path:

```
states: INT (digits, underscores allowed between) ->
           (FRAC: '.' then >=1 digit, underscores allowed) ->
           (EXP: 'e'/'E', optional sign, >=1 digit, underscores FORBIDDEN)
at least one digit in INT; int part must not have leading zeros.
copy accepted bytes into buf, skipping underscores (only digit characters are
  written); on END decode once -> String.
```

### 6.3 Boolean reader (shared JSON/TOML): `readBoolWord(input, firstByte, buf)`

```
firstByte 't' -> expect "true"  : bytes r,u,e  (else CfgException)
firstByte 'f' -> expect "false" : bytes a,l,s,e (else CfgException)
write the literal bytes into buf; decode -> String literal.
```

JSON dispatches only on `t`/`f`; TOML routes `t`/`f`/`i`/`n` into the same
word reader and validates the full word.

### 6.4 TOML inf/nan word reader: `readTomlWord(input, firstByte, buf)`

```
word = alpha bytes (a-z,A-Z) until non-alpha (unread it).
if word == "inf"  -> emit "Infinity"   (with '-' sign prefix -> "-Infinity")
if word == "nan"  -> emit "NaN"
else -> CfgException("invalid toml value: " + ...) // built from buf only on error path
```

`+inf`/`-inf`/`+nan`/`-nan` are handled by the sign-then-peek path in 6.2;
bare `inf`/`nan` are handled by the dispatcher routing `i`/`n` directly.

### 6.5 String value scanners (existing logic, sink swapped)

`readJsonStrValue(input, buf)` and `readTomlStrValue(input, buf)` keep their
current logic (escape handling incl. `\uXXXX`, surrogate pairs, `\x/\u/\U` for
TOML) but write into `HeapWriteBuffer` via `writeByte((byte) b)` and
`buf.writeCodePointInUtf8(cp)` for escapes. The terminating `"` is consumed by
the scanner exactly as today; the following state continues normally.

## 7. Per-File Change List

### 7.1 `CfgUtil.java` (io.jingproject.common.conf)

Modified:

- `search(InputStream, OutputStream, int...)` -> `search(InputStream, WriteBuffer, int...)`
  - signature change only; the `output.write(b)` line becomes
    `output.writeByte((byte) b)`. Used by the KEY_START states of both readers.
- `readCfgKey(byte[])` -> `readCfgKey(byte[] data, int off, int len)`
  - callers now pass `(buf.rawByteArray(), 0, buf.intPosition())` instead of a
    freshly `toByteArray()`-ed copy.
  - validation loop unchanged (still walks raw bytes); terminal
    `new String(data, off, len, US_ASCII)`.
- `readCfgNestedKey(byte[], int)` -> `readCfgNestedKey(byte[] data, int off, int len, int maxDepth)`
  - same range-based change; validation loop unchanged; depth checks unchanged.
- remove `writeUnicodeInUtf8(ByteArrayOutputStream, int)`:
  replaced by `WriteBuffer.writeCodePointInUtf8`, which is byte-identical logic.

Added (all byte-level scanners, no regex):

| method | purpose |
|---|---|
| `readJsonNumber(InputStream, int firstByte, HeapWriteBuffer buf) -> String` | FSM 6.1 |
| `readJsonBool(InputStream, int firstByte, HeapWriteBuffer buf) -> String` | FSM 6.3 |
| `readTomlNumber(InputStream, int firstByte, HeapWriteBuffer buf) -> String` | FSM 6.2 (routes dec/radix/float/inf-nan) |
| `readTomlWord(InputStream, int firstByte, HeapWriteBuffer buf) -> String` | FSM 6.3 + 6.4 (true/false/inf/nan) |
| private `readTomlInfNan(...)`, private `accumulateInt(...)`, private `readTomlFloat(...)` | internal helpers of 6.2/6.4 |

Unchanged: `assume`, `ignore`, `ignoreTillEOF`, `rejectKey`, `readUnicode`, the
priv-constructor utility-class guard.

Note: scanner inputs are typed as `InputStream`; inside they cast to
`PushbackInputStream` (readers guarantee the wrapped stream).

### 7.2 `CfgReader.java` — `JsonCfgReader`

Fields / construction:

- `private final InputStream input;` -> `private final PushbackInputStream input;`
- constructor: `this.input = new PushbackInputStream(input);` (the byte-arg
  parse must not be closed separately; the outer try-with-resources closes the
  original).
- add `private final HeapWriteBuffer out = new HeapWriteBuffer(64);`
- delete the per-parse `try (ByteArrayOutputStream output = ...)` wrapper;
  `parse` becomes a plain method using `out`.

State changes (by current state name / line refs of the current file):

| state / method | change |
|---|---|
| `STR_START` | `readJsonStrValue(input, out)`; then `String str = new String(out.rawByteArray(), 0, out.intPosition(), StandardCharsets.UTF_8); out.reset();` — single terminal decode |
| `KEY_START` | `CfgUtil.search(input, out, '"')`; `key = CfgUtil.readCfgKey(out.rawByteArray(), 0, out.intPosition()); out.reset();` |
| `EXPECT_VALUE` | add dispatcher branches after `{`: `'-'` / `'0'..'9'` -> `String num = CfgUtil.readJsonNumber(input, b, out); ... put(n, new CfgItem(num)); state = STR_ARR_OBJ_END;` ; `'t'`/`'f'` -> `CfgUtil.readJsonBool(...)` -> same tail; all use the existing `putIfAbsent` duplicate check; `key` nulled; |
| `ARR_START` | loop body gains the same two branches (number then bool), appending the canonical String to `strList`; the `,`/`]` continuation logic is unchanged |

`State` enum: unchanged.

### 7.3 `CfgReader.java` — `TomlCfgReader`

Mirror of JsonCfgReader:

- constructor wraps `PushbackInputStream`, adds `private final HeapWriteBuffer out = new HeapWriteBuffer(64);`, drops the `ByteArrayOutputStream` wrapper from `parse`.
- `STR_START` / `KEY_START`: identical sink swap as JSON.
- `VALUE_START`: dispatcher adds
  - `'-'` / `'+'` / `'0'..'9'` -> `CfgUtil.readTomlNumber(input, b, out)` -> `CfgItem(...)`, `state = VALUE_END`;
  - `'t'` / `'f'` / `'i'` / `'n'` -> `CfgUtil.readTomlWord(input, b, out)` -> `CfgItem(...)`, `state = VALUE_END`;
  - all other exits as today (`CfgException("Corrupt toml configuration")`).
- `ARR_START`: loop body gains the same two branches (number, then word);
  `,`/`]` continuation logic unchanged.
- `TABLE_START` / `COMMENT` / `INITIAL` / `VALUE_END`: unchanged.

### 7.4 Unchanged files

| file | reason |
|---|---|
| `PropertiesCfgReader` | reads via `java.util.Properties.load(Reader)`; values are strings by nature; no buffer/scan change |
| `Cfg.java` | type model untouched |
| `DefaultConfigurationFacade.java` | only routes `InputStream` into readers; dispatch/conf/confList untouched |
| `CfgException.java` | unchanged |
| `ConfigurationFactory` / `ConfigurationFacade` (io.jingproject.common) | untouched |

### 7.5 `module-info.java`

No change required (`PushbackInputStream`, `HeapWriteBuffer`, `StandardCharsets`
are already accessible within `jing-common`).

## 8. Error Handling

- All syntax errors: `CfgException` with a lowercase-starting message, thrown at
  the exact byte that fails (byte value or the offending sequence is included:
  `invalid json number: ...`, `invalid toml decimal integer: ...`,
  `invalid toml value: ...`). No partial `String` is left behind; the buffer is
  simply reset on the next token.
- Duplicate keys/tables: existing `putIfAbsent` paths, unchanged messages.
- Overflow of TOML 64-bit integers: `CfgException` (e.g.
  `integer literal out of range: ...`).
- `HeapWriteBuffer` growth beyond `limit`: would throw unchecked
  `SizeLimitExceededException`; with `new HeapWriteBuffer(64)` (limit =
  `Integer.MAX_VALUE`) this is unreachable in practice (documented D5).

## 9. Validation / Test Plan

New test file `jing-common/src/test/java/io/jingproject/commontest/conf/CfgNumberParsingTest.java` (JUnit 6, class must end in `Test`; `jing-common` depends only on JUnit). No third-party assertions beyond JUnit.

Coverage matrix (via `String`-producing helper methods in the test or by
parsing small resources / in-memory `ByteArrayInputStream`):

| input | expected stored value |
|---|---|
| JSON `{"a": 42}` | `CfgItem("42")` |
| JSON `{"a": -7}` | `CfgItem("-7")` |
| JSON `{"a": 3.14}` | `CfgItem("3.14")` |
| JSON `{"a": 1e10}` / `2.5E-3` / `-0.5` | `CfgItem("1e10")` / `"2.5E-3"` / `"-0.5"` |
| JSON `{"a": 0}` / `{"a": -0}` | `"0"` / `"-0"` |
| JSON `{"a": true}` / `{"a": false}` | `"true"` / `"false"` |
| JSON `{"a": [1, "two", true]}` | `CfgList(["1", "two", "true"])` |
| JSON rejections | `{"a": 007}`, `{"a": .5}`, `{"a": 5.}`, `{"a": 1e}`, `{"a": +1}`, `{"a": 1_0}`, `{"a": 0xFF}` |
| TOML `a = 42` / `a = -7` / `a = +3` | `"42"` / `"-7"` / `"3"` |
| TOML `a = 1_000_000` | `"1000000"` |
| TOML `a = 0xFF` / `0o77` / `0b1010` | `"255"` / `"63"` / `"10"` |
| TOML `a = 3.14` / `1e10` / `2.5E-3` / `1_0.5` | `"3.14"` / `"1e10"` / `"2.5E-3"` / `"10.5"` |
| TOML `a = inf` / `-inf` / `+inf` | `"Infinity"` / `"-Infinity"` / `"Infinity"` |
| TOML `a = nan` / `-nan` / `+nan` | `"NaN"` |
| TOML `a = true` / `false` | `"true"` / `"false"` |
| TOML `a = [1, "two", true]` | `CfgList(["1", "two", "true"])` |
| TOML `a = -9223372036854775808` | `"-9223372036854775808"` |
| TOML rejections | `a = 007`, `a = 0x`, `a = 0xFF_`, `a = _1`, `a = 1__0`, `a = 1e1_0`, `a = .5`, `a = 5.`, `a = 9223372036854775808`, `a = -0xFF` |
| Mixed TOML table `[t]` with numeric/bool keys | nested values canonicalized |

JDK-convertibility assertion helper in the test: for every accepted canonical
string, assert the corresponding `Integer.parseInt` / `Long.parseLong` /
`Double.parseDouble` / `Boolean.parseBoolean` succeeds (this pins the contract
of 5.3).

## 10. Estimated Diff Size

| file | approx. added lines | approx. modified lines |
|---|---|---|
| `CfgUtil.java` | +110 (scanners) | +30 (signature/search) |
| `CfgReader.java` JsonCfgReader | +25 | +35 |
| `CfgReader.java` TomlCfgReader | +35 | +45 |
| test (new) | +150 | — |

## 11. Open Questions / Decisions Taken

- JSON sign: strict RFC 8259 (reject `+`). Decision taken (documents the
  current "Corrupted" strictness).
- TOML radix literals: unsigned per spec (reject `-0xFF`). Decision taken.
- `-0` TOML decimal canonicalizes to `"0"` via `Long.toString` (information
  equivalent); JSON keeps `"-0"` verbatim (byte copy). Both acceptable under
  5.3.
- Array-of-objects / empty arrays: remain unsupported (existing limitation,
  unchanged).
- inf/nan canonical casing: `Infinity`/`-Infinity`/`NaN` (Java
  `Double.parseDouble` spellings). Decision taken.