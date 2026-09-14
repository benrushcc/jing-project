# jing-marshall-cbor 模块设计文档

> 目标：在 jing-marshall 序列化框架基础上，仿照 `jing-marshall-json` 模块的写法，新增一个
> **CBOR（RFC 8949）二进制序列化格式**模块。本文档只做设计，不涉及实现代码。
>
> 状态：设计稿 v0.2（已依据 jing-marshall-json 侦察报告补齐各 TODO 章节，附 CBOR 规范对照表）
> 关联文档：[Marshall.md](./Marshall.md)（marshall 框架总体设计思路）

---

## 1. 概述

### 1.1 背景

jing-marshall 是一个"编译期元数据 + 运行期格式无关序列化"的框架：

- 编译期由注解处理器（`jing-marshall-processor`）为被 `@Marshallable` 标注的类型生成
  门面类（`MarshallFacade` 实现），以 SPI 方式注册，运行期经 `Marshalls` 静态注册中心
  按类型检索。
- 运行期的具体格式模块（当前仅有 `jing-marshall-json`）只消费门面 API，**不实现任何
  接口、不注册任何 provider**，因此新增格式模块对既有框架是"零侵入"的。

`doc/Marshall.md` 中明确写到"包含 json 和 cbor 的实现"，即 CBOR 从框架设计之初就是
规划内的内置格式之一。本项目构建本模块为 `jing-marshall-cbor`。

### 1.2 目标

1. 提供与 `jing-marshall-json` 对称的公开 API：序列化 / 反序列化入口、选项体系、
   异常体系、转换器（transformer）集成。
2. 完整实现 CBOR 数据模型（RFC 8949）：major type 0–7、整数最短编码、字节串/文本串、
   数组/映射、标签（tag）、简单值、浮点数（float16/32/64）。
3. 与 jing-marshall 门面体系深度契合：`readXxx(instance, index)` 序列化 /
   `newBuilder() → writeXxx(index, value) → construct(builder)` 反序列化、
   `MarshallInfo` 类型分派（`type & TYPE_MASK`）、零分配键名查找
   （`marshallInfoByMappedName(byte[],from,to)` + 哈希比对）。
4. 复用全部注解与转换器管道：`@Marshallable` / `@MarshallAttr` / `@Transformable`
   无需任何改动即可在 CBOR 中生效。
5. 保持项目的性能契约：结构化访问走 VarHandle 生成的 `readXxx/writeXxx`，键名比对走
   紧凑字节 + 哈希的零分配路径；CBOR 输出使用"最短表示（preferred serialization）"。
6. 不引入任何第三方依赖（遵循项目全局约束：除 `jing-bench` 外不得有第三方依赖）。

### 1.3 非目标

- 不实现流式/增量编码的完整 push/pull 抽象（如需网络流式场景，可后续在内部建立
  增量写入器，本设计先做一次性 byte[] 转换）。
- 不做 CBOR 与 JSON 的数据模型自动互转模块（可用各自 transformer 手工适配）。
- 不把 `jing-marshall` 的核心类型系统扩展为支持更多内建类型（如 Instant 等）；新类型
  一律走 transformer 管道。（详见 §9 风险）。
- 不实现 CBOR 映射键排序之外的杂项扩展（如 duplicated keys 的宽松模式，默认严格）。

---

## 2. jing-marshall 核心设计回顾（新格式模块的对接基础）

> 本节内容来自对 `jing-marshall` 源码的实际侦察。这是 CBOR 模块设计的"契约来源"。

### 2.1 编译期元数据 → 运行期门面

被 `@Marshallable` 标注的 class / record / enum，由 `MarshallProcessor` 生成
`XxxMarshallFacade implements MarshallFacade`（`@Provider(target = MarshallFacade.class)`、
`final`、带 `AtomicBoolean GUARD` 单例闸门）。`Marshalls` 静态块用
`ServiceLoader` 加载全部 `MarshallFacade` 与 `MarshallTransformerFacade`，
按 `marshallableType()` 分桶缓存，重复注册抛 `ExceptionInInitializerError`。

格式模块（如 json / cbor）通过以下查询接入：

```java
MarshallFacade fc = Marshalls.beanMarshallFacade(clazz);          // bean/record/继承链
MarshallFacade efc = Marshalls.enumMarshallFacade(enumType);       // enum
MarshallInfo inf = Marshalls.enumItemMarshallInfo(enumValue);      // enum 项元数据
MarshallTransformerFacade tfc = Marshalls.marshallTransformerFacade(transformerClass);
```

### 2.2 MarshallFacade 方法族（格式模块依赖的全部契约）

1. **元数据族**：`marshallableType()`、`marshallInfos()`、`totalElements()`、
   `primitiveElements()`、`marshallInfoByFieldName(...)` / `marshallInfoByMappedName(...)`
   （各自提供 `String`、`byte[] 切片`、`MemorySegment 切片` 三种形态 + Charset 便捷重载）。
2. **读取族（序列化方向）**：`readBoolean/readByte/readShort/readChar/readInt/readLong/
   readFloat/readDouble(Object instance, int index)` + `readObject(Object, int)`，仅实现对
   实际存在字段类型的覆盖。
3. **构建族（反序列化方向）**：`newBuilder()` → `MarshallBuilder.writeXxx(index, value)`
   → `construct(builder)`。（enum 门面无构建族，默认方法抛 `UnsupportedOperationException`。）

```java
public interface MarshallFacade {
    Class<?> marshallableType();
    List<MarshallInfo> marshallInfos();
    default int totalElements()
    default int primitiveElements()
    MarshallInfo marshallInfoByFieldName(String fieldName);
    MarshallInfo marshallInfoByFieldName(byte[] bytes, int from, int to);
    MarshallInfo marshallInfoByFieldName(MemorySegment segment, long from, long to);
    // ... 同名 mappedName 三形态 + Charset/范围校验便捷重载（default）

    default boolean readBoolean(Object instance, int index) { throw new UnsupportedOperationException(); }
    // ... readByte/readShort/readChar/readInt/readLong/readFloat/readDouble/readObject 同构

    default MarshallBuilder newBuilder() { throw new UnsupportedOperationException(); }
    default Object construct(MarshallBuilder builder) { throw new UnsupportedOperationException(); }
}
```

### 2.3 MarshallInfo：字段级元数据（CBOR 类型分派的依据）

```java
@ProcessorApi
public record MarshallInfo(
    Class<?> rawType,
    Class<?> firstGenericType,   // Map<K,V> 的 K / List<T> 的 T；无则 null
    Class<?> secondGenericType,  // Map<K,V> 的 V；无则 null
    int index,                   // 字段序号，= Builder/Reader 存取下标
    String fieldName,
    byte[] fieldNameUtf8Bytes,
    String mappedName,           // 序列化映射名（NamingConvention 转换或 @MarshallAttr 覆盖后）
    byte[] mappedNameUtf8Bytes,
    byte type,                   // MarshallUtil 类型编码 0..50
    byte flags                   // 见下
) {
    public boolean fieldNameSimple();   // flags & 1
    public boolean mappedNameSimple();  // flags & 2
    public boolean skipSerializing();   // flags & 4
    public boolean skipDeserializing(); // flags & 8
}
```

