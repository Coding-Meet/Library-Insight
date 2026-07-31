# Library Insight — Complete Command Reference

> This file contains the full CLI command reference.
> For installation and quick start, see the [Getting Started](getting-started.md) guide.

---

## Running commands

Once installed globally:

```bash
library-insight <command> [options]
```

> [!IMPORTANT]
> **AI Agent Accuracy Rule:**
> If you are an AI assistant, do not assume the library API from memory, web docs, or random examples. Scan or query the exact library version used by the project first.
>
> **Token Efficiency Rule:**
> Do NOT read raw `build/library-insight-index.json` or generated `API_REFERENCE.md` files. Use the dedicated query subcommands to look up only what you need.

---

<!-- --8<-- [start:commands] -->

## 1. `scan` — Scan a Library

Scan a JAR, AAR, local directory, or Maven coordinate. Use this first to build the local index.

> **Offline-First & Smart Caching:**
>
> - Checks your Gradle cache (`~/.gradle/caches/`) first before downloading.
> - Inside a Gradle project, downloaded artifacts land in `build/library-insight/cache/`.

```bash
library-insight scan com.squareup.retrofit2:retrofit:2.11.0
```

**Optional Parameters:**

- `--db <file>`: Path to save the JSON index database (default: `build/library-insight-index.json`)
- `-s, --sources <file>`: Path to sources JAR/AAR or source code folder to extract Javadoc/KDoc comments & guide examples
- `--repo <url>`: Additional Maven repository URL to download coordinates (multiple allowed)
- `--lib-name <name>`: Override the library name in the generated index
- `--lib-version <version>`: Override the version tag in the generated index

**Example with options:**

```bash
library-insight scan com.squareup.okhttp3:okhttp:4.12.0 --sources okhttp-sources.jar --repo https://maven.google.com
```

**Example output:**

```
Detected Maven coordinate: com.squareup.retrofit2:retrofit:2.11.0
  -> Using cached binary JAR from Gradle cache: retrofit-2.11.0.jar
  -> Using cached sources JAR from Gradle cache: retrofit-2.11.0-sources.jar
Scan complete! Found 113 classes across 3 packages.
Saved API index to: build/library-insight-index.json
```

---

## 2. `search` — Search Symbols

Search for packages, classes, methods, or properties in the saved index.

```bash
library-insight search Retrofit
```

**Optional Parameters:**

- `--db <file>`: Index database JSON file path to read from (default: `build/library-insight-index.json`)

**Example with options:**

```bash
library-insight search "anno:Keep" --db custom-index.json
```

**Example output:**

```
Found 2 matches for 'Retrofit':
--------------------------------------------------
[CLASS]     class retrofit2.Retrofit
           Source: src/main/java/retrofit2/Retrofit.java:18
[CLASS]     class retrofit2.Retrofit$Builder
           Source: src/main/java/retrofit2/Retrofit.java:82
--------------------------------------------------
```

---

## 3. `explain` — Explain a Class

Print detailed structural information (modifiers, superclass, constructors, properties, methods, Javadoc/KDoc, and nested usage guide examples extracted from README/Dokka markdown files) for a specific class.

```bash
library-insight explain HtmlBuilder
```

**Optional Parameters:**

- `--db <file>`: Index database JSON file path to read from (default: `build/library-insight-index.json`)

**Example with options:**

```bash
library-insight explain HtmlBuilder --db custom-index.json
```

**Example output:**

```
==================================================
 CLASS EXPLAIN REPORT
==================================================
Class:       com.meet.sample.HtmlBuilder
Package:     com.meet.sample
Kind:        class
Visibility:  public
Source:      sample/src/main/kotlin/com/meet/sample/SampleLibrary.kt:25
Annotations: @HtmlDsl
==================================================

Imports:
  - kotlin.text.*
  - retrofit2.Retrofit

Properties:
  - private val children: Any (sample/src/main/kotlin/com/meet/sample/SampleLibrary.kt:27)

Methods:
  // /** Adds a paragraph element to the HTML output. */
  - public fun p(text: String): Unit (sample/src/main/kotlin/com/meet/sample/SampleLibrary.kt:29)
  // /** Adds a heading element. */
  - public fun h1(text: String): Unit (sample/src/main/kotlin/com/meet/sample/SampleLibrary.kt:32)
  // /** Adds a nested div block. */
  - public fun div(block: HtmlBuilder.() -> Unit): Unit (sample/src/main/kotlin/com/meet/sample/SampleLibrary.kt:35)
  - public fun build(): String (sample/src/main/kotlin/com/meet/sample/SampleLibrary.kt:41)
```

