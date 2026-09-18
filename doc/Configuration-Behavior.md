# jing-common 配置子系统「输入→输出」行为规范（最终实现状态）

> 适用范围：`io.jingproject.common.conf` 包（CfgReader 三实现、CfgUtil、Cfg、CfgException、DefaultConfigurationFacade）与 `io.jingproject.common` 的 ConfigurationFacade / ConfigurationFactory。
> 证据基础：最终源代码 + `commontest` 下 CfgTest / JsonCfgReaderTest / TomlCfgReaderTest / PropertiesCfgReaderTest / ConfigurationFactoryTest。

## 1. 总体数据模型

### 1.1 Cfg 密封层级（`Cfg.java`）

```
Cfg (sealed interface)
├── CfgItem(String value)      // 标量值节点，value 为规范化后的字符串
├── CfgList(List<String> value) // 列表节点，元素为字符串（数组一律扁平的标量列表）
└── CfgObject(Map<String, Cfg> value) // 对象节点，键 → 子节点
```

- **值模型为纯字符串层**：所有格式的数字、布尔、inf/nan 在解析期被规范化/校验为字符串后存入 `CfgItem`，不做数值换算（具体规范化规则见各格式章节）；`CfgList` 元素同样全部是字符串。
- 三实现均为嵌套 record，`CfgException` 为唯一用户可见异常（`RuntimeException`）。
- `asImmutable()`：`CfgItem` 返回自身（String 本就不可变）；`CfgList` 返回 `List.copyOf` 包装；`CfgObject` 深拷贝整棵子树。**注意**：门面公共 API 不依赖它（见 2.2），它面向直接持有 `Cfg` 的调用方。

### 1.2 解析器共用语义（三个 reader）

- **单次使用**：reader 是状态机实例，`parse()` 只可调用一次（代码注释明确声明，二次调用行为未定义）。
- **键字符集**（三格式统一，由 `CfgUtil.rejectKey` 定义）：`[a-zA-Z0-9_-]`，字节级 ASCII 判定；`readCfgKey`（单段键）与 `readCfgNestedKey`（点号分段键）共用。
- **maxDepth**：三 reader 均先校验 `maxDepth <= 0 → CfgException("invalid max depth: " + maxDepth)`；层级语义见第 4 节。
- **键冲突**：同一作用域内重复键一律 `CfgException("Duplicate key: " + key)`（Properties 的 flat 层例外，见 3.3）。
- **数组**：三格式均只接受标量元素；元素可为字符串/数字/布尔（TOML 另含 inf/nan）；禁止嵌套数组与对象元素、禁止尾逗号。
- **解析错误的抛出**：面向用户输入的所有错误路径抛 `CfgException`；仍保留的 `AssertionError` 仅存在于程序契约违规守卫（如 null 输入流、非法 buffer 类型），正常文件输入不可达。

### 1.3 门面级单例缓存语义

- `ConfigurationFactory.instance()`：Holder 单例；优先 `ServiceLoader` 提供的 `ConfigurationFacade` 实现，无则 `DefaultConfigurationFacade`。
- `DefaultConfigurationFacade.getConfiguration()`：Holder 单例，**首个 conf/confList 调用时才加载**，之后全进程复用同一棵 `CfgObject` 解析结果（不监听文件变化）。
- 资源查找：`jing.config.file`（默认 `jing-config`）+ 扩展名，classpath 资源；扩展名缺省时按 **toml → json → properties** 顺序取**第一个存在**的文件，找到即解析并停止，解析失败不回退到下一个扩展名。
- 自定义扩展名：`jing.config.ext`（逗号无关，单值），非白名单（toml/json/properties）→ 抛异常。
- 深度上限：环境变量 `JING_CONFIG_MAX_DEPTH`（默认 128），必须 **> 4**，否则 `ExceptionInInitializerError("max depth must be greater than 4, actual: ...")`；无法解析为 int 时同样以 `ExceptionInInitializerError` 包装抛出。

## 2. 公开 API 行为契约

### 2.1 `ConfigurationFacade`（接口）

```
String conf(String key);
List<String> confList(String key);
```

### 2.2 `DefaultConfigurationFacade`

统一经私有 `navigate(key)`（`DefaultConfigurationFacade.java` L91-112）解析键路径；两方法行为：