### 2.4 MarshallUtil 类型编码表（0–50，`TYPE_SIZE=64`）

| 区间 | 取值 | 含义 |
|---|---|---|
| 0–7 | BYTE..DOUBLE | 8 种原始类型 |
| 8–15 | *\_WRAPPER | 8 种包装类型 |
| 16–23 / 24–31 | BYTE_ARRAY..DOUBLE_ARRAY / BYTE_WRAPPER_ARRAY..DOUBLE_WRAPPER_ARRAY | 原始/包装数组 |
| 32 | RAW_TYPE | 其他任意类型（对象形态） |
| 33 | ARRAY_TYPE | 引用类型数组 |
| 34 | ENUM_TYPE | 枚举 |
| 35–44 | CHARSEQUENCE, STRING, UUID, BIG_INTEGER, BIG_DECIMAL, LOCAL_DATE, LOCAL_TIME, LOCAL_DATE_TIME, OFFSET_TIME, OFFSET_DATE_TIME | 内建对象类型 |
| 45–47 | ONE_GENERIC, COLLECTION_INTERFACE, COLLECTION_IMPL | 单泛型 / 集合 |
| 48–50 | TWO_GENERIC, MAP_INTERFACE, MAP_IMPL | 双泛型 / 映射 |

- `type & TYPE_MASK` 后分派：`<= DOUBLE_TYPE(7)` 走基元直读；数组区间走数组处理；
  `ENUM_TYPE` 走枚举；35–44 走内建对象；集合/映射走容器处理。
- 容器实例化用 `MarshallUtil.newCollectionInterface/newMapImpl` 六工厂（java.util 接口
  与常见实现）。Map 键的约束：**序列化时 K 必须为 String/CharSequence**（JSON 硬性要求，
  CBOR 同样遵循，映射键统一用文本串）。
- 反序列化缺失字段检查：默认只强制原始类型字段齐全（`primitiveElements()`），选项
  `ensureAllFieldsPresent` 开启后强制 `totalElements()`。CBOR 需复刻
  "位图去重 + 收尾缺失检查" 契约。

### 2.5 转换器管道（transformer）

用户类型 `X` ↔ 内建类型 `Y` 的互转由 `MarshallTransformer<X, Y>` + `@Transformable`
声明，`MarshallTransformerProcessor` 生成门面 `MarshallTransformerFacade`（含
`customType()/builtinType()/toCustom(Object)/toBuiltin(Object)` 及数组批量方法）。
格式模块的约束：**`builtinType` 必须实现本格式的 primitive 接口族**（对 JSON 是
`JsonPrimitiveType` 及其子类 `JsonBoolType/JsonNumberType/JsonStrType`）。CBOR 模块需要
定义自己的 primitive 类型族（见 §5.4）。

管道是两层的：`custom → toBuiltin() → 格式 primitive 值`；数组走 `toBuiltinArray`。

### 2.6 命名与顺序

- `mappedName` 计算：`@MarshallAttr.mappedName` 非空则直接采用（完全跳过转换）；否则
  若 `@Marshallable.from/to` 均非 ORIGINAL 则经 `NamingConvention.cast(from,to,name)`。
- `index` 贯穿三方契约：`MarshallInfo.index` = Builder 写下标 = Facade 读下标 =
  enum 常量在 `values()` 中的次序。
- 字段展平顺序：祖先 → 后代，每层按声明顺序（`ExtendEntity` 验证：父 4 + 子 2 = 6）。
- 上限 65535 字段。

### 2.7 注解处理器代码生成机制（jing-marshall-processor）

> 本节内容来自对 `jing-marshall-processor` 源码的完整侦察。

**两个处理器，各认一个注解**（均继承 `AbstractProcessor`，
`SourceVersion.latestSupported()`，`process()` 返回 `true` 独占声明）：

| 处理器 | 触发注解 | 产出 | SPI 注册 |
|---|---|---|---|
| `MarshallProcessor` | `@Marshallable` | `_X$$MarshallFacade implements MarshallFacade` | `@Provider(target = MarshallFacade.class)` |
| `MarshallTransformerProcessor` | `@Transformable` | `_X$$MarshallTransformerFacade implements MarshallTransformerFacade` | `@Provider(target = MarshallTransformerFacade.class)` |

**编译期信息模型**（5 个 record，仅编译期存在，不落盘）：

- `MarshallFieldInfo`：每字段一条，含 `marshallIndex`（全局 0 基序号）、
  `fieldNameOffset/mappedNameOffset`（名字 UTF-8 字节在紧凑数组中的偏移）、
  `fieldNameUtf8Bytes/mappedNameUtf8Bytes`、skip 标志、所属继承链 typeIndex。
- `MarshallTypeInfo`：按字段类型分桶（原始类型归入对应 primitive 类、引用类型统一
  `Object.class`），生成 `readX/writeX` 方法时保证**零装箱**。
- `MarshallSwitchInfo`：选定哈希器后同哈希的名字归一组，生成
  `case hash -> { if (equals) return ...; }` 冲突链。
- `MarshallProcessorInfo`：全量聚合（继承链、字段、类型桶、两套哈希选择与冲突组）。
- `MarshallTransformerInfo`：转换器类 + `MarshallTransformer<Custom,Builtin>` 两个类型实参。

**生成流程与命名**：`GeneratorSource`（jing-common-processor 的行式块模型，4 空格
缩进、import 排序管理、字符串字面量转义）→ 命名规则 `Utils.generateClassName`
→ `_<简单名>$$MarshallFacade`，与被注解类**同包同模块**，经
`Filer.createSourceFile("module/pkg._X$$Y")` 落到 `target/generated-sources/annotations`。
生成类结构固定为：

```java
@Provider(target = MarshallFacade.class)
@Generated
public final class _BeanEntity$$MarshallFacade implements MarshallFacade {
    private static final List<VarHandle> VHS;      // 仅 CLASS 场景
    private static final AtomicBoolean GUARD = new AtomicBoolean(false);
    private static final List<MarshallInfo> MARSHALL_INFOS;
    private static final MarshallHashInfo HASH_INFO;

    static {  // privateLookupIn 逐层（继承链每层一个 lookup）+ findVarHandle 每个字段
        ...
        // MarshallInfo 7 参便捷构造 + new MarshallHashInfo(INFOS, fieldHashIndex, mappedHashIndex)
    }
    // 构造器：GUARD.compareAndSet(false, true) 失败抛 IllegalStateException（单例闸门）
    // 6 个 marshallInfoBy*(String/byte[],int,int/MemorySegment) + 类型桶 readX + newBuilder/construct + 内嵌 Builder(record)
}
```

- **CLASS**：字段读写走 `VHS.get(i).get/set(instance, value)`（VarHandle，含私有继承字段，
  每层 `privateLookupIn(X.class, lookup)`）；`newBuilder()` 预 new 目标类，
  `construct` 用 record 解构 `writer instanceof Builder(BeanEntity instance)` 取回实例。
