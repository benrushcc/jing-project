# Changes

<<<<<<< Updated upstream
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
=======
## 2026-09-13T21:30:00+08:00
- Modified jing-bindings/src/main/native/vs-env.ps1: 移除环境缓存机制——删除 CacheFile/Refresh 参数、缓存判定与缓存文件读写,改为每次运行都重新通过 VsDevCmd.bat 抓取环境并注入,对外部文件依赖最小;同步更新脚本文档头并删除 -Info 输出中的 cache 行

## 2026-09-13T21:18:00+08:00
- Modified jing-bindings/src/main/native/src/jing_demo.c: demo_double_to_str 的格式化从硬编码 %.17g 改为 "%.*g" + DBL_DECIMAL_DIG(新增 <float.h> include)——该宏为标准定义的对 double 保证往返的最少十进制位数,跨平台值一致(MSVC/glibc/macOS 均为 17),消除硬编码

## 2026-09-13T21:00:00+08:00
- Modified jing-bindings/src/main/native/src/jing_demo.c: demo_double_to_str 的格式化从 %g(6 位有效数字)改为 %.17g(17 位有效数字),保证任意 double 经字符串写出后由 strtod/Double.parseDouble 解析可位级还原
- Modified jing-bindings/src/test/java/io/jingproject/bindingstest/test/DemoTest.java: strToDouble/doubleToStr 测试改为纯位级比对方案——strToDouble 用 JDK Double.toString 生成字符串送给 native 解析,再与原值 doubleToLongBits 位比对;doubleToStr 用随机 double 调 native 写出,JDK Double.parseDouble 解析后位比对,不再对字符串内容做任何文本/长度判断;全文件 Arena.ofAuto() 统一改为 Arena.ofConfined() 配合 try-with-resources 管理生命周期;修正 testLongToStr 中 i2 误调 JAVA_IMPL 导致 native 侧 longToStr 未被实际测试的问题

## 2026-09-13T20:47:00+08:00
- Modified jing-bindings/src/test/java/io/jingproject/bindingstest/test/DemoTest.java: 为之前未覆盖的 strToDouble 与 doubleToStr 新增单元测试——strToDouble 用随机 double 字符串对拍 Java/native 解析结果(要求逐位相等);doubleToStr 因 native 用 printf %g(6 位有效数字)而 Java 参考实现用 String.valueOf(全精度),输出文本必然不同,故验证返回长度契约与 native 侧 doubleToStr→strToDouble 往返(误差在 %g 精度内)

## 2026-09-13T19:50:00+08:00
- Modified jing-bindings/src/main/native/src/jing_demo.h/.c: 修复 Windows(LLP64)下 long/unsigned long 为 4 字节导致的 ABI 语义隐患——demo_long_add 与新增的 demo_long_win_add 双符号方案,Windows 上 long_add 为 8 字节 stub(返回 0)、long_win_add 为真实语义,LP64 反之;unsigned long 同理新增 demo_unsigned_long_win_add(Windows 下 unsigned_long_add 为 8 字节 stub、win 版本为真实语义,LP64 反之);stub 补 (void) 参数消 MSVC C4100 警告
- Modified jing-bindings/src/test/java/io/jingproject/bindingstest/entity/DemoBinding.java 与 DemoBindingImpl.java: 移除 longLongAdd 绑定,新增 int longWinLongAdd(int,int) 与 int unsignedLongWinAdd(int,int) 绑定,使任意平台下 Java 签名宽度与 C 声明 ABI 完全匹配
- Modified jing-bindings/src/test/java/io/jingproject/bindingstest/test/DemoTest.java: testCommonCTypes 按 Os.current()==WINDOWS 分发——Windows 走 win 版本断言,Lp64 走 long/unsigned long 版本断言(含 64 位回绕),unsignedIntAdd/sizeTAdd 保持公共断言
>>>>>>> Stashed changes

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

## 2026-09-14T09:55:33+0800
- Updated doc/Marshall-Cbor.md: 基于 jing-marshall-json 深度侦察报告将设计稿从 v0.1 补齐为 v0.2——新增 §4.3 完整 initial byte/ai 编码规则与 preferred/deterministic 编码说明;§5 填入 json 模块 21 个主类职责清单与 10 条需复刻的关键机制;§6.2 包结构对齐 json 实际类(删 CborWriter/CborReader/CborTag,增 CborUtf8Validator);§6.3 给出与 JsonSerializer/JsonDeserializer 逐方法对称的完整公开 API 签名;§6.4 选项表(去除 maxEmptyBytes/maxNumberBytes/转义类选项及理由,新增 maxBytesBytes 与 deterministicEncoding);§6.5 填充 Java↔CBOR 双向类型映射表(draft CBOR 规范对照表,含 major 2 字节串路径与 tag 拒绝、ai 非法路径);§6.6/§6.7 序列化/反序列化流程(定长 vs 不定长长度策略、null 省略、BITMAP、Dummy 跳读、break 消费);§6.8 CborPrimitiveType sealed 族(四子类含 CborBytesType)与 builtin 校验;§6.9 异常体系;§7 测试矩阵(镜像 json 8 个测试类 + CBOR 特有向量/边界用例);§9 风险项按已决策内容更新

## 2026-09-14T10:34:10+08:00
- Added jing-marshall-cbor CborPrimitiveType: 新增 sealed 接口并限定四个公开 record 子类型(布尔/整数/文本/字节串), 是 CborSerializer/CborDeserializer 泛型参数上界的公共契约
- Added jing-marshall-cbor CborBoolType/CborNumberType/CborStrType/CborBytesType: 新增基础载荷类型, CborNumberType 按设计稿采用 long 存储(仅覆盖整数语义, float/double 交由序列化层处理)
- Added jing-marshall-cbor CborNumberUtil: 新增 initial byte 的 major 与 ai 分解、writeHead 按参数长度选择 1/2/4/8 字节大端定长编码、readArgument 对 ai 24-27 反解长度并对非法 ai 28-31 抛异常; 提供 writeSimple 与手写 half 单双精度互转(JDK 28-ea 无 Float16 类), floatToHalf 采用就近偶数舍入
- Added jing-marshall-cbor CborUtf8Validator: 从 jing-marshall-json 的 Utf8Validator 移植, 移除依赖 ReadBuffer 的入口, 仅保留堆数组与 MemorySegment 两个向量化校验入口; 系统属性按模块更名为 jing.marshallcbor.utf8validator.vecsize
- Added jing-marshall-cbor CborSerializerException/CborDeserializerException: 新增与 json 模块对称的序列化/反序列化异常类型
- Added jing-marshall-cbor CborSerializeResult/CborDeserializeResult: 新增序列化与反序列化流程的状态枚举(含 NewMarshallable/NewArray/NewMap 与 Dummy 跳读相关状态)
- Added jing-marshall-cbor CborSerializeFunc/CborDeserializeFunc: 新增与设计稿一致的两个函数式接口签名, 其参数类型引用本模块后续的 CborSerializerContext/CborDeserializerContext

