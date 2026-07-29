---
name: library-insight
description: JVM API Explorer & MCP Server that indexes public APIs, visualizes dependency graphs, audits version deprecations, and extracts JVM archive signatures (JAR/AAR) or Maven coordinates for AI context.
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
> - Use **`library-insight audit`** to check project dependency API deprecations recursively.
> - Use **`library-insight migrate <old> <new>`** to get a migration advisor report with replacement suggestions.
> - Use **`library-insight search-central <query>`** to search Maven Central.
> - Use **`library-insight dependency-graph <coord>`** to visualize dependency trees.
> - Use **`library-insight semver <old> <new>`** to lint Semantic Versioning API compliance.
> - Use **`library-insight mcp`** to communicate as a Model Context Protocol (MCP) server.
> - Use **`library-insight dsl-report [--package <pkg>]`** to generate a Kotlin DSL surface report (type aliases, `@DslMarker` scopes, extension functions, lambda receivers, reified functions).
> - Use **`library-insight examples <class>`** to generate typical usage code examples from bytecode signatures.
> - Use **`library-insight health`** to generate a Package Health & API Complexity Report.
> - Use **`library-insight dependency-check`** to audit all transitive classpath dependencies for runtime conflicts.
> - Use **`library-insight callgraph <class.method>`** to trace internal library method invocation call graphs recursively.
>
> **MCP Integration Rule:**
> If an MCP server is already configured and available (e.g., in Cursor, Claude Desktop, or another IDE with MCP support), **prefer the MCP server over shelling out to the CLI**. The MCP server exposes `scan_library`, `search_symbols`, and `explain_class` tools natively without subprocess overhead.

## Command Reference

The command line tool `library-insight` can be executed globally by:
* Installing via installer script: `curl -fsSL https://raw.githubusercontent.com/Coding-Meet/Library-Insight/main/install.sh | bash`
* Running local build: `~/.library-insight/bin/library-insight` (linked globally as `library-insight`)

{{COMMAND_REFERENCE}}
