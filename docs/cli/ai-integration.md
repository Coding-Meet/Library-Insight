# AI Context & Integration

## `ai-export` — AI Context Export (Recommended for AI prompts)

Splits the scanned database into a token-efficient directory structure under `build/ai-context/`. AI agents read `metadata.json` first, then load only the class files they need — reducing token usage by 95%+.

```bash
library-insight ai-export
```

**Positional Arguments:**

- `[output-dir]`: Optional target output directory to save AI context files (default: `build/ai-context/`)

**Optional Parameters:**

- `--db <file>`: Index database JSON file path to read from (default: `build/library-insight-index.json`)

**Example with options:**

```bash
library-insight ai-export custom-ai-context/ --db custom-index.json
```

**Example output:**

```
Generated compact LLM context directory structure at: build/ai-context
```

---

## `mcp` — MCP Server

Start the Model Context Protocol server on stdio. Connect Cursor, Claude Desktop, or any MCP-compatible IDE to use `scan_library`, `search_symbols`, `explain_class`, and `dsl_report` tools natively.

```bash
library-insight mcp
```

**Optional Parameters:**

- `--db <file>`: Index database JSON file path to read from and write to (default: `build/library-insight-index.json`)

**Example with options:**

```bash
library-insight mcp --db /path/to/project/custom-index.json
```

**Example output (JSON-RPC tools list response):**

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "tools": [
      {
        "name": "scan_library",
        "description": "Scans a Java/Kotlin library and creates an API index.",
        "inputSchema": {
          "type": "object",
          "properties": { "pathOrCoordinate": { "type": "string" } }
        }
      },
      {
        "name": "search_symbols",
        "description": "Search for symbols in the active library index.",
        "inputSchema": {
          "type": "object",
          "properties": { "query": { "type": "string" } }
        }
      }
    ]
  }
}
```

> **MCP vs CLI:** If an MCP server is already configured in your IDE, prefer it over running CLI commands directly.

---

## `init` — Initialize Workspace Skill

Write a `SKILL.md` into `.agents/skills/library-insight/` so local AI agents auto-discover the CLI.

```bash
library-insight init
```

**Example output:**

```
Initializing Library Insight agent environment...
Creating directory: .agents/skills/library-insight/
Successfully initialized workspace skill instructions!
```

---

## `skills` — Manage Agent Skills

**Add skill to the current workspace:**

```bash
library-insight skills add
```

**List registered skills in the current workspace:**

```bash
library-insight skills list
```

**Example output (skills list):**

```
Workspace AI Agent Skills:
  - [Installed] library-insight
```

---

## `export` — Export Index

Export the scanned index to Markdown or JSON.

> For large libraries, Markdown files can be huge. Use `ai-export` for AI prompts instead.

**Export to Markdown format:**

```bash
library-insight export markdown
```

**Export to JSON format:**

```bash
library-insight export json
```

**Positional Arguments:**

- `[output-file]`: Optional target output file path to write export content to. If not specified, defaults to `build/API_REFERENCE.md` or `build/library-insight-index.json`. Use `-` to print to stdout.

**Optional Parameters:**

- `--db <file>`: Index database JSON file path to read from (default: `build/library-insight-index.json`)

**Example with options:**

```bash
library-insight export markdown API_REFERENCE.md --db custom-index.json
```

**Example output:**

```
Exported MARKDOWN to: build/API_REFERENCE.md
```

---