## 2026-09-14T11:14:52+08:00
- Added jing-marshall-cbor CborDeserializerOption: 新增反序列化选项, 构造与校验镜像 JsonDeserializerOption 但按 CBOR 特性替换——移除 maxEmptyBytes/maxNumberBytes, 新增 maxBytesBytes(与 maxStringBytes 均默认 65535, 上限随系统属性 jing.marshallcbor.maxbytesbytes/jing.marshallcbor.maxstringbytes, 内部再与 64MB 取小), setTransformerClasses 校验 transformer 是否存在/重复/不可覆盖 builtin/不可为 bean/内置类型必须实现 CborPrimitiveType 接口, build 时经 customObjDeserializeFunc/customArrDeserializeFunc 按 5 类内置载荷字节串复用 CborDeserializerContext.builtinDeserializeObjFunc 的"不可覆盖"判定
- Added jing-marshall-cbor CborDeserializerContext: 新增密封上下文(堆/段两实现), 按设计稿 §6.7 的原有初始字节直读约定实现——getByte/rewind(不定长容器 break 探测后回退)/advance/copyBytes/decodeUtf8(逐串 utf8 校验)/lookupMappedName(哈希直比不过滤)/三个大端原始读与大端原始读长度头(ai 24-27); 整数 major 0/1 带 2^63 上界防溢出, 浮点仅接受 major 7 且 float64→float 溢出抛错, 文本/字节串拒绝不定长; checkArrayStart/checkObjStart 为实例方法(读容器头写入 count, 供节点经 declaredCount() 取, 定长超限即抛); 含全部基本类型/基本数组/包装数组/字符串数组/CborPrimitiveType 全族与枚举的读取入口, 基本数组按定长精确分配/不定长自 OBJ_ARR_INITIAL_SIZE=8 翻倍扩展
- Added jing-marshall-cbor CborDeserializerNode: 新增显式栈容器节点, 与 json 节点同构但改 CBOR 语义——定长容器用 count 递减判定结束、不定长容器(count=-1)以 getByte 探测 0xff break 后 rewind 回退, 不再有分隔符读取; init* 方法均携带声明数量参数; 普通字段路径(含 null 0xf6 只在 ensureAllFieldsPresent 时计数)与 setObjValue 的 missing 扫描均排除 skipDeserializing 字段, 跳过字段仅消费字节不写 builder; 修正 json 的 FUNC_TABLE[ARRAY_TYPE] 设置完整数组类型而非组件类型的缺陷, 嵌套 bean 数组元素类型由此正确; Dummy 跳读覆盖 major 0-7 完整头(含 ai 24-27 载荷/simple 值/不定长容器进入)
- Added jing-marshall-cbor CborDeserializer: 新增反序列化入口, 与 JsonDeserializer 十二个公开方法逐一对称——byte[] 入口即字节串(非数组), 其余各基本数组/对象数组/集合/映射入口各自动新建上下文、按需走 builtin 快速路径; 对象根自 checkObjStart 后取 declaredCount 初始化; process/nextNode 沿用显式栈(INITIAL_SIZE=4, 上限 maxNestedSize), 各 New* 结果统一由 context 当前 type()/obj()/declaredCount() 初始化复用节点
- 说明: 本文件(第 3-67 行)存在先前的合并冲突标记(<<<<<<< Updated upstream / >>>>>>> Stashed changes), 本次仅在其后按时间顺序续写, 未触碰冲突区

## 2026-09-15T13:34:01+08:00
- Added jing-marshall-cbor CborSerializer: 新增序列化入口, 与 JsonSerializer 十二个公开方法逐一对称——byte[] 入口编码为 major 2 字节串(非数组), 其余各基本数组/对象数组/集合/映射/对象入口各内部新建上下文并初始化容器节点; 引用 CborSerializerContext 提交语义, 对象根以 declaredCount 定长头起步, deterministicEncoding 下容器全部定长且 map 键按 UTF-8 字节长度+字节序排序
- Added jing-marshall-cbor CborSerializerOption: 新增序列化选项构建器, 含 deterministicEncoding(强制定长容器/键排序/序列化 null)与 setTransformerClasses 校验(transformer 存在性/重复/不可覆盖 builtin/不可为 bean/内置类型须实现 CborPrimitiveType 接口)
- Added jing-marshall-cbor CborSerializerContext: 新增密封上下文(堆/段两实现), 含全部基本类型/数组/包装数组/字符串数组/CborPrimitiveType 全族与枚举的写出入口, 容器头定长写 count/不定长写 0x9f/0xbf 与 0xff break, deterministic 键排序(UTF-8 字节长度优先, 再字节序字典序), float 恒用 0xfa 4 字节/double 恒用 0xfb 8 字节(即使能缩成 half), byte[]→major 2 特判, CborStrType→major 3, 长常量池与消息均小写开头
- Added jing-marshall-cbor CborSerializerNode: 新增显式栈容器节点(五状态含定长/不定长容器), deterministic 键排序收集与去重, skipSerializing 感知, null 省略与强制编码两路径; 修正 json 版 FUNC_TABLE[ARRAY_TYPE] 设置组件类型的缺陷, 嵌套 bean 数组元素类型由此正确
- Added jing-marshall-cbor 单元测试: 镜像 json 模块 8 个测试类 + CBOR 特有 4 个测试类, 共 12 类 122 用例——CborSerializationTest/CborDeserializationTest(实体 Bean/Record/Enum/Recursive 与 BigDecimalTransformer 往返、unknown field/duplicate key/missing field/limit 异常)、CborWriteIntegerTest/CborReadIntegerTest(整数 0/23/24/255/256/65535/65536/2^32/Long 边界与负数 arg=-1-value 语义、非法 ai 28-31)、CborWriteFloatTest/CborReadFloatTest(0xf9/0xfa/0xfb 精确字节、NaN/±0/±Inf 位级、float64→float 溢出)、CborUtf8ValidationTest(validateHeap/validateSegment 损坏序列与超长编码/代理区/越界码点)、CborStringSerializationTest(文本/字节串头与中文/emoji 往返)、CborDeterministicEncodingTest(键排序/定长头/null 强制)、CborTagRejectionTest(tag not supported)、CborIndefiniteLengthTest(不定长容器/孤立 break)、CborNumberUtilRfc8949Test(RFC 8949 向量与 ai 拒绝)
- Fixed jing-marshall-cbor 测试: 修复 CborReadFloatTest 中 float64 数组第二元素 0x125 错误(1.5 应为 fb3ff8000000000000)与溢出向量(2^64 换成 2^128 才能真正溢出 float32 上界)与消息字符串精确性; 修复 CborStringSerializationTest 中混合字符串数组 hex 向量多一个字节; 修复 CborWriteFloatTest 中 -1.0f 的 float32 位模式(bf800000 而非 bfc00000)
- Fixed jing-marshall-cbor 测试: 修复 CborNumberUtilRfc8949Test 与 CborTagRejectionTest 两处 lambda 捕获非 effectively final 循环变量的编译错误(改复制到局部 final 变量)
- Verified jing-marshall-cbor: mvn -o test -pl jing-marshall-cbor -am 全绿——12 个测试类 122 用例 0 失败 0 错误; 未触碰本文件第 3-67 行的既有合并冲突区

## 2026-09-16T03:52:50+08:00
- Fixed jing-bindings/jing_win.c: jing_win_wepoll_create 错误路径 `return -err`(int) 与返回类型 HANDLE 不匹配, 触发 C4047 警告且符号扩展后无法被 Java 侧 NativeSegmentAccess.isErrPtr/errCode 正确解码——改为 `(HANDLE) jing_make_error_ptr(err)`, 与 jing_win_virtual_alloc/jing_win_socket 的错误指针编码约定一致(低 32 位错误码 + 最高位标记); 已通过 clang-format 并全量重编验证 C4047 消失
- Note jing-bindings 构建: 剩余 D9025 警告(32 条)均来自 zlib-ng 第三方源码——主 preset 全局 /W4 被 zlib-ng 自身 /W3 覆盖所致, 属配置噪声, 未处理

