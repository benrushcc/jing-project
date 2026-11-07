# jing-marshall-cbor 实现与类型映射

> 模块：`jing-marshall-cbor`
> 标准：CBOR（RFC 8949）
> 关联文档：[Marshall.md](./Marshall.md)
> 状态：与当前实现一致

本文档描述 `jing-marshall-cbor` 的公开 API、CBOR 与 Java 类型映射、序列化/反序列化策略、限制与测试，并对比 Jackson CBOR 在关键策略上的选择。

---

## 1. 模块定位

`jing-marshall-cbor` 是 `jing-marshall` 的 CBOR 格式实现：

- 复用 `@Marshallable`、`@MarshallAttr`、`MarshallFacade`、`MarshallInfo` 和 transformer 体系；
- 不引入第三方运行时依赖；
- 直接读写调用方提供的 `WriteBuffer` / `ReadBuffer`，支持 heap 和 segment 两种实现；
- 所有容器和对象处理都使用显式栈或迭代，不使用递归；
- 输出使用 CBOR preferred serialization 的最短 head；Java `float` / `double` 始终按固定宽度写出，不做最短浮点宽度选择。

---

## 2. 公开 API

### 2.1 序列化

`CborSerializer`：

| 方法 | 说明 |
|---|---|
| `serializeMarshallableObject(Object, WriteBuffer)` | bean / record 对象 |
| `serializeArray(Object[], WriteBuffer)` | 对象数组，含包装类型数组和 `String[]` |
| `serializeCollection(Collection<T>, Class<T>, WriteBuffer)` | `List` / `Collection` |
| `serializeMap(Map<K,V>, Class<K>, Class<V>, WriteBuffer)` | key 仅支持 `String` / `CharSequence` |
| `serializeByteArray(byte[], WriteBuffer)` | `byte[]` 编码为 major 2 byte string |
| `serializeBooleanArray(boolean[], WriteBuffer)` | major 4 boolean 数组 |
| `serializeShortArray(short[], WriteBuffer)` | major 4 |
| `serializeCharArray(char[], WriteBuffer)` | major 4，每个 `char` 是单字符 text |
| `serializeIntArray(int[], WriteBuffer)` | major 4 |
| `serializeLongArray(long[], WriteBuffer)` | major 4 |
| `serializeFloatArray(float[], WriteBuffer)` | major 4，元素为 float32 |
| `serializeDoubleArray(double[], WriteBuffer)` | major 4，元素为 float64 |
| `serializeCborValue(CborValue, WriteBuffer)` | 通用 CBOR 值模型 |

### 2.2 反序列化

`CborDeserializer`：

| 方法 | 说明 |
|---|---|
| `deserializeMarshallableObject(Class<T>, ReadBuffer)` | bean / record |
| `deserializeArray(Class<T>, ReadBuffer)` | 对象数组 |
| `deserializeCol(Class<T>, ReadBuffer, Supplier<Collection<T>>)` | 集合 |
| `deserializeMap(Class<K>, Class<V>, ReadBuffer, Supplier<Map<K,V>>)` | key 仅支持 `String` / `CharSequence` |
| `deserializeByteArray(ReadBuffer)` | major 2 |
| `deserializeBooleanArray(ReadBuffer)` | major 4 |
| `deserializeShortArray(ReadBuffer)` | major 4 |
| `deserializeCharArray(ReadBuffer)` | major 4 |
| `deserializeIntArray(ReadBuffer)` | major 4 |
| `deserializeLongArray(ReadBuffer)` | major 4 |
| `deserializeFloatArray(ReadBuffer)` | major 4 |
| `deserializeDoubleArray(ReadBuffer)` | major 4 |
| `deserializeCborValue(ReadBuffer)` | 通用 CBOR 值模型 |

顶层反序列化方法在返回前提交 `ReadBuffer` 的新位置，因此同一个 buffer 可以继续读取后续数据。

---

## 3. CBOR 值模型

`CborValue` 是 sealed interface，实现类全部是顶层类：

