<!-- --8<-- [start:intro] -->

# Library Insight 🔍

### JVM API Explorer & MCP Server

Analyze Java & Kotlin libraries and source code with a standalone CLI or integrate directly into AI IDEs via MCP.

AI coding assistants often guess Java/Kotlin APIs from outdated documentation, web examples, or a different version than the one used in your project. That leads to missing methods, deprecated usage, incorrect signatures, and wasted debugging time.

**Library Insight** solves this by analyzing the exact JAR, AAR, Maven dependency, Gradle output, or local Java/Kotlin source code used by your project. It builds a searchable, version-aware API index from compiled bytecode (including Kotlin `@Metadata`) or source code, allowing you to explore APIs, generate AI-ready context, and understand your codebase using the exact code you're working with—not outdated documentation or web examples.

<!-- --8<-- [end:intro] -->

---

## 📖 Documentation

The complete documentation, architecture diagrams, command reference, and integration guides are available at:
👉 **[https://Coding-Meet.github.io/Library-Insight/](https://Coding-Meet.github.io/Library-Insight/)**

---

## Key Features

<!-- --8<-- [start:features] -->

- **MCP Server**: Connect Cursor, Claude Desktop, or any MCP-compatible IDE to query APIs directly.
- **Local Source Code Scanner (`scan-source`)**: Analyze Kotlin and Java source projects without compilation, preserving KDoc/Javadoc, imports, and declaration source locations (`file:line`).
- **Version-Correct API Lookup**: Build an API index from the exact JAR, AAR, Maven dependency, Gradle output, or source code used by your project to prevent AI hallucinations.
- **Deep Metadata Extraction**: Extract classes, constructors, methods, properties, nullability, generics, annotations, modifiers, and source metadata.
- **Kotlin DSL & Fluent API Mapping**: Detect `@DslMarker` scopes, type aliases, lambda builders, extension functions, and inline reified functions.
- **Method Call Graph Generator**: Trace and visualize method call trees to understand internal bytecode invocations.
- **Automatic Usage Examples**: Generate common usage patterns and extract examples from available documentation.
- **API Health & Complexity Reports**: Measure public API size, complexity metrics, and deprecation ratios.
- **Dependency & ABI Analysis**: Detect classpath conflicts, linkage issues (`LinkageError`, `NoSuchMethodError`), and deprecated dependency APIs.
- **Migration Advisor**: Compare two library versions and identify added, removed, deprecated, and replacement APIs.
- **Exporter Tools**: Export API indexes as JSON, Markdown reference documentation, or token-optimized AI context packages.
<!-- --8<-- [end:features] -->

---

## Quick Start

### 1. Installation

Install the CLI globally on your system (requires JDK 17+):

```bash
curl -fsSL https://raw.githubusercontent.com/Coding-Meet/Library-Insight/main/install.sh | bash
```

### 2. Inspecting a Library

```bash
# 1. Scan a local source project directory
library-insight scan-source src/main

# Or scan a compiled library from Maven Central (or your Gradle cache)
library-insight scan com.squareup.retrofit2:retrofit:2.11.0

# 2. Search for a class
library-insight search Retrofit

# 3. Explain API signatures and KDocs
library-insight explain Retrofit

# 4. Compare two versions for compatibility / breaking changes
library-insight diff com.squareup.retrofit2:retrofit:2.9.0 com.squareup.retrofit2:retrofit:2.11.0
```

### 3. Connect to MCP

Start the stdio-based MCP server:

```bash
library-insight mcp
```

For setup instructions in Cursor or Claude Desktop, see the [MCP Integration Guide](https://Coding-Meet.github.io/Library-Insight/mcp/).

---

## 🚀 Roadmap

<!-- --8<-- [start:roadmap] -->

We plan to expand Library Insight with deep source-level analysis capabilities, a three-layer clean architecture, and unified Kotlin Multiplatform (KMP) support:

### 🔍 Source Analysis Engine (Planned)
- **References Engine**: Build a symbol-to-usage index to locate references for any class, method, or property across the codebase (e.g. `library-insight references LoginRepository`).
- **Implementations**: Query all interface implementations or subclass declarations (e.g. `library-insight implementations Repository` -> `RoomRepository`, `NetworkRepository`).
- **Hierarchy**: Render the visual inheritance tree for any base class or interface (e.g. `library-insight hierarchy BaseViewModel`).
- **Source Call Graph**: Trace internal method execution paths using raw source file declaration locations.

### 🏗️ Clean Architectural Layers
To simplify maintenance, we are partitioning the codebase into three clean layers:
1. **Scanner Layer** (`scan`, `scan-source`) — Processes raw inputs (bytecode, sources, metadata) and compiles them.
2. **Unified Database** — Serves as the single serialization schema and repository index.
3. **Analysis & Tooling Layer** (`search`, `explain`, `references`, `implementations`, `hierarchy`, `callgraph`, `ai-export`, `export`) — Consumes the database and provides rich diagnostic tools.

### 📦 Kotlin Multiplatform (KMP) Support
- **KLib Metadata Reader**: Parse `.klib` metadata to extract signatures for iOS/Native, JS, and Wasm targets directly (bypassing JVM bytecode dependencies).
- **Platform-Aware Indexing**: Store platform target markers (`common`, `jvm`, `ios`, `js`, `wasm`) in the database schema.
- **KMP Coordinate Resolution**: Auto-resolve platform split coordinates (e.g. `ktor-client-core-iosarm64`) from the root KMP library Maven coordinate.
<!-- --8<-- [end:roadmap] -->

---

## License

This project is licensed under the Apache License, Version 2.0. See the [LICENSE](LICENSE) file for the full license text.

---

## 🛠️ Developer Cheatsheet (Internal Use)

Quick reference commands for development, testing, documentation, and releasing:

### 1. Agent Skill Generation

Regenerate [SKILL.md](file://.agents/skills/library-insight/SKILL.md) after editing [docs/cli.md](file://docs/cli.md):

```bash
./gradlew generateAgentSkill
```

### 2. Testing

Run the complete unit test suite across all modules:

```bash
./gradlew test
```

### 3. Documentation Site (MkDocs)

Manage the documentation website locally:

```bash
# Preview the docs site locally with live-reload (default: http://localhost:8000)
mkdocs serve --livereload

# Build static HTML site files
mkdocs build

# Force deploy documentation to GitHub Pages (gh-pages branch)
mkdocs gh-deploy --force
```

### 4. Releasing & Version Tagging

Release and publish a new version tag to GitHub:

```bash
git tag v1.2.0
git push origin v1.2.0
```

### 5. Demos

Run the command-line walkthrough scripts:

```bash
# Run the 3-minute quick command suite demo
./quick-demo.sh

# Run the comprehensive 21-command suite demo
./demo.sh
```

### 6. Build Distributions

Generate application binary packages (ZIP, TAR, and local install distributions):

```bash
./gradlew installDist distZip distTar
```
