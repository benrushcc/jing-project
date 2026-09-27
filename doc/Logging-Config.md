# jing-log 配置项参考（以实现为准）

> 本文件是 **jing-log 当前实现**（`jing-log/src/main`）中所有「与配置项有关的部分」的
> 完整导出，**逐条对照源码消费点**，仅供查阅，不构成任何代码变更。
>
> 与 `doc/Logging.md`（设计文档 v3/v4，含 §D10 配置总表）的关系：Logging.md 是
> **设计侧**的配置规划（键名/语义以设计意图表述）；本文件是 **实现侧**的事实性
> 配置清单（键、默认值、校验、报错、消费行号均以当前 Java 实现核对为准）。
> 当两者不一致时，以本文件（实现）为准。
>
> 本模块所有配置统一经 `jing-common` 的 `ConfigurationFactory` 读取
> （`ConfigurationFacade` SPI，默认 `DefaultOldConfigurationFacade` 按

> `toml → json → properties` 顺序探测配置文件）。jing-log 自身不加载、不解析
> 配置文件，只声明「消费哪些键 + 默认值 + 校验」。
>
> **键名规范**：全部为 `jing.log.*` 前缀；键中数字/枚举/布尔经 `confAs*` 解析；
> 数值范围键经 `parseInt/parseLong` 校验。任何非法值在 **handler 构造期（启动即校验）**
> 抛 `IllegalArgumentException`，消息统一 `<key> : unsupported value '<x>'` 或
> `<key> : value out of range [min, max], actual : n`。

---

## 1. 键总表（实现消费的全部配置项）

> 默认值 = 实现中的 `DEFAULT_*` 常量 / `conf(key, default)` 第二参。
> 消费点 = `jing-log/src/main/java/io/jingproject/log/` 下的读取行。

| 键 | 默认 | 取值 / 范围 | 校验报错 | 检验 | 校验行 |
|---|---|---|---|---|---|
| `jing.log.level` | `INFO` | `TRACE DEBUG INFO WARN ERROR`（大小写不敏感） | `jing.log.level : unsupported value '<x>'` | facade 全局阈值 | LoggerFacadeImpl:43,47 |
| `jing.log.handlers` | `[console]` | `console` `file`（confList） | `jing.log.handlers : unknown handler id '<x>'` | 激活 handler，顺序即顺序 | LoggerFacadeImpl:99,248,261 |
| `jing.log.batch.max` | `1024` | 正整数 `[1, MAX_INT]` | `jing.log.batch.max : invalid integer value '<x>'` / `jing.log.batch.max : value out of range [1, 2147483647], actual : n` | 单批上限（仅上限，不凑批） | LoggerFacadeImpl:49 |
| `jing.log.arena.size` | `16777216` | 正整数（字节）`[4096, MAX_INT]` | `jing.log.arena.size : invalid integer value '<x>'` / `jing.log.arena.size : value out of range [4096, 2147483647], actual : n` | `ArenaBase` 保留虚拟内存 | LoggerFacadeImpl:50 |

> `jing.log.level` 与 `jing.log.handlers` 由 `LoggerFacadeImpl` 消费；另见 §2 各 handler 自有键。

---

## 2. Console handler（`ConsoleLogEventHandler.java`，id=`console`）

| 键 | 默认 | 取值 / 范围 | 校验报错 | 默认常量 | 校验行 |
|---|---|---|---|---|---|
| `jing.log.ansi` | `true` | `true` `false` | — | ConsoleLogEventHandler:92 | :92 |
| `jing.log.console.pattern` | `FormatPattern.DEFAULT_PATTERN` | 格式串（D6 语法） | 解析失败 → `FormatPattern` 异常带位置 | :93 | :93 |
| `jing.log.console.err.level` | `WARN` | `TRACE`..`ERROR`（大小写不敏感）或 `none` | `jing.log.console.err.level : unsupported value '<x>'` | :94 | :102 |
| `jing.log.level.color`（通用） | `""`（无色） | 颜色名或逗号组合 | `jing.log.level.color : unsupported color name : <x>` | :105 | :174 |
| `jing.log.level.trace.color` `.debug.color` `.info.color` `.warn.color` `.error.color` | 继承全局或空 | 同上 | 同上 | :110 | :174 |
| `jing.log.timezone` | `""`=系统默认 | 任意 JDK `ZoneId` 名 | `jing.log.timezone : unsupported zone id '<x>'` | :228 | :235 |

> 颜色语义：`jing.log.ansi=false` 或对应颜色为空 → 不输出颜色；逐级颜色（`trace`..
> `error`）覆盖全局；`jing.log.level.color` 为全局默认、`jing.log.level.<level>.color`
> 覆盖指定等级。颜色名取值见 §4。

---

## 3. File handler（`FileLogEventHandler.java`，id=`file`）

