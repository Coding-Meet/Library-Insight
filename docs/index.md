--8<-- "README.md:intro"

---

## 📺 Video Walkthrough

Watch the full product showcase explaining Library Insight's core features, architecture, KMP integration, and AI MCP server setup:

<div class="video-container" style="position: relative; padding-bottom: 56.25%; height: 0; overflow: hidden; max-width: 100%; border-radius: 8px; box-shadow: 0 4px 20px rgba(0,0,0,0.25);">
  <iframe src="https://www.youtube.com/embed/jmvBqjGE_gg" title="Library Insight Walkthrough" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowfullscreen style="position: absolute; top: 0; left: 0; width: 100%; height: 100%;"></iframe>
</div>

---

## Key Features

--8<-- "README.md:features"

---

## Why Library Insight?

When you add a dependency, the first question is simple:

> "How do I use this version correctly?"

In real projects, that answer is often messy:

- **Outdated AI Context**: AI may write code for the latest release while your project uses an older version.
- **Incorrect Web Examples**: AI may copy an old blog post where the method name no longer exists.
- **Incomplete Docs**: Official docs may be incomplete or not updated for the release you installed.
- **Hidden Replacements**: Deprecated methods may still appear in examples, while the replacement is hidden in release notes or source comments.
- **Context Waste**: Huge generated docs waste AI context and make one class hard to find.

Library Insight turns the **library artifacts and source code you actually use** into the source of truth. Scan a JAR, AAR, KLib, Maven dependency, or local Java/Kotlin source directory, then use `search`, `explain`, `diff`, `migrate`, or `ai-export` to give humans and AI agents exact, version-aware API information.

---

## Architecture & Modular Design

Library Insight follows Clean Architecture principles. Below is the modular dependency flow:

```mermaid
graph TD
    subgraph CLI Layer
        CLI[library-insight-cli]
    end

    subgraph Orchestration Layer
        CORE[library-insight-core]
    end

    subgraph Processing Modules
        PARSER[library-insight-parser]
        KOTLIN[library-insight-kotlin]
        SEARCH[library-insight-search]
        EXPORT[library-insight-export]
    end

    subgraph Data & Common Utility Base
        MODEL[library-insight-model]
        COMMON[library-insight-common]
    end

    CLI --> CORE
    CORE --> PARSER
    CORE --> KOTLIN
    CORE --> SEARCH
    CORE --> EXPORT

    PARSER --> MODEL
    KOTLIN --> MODEL
    SEARCH --> MODEL
    EXPORT --> MODEL

    MODEL --> COMMON
    COMMON --> ASM[ASM Bytecode Reader]
    COMMON --> KTOR[Ktor HTTP Client]
```

---

## Core Modules

- `library-insight-common`: Utility classes for ZIP/JAR/AAR extraction, Ktor async HTTP engine, and filesystem operations.
- `library-insight-model`: Immutable Kotlin serialization structures representing the API index schema.
- `library-insight-parser`: Raw bytecode structure extraction using **ASM** and local source code parsing using **JavaParser** and **Kotlin PSI**.
- `library-insight-kotlin`: Kotlin metadata parsing (`kotlin-metadata-jvm`) and JVM bytecode enrichment.
- `library-insight-search`: Index search and query matching logic.
- `library-insight-export`: JSON, Markdown, and AI context formatters.
- `library-insight-core`: Orchestrates scan flows and implements the semantic API diffing engine.
- `library-insight-cli`: Command Line Interface definitions using **Clikt**.

---

## 🚀 Roadmap

--8<-- "README.md:roadmap"
