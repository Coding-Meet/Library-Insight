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

## 1. `scan` — Scan a Library

Scan a JAR, AAR, local directory, or Maven coordinate. Use this first to build the local index.

> **Offline-First & Smart Caching:**
> - Checks your Gradle cache (`~/.gradle/caches/`) first before downloading.
> - Inside a Gradle project, downloaded artifacts land in `build/library-insight/cache/`.

```bash
library-insight scan com.squareup.retrofit2:retrofit:2.11.0
library-insight scan com.squareup.okhttp3:okhttp:4.12.0
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

**Example output:**
```
Found 2 matching classes:
  - retrofit2.Retrofit
  - retrofit2.Retrofit$Builder
```

---

## 3. `explain` — Explain a Class

Print detailed structural information (modifiers, superclass, constructors, properties, methods, and Javadoc/KDoc) for a specific class.

```bash
library-insight explain Retrofit
```

**Example output:**
```
Class: retrofit2.Retrofit (public class)
  Constructors:
    + public constructor(okhttp3.Call$Factory, ...)
  Methods:
    + public fun <T> create(java.lang.Class<T>): T
    + public fun baseUrl(): okhttp3.HttpUrl
    + public fun callFactory(): okhttp3.Call$Factory
```

---

## 4. `diff` — Compare Versions

Compare two library archives to check for added, removed, and changed APIs including breaking changes.

```bash
library-insight diff retrofit-2.9.0.jar retrofit-2.11.0.jar
# Or via Maven coordinates:
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

```bash
library-insight export markdown
library-insight export json
```

---

## 7. `ai-export` — AI Context Export (Recommended for AI prompts)

Splits the scanned database into a token-efficient directory structure under `build/ai-context/`. AI agents read `metadata.json` first, then load only the class files they need — reducing token usage by 95%+.

```bash
library-insight ai-export
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

```bash
library-insight search-central retrofit
library-insight search-central clikt
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

Start the Model Context Protocol server on stdio. Connect Cursor, Claude Desktop, or any MCP-compatible IDE to use `scan_library`, `search_symbols`, and `explain_class` tools natively.

```bash
library-insight mcp
```

> **MCP vs CLI:** If an MCP server is already configured in your IDE, prefer it over running CLI commands directly.

---

## 13. `init` — Initialize Workspace Skill

Write a `SKILL.md` into `.agents/skills/library-insight/` so local AI agents auto-discover the CLI.

```bash
library-insight init
```

---

## 14. `skills` — Manage Agent Skills

```bash
library-insight skills add    # Add skill to current workspace
library-insight skills list   # List registered skills
```

---

## 15. `clear-cache` — Clear Local Cache

Delete all locally downloaded Maven artifacts.

```bash
library-insight clear-cache
```

---

## 16. `doctor` — Diagnostics

Check Java version, cache directory, and active AI agent skill configurations.

```bash
library-insight doctor
```

**Example output:**
```
[Library Insight Diagnostics]
1. Java Runtime Environment (JRE):
   - Version: 17.0.7
   - Status: OK (Java 17+ verified)
2. Local Cache Directory:
   - Status: OK
3. AI Agent Skill Registrations:
   - Gemini Config Skill: ACTIVE
```
