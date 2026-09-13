# jing-ffm 模块审查报告

> 审查日期：2026-09-13
> 范围：`jing-ffm`（运行时）、`jing-ffm-processor`（注解处理器），及其在 `jing-bindings` / `jing-log` 中的消费方式
> 性质：纯审查与建议，未修改任何代码

## 模块概览

- **jing-ffm**（运行时）：`@FFM`/`@Downcall` 注解、`Libs` 统一入口（搜索路径 → ServiceLoader 发现 `LibFacade` → 静态块急切加载所有库与符号 → 惰性生成 stub）、`NativeSegmentAccess`（原始地址内存访问 + jing_result ABI 辅助）。
- **jing-ffm-processor**：为每个 `@FFM` 接口生成 `XxxLibFacade`（`@Provider` SPI）+ 内部 `Impl`，用 `List.ofLazy` 按方法惰性绑定 `MethodHandle`。
- 总体设计清晰、意图明确（轻量 jextract 替代），代码风格统一、注释规范。核心问题集中在**错误处理、库去重、类型一致性、处理器校验缺口**四个方面。

---

## 严重（正确性问题）

### 1. `boolean` 类型被处理器拒绝，但运行时完整支持
- **证据**：`FfmProcessor.castFfmReturnType`（`FfmProcessor.java:345-363`）与 `castFfmParameterType`（`:365-382`）的 switch **缺少 `case BOOLEAN`**；而运行时 `Libs.castMemoryLayout`（`Libs.java:225-226`）和 `makeConstantMethodHandle`（`Libs.java:184-185`）都支持 `boolean`。
- **影响**：文档（`README.md:91` "the eight primitives"）与注解约束（"primitives or MemorySegment"）均宣称支持全部 8 种基本类型，但任何声明 `boolean` 参数/返回的绑定接口会直接编译失败（抛 `UnsupportedOperationException`）。运行时与处理器能力不一致。
- **建议**：在 `castFfmReturnType`/`castFfmParameterType` 补齐 `case BOOLEAN -> "boolean"`。若有意不支持，则改文档为 7 种类型。**倾向补齐，因为运行时已支持，差异是纯漏洞。**

### 2. `PosixBindings` 缺少 `@FFM` 注解 → Linux/macOS 上必然 NPE
- **证据**：`PosixBindings.java:7` 接口级**没有** `@FFM`（只有方法级 `@Downcall`）。处理器只扫描 `@FFM` 注解类型（`FfmProcessor.java:48-50, 55`），因此不会为它生成 facade。而 `PosixMmap.java:25`、`EpollMux.java:15`、`KqueueMux.java:15` 直接取 `Libs.impl(PosixBindings.class)` 并立即解引用。
- **影响**：`impl()` 查不到 descriptor 时返回 `null`（`Libs.java:261-267`），POSIX 平台上首次调用 `SYS_POSIX_BINDINGS.xxx()` 即 NPE。当前主平台是 Windows（`AGENTS.md` 要求先保证当前平台），所以未被暴露——这是一个**跨平台定时炸弹**。此外这些 `@Downcall` 全是死代码。
- **建议**：补上 `@FFM(libraryName = "jing_bindings", supportedOS = {Os.LINUX, Os.MACOS})`。同时确认 `LinuxBindings`/`MacosBindings` 与 `PosixBindings` 的方法划分是否符合预期（当前各绑定接口是并列的，未复用 PosixBindings）。

### 3. `Libs.impl()` 返回 `null` + README 推荐的 `static final` 模式 → 消费方静默 NPE
- **证据**：`README.md:129-139` 推荐 `private static final MyLib MY_LIB = Libs.impl(...)`；`Libs.impl` 在库缺失/平台不支持时返回 `null`（`Libs.java:263-266`）。消费方 `Mem.java:12-16` 做了 null 检查（良好示范），但 `WinConsoleLogEventHandler.java:11`、`PosixMmap.java:25`、`EpollMux.java:15`、`KqueueMux.java:15` 都**没有检查**。
- **影响**：与模块自身"库/函数缺失时程序仍能启动"的设计承诺矛盾——缺失函数有 `ForeignException` 兜底（`Libs.java:169-177`），缺失库却是裸 NPE，且发生在任意首次调用点，堆栈难定位。
- **建议**：把"缺失库"也纳入错误句柄机制。既然每个函数缺失时都会生成抛清晰异常的 error 句柄，可以为整个缺失的库生成一个同样语义的 impl 桩（调用即抛 `ForeignException("library not found: xxx")`），彻底消灭 null 传播。这是与现有行为最一致、改动最小的方案。

---

## 高（健壮性/设计）

