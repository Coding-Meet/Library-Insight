---
name: library-insight
description: JVM API Explorer & MCP Server that indexes public APIs, visualizes dependency graphs, audits version deprecations, and extracts JVM archive signatures (JAR/AAR) or Maven coordinates for AI context.
---

# Library Insight Agent Skill

Use this skill when you need to understand, inspect, or build AI prompts for external JVM libraries (Java/Kotlin JAR or AAR files) or public Maven dependency coordinates.

This tool extracts all classes, interfaces, methods, properties, and Javadoc/KDoc comments from bytecode and sources, creating an indexed database and a split AI-friendly directory structure.

> [!IMPORTANT]
> **AI Agent Token Optimization Rule:**
> **DO NOT** read the entire `build/library-insight-index.json` or `API_REFERENCE.md` files directly using file-viewing tools. They contain massive raw dumps of the entire library surface which will exhaust your context window.
> Instead, you **MUST** use the CLI query subcommands to interactively look up only the specific information you need:
> - Use **`library-insight search <query>`** to find packages or classes.
> - Use **`library-insight explain <class>`** to print the public API signature and Javadocs of a specific class.
> - Use **`library-insight diff <old> <new>`** to compare versions.
> - Use **`library-insight audit`** to check project dependency API deprecations recursively.
> - Use **`library-insight migrate <old> <new>`** to get a migration advisor report with replacement suggestions.
> - Use **`library-insight search-central <query>`** to search Maven Central.
> - Use **`library-insight dependency-graph <coord>`** to visualize dependency trees.
> - Use **`library-insight semver <old> <new>`** to lint Semantic Versioning API compliance.
> - Use **`library-insight mcp`** to communicate as a Model Context Protocol (MCP) server.
> - Use **`library-insight dsl-report [--package <pkg>]`** to generate a Kotlin DSL surface report (type aliases, `@DslMarker` scopes, extension functions, lambda receivers, reified functions).
> - Use **`library-insight examples <class>`** to generate typical usage code examples from bytecode signatures.
> - Use **`library-insight health`** to generate a Package Health & API Complexity Report.
> - Use **`library-insight dependency-check`** to audit all transitive classpath dependencies for runtime conflicts.
> - Use **`library-insight callgraph <class.method>`** to trace internal library method invocation call graphs recursively.
>
> **MCP Integration Rule:**
> If an MCP server is already configured and available (e.g., in Cursor, Claude Desktop, or another IDE with MCP support), **prefer the MCP server over shelling out to the CLI**. The MCP server exposes `scan_library`, `search_symbols`, and `explain_class` tools natively without subprocess overhead.

## Command Reference

The command line tool `library-insight` can be executed globally by:
* Installing via installer script: `curl -fsSL https://raw.githubusercontent.com/Coding-Meet/Library-Insight/main/install.sh | bash`
* Running local build: `~/.library-insight/bin/library-insight` (linked globally as `library-insight`)

## Available scripts

- **`scripts/install-cli.sh`** — Installs the `library-insight` command globally on the host system if not already available.
  ```bash
  bash scripts/install-cli.sh
  ```

### 1. Scan Dependencies (`scan`)
Scans a local JAR/AAR file, a directory of JARs, or resolves a Maven coordinate over HTTP, downloading it and its corresponding `-sources.jar` automatically from repositories (Maven Central, Google Maven, SoftBank).