| 类 | 内容 |
|---|---|
| `CborBool(boolean)` | simple value true / false |
| `CborInteger(long)` | major 0 / 1 |
| `CborFloat(long bits, CborFloatWidth width)` | 原始 wire bits 和宽度 |
| `CborText(String)` | UTF-8 text，非 null |
| `CborBytesType(byte[])` | major 2 byte string 的值对象 |
| `CborArray(List<CborValue>)` | 数组，防御性拷贝 |
| `CborMap(List<CborMapEntry>)` | map，保持 entry 顺序 |
| `CborMapEntry(CborValue key, CborValue value)` | 非 null key/value |
| `CborNull()` | `0xf6` |
| `CborTaggedType(long tag, CborValue value)` | major 6 tag |

`CborFloatWidth`：

- `HALF`：16 位 half 位模式；
- `SINGLE`：32 位 float 位模式；
- `DOUBLE`：64 位 double 位模式。

`CborFloat` 保留原始位模式，因此 NaN payload、Infinity 符号和 ±0 不会在解码再编码时丢失。`CborFloat.ofDouble` 只接受能精确表示为目标宽度的值；需要保留任意位模式时直接使用 `new CborFloat(bits, width)`。

`CborValue` 的编码/解码都是迭代实现：

- 数组/ map / tag 通过显式栈展开；
- 不依赖 Java 递归；
- map 保持 entry 顺序，不排序。

---

## 4. CBOR 与 Java 类型映射

### 4.1 总表

| Java 类型 | CBOR 类型 | 编码要点 |
|---|---|---|
| `byte` / `Byte` | major 0 / 1 | 最短整数 head |
| `short` / `Short` | major 0 / 1 | 最短整数 head |
| `int` / `Integer` | major 0 / 1 | 最短整数 head |
| `long` / `Long` | major 0 / 1 | 最短整数 head，覆盖 64 位 |
| `boolean` / `Boolean` | major 7 | `false=0xf4`，`true=0xf5` |
| `char` / `Character` | major 3 | 单字符 text；反序列化要求 UTF-16 长度恰好 1 |
| `float` / `Float` | major 7, ai 26 | 始终 `0xfa` + float32，不缩成 half |
| `double` / `Double` | major 7, ai 27 | 始终 `0xfb` + float64 |
| `byte[]` | major 2 | byte string，不是数字数组 |
| `CborBytesType` | major 2 | byte string 的值对象 |
| `String` / `CharSequence` | major 3 | 原始 UTF-8，不转义 |
| `CborText` | major 3 | text 值对象 |
| `boolean[]` 等 Java 原生数组 | major 4 | definite-length 数组 |
| `Byte[]` 等包装类型数组 | major 4 | definite-length 数组 |
| `String[]` / `CharSequence[]` | major 4 | definite-length text 数组 |
| `List<T>` / `Collection<T>` | major 4 | `List` definite；非 `List` 的 `Collection` indefinite |
| `Map<String,V>` / `Map<CharSequence,V>` | major 5 | key 仅支持字符串 |
| bean / record | major 5 | 字段使用 `mappedName` 作为 key |
| enum | major 3 | 使用映射后的枚举名，不是整数 |
| `CborTaggedType` | major 6 | tag + 一个 `CborValue` payload |
| transformer 自定义类型 | major 6 | 只能映射到 `CborTaggedType` |
| `CborValue` | 任意 | any-CBOR-value 模型 |
| `CborNull` | major 7 | `0xf6` |

### 4.2 整数

- 所有 Java 整数类型统一使用 major 0（非负）或 major 1（负数）。
- 负数参数编码为 `-1 - value`，因此 `-1` 对应参数 `0`。
- head 使用最短表示：
  - `0..23` 直接放在 initial byte；
  - `24..255` 使用 ai 24 + 1 字节；
  - 之后按 2/4/8 字节逐级增长。
- CBOR 层不区分 `byte` / `short` / `int` / `long`；反序列化时按目标 Java 类型做范围检查，溢出抛出 `CborDeserializerException`。

### 4.3 浮点

- Java `float` 固定写 `0xfa` + float32。
- Java `double` 固定写 `0xfb` + float64。
- 不把 Java `float` 自动缩成 half；也不把 `double` 自动缩成 float32。
- `CborFloat` 可以显式指定 `HALF` / `SINGLE` / `DOUBLE`，并保留原始位模式。
- 读取：
  - half / float32 / float64 -> `double`：无损加宽；
  - half / float32 -> `float`：无损；
  - float64 -> `float`：要求 raw-bit 精确回环，否则抛出 `float value loses precision`；
  - NaN 只有能精确保持位模式时才允许缩窄；Infinity、±0 必须保持符号。