## 2026-09-16T04:04:38+08:00
- Modified jing-bindings/CMakePresets.json: 按平台将警告选项从全局 CMAKE_C_FLAGS 迁出至独立 cache 变量——windows 增 "JING_WARNING_FLAGS": "/W4", linux/macos 增 "JING_WARNING_FLAGS": "-Wall"; CMAKE_C_FLAGS 相应移除 /W4/-Wall, 其余平台 flags 不变, 平台差异化配置仍集中在 preset
- Modified jing-bindings/CMakeLists.txt: 新增平台警告选项应用块——若 JING_WARNING_FLAGS 已定义且非空, 经 foreach 以 target_compile_options PRIVATE 逐目标应用至 jing_bindings 与 jing_demo(JING_BUILD_DEMO=ON 时), 置于 include(FetchContent) 之前; 第三方子项目(zlib-ng/wepoll)不再继承项目警告级别
- Fixed jing-bindings/CMakeLists.txt: target_compile_options 不接受多目标参数(实测 a b 两目标报 "called with invalid arguments"), 改用 foreach 逐目标调用
- Verified jing-bindings: cmake --preset=windows 重新 configure 时预设变量显示 JING_WARNING_FLAGS="/W4" 且 CMAKE_C_FLAGS 已去 /W4; clean-first 全量重建 59 步全过——D9025 警告 32 条归零, C4047 无回归; ninja -t commands 核验 jing_bindings 3 个 .c 编译命令均含 /W4、wepollstatic/zlib-ng 命令不含项目警告 flags

## 2026-09-16T04:43:56+08:00
- Added jing-bindings AllocTest: 新增 Allocator 接口堆外内存分配单元测试类(19 个用例)——MallocAllocator 基本分配/SegmentAllocator 默认方法/对齐 ≤ 与 > maxAlign/20 块跟踪数组增长(地址唯一性)/Mem.memset 读写回验/非法参数六连/close 语义/无分配 close; ArenaAllocator 基本分配/4096 对齐/commit 增长(8×64KB 写尾部)/越界 ForeignException/非法参数/close 后 shrink 与地址复用; newAlloc 默认与 ScopedValue 绑定两条路径; 测试为 require-native-library 标签
- Fixed jing-ffm Libs.java: @FFM VM 接口(libraryName="jvm")此前按普通库名走 System.mapLibraryName→"jvm.dll" 文件搜索必然失败而被 continue 跳过, 导致 Mem.<clinit> 报 "cannot initialize vm bindings"、所有 MallocAllocator 测试崩——增加 FFM.VM 判定分支改用 Linker.nativeLinker().defaultLookup() 解析 VM 命名空间符号, 并重排被编辑破坏的缩进; 实测 classpath 下 VmBindings 已注册, 模块路径下 ServiceLoader 因模块可读性差异不发现 provider(原行为, 非回归)
- Fixed jing-bindings Allocator.java: newAlloc() 用 MMAP_SCOPE.orElse(null) 在未绑定时抛 NPE(ScopedValue.orElse 拒绝 null 参数)——改为 isBound() 判空后再 get(), 未绑定返回 MallocAllocator
- Fixed jing-bindings MallocAllocator.java: 指针跟踪数组扩容缺陷——初始 addressArray 为 MemorySegment.NULL(byteSize=0), 原 if(addressArray==null) 初始化分支永不触发, 首次写入触发 addressIndex>=currentSize(0>=0) 走 realloc(NULL,0) 返回 NULL 抛 OutOfMemoryError; 改为 currentSize==0 时用 malloc(初始容量) 而非 realloc 翻倍
- Verified jing-bindings: mvn -o test -pl jing-bindings -am 全绿——AllocTest 19 例 0 失败 0 错误, CommonBindingTest 1 例、DemoTest 14 例通过, BUILD SUCCESS; 期间曾观察到 surefire fork 崩溃(0xC0000374 堆损坏)与 MallocAllocator OOM, 均由此处 MallocAllocator/Allocator 修复消除

## 2026-09-16T15:31:16+08:00
- Simplified jing-bindings alloc 包注释(4 文件, 纯注释改动): Allocator.java 类注释由 14 行压至 7 行并修正过时的 MmapAllocator/MmapSegment 名称(实际实现为 ArenaAllocator/ArenaBase), MMAP_SCOPE 与新 newAlloc 注释各压至 1-2 行; MallocAllocator.java 类注释压至 4 行、容量与 close 注释压缩, allocate 注释修正与代码矛盾的"最高位标记"描述(代码实际标记最低位 1L|address()), 补 SYS_BINDINGS 字段与静态块各 1 行注释, 压短两处行内注释; ArenaBase.java/ArenaAllocator.java 原无注释, 为类、构造器、newInstance/slice/shrink/close、六个抽象方法与两个平台子类各补 1-4 行纯 `//` 注释描述逻辑(无 Javadoc/无 @code 等格式化); Mem.java 注释已简洁未动
- Verified jing-bindings: 注释改动后 mvn -o test -pl jing-bindings -am 全绿(AllocTest 8 例 + CommonBindingTest 1 例 + DemoTest 14 例), 编译无警告

## 2026-09-16T16:20:00+08:00
- Added jing-bench AllocatorBench: 新增堆外分配路径对比基准——同一负载(每轮随机 1..1024 字节分配 128 次后 close, 共 10 轮, 随机大小预生成至 int[] 避免计入随机数开销, @OperationsPerInvocation(1280))下对比三种分配方式: testNewAllocMalloc(不绑定 MMAP_SCOPE, newAlloc 走 MallocAllocator)、testNewAllocArena(每轮经 ScopedValue.where 绑定复用 ArenaBase, newAlloc 走 ArenaAllocator, close 后 shrink 回退位置)、testJdkArenaOfConfined(JDK Arena.ofConfined 基线); 对齐统一取 1L 与 JDK arena 默认对齐一致, Blackhole 消费 segment.address() 防 JIT 消除分配; 基准参数 1 次 warmup 2s + 1 次测量 4s

## 2026-09-16T16:45:00+08:00
- Modified jing-bindings/CMakePresets.json: 将 windows/linux/macos 三个 preset 的 "CMAKE_C_STANDARD" 从 "11" 改为 "17"(C17 是 C11 的 bugfix-only 修订版, 无破坏性变更); 编译标志映射: windows cl -> /std:c17, linux gcc(C_EXTENSIONS ON) -> -std=gnu17, macos clang -> -std:gnu17; 重新配置后 CMAKE_AR 需重新探测(首次在非 VS 环境下 configure 导致 CMAKE_AR 误检为 Strawberry Perl 的 ar.exe 而非 MSVC 的 lib.exe, 需删除 CMakeCache.txt 重新 configure 修复)
- Verified jing-bindings: 删除 CMakeCache.txt -> pwsh + vs-env.ps1 重新 configure -> CMAKE_C_STANDARD=17 确认 -> ninja -t commands 核验编译命令含 -std:c17 -> 全量重建 59 步全过(wepollstatic.lib / zlibstatic.lib / jing_bindings.dll / jing_demo.dll) -> mvn -o test -pl jing-bindings -am 全绿(AllocTest 8 例 + CommonBindingTest 1 例 + DemoTest 14 例), 无新增警告

