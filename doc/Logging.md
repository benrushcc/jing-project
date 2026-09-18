# jing-log 异步日志实现方案与规格 (v2)

Status: design / not yet implemented
Scope: `jing-log` 模块为主，附带 `jing-bindings` 新增的日志 I/O native binding 设计。
本文档是对 jing-log 模块具体实现的完整规格。`jing-common` 的日志门面
（`io.jingproject.common.log`）与默认兜底 logger 已经实现，不属于本文档范围。

---

## 0. 背景与现状

### 0.1 目标

- 日志打印链路整体异步：`log(LogLevel, String, Throwable)` 只负责构造 `LogEvent`
  并投递到队列，绝不阻塞调用方线程。
- 消费端固定单线程，从队列批量拉取事件处理；批大小只是一道上限，有多少事件就
  处理多少。
- 具体如何消费由 `LogEventHandler` 决定，启用哪些 handler 由配置
  （`ConfigurationFactory`）决定，各 handler 通过 consumer 线程的 ScopedValue 拿到
  共享的 `ArenaBase` 进行堆外内存写入。
- 侧重 FFI 性能：复用一块 mmap 大内存，格式化直接写入堆外（连续内存天然适合
  扩容写入），每批尽量只发起一次系统调用。
- `LoggerImpl` 的 level 用 `MutableCallSite` 做分发，获得更好的 JIT 折叠机会。

### 0.2 已存在的骨架（保持/复用）

| 文件 | 现状 |
|---|---|
| `jing-common/.../log/Logger.java` | 接口，含 `void log(LogLevel, String, Throwable)` 与 debug/info/error 默认方法，保持不变 |
| `jing-common/.../log/LogLevel.java` | TRACE(-7355608)/DEBUG(-60937)/INFO(0)/WARN(60937)/ERROR(7355608)，`value()`、`fromString(String)`，保持不变 |
| `jing-common/.../log/LoggerFacade.java` | SPI：`Logger getLogger(Class<?>)` |
| `jing-common/.../log/LoggerFactory.java` | 静态入口，ServiceLoader + 默认 console 兜底；其持有的 `LazyConstant` 为 JDK 28 `java.base` 的 `java.lang.LazyConstant`，无编译问题 |
| `jing-log/.../LogEvent.java` | `record LogEvent(LogLevel level, OffsetDateTime offsetDateTime, String className, String threadName, String msg, Throwable throwable, Map<String,Object> logContext)`，保持不变 |
| `jing-log/.../LogEventHandler.java` | **接口签名需改造**：改为处理 `List<LogEvent>`（§2 D4） |
| `jing-log/.../LoggerFacadeImpl.java` | `LoggerFacade` + `LifeCycle`，持 `MpscLinkedQueue<LogEvent>` 与未启动线程，`start()/stop()` 为空，`LOGGING_CONTEXT` ScopedValue 已声明 |
| `jing-log/.../LoggerImpl.java` | `MutableCallSite` level 分发已实现；`log()` 为 TODO 空壳 |
| `jing-log/.../WinConsoleLogEventHandler.java` | 实验残留，作为旧 native 方案的一部分将被移除，由新的 `ConsoleLogEventHandler` 取代 |
| `jing-log/src/main/resources/META-INF/services/...LoggerFacade` + `module-info.java` | 手工维护的 SPI 注册，保持不变 |

### 0.3 与 v1 文档的差异（本次修订要点）

1. ~~LazyConstant 编译阻塞~~ 不存在：`java.lang.LazyConstant` 是 JDK 28 `java.base`
   正式类（`java/lang/LazyConstant.java`），`jing-common` 可正常编译。
2. 日志 I/O 的 native 与 binding 全部**重新按需设计**：旧 `WinBindings`/
   `PosixBindings` 中的文件与控制台相关函数属实验性质，不进入正式设计。正式需要
   哪些 native 调用，见 §2 D8（核心章节）。
3. handler 共享 Arena：通过 consumer 线程绑定的 ScopedValue（复用 jing-bindings
   已有的 `Allocator.MMAP_SCOPE`）+ `Allocator.newAlloc()` 获取堆外写入能力。
4. 消费唤醒模型：队列为空时消费线程 `park()`，生产端每次投递后 `unpark()`。
5. 移除 v1 中 `AbstractBatchLogEventHandler` 与可复用堆缓冲的思路：不再经堆缓冲
   中转，直接格式化进堆外；mmap 空间不足时 flush 一次即可腾出空间继续。
6. ANSI 颜色不做平台探测：配置里有颜色配置即生效；提供全局开关 `jing.log.ansi`
   一键禁用。