---

## 4. `diff` — Compare Versions

Compare two library archives to check for added, removed, and changed APIs including breaking changes.

```bash
library-insight diff retrofit-2.9.0.jar retrofit-2.11.0.jar
```

**Or via Maven coordinates:**

```bash
library-insight diff com.squareup.retrofit2:retrofit:2.9.0 com.squareup.retrofit2:retrofit:2.11.0
```

**Example output:**

```
==================================================
 LIBRARY INSIGHT API DIFF REPORT
==================================================
Breaking Changes Found: NO
➕ Added Classes:
  - retrofit2.Reflection
📝 Changed Classes:
  Class: retrofit2.Invocation
    Added Methods:
      + fun service(): java.lang.Class<?>
```

---

## 5. `migrate` — Migration Advisor

Compare two versions and get a structured migration report showing removed, deprecated, and replacement APIs.

```bash
library-insight migrate com.squareup.retrofit2:retrofit:2.9.0 com.squareup.retrofit2:retrofit:2.11.0
```

**Optional Parameters:**

- `--repo <url>`: Additional Maven repository URLs to resolve coordinates (multiple allowed)

**Example with options:**

```bash
library-insight migrate com.squareup.retrofit2:retrofit:2.9.0 com.squareup.retrofit2:retrofit:2.11.0 --repo https://repo.maven.apache.org/maven2
```

**Example output:**

```
==================================================
        Library Insight Migration Report
==================================================
Old Version : 2.9.0
New Version : 2.11.0

❌ Removed Classes
  - retrofit2.Platform$Android
❌ Removed Methods
  - fun retrofit2.Platform.defaultCallbackExecutor(): Executor

Binary Compatibility: ❌ BREAKING CHANGES DETECTED
```

---

## 6. `export` — Export Index

Export the scanned index to Markdown or JSON.

> For large libraries, Markdown files can be huge. Use `ai-export` for AI prompts instead.

**Export to Markdown format:**

```bash
library-insight export markdown
```

**Export to JSON format:**

```bash
library-insight export json
```

**Positional Arguments:**

- `[output-file]`: Optional target output file path to write export content to. If not specified, defaults to `build/API_REFERENCE.md` or `build/library-insight-index.json`. Use `-` to print to stdout.

**Optional Parameters:**

- `--db <file>`: Index database JSON file path to read from (default: `build/library-insight-index.json`)

**Example with options:**

```bash
library-insight export markdown API_REFERENCE.md --db custom-index.json
```

**Example output:**

```
Exported MARKDOWN to: build/API_REFERENCE.md
```

---

## 7. `ai-export` — AI Context Export (Recommended for AI prompts)

Splits the scanned database into a token-efficient directory structure under `build/ai-context/`. AI agents read `metadata.json` first, then load only the class files they need — reducing token usage by 95%+.

```bash
library-insight ai-export
```

**Positional Arguments:**

- `[output-dir]`: Optional target output directory to save AI context files (default: `build/ai-context/`)

**Optional Parameters:**

- `--db <file>`: Index database JSON file path to read from (default: `build/library-insight-index.json`)

**Example with options:**

```bash
library-insight ai-export custom-ai-context/ --db custom-index.json
```

**Example output:**

```
Generated compact LLM context directory structure at: build/ai-context
```

---

## 8. `audit` — Dependency API Audit

Scan all project Gradle dependencies recursively (`build.gradle.kts`, `libs.versions.toml`) and report deprecated classes, methods, and properties found in the bytecode.

```bash
library-insight audit
```

**Example output:**

```
==================================================
      Library Insight Dependency Audit
==================================================
Found 10 dependencies to audit.
Auditing org.ow2.asm:asm:9.7...
  - Status: ⚠️  Deprecations detected
    * Deprecated Methods    : 2
    * Deprecated Properties : 2
Audit Summary: Scanned 10 libraries. Total Deprecated APIs: 1819
```

---

## 9. `search-central` — Search Maven Central

Search Maven Central dynamically for matching coordinates and versions.

**Search for Retrofit on Maven Central:**