| 场景 | conf(key) | confList(key) |
|---|---|---|
| key 为 `""` 空串 | `CfgException("invalid key: ")`（navigate 前置检查） | 同左 |
| key 为 `null` | NullPointerException（无守卫，`key.isEmpty()` 处触发） | 同左 |
| 键路径任意中间段不存在 | 返回 `null` | 返回 `null` |
| 末段不存在 | 返回 `null` | 返回 `null` |
| 末段为 `CfgItem` | 返回其 value 字符串 | `CfgException("invalid key: <key>, type : CfgItem")` |
| 末段为 `CfgList` | `CfgException("invalid key: <key>, type : CfgList")` | 返回 `List.copyOf(value)`（**每次调用返回不可变拷贝，不泄漏内部 ArrayList**） |
| 末段为 `CfgObject` | `CfgException("invalid key: <key>, type : CfgObject")` | `CfgException("invalid key: <key>, type : CfgObject")` |
| 中间段为 `CfgItem`/`CfgList`（非对象） | `CfgException("invalid key: <key>, current nested key: <段>, type : <类型名>")` | 同左 |
| key 含非法字节（如空格、`.`连续等） | `CfgUtil` 系异常（"invalid key byte: N" / "invalid consecutive delimiters" / "invalid delimiters"） | 同左 |
| key 段数 > MAX_DEPTH | `CfgException("maximum nesting depth exceeded")` | 同左 |

- 键路径用 UTF-8 字节解析，点号分段；`"a.b"` 表示两层。

### 2.3 `ConfigurationFactory`（静态门面）

| 方法 | 行为 |
|---|---|
| `conf(key)` / `confList(key)` | 委托 `instance()`，语义同 2.2 |
| `conf(key, default)` | 值为 null 或 blank（`isBlank()`，含全空白）→ 返回 default；否则返回原值 |
| `valueAsBoolean(v, def)` | `equalsIgnoreCase` 匹配 `"true"`/`"false"` → 对应布尔；其余（null/blank/其他）→ default |
| `confAsBoolean(key, def)` | `valueAsBoolean(conf(key), def)`；key 缺失 → conf 返回 null → default |
| `valueAsInt/Long/Float/Double(v, def)` | `v == null || v.isBlank()` → default；否则 `parse*` 转换，**解析失败抛 NumberFormatException（不回落 default）** |
| `confAsInt/Long/Float/Double(key, def)` | 委托 `conf`；缺失（null→default）、空白值（blank→default）、非法数值（NumberFormatException） |

- 设计对齐点：配置文件中写入的值经解析器得到 `""` 等空白时，`confAs*` 与 `conf(key, default)` 行为一致（都取 default）；只有 **非空白且非法的字符串** 才会让 NumberFormatException 逃逸。

## 3. 三种格式的输入→输出规范

### 3.1 JSON（`JsonCfgReader`）

**结构规则**：根必须是对象；空白字符集 `' ' '\t' '\r' '\n'` 各处可用；对象值可为对象/数组/标量；数组元素仅标量。字符串默认 UTF-8 字节透传解码（非 UTF-8 字节产生 U+FFFD 替换，不报错）；控制字符（含裸换行）不做规范校验、原样接受（宽容行为）。

**合法输入 → 输出示例**

| 输入 | 输出（CfgObject 展开） |
|---|---|
| `{}` | `CfgObject{}`（根空对象合法） |
| `{"a": 42}` | `CfgObject{a: CfgItem("42")}` |
| `{"a": -7}` | `CfgObject{a: CfgItem("-7")}` |
| `{"a": -0}` | `CfgObject{a: CfgItem("-0")}`（负零保留符号与 `0` 区分） |
| `{"a": 0}` / `{"a": 123}` | `CfgItem("0")` / `CfgItem("123")` |
| `{"a": 12345678901234567890}` | `CfgItem("12345678901234567890")`（超 long 范围，纯文本保留） |
| `{"a": 3.14}` / `{"a": 0.5}` / `{"a": -0.5}` | 原样字符串 |
| `{"a": 1e10}` / `{"a": 0e0}` / `{"a": 1E2}` / `{"a": 2.5E-3}` / `{"a": 1.5E+10}` | 原样字符串（指数记法保留大小写） |
| `{"a": true}` / `{"a": false}` | `CfgItem("true")` / `CfgItem("false")` |
| `{"a": ""}` | `CfgItem("")` |
| `{"a": "你"}` / `{"a": "\u4F60"}` | `CfgItem("你")` |
| `{"a": "\uD83D\uDE00"}` | `CfgItem("😀")`（合法代理对合并为码点，UTF-8 编码） |
| `{"a": [1, "two", true]}` | `CfgList["1", "two", "true"]`（元素统一字符串化） |
| `{"a": []}` | `CfgList[]`（空数组合法） |
| `{"a": {}}` | `CfgObject{a: CfgObject{}}`（空嵌套对象合法，入帧后立即弹帧落库） |
| `{"a": {"b": "c"}}`、`{"a": [1, 2], "b": {"c": {}}}` 等 | 逐层嵌套展开 |
| 首尾/键两侧空白、`{ "a" : 1 }` | 正常解析（跳过空白） |
| `{"a":1} `（根后有空白） | 正常（尾随空白容忍） |