7. 配置习惯大改：细粒度单项配置（如 `jing.log.level.color`、
   `jing.log.level.error.color`），颜色不再靠格式串表达，简化用户配置负担。
8. DateTimeFormatter 支持预设名（如 `ISO_OFFSET_DATE_TIME`），命中预设直接使用，
   未命中才按自定义模式解析。
9. JSON 输出不依赖任何库（包括 jing-marshall），直接按固定 schema 手工拼接。
10. 批大小只作为上限；`LogEventHandler` 直接处理 `List<LogEvent>`。

---

## 1. 总体架构

```
                    ┌────────────────────────  生产端（任意线程）──────────────┐
 LoggerFactory → Logger  ── log(level,msg,thr) ──> new LogEvent(...) ──> MPSC queue
 (jing-common)   (LoggerImpl)                      [level 由 MutableCallSite 折叠]      │
                                    [logContext: LOGGING_CONTEXT 绑定? 快照 : Map.of()] │
                                                                                        │
  ┌─────────────── 消费端（单线程 jing-log-consumer）───────────────────────────────┴─
  │  queue 空 → LockSupport.park()            ← 生产端每次 offer 后 unpark()
  │  drain 到空（单批 ≤ batch.max）→ List<LogEvent>
  │     ↓
  │  ScopedValue.where(Allocator.MMAP_SCOPE, arenaBase).run(处理体)   // 整轮绑定
  │     ↓
  │  对每个激活 handler 依次 handle(batch)：
  │     各自 try (Allocator alloc = Allocator.newAlloc()) 直接从共享 ArenaBase 分配
  │     格式化直接写入堆外 region（光标式、容量受检）
  │     单批 ≤ 少数几次系统调用（Windows WriteFile / POSIX write）
  │     region 无法容纳 → 先 flush 一次腾出空间再继续
  └──────────────────────────────────────────────────────────────────────────────
```

---

## 2. 设计决策

### D1. 队列与消费线程（park/unpark）

- 复用 `jing-common` 的 `MpscLinkedQueue<LogEvent>`（无锁 MPSC，无界，v1 无背压）。
- 消费线程为平台 daemon 线程，名 `jing-log-consumer`，在 `LoggerFacadeImpl.start()`
  中创建；`stop()` 中停止。
- 消费循环契约：
  1. 队列为空 → `LockSupport.park()`；被唤醒（spurious 也接受）后先查队列。
  2. 从队列反复 `poll()`：单批最多 `jing.log.batch.max` 个事件；一批处理完毕后若
     队列仍有事件，立即继续拉下一批（不 park），直到队列空。
  3. 一批事件作为 `List<LogEvent>` 依次交给所有激活的 handler 处理，然后回到 poll。
- 生产端：`queue.offer(event)` 之后立刻 `LockSupport.unpark(consumerThread)`。
  park/unpark 语义天然防丢失唤醒（unpark 先于 park 时记为许可）。
- 队列在 `start()` 前投递的事件同样可见，start 后一并消费（无界队列，天然安全）。

### D2. 生产端：`LoggerImpl.log()`

```
public void log(LogLevel level, String msg, Throwable throwable) {
    if (level == null) throw new IllegalArgumentException("level must not be null");
    if (!enabled(level)) return;                      // MutableCallSite 快路径折叠
    LogEvent event = new LogEvent(
        level,
        OffsetDateTime.now(),
        className,                                   // 构造时预计算一次
        Thread.currentThread().getName(),
        msg,
        throwable,
        LoggerFacadeImpl.LOGGING_CONTEXT.isBound()
            ? LOGGING_CONTEXT.get()                   // 借用当前作用域的值（应视为不可变）
            : Map.of());                             // 未绑定 → 空不可变 Map
    queue.offer(event);
    LockSupport.unpark(consumerThread);
}
```

- `log()` 自带 `enabled` 检查，避免直接调用 `log()` 时把低于阈值的日志也入队。
  `Logger` 默认方法中的 debug/info 门控保留。
- `className` 在 `LoggerImpl` 构造时取 `clazz.getCanonicalName()` 存一次。
- LogContext（MDC 语义）：`LOGGING_CONTEXT` 是 `LoggerFacadeImpl` 的静态
  `ScopedValue<Map<String,Object>>`；`log()` 实现读它：绑定则把该值放入事件的
  `logContext`，未绑定则放 `Map.of()`。事件中的 `logContext` 永不 null。
- `OffsetDateTime.now()` 每事件一次，v1 接受（consideration：可换 `Instant`）。

### D3. 消费端批量处理与生命周期

