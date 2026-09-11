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