**拒绝表（输入 → 异常消息原文）**

| 输入 | 异常消息 |
|---|---|
| `""`（空输入）、`{`、`{"a":` | `EOF reached` |
| `{"a": 1`（缺根收尾 `}`） | `EOF reached` |
| `{"a":1}x`、`{"a":1}}`（根闭合后非空白尾随字节） | `Corrupted json configuration` |
| `[1,2]`、`"s"`、`123`、`null`（根非对象） | `Corrupted json configuration` |
| `{"a": null}` / `{"a": NaN}`（不支持的值首字节） | `Corrupted json configuration` |
| `{"a": }` / `{"a":1,}` / `{` 后非法字符 | `Corrupted json configuration` |
| `{"a": 007}`、`{"a": 01}` | `invalid json number: leading zero: 55`（b 为首个违规数字字节值） |
| `{"a": .5}`、`{"a": .}`、`{"a": 5.}`、`{"a": 1..2}` | `invalid json number: 46` 等（b 为该处读到的字节值） |
| `{"a": 1e}`、`{"a": 1e+}`、`{"a": 1e-}` | `invalid json number: 125` 等 |
| `{"a": +1}`、`{"a": -}` | `invalid json number: 43` / `...: 125` |
| `{"a": 1_0}`、`{"a": 0xFF}`、`{"a": 0x1}` | 数字扫描后由状态机在 `_`/`F` 处报 `Corrupted json configuration`（下划线/十六进制不受支持） |
| `{"a": truex}` | `Corrupted json configuration`（`true` 读毕，`x` 在值后） |
| 重复键 `{"a": "1", "a": "2"}`、`{"a":{},"a":{}}` | `Duplicate key: a` |
| `{"a": 1, "a": {}}` 类跨形态重复 | `Duplicate key: a` |
| `{"a": "x\u0042"}` 键内转义（键含 `\`） | `invalid key byte: 92` |
| `{"": 1}`（空键） | `empty key` |
| 键含空格/非 ASCII（如 `{"a b":1}`、`{"你":1}`） | `invalid key byte: 32` / `invalid key byte: 228` |
| 值字符串 `"abc` 未闭合 | `EOF reached` |
| `"\\q"` 类未知转义 | `invalid escape sequence: 113` |
| `"\uD83D"`（单独高代理后跟非 `\u`） | `invalid target, assumed: 92 actual: 34`（assume 失败即报错） |
| `"\uG004"` 非法十六进制 | `bad hex character: 71` |
| `[1,]` / `[[]]` / `[{}]`（尾逗号/嵌套元素） | `Corrupted json configuration` |
| 深度超限（见第 4 节） | `maximum nesting depth exceeded` |
| `maxDepth <= 0` | `invalid max depth: 0` |

**宽松点**（不报错但偏离 RFC 8259）：字符串内裸控制字符/裸换行原样接受；未配对代理（除上述紧随非 `\u` 的硬错误外）经 `writeCodePointInUtf8` 写出后以 U+FFFD 呈现（例如 `"\uD83D\u0041"` → `"\uFFFDA"`）。

### 3.2 TOML（`TomlCfgReader`）

**结构规则**：行式 kv + `[表头]`；键字符集 `[a-zA-Z0-9_-]`（点号键 `a.b = 1` 不支持）；值可为双引号字符串、数字（含 inf/nan）、布尔、扁平数组；键/值两侧空白 `' ' '\t'` 容忍，行分隔允许 `\r\n`/`\n`；空文件/纯空白文件 → 空 `CfgObject{}`。

**合法输入 → 输出示例**