- `LoggerFacadeImpl`：
  - 字段：`ArenaBase arena`、消费线程、激活 handler 列表、`volatile boolean running`。
  - `start()`（幂等）：读配置并校验（fail fast）→ 创建 `ArenaBase` →
    实例化激活 handler → 启动消费线程；线程体 = 
    `ScopedValue.where(Allocator.MMAP_SCOPE, arena).run(消费循环)`。
  - `stop()`（幂等）：`running=false` → `unpark` 消费线程 → `join` → 消费线程退出
    前最后一次 drain + 所有 handler `flush()` + 关闭 handler（文件句柄等）→
    `arena.close()`。
- 一个批量源 `List<LogEvent>` 依次交给所有 handler；handler 只读事件。

### D4. `LogEventHandler` 接口

```java
public interface LogEventHandler {
    void handle(List<LogEvent> events);      // 尽量一次 handle 处理一批

    default void flush() {}                  // 由消费端按策略定期/停止时调用
}
```

- 去掉 `@FunctionalInterface` 与单事件 `handle`；这是 jing-log 模块内私有 API。
- 每个 handler 的 `handle` 在消费线程内执行（ScopedValue 可见），每批内部自主
  决定分配/格式化/写出节奏。

### D5. 共享 ArenaBase 与堆外写入模型

**获取方式（通用机制）**：消费线程的整轮处理体包在
`ScopedValue.where(Allocator.MMAP_SCOPE, arena).run(...)` 里。handler（及任何需要
堆外内存的组件）通过 `Allocator.newAlloc()` 拿到分配器：
- ScopedValue 已绑定 → 返回 `ArenaAllocator`（bump 分配，over 共享 `ArenaBase`）；
- 未绑定（如单元测试）→ 回退 `MallocAllocator`，行为不变。

**每批写入契约**（handler 内实现）：

1. `try (Allocator alloc = Allocator.newAlloc()) { ... }`。
2. 记录批次起点 `regionStart = arena.pos()`；后续对同批事件连续分配 region，由于
   `ArenaBase` 是 mmap 保留的一段**地址连续**空间 + bump 分配，各 region 首尾相邻，
   本批累计写入形成一段连续虚拟内存。
3. 每个事件用编译期固定的格式化 method handle **直接**写入 region
   （`MemorySegment` + 偏移 + 容量上限），不经过堆缓冲中转；写满一 region 后继续
   在下一相邻 region 写入。
4. 批末（或策略触发 flush 时）对整段连续区发起一次系统调用：
   - Windows：`WriteFile(handle, seg, len, result)`；
   - POSIX：`write(fd, seg, len, result)`。
   因此每批的系统调用次数 ≈ 每次 flush 次数（无不足时 1 次）。
5. **容量不足处理**：格式化某事件时若剩余容量不够（或 arena 已到 bound），
   立即把已格式化部分 flush 出去（写一次），随后 `alloc.close()`（`shrink` 回卷，
   空间清出），再重新分配新 region 继续处理当前及后续事件。每次容量不足只多一次
   系统调用 + 一次内存回卷。
6. `alloc.close()` → `arenaBase.shrink(pos)` → 下一批从起点重新 bump，复用同一块
   mmap 内存，无反复 mmap/munmap、无每次分配的 malloc/free。

**格式化器的容量保证**：格式化 method handle 返回"本事件实际写入字节数"，或返回
`-1` 表示"此 region 剩余容量不足"（此时该事件尚未写入任何字节即可判定）。判定
规则：先按最坏情况估算再写（见 D7.4），保证单事件要么完整写入连续区，要么
完全未写（可安全触发 flush 重试）。单个事件超过整个 arena 时降级为丢弃并上报
（§2 D9）。

### D6. 格式化模式语言（严格子集，无颜色字）

颜色不再由格式串表达（由配置控制，见 D7.3），因此模式语言比 logback 更严格也更
简单。静态初始化期完成解析、校验与编译；任何非法模式都在启动期失败。

#### 词法

```
pattern    := segment*
segment    := literal | conversion
literal    := 除 '%' 外任意字符；'%%' 输出字面 '%'
conversion := '%' width? specifier options?
width      := '-'? digits          （仅 level/logger 支持，见 specifier 表）
specifier  := 'date'|'d' | 'thread'|'t' | 'level'|'p' | 'logger'|'c'
            | 'msg'|'m' | 'ex'|'throwable' | 'n' | 'X'|'context'
options    := '{' 见各 specifier '}'
```

#### 各 specifier

