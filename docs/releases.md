# Release Notes

All releases for **Library Insight** are documented below.

---

## v1.5.0

_Released on September 10, 2026_

This major feature release introduces **BOM (Bill of Materials) Auto-Resolution**, **Deep Recursive DSL & Scope Resolution**, **Smart Symbol Facade & Typo Lookup**, and native **`li` short CLI command alias** support across all platforms.

### 📦 Major Feature Highlights

- **BOM (Bill of Materials) Auto-Resolution**: Added automatic Maven POM XML parsing and `<dependencyManagement>` expansion for BOM coordinates (e.g., `androidx.compose:compose-bom` or `com.google.firebase:firebase-bom`). Scanning a BOM coordinate automatically resolves, downloads, and merges all constituent managed library artifacts (e.g., 92 Compose libraries) into a unified API index.
- **Deep Recursive Explain (`-d`, `--deep`)**: Added single-turn recursive DSL receiver scope, parameter type, and return type expansion. Running `library-insight explain Grid --deep` automatically discovers and outputs all referenced receiver scopes (`GridScope`, `GridConfigurationScope`) and track specs in 1 single turn, eliminating multi-turn AI exploration loops.
- **Smart `explain` Symbol Resolution**: Enhanced `explain` to automatically map top-level Kotlin functions to their facade classes (e.g., `explain Grid` → `GridKt`), resolve member method/property queries directly to their declaring classes, and provide Levenshtein fuzzy typo suggestions ("Did you mean one of these?").
- **`li` Native Short Command Alias**: Added native binary executable generation for `li` alongside `library-insight` across macOS, Linux, and Windows (`li.bat`), allowing users and AI agents to execute commands using either `library-insight` or `li`.
- **Zero-Knowledge Version Catalog Discovery**: Integrated smart project inspection for `gradle/libs.versions.toml` and `build.gradle.kts` to automatically discover library versions when omitted by developers.

---

## v1.4.1

_Released on August 17, 2026_

This is a maintenance release that improves CLI version checks, resolves relative path scanner bugs, silences KMP fallback noise, and enhances AI auto-discovery.

### 🐛 Bug Fixes & Refactoring

- **Quiet KMP Target Fallbacks**: Silenced failed variant target resolution warnings (like `Warn: Failed to resolve variant...`) when scanning non-Kotlin Multiplatform libraries (e.g. Retrofit), ensuring a quiet console log fallback to standard JVM artifacts.
- **Relative Path Canonicalization**: Fixed a bug where scanning source files in the current folder (e.g. `scan-source .`) resolved the library index name literally to `"."`. It now correctly uses the normalized parent directory name, restoring path resolving for local callgraph analysis.

### 🖥️ CLI Option & Skill Discovery Updates

- **Native Version Command**: Added the standard Clikt `versionOption` so running `library-insight -v` or `library-insight --version` outputs the active CLI build version dynamically.
- **AI Agent Skill Auto-Discovery**: Added explicit trigger phrases inside the YAML frontmatter description of `SKILL.md` to help agent frameworks (such as Claude Code) auto-enable the tool proactively.

---

## v1.4.0

_Released on August 16, 2026_

This release introduces first-class Kotlin Multiplatform (KMP) support to download, parse, and merge platform-specific targets (`.klib`, JVM `.jar`/`.aar`) from a single root coordinate.

### 📦 Kotlin Multiplatform (KMP) Support

- **Gradle Module Metadata Resolution**: Resolves target split coordinates (e.g. `iosarm64`, `js`, `wasm-js`, `jvm`) automatically from Gradle Module Metadata (`.module` JSON).
- **KLIB Metadata Scanner**: Reads platform target tags and package structures directly from Kotlin Native `.klib` metadata ZIP archives.
- **Unified Multiplatform Merging**: Consolidates package declarations, constructors, methods, and properties across all target platforms into a single unified index.
- **Platform-Aware Reports**: Displays target annotations (e.g. `[common]`, `[jvm]`) in explain reports and MCP tool outputs when signatures vary by platform.

### 🖥️ CLI Updates & Architecture Refactoring

- **Self-Updating Engine (`update` Command)**: Checks for the latest version on GitHub, automatically upgrades the CLI binaries, and distributes the updated Agent Skills dynamically.
- **Clean Architecture Partitioning**: Fully modularized the codebase into Scanner Layer, Unified Database, and Tooling/Analysis Layer.
- **Configuration Cache Compliance**: Dynamic CLI version resolution configured safely, enabling 100% compatibility with Gradle's Configuration Cache.
- **Categorized Modular Documentation**: CLI Reference split into 6 functional files under `docs/cli/` to improve website navigation and AI agent contextual lookup.

---

## v1.3.0

_Released on July 31, 2026_

This release introduces `scan-source`, enabling Library Insight to analyze local Kotlin and Java source projects without compilation. Source declarations, documentation, imports, and precise source locations are indexed into the same unified API database used for compiled libraries.

### 🔍 Raw Source Directory Scanner (`scan-source`)

Scan raw source code directories containing Kotlin (`.kt`) and Java (`.java`) files without compiling them. It generates the same unified API index as the existing scan command, allowing existing commands such as search, explain, export, and ai-export to work without any workflow changes.

```bash
library-insight scan-source app/src/main
```

**Features:**

- **Java Source Parsing**: Uses JavaParser to extract classes, interfaces, enums, records, constructors, methods, fields, generics, annotations, modifiers, and Javadocs.
- **Kotlin Source Parsing**: Uses the official Kotlin compiler PSI (`kotlin-compiler-embeddable`) to extract KDoc, modifiers, companion objects, secondary constructors, extension receivers, generics, and Kotlin-specific language features.

### 📍 Source Location & Imports Indexing

- **Source Locations**: Stores the relative file path, line number, and column for every indexed class, constructor, method, and property.
- **Import Statements**: Persists file-level import lists inside the class API model, helpful for dependency analysis and code structure auditing.
- **Backward-Compatible Database**: Existing bytecode-scanned indices are backward-compatible and load seamlessly without schema validation breaks.

### 🖥️ CLI Improvements

- scan-source displays Kotlin/Java file statistics and indexing progress.
- search now shows declaration source locations when available.
- explain now displays source locations, imports, and declaration locations for methods and properties.

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

_Also registered as the `dsl_report` tool on the MCP server!_

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
