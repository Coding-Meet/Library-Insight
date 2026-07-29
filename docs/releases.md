# Release Notes

All releases for **Library Insight** are documented below.

---

## v1.2.0

_Released on July 29, 2026_

This major update introduces deep Kotlin DSL analysis, bytecode call graph visualization, automated example generation, package health metrics, and compile-time ABI linkage checks to prevent runtime errors.

### 🧩 Kotlin DSL & Fluent API Report (`dsl-report`)

Expose the structure of DSL-heavy libraries. Scans and groups:
- **Type aliases** — extracted from package metadata.
- **DSL scopes** — classes carrying `@DslMarker` annotations.
- **Extension functions** — mapped to their receiver types.
- **Lambda builders** — functions taking lambda-with-receiver blocks.
- **Inline reified entry points**.

*Also registered as the `dsl_report` tool on the MCP server!*

```bash
library-insight dsl-report
```

### 🌳 Method Call Graph Generator (`callgraph`)

Recursively traces method call instructions to render a visual tree of internal invocations inside the bytecode. Excellent for understanding execution flows and auditing internals.

```bash
library-insight callgraph HtmlBuilder.div
```

### 📝 Automatic Examples Generator (`examples`)

Generates typical instantiation patterns (constructors, builders, factories, singletons) from bytecode signatures, and extracts code blocks from Dokka/README markdown files.

```bash
library-insight examples HtmlBuilder
```

### 🩺 Package Health & Complexity (`health`)

Analyzes structural complexity indices, public API distributions (classes, methods, properties), and deprecation ratios to grade the library.

```bash
library-insight health
```

### 🚨 Classpath ABI Linkage Detector (`dependency-check`)

Checks compiled transitive dependencies for method and field signatures missing from the classpath. Proactively flags runtime risks such as `NoSuchMethodError`, `NoSuchFieldError`, or `LinkageError`.

```bash
library-insight dependency-check
```

### ⚙️ MCP Server Enhancements

- **`dsl_report` Tool**: AI assistants can query DSL structures natively.
- **Dynamic Database (`--db`)**: Run multiple indices by passing custom database file paths.
- **Smart Local Scans**: Parses local JAR filenames (`retrofit-2.11.0.jar` -> version `2.11.0`) and auto-detects sister `-sources` archives in the same directory.

---

## v1.1.0

_Released on July 25, 2026_

This release evolves Library Insight from a CLI scanner into a full **JVM API Explorer & MCP Server** — your AI IDE can now use it automatically, without any manual commands.

### 🔌 MCP Server (Model Context Protocol)

Connect Cursor, Claude Desktop, or any MCP-compatible IDE directly. AI agents can call `scan_library`, `search_symbols`, and `explain_class` natively without leaving the editor.

```bash
library-insight mcp
```

### 🔄 Migration Advisor (`migrate`)

Compare two library versions and get a structured report of:

- ❌ Removed classes and methods
- ⚠️ Deprecated APIs with replacement suggestions
- ✅ Added APIs

Perfect for upgrading Retrofit, OkHttp, Compose, or Kotlin.

```bash
library-insight migrate com.squareup.retrofit2:retrofit:2.9.0 com.squareup.retrofit2:retrofit:2.11.0
```

### 🔍 Dependency API Audit (`audit`)

Scan all project Gradle dependencies recursively and report deprecated classes, methods, and properties found in the actual bytecode.

```bash
library-insight audit
```

### 🌳 Dependency Graph (`dependency-graph`)

Print a visual recursive tree of transitive compile dependencies from POM descriptors.

```bash
library-insight dependency-graph com.github.ajalt.clikt:clikt-jvm:4.4.0
```

### 🔎 Search Maven Central (`search-central`)

Find Maven coordinates and versions without leaving the terminal.

```bash
library-insight search-central retrofit
```

### ✅ SemVer Compliance Checker (`semver`)

Verify that a version bump correctly reflects the actual bytecode changes. Flags unbumped breaking changes.

```bash
library-insight semver com.squareup.retrofit2:retrofit:2.9.0 com.squareup.retrofit2:retrofit:2.11.0
```

### 📋 Other Improvements

- **Centralized logging** — all commands write structured logs; send log files when reporting issues.
- **`quick-demo.sh`** — 6-command, 2–3 minute onboarding walkthrough (great for YouTube).
- **`docs/cli.md`** — complete 16-command reference extracted from README.
- **README slimmed down** — Installation + Quick Start only, with links to full reference.
- **AI Skill updated** — MCP server preference rule added: _if MCP is available, prefer it over the CLI_.

---

## v1.0.0

_Released on July 20, 2026_

Library Insight is a bytecode-driven command-line tool designed to inspect, analyze, and index compiled Java & Kotlin libraries (JARs, AARs, or Maven coordinates) directly without requiring source code.

### ✨ Key Features & Capabilities

- 📦 **Bytecode & Metadata Extraction**: Parses `.class` bytecode using **ASM** and decodes Kotlin `@Metadata` annotations using `kotlin-metadata-jvm` across JARs, AARs, and Gradle build outputs.
- ⚡ **Offline-First Gradle Caching**: Automatically checks local Gradle module caches (`~/.gradle/caches/modules-2/files-2.1/`) before fetching from repositories, enabling zero-copy, fully offline scans.
- 🔍 **Symbol Search (`search`)**: Fast, case-insensitive lookup across packages, classes, interfaces, methods, constructors, and properties.
- 📖 **API Inspector (`explain`)**: Detailed inspection of class structures, modifiers, extension receivers, suspend/inline flags, and Javadocs.
- 🔄 **Semantic Version Diffing (`diff`)**: Compares two library archives to detect binary breaking changes (deleted methods, visibility reductions, changed modifiers).
- 🤖 **Token-Efficient AI Context Export (`ai-export`)**: Splits the extracted API database into small per-class JSON files (`build/ai-context/`), reducing LLM context window bloat by up to 95% for coding assistants (Cursor, Gemini, Claude, Copilot).
- 🩺 **Diagnostics Engine (`doctor`)**: System diagnostic health checks for JRE 17+ environments, local cache status, and global AI agent skill configurations.