```bash
library-insight search-central retrofit
```

**Or search for another library like Clikt:**

```bash
library-insight search-central clikt
```

**Example output:**

```
Searching Maven Central for 'clikt'...

Found 10 matching libraries on Maven Central:

📦 Coordinate: com.github.ajalt.clikt:clikt:5.0.3
   Repository: central
   Group:      com.github.ajalt.clikt
   Artifact:   clikt
--------------------------------------------------
📦 Coordinate: com.github.ajalt:clikt:2.8.0
   Repository: central
   Group:      com.github.ajalt
   Artifact:   clikt
```

---

## 10. `dependency-graph` — Dependency Tree

Print a visual recursive tree of transitive dependencies from POM descriptors.

```bash
library-insight dependency-graph com.github.ajalt.clikt:clikt-jvm:4.4.0
```

**Example output:**

```
com.github.ajalt.clikt:clikt-jvm:4.4.0
│   ├── com.github.ajalt.mordant:mordant-jvm:2.5.0
│   │   ├── com.github.ajalt.colormath:colormath-jvm:3.5.0
│   │   │   ├── org.jetbrains.kotlin:kotlin-stdlib:1.9.21
```

---

## 11. `semver` — SemVer Compliance Check

Verify that the version number bump between two releases correctly reflects the bytecode changes (breaking change requires major bump, added APIs require minor bump).

```bash
library-insight semver com.squareup.retrofit2:retrofit:2.9.0 com.squareup.retrofit2:retrofit:2.11.0
```

**Example output:**

```
🚨 SemVer Violation: Version bump does not match API changes!
  ❌ API Breaking Change detected but MAJOR version was not incremented!
```

---

## 12. `mcp` — MCP Server

Start the Model Context Protocol server on stdio. Connect Cursor, Claude Desktop, or any MCP-compatible IDE to use `scan_library`, `search_symbols`, `explain_class`, and `dsl_report` tools natively.

```bash
library-insight mcp
```

**Optional Parameters:**

- `--db <file>`: Index database JSON file path to read from and write to (default: `build/library-insight-index.json`)

**Example with options:**

```bash
library-insight mcp --db /path/to/project/custom-index.json
```

**Example output (JSON-RPC tools list response):**

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "tools": [
      {
        "name": "scan_library",
        "description": "Scans a Java/Kotlin library and creates an API index.",
        "inputSchema": {
          "type": "object",
          "properties": { "pathOrCoordinate": { "type": "string" } }
        }
      },
      {
        "name": "search_symbols",
        "description": "Search for symbols in the active library index.",
        "inputSchema": {
          "type": "object",
          "properties": { "query": { "type": "string" } }
        }
      }
    ]
  }
}
```

> **MCP vs CLI:** If an MCP server is already configured in your IDE, prefer it over running CLI commands directly.

---

## 13. `init` — Initialize Workspace Skill

Write a `SKILL.md` into `.agents/skills/library-insight/` so local AI agents auto-discover the CLI.

```bash
library-insight init
```

**Example output:**

```
Initializing Library Insight agent environment...
Creating directory: .agents/skills/library-insight/
Successfully initialized workspace skill instructions!
```

---

## 14. `skills` — Manage Agent Skills

**Add skill to the current workspace:**

```bash
library-insight skills add
```

**List registered skills in the current workspace:**

```bash
library-insight skills list
```

**Example output (skills list):**

```
Workspace AI Agent Skills:
  - [Installed] library-insight
```

---

## 15. `clear-cache` — Clear Local Cache

Delete all locally downloaded Maven artifacts.

```bash
library-insight clear-cache
```

**Example output:**

```
Clearing local cache at: ~/.library-insight/cache...
Cache cleared successfully. Deleted 12.4 MB.
```

---

## 16. `doctor` — Diagnostics

Check Java version, cache directory, and active AI agent skill configurations.

```bash
library-insight doctor
```

**Example output:**

```
==================================================
      Library Insight Diagnostics & Doctor
==================================================

1. Java Runtime Environment (JRE):
   - Version: 17.0.17
   - Vendor: Microsoft
   - Status: OK (Java 17+ verified)

2. Local Download Cache:
   - Path: ~/.library-insight/cache
   - Status: OK

