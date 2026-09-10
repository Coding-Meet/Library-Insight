<!-- --8<-- [start:intro] -->

# Library Insight 🔍

### API Explorer & MCP Server for Java, Kotlin & KMP

Analyze Java and Kotlin libraries, JVM artifacts, Kotlin Multiplatform libraries, and local source code with a standalone CLI or integrate directly into AI IDEs via MCP.

AI coding assistants often guess Java/Kotlin APIs from outdated documentation, web examples, or a different version than the one used in your project. That leads to missing methods, deprecated usage, incorrect signatures, and wasted debugging time.

**Library Insight** solves this by analyzing the exact JAR, AAR, Maven dependency, Gradle output, or local Java/Kotlin source code used by your project. It builds a searchable, version-aware API index from compiled bytecode (including Kotlin `@Metadata`) or source code, allowing you to explore APIs, generate AI-ready context, and understand your codebase using the exact code you're working with—not outdated documentation or web examples.

> **Developer Prompt Example:**
> _"Use my `library-insight` CLI to scan `androidx.compose.foundation:foundation-layout:1.12.0`, search for `Grid`, explain its DSL structure, and create a working Jetpack Compose Grid layout example."_
>
> **AI Agent Response Workflow:**
>
> 1. Executes `library-insight scan androidx.compose.foundation:foundation-layout:1.12.0`
> 2. Executes `library-insight search Grid`
> 3. Executes `library-insight explain Grid --deep` to inspect parameters, receiver scopes (`GridScope`), and embedded KDocs in 1 turn.
> 4. Generates **100% accurate, version-correct Kotlin code** grounded directly in the artifact bytecode—without hallucinating deprecated signatures or relying on outdated web snippets!

<!-- --8<-- [end:intro] -->

---

## 📖 Documentation

The complete documentation, architecture diagrams, command reference, and integration guides are available at:
👉 **[https://Coding-Meet.github.io/Library-Insight/](https://Coding-Meet.github.io/Library-Insight/)**

---

## 📺 Video Walkthrough

Watch the full **13-minute product showcase** explaining Library Insight's core workflows, local source scanning, version diffing, Kotlin Multiplatform support, and AI IDE integrations:

<p align="center">
  <a href="https://www.youtube.com/watch?v=jmvBqjGE_gg" target="_blank">
    <img src="https://img.youtube.com/vi/jmvBqjGE_gg/maxresdefault.jpg" alt="Library Insight Walkthrough Video" width="800" style="max-width: 100%; border-radius: 8px; box-shadow: 0 4px 20px rgba(0,0,0,0.15);" />
  </a>
</p>

---

## Key Features

<!-- --8<-- [start:features] -->

- **MCP Server**: Connect Cursor, Claude Desktop, or any MCP-compatible IDE to query APIs directly.
- **Kotlin Multiplatform (KMP) Support**: Resolve coordinates from Gradle Module Metadata (`.module` JSON), parse Native `.klib` metadata files, and merge platform variant API targets (`common`, `jvm`, `ios`, `js`, `wasm`).
- **Bill of Materials (BOM) Auto-Resolution**: Parse Maven BOM POM XML files (`compose-bom`, `firebase-bom`) and automatically resolve, download, scan, and merge all constituent managed library artifacts into a unified API index.
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

## Roadmap

<!-- --8<-- [start:roadmap] -->

We plan to expand Library Insight with deep source analysis and automated project sync:

### 1. Source Analysis Engine

- **References Engine**: Build a symbol-to-usage index to locate references for any class, method, or property across your local codebase (e.g. `library-insight references LoginRepository`).
- **Implementations**: Query interface implementations or subclass declarations (e.g. `library-insight implementations Repository` -> `RoomRepository`, `NetworkRepository`).
- **Hierarchy**: Render visual inheritance trees for any base class or interface (e.g. `library-insight hierarchy BaseViewModel`).
- **Source Call Graph**: Trace internal method execution paths using raw source file declaration locations.

### 2. Project Auto-Sync (`library-insight sync`)

- **Version Catalog & Gradle Sync**: Automatically parse `gradle/libs.versions.toml` and `build.gradle.kts` in your project root to auto-index all declared project dependencies in 1 command without manually typing coordinates.

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
git tag v1.5.0
git push origin v1.5.0
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