| 键 | 默认 | 取值 / 范围 | 校验报错 | 默认常量 | 校验行 |
|---|---|---|---|---|---|
| `jing.log.file.path` | `logs/jing.log` | 非空路径 | `jing.log.file.path must not be null` | :157 | :132 |
| `jing.log.file.pattern` | `FormatPattern.DEFAULT_PATTERN` | 格式串 | 同 §2 pattern | :158 | :146-151 |
| `jing.log.file.format` | `log` | `log` `json` | `jing.log.file.format : unsupported value '<x>'` | :159 | :696 |
| `jing.log.file.rotation` | `size` | `off` `size` `time` | `jing.log.file.rotation : unsupported value '<x>'` | :160 | :696 |
| `jing.log.file.rotate.size` | `10485760` | 正整数（字节） | `jing.log.file.rotate.size : invalid long value '<x>'` / `jing.log.file.rotate.size : value out of range [1, ...], actual : n` | :161 | :705,710 |
| `jing.log.file.rotate.period` | `daily` | `daily` `hourly` | `jing.log.file.rotate.period : unsupported value '<x>'` | :162 | :696 |
| `jing.log.file.maxBackups` | `7` | 非负整数 `[0, MAX_INT]` | `jing.log.file.maxBackups : invalid integer value '<x>'` / `jing.log.file.maxBackups : value out of range [0, 2147483647], actual : n` | :163 | :706,710 |
| `jing.log.file.maxAge` | `30` | 非负整数（天，0=不删） | `jing.log.file.maxAge : invalid integer value '<x>'` / `jing.log.file.maxAge : value out of range [0, 2147483647], actual : n` | :164 | :706,710 |
| `jing.log.file.flush` | `interval` | `off` `interval` `immediate` | `jing.log.file.flush : unsupported value '<x>'` | :165 | :696 |
| `jing.log.file.flush.interval` | `1000` | 正整数（ms） | `jing.log.file.flush.interval : invalid long value '<x>'` / `jing.log.file.flush.interval : value out of range [1, ...], actual : n` | :166 | :707,710 |
| `jing.log.timezone` | `""`=系统默认 | 任意 JDK `ZoneId` 名 | `jing.log.timezone : unsupported zone id '<x>'` | :720 | :727 |

> `json` 格式忽略 pattern，固定 schema 手工拼接；`log` 格式才有
> `pattern`/`pattern` 校验。rotation 见 `Rotation`，period 见 `Period`（§4）。

---

## 4. 校验/报错消息与枚举值（实现为准）

- **数值**（`parseInt` / `parseLong`）：`<key> : invalid integer value '<x>'`
  或 `<key> : invalid long value '<x>'`；越界 →
  `<key> : value out of range [<min>, <max>], actual : <n>`（FileLogEventHandler.java
  `:705-710` 与 `:266-269` 两种消费路径）。
- **枚举**（`parseEnum`）：`<key> : unsupported value '<x>'`
  （`ConsoleLogEventHandler.java:174` 与 `FileLogEventHandler.java:696` 共用模式；
  枚举含 `Format`、`Rotation`、`Period`、`FlushMode`、`LogLevel`、颜色名）。
- **颜色名**（`jing.log.*.color`）：`black red green yellow blue magenta cyan white bold`
  及逗号组合；未知 → `jing.log.level.color : unsupported color name : <x>`。
- **时区**（`jing.log.timezone`）：`ZoneId.of`；`DateTimeException` →
  `jing.log.timezone : unsupported zone id '<x>'`。
- **报错统一**：handler 构造期静态初始化即 fail-fast；消息恒以键开头，如
  `jing.log.console.err.level : unsupported value 'x'`。

---

## 5. 默认常量定义处（jing-log 源码，作为默认值来源）

| 常量 | 值 | 文件:行 |
|---|---|---|
| `LoggerFacadeImpl.DEFAULT_BATCH_MAX` | `1024` | LoggerFacadeImpl:27 |
| `LoggerFacadeImpl.DEFAULT_ARENA_SIZE` | `16777216` | LoggerFacadeImpl:28 |
| `LoggerFacadeImpl.DEFAULT_HANDLERS` | `[console]` | LoggerFacadeImpl:29 |
| `ConsoleLogEventHandler.DEFAULT_GLOBAL_COLOR` | `""` | ConsoleLogEventHandler:26 |
| `FormatPattern.DEFAULT_PATTERN` | `%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n` | FormatPattern:30 |
| `FileLogEventHandler.DEFAULT_PATH` | `logs/jing.log` | FileLogEventHandler:39 |
| `FileLogEventHandler.DEFAULT_ROTATE_SIZE` | `10485760` | FileLogEventHandler:40 |
| `FileLogEventHandler.DEFAULT_MAX_BACKUPS` | `7` | FileLogEventHandler:41 |
| `FileLogEventHandler.DEFAULT_MAX_AGE` | `30` | FileLogEventHandler:42 |
| `FileLogEventHandler.DEFAULT_FLUSH_INTERVAL` | `1000` | FileLogEventHandler:43 |