- `CborFloat.ofDouble` 对 half / single 做精确性检查；`new CborFloat(bits, width)` 直接接受原始位模式。

### 4.4 文本

- CBOR text string 是 major 3，内容是原始 UTF-8，**没有引号和转义**。
- 长度是 **UTF-8 字节长度**，不是 Java `String.length()`。
- 读取时做 UTF-8 校验，拒绝 overlong、非法 surrogate、超出 Unicode 范围的编码。
- `char` 编码为单字符 text；反序列化要求解码后的 UTF-16 长度恰好为 1，因此 emoji 不能反序列化为 `char`。
- 空字符串编码为 `0x60`。

字符串写入策略见第 5 节。

### 4.5 字节串

- `byte[]` 和 `CborBytesType` 都是 major 2 byte string，不展开为数字数组。
- 读取长度受 `maxBytesBytes` 限制。
- 反序列化 `byte[]` 时直接复制 payload。

### 4.6 数组、集合、map、对象

- 原生数组、包装类型数组、`String[]`、`List`：definite-length major 4。
- 非 `List` 的 `Collection`：indefinite-length major 4（`0x9f ... 0xff`），因为无法在不遍历的情况下预知大小。
- 对象和 map：
  - `serializeNullInObjOrMap=true`：definite-length major 5，写 null 字段/值；
  - 默认 `false`：indefinite-length major 5，省略 null 字段/值。
- map key 只支持 `String` / `CharSequence`；其他 key 类型在入口处拒绝。
- `CborMap` / `CborValue` 模型允许任意 `CborValue` 作为 key，不经过上述 Java map 限制。
- 对象字段 key 使用 `MarshallInfo.mappedNameUtf8Bytes()` 预编码字节，避免重复 UTF-8 编码。

### 4.7 tag 与 transformer

- 只有 `CborTaggedType` 是内置 tag 类型。
- transformer 的 `builtinType()` 必须是 `CborTaggedType.class`；其他 builtin 类型直接拒绝。
- 自定义类型不能是 marshallable bean。
- 内置映射优先级高于 transformer，transformer 不能覆盖内置类型。
- 反序列化时，tag 只由 `CborTaggedType`、`CborValue` 或对应 transformer 接受；普通字段遇到 tag 会抛出异常。
- `CborTaggedType` 的 payload 是 `CborValue`。

### 4.8 enum

- enum 不使用 CBOR 整数编码。
- 序列化时写映射后的枚举名字符串；没有映射时写 `enum.name()`。
- 反序列化通过枚举名查找，不做序号映射。

---

## 5. 序列化策略

### 5.1 字符串：假定 ASCII + 向量 S2B + memmove

这是实现中最关键的性能路径。

`serializeStr` 的步骤：

1. 按 `str.length()` 计算临时 head 宽度，写最短 head；
2. 记录 `dataStart`；
3. `encodeUtf8FromPosition`：
   - 空串直接 `0x60`；
   - 短字符串直接标量检查 ASCII；
   - 长字符串 `getChars` 到复用 `char[]`；
   - 对每个 `ShortVector` 做 S2B，**整块写入目标**，拷贝本身不做 mask；
   - 用 `(c & 0xFF80) != 0` 找第一个非 ASCII lane；
   - 找到后从该位置开始，用标量编码后缀到复用 `utf8Scratch`，再 `putBytes` 覆盖目标尾部；
   - 已写入的 ASCII 向量前缀全部保留；
4. 计算最终 UTF-8 字节数；
5. 如果最终字节数与临时 head 同宽：
   - 回到起点改写 head；
   - 恢复数据末尾位置；
6. 如果 head 变宽：
   - `ensureCapacity(delta)`；
   - 把已有数据 `memmove` 右移 `delta`；
   - 写正式 head；
   - 恢复末尾位置。

`memmove`：

- heap：`System.arraycopy`，支持重叠右移；
- segment：从后往前逐字节复制，避免覆盖。

示例：32 个 ASCII 字符最终 32 字节，临时 head 就是正确 head；32 个 surrogate pair 最终 64 字节，头宽度仍为 2 字节，只回填 head，不移动数据；23 个 ASCII 字符加一个中文字符最终 25 字节，head 从 1 字节变 2 字节，触发 memmove。

### 5.2 为什么不用 scratch 整串重编码

字符串头是长度前缀，最终 UTF-8 长度只有在编码后才确定。当前实现选择：