| 记号 | 源 | options | 说明 |
|---|---|---|---|
| `%date` / `%d` | `offsetDateTime` | `{预设名或模式}`，默认 `yyyy-MM-dd HH:mm:ss.SSS` | 预设：JDK `DateTimeFormatter` 常量名，命中直接用常量；未命中按模式经 `DateTimeFormatterBuilder.parsePattern` 校验构造，非法字母 → 初始化期异常。预设清单见下 |
| `%thread` / `%t` | `threadName` | 无 | |
| `%level` / `%p` | `level` | `width`：`%-5level` 左对齐补空格，`%5level` 右对齐；无宽度不补 | level 名称字节（含 ANSI 色码）初始化期预计算一次 |
| `%logger` / `%c` | `className` | `{N}` 缩写预算，N≥1 | 缩写规则见下 |
| `%msg` / `%m` | `msg` | 无 | null 输出 `null` |
| `%ex` / `%throwable` | `throwable` | 无 | 渲染完整堆栈以 `\n` 分行；null 时输出空串；容量不足时截断并追加 `...`（见 D7.4） |
| `%n` | — | 无 | 恒输出单字节 `0x0A`（不做平台换行转换） |
| `%X{key}` / `%context{key}` | `logContext` | `{key}` 必填 | `String.valueOf(value)`；缺 key / 空 Map → 空串 |

#### 日期预设清单

`ISO_LOCAL_DATE`、`ISO_OFFSET_DATE`、`ISO_DATE`、`ISO_LOCAL_TIME`、
`ISO_OFFSET_TIME`、`ISO_TIME`、`ISO_LOCAL_DATE_TIME`、`ISO_OFFSET_DATE_TIME`、
`ISO_ZONED_DATE_TIME`、`ISO_DATE_TIME`、`ISO_INSTANT`、`BASIC_ISO_DATE`、
`RFC_1123_DATE_TIME`。

匹配规则：`%d{值}` 先与该清单做大小写不敏感比对，命中 → 使用对应
`DateTimeFormatter` 常量；未命中 → 视为自定义模式文本，交给
`DateTimeFormatterBuilder.parsePattern` 构造（非法模式字母在初始化期抛异常）。

#### `%logger{N}` 缩写规则

1. 全名长度 ≤ N → 原样输出。
2. 否则：最后分段（类名）保持完整；自前向后逐段取"首字母 + `.`"，占用预算直到
   剩余预算不足，此时停止（宁短不截断类名）。
3. 仅一段且长度 > N → 直接截断到 N。

#### 严格性规则（与 logback 的差异，逐一列举）

1. 未知 specifier（`%foo`）→ 初始化期异常，消息带出错位置下标。
2. `width` 必须为数字；`-` 后无数字、options 里给 width、数字非法 → 异常。
3. 不支持 options 的 specifier 出现 `{}`、`{N}` 中 N 非法 → 异常。
4. 日期模式经 `parsePattern` 校验，未知模式字母 → 异常。
5. 孤立行尾 `%` → 异常。
6. `%n` 恒为 `0x0A`。
7. 字面文本不做 trim，空白原样保留。

#### 默认模式

- Console：`%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n`
- File（log 格式）：同上。

#### D6.4 编译期 MethodHandle 格式化器

在 handler 的 static 初始化中完成：

1. 解析模式 → 节点序列（AST）。
2. 常量部分预编码为 `byte[]`：字面文本、`%n`、level 名称字节（含宽度与 ANSI 色码
   包裹）。
3. 动态部分绑定最快路径：
   - `%date`：绑定构造好的 `DateTimeFormatter`，通过 `formatTo(temporal, Appendable)`
     直接写 region（region 适配为 Appendable，写 region 即写堆外，零堆中转）；
   - `%thread` / `%logger` / `%msg`：UTF-8 直接编入 region（复用 CharsetEncoder /
     直接逐字符编码，不产生堆 String）；
   - `%ex`：`Throwable.printStackTrace(PrintStream)` 写 region（region 适配
     OutputStream）；输出量估算计入最坏情况（见下）。
4. 用 `MethodHandles` 组合子链成单一 method handle：
   `long fill(MemorySegment region, long offset, long capacity, LogEvent event)`
   → 新 offset；容量不足 → `-1`（写前即判，保证无部分写入）。
   容量判定按"每节点最坏字节数"累计：string 类字段按 `len*3`（UTF-8 上界）、日期按
   固定上界、`%ex` 按 region 剩余空间上限处理（写满即截断，见 D6.4）。
5. 存入 `static final`，运行期零解析、零常量重算。

handler 类惰性加载：static 初始化发生在 `start()` 实例化 handler 时，模式/预设/
颜色等配置错误在启动期立刻暴露（fail fast）。

### D7. `ConsoleLogEventHandler`

- 取代并删除实验残留 `WinConsoleLogEventHandler`。跨平台：Windows 用新 binding
  （§2 D8），POSIX 用新 binding。