| 输入 | 输出 |
|---|---|
| `""`（空文件） | `CfgObject{}` |
| `a = 42` / `a = -7` / `a = +3` | `CfgItem("42")` / `CfgItem("-7")` / `CfgItem("3")`（正号剥除） |
| `a = 1_000_000` | `CfgItem("1000000")`（下划线剥除） |
| `a = 0` / `a = -0` / `a = +0` | `CfgItem("0")`（符号归并） |
| `a = -9223372036854775808` | `CfgItem("-9223372036854775808")`（Long.MIN 精确支持） |
| `a = 0xFF` / `a = 0xF_F` / `a = 0x0` | `CfgItem("255")` / `CfgItem("255")` / `CfgItem("0")`（十六进制换算十进制） |
| `a = 0o77` / `a = 0b1010` / `a = 0b10_10` | `CfgItem("63")` / `CfgItem("10")` / `CfgItem("10")` |
| `a = 3.14` / `a = 0.5` / `a = 1.0` 类 | 原样字符串 |
| `a = 1e10` / `a = 5e0` / `a = 1e+2` / `a = 2.5E-3` | 原样字符串 |
| `a = 1_0.5` | `CfgItem("10.5")`（整数部分下划线剥除） |
| `a = inf` / `a = +inf` | `CfgItem("Infinity")` |
| `a = -inf` | `CfgItem("-Infinity")` |
| `a = nan` / `a = -nan` / `a = +nan` | `CfgItem("NaN")`（负号对 nan 不保留） |
| `a = true` / `a = false` | `CfgItem("true")` / `CfgItem("false")` |
| `key = "value"` / `key = "\u4F60"` | `CfgItem("value")` / `CfgItem("你")` |
| `a = []` | `CfgList[]`（空数组合法） |
| `a = [1, "two", true]` | `CfgList["1", "two", "true"]` |
| `[server]` + `port = "8080"` | `CfgObject{server: CfgObject{port: CfgItem("8080")}}` |
| `[a.b]` + `c = "1"` | `CfgObject{a: CfgObject{b: CfgObject{c: CfgItem("1")}}}`（表头逐段建链） |
| `[server]`、`[server.tls]` 分节混合 | 父表已存在则导航复用，合成一棵树 |
| `# comment` 行 / 值后 `# 注释` / 表头后 `# 注释` | 注释忽略 |
| `a = 1\n# trailing comment`（**注释至 EOF 无换行**） | `CfgObject{a: CfgItem("1")}`（合法，不报 EOF） |
| `# only comment` / 全空白文件 | `CfgObject{}` |
| `a = 1#注释`（值与注释间无空格） | 合法（`#` 触发注释态） |
| `a = [1,\n2]`（数组跨行） | `CfgList["1", "2"]`（数组内空白含换行均跳过） |

**拒绝表（输入 → 异常消息原文）**

| 输入 | 异常消息 |
|---|---|
| `a = 1b`、`a = truex`、`[t]x`（值后/表头后杂散字节） | `Corrupt toml configuration` |
| `[]`（空表头） | `Corrupt toml configuration` |
| `[t`（表头未闭合） | `EOF reached` |
| `[a]\nx = 1\n[a]`（完全相同表头重复） | `Duplicate table: a` |
| `a = 1` 后 `[a]` 或 `[a]` 后 `a = 1`（表/键冲突） | `Duplicate table: a` / `Duplicate key: a` |
| `a b = "1"` | `invalid key byte: 32` |
| `a = 007` | `invalid toml integer: leading zero: 55` |
| `a = _1` | 断言于数字不合法首字节 → `invalid toml number: 95` |
| `a = 0_1` | `invalid toml integer: misplaced underscore: 49` |
| `a = 1__0` | `invalid toml integer: adjacent underscore` |
| `a = 1_` | `invalid toml integer: trailing underscore` |
| `a = +` | `invalid toml number: -1` |
| `a = +0xFF` / `a = -0xFF` | `invalid toml integer: sign not allowed for radix literal` |
| `a = 0x` | `invalid toml integer: missing digits after radix prefix` |
| `a = 0xG` / `a = 0o8` / `a = 0b2` | `integer literal out of range: 71` 等（radix 溢出一并拒绝非法 digit） |
| `a = 0xFF_` | `invalid toml integer: trailing underscore` |
| `a = 9223372036854775808` | `integer literal out of range: 9223372036854775808` |
| `a = 1e1_0` | `invalid toml float: underscore in exponent` |
| `a = .5` / `a = 5.` / `a = 1.` | `invalid toml float: 46` 等 |
| `a = 1._0` | `invalid toml float: 95` |
| `a = 1e+` | `invalid toml float: 125` |
| `a = infinity` / `a = infi` / `a = NAN` | `invalid toml value: infinity` 等（只认小写 `inf`/`nan`，其余字母串按 word 报错；`N` 首字节 → `Corrupt toml configuration`） |
| 重复键 `key = "a"\nkey = "b"`（同表内） | `Duplicate key: key` |
| `key = "a`（未闭合字符串） | `EOF reached` |
| `key = "\q"` | `invalid escape sequence: 113` |
| `key = "\uD83D"`（TOML 禁代理码点） | `invalid surrogate code point: 55357` |
| `key = "\U00110000"`（超出 Unicode 范围） | `invalid code point: 1114112` |
| `key = [1,]`（尾逗号）/ 数组内嵌套 | `Corrupt toml configuration` |
| `a = 1 b = 2`（一行两 kv） | `Corrupt toml configuration` |
| 注释行含控制字符（0x00-0x08、0x0A-0x1F、0x7F，tab 除外） | `Corrupt toml configuration` |
| 深度超限（见第 4 节） | `maximum nesting depth exceeded` |
| `maxDepth <= 0` | `invalid max depth: 0` |

