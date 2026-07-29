# Library Insight 🔍

### JVM API Explorer & MCP Server for AI IDEs

AI coding assistants often guess Java/Kotlin library APIs from old docs, latest web examples, or a different version than the one installed in your project. That leads to missing methods, deprecated usage, wrong signatures, and wasted debugging time.

**Library Insight** fixes that by scanning the exact JAR/AAR, Gradle output, or Maven version you use. It reads compiled `.class` structures and Kotlin `@Metadata` annotations to build a searchable, version-correct public API index.

---

## 📖 Documentation

The complete documentation, architecture diagrams, command reference, and integration guides are available at:
👉 **[https://Coding-Meet.github.io/Library-Insight/](https://Coding-Meet.github.io/Library-Insight/)**

---

## Key Features

*   **MCP Server**: Connect Cursor, Claude Desktop, or any MCP-compatible IDE to query APIs directly.
*   **Version-Correct API Lookup**: Scans the exact library version in your project to prevent AI hallucinations.
*   **Deep Metadata Extraction**: Extracts classes, constructors, methods, properties, nullability flags, generics, and annotations.
*   **Migration Advisor**: Compares two versions and lists deprecated, added, and replacement APIs.
*   **Dependency API Audit**: Scans project dependencies and highlights deprecated library APIs inside bytecode.
*   **Exporter Tools**: Exports indexes to JSON, readable Markdown reference docs, or token-optimized AI context packages.

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

## License

Copyright 2026 Library Insight Authors. Licensed under the Apache License, Version 2.0.
