<!-- --8<-- [start:intro] -->

# Library Insight 🔍

### JVM API Explorer & MCP Server

Analyze Java & Kotlin libraries with a standalone CLI or integrate directly into AI IDEs via MCP.

AI coding assistants often guess Java/Kotlin library APIs from outdated documentation, web examples, or a different version than the one installed in your project. That leads to missing methods, deprecated usage, incorrect signatures, and wasted debugging time.

**Library Insight** solves this by scanning the exact JAR, AAR, Gradle output, or Maven dependency used by your project. It reads compiled `.class` structures and Kotlin `@Metadata` annotations to build a searchable, version-correct public API index, ensuring the reported APIs always match the installed version instead of outdated documentation or web examples.

<!-- --8<-- [end:intro] -->

---

## 📖 Documentation

The complete documentation, architecture diagrams, command reference, and integration guides are available at:
👉 **[https://Coding-Meet.github.io/Library-Insight/](https://Coding-Meet.github.io/Library-Insight/)**

---

## Key Features

<!-- --8<-- [start:features] -->

- **MCP Server**: Connect Cursor, Claude Desktop, or any MCP-compatible IDE to query APIs directly.
- **Version-Correct API Lookup**: Scans the exact library version in your project to prevent AI hallucinations.
- **Deep Metadata Extraction**: Extracts classes, constructors, methods, properties, nullability flags, generics, and annotations.
- **Kotlin DSL & Fluent API Mapping**: Scans `@DslMarker` scopes, type aliases, lambda parameter builders, and inline reified functions.
- **Method Call Graph Generator**: Recursively traces and renders method call trees inside bytecode to analyze internal invocations.
- **Automatic Usage Examples**: Auto-generates standard boilerplate usage patterns and extracts guide examples from docs.
- **API Health & Complexity Audits**: Computes public API count distributions, complexity indices, and deprecation ratio scores.
- **Linkage Conflict & ABI Detector**: Scans your classpath for classpath mismatches that could trigger `LinkageError` or `NoSuchMethodError`.
- **Migration Advisor**: Compares two versions and lists deprecated, added, and replacement APIs.
- **Dependency API Audit**: Scans project dependencies and highlights deprecated library APIs inside bytecode.
- **Exporter Tools**: Exports indexes to JSON, readable Markdown reference docs, or token-optimized AI context packages.
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
# 1. Scan a library from Maven Central (or your Gradle cache)
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

We plan to expand Library Insight into a unified **Kotlin Multiplatform (KMP)** API explorer:

- **KLib Metadata Reader**: Parse `.klib` metadata to extract signatures for iOS/Native, JS, and Wasm targets directly (bypassing JVM bytecode dependencies).
- **Platform-Aware Indexing**: Store platform target markers (`common`, `jvm`, `ios`, `js`, `wasm`) in the database schema so AI agents know exactly where APIs are available.
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


