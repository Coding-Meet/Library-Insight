# Library Insight 🔍

Library Insight is a command-line tool that analyzes Java and Kotlin libraries (JAR/AAR) to inspect, extract, and index their complete public API surface from compiled bytecode and Kotlin metadata.

Instead of requiring source code, the tool reads compiled `.class` structures (using ASM) and `@Metadata` annotations (using `kotlin-metadata-jvm`) to construct an accurate public API index. 

This is incredibly useful for:
- Documentation generation
- API compatibility verification (checking for breaking changes)
- IDE extensions and indexing engines
- Context generators supplying AI assistants with exact signatures

---

## Key Features

- **Multi-Format Support**: Reads JARs, AARs (including nested JARs), directories, and Gradle build outputs.
- **Deep Metadata Extraction**:
  - **Classes/Interfaces/Objects**: Modifiers, companion objects, data/value flags, annotation markers, nested declarations, interfaces, inheritance.
  - **Constructors & Methods**: Visibility, parameter names, default arguments, return types, generic signatures/bounds, extension receivers, operators, infixes, inline, and suspend keywords.
  - **Properties**: Mutability (`val`/`var`), const declarations, lateinits, backing fields, and custom accessors.
- **Format Exporters**: Converts API indices to structured **JSON** or highly readable **Markdown** reference documentation.
- **Search Engine**: Performs case-insensitive searches across packages, classes, methods, and properties.
- **Diff Engine**: Compares two versions of a library to highlight added, removed, or changed APIs, deprecations, and **binary breaking changes** (like visibility reduction, method deletion, or changing suspend modifiers).
- **AI-Context Exporter**: Generates a compact, token-efficient `ai-context.json` file optimized for LLMs (ChatGPT, Gemini, Claude, Cursor, Copilot).

---

## Architecture & Modular Design

Library Insight follows **Clean Architecture** principles and is composed of the following modules:

- `library-insight-common`: Utility classes for ZIP/JAR/AAR extraction and filesystem operations.
- `library-insight-model`: Immutable Kotlin serialization structures representing the API index schema.
- `library-insight-parser`: Raw bytecode structure extraction using **ASM** and JVM signature parsing.
- `library-insight-kotlin`: Kotlin metadata parsing (`kotlin-metadata-jvm`) and JVM bytecode enrichment.
- `library-insight-search`: Index search and query matching logic.
- `library-insight-export`: JSON, Markdown, and AI context formatters.
- `library-insight-core`: Orchestrates scan flows and implements the semantic API diffing engine.
- `library-insight-cli`: Command Line Interface definitions using **Clikt**.

---

## Build & Installation

### Requirements
- JDK 17 or higher
- Gradle (provided wrapper)

### Build Executable
Compile the project and install the local executable command distribution:
```bash
./gradlew installDist
```
The executable binary will be generated at:
`./library-insight-cli/build/install/library-insight/bin/library-insight`

---

## CLI Usage & Commands

### 1. Scan Library
Scan a JAR, AAR, or directory and save the API index to a local database (`.library-insight-index.json` by default).
```bash
# Scan a JAR
./library-insight scan my-library.jar

# Scan an Android AAR with specific version
./library-insight scan my-android-lib.aar --lib-version 1.2.0

# Scan a directory of libraries
./library-insight scan app/libs/ --db my-custom-db.json

# Scan Maven coordinate directly (downloads binary + sources automatically from Maven Central/Google Maven/SoftBank)
./library-insight scan com.aldebaran:qisdk:1.7.5

# Scan Maven coordinate with custom repository URL
./library-insight scan com.mycompany:mylib:1.0.0 --repo https://jitpack.io
```

### 2. Search Symbols
Search for packages, classes, methods, or properties in the saved index.
```bash
./library-insight search AnimationBuilder
./library-insight search repeatCount
```

### 3. Explain Class
Print detailed structural details (modifiers, superclasses, constructors, properties, and methods) about a specific class.
```bash
./library-insight explain Calculator
```

### 4. Export Index
Export the scanned index to Markdown reference sheets or pretty-printed JSON.
```bash
# Print Markdown reference sheet to stdout
./library-insight export markdown

# Write JSON index to file
./library-insight export json output.json
```