**细节**：`[a.b]` 之后再 `[a]` 表头——实现允许（导航进入已存在对象继续合并，仅对**完全相同的表头字符串**做 `Duplicate table` 检测，与 TOML 规范"不可重定义父表"存在出入，属实现行为）。逃逸表：`\b \t \n \f \r \e(ESC 0x1B) \" \\ \xHH \uHHHH \UHHHHHHHH`。

### 3.3 Properties（`PropertiesCfgReader`）

**结构规则**：底层 `java.util.Properties.load(Reader)`（UTF-8 Reader 包装），完全继承其语法：`key=value` / `key:value` / `key value` 分隔、`#`/`!` 注释、行尾 `\` 续行、键值内 `\uXXXX` 转义、空行忽略。其上层再叠加：键按 `.` 展开为嵌套对象；值形如 `[...]` 拆成列表；键/值 blank 的条目直接跳过。

**合法输入 → 输出示例**

| 输入 | 输出 |
|---|---|
| `a=1\nb=2` | `CfgObject{a: CfgItem("1"), b: CfgItem("2")}` |
| `a.b=1` | `CfgObject{a: CfgObject{b: CfgItem("1")}}` |
| `key=[a, b]` | `CfgList["a", " b"]`（按 `,` 切分，**前导空格保留**，仅 blank 项剔除） |
| `key=[]` | `CfgList[]`（切分结果全 blank） |
| `# comment\na=1` | `CfgObject{a: CfgItem("1")}` |
| `a=1\na=2`（flat 层重复） | `CfgObject{a: CfgItem("2")}`（**Properties.load 语义：后值覆盖**，不报错） |
| `server.host=localhost\nserver.ports=[8080,8081]\nserver.tls.enabled=true` | 三层混合树（对象/列表/标量） |
| 空文件 | `CfgObject{}` |
| `a.b=1` + `a.b.d=2` | 合并且生（中间对象导航复用） |

**拒绝表**

| 输入 | 异常消息 |
|---|---|
| `a.b=1\na=2`（标量撞已有对象） | `Duplicate key: a.b`（消息用完整点号键） |
| `a=1\na.b=2`（对象撞已有标量） | `Duplicate key: a.b` |
| 键含非法字节（如 `a b=1`、`a:b=1`、非 ASCII 键） | `invalid key byte: 32` 等 |
| `.a=1` / `a..b=1` | `invalid consecutive delimiters` |
| `a.=1` | `invalid delimiters` |
| 键段数 > maxDepth | `maximum nesting depth exceeded` |
| `maxDepth <= 0` | `invalid max depth: 0` |

**细节**：`stringPropertyNames()` 遍历顺序不保证 → 输出 `CfgObject` 的键顺序不保证（HashMap）；`case 0`（空键列表）不可达，防御性抛 `CfgException("invalid key: " + key)`；值 `[` 开头但非 `]` 结尾（如 `key=[a`）不拆列表，整体作为 `CfgItem("[a")`。

## 4. maxDepth 边界

统一入口校验：三个 reader 对 `maxDepth <= 0` 一律 `CfgException("invalid max depth: <n>")`；门面层再由 `JING_CONFIG_MAX_DEPTH`（默认 128，须 > 4）兜底。

| 格式 | 判定规则（代码精确式） | 允许样例 | 拒绝样例 |
|---|---|---|---|
| JSON | 进入子对象时 `frames.size() + 2 > maxDepth` 即抛（**根=第 1 层**；数组不占深度）；即 maxDepth=N 允许根下 N-1 层对象 | `{"a":1}`（maxDepth=1）；`{"a":{"b":"c"}}`、`{"a":[]}`（maxDepth=2）；`{}`（任意合法 maxDepth） | `{"a":{"b":"c"}}`（maxDepth=1）；`{"a":{"b":{"c":"d"}}}`（maxDepth=2，消息 `maximum nesting depth exceeded`）；`{"a":{}}`（maxDepth=1，空对象同样入帧检查） |
| TOML | 表头键段数 `> maxDepth` 即抛（`readCfgNestedKey`，段 ≤ maxDepth 允许）；普通 kv 键为单段恒满足；表内 kv 不再单独计深 | `a = 1`（maxDepth=1）；`[a.b]` + `c = "1"`（maxDepth=2） | `[a.b.c]` + `c = "1"`（maxDepth=2，`maximum nesting depth exceeded`） |
| Properties | 点号键段数 `> maxDepth` 即抛 | `a=1`（maxDepth=1）；`a.b=1`（maxDepth=2） | `a.b.c=1`（maxDepth=2，`maximum nesting depth exceeded`） |