- **RECORD**：reader 直接调访问器；Builder 为私有静态类攒字段后 `build()` 调规范构造。
- **ENUM**：只生成元数据 + 6 个名字查找，无 readX/newBuilder/construct。
- 校验规则：class 非 abstract + 全继承链 `@Marshallable` + public 无参构造 + 字段非
  static/final + 字段泛型 ≤2；record ≥1 组件；enum public 非空。`AnnoUtil` 硬性约束
  （顶层、public、禁多维数组等）。失败抛 `AnnotationProcessorException` 中断编译。
- 生成代码风格：零注释、强制大括号、无 import static、`@SuppressWarnings("unchecked")`
  前置、异常消息小写开头、防溢出用 `Math.incrementExact/addExact`、无递归
  （Transformer 超类扫描用 `ArrayDeque` 迭代）。

**对 CBOR 模块的关键结论**：processor 侧**完全格式无关**——不需要新 processor、不需要
新注解。json 模块没有任何 `*Processor` 就是证明。CBOR 模块主代码只做两件事：
(1) 运行期消费 `Marshalls.*` 与门面 API；(2) 测试期挂
`annotationProcessorPaths` + `proc:full` + `jing-maven-plugin(scope=test)` 让测试实体
走完整 SPI 注册链。

---

## 3. SPI 与模块接线机制

### 3.1 全链路

1. 用户类标 `@Marshallable` / `@Transformable` → `jing-marshall-processor` 生成
   `_X$$MarshallFacade` / `_X$$MarshallTransformerFacade`（标注 `@Provider(target=...)`）。
2. `jing-common-processor` 的 `ProviderProcessor` 收集本模块所有 `@Provider` 类
   （要求 final、public、顶层；`target()` 必须是 interface），在 `processingOver()` 轮
   写出 `jing-providers.json` 到 `CLASS_OUTPUT` 根：
   ```json
   {
     "io.jingproject.marshall.MarshallFacade": [
       "io.jingproject.marshalljsontest.entity._BeanEntity$$MarshallFacade"
     ]
   }
   ```
3. `jing-maven-plugin` 的 `process-jing-providers` goal（`process-classes` /
   `process-test-classes` 阶段）：
   - 解析 `jing-providers.json` → 为每个 `<接口, 实现类集合>` 生成
     `META-INF/services/<接口全名>` 文件；
   - 使用 `java.lang.classfile` API 改写 `module-info.class`，注入
     `provides <接口> with <实现类>` 指令；
   - 完成后将 json 重命名为 `jing-providers-consumed.json`。
4. 运行期 `Marshalls` 静态块 `ServiceLoader.load` 加载两套 SPI 并分桶缓存
   （`jing.marshall` 模块 `module-info` 已预声明 `uses`，消费者无需再写）。
5. `jing-marshall-processor` 自身是 CP 双通道：`module-info provides
   javax.annotation.processing.Processor`（JPMS 侧）+ `META-INF/services/...`
   （classpath 侧）；消费模块的 pom 用 `annotationProcessorPaths` 显式挂载。

### 3.2 对 CBOR 模块的要求

- CBOR 模块 **不产生 provider、不声明 `provides`**；序列化器只经 `Marshalls` 静态方法
  查询门面。（若未来需要"格式工厂类"自身的 SPI，可用 `@Provider` + `provides/uses`
  走同一管线。）
- 与 json 一致：测试期 pom 接线 `annotationProcessorPaths` →
  `jing-marshall-processor`、`proc:full`（主编译保持 `proc:none`）、
  `jing-maven-plugin` 以 `scope=test` 挂 `process-test-classes`。

---

## 4. CBOR（RFC 8949）技术基础

> 本模块设计所依赖的编码规则（initial byte、additional information、简单值、标签、
> preferred / deterministic encoding）在 §4.3 完整给出。

### 4.1 CBOR 是什么（简述）

CBOR（Concise Binary Object Representation，RFC 8949，2020-12）是基于
RFC 7049 的数据格式，设计目标：极小的编码开销 + 较强的可扩展性 + 无需 schema 即可
互操作。与 JSON 的主要差异：

- **二进制编码**：没有文本语法，空格/逗号/分号等分隔符全部消失。
- **显式长度**：数组/映射/字符串都带字节长度前缀，无需"读到结尾才知道截止"。
- **原生字节串**：major type 2 直接承载二进制数据（JSON 只能 base64 字符串）。
- **更宽的整数**：无符号 0..2^64-1、有符号 -2^64..2^64-1（JSON 的 Number 语义边界
  更模糊）。
- **标签（tag）**：major type 6 前置标识，可为任意类型附加语义（日期、大整数、
  decimal fraction 等）。
- **确定性编码**：同一种数据可有多种编码；确定性规则给出互操作时的唯一规范。

### 4.2 类型映射总体思路（对照 json）

JSON 的 7 类数据 → CBOR 的直接对应：

| JSON 语义 | CBOR major type |
|---|---|
| null / 空值 | simple value 0xf6 (null)；"undefined" 0xf7 不用 |
| bool | simple value 0xf4 (false) / 0xf5 (true) |
| 整数 | major 0（非负）/ major 1（负） |
| 浮点数 | major 7 + float16/32/64 |
| 字符串 | major 3（文本串，必为合法 UTF-8） |
| 数组 | major 4 |
| 对象 | major 5（映射，键为文本串，遵循排序规则） |

### 4.3 initial byte 与 additional information 编码规则

每个数据项（data item）都以一个 initial byte 开头：

```
  bit 7 6 5 | 4 3 2 1 0
  major type | additional information
```

高 3 位为 major type（0–7），低 5 位为 additional information（0–31）：

| major | 含义 | 负载 |
|---|---|---|
| 0 | 无符号整数 | 见 additional information 规则 |
| 1 | 负整数（值为 -1-n，n 按下方规则编码） | 见 additional information 规则 |
| 2 | 字节串（byte string） | 长度 + 原始字节 |
| 3 | 文本串（text string，必须是合法 UTF-8） | 长度 + UTF-8 字节 |
| 4 | 数组（array） | 长度 + 各元素数据项 |
| 5 | 映射（map） | 长度 + 交替的键/值数据项 |
| 6 | 标签（tag） | 标签号 + 一个被标记的数据项 |
| 7 | 简单值 / 浮点数 | 见 simple value 表 |

**additional information → 数值/长度负载的统一规则**（整数、字节串长度、容器长度一致）：

| AI | 负载 |
|---|---|
| 0–23 | 无负载，AI 本身即数值（最短形式） |
| 24 | 后随 1 字节 uint8（大端） |
| 25 | 后随 2 字节 uint16（大端） |
| 26 | 后随 4 字节 uint32（大端） |
| 27 | 后随 8 字节 uint64（大端） |
| 28–30 | 保留，禁止使用 |
| 31 | 不定长（major 2/3/4/5），以 0xff break 数据项结束 |

**major 7 的 simple value / 浮点数**：

