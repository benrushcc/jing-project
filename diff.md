# Changes

## 2026-09-13T23:51:24+0800
- Modified jing-common/conf/CfgReader.java: 注释规范化——JsonCfgReader 中 10 处中文注释(parse 方法上的"构建出来的Reader只能parse一次"与 enum State 各状态说明)全部改为纯英文双斜杠注释,符合项目注释规范(纯英文、// 形式、小写开头)
- 规范化后 jing-common 全部 36 个单元测试通过

## 2026-09-13T22:07:37+0800
- Modified jing-common/conf/Cfg.java: CfgItem/CfgList/CfgObject 三个 record 实现类移入 Cfg 密封接口内部成为嵌套类型,删除独立文件,类型全名变为 Cfg.CfgItem/Cfg.CfgList/Cfg.CfgObject
- Modified jing-common/conf/CfgReader.java: JsonCfgReader/PropertiesCfgReader/TomlCfgReader 三个实现类移入 CfgReader 密封接口内部成为嵌套类型,删除独立文件,类型全名变为 CfgReader.JsonCfgReader/CfgReader.PropertiesCfgReader/CfgReader.TomlCfgReader
- Fixed jing-common/conf/CfgReader.java: 修复 JsonCfgReader 的 STR_ARR_OBJ_END 状态在字符串/数组值结束后无条件弹出父节点的问题——改为先读后续字节再分支:',' 留在当前对象内继续读键、'}' 才弹出父节点挂载完成的嵌套对象、EOF 在顶层返回结果/非顶层抛 CfgException,修复嵌套对象含多个键值对时后续键被错误挂到根级的问题(原实现仅单键嵌套对象恰好能工作)
- Modified jing-common/conf/DefaultConfigurationFacade.java: 导入改为 Cfg.CfgItem/Cfg.CfgList/Cfg.CfgObject 与 CfgReader.JsonCfgReader/CfgReader.PropertiesCfgReader/CfgReader.TomlCfgReader
- Modified jing-common 测试: JsonCfgReaderTest/TomlCfgReaderTest/PropertiesCfgReaderTest 导入改为嵌套类型,新增 parse(String)/parse(String, int) 双辅助方法;三个测试类各新增 testMaxDepthExceeded(嵌套层数超过 maxDepth=5 断言抛 CfgException)与 testComplexMixed(数组+嵌套对象混合用例,三种格式等价断言);CfgDataModelTest 导入改为 Cfg.CfgItem/Cfg.CfgList/Cfg.CfgObject
- 修复后 jing-common 全部 36 个单元测试通过(原 30 个 + 新增 6 个)

## 2026-09-13T21:43:15+0800
- Added jing-common/conf/CfgReader.java: 新增 sealed interface CfgReader,提供 CfgObject parse(int maxDepth) throws IOException,由 JsonCfgReader/PropertiesCfgReader/TomlCfgReader 实现
- Modified jing-common/conf/JsonCfgReader.java: 实现 CfgReader,parse 增加 maxDepth 参数,对象嵌套深度检查改用参数(替换原 CfgUtil.maxDepth())
- Modified jing-common/conf/PropertiesCfgReader.java: 实现 CfgReader,parse 增加 maxDepth 参数,readCfgNestedKey 传入 maxDepth
- Modified jing-common/conf/TomlCfgReader.java: 实现 CfgReader,parse 增加 maxDepth 参数,表路径 readCfgNestedKey 传入 maxDepth
- Modified jing-common/conf/CfgUtil.java: 移除 MAX_DEPTH 相关静态字段与 resolveMaxDepth/parseMaxDepth/maxDepth 方法(归属移到 facade);readCfgNestedKey 增加 maxDepth 参数,段数检查改用参数
- Modified jing-common/conf/DefaultConfigurationFacade.java: MAX_DEPTH 的赋值与范围校验统一在 static 块内完成——从环境变量 JING_CONFIG_MAX_DEPTH 读取,Integer.parseInt 解析,默认 128,必须大于 4(≤4 抛 ExceptionInInitializerError);createConfiguration 三处 parse 传入 MAX_DEPTH,conf/confList 的 readCfgNestedKey 传入 MAX_DEPTH
- Modified jing-common 测试: JsonCfgReaderTest/TomlCfgReaderTest/PropertiesCfgReaderTest 的 parse() 改为 parse(128),CfgUtilTest 的 readCfgNestedKey 增加 128 参数,适配新接口签名

## 2026-09-13T21:17:57+0800
- Fixed jing-common/conf/DefaultConfigurationFacade.java: 扩展名校验从误用系统属性变量 fileExt 改为循环变量 ext,修复默认加载路径(jing.config.ext 未设置)必抛 "unsupported configuration file extension" 的问题;嵌套 key 遍历从 subList(1, size-1) 改为 subList(0, size-1),修复 conf/confList 查询嵌套 key(如 server.port)恒返回 null 的问题;删除未使用的 MAX_DEPTH 字段
- Modified jing-common/conf/CfgUtil.java: MAX_DEPTH 改为从环境变量 JING_CONFIG_MAX_DEPTH 读取,默认 128,必须大于 4(非法值或 ≤4 抛 CfgException),校验逻辑抽为公开静态方法 parseMaxDepth;readCfgNestedKey 的 key 段数限制改用新值
- Modified jing-common/conf/JsonCfgReader.java: 新增对象嵌套深度检查,嵌套层数达到 MAX_DEPTH 时抛 CfgException
- Modified jing-common/conf/Cfg.java/CfgItem.java/CfgList.java/CfgObject.java: 移除 type() 方法,报错信息改用 getClass().getSimpleName() 直接打印类型
- Modified jing-common/.../CfgDataModelTest.java: 删除 testTypes 测试(type() 已移除)
- 顺带修正 DefaultConfigurationFacade 中文注释与 CfgUtil 中大写开头的异常消息,符合项目规范

## 2026-09-13T19:35:00+0800
- Fixed jing-common/conf/TomlCfgReader.java: VALUE_END 状态改用 CfgUtil.ignore(input, ' ', '\t') 跳过行尾空白后只读一个字节再分支,修复原实现用 ignore 跳过全部换行/注释字节导致多行 TOML(多键值、表、嵌套表)抛 AssertionError 的问题,重复键/重复表检测不再被掩盖
- Fixed jing-common/conf/JsonCfgReader.java: STR_ARR_OBJ_END 顶层(parent==null)分支不再解析完第一个值立即返回,改为读取后续字节——',' 进入 EXPECT_KEY、'}' 或 EOF 返回结果、其余抛 CfgException,修复多顶层键只解析第一个、重复键检测被绕过的问题
- 修复后 jing-common 全部 31 个单元测试通过(含新增 30 个配置加载测试)

## 2026-09-13T18:55:05+0800
- Modified jing-common/module-info.java: 将 io.jingproject.common.conf 限定导出给 jing.commontest 测试模块,使单元测试可直接访问内部解析器实现
- Added jing-common/src/test/java/io/jingproject/commontest/TomlCfgReaderTest.java: 新增 TOML 解析器单元测试,用固定字符串直接构造 InputStream,覆盖单键值、多键值、表、嵌套表、数组、注释、Unicode 转义、重复键、重复表、非法键
- Added jing-common/src/test/java/io/jingproject/commontest/JsonCfgReaderTest.java: 新增 JSON 解析器单元测试,覆盖单键值、多键、嵌套对象、数组、Unicode 转义、代理对、重复键、损坏输入
- Added jing-common/src/test/java/io/jingproject/commontest/PropertiesCfgReaderTest.java: 新增 properties 解析器单元测试,覆盖简单键值、嵌套键、数组、注释、重复键后者覆盖
- Added jing-common/src/test/java/io/jingproject/commontest/CfgUtilTest.java: 新增 CfgUtil 工具单元测试,覆盖 readCfgKey/readCfgNestedKey/readUnicode/writeUnicodeInUtf8/rejectKey
- Added jing-common/src/test/java/io/jingproject/commontest/CfgDataModelTest.java: 新增 Cfg 数据模型单元测试,覆盖 type() 与 asImmutable()
- 测试发现两个解析器缺陷(暂不修复,仅记录):TomlCfgReader 的 VALUE_END 状态用 CfgUtil.ignore 跳过全部换行/注释字节,多行 TOML 会抛 AssertionError;JsonCfgReader 的 STR_ARR_OBJ_END 在顶层(parent==null)解析完第一个值后立即返回,多顶层键只解析第一个

## 2026-09-13T18:24:37+08:00
- Added doc/Configuration-v2.md: 根据当前 jing-common 配置模块源码重新确定设计,生成全英文设计文档,记录 SPI 可插拔架构、ConfigurationFactory/ConfigurationFacade/DefaultConfigurationFacade 分层、Cfg sealed 数据模型、toml/json/properties 三种解析器子集、key 规则与加载机制,并记录三个已知问题(扩展名校验变量误用、嵌套 key 遍历跳过首段、未使用的 MAX_DEPTH)

## 2026-09-13T16:35:00+08:00
- Modified jing-common/anno/Fragile.java: 类注释从 Javadoc 改为 `//` 风格并润色,明确 @Fragile 类在设定上不当使用会导致 JVM 崩溃,内部无需过度防御性编程,只需保证正确输入下输出正确结果
- Modified jing-common/anno/Provider.java: 类注释与 target() 元素注释从 Javadoc 改为 `//` 风格并润色
- Modified jing-common/anno/ProcessorApi.java: 类注释从 Javadoc 改为 `//` 风格并润色
- Modified jing-common/anno/Generated.java: 类注释从 Javadoc 改为 `//` 风格并润色,{@link} 引用改为纯文本描述 Utils.generateClassName
- Modified AGENTS.md: 在 Code Conventions 中新增 @Fragile 设计语义说明——不当使用会导致崩溃,无需过度防御性编程,只需保证正确输入下正确输出
- Modified jing-ffm/Downcall.java: 在 constant 元素补充注释,说明 constant 会改变函数调用行为——在类加载时即完成初始化并执行第一次调用,而非延迟到首次调用,以便更好地实现常量折叠
- Modified jing-ffm/README.md: 在 @Downcall 的 constant() 说明中补充其改变调用行为(类加载时初始化并首次调用,非延迟到首次调用)以支持常量折叠的描述

## 2026-09-13T16:21:19+08:00
- Modified jing-ffm/Libs.java: 在 JING_CRITICAL 处补充注释,说明 critical 选项强烈建议开启,对特别短的调用提升明显;仅当遇到因开启 critical 导致的问题时才建议关闭以提高稳定性

## 2026-09-13T16:12:51+08:00
- Modified jing-ffm/NativeSegmentAccess.java: 更新类注释,说明 jing-common 已有 SegmentAccess 负责 MemorySegment 读写,而 NativeSegmentAccess 专门针对堆外内存、直接通过指针偏移访问,相比 SegmentAccess 节省一次边界检查开销,但更易出错,故以 @Fragile 标记

## 2026-09-13T16:04:29+08:00
- Modified jing-bindings/src/main/native/src/jing_ssl.h/.c: 条件编译宏从错误的 JING_USE_WEPOLL 修正为 JING_USE_SSL,文件保留不编译,留作后续 SSL 支持基础(W6)

## 2026-09-13T16:02:00+08:00
- Modified jing-bindings/src/main/java/io/jingproject/bindings/LinuxBindings.java: 将 9 个 @Downcall 的 methodName 从 jing_epoll_* 改为 jing_linux_epoll_*(与 native 侧 jing_linux.h 导出符号一致)
- Modified jing-bindings/src/main/java/io/jingproject/bindings/MacosBindings.java: @FFM 的 libraryName 从 "jing-bindings" 改为 "jing_bindings"; 将 5 个 @Downcall 的 methodName 改为 jing_macos_* 前缀(与 native 侧 jing_macos.h 一致)
- Modified jing-bindings/src/main/java/io/jingproject/bindings/WinBindings.java: createSocket methodName 从 jing_socket 改为 jing_win_socket 且返回类型 int→long; 删除 4 个仅 POSIX 可用的方法(stdOutputFileno/stdErrorFileno/openFd/writeFd); 将 11 个 wepoll 相关 methodName 从 jing_wepoll_* 改为 jing_win_wepoll_*; wepollWait 签名对齐 C 侧(去掉 r 参数,返回类型 void→int)

## 2026-09-13T15:43:33+08:00
- Modified jing-bindings/src/main/native/src/jing_linux.h: 去掉 jing_linux_epoll_create 的 jing_result* r 参数,改为 void 保持与其他无参函数一致
- Modified jing-bindings/src/main/native/src/jing_linux.c: 同步修改 jing_linux_epoll_create 定义,移除未使用的 r 参数
- Modified jing-bindings/src/main/native/src/jing_comp.h: 给 jing_zlib_ng_version 声明补上 JING_EXPORT_SYMBOL 导出宏
- Modified jing-bindings/src/main/native/src/jing_posix.h: 在 fs related 区域补上 jing_posix_close 声明,放在 jing_sync_fd 之后
- Modified jing-bindings/src/main/native/CMakeLists.txt: JING_USE_WEPOLL 默认 OFF 改为 ON; target_sources 追加 src/jing_comp.c; 删除第 83-102 行被注释掉的 boringssl 死配置块

## 2026-09-13T15:43:33+08:00
- Modified jing-bindings/AGENTS.md: 新增 Return Value Design 章节,规定 native 函数返回值设计规范——简单场景统一用 `-errno` 作为错误返回值(errno 恒为正,Java 侧取绝对值即可还原 errno);复杂场景(如同时返回指针与长度)用固定 16 字节的 `jing_result` 结构体作为出参,可容纳各类返回值类型
- Modified jing-ffm/README.md: 新增 Return Value Design 章节,描述与 jing-bindings/AGENTS.md 相同的返回值约定,并说明 Java 侧通过 NativeSegmentAccess 的 JING_RESULT_LAYOUT(16 字节,运行时校验)与 errCode 访问器读取

## 2026-09-13T14:51:03+08:00
- Modified jing-bindings/PosixBindings.java: 补充缺失的 @FFM 注解(libraryName = "jing_bindings", supportedOS = {Os.LINUX, Os.MACOS}),使该接口能被 jing-ffm-processor 正常生成 facade

## 2026-09-13T14:47:08+08:00
- Modified jing-ffm/README.md: 在 Loading Behavior 章节补充平台支持标注,明确仅支持 Windows/Linux/macOS 且仅限 64 位架构(x64 或 aarch64),不支持 32 位平台

## 2026-09-13T14:39:52+08:00
- Modified jing-ffm/README.md: 新增 Type Mapping 章节,列出 Java 类型与 C 类型(64 位)的映射表,并重点说明 boolean 映射规则与注意事项(boolean 仅与 C `_Bool` 完美映射,不可与 C `int` 混用,并给出第三方 API 的适配建议)

## 2026-09-13T14:24:35+08:00
- Modified jing-ffm-processor/FfmProcessor.java: 代码生成补全 boolean 类型支持(castFfmReturnType/castFfmParameterType 增加 BOOLEAN case),使 @Downcall 方法可声明 boolean 参数与返回
- Modified jing-bindings/src/main/native/src/jing_demo.h/.c: 新增 25 个类型映射验证函数,覆盖常见 C 类型(long/long long/size_t/unsigned int/unsigned long/char*/void*/sizeof 系列)与基础类型(bool/byte/short/char/float/void/指针),用于验证 Java FFM 与 C 的 ABI 映射
- Modified jing-bindings/src/test/.../DemoBinding.java: 新增 23 个 @Downcall 方法(boolean 含 constant/critical 组合)
- Modified jing-bindings/src/test/.../DemoBindingImpl.java: 新增对应 Java 参考实现(整数回绕/位模式语义与 C 一致)
- Modified jing-bindings/src/test/.../DemoTest.java: 新增 6 个测试方法(boolean/byte/short/char/float/常见C类型/指针/void/sizeof 平台断言),验证 Java↔C 类型映射

## 2026-09-13T13:54:00+08:00
- Modified pom.xml: 将 surefire 的 excludedGroups 参数化为 ${surefire.excludedGroups} 属性(默认值不变),使 require-native-library 标签的测试可通过 -Dsurefire.excludedGroups= 在命令行运行

## 2026-09-13T13:37:11+08:00
- Fixed jing-bindings/src/main/native: 补齐 jing_posix.c 缺失的 <unistd.h>/<fcntl.h>/<sys/mman.h> 头文件,修复 jing_common.c 缺失的 <stdlib.h>/<stdalign.h>,jing_linux.h 补充 <sys/epoll.h> 并将 epoll_create/ctl/wait 声明与实现统一为 int 返回,修复 jing_posix.c 中 write 循环 total 累加变量错误,修复 jing_demo.c 的 PRId64 格式化警告,使 native 项目可在 Linux 上无警告构建

## 2026-09-10
- Created AGENTS.md: added project guidelines for AI agents
- Created jing-marshall/README.md: added documentation for jing-marshall module
- Rewrote jing-marshall/README.md: added serialization 2.0 motivation, metadata-focused design, removed format-specific content
- Created jing-marshall-json/README.md and jing-marshall-processor/README.md: empty placeholders
- Updated jing-marshall/README.md: corrected Serialization 2.0 description as unified object schema model, clarified VarHandle vs record accessor field access
- Updated jing-marshall/README.md: added detailed MarshallInfo field explanations (type optimization, mappedName mapping), generic type handling strategy, generic bean limitation
- Updated jing-marshall/README.md: added enum skipSerializing/skipDeserializing explanation
- Updated jing-marshall/README.md: added VarHandle module boundary explanation for same-module parent class constraint
- Created jing-marshall-json/README.md: added JSON format implementation documentation
- Updated jing-marshall-json/README.md: clarified buffer-based operation (not streaming), JsonPrimitiveType purpose and bridge role, type resolution priority, transformer builtin type must be JsonStrType
- Updated jing-marshall-json/README.md: changed custom type example to LocalDateTime/JsonStrType, added detailed performance benchmarks (serialization/deserialization), added floating-point format notes
- Updated jing-marshall-json/README.md: added twitter.json 632KB dataset size, throughput MB/s calculations, statelessness as core advantage
- Updated jing-marshall-json/README.md: added float parsing performance note (2-3x faster than JDK, algorithm simplicity as primary reason)
- Updated jing-marshall-json/README.md: rephrased Constraints to explain limitations due to jing-marshall's type system design
- Updated jing-marshall-json/README.md: corrected Type Resolution Priority to reflect actual natively mapped types (wrappers, String, JsonPrimitiveType, primitive arrays) vs marshall-supported types
- Updated jing-marshall-json/README.md: corrected note to clarify that UUID/BigInteger/BigDecimal/date/time types require user-provided Transformer implementations
- Updated jing-marshall/README.md: rewrote Type Transformer section to explain @Transformable + MarshallTransformerProcessor auto-generation
- Updated jing-marshall/README.md: clarified MarshallTransformer as bidirectional converter between wire format native types and custom types
- Updated jing-marshall-json/README.md: rewrote Custom Type Transformers to use @Transformable pattern with auto-generated facade
- Modified jing-marshall-json/src/test/.../entity/BeanEntity.java: added BigDecimal decimalValue field
- Created jing-marshall-json/src/test/.../entity/BigDecimalTransformer.java: Transformer for BigDecimal ↔ JsonStrType
- Created jing-marshall-json/src/test/.../entity/BigDecimalTransformerFacade.java: hand-written MarshallTransformerFacade with @Provider annotation
- Modified jing-marshall-json/src/test/.../JsonSerializationTest.java: testSerializeBeanEntity with BigDecimal Transformer
- Modified jing-marshall-json/src/test/.../JsonDeserializationTest.java: testDeserializeBeanEntity with BigDecimal Transformer

## 2026-09-11T17:48:00+08:00
- Modified jing-marshall-json/src/test/.../StringSerializationTest.java: migrated from @Tag("view-output") with System.out.println to deterministic assertEquals with round-trip verification (serialize → deserialize → compare input)
- Modified jing-marshall/src/test/.../HashStrategyTest.java: migrated from @Tag("view-output") with System.out.println to fixed-seed (42L) deterministic tests with range assertions for collision rates and max collisions
- Deleted CaptureStringOutputs.java and CaptureHashOutputs.java: temporary helper files no longer needed
- Modified StringSerializationTest.java and HashStrategyTest.java: replaced import static with Assertions.* class-qualified calls

## 2026-09-11T17:55:00+08:00
- Created jing-common-processor/README.md: added documentation for SPI registration pipeline, code generation utilities, and downstream usage guide
- Updated jing-common-processor/README.md: added design philosophy section explaining string-based code generation rationale, @Provider SPI pipeline, and why build plugin is required for module-info bytecode modification
- Updated jing-common-processor/README.md: clarified @ProcessorApi description -- class names and method names are referenced as strings in APT, renaming requires corresponding APT changes
- Updated jing-common-processor/README.md: corrected SPI pipeline steps -- Step 1 describes APT generating @Provider-annotated class, Step 2 describes ProviderProcessor generating JSON, Step 3 describes build plugin consuming JSON; removed POM config example

## 2026-09-11T18:05:00+08:00
- Created jing-maven-plugin/README.md: added documentation for plugin goal, parameters, configuration examples for main/test/both scopes, prerequisites, and SPI pipeline overview
- Updated jing-maven-plugin/README.md: expanded compressed XML tags to separate lines, simplified configuration to main scope and test scope only
- Updated jing-common-processor/README.md: replaced Step 3 detailed description with reference to jing-maven-plugin README

## 2026-09-13T00:29:00+08:00
- Created jing-ffm/README.md: 补充 jing-ffm 模块文档，说明 @FFM/@Downcall 注解、Libs 运行时库加载机制与搜索路径优先级、constant/critical 语义、生成代码结构、NativeSegmentAccess 原始内存访问工具，以及与 jing-ffm-processor 的配合流程
- Updated jing-ffm/README.md: 精简为短句分行的风格，重写 Motivation 加入 Project Panama 背景（JNI 痛点、jextract 的局限），突出与 jing-ffm-processor 配合通过代码生成实现更安全简洁的 native downcall
- Updated jing-ffm/README.md: 新增 Downcall Restrictions 章节，说明模块仅面向 downcall 且服务于项目内部；详述 critical 的正确使用场景及 JVM JVM_LEAF 内部参考基准；解释 allowHeap（堆外内存拷贝与 GC pinning 开销）、firstVariadicArg（可变参数 ABI 复杂性）、captureCallState（errno 获取透明度不足）三项 Linker.Option 在设计上不支持的理由
- Updated jing-ffm/README.md: 新增 Loading Behavior 章节，说明所有 @FFM 声明库随 Libs 类加载一次性 eager 加载且进程生命周期内不卸载（不支持自定义生命周期）；平台识别仅限 Windows/Linux/macOS，supportedOS 不含当前平台时库被静默忽略；函数 stub 绑定延迟到首次调用，以均衡启动与运行时性能
- Updated jing-ffm/README.md: 改写 Usage 章节为更详细的用法指引（生成接口调用、libDescriptor 底层检查、VM 函数 addrFromVM/mhFromVM 取 C 标准库函数示例、addrFromLib/mhFromLib 仅供 APT 生成代码使用用户不应直接调用）；同步重组 Libs Core Methods 说明，区分用户侧（impl/libDescriptor）与 APT 侧（addr*/mh*）两组 API
- Updated jing-ffm/README.md: 精简文档：删除 Usage 中 Not for User Code 小节；Runtime: Libs 仅保留 Library Search Path 与 jing.ffm.critical 两点核心内容；Generated Binding 删除 Notable points 说明及其后全部章节（LibFacade、LibDescriptor、ForeignException、NativeSegmentAccess、Module Declaration、Dependency）
- Updated jing-ffm/README.md: 在 Usage 生成接口小节补充说明，获取的实现应存放在 static final 字段中以便 JIT 常量折叠获得最佳性能，不建议每次调用时作为局部变量重新获取

## 2026-09-13T14:05:00+08:00
- Modified jing-bindings/src/main/native/src/jing_demo.h: 新增 25 个类型映射验证函数声明(含 long/long long/size_t/unsigned int/unsigned long/指针/bool/byte/short/char/float/void 等基础类型)
- Modified jing-bindings/src/main/native/src/jing_demo.c: 新增 25 个类型映射验证函数实现,用于验证 Java FFM 绑定与 C 的类型映射正确性
