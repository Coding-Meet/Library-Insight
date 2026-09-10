---
name: library-insight
description: Use when the user asks to inspect a Java, Kotlin, or Kotlin Multiplatform (KMP) library API, scan Bill of Materials (BOM) coordinates (compose-bom, firebase-bom), auto-discover versions from Version Catalog (libs.versions.toml), verify method/class availability in installed dependency versions, compare or migrate between versions, audit deprecated APIs, check Gradle dependency graphs, scan local source code, or run commands like li scan and li explain.
---

# Library Insight Agent Skill

Use this skill when you need to understand, inspect, or build AI prompts for:

- Java and Kotlin libraries (JAR/AAR files)
- Kotlin Multiplatform libraries (KLib files)
- Maven Central dependencies
- Local Java or Kotlin source code projects

Library Insight analyzes compiled libraries (JVM artifacts and Kotlin Multiplatform libraries) and local source code to build a searchable API index. It extracts public APIs, type information, documentation, source metadata, and Kotlin-specific language features, allowing AI agents to work with the exact code being used instead of relying on outdated documentation or web examples.

> [!IMPORTANT]
> **AI Agent Token Optimization Rule:**
>
> **DO NOT** read the entire `build/library-insight-index.json` or generated `API_REFERENCE.md` files directly using file-viewing tools. These files can be extremely large and will quickly exhaust the available context window.
>
> Instead, interact with the indexed database using the CLI query commands so that only the required symbols are loaded into context.
>
> Both **`library-insight`** and the short alias **`li`** can be used interchangeably (e.g., `li scan ...`, `li search ...`, `li explain ...`).
>
> **Indexing & Version Discovery**
>
> - Use **`library-insight scan <jar|aar|klib|directory|maven-coordinate>`** to index compiled libraries.
> - **BOM & KMP Scanning:** For Bill of Materials (BOM) coordinates (e.g. `androidx.compose:compose-bom:2024.09.00` or `com.google.firebase:firebase-bom`) and KMP libraries, `library-insight scan` automatically resolves member libraries and applies target platform filtering (`-p, --platform android` by default) to filter out non-target KMP variants (iOS, JS, Wasm), stubs, and lints in seconds. Pass `--platform all` to scan all variants.
> - **Zero-Knowledge Version Discovery:** If the user does not specify a library version, inspect `gradle/libs.versions.toml` or `build.gradle.kts` to discover the project's declared version. If not present in the project, run **`library-insight search-central <query>`** to query the latest coordinate from Maven Central.
> - Use **`library-insight scan-source <directory>`** to index a local Java/Kotlin source project without compilation.
>
> **Querying**
>
> - Use **`library-insight search <query>`** to locate packages, classes, methods, or properties.
> - Use **`library-insight explain <class|function|member> [--deep]`** to inspect a class or top-level Kotlin symbol. Always pass **`--deep`** (or **`-d`**) when exploring DSLs or multi-type APIs (e.g. `library-insight explain Grid --deep` auto-resolves top-level `GridKt` and recursively includes all referenced receiver scopes like `GridScope` and `GridConfigurationScope` in 1 single turn).
> - Use **`library-insight examples <class>`** to generate typical usage examples.
>
> **Analysis**
>
> - Use **`library-insight diff <old> <new>`** to compare two library versions.
> - Use **`library-insight migrate <old> <new>`** to generate a migration report with replacement suggestions.
> - Use **`library-insight audit`** to audit project dependency APIs recursively.
> - Use **`library-insight dependency-check`** to detect runtime linkage and ABI conflicts across transitive dependencies.
> - Use **`library-insight dependency-graph <coordinate>`** to visualize dependency trees.
> - Use **`library-insight semver <old> <new>`** to validate Semantic Versioning API compatibility.
> - Use **`library-insight health`** to generate package health and API complexity reports.
> - Use **`library-insight dsl-report [--package <pkg>]`** to inspect Kotlin DSL surfaces, including `@DslMarker` scopes, type aliases, extension functions, lambda receivers, and inline reified functions.
> - Use **`library-insight callgraph <class.method>`** to recursively trace internal library method invocation call graphs.
>
> **AI Context**
>
> - Use **`library-insight ai-export`** to generate a token-optimized AI context package.
> - Use **`library-insight export`** to export the indexed API as JSON or Markdown documentation.
>
> **Important**
>
> Both **`scan`** and **`scan-source`** generate the same API index format. Once an index has been created, existing commands such as **`search`**, **`explain`**, **`export`**, and **`ai-export`** work identically regardless of whether the data originated from compiled bytecode or local source code.

> [!NOTE]
> **What Library Insight extracts**
>
> Depending on the input, Library Insight indexes:
>
> - Packages
> - Classes
> - Interfaces
> - Enums
> - Java Records
> - Kotlin Objects
> - Companion Objects
> - Constructors
> - Methods
> - Properties and Fields
> - Generic type parameters
> - Nullability information
> - Modifiers
> - Annotations
> - Package imports
> - Source locations (`file:line`)
> - Javadoc
> - KDoc
> - Kotlin metadata (`@Metadata`)
> - Data classes
> - Value classes
> - Sealed classes and interfaces
> - Extension functions
> - Kotlin DSL constructs

> [!TIP]
> **MCP Integration Rule**
>
> If an MCP server is already configured (for example in Cursor, Claude Desktop, VS Code, Windsurf, or another MCP-compatible IDE), prefer using the MCP server instead of invoking the CLI through subprocesses.
>
> The MCP server provides native tools for indexing libraries or source projects, searching indexed symbols, explaining APIs, and serving AI context with lower overhead than repeatedly executing CLI commands.

## Typical Workflows

### Analyze a compiled library

```bash
library-insight scan my-library.jar

library-insight search Retrofit

library-insight explain retrofit2.Retrofit

library-insight ai-export
```

### Analyze a local source project

```bash
library-insight scan-source app/src/main

library-insight search LoginRepository

library-insight explain LoginRepository

library-insight ai-export
```

### Compare two versions

```bash
library-insight diff old.jar new.jar

library-insight migrate old.jar new.jar

library-insight semver old.jar new.jar
```

## Command Reference

The `library-insight` CLI can be executed globally by:

- Installing using the installer:

```bash
curl -fsSL https://raw.githubusercontent.com/Coding-Meet/Library-Insight/main/install.sh | bash
```

- Running the locally installed binary:

```bash
library-insight
```

or

```bash
~/.library-insight/bin/library-insight
```

## Additional Guidance for AI Agents

When answering questions about a library or project:

1. Index the target if it has not already been indexed.
2. Search before explaining.
3. Explain only the symbols relevant to the user's request.
4. Avoid loading the full index into context.
5. Prefer focused symbol lookups over large exports.
6. Generate AI context (`ai-export`) only when broad project understanding is required.
7. When an MCP server is available, prefer MCP tools over spawning CLI processes.

{{COMMAND_REFERENCE}}