| initial byte | 含义 |
|---|---|
| 0xf4 | false |
| 0xf5 | true |
| 0xf6 | null |
| 0xf7 | undefined（不用） |
| 0xf8–0xff | 保留（不要使用） |
| 0xf9 xx xx | float16（半精度） |
| 0xfa xx xx xx xx | float32（单精度） |
| 0xfb xx xx xx xx xx xx xx xx | float64（双精度） |
| 0xe0–0xf3 | simple value 0–19（不常用） |

**标准标签目录（major 6，与本项目相关的部分）**：

| tag | 含义 |
|---|---|
| 0 | 基于文本的日期时间（RFC 3339 文本串） |
| 1 | 基于纪元的日期时间（数值） |
| 2 / 3 | 无符号 / 有符号任意精度大整数（bignum） |
| 4 / 5 | decimal fraction / bigfloat |
| 21–23 | 期望后续转换为 base64 / hex 等（仅提示性） |
| 32 / 33 / 34 | URI / base64url / base64（仅提示性） |
| 38 | UUID |
| 55799 | 自描述 CBOR 头（可选） |

**preferred serialization（RFC 8949 §4.2.1，本项目默认）**：

1. 整数与长度一律取"最短能装下"的 additional information（AI 0–23 直写，否则按
   24/25/26/27 递进），无冗余前导零。
2. 浮点数：float 写 float32、double 写 float64；float 可无损转为 float16 时可写
   half（v1 可不做）。
3. 不做映射键重排（不强制键排序）。

**deterministic encoding（RFC 8949 §4.2.3 core deterministic，选项开启）**：

1. 在最短表示基础上编码整数与长度。
2. 映射键按"字节长度优先、再按字节字典序"排序；项目 v1 中对象字段按
   `MarshallInfo` 顺序写入，deterministic 模式下需先对 mappedName 排序。
3. 数组/映射一律用定长形式（不定长与定长不等价，deterministic 要求定长）。

---

## 5. jing-marshall-json 现有实现分析（CBOR 的对照样板）

> 本节是 `jing-marshall-json` 主源码（21 个文件）+ 测试源码的完整侦察结论，
> CBOR 模块的结构与流程将逐一与之对齐。

### 5.1 类清单与职责

主源码位于 `jing-marshall-json/src/main/java/io/jingproject/marshalljson`：

| 类 | 职责 |
|---|---|
| `JsonSerializer` | 序列化入口：12 个公开方法（8 个原生数组 + marshallable 对象 + 对象数组 + 泛型 collection/map），持有 `JsonSerializerOption`，`INITIAL_SIZE=4` / `MAX_SIZE=4096` |
| `JsonSerializerOption` | 序列化选项（final + Builder），含 `setTransformerClasses` 全量校验 |
| `JsonSerializeResult` | 枚举：Continue/Finished/NewMarshallable/NewArray/NewCollection/NewMap |
| `JsonSerializeFunc` | 序列化函数式接口（分派表元素） |
| `JsonSerializerContext` | sealed 抽象类，Heap/Segment 双实现。字节级写出原语、类型标签写出（`serializeJsonBoolType/JsonNumberType/JsonStrType`）、字符串转义、数值原语、`valueSerializeFunc` 类型分派、`commit()` 一次性回写 |
| `JsonSerializerNode` | 显式栈节点（OBJ/ARR/COL/LIST/MAP 状态机），`process(context)` 返回 `JsonSerializeResult` |
| `JsonDeserializer` | 反序列化入口：12 个公开方法（8 个原生数组 + 对象 + 数组 + 集合 + 映射），持有 `JsonDeserializerOption` |
| `JsonDeserializerOption` | 反序列化选项（Builder），限额经系统属性 `jing.marshalljson.max*` 决定上限 |
| `JsonDeserializeResult` | 枚举：Continue/Finish/NewMarshallable/NewArr/NewCol/NewMap/NewDummyObj/NewDummyCol |
| `JsonDeserializeFunc` | 反序列化函数式接口 |
| `JsonDeserializerContext` | sealed 抽象类，Heap/Segment 双实现。`validate()`（整段 UTF-8 校验）、`nextValuableByte()`（跳空白）、firstByte 分派、`deserializeJson*Type`、键名哈希查找、BITMAP 去重/缺失检查、读对象 |
| `JsonDeserializerNode` | 显式栈节点，含 DUMMY_OBJ/DUMMY_COL 未知字段平衡跳读 |
| `JsonPrimitiveType` | sealed interface，permits `JsonBoolType`/`JsonNumberType`/`JsonStrType`（均 value record；`JsonNumberType` 承载数字字面字节 `byte[]`） |
| `JsonNumberUtil` | 数值工具：整数两位一读 LUT、浮点采用 Russ Cox uScale 算法（float 最短 15 字符、double 24）、溢出检查 |
| `Utf8Validator` | 整段 UTF-8 校验（向量 + 标量双实现，向量宽度由 `jing.marshalljson.serialize.vecsize` 控制） |
| `JsonIndentationLevel` | NONE/TWO/FOUR 三档缩进 |
| `JsonSerializerException` / `JsonDeserializerException` | `extends RuntimeException`，仅 `(String message)` 构造 |

### 5.2 关键机制（CBOR 需逐一复刻）

1. **格式模块不 implements MarshallFacade**：不实现门面接口、不注册 provider；只经
   `Marshalls.beanMarshallFacade(Class)` 拉取 facade 驱动读写。
2. **序列化驱动**：`fc.readInt/readObject(instance, index)` + `marshallInfos()`；
   **反序列化驱动**：`fc.newBuilder()` → `MarshallBuilder.writeXxx(index, value)` →
   `fc.construct(builder)`。
3. **显式栈代替递归**：`process` 用 `JsonSerializerNode[]` 栈（容量 4 起倍增），
   `nextNode` 按 `process(context)` 返回的 Result 分派；深度上限 `maxNestedSize`
   （默认 64，范围 [4, 4096]）；不检测循环引用，对象引用环由深度上限兜底。
4. **序列化直写字节**：不经 String/StringBuilder，直接写 `WriteBuffer`
   （`byte[]` Heap / `MemorySegment` Segment 双实现）；字符串先算 UTF-8 字节长度、
   按需转义，`commit()` 一次性回写。
5. **反序列化两步走**：先 `validate()` 全量校验 UTF-8（失败抛
   `"not valid utf-8 content"`）→ `nextValuableByte()` 跳过空白 → firstByte 分派
   （`t`/`f`→bool、`"`→str、`n`→null、`{`/`[`→容器、其余→数字）。
6. **值类型优先级**（与 Marshall.md §json 一致）：内建原始类型不可覆盖 → 数组/包装 →
   `JsonPrimitiveType` → 用户 transformer → 枚举（name/mappedName 文本）→
   Marshallable 对象。
7. **类型分派**：基元走 `FUNC_TABLE[type]` 直写；引用类型走
   `context.valueSerializeFunc(type)` 查 `customFuncMap`/`customArrFuncMap`（HashMap
   查表，非 bitset）。
