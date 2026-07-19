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
* Installing via npm: `npm install -g library-insight`
* Running instantly with npx: `npx library-insight <command> [options]`
* Installing via shell: `~/.library-insight/bin/library-insight` (linked globally as `library-insight`)

## Available scripts

- **`scripts/install-cli.sh`** — Installs the `library-insight` command globally on the host system if not already available.
  ```bash
  bash scripts/install-cli.sh
  ```

### 1. Scan Dependencies (`scan`)
Scans a local JAR/AAR file, a directory of JARs, or resolves a Maven coordinate over HTTP, downloading it and its corresponding `-sources.jar` automatically from repositories (Maven Central, Google Maven, SoftBank).

*(Note: In Gradle/Kotlin project directories, downloaded artifacts are saved locally to `build/library-insight/cache/`. To run fully offline, the scanner automatically references the local machine's Gradle caches (`~/.gradle/caches/modules-2/files-2.1/`) directly without copying, saving disk space).*

* **Scan Local JAR/AAR**:
  ```bash
  library-insight scan path/to/library.aar --sources path/to/library-sources.jar
  ```
* **Scan Maven Coordinate**:
  ```bash
  library-insight scan groupId:artifactId:version
  ```
* **Scan with Custom Repositories**:
  ```bash
  library-insight scan groupId:artifactId:version --repo https://jitpack.io
  ```

*Default Database Output*: `build/library-insight-index.json`.

### 2. Search Symbols (`search`)
Search for classes, packages, methods, or properties in the saved index.
```bash
library-insight search ClassNameOrMethod
```

### 3. Explain Class (`explain`)
Print detailed structural details (modifiers, superclasses, constructors, properties, and methods) with their documentation.
```bash
library-insight explain FullOrSimpleClassName
```

### 4. Export Index (`export`)
Export the index database to pretty JSON or structured Markdown reference sheets.
*(Note: For large libraries, single Markdown sheets are huge; use `ai-export` for AI context instead).*
```bash
# Automatically saves to build/API_REFERENCE.md
library-insight export markdown

# Automatically saves to build/library-insight-index.json
library-insight export json

# Or write to a custom path
library-insight export markdown custom-path.md

```

### 5. Diff Library Versions (`diff`)
Compare two library versions to check for changes and potential breaking changes.
```bash
library-insight diff old-library.jar new-library.jar
```

### 6. Export AI Context (`ai-export`)
**Recommended for AI Integration.** Instead of a single giant `API_REFERENCE.md` file, this splits the scanned database into a token-efficient directory structure under `build/ai-context/`. AI agents can inspect `metadata.json` first, and then load only the specific class JSON files they need, reducing token usage by 95%+:
- `metadata.json` (describing library name, version, and packages).
- Subfolders for packages mapped as `group-artifact-package` (dots replaced with hyphens) and subpackages nested as real subdirectories.
- Individual class-specific JSON files containing compact signatures and Javadocs.
```bash
library-insight ai-export
```

### 7. Clear Cache (`clear-cache`)
Clears all downloaded cached Maven binaries and sources from local storage.
```bash
library-insight clear-cache
```

### 8. Diagnostics & Doctor (`doctor`)
Run diagnostic checks for the Java runtime, Node.js, and verify all global AI Agent skill configurations.
```bash
library-insight doctor
```
