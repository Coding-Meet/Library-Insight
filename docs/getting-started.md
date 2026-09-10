# Getting Started

Get up and running with **Library Insight** to analyze Java, Kotlin, and Kotlin Multiplatform dependencies and enable correct API context for your development environment.

---

## Requirements

- **JDK 17 or higher** is required to execute the Java/Kotlin runtime engine.
- An active terminal environment (macOS/Linux/Windows).

---

## Installation Options

=== "Option A: One-Line Global Installer (Recommended)"

    You can install the CLI globally on your system instantly with zero Node.js/npm dependencies using the installer script:

    ```bash
    curl -fsSL https://raw.githubusercontent.com/Coding-Meet/Library-Insight/main/install.sh | bash
    ```

    Once installed, both `library-insight` and `li` commands are available globally and can be used interchangeably.

=== "Option B: Manual Build from Source"

    If you just want to run a local build without registering it globally on your system:

    1. Clone the repository and navigate to the project root:
       ```bash
       git clone https://github.com/Coding-Meet/Library-Insight.git
       cd Library-Insight
       ```
    2. Build using Gradle wrapper:
       ```bash
       ./gradlew installDist
       ```
    3. The executable binary will be generated at:
       ```
       ./library-insight-cli/build/install/library-insight/bin/library-insight
       ```

---

## Uninstallation

To cleanly remove the global CLI binary, installation files, and registered AI agent skills from your system:

```bash
curl -fsSL https://raw.githubusercontent.com/Coding-Meet/Library-Insight/main/uninstall.sh | bash
```

---

## Basic Workflow / Quick Start

**Library Insight** works in a simple 4-step pipeline: **Index** -> **Explore** -> **AI Context Export** -> **Version Audit**.

Here is how developers and AI coding agents use Library Insight step-by-step:

---

### Step 1: Index Your API Surface (`scan` / `scan-source`)

> **Why this step?** AI models hallucinate outdated methods because they don't know the exact library version installed in your project. Indexing creates a version-aware local API database from real bytecode or source files.

- **Scan a Compiled Dependency (e.g. Jetpack Compose Layout):**

  ```bash
  library-insight scan androidx.compose.foundation:foundation-layout:1.12.0
  ```

- **Scan a Bill of Materials (BOM):**

  ```bash
  library-insight scan androidx.compose:compose-bom:2024.09.00
  ```

- **Scan Local Source Code:**

  ```bash
  library-insight scan-source app/src/main
  ```

- **Scan a Kotlin Multiplatform (KMP) Library:**
  ```bash
  library-insight scan io.ktor:ktor-client-core:3.0.0
  ```

---

### Step 2: Explore & Inspect Signatures (`search` / `explain`)

> **Why this step?** Discover classes, top-level functions, parameter signatures, and receiver scopes directly from your terminal or AI prompt.

1. **Search for symbols in the index:**

   ```bash
   library-insight search Grid
   ```

2. **Inspect standard symbol signatures & KDocs:**

   ```bash
   library-insight explain Grid
   ```

3. **Inspect DSL receiver scopes in 1 turn (`--deep`):**
   ```bash
   library-insight explain Grid --deep
   ```
   _(Automatically resolves top-level `@Composable fun Grid` to `GridKt` and displays nested scopes like `GridScope` and `GridItemSpanScope`)._

---

### Step 3: Connect to AI Coding Assistants (`ai-export` / `mcp`)

> **Why this step?** Feed version-accurate API knowledge to Cursor, Claude, Gemini, or Copilot so they generate 100% correct code without hallucinating.

1. **Export Token-Optimized Markdown Files:**

   ```bash
   library-insight ai-export
   ```

   _(Generates compact documentation under `build/ai-context/` for workspace chats)._

2. **Start the Native MCP Server:**
   ```bash
   library-insight mcp
   ```
   _(Integrates natively with Cursor, Windsurf, or Claude Desktop)._

---

### Step 4: Compare Library Versions & Upgrades (`diff`)

> **Why this step?** Detect breaking changes, added/removed methods, or deprecations before upgrading a dependency in your project.

```bash
library-insight diff androidx.compose.foundation:foundation-layout:1.11.0 androidx.compose.foundation:foundation-layout:1.12.0
```
