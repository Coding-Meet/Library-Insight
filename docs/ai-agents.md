# AI Agent Skill Integration

**Library Insight** bundles a Custom AI Agent Skill (`SKILL.md`) that teaches AI assistants (like Claude, Gemini, Cursor, Copilot, Junie, etc.) to verify real dependency APIs before writing code.

The skill is designed around one core rule: **do not guess from web examples when the installed library version can be scanned directly.** Agents should use `search`, `explain`, `diff`, and `ai-export` to confirm what exists in the actual artifact.

---

## 1. Global Auto-Integration

When you install the CLI globally via the recommended shell installer (`install.sh`), the script automatically copies the agent skill file into your user profile configurations:

- `~/.cursor/skills/library-insight`
- `~/.gemini/config/skills/library-insight`
- `~/.claude/skills/library-insight`
- `~/.agents/skills/library-insight`
- `~/.copilot/skills/library-insight`
- `~/.junie/skills/library-insight`

Any active AI agent running on your computer will instantly discover and utilize the `library-insight` command tree when you ask a question.

---

## 2. Project Workspace Scoping

If you want to install the skill scoped _only_ to your current project directory (so that any developer working in the repository gets the skill context), run the following command in the project root:

```bash
library-insight init
# or
library-insight skills add
```

This creates `.agents/skills/library-insight/SKILL.md` inside your project root. AI coding assistants loaded in the workspace will automatically read this skill file to understand how to interact with the Library Insight tool tree.

---

## 3. Feeding Source Code Context to LLMs

In addition to compiled dependencies, you can feed raw source code structures directly to AI agents:

1. Scan your project's local source code:
   ```bash
   library-insight scan-source app/src/main
   ```
2. Export the index database into a token-optimized directory structure:
   ```bash
   library-insight ai-export
   ```

This generates a compact, token-efficient split structure under `build/ai-context/`. You can feed this directory into Cursor, Claude, or Gemini workspace chats, giving them full knowledge of your classes, properties, constructors, methods, Javadocs/KDocs, file imports, and declaration line locations without wasting context tokens on massive raw file reads.

---

## 4. Real-World Prompt Examples for Developers & AI Agents

Here are concrete examples of how you can prompt AI coding assistants (Cursor, Claude, Gemini, Copilot, etc.) to use **Library Insight** CLI commands automatically:

### Example 1: Exploring Unfamiliar Libraries & DSLs

> **Developer Prompt:**
> _"Use my `library-insight` CLI to scan `androidx.compose.foundation:foundation-layout:1.12.0`, search for `Grid`, explain its DSL structure, and create a working Jetpack Compose Grid layout example."_

**AI Agent Response Workflow:**

1. Executes `library-insight scan androidx.compose.foundation:foundation-layout:1.12.0`
2. Executes `library-insight search Grid`
3. Executes `library-insight explain Grid --deep` to inspect top-level `GridKt`, receiver scopes (`GridScope`, `GridConfigurationScope`), and KDocs in 1 turn.
4. Generates 100% accurate, hallucination-free Kotlin Compose code matching the exact bytecode API signatures without needing web searches.

---

### Example 2: Scanning Bill of Materials (BOM)

> **Developer Prompt:**
> _"Scan `androidx.compose:compose-bom:2024.09.00` using `library-insight scan` and explain how `LazyVerticalGrid` and `StaggeredGridCells` work."_

**AI Agent Response Workflow:**

1. Executes `library-insight scan androidx.compose:compose-bom:2024.09.00` (automatically parses POM XML, filters out non-Android/KMP stubs via default `--platform android`, and merges target libraries into one index in seconds).
2. Executes `library-insight explain LazyVerticalGrid --deep` to discover `LazyGridScope`, `LazyGridItemSpanScope`, and `GridItemSpan`.
3. Writes precise grid code using the exact version parameter signatures.

---

### Example 3: Auditing Library Version Upgrades & Breaking Changes

> **Developer Prompt:**
> _"Compare Retrofit versions `2.9.0` vs `2.11.0` using `library-insight diff` and tell me what APIs were added, removed, or deprecated."_

**AI Agent Response Workflow:**

1. Executes `library-insight diff com.squareup.retrofit2:retrofit:2.9.0 com.squareup.retrofit2:retrofit:2.11.0`
2. Analyzes the generated JSON/Console diff report for signature modifications.
3. Provides a step-by-step migration advisor guide and deprecation checklist.

---

### Example 4: Exporting Local Source Code Context for AI Coding

> **Developer Prompt:**
> _"Scan our local codebase `library-insight scan-source app/src/main`, run `library-insight ai-export`, and explain how `DashboardViewModel` interacts with our repository layer."_

**AI Agent Response Workflow:**

1. Executes `library-insight scan-source app/src/main`
2. Executes `library-insight ai-export`
3. Inspects token-optimized markdown files in `build/ai-context/` to explain architecture and dependency flows with 0 token waste on raw code files.

---

### Example 5: Version Catalog Auto-Discovery & Zero-Knowledge Scanning

> **Developer Prompt:**
> _"I want to create a grid layout using Compose, but I don't know the exact library version. Find the library name and version from our project's version catalog (`gradle/libs.versions.toml`), scan it with `library-insight`, and generate the code for me."_

**AI Agent Response Workflow:**

1. **Version Catalog Inspection**: The AI Agent inspects `gradle/libs.versions.toml` or `build.gradle.kts` to locate the project's declared library coordinates or BOM references (e.g. `androidx.compose.foundation:foundation-layout` or `compose-bom = "2024.09.00"`).
   _(If not declared in the project, the agent runs `library-insight search-central "foundation-layout"` to query the latest release on Maven Central)._
2. **Recommends & Executes Scan**: The agent recommends the exact command (`library-insight scan androidx.compose.foundation:foundation-layout:1.12.0` or `library-insight scan androidx.compose:compose-bom:2024.09.00`) and executes it immediately.
3. **Explores & Explains**: Runs `library-insight explain Grid --deep` to verify `GridScope`, `GridConfigurationScope`, and parameter signatures for that specific version.
4. **Generates Precise Code**: Delivers exact, version-matched Kotlin code tailored to the developer's project configuration!
