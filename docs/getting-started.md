# Getting Started

Get up and running with **Library Insight** to analyze JVM dependencies and enable correct API context for your development environment.

---

## Requirements

*   **JDK 17 or higher** is required to execute the Java/Kotlin runtime engine.
*   An active terminal environment (macOS/Linux/Windows).

---

## Installation Options

=== "Option A: One-Line Global Installer (Recommended)"

    You can install the CLI globally on your system instantly with zero Node.js/npm dependencies using the installer script:

    ```bash
    curl -fsSL https://raw.githubusercontent.com/Coding-Meet/Library-Insight/main/install.sh | bash
    ```

    Once installed, you can execute the `library-insight` command directly from any folder.

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

Here is a typical flow when you add or inspect a library:

### 1. Scan a Library
Scan a dependency from Maven Central (or picks it from your local Gradle cache).
```bash
library-insight scan com.squareup.retrofit2:retrofit:2.11.0
```

### 2. Search for Symbols
Find a class, interface, or property in the compiled library index.
```bash
library-insight search Retrofit
```

### 3. Explain class signatures
Inspect full API signatures and Kotlin/Java docs for a specific class.
```bash
library-insight explain Retrofit
```

### 4. Compare Versions
See what was added, removed, or changed between two versions.
```bash
library-insight diff com.squareup.retrofit2:retrofit:2.9.0 com.squareup.retrofit2:retrofit:2.11.0
```