### 5. Diff Library Versions
Compare two library archives directly to check for changes and potential breaking changes.
```bash
./library-insight diff old-library.aar new-library.aar
```

### 6. Export AI Context
Generate a compact, token-efficient split context folder structure (`ai-context/`) for LLMs.
```bash
./library-insight ai-export
```

### 7. Clear Cache
Clear all downloaded and cached artifacts from the local cache directory to free up space.
```bash
./library-insight clear-cache
```


```
Library-Insight/
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── library-insight-cli/
│   ├── src/
│   │   └── main/
│   │       └── kotlin/
│   │           └── com/
│   │               └── meet/
│   │                   └── libraryinsight/
│   │                       └── cli/
│   │                           ├── DatabaseHelper.kt
│   │                           └── Main.kt
│   └── build.gradle.kts
├── library-insight-common/
│   ├── src/
│   │   └── main/
│   │       └── kotlin/
│   │           └── com/
│   │               └── meet/
│   │                   └── libraryinsight/
│   │                       └── common/
│   │                           └── ArchiveUtils.kt
│   └── build.gradle.kts
├── library-insight-core/
│   ├── src/
│   │   ├── main/
│   │   │   └── kotlin/
│   │   │       └── com/
│   │   │           └── meet/
│   │   │               └── libraryinsight/
│   │   │                   └── core/
│   │   │                       ├── diff/
│   │   │                       └── LibraryAnalyzer.kt
│   │   └── test/
│   │       └── kotlin/
│   │           └── com/
│   │               └── meet/
│   │                   └── libraryinsight/
│   │                       └── core/
│   │                           └── diff/
│   └── build.gradle.kts
├── library-insight-export/
│   ├── src/
│   │   └── main/
│   │       └── kotlin/
│   │           └── com/
│   │               └── meet/
│   │                   └── libraryinsight/
│   │                       └── export/
│   │                           ├── AiExporter.kt
│   │                           ├── JsonExporter.kt
│   │                           └── MarkdownExporter.kt
│   └── build.gradle.kts
├── library-insight-kotlin/
│   ├── src/
│   │   └── main/
│   │       └── kotlin/
│   │           └── com/
│   │               └── meet/
│   │                   └── libraryinsight/
│   │                       └── kotlin/
│   │                           ├── KotlinMetadataEnricher.kt
│   │                           └── KotlinMetadataParser.kt
│   └── build.gradle.kts
├── library-insight-model/
│   ├── src/
│   │   └── main/
│   │       └── kotlin/
│   │           └── com/
│   │               └── meet/
│   │                   └── libraryinsight/
│   │                       └── model/
│   │                           └── LibraryApiIndex.kt
│   └── build.gradle.kts
├── library-insight-parser/
│   ├── src/
│   │   ├── main/
│   │   │   └── kotlin/
│   │   │       └── com/
│   │   │           └── meet/
│   │   │               └── libraryinsight/
│   │   │                   └── parser/
│   │   │                       ├── BytecodeParser.kt
│   │   │                       ├── RawClassData.kt
│   │   │                       └── SignatureParser.kt
│   │   └── test/
│   │       └── kotlin/
│   │           └── com/
│   │               └── meet/
│   │                   └── libraryinsight/
│   │                       └── parser/
│   │                           └── SignatureParserTest.kt
│   └── build.gradle.kts
├── library-insight-search/
│   ├── src/
│   │   └── main/
│   │       └── kotlin/
│   │           └── com/
│   │               └── meet/
│   │                   └── libraryinsight/
│   │                       └── search/
│   │                           └── SearchEngine.kt
│   └── build.gradle.kts
├── sample/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/
│   │       │       └── meet/
│   │       │           └── sample/
│   │       │               └── JavaLibrary.java
│   │       └── kotlin/
│   │           └── com/
│   │               └── meet/
│   │                   └── sample/
│   │                       └── SampleLibrary.kt
│   └── build.gradle.kts
├── ai-context.json
├── build.gradle.kts
├── gradlew
├── gradlew.bat
├── local.properties
├── metadata-jvm.md
├── README.md
└── settings.gradle.kts
```

---

## License

Copyright 2026 Library Insight Authors. Licensed under the Apache License, Version 2.0.