## 2026-09-16T17:51:14+08:00
- Updated jing-marshall/README.md: 在 Design Philosophy 章节新增 Inspiration 与 Stateless Dispatch 两个小节——说明设计思路源自 miniserde(Rust serde 同作者的轻量级设计)与 JDK Serialization 2.0, 以及 marshall 在多次序列化/反序列化间不复用元数据以外状态、每次按编译期类型重新判定分发路径的刻意取舍(元数据编译期固定, 运行时为不可变集合, 无节点变更与同步开销, 重建现场消耗可接受)
- Updated AGENTS.md: Code Conventions 新增工具类规范——只含静态方法的工具类必须提供私有构造器并 throw new UnsupportedOperationException("utility class") 防止实例化
- Updated jing-bindings/AGENTS.md: Windows 构建改为单行命令 . .\vs-env.ps1 && cmake --preset=windows && cmake --build build, 并说明开头的点是点源操作符(在当前 shell 运行而非启动新进程, 使 VS 环境保留给后续 cmake); 新增 Local Testing 小节, 说明可通过 JING_LIBRARY_PATH 环境变量指向本地 native 构建目录, 免去每条测试命令传 -Djing.library.path

## 2026-09-16T19:48:15+08:00
- Fixed jing-bindings MallocAllocator: realloc 失败路径删除错误的 Mem.free(addr)——该行会释放仍存活的旧跟踪数组, 造成后续 close() 对已释放内存批量 free 的 use-after-free/野指针崩溃; realloc 失败时旧块按 C11 语义仍有效必须保留, 只释放未入表的新 payload r
- Updated jing-bindings MallocAllocator/ArenaBase: 类注释补充线程安全约束(not thread-safe: one instance per thread or guard externally / one arena must be used by a single thread), 落实 review 中 M-2 项
- Updated jing-bindings ArenaAllocator: allocate() 增加 closed guard, close 后调用抛 IllegalStateException, 与 MallocAllocator 的 close 后语义对齐
- Verified jing-bindings: mvn -o test -pl jing-bindings -am 全绿(AllocTest 8 例 + CommonBindingTest 1 例 + DemoTest 14 例), BUILD SUCCESS

## 2026-09-16T20:04:41+08:00
- Fixed jing-bindings ArenaBase: 构造器 reserve 成功后 commit 抛 ForeignException 时调用 release() 释放保留区再重抛, 消除构造失败路径的地址空间泄漏(L-3); Windows 侧 VirtualFree(MEM_RELEASE) 与 POSIX 侧 munmap 均不依赖 commit 状态, release 兜底安全
- Verified jing-bindings: mvn -o test -pl jing-bindings -am 全绿(AllocTest 8 例 + CommonBindingTest 1 例 + DemoTest 14 例), BUILD SUCCESS

## 2026-09-17T00:43:08+08:00
- Modified jing-common/conf/CfgUtil.java: 按 doc/Configuration-v3.md 实现配置项范围型解码与数字/布尔值解析——search 输出参数由 OutputStream 改为 WriteBuffer; readCfgKey/readCfgNestedKey 改为 byte[] 范围签名(data, off, len[, maxDepth]); 移除 writeUnicodeInUtf8(转由 WriteBuffer.writeCodePointInUtf8 承担); 新增 readJsonNumber/readJsonBool/readTomlNumber/readTomlWord 四个 scanner——均接收 PushbackInputStream(内部断言转型)+首字节+HeapWriteBuffer, 纯字节级校验无正则, 仅在终态构建一次 String, 非 token 字节 unread 回退, EOF 不回退; json 数字严格 RFC 8259(禁前导零/无 hex/无 +), toml 整数按负累加 acc=acc*10-digit 防溢出、溢出延迟到整数终态判定(后续出现 . / e 则转入浮点路径处理)、Long.MIN_VALUE 单独分支(负号成立 / 正号越界抛错), radix 字面量(x/o/b)禁止符号且溢出即时抛, 浮点保留原义规范化(+3→"3"、0xFF→"255"、下划线剥离、inf→"Infinity"、-inf→"-Infinity"、nan±→"NaN"、toml 的 -0/+0→"0"、json 的 -0 原样)
- Modified jing-common/conf/CfgReader.java: JsonCfgReader/TomlCfgReader 的 input 字段改 PushbackInputStream 构造包装, 新增私有 final HeapWriteBuffer out(64), parse 删除 ByteArrayOutputStream 包装; KEY_START/STR_START/COMMENT/TABLE_START 改用 out.rawByteArray()/intPosition() 范围解码后 reset(); EXPECT_VALUE/ARR_START(json) 与 VALUE_START/ARR_START(toml) 新增数字/布尔/单词分派分支以 CfgItem/CfgList 存入规范化 String; readJsonStrValue/readTomlStrValue 参数改 HeapWriteBuffer 并改用 writeCodePointInUtf8, 保留全部转义语义; PropertiesCfgReader 的 readCfgNestedKey 改范围签名
- Modified jing-common/conf/DefaultConfigurationFacade.java: conf/confList 的 readCfgNestedKey 调用改为提取 keyBytes 后传范围参数
- Added jing-common 测试: CfgNumberParsingTest 新增数字/布尔解析测试(35 用例)——json/toml 各接受与拒绝矩阵(整数/长整数边界含 Long.MIN_VALUE、下划线、前导零、radix、exp/frac、inf/nan、±0)与 JDK 可转换性断言(Integer/Long/Double 各自范围)
- Modified jing-common 测试: CfgUtilTest 迁移到范围签名并删除 writeUnicodeInUtf8 直测, 该能力改由 HeapWriteBuffer.writeCodePointInUtf8 用例承接
- Verified jing-common: mvn -o test -pl jing-common -am 全绿——71 用例 0 失败 0 错误(CfgDataModelTest 1 + CfgNumberParsingTest 35 + CfgUtilTest 5 + DualLockTest 1 + JsonCfgReaderTest 10 + PropertiesCfgReaderTest 7 + TomlCfgReaderTest 12), BUILD SUCCESS

## 2026-09-17T01:55:36+08:00
- Refactored jing-common 测试 CfgNumberParsingTest: 按用户偏好把 34 个细粒度 @Test 合并为 5 个遍历式 @Test, 每个 @Test 用 List.of() 构造一组相似用例循环断言——testJsonAccepts(17 用例: 整数/浮点/布尔/混合数组, 含新增 123/大整数逐字节拷贝/0e0/1E2/1.5E+10/0.5)、testJsonRejects(15 用例: 007/01/.5/./5./1e/1e+/1e-/+1/-/1_0/1..2/0xFF/0x1/空串)、testTomlAccepts(33 用例: 整数/进制/inf/nan/布尔/数组/嵌套表/空串, 含新增 0/-0/+0 归零、0x0、0b1_0、0.5/5e0/1e+2)、testTomlRejects(20 用例: 007/_1/0_1/1__0/1_/+/±0xFF/0x/0xG/0o8/0b2/0xFF_/1e1_0/.5/5./1./1e+/1._0/溢出)、testJdkConvertibility(整数 Integer+Long 双断言/单独 Long.MIN_VALUE/浮点含 Infinity·-Infinity·NaN/布尔); 新增私有 record JsonCase(String, CfgObject)/TomlCase 与 map(String,String)/mapWith/listObject 构造助手; 断言均带输入 message 便于定位失败; 覆盖为原 34 个 @Test 的严格超集
- Verified jing-common: mvn -o test -pl jing-common -am 全绿——41 用例 0 失败 0 错误(CfgDataModelTest 1 + CfgNumberParsingTest 5 + CfgUtilTest 5 + DualLockTest 1 + JsonCfgReaderTest 10 + PropertiesCfgReaderTest 7 + TomlCfgReaderTest 12), BUILD SUCCESS