### 4. 同一物理库被每个 facade 重复打开
- **证据**：`Libs.java:39-59` 的去重键是 `facade.target()`（即 `@FFM` 接口类型），不是库。`jing_bindings` 的 `CommonBinding`/`CompBindings`/`LogBindings`（Windows 上再加 `WinBindings`）每个都会独立执行 `searchLibrary`（`:46`）+ `SymbolLookup.libraryLookup`（`:50`）。
- **影响**：启动时重复文件探测与库打开，多个 `Arena.global()` 句柄持有同一库。
- **建议**：按映射后的 `(mappedName, libPath)` 建立一次性缓存，同一库只 open 一次、共享一个 `LibDescriptor` 和 `functions` 映射，不同 facade 只向其追加各自符号。

### 5. 单个库打开失败 → 全局静态初始化崩溃
- **证据**：`SymbolLookup.libraryLookup`（`Libs.java:50`）无 try-catch。库存在但无法打开（依赖缺失、格式错误）时抛异常，导致 `Libs` 类初始化失败（`ExceptionInInitializerError`），**全部**绑定不可用。
- **影响**：与"某个库坏掉不影响其他库"的设计意图冲突。目前只有"库文件不存在"被安全跳过（`:47-49`），"打开失败"没有隔离。
- **建议**：按库 try-catch 隔离加载，失败库不注册（或注册为空），用日志门面记录原因（符合 AGENTS.md 禁止 `System.out` 的约定）。

### 6. `MacosBindings` 库名不一致（`jing-bindings` vs `jing_bindings`）
- **证据**：`MacosBindings.java:9` 用连字符 `"jing-bindings"`，其余接口全用下划线 `"jing_bindings"`（`WinBindings.java:9`、`LinuxBindings.java:7`、`CommonBinding.java:8` 等）。
- **影响**：`System.mapLibraryName` 后 macOS 上映射为 `libjing-bindings.dylib`，与下划线命名的构建产物 `libjing_bindings.dylib` 不匹配 → macOS 上该绑定**必然静默跳过**（`Libs.java:47-49`）。macOS 平台从未被验证过，这大概率是个潜在失效点。
- **建议**：统一为 `jing_bindings`。

### 7. constant 方法的异常双重包裹
- **证据**：缺失符号 → `makeErrorMethodHandle`（`Libs.java:169-177`）生成抛 `ForeignException("native method not found : x")` 的句柄 → `makeConstantMethodHandle` 立即 `invokeExact()`（`:183` 等）→ catch-all 再包一层 `ForeignException("failed to invoke constant foreign method")`（`:203-205`）。
- **影响**：真实的"符号缺失"错误被二次包装，`ForeignException` 套 `ForeignException`，消息重复，排查困难。
- **建议**：对已知的 `ForeignException` 原样重抛，只包装其他 `Throwable`；并在包一层时保留 cause 链条。

---

## 中（API 设计 / 处理器校验）

### 8. `types` 列表的"index 0 = 返回类型"约定脆弱
- **证据**：`Libs.mhFromVM`/`mhFromLib`（`Libs.java:126, 143`）接受 `List<Class<?>> types`，约定首元素是返回类型、其余是参数——公开 API 无文档说明，与 `FunctionDescriptor` 的直观顺序（先参数后返回）相反，极易误用。
- **建议**：引入小 record，如 `record Sig(Class<?> resType, List<Class<?>> paramTypes)`，或在 javadoc 明确约定并给示例。

### 9. 编译期校验缺口（处理器只查"形式"，不查"语义"）
- `constant` + 带参数：运行期才抛（`Libs.java:152-154`）→ 应移到 `FfmProcessor.checkFfmElement`（`:75-114`）。
- `constant` + `void` 返回：运行期走到分支才炸（`Libs.java:200-201`）→ 编译期应拒绝。
- 非法参数/返回类型：处理器抛 `UnsupportedOperationException`（`FfmProcessor.java:359, 361, 378, 380`）而非 `AnnotationProcessorException`，javac 报错信息质量差。
- **建议**：在 `checkFfmElement` 统一校验参数/返回类型集合、`constant` 约束、`methodName` 非空，全部抛 `AnnotationProcessorException`。

### 10. 继承的接口方法被静默忽略
- **证据**：`FfmProcessor` 只遍历 `t.getEnclosedElements()`（`FfmProcessor.java:87, 123`），从父接口继承的抽象方法不会被处理（不校验也不生成）。
- **影响**：当前消费方都是平铺接口（`PosixBindings` 本想被复用但没被复用），暂无实际影响，但使用者若让 `@FFM` 接口继承其他接口会得到"方法缺失"的困惑。
- **建议**：文档明确"不支持继承"，或遍历超接口并合并校验。