8. **对象反序列化**：`fc.marshallInfoByMappedName(byte[], from, to)` 零分配键名查找
   （`MarshallInfo` 两套哈希组），BITMAP 防重复键/收尾检查；`ensureAllFieldsPresent`
   开启后强制 `totalElements()` 齐全（默认只强制原始类型字段）；未知字段走
   DUMMY_OBJ/DUMMY_COL 平衡跳读（`maxDummyElements` 默认 4）。
9. **限额体系**：序列化侧 `maxNestedSize`；反序列化侧
   `maxEmptyBytes/maxNumberBytes/maxStringBytes/maxArrayElements/maxMapElements/
   maxDummyElements/maxNestedSize/charBufferSize`，Builder 越界抛
   `IllegalArgumentException`。
10. **异常消息**：小写字母开头（项目规范），如 `"arr must not be null"`、
    `"exceeded maximum nested size : 64"`、`"type not marshallable : io.X"`。

### 5.3 测试覆盖

`src/test/java/io/jingproject/marshalljsontest/` 下 8 个测试类：
WriteIntegerTest / ReadIntegerTest / WriteFloatTest / ReadFloatTest /
Utf8ValidationTest / StringSerializationTest / JsonSerializationTest /
JsonDeserializationTest，配合 `entity/`（BeanEntity、RecordEntity、EnumEntity、
RecursiveEntity）与 `transformers/BigDecimalTransformer`。CBOR 的测试矩阵见 §7。

---

## 6. jing-marshall-cbor 模块设计

### 6.1 工程骨架

新建 Maven 子模块 `jing-marshall-cbor`，在根 `pom.xml` 的 `<modules>` 中登记在
`jing-marshall-json` 之后、`jing-bench` 之前：

```xml
<module>jing-marshall-cbor</module>
```

父 pom 继承、`groupId=io.jingproject`、`artifactId=jing-marshall-cbor`、
`version=${revision}`。依赖项：

| 依赖 | 作用域 | 说明 |
|---|---|---|
| `jing-common` | compile | 传递性（经 jing-marshall） |
| `jing-marshall` | compile | 门面 API 全部来源 |
| `jing-marshall-processor` | test | 仅测试期参与注解处理（`proc=full` + `annotationProcessorPaths`） |
| `junit-jupiter` | test | 测试框架 |
| `jing-maven-plugin` | build | `test` 作用域下处理测试期 provider 描述符 |

**`module-info.java`（逐行镜像 json 的结构，已对照 `jing-marshall-json` 原文件核实）**：

```java
module jing.marshall.cbor {
    requires transitive jing.common;
    requires transitive jing.marshall;
    requires jdk.incubator.vector;

    exports io.jingproject.marshallcbor;
}
```

### 6.2 包结构

```
io.jingproject.marshallcbor
├── CborSerializer           // 序列化入口（12 个公开方法，逐一对齐 JsonSerializer）
├── CborSerializerOption     // 序列化选项（Builder 模式，含 setTransformerClasses 校验）
├── CborSerializerException  // 序列化异常（extends RuntimeException，仅 String 构造）
├── CborSerializerNode       // 显式栈节点（OBJ/ARR/COL/LIST/MAP 状态机）
├── CborSerializerContext    // sealed 抽象类，Heap/Segment 双实现，直写 WriteBuffer
├── CborSerializeResult      // Continue/Finished/NewMarshallable/NewArray/NewCollection/NewMap
├── CborSerializeFunc        // 序列化函数式接口（分派表元素）
├── CborDeserializer         // 反序列化入口（12 个公开方法，逐一对齐 JsonDeserializer）
├── CborDeserializerOption   // 反序列化选项（Builder + 系统属性上限）
├── CborDeserializerException
├── CborDeserializerNode     // 显式栈节点，含 DummyObj/DummyCol 未知字段跳读
├── CborDeserializerContext  // sealed 抽象类，Heap/Segment 双实现
├── CborDeserializeResult    // Continue/Finish/NewMarshallable/NewArr/NewCol/NewMap/NewDummyObj/NewDummyCol
├── CborDeserializeFunc      // 反序列化函数式接口
├── CborPrimitiveType        // sealed interface（对称 JsonPrimitiveType）
├── CborBoolType             // value record(boolean data)
├── CborNumberType           // value record(long data)，整数语义
├── CborStrType              // value record(String data)
├── CborBytesType            // value record(byte[] data)（CBOR 特有，对应 major 2）
├── CborNumberUtil           // initial byte 拼装/解析、ai 最短编码、half 浮点转换
└── CborUtf8Validator        // 文本串 UTF-8 校验（向量 + 标量，独立于 json 模块）
```

与 json 的差异说明：不设 `CborIndentationLevel`（二进制无缩进概念）；不设
`CborWriter/CborReader`（复用 `jing-common` 的 `WriteBuffer/ReadBuffer` 及其
`writeInt/writeLong/...（..., ByteOrder.BIG_ENDIAN）` 原语）；不设 `CborTag` 常量类
（v1 不产出标签，标签号仅在 `CborNumberUtil` 中以解析分支形式出现）。

### 6.3 公开 API 形状（镜像 json 的完整方法面）

入口类构造器取选项对象（`option == null` 抛对应 Exception），栈容量用
`INITIAL_SIZE = 4` / `MAX_SIZE = 4096` 常量约束。方法面与 json 一一对称：

```java
public final class CborSerializer {
    public CborSerializer(CborSerializerOption option)

    // 8 个原生数组类型
    public void serializeByteArray(byte[] arr, WriteBuffer writeBuffer)
    public void serializeBooleanArray(boolean[] arr, WriteBuffer writeBuffer)
    public void serializeShortArray(short[] arr, WriteBuffer writeBuffer)
    public void serializeCharArray(char[] arr, WriteBuffer writeBuffer)
    public void serializeIntArray(int[] arr, WriteBuffer writeBuffer)
    public void serializeLongArray(long[] arr, WriteBuffer writeBuffer)
    public void serializeFloatArray(float[] arr, WriteBuffer writeBuffer)
    public void serializeDoubleArray(double[] arr, WriteBuffer writeBuffer)

    // 对象 / 数组 / 集合 / 映射
    public void serializeMarshallableObject(Object marshallable, WriteBuffer writeBuffer)
    public void serializeArray(Object[] arr, WriteBuffer writeBuffer)   // 枚举不可直接序列化
    public <T> void serializeCollection(Collection<T> collection, Class<T> elementType, WriteBuffer writeBuffer)
    public <K, V> void serializeMap(Map<K, V> map, Class<K> keyType, Class<V> valueType, WriteBuffer writeBuffer)
}

public final class CborDeserializer {
    public CborDeserializer(CborDeserializerOption option)

    // 8 个原生数组类型（ReadBuffer 参数形态，逐一对齐）
    public byte[] deserializeByteArray(ReadBuffer readBuffer)
    public boolean[] deserializeBooleanArray(ReadBuffer readBuffer)
    public short[] deserializeShortArray(ReadBuffer readBuffer)
    public char[] deserializeCharArray(ReadBuffer readBuffer)
    public int[] deserializeIntArray(ReadBuffer readBuffer)
    public long[] deserializeLongArray(ReadBuffer readBuffer)
    public float[] deserializeFloatArray(ReadBuffer readBuffer)
    public double[] deserializeDoubleArray(ReadBuffer readBuffer)

    public <T> T deserializeMarshallableObject(Class<T> marshallableType, ReadBuffer readBuffer)
    public <T> T[] deserializeArray(Class<T> componentType, ReadBuffer readBuffer)
    public <T> Collection<T> deserializeCol(Class<T> elementType, ReadBuffer readBuffer, Supplier<Collection<T>> supplier)
    public <K, V> Map<K, V> deserializeMap(Class<K> keyType, Class<V> valueType, ReadBuffer readBuffer, Supplier<Map<K, V>> supplier)
}
```