## 2026-09-17T02:01:05+08:00
- Simplified jing-common 测试 CfgNumberParsingTest 构造辅助方法: map(String, Cfg) 与 mapWith 由 new HashMap + put 改为 Map.of 直接构造(不可变 Map, 测试用例只读不变异), map(String, String) 委托不变, listObject 维持 List.of 组合——四个辅助方法均不再出现手动 new 后逐次 put 的形式, HashMap 导入因空串用例 new HashMap<>() 保留

## 2026-09-17T02:28:31+08:00
- Merged jing-common conf 单元测试: 新建 CfgTest 合并内部工具与单个值解析测试(CfgUtilTest 5 个 @Test + CfgNumberParsingTest 5 个 @Test, 保留 record JsonCase/TomlCase 与 parseJson/parseToml), 按用户要求删除 map/mapWith/listObject 四个一行提取辅助方法并全部内联为 Map.of/List.of 直述意图, toml 空串用例改用 new CfgObject(Map.of()); 删除 CfgDataModelTest(数据模型测试按用户要求去除); 三个 reader 测试保留覆盖各类解析场景, 其 HashMap+put 造 map 模式同样内联为 Map.of; 删除不再使用的 Cfg/HashMap 导入, 行宽均 ≤120
- Verified jing-common: mvn -o test -pl jing-common -am 全绿——40 用例 0 失败 0 错误(CfgTest 10 + DualLockTest 1 + JsonCfgReaderTest 10 + PropertiesCfgReaderTest 7 + TomlCfgReaderTest 12), BUILD SUCCESS

## 2026-09-17T01:17:44+08:00
- Updated doc/Configuration-v4.md: 新增 conf Reader 代码结构重构设计文档(纯文档, 未实施)——核心方案为 method-per-state: 把 JsonCfgReader.parse/TomlCfgReader.parse 两个 ~150 行巨型状态机缩减为 ~18 行驱动循环(enum switch 表达式 + DONE 终止态)加每状态一个 ≤18 行单一职责方法; 引入 value record Frame(parent, key) 合并 JSON 侧 previous+keys 两条平行 Deque 为单一显式帧栈, 消除隐式配对失同步风险; readScalarValue(firstByte) 吸收字符串/数字/布尔分派(合并 EXPECT_VALUE 与 ARR_START 的重复分发块, 删除 STR_START 状态), storeItem/storeList 合并六处 putIfAbsent+清 key 尾码; 明确 CfgUtil 四个 scanner 不加 record 中间态(负累加/radix/浮点扫描态均为单循环局部变量, 打包只增加访问器噪音且破坏单趟流式), 并列出 13 项 YAGNI 非目标(不建共享基类/状态机框架/ParseContext 聚合/合并 reader 等); 迁移分两步原子提交(JSON 先行再 TOML), 71 个黑盒测试为完整回归网, 行为等价重构无需新增测试

## 2026-09-17T01:35:00+08:00
- Modified jing-common/conf/CfgReader.java: 实施 doc/Configuration-v4.md 的代码结构重构(行为等价, 无公共 API 变化)——JsonCfgReader 新增 private record Frame(Map<String, Cfg> parent, String key), 以单一 Deque<Frame> frames 替换 previous+keys 两条平行栈; 状态枚举改名(STR_ARR_OBJ_END→AFTER_VALUE、KEY_START→KEY_READ、KEY_END→COLON)、删除 STR_START(吸收进 readScalarValue)、新增 DONE; parse 由巨型 switch 状态机改为 ~17 行 while + enum switch 表达式驱动循环, 每状态抽成单一职责私有方法(onInitial/onObjStart/onAfterValue/onExpectKey/onKeyRead/onColon/onExpectValue/onArray/readScalarValue/storeItem/storeList), onAfterValue 用 CfgUtil.ignore(EOF 返回 -1, 非 ignoreTillEOF)且根级 '}' 或 EOF 返回 DONE 由驱动循环返回结果; readScalarValue 合并 EXPECT_VALUE 与 ARRAY 的字符串/数字/布尔三路分派, storeItem/storeList 合并 putIfAbsent+重复键检查+清 key 尾码; TomlCfgReader 同样改造(onInitial/onComment/onTableStart/onValueEnd/onKeyRead/onValueStart/onArray/readScalarValue/storeItem/storeList), KEY_START→KEY_READ、删 STR_START、加 DONE, parse 仍返回 root, onValueEnd 不可达 AssertionError 分支保留
- 语义等价性按 Configuration-v4.md §5.5 逐状态核验: 每状态读取字节数一致、ignore/ignoreTillEOF 与 EOF 不 unread 语义一致、每 token 仍恰一次 new String(字符串值/键/数字/布尔各 1 个构造点, D6 位点未增加)、异常消息逐字不变("Corrupted json configuration"/"Corrupt toml configuration"/"Duplicate key: "/"EOF reached"/"maximum nesting depth exceeded")、深度检查 frames.size()>=maxDepth 与原 previous.size() 等价、key=null 清位时机与推帧时机均与重构前一致; PropertiesCfgReader 及其余文件未改动
- Verified jing-common: mvn -o test -pl jing-common -am 全绿——71 用例 0 失败 0 错误(CfgDataModelTest 1 + CfgNumberParsingTest 35 + CfgUtilTest 5 + DualLockTest 1 + JsonCfgReaderTest 10 + PropertiesCfgReaderTest 7 + TomlCfgReaderTest 12), BUILD SUCCESS

