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
> - **BOM & KMP Scanning:** For Bill of Materials (BOM) coordinates (e.g. `androidx.compose:compose-bom:2024.09.00` or `com.google.firebase:firebase-bom`) and KMP libraries, `library-insight scan` automatically resolves member libraries and applies target platform filtering (`-p, --platform android` by default) to filter out non-target KMP variants (iOS, JS, Wasm), stubs, and lints in seconds. Pass `--platform all` to scan all variants. Use **`-i, --include <pattern>`** (e.g. `-i "*foundation*"` or `-i "*ui*"`) to selectively scan matching member artifacts. **AI Agent Rule:** Use surrounding wildcards like `-i "*foundation*"` for include filters to match full group/artifact coordinates. Automatically infer and append missing `-i` and `-p` flags based on intent.
> - **Zero-Knowledge Version Discovery:** If the user does not specify a library version, inspect `gradle/libs.versions.toml` or `build.gradle.kts` to discover the project's declared version. For standard open-source dependencies not declared in the project, run **`library-insight search-central <query>`** to query Maven Central. (Note: AndroidX and Compose libraries are hosted on Google Maven, so check `libs.versions.toml` or `build.gradle.kts` directly).
> - Use **`library-insight scan-source <directory>`** to index a local Java/Kotlin source project without compilation.
>
> **Querying**
>
> - Use **`library-insight search <query>`** to locate packages, classes, methods, or properties.
> - Use **`library-insight explain <class|function|member> [--deep]`** to inspect a class or top-level Kotlin symbol. Always pass **`--deep`** (or **`-d`**) when exploring DSLs or multi-type APIs (e.g. `library-insight explain Grid --deep` auto-resolves top-level `GridKt` and recursively includes all referenced receiver scopes like `GridScope` and `GridConfigurationScope` in 1 single turn).
> - **Inner & Companion Class Shell Escaping:** When querying nested or Companion classes in bash/zsh shell, escape the dollar sign with quotes (e.g. `library-insight explain "GridTrackSize\$Companion"`).
> - Use **`library-insight examples <class>`** to generate typical usage examples (for top-level Kotlin functions, pass the facade class name e.g. `examples GridKt`).
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

When answering questions or generating code for a library or project, AI agents MUST follow this mandatory step-by-step workflow:

1. **Step 1: Version & Coordinates Discovery:** Check `gradle/libs.versions.toml` or `build.gradle.kts` to discover declared library versions. For undeclared libraries, run `library-insight search-central <query>` on Maven Central.
2. **Step 2: Indexing:** Run `library-insight scan <coordinate> -i "<include-pattern>" -p android` (or `scan-source`) to index the target library.
3. **Step 3: Mandatory Symbol Search:** ALWAYS run `library-insight search <query>` (e.g. `library-insight search Grid`) FIRST to discover all matching packages, classes, methods, and properties in the index. **DO NOT skip `search` or jump directly to `explain` without searching first.**
4. **Step 4: Exact Symbol Priority Rule:** Run `library-insight explain <ExactSymbol> --deep` on the exact requested symbol (e.g. `Grid`) or top match from `search` before considering secondary alternatives.
5. **Step 5: Code Generation & File Edits Rule:** Directly modify or create the target source file in the user's project with the version-matched code instead of only outputting raw code snippets in the chat response.

{{COMMAND_REFERENCE}}
