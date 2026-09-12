# Changes

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