注：TOML/Properties 的"段数 ≤ maxDepth"以**键路径段计数**为准（表头 `[a.b]` 计 2 段），与 JSON 的"根-相对对象层级"表述口径不同，但三者共同保证"层级 > maxDepth 一律拒绝"。

## 5. 错误总表（可达路径 → 消息原文）

消息按格式分组；动态片段以 `<...>` 标注（字节值以 int 形式插入，如空格=32、`}`=125）。

**公共（三个 reader 共用 CfgUtil）**

| 路径 | 消息 |
|---|---|
| `maxDepth <= 0`（三 reader parse 入口） | `invalid max depth: <n>` |
| 单段键为空（如 JSON `{"":1}`） | `empty key` |
| 键含非法字节（`readCfgKey`/`readCfgNestedKey`） | `invalid key byte: <b>` |
| 点号键前导/连续点 | `invalid consecutive delimiters` |
| 点号键尾点 | `invalid delimiters` |
| 点号键段数超限 | `maximum nesting depth exceeded` |
| 十六进制转义含非法字符 | `bad hex character: <b>` |
| 流意外终止（search/ignoreTillEOF/assume/字符串/unicode 等） | `EOF reached` |

**JSON（JsonCfgReader + CfgUtil）**

| 路径 | 消息 |
|---|---|
| 语法/结构性错误（根非对象、尾随非空白、值位置杂散、数组分隔错误、不支持的标量首字节） | `Corrupted json configuration` |
| 重复键（标量/列表/嵌套对象落库时） | `Duplicate key: <key>` |
| 未知转义 | `invalid escape sequence: <b>` |
| 高代理后 assume 失败 | `invalid target, assumed: 92 actual: <b>` |
| 数字前导零 | `invalid json number: leading zero: <b>` |
| 数字其他非法形态（小数/指数缺位、孤立 `-`、`+` 等） | `invalid json number: <b>` |
| 布尔字面量不匹配 | `invalid json boolean: <b>` |
| 值字符串未闭合 | `EOF reached` |

**TOML（TomlCfgReader + CfgUtil）**

| 路径 | 消息 |
|---|---|
| 行尾/表头后杂散字节、空表头、值首字节不支持 | `Corrupt toml configuration` |
| 注释行含拒绝控制字符 | `Corrupt toml configuration` |
| 重复键（`storeItem`/`storeList`） | `Duplicate key: <key>` |
| 表头重复或与标量冲突 | `Duplicate table: <表头原文>` |
| 未知转义 | `invalid escape sequence: <b>` |
| 转义为代理码点（`\x`/`\u`/`\U`） | `invalid surrogate code point: <码点>` |
| `\U` 超范围 | `invalid code point: <码点>` |
| 数字起始非法 | `invalid toml number: <b>` |
| radix 带符号 | `invalid toml integer: sign not allowed for radix literal` |
| 前导零 | `invalid toml integer: leading zero: <d>` |
| `0` 后下划线 | `invalid toml integer: misplaced underscore: <d>` |
| 相邻下划线 / 尾下划线 / radix 错位下划线 | `invalid toml integer: adjacent underscore` / `invalid toml integer: trailing underscore` / `invalid toml integer: misplaced underscore` |
| radix 缺数字 | `invalid toml integer: missing digits after radix prefix` |
| 整数溢出（十进制带位数输出 / radix 带字节值 / MIN 取反） | `integer literal out of range: <digits>` / `integer literal out of range: <b>` / `integer literal out of range` |
| 浮点小数位缺数字 / 相邻下划线 / 尾下划线 | `invalid toml float: <b>` / `invalid toml float: adjacent underscore: <b>` / `invalid toml float: trailing underscore` |
| 浮点指数缺数字 / 指数内下划线 | `invalid toml float: <b>` / `invalid toml float: underscore in exponent` |
| 布尔/词字面量不匹配、非法 inf/nan 词 | `invalid toml value: <b>` / `invalid toml value: <word>` |

**Properties**

| 路径 | 消息 |
|---|---|
| 重复/冲突（点号键展开时） | `Duplicate key: <完整点号键>` |
| 空键列表（防御分支，不可达） | `invalid key: <key>` |
| 其余键/深度错误 | 同公共表 |

**门面（DefaultConfigurationFacade / ConfigurationFactory 触发）**