- 先写临时头；
- 直接在目标缓冲区编码；
- 最后按需回填 head / memmove。

这避免了为每个非 ASCII 字符串复制整串到 scratch，也保留了向量前缀的写入成果。

### 5.3 数组和集合

- Java 原生数组、包装类型数组、`String[]`、`List`：先写 definite head，再逐个元素写。
- 非 `List` 的 `Collection`：写 indefinite head，逐元素写，最后 `0xff`。
- 数组和 list 中的 null 元素写 `0xf6`。
- `CborValue.CborArray` 使用 definite head，元素通过显式栈编码。

### 5.4 对象和 map

- 默认模式：indefinite map，省略 null 字段/值；
- `serializeNullInObjOrMap=true`：先计算非跳过字段数/非 null key 数，写 definite map，null 值写成 `0xf6`；
- 对象字段按 `MarshallInfo` 顺序写出，key 使用预编码 UTF-8；
- map 保持迭代顺序，不排序；
- 非字符串 key 在入口处拒绝。

### 5.5 `CborValue`

`serializeCborValue` 使用显式 `Deque<CborValue>`：

- `CborBool` -> simple；
- `CborInteger` -> major 0/1；
- `CborFloat` -> 按 `width` 直接写原始 bits；
- `CborText` -> 字符串路径；
- `CborBytesType` -> major 2；
- `CborNull` -> `0xf6`；
- `CborArray` -> definite 数组；
- `CborMap` -> definite map；
- `CborTaggedType` -> major 6 + payload。

整个过程没有 Java 递归。

---

## 6. 反序列化策略

### 6.1 初始字节

每个值先读 initial byte：

- major 0/1：整数；
- major 2：byte string；
- major 3：text string；
- major 4：数组（definite / indefinite）；
- major 5：map（definite / indefinite）；
- major 6：tag；
- major 7：simple / float。

`readArgument` 处理 ai 0–23、24、25、26、27；ai 28–30 非法。`readLength` 额外把 ai 31 表示为 indefinite（`-1`）。

### 6.2 整数和浮点

- 整数读取后按目标类型做范围检查。
- float：
  - ai 25 -> half；
  - ai 26 -> float32；
  - ai 27 -> float64；
  - 读 `float` 时对 float64 做 raw-bit 精确性检查；
  - 读 `double` 时 half/float32/float64 都是无损加宽。

### 6.3 文本和字节串

- text 必须是 definite length；indefinite text 抛 `indefinite length not supported`。
- 读取 text 前做 UTF-8 校验；heap 使用 `CborUtf8Validator.validateHeap`，segment 使用 `validateSegment`。
- byte string 也必须是 definite length；直接复制 payload。
- 字符串/字节串长度分别受 `maxStringBytes` / `maxBytesBytes` 限制。

### 6.4 数组和集合

- definite 数组：读取 declared count，直接构造目标数组；
- indefinite 数组：读到 `0xff` 为止；
- 原生数组、包装类型数组、`String[]` 都走专用反序列化；
- `List`/`Collection` 通过 `initCol`，null 元素保留；
- 数组元素数量受 `maxArrayElements` 限制。

### 6.5 map 和对象

- 对象字段通过 `marshallInfoByMappedName` 查找；
- 重复 key 抛 `duplicate key`；
- 未知字段通过 dummy 节点跳过，受 `maxDummyElements` 限制；
- 缺失字段：
  - `ensureAllFieldsPresent=true` 时缺字段抛异常；
  - 原始类型字段默认也要求存在；
  - 其他字段允许缺失；
- null 字段值按“缺失”处理（除非目标类型是 `CborValue`）；
- `skipSerializing` / `skipDeserializing` 字段双向跳过；
- `Map<String,V>` 的 key 必须能解码为字符串。

### 6.6 `CborValue`

`deserializeCborValue` 使用显式 frame 栈：

- array frame：收集元素；
- map frame：等待 key / value 配对；
- tag frame：等待 payload；
- 读到 `0xff` 结束 indefinite 容器；
- 支持任意 tag；不认识的 tag 作为 `CborTaggedType` 保留。

### 6.7 UTF-8 校验

`CborUtf8Validator` 提供：

- heap / segment 的向量化校验；
- 标量 fallback；
- 拒绝 overlong、surrogate、超出 Unicode 范围的编码；
- `skipString` 和按名称查找失败后的路径也会校验 UTF-8。