- 输出流路由：`jing.log.console.err.level`（默认 `WARN`）——level 达到该值及以上的
  事件写入 stderr，其余写 stdout；值 `none` 时全部走 stdout。stderr 与 stdout 是
  两个独立输出目标，批末各自 flush 自己的 region（每批至多 2 次系统调用）。
- **ANSI 颜色（仅 console，仅 level 词）**：
  - 全局开关 `jing.log.ansi`（默认 `true`）：`false` 时所有颜色配置失效。
  - 颜色只作用于 `%level` 的输出，样式由配置给出，不再出现在格式串里：
    - `jing.log.level.color`：所有 level 的通用颜色；缺省 → 无颜色；
    - `jing.log.level.trace.color` / `.debug.color` / `.info.color` / `.warn.color` /
      `.error.color`：逐 level 覆盖通用值；缺省 → 用通用值（或无色）。
  - 颜色值：`black` `red` `green` `yellow` `blue` `magenta` `cyan` `white` `bold`，
    支持逗号组合（如 `red,bold`），映射到 SGR 前景色码（30–37 / 1）。空字符串或
    缺省 = 无色。
  - 不做任何平台/终端探测：只要 `jing.log.ansi=true` 且对应 level 有颜色配置，
    即输出 `\x1b[<code>m` + level 名 + `\x1b[0m`。
- 批内写出：`WriteFile`/`write` 到 stdout/stderr 句柄，C 侧做全量写入循环；
  Java 侧检查 `jing_result` 错误标志，失败走 §2 D9 失效处理。
- 无需 flush（终端写本身无缓冲语义），`flush()` 为空实现。

### D8. FileLogEventHandler 与 Native API 设计（重点章节）

#### D8.1 功能规格

- 追加写；`start()` 时用 JDK `Files.createDirectories` 建父目录（启动期、非热路径）。
- 文件名：`jing.log.file.path`（默认 `logs/jing.log`）；`format=json` 且路径无已
  识别后缀时自动补 `.json`。
- 旋转：`jing.log.file.rotation` = `off` | `size` | `time`（默认 `size`）。
  - `off`：单文件，不旋转（仍遵守 flush 策略）。
  - `size`：活跃文件为基本名；`writtenBytes + 本批字节 > jing.log.file.rotate.size`
    时轮转：`基本名.1.扩展名` ← 旧活跃文件，编号顺移
    （`基本名.N → 基本名.N+1`），超出 `jing.log.file.maxBackups` 的最旧删除；
    顺移循环实现（禁止递归）。尺寸通过 open 时查询一次 + 之后 Java 侧累计写入
    字节数跟踪（见 D8.3 fileSize 用途）。
  - `time`：周期 `jing.log.file.rotate.period` = `daily`|`hourly`；当前文件名 =
    基本名 + `.` + `yyyy-MM-dd`（或 `yyyy-MM-dd-HH`）；批次开始前检查周期键是否
    过期，过期则 flush+close → 打开新周期文件。时钟使用注入的 `Clock`（默认系统
    时区），可测。
- 保留时限：`jing.log.file.maxAge`（天，默认 30，`0` = 不删）。`start()` 与每次
  旋转时扫描匹配归档命名模式的文件，删除 `maxAge` 前修改的文件（`Files.list` +
  `getLastModifiedTime`）。
- flush 策略：`jing.log.file.flush` = `off` | `interval` | `immediate`
  （默认 `interval`，间隔 `jing.log.file.flush.interval` ms，默认 1000）。
  - `interval`：距上次 flush 超过间隔时，批末 flush；
  - `immediate`：每批 flush；
  - `off`：不主动 flush；
  - `stop()` 与旋转关闭前必定 flush。
