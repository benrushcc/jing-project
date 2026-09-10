# Project Structure

The project uses local JDK 28ea and Maven 3. The top-level directory contains only configuration files; all implementation code resides in individual submodules. Tests use JUnit 6, benchmarks use JMH.

## Submodules

- **jing-common**: Foundation module and core dependency for all other modules. Must not depend on any other module.
- **jing-common-processor**: Core dependency for other annotation processors. Provides code generation primitives and creates a `jing-providers.json` descriptor based on generated classes. This file is consumed by `jing-maven-plugin` to generate the corresponding SPI descriptor files.
- **jing-ffm** & **jing-ffm-processor**: A mechanism for automatically creating downcall bindings through interface definitions and annotations.
- **jing-bindings**: Uses FFM capabilities to implement downcall bindings for native libraries required by other modules in the project.
- **jing-marshall**, **jing-marshall-processor**, **jing-marshall-json**: A serialization framework with its JSON implementation.
- **jing-log**: Default implementation of the logging facade provided by `jing-common`.
- **jing-net**: Default implementation of the network library facade provided by `jing-common`.
- **jing-bench**: JMH performance benchmarks for other submodules. May depend on all other modules.

For detailed module descriptions, refer to the README file in each submodule.

# Guidelines

## Build & Run

- Build: `mvn clean install -DskipTests=true`
- Run benchmark: `java --enable-preview --add-modules jdk.incubator.vector -jar ./jing-bench/target/benchmarks.jar YourBench`

## Dependency Rules

- All modules except `jing-bench` must NOT introduce any third-party dependencies beyond JUnit.
- `jing-bench` may introduce third-party dependencies (e.g. JMH), but document the rationale.

## Code Conventions

- JUnit test classes must end with `Test`.
- JMH test classes must end with `Bench`.
- Generated code may include brief comments. Comments must use `//` only (no `/* */`). Multi-line comments must align `//` vertically. Comments must be written in pure English.
- Indent with 4 spaces. Max line width: 120 characters.
- No `System.out.println` in production code; use the logging facade from `jing-common`.
- Comments and exception messages must start with a lowercase letter. Favor lowercase text throughout, except for proper terms that require uppercase

## Changes Tracking

After modifying code, append a Chinese description of the change reason to `diff.md` in the project root. Create the file if it does not exist. Use the following format with a timestamp:

```markdown
## YYYY-MM-DD
- Modified <module>/<class>: <reason in Chinese>
```

## Restrictions

- Do NOT perform any git operations (no commit, push, branch, etc.).
- Never hardcode secrets, tokens, or credentials in code or configuration.
- Never commit secrets to version control.