### 6.8 限制

`CborDeserializerOption`：

| 限制 | 默认值 | 最小值 | 系统属性 |
|---|---:|---:|---|
| `maxStringBytes` | 65535 | 64 | `jing.marshallcbor.maxstringsize` |
| `maxBytesBytes` | 65535 | 64 | `jing.marshallcbor.maxbytesbytes` |
| `maxArrayElements` | 1000 | 100 | `jing.marshallcbor.maxarraysize` |
| `maxMapElements` | 200 | 20 | `jing.marshallcbor.maxmapsize` |
| `maxDummyElements` | 4 | 0 | 无 |
| `maxNestedSize` | 64 | 4（最大 4096） | 无 |
| `charBufferSize` | 128 | 128 | 无 |

系统属性值还会被硬上限截断；超过硬上限时取硬上限。

`CborSerializerOption`：

| 选项 | 默认值 | 说明 |
|---|---|---|
| `serializeNullInObjOrMap` | `false` | true 时写 null 字段/值并使用 definite map |
| `maxNestedSize` | 64 | 4..4096 |
| `setTransformerClasses` | 空 | 只能映射到 `CborTaggedType` |

---

## 7. 异常

| 异常 | 场景 |
|---|---|
| `CborSerializerException` | 非法参数、类型不可序列化、嵌套超限、字符串非法 surrogate、浮点不精确 |
| `CborDeserializerException` | 非法 initial byte、长度超限、整数溢出、UTF-8 非法、缺字段、重复 key、嵌套超限 |

异常消息统一以小写字母开头。

---

## 8. 实现结构

| 类 | 职责 |
|---|---|
| `CborSerializer` / `CborDeserializer` | 公开 API 与入口校验 |
| `CborSerializerOption` / `CborDeserializerOption` | 选项、transformer、限制 |
| `CborSerializerContext` | 值编码、字符串策略、容器 head、heap/segment 写入 |
| `CborDeserializerContext` | 值解码、UTF-8、整数/浮点、容器 head、heap/segment 读取 |
| `CborSerializerNode` / `CborDeserializerNode` | 显式栈状态机，处理对象/数组/集合/map |
| `CborValue` 及实现类 | 通用 CBOR 值模型 |
| `CborNumberUtil` | major/ai 分解、half 转换 |
| `CborUtf8Validator` | UTF-8 向量化/标量校验 |
| `CborBytesType` / `CborTaggedType` | major 2 / major 6 值对象 |

序列化和反序列化都支持 heap `byte[]` 与 `MemorySegment` 两种 buffer。

---

## 9. 与 Jackson CBOR 的差异

对比对象：`jackson-dataformat-cbor` 3.2.2 的 `CBORGenerator` / `CBORParser`。

### 9.1 字符串 head

Jackson：

- 按字符数分桶预留 head：
  - `≤23` 字符预留 1 字节；
  - `≤255` 字符预留 2 字节；
  - `≤ MAX_LONG_STRING_CHARS`（默认输出 buffer 16000 字节时约 3996 字符）预留 3 字节；
- 数据直接编码在预留 head 之后；
- head 不够宽时用 `System.arraycopy` 右移 1–2 字节；
- 更长的字符串使用 indefinite-length chunks（`0x7f ... 0xff`），每块带自己的长度。

jing：

- 按 `String.length()` 写最短临时 head；
- 向量 S2B 直接写目标；
- 非 ASCII 后标量编码后缀并批量覆写；
- 最终长度与临时 head 同宽时原地回填；变宽时 memmove；
- 不输出 indefinite text chunks，超长字符串使用 definite length 和更大的 head（5/9 字节）。

### 9.2 字符串编码

Jackson：

- 标量 `_encode` 先扫 ASCII，遇到第一个非 ASCII 后走多字节标量路径；
- 不使用 Vector API。

jing：

- ASCII 使用 `ShortVector` + `ByteVector` 向量 S2B；
- 非 ASCII 从第一个非 ASCII lane 开始标量编码后缀；
- 这是 jing 在 StringArray ascii / Twi 等场景领先的主要原因。

### 9.3 其他差异