*(Note: In Gradle/Kotlin project directories, downloaded artifacts are saved locally to `build/library-insight/cache/`. To run fully offline, the scanner automatically references the local machine's Gradle caches (`~/.gradle/caches/modules-2/files-2.1/`) directly without copying, saving disk space).*

* **Scan Maven Coordinate**:
  ```bash
  library-insight scan com.squareup.retrofit2:retrofit:2.11.0

  # Optional Parameters:
  #   --db <file>             Path to save the JSON index database (default: build/library-insight-index.json)
  #   -s, --sources <file>    Path to sources JAR/AAR or source code folder to extract Javadoc/KDoc comments & guide examples
  #   --repo <url>            Additional Maven repository URL to download coordinates (multiple allowed)
  #   --lib-name <name>       Override the library name in the generated index
  #   --lib-version <version> Override the version tag in the generated index
  library-insight scan com.squareup.okhttp3:okhttp:4.12.0 --sources okhttp-sources.jar --repo https://maven.google.com

**Example Output:**
```text
Detected Maven coordinate: com.squareup.retrofit2:retrofit:2.11.0
  -> Using cached binary JAR from Gradle cache: retrofit-2.11.0.jar
  -> Using cached sources JAR from Gradle cache: retrofit-2.11.0-sources.jar
Scan complete! Found 113 classes across 3 packages.
Saved API index to: /Users/meet/AndroidStudioProjects/Library-Insight/build/library-insight-index.json
```

### 2. Search Symbols (`search`)
Search for classes, packages, methods, or properties in the saved index.
```bash
library-insight search Retrofit

# Optional Parameters:
#   --db <file>             Index database JSON file path to read from (default: build/library-insight-index.json)
library-insight search "anno:Keep" --db custom-index.json

**Example Output:**
```text
Found 2 matching classes:
  - retrofit2.Retrofit
  - retrofit2.Retrofit$Builder
```

### 3. Explain Class (`explain`)
Print detailed structural details (modifiers, superclasses, constructors, properties, and methods) with their documentation.
```bash
library-insight explain Retrofit

# Optional Parameters:
#   --db <file>             Index database JSON file path to read from (default: build/library-insight-index.json)
library-insight explain HtmlBuilder --db custom-index.json

**Example Output:**
```text
Class: retrofit2.Retrofit (public class)
  Constructors:
    + public constructor(okhttp3.Call$Factory, okhttp3.HttpUrl, java.util.List<retrofit2.Converter$Factory>, java.util.List<retrofit2.CallAdapter$Factory>, java.util.concurrent.Executor, boolean)
  Methods:
    + public fun <T> create(java.lang.Class<T>): T
    + public fun baseUrl(): okhttp3.HttpUrl
```

### 4. Export Index (`export`)
Export the index database to pretty JSON or structured Markdown reference sheets.
*(Note: For large libraries, single Markdown sheets are huge; use `ai-export` for AI context instead).*
```bash
library-insight export markdown

# Optional Parameters:
#   --db <file>             Index database JSON file path to read from (default: build/library-insight-index.json)
#   -o, --output <file>     Target output file path to write export content to
library-insight export markdown --db custom-index.json -o API_REFERENCE.md

**Example Output:**
```text
Exported MARKDOWN to: /Users/meet/AndroidStudioProjects/Library-Insight/build/API_REFERENCE.md
```

### 5. Diff Library Versions (`diff`)
Compare two library versions to check for changes and potential breaking changes.
```bash
library-insight diff retrofit-2.9.0.jar retrofit-2.11.0.jar
```

**Example Output:**
```text
==================================================
 LIBRARY INSIGHT API DIFF REPORT
==================================================
Old: retrofit-2.9.0
New: retrofit-2.11.0
Breaking Changes Found: NO
==================================================
➕ Added Classes:
  - retrofit2.Reflection
📝 Changed Classes:
  Class: retrofit2.Invocation
    Added Methods:
      + fun service(): java.lang.Class<?>
```

### 6. Export AI Context (`ai-export`)
**Recommended for AI Integration.** Instead of a single giant `API_REFERENCE.md` file, this splits the scanned database into a token-efficient directory structure under `build/ai-context/`. AI agents can inspect `metadata.json` first, and then load only the specific class JSON files they need, reducing token usage by 95%+:
```bash
library-insight ai-export

# Optional Parameters:
#   --db <file>             Index database JSON file path to read from (default: build/library-insight-index.json)
#   -o, --output-dir <dir>  Target output directory to save AI context files (default: build/ai-context/)
library-insight ai-export --db custom-index.json -o custom-ai-context/

**Example Output:**
```text
Generated compact LLM context directory structure at: /Users/meet/AndroidStudioProjects/Library-Insight/build/ai-context
```

### 7. Clear Cache (`clear-cache`)
Clears all downloaded cached Maven binaries and sources from local storage.
```bash
library-insight clear-cache
```

**Example Output:**
```text
Cache cleared successfully. Deleted 2.45 MB.
```

### 8. Diagnostics & Doctor (`doctor`)
Run diagnostic checks for the Java runtime, local cache directory, and verify all global AI Agent skill configurations.
```bash
library-insight doctor
```

**Example Output:**
```text
[Library Insight Diagnostics]
1. Java Runtime Environment (JRE):
   - Path: /Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home/bin/java
   - Version: 17.0.7
   - Status: OK (Java 17+ verified)
2. Local Cache Directory:
   - Path: /Users/meet/AndroidStudioProjects/Library-Insight/build/library-insight/cache
   - Status: OK
3. AI Agent Skill Registrations:
   - Gemini Config Skill: ACTIVE (registered)
   - Cursor Skill: ACTIVE (registered)
```

### 9. Model Context Protocol Server (`mcp`)
Start the Model Context Protocol (MCP) server listening on stdio. Allows external AI clients (like Cursor or Claude Desktop) to invoke `scan_library`, `search_symbols`, and `explain_class` tools directly.
```bash
library-insight mcp
```

### 10. Initialize Workspace Skill (`init`)
Write a `SKILL.md` into `.agents/skills/library-insight/` so local AI agents auto-discover the CLI.
```bash
library-insight init
```

### 11. Manage Agent Skills (`skills`)
Add or list AI agent skills registration details in detected IDE configuration directories.
```bash
library-insight skills add
library-insight skills list
```

### 12. Dependency API Audit (`audit`)
Scan and audit all Gradle build file dependencies recursively to find deprecated classes, methods, and properties inside active versions.
```bash
library-insight audit
```

**Example Output:**
```text
==================================================
      Library Insight Dependency Audit
==================================================
Detected Gradle Version Catalog at gradle/libs.versions.toml
Scanning 11 Gradle build file(s)...
Found 10 dependencies to audit.
...
Audit Summary: Scanned 10 libraries successfully.
Total Deprecated APIs found: 1819
==================================================

### 13. API Migration Advisor (`migrate`)
Compare two library versions and output a migration advisor report showing removed, deprecated, and replacement APIs.
```bash
library-insight migrate com.squareup.retrofit2:retrofit:2.9.0 com.squareup.retrofit2:retrofit:2.11.0

# Optional Parameters:
#   --repo <url>            Additional Maven repository URLs to resolve coordinates (multiple allowed)
library-insight migrate com.squareup.retrofit2:retrofit:2.9.0 com.squareup.retrofit2:retrofit:2.11.0 --repo https://repo.maven.apache.org/maven2
```

### 14. Search Maven Central (`search-central`)
Search Maven Central Solr repository indices dynamically for matching packages and versions:
```bash
library-insight search-central retrofit
```

### 15. Dependency Graph (`dependency-graph`)
Renders a visual hierarchical tree of dependencies resolved recursively from `.pom` XML package descriptors:
```bash
library-insight dependency-graph com.github.ajalt.clikt:clikt-jvm:4.4.0
```

### 16. SemVer Compliance (`semver`)
Lints version bumps against actual bytecode modifications to enforce Semantic Versioning (SemVer) compliance:
```bash
library-insight semver com.squareup.retrofit2:retrofit:2.9.0 com.squareup.retrofit2:retrofit:2.11.0
```

### 17. Kotlin DSL Surface Report (`dsl-report`)

Generates a structured report of the library's Kotlin DSL API surface. Essential for DSL-heavy libraries.

Report sections:
- **Type aliases** — extracted from Kotlin package metadata
- **DSL scopes** — classes carrying `@DslMarker`-annotated annotations (grouped by marker)
- **Extension functions** — all receiver-typed functions
- **Lambda-with-receiver parameters** — DSL builder functions
- **Inline reified functions**

```bash
library-insight dsl-report
library-insight dsl-report --package io.ktor.client --db custom-index.json
```

### 18. API Usage Examples Generator (`examples`)

Auto-generates typical instantiation, builder usage, and method-call code examples from bytecode signatures, and extracts nested guide examples from README/Dokka markdown files:
```bash
library-insight examples HtmlBuilder
library-insight examples HtmlBuilder --db custom-index.json
```

### 19. Package Health & Complexity Report (`health`)

Generates public API statistics, deprecation ratios, topo package sizes, and structural complexity metrics:
```bash
library-insight health
library-insight health --db custom-index.json
```

### 20. Transitive Dependency Conflict Detector (`dependency-check`)

Scans transitive classpath bytecode call instructions to highlight potential runtime LinkageError and NoSuchFieldError conflicts:
```bash
library-insight dependency-check
library-insight dependency-check --dir <project-dir>
```

### 21. Method Call Graph Generator (`callgraph`)

Generates a recursive tree of internal library methods called by a specific method node:
```bash
library-insight callgraph Retrofit.Builder.build
library-insight callgraph Retrofit.Builder.build --db custom-index.json
```
