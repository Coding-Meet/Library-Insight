# Analysis & Diagnostics

## `callgraph` — Method Call Graph Generator

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

## `health` — Package Health & Complexity Report

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

## `dependency-check` — Transitive ABI Dependency Conflict Detector

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

## `audit` — Dependency API Audit

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

## `doctor` — Diagnostics

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