与 json 完全一致的入参校验规则：null 参数、primitive/多维数组/泛型组件误用、Map 键
非 String/CharSequence、supplier 返回 null 等，均抛对应的 `Cbor*Exception`
（消息小写开头）。注意 CBOR 的 `serializeByteArray` 走 **major 2 字节串**路径，
而非像 json 那样把 `byte[]` 展开为数字数组（json 中字节数组序列化为 `[0,127,-1,...]`
形式的数值数组），见 §6.5 映射表。

### 6.4 公开 API 与选项设计

**CborSerializerOption（镜像 JsonSerializerOption + 一项 CBOR 特有开关）**：

| 选项 | 默认 | 说明 |
|---|---|---|
| `setTransformerClasses(Class<?>...)` | — | 注册转换器，builtin 必须是 `CborPrimitiveType` 子类 |
| `setSerializeNullInObjOrMap(boolean)` | false | false 时对象字段 / Map 值为 null 的条目整体省略（与 json 一致） |
| `setMaxNestedSize(int)` | 64 | 嵌套深度上限，范围 [4, 4096] |
| `setDeterministicEncoding(boolean)` | false | 开启 core deterministic encoding（RFC 8949 §4.2.3，见 §4.3） |

无 `indentationLevel`（二进制格式无缩进概念）。

**CborDeserializerOption（镜像 JsonDeserializerOption，按二进制特性调整）**：

| 选项 | 默认 | 说明 |
|---|---|---|
| `setTransformerClasses(Class<?>...)` | — | 同序列化侧 |
| `setEnsureAllFieldsPresent(boolean)` | false | true 时对象缺失任意字段抛异常（默认只强制原始类型字段齐全） |
| `setMaxStringBytes(int)` | 65535 | 文本串最大字节数（范围 [64, 64MB]，上限系统属性 `jing.marshallcbor.maxstringsize`） |
| `setMaxBytesBytes(int)` | 65535 | 字节串最大字节数（CBOR 特有，json 无对应管道；范围/上限取法同 maxstringsize） |
| `setMaxArrayElements(int)` | 1000 | 数组元素数上限 |
| `setMaxMapElements(int)` | 200 | 映射条目数上限 |
| `setMaxDummyElements(int)` | 4 | 未知字段平衡跳读条目上限 |
| `setMaxNestedSize(int)` | 64 | 嵌套深度上限 |
| `setCharBufferSize(int)` | — | 文本解码字符缓冲（向上取整到 SIMD 向量宽度的倍数） |

**有意去除的 json 选项及其理由**：

- `maxEmptyBytes`：CBOR 无空白 / 分隔符，无需跳空白限额。
- `maxNumberBytes`：CBOR 数值是二进制编码（整数最长 9 字节、浮点最长 9 字节），不存在
  "数字字面文本"可超界；若未来引入 bignum 标签（tag 2/3）再恢复该限额概念。
- `jing.marshalljson.escapeslash` / `filtersurr`：CBOR 文本串不做 JSON 式转义，字符串以
  原始 UTF-8 字节写出（`String.getBytes(UTF_8)` 恒产生合法 UTF-8），无需过滤孤立代理项。

### 6.5 类型映射表

**Java/jing 类型 → CBOR 编码**（序列化方向，按 MarshallUtil 类型编码组织）：

| MarshallUtil type | Java 类型 | CBOR 编码 |
|---|---|---|
| BYTE/SHORT/INT/LONG（0,2,4,5） | byte/short/int/long | 非负 → major 0；负 → major 1（-1-n），均取 ai 最短编码 |
| *\_WRAPPER（8,10,12,13） | 对应包装类 | 同上；null → 0xf6 |
| BOOLEAN（1）/ BOOLEAN_WRAPPER（9） | boolean/Boolean | 0xf5 / 0xf4；null → 0xf6 |
| CHAR（3）/ CHAR_WRAPPER（11） | char/Character | 长度 1 的文本串（major 3），与 json 语义对齐；null → 0xf6 |
| FLOAT（6）/ DOUBLE（7）及其包装 | float/double | major 7：float → 0xfa（float32）；double → 0xfb（float64）；null → 0xf6 |
| BYTE_ARRAY（16） | byte[] | **major 2 字节串**（长度前缀 + 原始字节），与 json 的"数字数组展开"不同，是 CBOR 的核心优势路径 |
| BOOLEAN/SHORT/CHAR/INT/LONG/FLOAT/DOUBLE_ARRAY（17–23） | 其他原始数组 | 定长数组 major 4，元素各自编码（char[] 元素为单字符文本串） |
| *\_WRAPPER_ARRAY（24–31） | 包装类型数组 | 定长数组 major 4，元素各自编码（含 null→0xf6） |
| CHARSEQUENCE/STRING（35,36） | CharSequence/String | major 3 文本串（长度前缀 + UTF-8 字节）；null → 0xf6 |
| ENUM（34） | 枚举 | major 3 文本串（mappedName，缺省 name） |
| ARRAY（33） | 引用类型数组 | 定长数组 major 4，元素按组件类型递归 |
| COLLECTION_INTERFACE/IMPL（46,47） | List 等 | major 4：List 定长（size()）；非 List 集合由迭代器流式写不定长（0x9f ... 0xff） |
| MAP_INTERFACE/IMPL（49,50） | Map<String,V> | major 5：键为 major 3 文本串（约束同 json：K 必须 String/CharSequence） |
| ONE_GENERIC/TWO_GENERIC（45,48） | 泛型容器 | 按容器元素 / 值类型递归（同样依赖 MarshallInfo 泛型参数） |
| RAW（32）非 Marshallable | 其他类型 | 走 transformer 管道（builtin 须为 `CborPrimitiveType` 子类），否则抛 "type not marshallable" |
| RAW（32）Marshallable | @Marshallable bean/record | major 5 映射：键 = mappedName 文本串，值 = 字段按上表规则 |
| 自描述头（可选） | — | 开头写 tag 55799（0xd9 0xd9 0xf7）便于格式探测，v1 默认不写 |

**CBOR 数据项 → jing 类型分派**（反序列化方向，按 initial byte 的 major/ai）：