- 文件内容格式 `jing.log.file.format`：
  - `log`：`jing.log.file.pattern`（语法同 D6，无颜色）。
  - `json`：**固定 schema 手工拼接，零依赖（含 jing-marshall）**。每个事件一行
    JSON 对象，UTF-8 输出，`\n` 结尾。字段顺序固定：
    `{"ts":..,"level":..,"thread":..,"logger":..,"msg":..,"ctx":{..},"ex":..}`
    - 必有：`ts`（ISO_OFFSET_DATE_TIME 文本）、`level`、`thread`、`logger`、`msg`；
    - 可选：`logContext` 非空时输出 `ctx`；`throwable` 非 null 时输出 `ex`（堆栈
      渲染为单字段字符串，内部换行转义为 `\n` 字面）；
    - 转义规则：`"` → `\"`，`\` → `\\`，`\n`/`\r`/`\t`/`\b`/`\f` → 对应转义，
      其余 <0x20 控制字符 → `\uXXXX`。手工按字符判跳转义，直接写入 region。

#### D8.2 Native 重新按需设计——结论先行

旧的实验 binding（`WinBindings` 控制台/文件、`PosixBindings` 缺失项）一律不沿用。
正式实现需要在 `jing-bindings` 新增两个 `@FFM` 接口并配套 native 实现。以下列表
即**完成任务所需的全部** native 调用，一项不多：

需要的 I/O 原语 = 控制台句柄(仅 Win) + 打开追加 + 全量写 + flush + 尺寸(仅文件) +
关闭。

用 `jing_result` 16 字节结构承载返回值与错误（沿用现有 ABI，`NativeSegmentAccess`
访问）；文件名以 UTF-8 空终止字符串传入（Windows C 侧自行转 UTF-16）。

##### Windows —— 新 binding `WinLogIoBindings`

`@FFM(libraryName = "jing_bindings", supportedOS = {Os.WINDOWS})`

| Java @Downcall | C 函数 | 语义 |
|---|---|---|
| `long stdoutHandle()` `constant,critical` | `jing_log_win_stdout_handle()` | `GetStdHandle(STD_OUTPUT_HANDLE)`，类加载期求值 |
| `long stderrHandle()` `constant,critical` | `jing_log_win_stderr_handle()` | `GetStdHandle(STD_ERROR_HANDLE)` |
| `void openAppend(MemorySegment fileNameCStr, MemorySegment jingResult)` | `jing_log_win_open_append(const char*, jing_result*)` | `CreateFileW(GENERIC_WRITE, FILE_APPEND_DATA, OPEN_ALWAYS)`，handle 经 result 返回 |
| `void write(long handle, MemorySegment buf, int len, MemorySegment jingResult)` | `jing_log_win_write(HANDLE, const char*, size_t, jing_result*)` | `WriteFile` **全量写入循环**（处理短写），字节数经 result 返回 |
| `void flush(long handle, MemorySegment jingResult)` | `jing_log_win_flush(HANDLE, jing_result*)` | `FlushFileBuffers` |
| `void fileSize(long handle, MemorySegment jingResult)` | `jing_log_win_file_size(HANDLE, jing_result*)` | `GetFileSizeEx`，供 size 旋转初始值 |
| `void close(long handle, MemorySegment jingResult)` | `jing_log_win_close(HANDLE, jing_result*)` | `CloseHandle` |

##### POSIX —— 新 binding `PosixLogIoBindings`

`@FFM(libraryName = "jing_bindings", supportedOS = {Os.LINUX, Os.MACOS})`

| Java @Downcall | C 函数 | 语义 |
|---|---|---|
| `void openAppend(MemorySegment fileNameCStr, MemorySegment jingResult)` | `jing_log_posix_open_append(const char*, jing_result*)` | `open(path, O_WRONLY\|O_CREAT\|O_APPEND, 0644)`，fd 经 result 返回 |
| `void write(int fd, MemorySegment buf, long len, MemorySegment jingResult)` | `jing_log_posix_write(int, const char*, size_t, jing_result*)` | `write` **全量写入循环**（处理 EINTR/短写） |
| `void flush(int fd, MemorySegment jingResult)` | `jing_log_posix_flush(int, jing_result*)` | `fsync` |
| `void fileSize(int fd, MemorySegment jingResult)` | `jing_log_posix_file_size(int, jing_result*)` | `fstat` st_size |
| `void close(int fd, MemorySegment jingResult)` | `jing_log_posix_close(int, jing_result*)` | `close` |

##### 明确不做成 native 的事（理由一并记录）

- **POSIX stdout/stderr fd**：POSIX 规定 `STDOUT_FILENO=1`、`STDERR_FILENO=2` 恒为
  常量，直接以整数 1/2 使用，不需要 binding。Windows 的句柄必须在运行期
  `GetStdHandle` 获取（可被重定向），故保留两个常量 binding。
- **文件旋转的改名/删除/列举/mtime**：旋转与清理发生在消费线程的边界时刻，属
  冷路径；用 JDK `java.nio.file.Files`（`move`/`delete`/`list`/茎取 mtime）即可，
  不为它扩大 native 面。
- **目录创建**：`Files.createDirectories` 启动期一次调用。
- **ANSI 检测 / isatty / terminal capability**：按需求不做探测（§0.3-6）。
- **时钟 / 时间函数**：Java `Clock`/`OffsetDateTime` 已满足旋转与时间戳需求。
- **内存分配相关**：arena 的 mmap/VirtualAlloc/commit 等沿用现有 `ArenaBase`
  底层（该部分 infra 保留复用，不重设计）。
- 上述 C 函数在 jing_bindings native 库中新增 `jing_log_win.c` /
  `jing_log_posix.c`（或并入现有 `jing_win.c`/`jing_posix.c`，实现自由，但函数名
  统一 `jing_log_*` 前缀）。

### D9. 错误处理

- handler 构造期（static 初始化）全部校验 fail fast：模式、预设置、颜色、旋转/
  flush/format 枚举、handler id。枚举错误用统一的 `CfgException`（含键与非法值）。
- 写失败（`jing_result` 错误标志）：不抛给调用方。消费线程 catch per-handler：
  标记该 handler 失效，后续批次跳过；通过 `System.err` 输出一次降级错误（与
  `DefaultLogger` 的 System.out 类似，属蓄意降级输出）。不中断消费循环。
- 单事件超过整个 arena → 丢弃并上报一次（同上降级通道）。

### D10. 配置支持总表（细致版）

读取一律走 `ConfigurationFactory`。合法性：字符串枚举在启动期（handler 实例化 /
facade 构造）校验，非法值抛 `CfgException("unsupported <item> : <value>")`；
布尔/数值沿用 `confAsBoolean`/`confAsInt` 语义（空白 → 默认，解析失败抛异常）。

| key | 可提供的每一个值 | 默认 | 语义 |
|---|---|---|---|
| `jing.log.level` | `TRACE` `DEBUG` `INFO` `WARN` `ERROR`（大小写不敏感） | `INFO` | 全局阈值；`LogLevel.fromString` |
| `jing.log.handlers` | `console` `file`（confList） | `[console]` | 激活 handler，顺序即调用顺序；未知 id → 启动失败 |
| `jing.log.batch.max` | 正整数 | `1024` | 单批上限（仅上限，不凑批） |
| `jing.log.arena.size` | 正整数（字节） | `16777216` | `ArenaBase` 保留虚拟内存 |
| `jing.log.ansi` | `true` `false` | `true` | 全局 ANSI 开关；`false` 时颜色配置一律不输出 |
| `jing.log.level.color` | `black` `red` `green` `yellow` `blue` `magenta` `cyan` `white` `bold` 或逗号组合；空=无色 | 无 | `%level` 通用颜色 |
| `jing.log.level.trace.color` | 同上 | 无 | 覆盖通用值（trace） |
| `jing.log.level.debug.color` | 同上 | 无 | 覆盖通用值（debug） |
| `jing.log.level.info.color` | 同上 | 无 | 覆盖通用值（info） |
| `jing.log.level.warn.color` | 同上 | 无 | 覆盖通用值（warn） |
| `jing.log.level.error.color` | 同上 | 无 | 覆盖通用值（error） |
| `jing.log.console.pattern` | 格式串（D6 语法） | `%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n` | console 格式 |
| `jing.log.console.err.level` | `TRACE` `DEBUG` `INFO` `WARN` `ERROR` `none` | `WARN` | ≥ 此 level 走 stderr；`none` → 全走 stdout |
| `jing.log.file.path` | 文件路径 | `logs/jing.log` | 相对路径基于工作目录 |
| `jing.log.file.pattern` | 格式串（D6 语法） | 同 console 默认 | 文件格式（`log` 模式） |
| `jing.log.file.format` | `log` `json` | `log` | `json` 忽略 pattern，固定 schema 手工拼接 |
| `jing.log.file.rotation` | `off` `size` `time` | `size` | 轮转模式 |
| `jing.log.file.rotate.size` | 正整数（字节） | `10485760` | size 模式阈值 |
| `jing.log.file.rotate.period` | `daily` `hourly` | `daily` | time 模式周期 |
| `jing.log.file.maxBackups` | 非负整数 | `7` | size 模式归档编号上限 |
| `jing.log.file.maxAge` | 非负整数（天）；`0`=不删 | `30` | 归档保留时限（两模式皆适用） |
| `jing.log.file.flush` | `off` `interval` `immediate` | `interval` | flush 策略 |
| `jing.log.file.flush.interval` | 正整数（ms） | `1000` | `interval` 模式间隔 |

### D11. 测试与验证

测试放 `jing-log/src/test`（JUnit，模块化布局参照 jing-common 的 open 测试模块
模式，实现自由）。handler 提供包内测试后门：可注入假 `Clock`、可替换的写目标
（capture 字节），ScopedValue 未绑定 arena 时自动回退 MallocAllocator，天然可测。

1. **模式解析器**：合法模式编译；未知 specifier / 坏 width / 非法 `{N}` / 非法日
   期字母 / 孤立 `%` → 异常带位置。
2. **日期预设**：`%d{ISO_OFFSET_DATE_TIME}` 等 13 个预设命中；未命中按自定义模式
   构造；大小写不敏感。
3. **格式化 golden**：固定 LogEvent + 固定模式 → 逐字节比对（伪配置色码开/关、
   宽度、`%logger{N}` 缩写、`%X`、null msg、`%ex`、容量不足返回 `-1`）。
4. **Console**：stderr 路由边界；颜色配置生效矩阵（通用 vs 逐级、`ansi=false`、
   空值无色）；每批系统调用次数（用 capture 写目标计数）。
5. **File**：size 轮转编号与 maxBackups；time 轮转（假 Clock 跨周期）；maxAge 删除；
   json schema golden（含转义、ctx/ex 可选字段）；flush 三策略触发时机；stop 最终
   flush。
6. **异步集成**：`LoggerImpl.log()` 后事件最终出现（轮询断言）；level 门控 /
   `setLevel` 动态生效；多生产者并发无丢失；park/unpark 及时消费（投递后短时限
   内出现）；停止后队列残留被最终 drain。
7. **构建验证**：`mvnd clean install -DskipTests=true` 后 `mvnd -pl jing-log test`；
   启动期 fail-fast 用例（坏模式 / 坏 handler id / 坏颜色名）。
8. **基准**（`jing-bench`，类名 `LogBench`）：log() 入队吞吐；消费端批量格式化
   吞吐；格式化 MH ns/op；批级单写 vs 逐事件写。运行：
   `java --enable-preview --add-modules jdk.incubator.vector -jar ./jing-bench/target/benchmarks.jar LogBench`

---

## 3. 非目标（v1 明确不做）

- 无异步文件 I/O（io_uring/IOCP）；写调用由消费线程同步发出。
- 无 logback 全集：无过滤器、无嵌套 appender、无独立 async 队列（本实现自身即
  异步）、无 XML/专用配置文件；配置只来自 `ConfigurationFactory`。
- 无颜色语法高级形式（rgb、背景色、格式串内色词、括号分组、终端探测）。
- 不做堆内事件池 / 堆缓冲中转（堆外直写）。
- 不做队列背压/丢弃策略（无界队列）——见 §4 开放问题。
- JSON 只用固定 schema，不做通用 JSON 序列化框架集成。

## 4. 开放问题与 consideration（记档，不进 v1）

- **背压**：日志峰值可打爆内存。备选：有界队列 + 丢弃计数（facade 提供
  `dropped` 计量）或按 level 丢弃。v1 取无界。
- **时间戳成本**：`OffsetDateTime.now()` 每事件一次；可换 `Instant` + 消费端分区
  计算。
- **logContext**：`Map.of()` 与绑定快照两种形态并存；快照语义需文档明确
  （取值引用、应视为不可变）。
- **`%ex` 截断**：超容量时截断追加 `...`，与"容量不足 flush 重试"的策略在此字段上
  例外（栈轨迹不可重放、易超界），后续可改为单独溢出文件。
- **环境变量/系统属性**：`jing.log.level` 目前走配置文件；是否兼容现有
  `System.getProperty("jing.log.level")` 语义待定。
- **消费唤醒优化**：park/unpark 已够用；如需降延迟可引入条件变量式信号量，
  当前不必须。

## 5. 实施顺序

严格按阶段推进，每阶段以编译 + 该阶段测试收尾（§2 D11 对应项）。

- **Phase 0（native）**：`jing-bindings` 新增 `WinLogIoBindings` /
  `PosixLogIoBindings` 两个 `@FFM` 接口 + `jing_log_win.c` / `jing_log_posix.c`
  实现 + binding 测试。验证：`mvnd clean install -DskipTests=true` 全量编译。
- **Phase 1（消费核心）**：`LogEventHandler` 接口改 `handle(List)` +
  `flush()`；`LoggerFacadeImpl` 的 start/stop + park/unpark 消费循环 +
  `ScopedValue.where(MMAP_SCOPE, arena)` 绑定；`LoggerImpl.log()` 接线
  （含 logContext 快照）。验证：异步集成测试 1 组 + LogLevelTest。
- **Phase 2（格式化引擎）**：D6 严格解析器（含日期预设）+ 编译期 MethodHandle
  fill（容量受检）+ 测试。
- **Phase 3（Console handler）**：D7 全量（路由、颜色配置、region 直写、
  D8 Windows/POSIX 写目标）+ 测试。
- **Phase 4（File handler）**：D8.1 全量（打开/旋转/保留/flush/log+json）+ 测试。
- **Phase 5（收尾）**：基准 `LogBench`、启动 fail-fast 清单核对、全量回归。
- 改动后按仓库惯例在根目录 `diff.md` 追加中文变更说明。