| 路径 | 消息 |
|---|---|
| `conf("")` / `confList("")` | `invalid key: ` |
| 类型不匹配（conf 命中 List/Object 等） | `invalid key: <key>, type : <CfgItem\|CfgList\|CfgObject>` |
| 中间段非对象 | `invalid key: <key>, current nested key: <段>, type : <类型名>` |
| 扩展名非法（jing.config.ext） | `unsupported configuration file extension: <ext>` |
| 所有候选文件均缺失 | `configuration file not found, filename : <fileName> , exts: <[toml, json, properties]>` |
| 解析抛 IOException / 承载异常 | 包装为 `ExceptionInInitializerError`（Holder 类初始化失败，后续访问抛 `NoClassDefFoundError`） |
| 配置数值转换失败（非空白非法串） | `NumberFormatException`（ConfigurationFactory 不捕获） |
| key 为 null | `NullPointerException`（门面无守卫） |

## 6. 明确不支持的子集清单

**JSON**
- 根必须为对象：顶层数组/字符串/数字/null 均拒绝。
- `null` 值不支持（首字节 `n` → `Corrupted json configuration`）。
- 键限制为 `[a-zA-Z0-9_-]`：非 ASCII 键、含空格/标点键、转义键（`\uXXXX` 或 `\"` 内嵌）均拒绝。
- 数组元素仅标量：`[[]]`、`[{}]` 拒绝；尾逗号 `[1,]`、`{"a":1,}` 拒绝。
- 数字无 `+` 号、无下划线、无十六进制/八进制/二进制、无 `NaN`/`Infinity` 字面量。
- 宽容（非规范但接受）：字符串内裸控制字符；未配对代理的 U+FFFD 替换；非法 UTF-8 字节替换。

**TOML**
- 字符串仅双引号基本形式：单引号字面量 `'...'`、多行 `"""`/`'''` 均以 `Corrupt toml configuration` 或空串+杂散字节路径拒绝。
- 无 dotted key（`a.b = 1` → `invalid key byte: 46`），嵌套仅通过 `[a.b]` 表头。
- 无日期/时间/datetime 值类型。
- 数组仅标量元素，无嵌套数组/对象；无尾逗号。
- 键字符集同 JSON 限制（无引号键）。
- `inf`/`nan`/`true`/`false` 仅小写字母；`infinity`、`NAN` 等组合拒绝。
- 表头重复检测仅精确匹配整串；`[a.b]` 后 `[a]` 的父表重定义**不报错**（合并行为，与 TOML 规范有出入）。
- 注释仅 `#`；tab 允许；C1 控制字符（0x80-0x9F）在注释内不校验。

**Properties**
- 键字符集同 JSON 限制；含 `.` 的键按段展开，其他分隔符（`:`、`=`、空格）由 `Properties.load` 消解后再校验。
- flat 层重复键后写覆盖（Properties 语义），点号展开后的结构冲突拒绝。
- 输出键顺序不保证。

---

## 附录 A：功能变动范围（vs 提交 3ce7a07）

### A.1 统计摘要

本会话所有改动（含 v4 重构 + 修复轮 + 测试补充 + 文档更新）与 HEAD 提交 `3ce7a07` 的对比：

| 类别 | 文件数 | 新增行 | 删除行 | 备注 |
|---|---|---|---|---|
| 主实现 | 4 | +796 | -415 | CfgReader 387+/299-；CfgUtil 371+/43-；Facade 34+/69-；ConfigurationFactory 4+/4- |
| 测试 | 5（3 改 2 删） | +152 | -110 | JsonCfgReaderTest +57/-33；TomlCfgReaderTest +59/-39；PropertiesCfgReaderTest +36/-30；CfgDataModelTest -27（已删）；CfgUtilTest -54（已删） |
| 新增文件（未计入 diff） | 3 | — | — | CfgTest.java（10 用例）、ConfigurationFactoryTest.java（5 用例 11 断言）、doc/Configuration-v4.md |
| 文档 | 2 | +48 | -12 | Configuration-v2.md（§5 格式子集修正）；diff.md（27 行新增） |
| **合计** | **14** | **~1000** | **~540** | 净增 ~460 行 |

### A.2 功能变动表（行为级 before → after）

以下列出**功能语义变更**（非纯结构性/风格调整），按影响面排序：

