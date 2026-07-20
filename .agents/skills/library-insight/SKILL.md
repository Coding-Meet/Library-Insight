---
name: library-insight
description: Indexes public APIs and extracts source code comments from Java/Kotlin compiled archives (JAR/AAR) or Maven coordinates, generating compact context JSON layouts for AI agents.
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
  ```

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
```

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
```

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
```

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
```

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
Run diagnostic checks for the Java runtime, Node.js, and verify all global AI Agent skill configurations.
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
2. Node.js Environment:
   - Version: v18.16.0
   - Status: OK
3. Local Cache Directory:
   - Path: /Users/meet/AndroidStudioProjects/Library-Insight/build/library-insight/cache
   - Status: OK
4. AI Agent Skill Registrations:
   - Gemini Config Skill: ACTIVE (registered)
   - Cursor Skill: ACTIVE (registered)
```