3. Global AI Agent Skill Configurations:
   - Cursor               : INSTALLED (Verified)
   - Gemini Config        : INSTALLED (Verified)
   - Claude Desktop       : INSTALLED (Verified)
   - Antigravity Agents   : INSTALLED (Verified)
   - GitHub Copilot       : INSTALLED (Verified)

==================================================
Diagnostics completed.
```

---

## 17. `dsl-report` — Kotlin DSL Surface Report

Generate a dedicated Kotlin DSL surface report for DSL-heavy libraries. Shows:

- **Type aliases** extracted from package metadata
- **DSL scopes** — classes annotated with `@DslMarker` markers, grouped by marker
- **Extension functions** — all `ReceiverType.function()` signatures
- **Lambda-with-receiver parameters** — DSL builder functions (`block: Builder.() -> Unit`)
- **Inline reified functions** — functions with `reified` type parameters

```bash
library-insight dsl-report
```

**Optional Parameters:**

- `--db <file>`: Index database JSON file path to read from (default: `build/library-insight-index.json`)
- `-p, --package <pkg>`: Filter results to a specific package name prefix

**Example with options:**

```bash
library-insight dsl-report --package io.ktor.client --db custom-index.json
```

**Example output:**

```
==================================================
  DSL SURFACE REPORT  —  ktor-client-core-jvm 2.3.12
==================================================

▶ Type Aliases (3)
  typealias HttpClientConfig<T> = T.() -> Unit
  typealias ResponseValidator = suspend (response: HttpResponse) -> Unit
  typealias HeadersBuilder = StringValuesBuilder

▶ DSL Scopes — @DslMarker annotated classes (5)
  @KtorDsl → HttpClientConfig, HttpRequestBuilder, HeadersBuilder

▶ Extension Functions (24)
  fun HttpClient.get(urlString: String, block: (String) -> Unit): HttpResponse
  fun HttpRequestBuilder.contentType(contentType: ContentType): Unit
  ...

▶ Lambda-with-Receiver Parameters — DSL builder functions (12)
  fun httpClient([config: HttpClientConfig<*>.() -> Unit]): HttpClient
  fun HttpRequestBuilder.headers([block: HeadersBuilder.() -> Unit]): Unit
  ...

▶ Inline Reified Functions (3)
  inline fun <reified T> HttpClient.get(url: String): T
  inline fun <reified T> HttpResponse.body(): T
  ...

==================================================
  Tip: run 'explain <ClassName>' for full API details on any class above.
==================================================
```

> **Note for DSL library authors:** If your library uses `@DslMarker` and the annotation is bundled in the same JAR, Library Insight will group all DSL builder scopes by their marker annotation — making it easy for AI agents and developers to understand which builders can safely nest.

---

## 18. `examples` — API Usage Examples Generator

Generate idiomatic Kotlin code examples showing typical usage patterns for a specific class. Automatically scans bytecode signatures to determine target design patterns (Constructor, Builder, Factory, Singleton) and extracts nested guide examples from README/Dokka markdown files.

```bash
library-insight examples HtmlBuilder
```

**Optional Parameters:**

- `--db <file>`: Index database JSON file path to read from (default: `build/library-insight-index.json`)

**Example with options:**

```bash
library-insight examples HtmlBuilder --db custom-index.json
```

**Example output:**

```
==================================================
  API USAGE EXAMPLES GENERATOR  —  HtmlBuilder
==================================================
// Target API: com.meet.sample.HtmlBuilder

Detected Usage Patterns:
  ✓ Constructor
  ✗ Builder
  ✗ Factory
  ✗ Singleton
==================================================

// Pattern: Guide Examples (from README/Dokka)
val result = html {
    head {
        title("My Page")
    }
    div {
        p("Welcome to Library Insight!")
    }
}

// Pattern: Constructor Instantiation
val htmlbuilder = com.meet.sample.HtmlBuilder()

// API Invocation Examples
  htmlbuilder.p(text = "example") // returns: kotlin.Unit
  htmlbuilder.h1(text = "example") // returns: kotlin.Unit
  htmlbuilder.div(block = { }) // returns: kotlin.Unit
  htmlbuilder.build() // returns: kotlin.String