| 方面 | Jackson CBOR | jing CBOR |
|---|---|---|
| 字符串引用 | 可选 tag 256 | 未实现 |
| 超长字符串 | indefinite chunks | definite + 大 head |
| transformer | 无此概念 | 自定义类型 ↔ `CborTaggedType` |
| map key | 支持更多 key 类型 | 仅 `String` / `CharSequence` |
| null 省略 | 由 mapper 配置 | `serializeNullInObjOrMap` |
| 浮点宽度 | 可按配置/最短宽度 | Java float/double 固定宽度 |
| 容器不定长 | 支持 | 对象/非 List 集合默认不定长 |

---

## 10. 基准

基准位于 `jing-bench` 的 `io.jingproject.bench.marshallcbor` 包：

- `IntegerSerializationBench` / `IntegerDeserializationBench`
- `MapSerializationBench` / `MapDeserializationBench`
- `StringArraySerializationBench` / `StringArrayDeserializationBench`
- `StringBeanSerializationBench` / `StringBeanDeserializationBench`
- `TwiSerializationBench` / `TwiDeserializationBench`

数据：

- Integer：64 个 `NumberEntity`；
- Map：1000 个 entry；
- StringArray/StringBean：1000 个元素，size=32；
- Twi：`twitter.json` 完整对象；
- 反序列化双方共用同一份 CBOR 字节，避免输入差异。

最近一次 CBOR vs Jackson CBOR 结果（JDK 28-ea，JMH 1.37，1 fork，warmup/measurement 见结果文件）：

| 场景 | Jackson | jing | 结果 |
|---|---:|---:|---|
| Integer 序列化 | 132.2 ns | 48.5 ns | jing 快 2.73× |
| Map 序列化 | 14,104 ns | 12,514 ns | jing 快 1.13× |
| StringArray ascii 序列化 | 19,483 ns | 10,118 ns | jing 快 1.93× |
| StringArray surr 序列化 | 38,280 ns | 43,083 ns | jing 慢约 13% |
| StringBean ascii 序列化 | 132.7 ns | 67.2 ns | jing 快 1.98× |
| StringBean surr 序列化 | 168.9 ns | 123.5 ns | jing 快 1.37× |
| Twi 序列化 | 369.6 µs | 299.3 µs | jing 快 1.23× |
| Integer 反序列化 | 219.7 ns | 53.8 ns | jing 快 4.08× |
| Map 反序列化 | 27,408 ns | 26,126 ns | jing 快 1.05× |
| StringArray ascii 反序列化 | 27,013 ns | 15,270 ns | jing 快 1.77× |
| StringArray surr 反序列化 | 67,474 ns | 79,286 ns | jing 慢 1.18× |
| StringBean ascii 反序列化 | 301.5 ns | 62.9 ns | jing 快 4.80× |
| StringBean surr 反序列化 | 432.2 ns | 206.2 ns | jing 快 2.10× |
| Twi 反序列化 | 1,018.6 µs | 612.8 µs | jing 快 1.66× |

结论：

- 序列化 6/7 场景明显快于 Jackson；`StringArray surr` 约 13% 慢；
- 反序列化 6/7 场景快于 Jackson，`StringArray surr` 慢约 18%；
- Integer、Map、StringBean、Twi 是主要优势场景。

---

## 11. 测试

测试类：

- `CborSerializationTest`
- `CborDeserializationTest`
- `CborStringSerializationTest`
- `CborWriteIntegerTest`
- `CborReadIntegerTest`
- `CborWriteFloatTest`
- `CborReadFloatTest`
- `CborUtf8ValidationTest`

覆盖：

- 整数边界和最短 head；
- 浮点 half/float32/float64、NaN、Infinity、±0、精确缩窄；
- UTF-8 合法/非法编码；
- 字符串 ASCII、中文、emoji、surrogate、head 宽度增长、Segment 路径；
- byte string；
- 数组、集合、map、null；
- bean / record / 递归对象；
- `CborValue` 环回；
- `CborTaggedType` / `BigDecimalTransformer`；
- 未知字段、重复 key、缺字段、限制。

运行：

```text
mvn -o -pl jing-marshall-cbor -am clean test
```

当前：**108 个测试，0 failures / 0 errors**。

---

## 12. 参考资料

- RFC 8949: Concise Binary Object Representation (CBOR)
- RFC 8949 Appendix A: Examples
- `jing-marshall` 框架文档：[Marshall.md](./Marshall.md)
- Jackson CBOR 3.2.2 `CBORGenerator` / `CBORParser`