| initial byte | 反序列化通道 |
|---|---|
| major 0 / 1（ai 0–27） | byte/short/int/long 及包装类；char 不接整数（与 json 一致，char 只接单字符文本串） |
| major 2 | byte[] 或 Byte[]（按期望类型） |
| major 3 | String/CharSequence / 枚举（mappedName 哈希匹配）/ char（长度 1） |
| major 4（定长或不定长） | 数组 / Collection（按期望类型） |
| major 5（定长或不定长） | @Marshallable bean（键名哈希匹配）/ Map<String,V> |
| major 7 ai 20/21 | boolean/Boolean |
| major 7 ai 22 | null（引用类型返回 null；原始类型字段抛非法 null 类异常） |
| major 7 ai 25/26/27 | float/double（half/single/double 三种宽度统一换算） |
| major 6（标签） | v1 拒绝并抛 "tag not supported"，避免误读 bignum/日期等语义 |
| ai 24、ai 28–30、0xf8–0xff | 抛 "unsupported or illegal initial byte" |

> 上表即"CBOR 规范 ↔ jing 类型系统"的核心对照：major 0/1 ↔ 整数族、
> major 2 ↔ byte[]、major 3 ↔ String/CharSequence/枚举/char、major 4 ↔ 数组/集合、
> major 5 ↔ Marshallable 对象/Map、major 7 simple 值 ↔ boolean/null、
> major 7 浮点 ↔ float/double。

### 6.6 序列化流程

镜像 json 的"入口 → 上下文 → 根节点 → 显式栈"骨架，差异点在于写编码：

1. **入口**：`serializeXxx(arr, wb)` 判空 → `CborSerializerContext.newCtx(option, wb)`
   → 根节点 init → `process(root, context)` → `context.commit()`（一次性回写
   `WriteBuffer`）。
2. **对象写出**：`fc.marshallInfos()` 取字段序；先写 head(major 5, 条目数) 再逐字段
   写 mappedName 文本串 + 值（值类型经 `FUNC_TABLE[type]` 或 `valueSerializeFunc`
   分派，与 json 同构）。
3. **字节数组特判**：`byte[]` 直接写 head(major 2, length) + 原始字节，不走逐元素
   展开（与 json 的差异路径，§6.3 已注明）。
4. **错误入参拦截**：enum 对象、primitive 组件、多维数组、泛型组件、Map 非文本键
   ——与 json 相同的守卫逻辑前置。
5. **显式栈**：`CborSerializer.process` 复用 json 的双槽游走算法（`nodes` 数组容量 4
   起倍增），`nextNode` 按 `CborSerializeResult` 分派（NewMarshallable/NewArray/
   NewCollection/NewMap），深度达到 `maxNestedSize` 抛
   `"exceeded maximum nested size : 64"`。
6. **长度策略（null 省略 vs 定长）**：`serializeNullInObjOrMap=false` 时 null
   字段/条目不写，条目数会变化，因此：
   - bean 对象：条目数 = `marshallInfos()` 中非 `skipSerializing` 的字段数，null 省略
     生效时改为**不定长 map（0xbf ... 0xff）**；
   - `Map`：`true` 模式定长（`map.size()`），`false` 模式不定长；
   - `List` / 数组：定长（size()/length 已知）；非 List 的 `Collection`：迭代器流式 +
     不定长数组（0x9f ... 0xff）。
7. **确定性编码**：`deterministicEncoding=true` 时对象 / Map 键先按
   "长度优先 + 字节字典序"排序，并强制 `serializeNullInObjOrMap=true`（定长才能保证
   排序后的字节级可复现）。
8. **无转义、无缩进、无空白**：文本串 val 直接 UTF-8 字节写出（surrogate 由
   `getBytes(UTF_8)` 天然替换，无需过滤器）；浮点走
   `WriteBuffer.writeFloat/writeDouble(..., BIG_ENDIAN)`；整数走
   `CborNumberUtil.writeHead(major, ai, value)` 拼装 initial byte + 最短负载。

> 字符串写出与 json 的最大差异：无转义扫描、无 ASCII 快速路径需求（仍可用向量指令
> 加速长度压码与字节拷贝），`CborUtf8Validator` 仅用于反序列化侧文本校验。

### 6.7 反序列化流程

与 json 最大的结构性差异：**没有全量 UTF-8 预校验、没有空白跳过、没有 firstByte 语法
分派**——CBOR 是长度前缀二进制，读取天然"按 initial byte 前进"：

1. **入口**：判空 → `CborDeserializerContext.newContext(option, rb)` → 读 initial
   byte → 按 `major = b >>> 5` / `ai = b & 0x1F` 分派。文本串解码时逐段做 UTF-8
   校验（`CborUtf8Validator`），失败抛 `"not valid utf-8 content"`。
2. **对象**：head(major 5) 定长 → 逐键 `marshallInfoByMappedName(byte[], from, to)`
   零分配哈希匹配（复刻 json 的两套哈希组）；BITMAP 防重复键；收尾检查
   `ensureAllFieldsPresent`（默认只强制原始类型字段齐全，借助 `primitiveElements()`）；
   反序列化侧按 `skipDeserializing` 标志跳过字段（值照读、不写 builder）。
3. **构建**：`fc.newBuilder()` → `MarshallBuilder.writeXxx(index, value)` →
   `fc.construct(builder)`；enum 门面无构建族，走别名查找 + `enumItemMarshallInfo`
   路径（与 json 相同）。
4. **未知字段**：DummyObj / DummyCol 平衡跳读；不定长容器（ai 31）须正确消费
   0xff break；`maxDummyElements` 超限抛异常。
5. **不定长容积限**：`maxArrayElements` / `maxMapElements` 对"声明条目 + 已读条目"
   同时计数，防 break 延迟导致的放大攻击。
6. **非法路径**：major 7 的 ai 24 与 0xf8–0xff、保留 ai 28–30 抛
   `"unsupported or illegal initial byte"`；major 6 抛 `"tag not supported"`。
7. **边界/越界**：长度前缀超出 `ReadBuffer` 剩余字节、`maxStringBytes` /
   `maxBytesBytes` 超限等抛 `CborDeserializerException`（消息小写开头）。
8. **显式栈**：与序列化侧同构的 `CborDeserializerNode[]` 栈 + 深度上限；`initObj /
   initArr / initCol / initMap / initDummy*` 各分支对应 `CborDeserializeResult`
   （NewMarshallable / NewArr / NewCol / NewMap / NewDummyObj / NewDummyCol）。

### 6.8 转换器集成

镜像 json 的约束与注册逻辑：

1. **builtin 类型接口族**（对称 `JsonPrimitiveType`）：

```java
public sealed interface CborPrimitiveType
    permits CborBoolType, CborNumberType, CborStrType, CborBytesType {}

public value record CborBoolType(boolean data) implements CborPrimitiveType {}
public value record CborNumberType(long data) implements CborPrimitiveType {}
public value record CborStrType(String data) implements CborPrimitiveType {}
public value record CborBytesType(byte[] data) implements CborPrimitiveType {}
```

   - `CborNumberType` 只承载**整数**语义：CBOR 的整数（major 0/1）与浮点（major 7）
     编码路径分离，而 `MarshallUtil` 已原生区分 `FLOAT_TYPE/DOUBLE_TYPE`，浮点值无需
     经 primitive 标签中转。json 的 `JsonNumberType(byte[])` 需要"数字字面文本"是
     因为 JSON 数字是文本；CBOR 无此问题。