### 11. `NativeSegmentAccess` 职责错位：泛用运行时内嵌特定 ABI
- **证据**：`NativeSegmentAccess.java:24-56`（`jing_result` 结构布局）、`:57, 317-323`（错误指针编码 `0x8000000000000000`）、`:273-315`（`rByte`/`rErrCode`/`rLen` 系列）全是 **jing_bindings 特定**的 C ABI；`:77-267` 的原始地址 get/set 辅助函数与 `jing-common` 的 `SegmentAccess`（`:80-240`）**完全重复**；`:59-65` 的 `ensureInitialized(Os.class)` hack 也在 `SegmentAccess.java:68-74` 复制了一份。
- **建议**：原始内存访问收敛到 `SegmentAccess`（jing-common）；`jing_result`/err-ptr 相关移入 `jing-bindings` 或独立模块，让 jing-ffm 保持泛用性。若不重构，至少把 `ensureInitialized` 集中到一处并注释其用途。

### 12. `Linker.Option.critical(false)` 语义陷阱
- **证据**：`Libs.java:160-161`。该 API 的参数是 `allowHeapAccess`（是否允许堆访问），`critical(false)` 实际含义是**"开启 critical + 禁止堆访问"**——代码行为正确，但字面极易被误读为"非 critical"，未来维护者可能当成 bug 改掉。
- **建议**：提取命名良好的局部常量（如 `CRITICAL_NO_HEAP`）或加一行注释；README 的 `jing.ffm.critical` 章节已说明禁止堆访问是刻意的，与代码呼应一下更好。

---

## 低（风格/文档/细节）

| # | 问题 | 证据 | 建议 |
|---|------|------|------|
| 13 | `JING_LIBRARY_PATH` 按**单目录**处理，`java.library.path` 却按 `File.pathSeparator` 分割 | `Libs.java:90-98` | 统一语义：要么文档写明"单个目录"，要么也支持多路径分割 |
| 14 | 生成代码中 `methodNames` 的 `List.of(...)` 全部字面量挤一行，多方法接口（`WinBindings` 二十余个方法）超 120 列，违反 AGENTS.md | `FfmProcessor.java:217-237` | 每行一个元素 |
| 15 | 生成 facade 的构造器 `GUARD` 单例防御（`AtomicBoolean`）——SPI 机制本身只实例化一次，属过度设计 | `FfmProcessor.java:146-170` | 可简化；至少说明用途 |
| 16 | `LogBindings` 是零方法空接口，作用是隐式强制加载 `jing_bindings` 库 | `LogBindings.java:5-8` | 加注释说明意图，或提供显式 `Libs.ensureLoaded(...)` |
| 17 | `jing-ffm` 与 `jing-ffm-processor` **均无测试**（git 历史 `6b9c59f` 将测试迁往 jing-bindings） | 无 `src/test` | 处理器是代码生成器，建议补 JUnit 编译测试：合法生成、缺 `@Downcall`、varargs、throws、sealed、非法返回类型、`constant`+带参、继承方法、`boolean` 等负例 |
| 18 | 错误消息语法/信息质量 | `FfmProcessor.java:110`（"Only non-type parameters method..."）；`Libs.java:172`（错误句柄消息不含库名） | 修语法；消息带上库名/接口名以区分多库同名符号 |
| 19 | `Linker.nativeLinker()` 每次调用都新建 | `Libs.java:121, 159` | 缓存为 `static final`（调用频率低，属顺手优化） |
| 20 | README 未说明运行期需要 `--enable-native-access`（`reinterpret`/`libraryLookup`/`downcallHandle` 均属 restricted API） | `NativeSegmentAccess.java:327`；`Libs.java:50, 161` | 在 README "运行/构建" 章节补充 JVM 启动参数说明 |

---

## 最高杠杆修改建议（Top 5）

1. **补齐 `boolean` 支持**（或改文档）——运行时与文档、处理器三方拉齐，一行 switch 的事（严重 #1）。
2. **给 `PosixBindings` 补 `@FFM`**——消灭跨平台 NPE 定时炸弹（严重 #2）。
3. **库缺失也走 error 桩**（消 null 传播）+ **按库去重加载 + 库失败隔离**——三项合并解决 `Libs` 静态块的健壮性（严重 #3 + 高 #4/#5）。
4. **把 `@Downcall` 语义校验移入处理器**，统一抛 `AnnotationProcessorException`，并修掉双包裹异常（中 #9 + 高 #7）。
5. **收敛 `NativeSegmentAccess`**：raw 访问并入 `SegmentAccess`，jing_result ABI 移出泛用模块（中 #11）。