# Scanners & Indexing

## `scan` — Scan a Library

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

### Kotlin Multiplatform (KMP) Scanning

When you supply a root Kotlin Multiplatform library coordinate, the `scan` command automatically:

1. Inspects Gradle Module Metadata (`.module`) to resolve target split coordinates.
2. Resolves and downloads each platform's binaries (`.klib` or JVM `.jar`/`.aar`) and sources JAR.
3. Automatically parses `.klib` manifests and maps compilation targets (e.g. `ios`, `js`, `wasm`, `jvm`).
4. Merges all declarations, constructors, methods, and properties into a single unified index.

```bash
library-insight scan io.ktor:ktor-client-core:3.0.0
```

When explaining a class from a KMP index, platform target tags are annotated on class and member declarations:

```
==================================================
 CLASS EXPLAIN REPORT
==================================================
Class:       io.ktor.client.HttpClient
Package:     io.ktor.client
Kind:        class
Visibility:  public
Targets:     common, jvm
...
Constructors:
  - public constructor(engine: HttpClientEngine, userConfig: HttpClientConfig<out HttpClientEngineConfig>) [common]
  - public constructor(engine: io.ktor.client.engine.HttpClientEngine, ... ) [jvm]
```

---

## `scan-source` — Local Source Directory Scanner

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

---

## `clear-cache` — Clear Local Cache

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

## `update` — Self-Update CLI

Check for the latest release on GitHub and automatically download and update the Library Insight installation to the latest version.

```bash
library-insight update
```

**Example output:**

```
Checking for updates...
A new version is available: v1.4.0 (Current: v1.3.0)
Updating Library Insight...
==================================================
 Installing Library Insight v1.4.0...
==================================================
...
SUCCESS: Library Insight installed globally!
Library Insight updated successfully to v1.4.0!
```

---