2. **注册校验**（逐条复刻 json Builder）：transformer 类与数组非空、`customType`
   未重复注册、`customType` 非内建类型（无法覆盖原生）、非 Marshallable bean、
   **`builtinType` 必须是 `CborPrimitiveType` 的实现类**——违反即抛
   `IllegalArgumentException`。
3. **分派**：`customObjSerializeFunc` / `customObjDeserializeFunc` 按 `builtinType`
   的四个具体子类分支，调用 `c.serializeCborXxxType(...)` /
   `c.deserializeCborXxxType(...)`；数组批量走 `toBuiltinArray` / `toCustomArray`
   （对应 json 的 `customArrFunc*`）。
4. **示例**：`@Transformable final class XxxTransformer implements
   MarshallTransformer<LocalDateTime, CborStrType>`；注册
   `setTransformerClasses(XxxTransformerFacade.class)`。

### 6.9 异常体系

镜像 json，两个异常类结构完全一致：

```java
public final class CborSerializerException extends RuntimeException {
    public CborSerializerException(String message) { super(message); }
}
public final class CborDeserializerException extends RuntimeException {
    public CborDeserializerException(String message) { super(message); }
}
```

要点：

- 仅 `(String message)` 构造（json 同款，无 cause 链、无重载）。
- 消息一律小写开头：`"arr must not be null"`、`"enum cannot be directly
  serialized"`、`"type not marshallable : io.X"`、`"exceeded maximum nested size"`、
  `"not valid utf-8 content"`、`"unsupported or illegal initial byte"`、
  `"tag not supported"` 等。
- 选项越界在 Builder 内抛 `IllegalArgumentException`（与 json 一致，不属于
  `Cbor*Exception`）。

---

## 7. 测试策略

测试骨架镜像 json 的 8 个测试类（`jing-marshall-json/src/test/java/io/jingproject/
marshalljsontest/`），测试实体复用 `entity/` 的 BeanEntity / RecordEntity /
EnumEntity / RecursiveEntity（含 BigDecimalTransformer），测试包名对称：
`io.jingproject.marshallcbortest`，JUnit 类名以 `Test` 结尾。

### 7.1 镜像 json 的测试矩阵

| json 测试类 | cbor 对应 | 覆盖点 |
|---|---|---|
| WriteIntegerTest | CborWriteIntegerTest | 8 种原始类型数组直接写出、字节精确断言（含大端序） |
| ReadIntegerTest | CborReadIntegerTest | 原始类型数组直接读取、越界 / 非法 initial byte |
| WriteFloatTest | CborWriteFloatTest | float32/float64 编码字节精确断言、负数/NaN/±0/次正规数 |
| ReadFloatTest | CborReadFloatTest | half/single/double 三种宽度混读、非法宽度报错 |
| Utf8ValidationTest | CborUtf8ValidationTest | 文本串字节合法性（合法 1–4 字节序列、非法续字节、截断） |
| StringSerializationTest | CborStringSerializationTest | 文本串往返（ASCII/中文/emoji/代理对）、长度前缀各档位 |
| JsonSerializationTest | CborSerializationTest | Bean/Record/Enum/递归实体往返、transformer（BigDecimal→CborStrType）、null 省略、定长/不定长 |
| JsonDeserializationTest | CborDeserializationTest | 位图去重、缺失字段（ensureAllFieldsPresent）、未知字段 Dummy 跳读、文本串/字节串限额 |

### 7.2 CBOR 特有/新增测试

| 测试点 | 说明 |
|---|---|
| 整数最短编码 | 0–23 直写、24→ai24、256→ai25、65536→ai26、2^32→ai27；负数 -1-n 边界（-1、-24、-256、-65536 等） |
| 字节串 | 空字节串、含 NUL / 高位字节的二进制往返、长度前缀各档位、超大长度报错 |
| 定长 vs 不定长 | List/Map 定长、非 List Collection 不定长往返、0xff break 正确消费与缺失报错 |
| 确定性编码 | `deterministicEncoding=true` 时键排序后的字节级断言（含定长强制） |
| 标签拒绝 | 输入含 tag 6（如自描述头 0xd9 0xd9 0xf7）抛 "tag not supported" |
| RFC 8949 样例向量 | 附录 A 的标准数据项逐字节断言（如 `[1, [2, 3], ["4", 5]]`），保证互操作 |
| 深度与限额 | `maxNestedSize` 触顶、`maxArrayElements`/`maxMapElements`/`maxDummyElements` 超限、`maxStringBytes`/`maxBytesBytes` 超限 |
| 大端序 | 多字节整数/浮点字节序与 RFC 8949 规定一致（`ByteOrder.BIG_ENDIAN`） |

---

## 8. 构建与工程集成

- 根 pom `<modules>` 登记。
- `mvn clean install -DskipTests=true` 全量构建通过。
- `diff.md` 追加变更说明。
- 可选用 `jing-bench` 增加 CBOR vs JSON 吞吐对比基准（遵循 jing-bench 的三方依赖
  说明规则）。

---

## 9. 风险与待决问题

1. **是否要动用 `MarshallUtil` 类型表扩展位**（新增如 Instant 内建类型）：默认不做，
   走 transformer；若确需，必须同步修改 jing-marshall 与 processor 并重跑全量测试。
2. **CBOR 映射键约束**：json 要求 K 为 String/CharSequence；CBOR 的映射键原则上支持
   任意类型，但为与门面/转换器体系保持一致，本设计默认映射键只用文本串（键写出集中
   在 `CborSerializerContext.serializeMapKey` 一处，便于后续扩展数值键）。
3. **确定性编码 vs 性能**：`deterministicEncoding` 默认关闭（preferred serialization
   只保证最短编码、不强制键排序）；开启后键需按"长度优先 + 字典序"排序、且强制
   `serializeNullInObjOrMap=true`（null 省略与定长容器冲突），有一定额外开销。
4. **tag 的采用边界**：v1 不产出、不消费任何标签（输入含 tag 一律报错），
   `BigInteger`/`BigDecimal` 走 transformer（tag 2/3、tag 4/5 语义留待后续版本，
   届时需恢复 bignum 长度限额概念）。
5. **与既有 json 的对称性**：公共 API、异常、选项、节点/上下文结构全部与 json 对称，
   降低使用者心智负担（§6.3–§6.9 已逐项对齐）。
6. **循环引用**：与 json 一致不检测引用环，对象环由 `maxNestedSize` 兜底。

---

## 10. 参考资料

- RFC 8949: Concise Binary Object Representation (CBOR) — https://www.rfc-editor.org/rfc/rfc8949
- RFC 7049: CBOR 前身（2013）
- cbor.io — 官方信息站与 FAQ