==================================================
```

---

## 19. `health` — Package Health & Complexity Report

Generate a detailed report showing public API statistics, deprecation ratios, topo package sizes, and structural complexity metrics (largest classes, deepest inheritance hierarchies, generic density).

```bash
library-insight health
```

**Optional Parameters:**

- `--db <file>`: Index database JSON file path to read from (default: `build/library-insight-index.json`)

**Example with options:**

```bash
library-insight health --db custom-index.json
```

**Example output:**

```
==================================================
    PACKAGE HEALTH & COMPLEXITY REPORT
==================================================
Library Target : sample-1.1.0 (1.0.0)
API Health Grade: A (Deprecation ratio: 1.11%)
==================================================

▶ API Distribution
  Total Public APIs   : 90
  ├─ Classes/Objects  : 16
  ├─ Constructors     : 13
  ├─ Methods          : 37
  ├─ Properties       : 21
  └─ Type Aliases     : 3
  Deprecated APIs     : 1
  Experimental APIs   : 0

▶ Package Topology
  Largest Package     : com.meet.sample (16 classes)
  Most Deprecated Pkg : com.meet.sample (1 deprecated methods)

▶ API Complexity Metrics
  Largest Class       : com.meet.sample.SampleLibraryKt (9 methods)
  Longest Signature   : com.meet.sample.User.copy (3 parameters)
  Deepest Inheritance : com.meet.sample.AppConfig (1 supertypes: kotlin.Any)
  Most Generic Class  : com.meet.sample.SampleLibraryKt$retry$1 (1 parameters: <T>)
==================================================
```

---

## 20. `dependency-check` — Transitive ABI Dependency Conflict Detector

Scan all Gradle build dependencies and verify classpath bytecode references against resolved dependency JARs. Flags potential runtime `LinkageError` and `NoSuchFieldError` issues before deployment.

```bash
library-insight dependency-check
```

**Optional Parameters:**

- `--dir <project-dir>`: Target project directory to scan (default: current directory)

**Example with options:**

```bash
library-insight dependency-check --dir /path/to/my-android-project
```

**Example output:**

```
==================================================
    DEPENDENCY CONFLICT & ABI DETECTOR
==================================================
Analyzing 5 dependencies on classpath...
Defined classes in classpath: 39
Defined methods: 582
Analyzing references for ABI linkage conflicts...

🚨 Potential ABI Method Conflicts (LinkageError risk):
  [Method Missing] class org.objectweb.asm.CurrentFrame (from asm-9.7.jar)
   └── Calls missing method: org.objectweb.asm.CurrentFrame.merge(...)

==================================================
Analysis Complete: ❌ 12 potential linkage conflicts detected.
==================================================
```

---

## 21. `callgraph` — Method Call Graph Generator

Generate a recursive tree representation showing all internal library methods called by a specific method node. Uses ASM instructions analysis to map actual execution paths.

```bash
library-insight callgraph AppConfigBuilder.database
```

**Optional Parameters:**

- `--db <file>`: Index database JSON file path to read from (default: `build/library-insight-index.json`)

**Example with options:**

```bash
library-insight callgraph AppConfigBuilder.database --db custom-index.json
```

**Example output:**

```
==================================================
  METHOD INVOCATION CALL GRAPH  —  database
==================================================

▶ Starting entrypoint: com.meet.sample.AppConfigBuilder.database(Lkotlin/jvm/functions/Function1;)V
└── com.meet.sample.DatabaseConfigBuilder.<init>()
==================================================
```

---

## 22. `scan-source` — Local Source Directory Scanner

Scan a local raw source directory containing Kotlin (`.kt`) and Java (`.java`) files to build an API index database.

```bash
library-insight scan-source sample/src/main/kotlin
```

**Optional Parameters:**

- `--db <file>`: Target index database JSON file path to write to (default: `build/library-insight-index.json`)
- `--lib-name <name>`: Override the library name tag in the generated index
- `--lib-version <version>`: Override the version tag in the generated index

**Example with options:**

```bash
library-insight scan-source src/main/kotlin --db build/my-app-index.json --lib-name MyApp --lib-version 1.0.0
```

**Example output:**

```
Scanning source directory: /Users/meet/AndroidStudioProjects/Library-Insight/sample/src/main/kotlin

Detected:
  • Kotlin files : 1
  • Java files   : 0

Scan complete!
Found 15 classes across 3 packages.

Saved API index to:
/Users/meet/AndroidStudioProjects/Library-Insight/build/library-insight-index.json
```

<!-- --8<-- [end:commands] -->