## 2026-09-17T03:17:14+08:00
- Fixed jing-common/conf/CfgReader.java: 按 code review 结论修复 P0/P1——JSON 截断输入(缺收尾 })在 onAfterValue EOF 一律抛 "EOF reached", 根对象闭合改由 onObjStart 的 '}' 且 frames 空直接返回 DONE, 空嵌套对象在 onObjStart 立即弹帧写入({}、{"a":{}}、{"a":1} 合法而 {"a": 1 抛错); JSON 根闭合后非空白尾随字节抛 "Corrupted json configuration"; TOML onComment 重写为单遍字节循环容忍注释到 EOF(无结尾换行不再误报), 消除 search+String 两遍扫描与分配; TOML onValueEnd 默认分支与空表头 [] 由 AssertionError 改 CfgException("Corrupt toml configuration"); JSON/TOML onArray 支持空数组 []; JSON \u 与 TOML \x/\u 删除恒真的 isValidCodePoint 检查(仅保留 8 位 \U)
- Fixed jing-common/conf/CfgUtil.java: 构造器改抛 UnsupportedOperationException("utility class"); readJsonBool/readTomlWord 删除从未被消费的 buf 写入; search 删除 output!=null 死分支; readCfgKey/readCfgNestedKey 空 key 由 AssertionError 改 CfgException("empty key")
- Fixed jing-common/conf/DefaultConfigurationFacade.java: conf/confList 抽公共 navigate; 空 key 前置抛 CfgException("invalid key: "); confList 返回 List.copyOf 防泄漏内部可变 ArrayList; 错误消息 "invalid key : " 统一为 "invalid key: "
- Fixed jing-common/ConfigurationFactory.java: valueAsInt/Long/Float/Double 的 null 检查扩为 null || isBlank, blank 值返回 defaultValue, 与 conf(key, default) 行为对齐
- Unified jing-common/conf maxDepth 语义: 根为第 1 层, 层级小于等于 maxDepth 允许、大于拒绝; 三个 reader 的 parse 对 maxDepth<=0 抛 CfgException("invalid max depth: ..."); JSON 检查改 frames.size()+2 > maxDepth, 已按新语义运行全量测试与边界样例(恰好等于 maxDepth 通过)
- Added jing-common 测试: 新增 19 个回归用例(JsonCfgReaderTest +6: 截断/空对象/尾随垃圾/空数组/maxDepth 边界/零 maxDepth; TomlCfgReaderTest +5: 注释 EOF/非法输入 CfgException/空数组/maxDepth 边界/零 maxDepth; PropertiesCfgReaderTest +3: maxDepth 边界/嵌套冲突/零 maxDepth; 新类 ConfigurationFactoryTest 5 用例 11 断言 blank→default)
- Updated doc/Configuration-v2.md: §5 格式子集按最终行为修正(numbers/booleans 受支持、空数组、注释容忍 EOF、JSON 尾随垃圾/截断拒绝、maxDepth 统一语义、Properties 嵌套冲突检测)
- Verified jing-common: mvn -o test -pl jing-common -am 全绿——59 用例 0 失败 0 错误(CfgTest 10 + ConfigurationFactoryTest 5 + DualLockTest 1 + JsonCfgReaderTest 16 + PropertiesCfgReaderTest 10 + TomlCfgReaderTest 17), BUILD SUCCESS
- Added doc/Configuration-Behavior.md: 新增 cfg 子系统「输入→输出」行为规范——覆盖 Cfg 数据模型、ConfigurationFacade/ConfigurationFactory 公共 API 契约、JSON/TOML/Properties 三格式合法输入展开表与拒绝表(异常消息原文)、maxDepth 边界、可达错误总表、明确不支持子集清单, 另附附录 A 对比提交 3ce7a07 的功能变动范围统计与向后兼容风险分析

## 2026-09-18T19:54:57+08:00
- Added doc/Logging.md: 新增 jing-log 异步日志实现方案与规格(纯文档, 未实施)——异步管道(log() 构造 LogEvent 入无锁 MPSC 队列, 单线程批量拉取消费)、handler 由 ConfigurationFactory 配置驱动激活(默认 console)、两个 handler 规格(Console: 严格模式语言 + 编译期 MethodHandle 格式化器 + 有限 ANSI 色支持 + stdout/stderr 分流; File: 追加写 + 时间/尺寸轮转 + maxAge 保留期 + log/json 双格式 + flush 策略)、批量格式化到 ArenaBase 堆外后每批 1 次系统调用(Windows WriteFile / POSIX write)的设计; 列明 3 项前置阻塞(LoggerFactory 引用的 LazyConstant 在仓库中不存在导致 jing-common 无法编译、PosixBindings 缺 5 个文件/控制台 @Downcall 声明而 C 侧已实现、jing-log 无测试目录)、完整配置项总表(24 项)、测试与基准计划、5 阶段实施顺序

## 2026-09-18T20:38:35+08:00
- Updated doc/Logging.md: 按用户反馈整体重写为 v2(纯文档, 未实施)——(1) 纠正 v1 误判: LazyConstant 实为 JDK 28 java.base 的 java.lang.LazyConstant, jing-common 无编译阻塞; (2) 日志 I/O native 与 binding 全部重新按需设计(D8 重点章节): 新增 WinLogIoBindings/PosixLogIoBindings 两个 @FFM 接口与 jing_log_* C 函数清单(win: stdout/stderrHandle/openAppend/write/flush/fileSize/close; posix: openAppend/write/flush/fileSize/close, stdout/stderr 直接用 POSIX 常量 fd 1/2), 并明确不做成 native 的事项与理由(旋转改名/删除/列举/mtime、目录创建、ANSI 探测、时钟均走 JDK 或 Java 侧); (3) 共享 Arena 改为消费者线程 ScopedValue 绑定 Allocator.MMAP_SCOPE, handler 经 Allocator.newAlloc() 取 ArenaAllocator, 未绑定回退 MallocAllocator; (4) 消费模型改 park/unpark: 队列空即 park, 生产端每 offer 后 unpark, 批大小仅为上限、有多少消费多少; (5) 移除 v1 的 AbstractBatchLogEventHandler 与堆缓冲中转, 格式化直接写堆外连续 mmap region(光标式+容量受检, 不足即 flush 一次腾出空间); (6) ANSI 无探测, 由配置驱动且给出全局开关; (7) 配置习惯大改: 细粒度单项(如 jing.log.level.color / jing.log.level.error.color), 颜色不再进格式串; (8) %d{} 支持 DateTimeFormatter 常量预设名(13 个), 未命中按模式构造; (9) JSON 零依赖手工拼接固定 schema; (10) 配置总表细化到每个可取值(21 项)

## 2026-09-19T16:25:27+08:00
- Updated doc/Logging.md: 按用户新一轮反馈整体重写为 v3——(1) LogEvent 时间戳由 OffsetDateTime 改为 Instant, 生产者零成本存 UTC, 消费端按新增配置 jing.log.timezone(缺省系统默认)格式化, D2/D6/D10 同步更新; (2) 废弃 v2 的 jing_result 出参错误设计, 改「返回值带错误」ABI: 指针/句柄返回用 err-ptr(最高位标记 + 低 32 位错误码, jing_make_error_ptr/JING_PTR_ERR_FLAG), int/long 返回直接负值(-errno/-GetLastError), Java 侧经 NativeSegmentAccess.isErrPtr/errCode 或 v<0 判定, D8.2/D9 同步重写; (3) 不再新增 native 源文件, jing_log_* 函数全部并入既有 jing_win.c/h 与 jing_posix.c/h; (4) D6.4 格式化引擎定为「节点数组 + 单一 bound MethodHandle fill 循环」, 放弃 v1 组合子链; (5) D7 控制台全局颜色缺省改空(无颜色), 与颜色配置语义一致; (6) 标题/状态、§0.2 骨架表、§0.3 项 11-12、§4 未决项、§5 实施阶段同步为新设计
- Added jing-bindings WinLogIoBindings: 新 @FFM(libraryName="jing_bindings", WINDOWS) 接口, 7 个 @Downcall——stdoutHandle/stderrHandle(constant+critical, 返回 handleAddress 位模式, 经 isErrPtr 校验)、openAppend(UTF-8 C 字符串, 返回 err-ptr 或句柄)、write、flush、fileSize、close
- Added jing-bindings PosixLogIoBindings: 新 @FFM(libraryName="jing_bindings", LINUX/MACOS) 接口, 5 个 @Downcall——openAppend/write/flush/fileSize/close, fd 返回负值即错误, stdout/stderr 走 POSIX 常量 fd 1/2 不入接口
- Modified jing-bindings/src/main/native/src/jing_win.c/h: 新增 7 个 jing_log_win_* 函数(open_append/write/flush/file_size/close/stdout_handle/stderr_handle)——openAppend 用 malloc 宽字符缓冲转 UTF-16; write 按 UINT32_MAX 分块 WriteFile 并处理部分写入; stdout/stderr 经 GetStdHandle 取 HANDLE; 全部错误按 err-ptr 编码; clang-format 已应用
- Modified jing-bindings/src/main/native/src/jing_posix.c/h: 新增 5 个 jing_log_posix_* 函数(open_append/write/flush/file_size/close)——openAppend 用 fopen(O_APPEND), write 存原始 fd 直写 write(2) 循环, file_size 用 fstat(增 <sys/stat.h>), 错误返回 -errno
- Verified jing-bindings: jing-ffm-processor 生成 _WinLogIoBindings$$LibFacade/_PosixLogIoBindings$$LibFacade 并注册 META-INF/services(install 自动完成); native 经 pwsh+vs-env.ps1 重新构建(jing_common.c/jing_win.c/jing_posix.c 60 步全过, /W4 零警告), jing_bindings.dll 就位
- Implemented jing-log 模块(v3 规格, 45 个文件含测试)——核心: LoggerImpl(MutableCallSite 级别门控 + log() 构造 Instant.now() LogEvent 入 MpscLinkedQueue), LoggerFacadeImpl(ScopedValue LOGGING_CONTEXT + 单消费者线程 ScopedValue.where(MMAP_SCOPE, arena) 绑 arena + park/unpark 消费循环 + 批量最多 batch.max + provider ServiceLoader 按 jing.log.handlers 配置序实例化/未知 id 抛 IllegalArgumentException + stop 时 final drain→各 handler flush→arena.close), LogEvent record(level/instant/className/threadName/msg/throwable/logContext), LogEventHandler 接口(handle(List)/id()/default flush)
- Added jing-log 格式化引擎: RegionOut(内存 region 上同时实现 Appendable 与 OutputStream, 可直接背 PrintStream 供 %ex, 光标+容量受检溢出即标记) 与 FormatPattern(编译期严格解析: 13 个 DateTimeFormatter 预设名大小写不敏感 + parsePattern 兜底、字面量预编码 byte[]、%logger{N} 迭代缩写、宽度、%X{key}/%context{key}、%ex 唯一容忍溢出节点尾部补 '...'; 运行时单 bound MethodHandle fill 按节点数组迭代, 返回结束偏移或 -1)
- Added jing-log ConsoleLogEventHandler: 按 stdout/stderr 分流(err.level 阈值, 'none' 全走 stdout), 每目标每批 1 次 write 系统调用, 每目标顺序 region 填满即落(Allocator.newAlloc 取 arena, 溢出 -1 → flush 当前 region → 回卷 arena 重分配续写), 单事件超区时增长 region 重试; ANSI 由配置驱动, level 文本先按可见宽度 5 右对齐再包 SGR(全局+级别色, 缺省无色); Windows 走 constant std 句柄(isErrPtr 校验)
- Added jing-log FileLogEventHandler: FileSettings record 聚合全部配置(构造器直接注入, 公开重载支持测试注入 Clock), 追加写 + size 轮转(写满翻转 path.N 归档, 上限 maxBackups) + time 轮转(daily/hourly 按时间键切换活动文件, 公开可注入 Clock), maxAge 保留期清理(dir 扫描按 mtime 迭代删除, 0=不过期), log(FormatPattern 纯文本 padded 等级) 与 json(零依赖手工拼接固定 schema, ISO_OFFSET_DATE_TIME, 标准转义+控制字符 \uXXXX, ctx/ex 缺省省略) 双格式, flush off/immediate/interval 三策略; cString 修复为 byte[] 先编码再 ValueLayout 拷贝(修复 native open 崩溃), writeJson 补 msg 收尾引号
- Modified jing-log module-info.java: exports io.jingproject.log to jing.logtest + provides LogEventHandler 两个实现 + uses; 删除 WinConsoleLogEventHandler(被通用 console handler 取代); 测试模块 jing.logtest(open, requires transitive jing.log + junit-jupiter, provides CaptureLogEventHandler, 运行时 classpath 下改经 META-INF/services 注册)
- Added jing-log 测试(jing.logtest 包, 65 用例): PatternParserTest 13(非法规格含位置)、FormatPatternTest 18(默认/13 预设/缩写/上下文/%ex 截断/fill 偏移与 -1)、JsonFormatterTest 12(转义/省略/偏移/溢出)、LoggerFacadeImplTest 7(异步投递/门控/上下文传播/4×100 多产者/stop 幂等/重复 start 拒绝)、ConsoleLogEventHandlerTest 8(分流/禁用降级/纯色与 level 文本规则免 native)、FileLogEventHandlerTest 7(size/time 轮转/maxAge/JSON 落盘/关闭冲刷, @TempDir+native); 配置经 jing-config.properties 走真实 DefaultConfigurationFacade, 修复 flush.interval 与 flush 键嵌套冲突
- Verified jing-log: mvnd -pl jing-log -am clean install -DskipTests=true 全过; JING_LIBRARY_PATH=jing-bindings/src/main/native/build 下 mvnd -pl jing-log test 全绿——65 用例 0 失败 0 错误, BUILD SUCCESS

## 2026-09-19T17:26:49+08:00
- Fixed jing-log ConsoleLogEventHandler: 超大事件丢弃分支补游标回卷(cursor=0)——此前重分配仍超限时 continue 不重置游标, 后续小事件会带着过期前缀写出, 造成重复/损坏输出(F1); 新增回归 testOversizedEventDroppedWithoutStaleCursor(900k+1.25MB+小事件, 捕获字节恰为两小事件拼接)与 FileLogEventHandlerTest.testOversizedEventDroppedFromFile
- Fixed jing-log LoggerFacadeImpl: 新增 closeHandler 辅助方法——禁用 handler 时先 close 再摘除(此前跳过 AutoCloseable.close 直置 null, native 文件句柄泄漏), 摘除点两处(deliver 失效 + 关闭循环)统一走closeHandler; 新增公开测试缝构造器 (List<LogEventHandler>, int batchMax, long arenaSize), start() 于 handlerIds 为空分支复用预接 handler(绕过配置与 ServiceLoader); 回归 testDisabledHandlerIsClosed(close 计数恰为 1, 无 double-close)(F2)
- Fixed jing-log RegionOut.writeUtf8: 孤立代理项(surrogate 未成对)现编码为 EF BF BD(U+FFFD), 与 appendChar/append 语义一致——此前单独低代理项写裸 E2xx 无效 UTF-8 字节; 新增 RegionOutTest(孤立高/低代理 ∘ U+FFFD, 合法代理对 \\uD83D\\uDE80 → F0 9F 9A 80)(F3)
- Fixed jing-log 两 handler 写出校验: 部分写判定由 v<0 收严为 v != len(负数与短写均判失败)——writeRegion/writeNative 各自抛带实际字节数的异常, fileSize 累计保持在成功全长校验之后, 不重复计数(F4); 回归 ConsoleLogEventHandlerTest.testPartialWriteRejected
- Fixed jing-bindings jing_win.c: jing_log_win_stdout_handle/stderr_handle 在 GetStdHandle 返回 NULL 时按 ERROR_INVALID_HANDLE 走 jing_make_error_ptr 错误编码, 与 INVALID_HANDLE_VALUE 路径一致(此前 NULL 直返被 Java 侧误当有效句柄)(F5); clang-format 已应用并重建 jing_bindings.dll
- Fixed jing-log FileLogEventHandler: maxAge 清理从 rotate() 移至 open() 末尾——启动即扫描一次归档目录, 旋转重开时同样执行, 不必等首次旋转才清理(F6); 回归 testMaxAgeCleanupRunsAtStartWithoutRotation, 原旋转清理测试保持绿
- Fixed jing-log FormatPattern: %logger/%c 现拒绝 width(%5logger 等 → 解析期异常带位置, 消息 does not support a width), 仅 %level 保留宽度支持, 与 specifier 表一致(F7); %d{} 与 %d{ } 空 options 抛 requires options 异常(F8); 对应回归 PatternParserTest.testWidthOnLoggerRejected 与 testEmptyDateOptionsRejected
- Fixed jing-log FileLogEventHandler.renderThrowable: JSON ex 渲染改与 %ex 同源——手写链遍历(漏 suppressed)替换为 StringWriter(256)+printStackTrace, 含 Suppressed/Caused by 全布局(F9); 回归 JsonFormatterTest.testRenderThrowableMatchesExEngine 字节级比对, 并修正该测试原有 frame 前缀断言(\t at 而非 4 空格)
- Simplified jing-log FormatPattern/RegionOut: 越 120 列的错误串拼接拆行(F10); RegionOut.reset()/remaining()/atCapacity() 三个死方法删除, main+test 全仓零调用点(F11)
- Added jing-log 测试: 上述 review 回归用例使测试总数 65 → 75(Console 10/File 9/FormatPattern 18/JsonFormatter 13/LoggerFacade 8/PatternParser 15/RegionOut 2), JING_LIBRARY_PATH 下全绿
- Modified jing-log module-info.java: exports io.jingproject.log to jing.logtest 改为整体 exports io.jingproject.log——按用户决策整个包导出, 模块路径应用可直接访问 LoggerFacadeImpl.LOGGING_CONTEXT(MDC 上下文); RegionOut/FormatPattern/两 handler 随之公开, 按实现细节对待不承诺 API 稳定; 复验 75 用例全绿
- Updated doc/Logging.md: 按用户要求整理设计文档至与最终实现一致——(1) 状态行标 2026-09-19 review 巡检修复; §0.2 骨架表 LogEventHandler 行补 id()/格式串行补模块包导出说明; §0.3 增补条目 13(模块导出决策)与 14(首轮 review 修复清单); (2) D4 接口代码块补 String id() 并说明 id 与配置/ServiceLoader 关系; (3) D5/D9 超限语义由"超过整个 arena"改为"超过单写 region(REGION_SIZE=1MiB, 重分配后仍放不下)丢弃并回卷游标"; (4) D6 语法 width 改为仅 level 支持, specifier 表 %logger 行与严格性规则第 2 条同步; (5) D7 写出失败判定补短写 v != len; (6) D8.1 JSON ex 注明与 %ex 同源(含 suppressed/cause), maxAge 措辞改每次 open(); (7) D9/D10 错误类型由 CfgException 修正为 IllegalArgumentException(消息模板与实现逐字对齐), jing-common 配置装载层 CfgException 单独注明; (8) D10 timezone 行空=系统默认、删 SystemDefault 伪值; (9) D11 增测试类别 8(review 回归)并将基准重排为 9; (10) §2 D2/D3 补 LOGGING_CONTEXT 公开可访问注记; (11) §5 标注 Phase 0–4 与 review 修复完成、LogBench 另行安排
- Verified jing-log: mvnd -pl jing-log test 全绿——75 用例 0 失败 0 错误, BUILD SUCCESS

## 2026-09-19T18:43:40+08:00
- Modified jing-common/conf/CfgUtil.java: TOML 数字解析改为「解析期仅校验格式、不判数值范围」的纯字符串层契约——readTomlNumber 十进制路径删除 long 负累加与全部 out of range 抛错, 数字字节直接拷入 HeapWriteBuffer(负号照写, 下划线校验不变), 终态一次 US_ASCII 解码; accumulateRadix 改为接收 HeapWriteBuffer, 删除溢出检查, 终态 new BigInteger(digits, radix).toString() 任意精度换算十进制; 其余格式消息/零与前导零/radix 前缀/浮点交接逐字节保持原样
- Modified jing-common 测试: CfgTest 拒绝表移除 "a = 9223372036854775808", 新增三个接受用例(a = 9223372036854775808 → "9223372036854775808"、a = -9223372036854775809 → "-9223372036854775809"、a = 0xFFFFFFFFFFFFFFFF → "18446744073709551615"), testJdkConvertibility 保留界内断言并新增超界 NumberFormatException 断言(Long.parseLong 两向 + Integer.parseInt); ConfigurationFactoryTest 新增 testValueAsLongOutOfRangeThrows 断言 valueAsLong/valueAsInt 对超界字面量抛 NumberFormatException——消费期范围判定契约由测试钉死
- Updated doc/Configuration-Behavior.md: 接受表与拒绝表按运行时捕获消息逐字对齐——TOML 接受表 0xFFFFFFFFFFFFFFFF → "18446744073709551615" 并注明带符号 radix 一律拒绝(移除误列的 -0xFFFFFFFFFFFFFFFF 接受行), 拒绝表修正 007→"leading zero", _1→corrupt(值首字节分派即拒), 0_1→"misplaced underscore", .5→corrupt(分派即拒), 5./1.→"invalid toml float", 1e+→"invalid toml float", 1._0→"invalid toml float"; 当前状态的异常消息统一为工作树小写形式(corrupted/corrupt json/toml configuration, 附录 A.2 历史引述列保留提交时大写)
- Updated doc/Configuration-v3.md: §9 其余退出路径消息改小写 corrupt toml configuration; §9 接受/拒绝用例表移除 -0xFFFFFFFFFFFFFFFF 接受行, 该输入归入拒绝(-0xFF 类带符号 radix 拒绝)
- Updated doc/Configuration-v4.md: 测试计数 71→60 与实际套件一致(9.2 成本、10.1 步骤验证、10.2 回归网、架构对比表), 设计代码片段异常消息改小写, 与最终实现逐字一致
- Verified jing-common: mvnd -pl jing-common test 全绿——60 用例 0 失败 0 错误(CfgTest 10 + ConfigurationFactoryTest 6 + DualLockTest 1 + JsonCfgReaderTest 16 + PropertiesCfgReaderTest 10 + TomlCfgReaderTest 17), BUILD SUCCESS
## 2026-09-19T23:12:18+08:00

- Added doc/Logging-Config.md: 新建「配置项参考（实现版）」文档——把 jing-log 当前实现中**所有与配置项有关的部分**逐条导出成实现侧事实清单(非设计稿): 明确与 doc/Logging.md 的关系(Logging.md 为设计规范/D10 为设计侧配置总表, 本文件为实现侧、以源码消费点/默认常量/校验报错/行号为准, 二者冲突时以实现为准); 内容含(1) jing-common 配置装载协议(ConfigurationFactory/DefaultConfigurationFacade, jing.config.file/jing.config.ext 系统属性 + toml→json→properties 探测序 + JING_CONFIG_MAX_DEPTH 深度上限、CfgException 语义)、(2) LoggerFacadeImpl 键总表(jing.log.level/handlers/batch.max/arena.size, 默认 INFO/console/1024/16777216, 范围与整数校验)、(3) ConsoleLogEventHandler 键(jing.log.ansi=true、jing.log.console.pattern=FormatPattern.DEFAULT_PATTERN、jing.log.console.err.level=WARN、jing.log.level.color/逐级 color、jing.log.timezone=""系统默认, 各色名枚举与 unsupported 报错)、(4) FileLogEventHandler 键(jing.log.file.path="logs/jing.log"/pattern/format=log|json/rotation=off|size|time/rotate.size=10485760/rotate.period=daily|hourly/maxBackups=7/maxAge=30/flush=off|interval|immediate/flush.interval=1000, 数值范围与枚举解析)、(5) 默认常量清单(DEFAULT_BATCH_MAX=1024、DEFAULT_ARENA_SIZE=16777216、DEFAULT_PATH、DEFAULT_ROTATE_SIZE=10485760 等, 附源码行号)、校验/报错消息逐字语; 纯文档新增, 不改动任何代码