| # | 变动项 | 3ce7a07 提交时状态 | 当前状态 | 影响面 |
|---|---|---|---|---|
| 1 | **JSON 数字/布尔支持** | `{"a":1}` / `{"a":true}` → `Corrupted json configuration`（v2 时期不支持） | `CfgItem("1")` / `CfgItem("true")`（严格文法规范化） | 所有含数字/布尔值的 JSON 配置均可解析；新增 ~100 用例覆盖 |
| 2 | **TOML 数字/布尔/inf-nan** | `a = 1` / `a = true` / `a = inf` → `Corrupted toml configuration` | `CfgItem("1")` / `CfgItem("true")` / `CfgItem("Infinity")`；十六进制/八进制/二进制统一换算十进制 | 新增 ~80 用例覆盖 |
| 3 | **截断 JSON 静默接受** | `{"a":1`（缺 `}`）→ `CfgObject{a: CfgItem("1")}`（漏接受） | → `CfgException("EOF reached")`（严格拒绝） | 向后不兼容：依赖此行为的调用方将收到异常 |
| 4 | **TOML 注释 EOF 误报** | `a = 1\n# comment`（无结尾换行）→ `CfgException("EOF reached")`（误拒绝合法输入） | → 成功返回（注释到 EOF 容忍） | 修复回归：恢复被错误拒绝的合法配置 |
| 5 | **用户输入异常统一为 CfgException** | `onValueEnd`/`readCfgKey`/`readCfgNestedKey`/`Properties case 0`/空表头 → `AssertionError`（会 crash JVM） | → `CfgException`（可捕获、描述明确） | 所有非法 TOML 值/空键路径/空表头不再导致 JVM 崩溃 |
| 6 | **maxDepth 语义统一** | JSON 用 `frames.size() >= maxDepth`（off-by-one 与 TOML/Properties 不一致）；`<=0` 未处理 | 统一为"根=第 1 层，层级 ≤ maxDepth 允许，> 拒绝；≤0 抛异常" | 配置嵌套深度约束三格式一致；边界行为明确 |
| 7 | **空数组支持** | `[]` / `a = []` → `Corrupted json/toml configuration`（readScalarValue 遇 `]` 报错） | → `CfgList[]`（合法空列表） | 数组首元素为空的配置均可解析 |
| 8 | **尾随垃圾拒绝** | `{"a":1}x` → `CfgObject{a: CfgItem("1")}`（静默忽略尾随内容） | → `CfgException("Corrupted json configuration")` | 严格拒绝非空白尾随字节；潜在安全改善 |
| 9 | **confList 不可变拷贝** | `confList("key")` 直接返回内部 `ArrayList` 的引用（调用方可修改） | 返回 `List.copyOf()`（每次新拷贝，不可变） | 向后兼容但行为变化：调用方不可再修改返回的列表 |
| 10 | **blank 值视为缺省** | `confAsInt("key", 5)` 遇空白值 → `NumberFormatException`（逃逸） | → 返回 `5`（blank → default，与 `conf(key, default)` 对齐） | 空白配置值不再引发转换异常 |
| 11 | **navigate 抽取 + 消息统一** | `conf()`/`confList()` ~40 行重复；`"invalid key : "` 混用 `"invalid key: "` | 共享 `navigate()`（~20 行）；消息统一为 `"invalid key: "` | 依赖异常消息内容的调用方（如日志匹配）需适配 |
| 12 | **构造器守卫规范化** | `CfgUtil` 私有构造 → `AssertionError` | → `UnsupportedOperationException("utility class")` | 仅反射/序列化误用场景 |
| 13 | **isValidCodePoint 清理** | JSON `\u`/TOML `\x`/`\u` 分支冗余检查 | 删除恒真分支（`\U` 8 位保留） | 纯内部清理，无行为变化 |
| 14 | **v4 method-per-state 重构** | CfgReader ~559 行巨型 switch | ~583 行 method-per-state（每状态独立方法，Frame record） | 行为等价，纯结构优化 |
| 15 | **空 key 守卫** | `readCfgKey`/`readCfgNestedKey` 空输入 → `AssertionError` | → `CfgException("empty key")`；门面层前置检查 `conf("")` → `CfgException("invalid key: ")` | 向后兼容：原来 crash 现在异常 |

### A.3 向后兼容风险

| 风险 | 严重度 | 说明 |
|---|---|---|
| 截断 JSON 现在抛错 | **中** | 原先静默成功，现在抛 CfgException。如果业务代码依赖 `{"a":1` 能解析，会收到异常。修复路径：补齐闭合括号。 |
| confList 返回不可变列表 | **低** | 原先返回可变引用；如果业务代码尝试修改返回值，会收到 UnsupportedOperationException。 |
| `"invalid key : "` → `"invalid key: "` | **低** | 如果日志/测试用正则/字符串匹配异常消息，需同步调整。 |
| 无效输入由 AssertionError → CfgException | **高**（修复向） | 原先 crash JVM，现在抛可捕获异常。任何原来 catch AssertionError 的代码无需改；但原先无守卫导致 JVM 崩溃的路径现在有明确异常。 |
| TOML/JSON 空数组合法 | **无** | 原先拒绝，现在接受。不会破坏现有合法配置。 |
| maxDepth 语义修正 | **低** | JSON 边界行为微调（off-by-one 修正）；现有配置极不可能恰好落在边界上。 |
