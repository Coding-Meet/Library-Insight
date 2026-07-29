# Model Context Protocol (MCP) Integration

**Library Insight** can run as a Model Context Protocol (MCP) server over standard input/output (`stdio`). This allows compatible AI IDEs and clients (like **Cursor**, **Claude Desktop**, or **VS Code** with MCP extensions) to query the library parser and search engine natively.

When connected, the AI agent gains access to the following tools:

*   `scan_library`: Scan and index a JAR/AAR or Maven coordinate.
*   `search_symbols`: Query the API index for classes, methods, or properties matching a name.
*   `explain_class`: Retrieve the exact public API signature and documentation for a class.
*   `dsl_report`: Generate a Kotlin DSL surface report for the active library index (type aliases, `@DslMarker` scopes, extension functions, builders, and inline reified entry points).

---

## How to Connect

### 1. Cursor IDE

1. Open **Cursor Settings** (gear icon in the top right corner).
2. Go to **Features** > **MCP**.
3. Click **+ Add New MCP Server**.
4. Configure the settings:
    *   **Name**: `Library Insight`
    *   **Type**: `command`
    *   **Command**: `library-insight mcp`
5. Click **Save**. The status should change to a green dot showing it's connected.

> [!NOTE]
> If you didn't install `library-insight` globally, specify the absolute path to the compiled script, e.g., `/Users/username/Library-Insight/library-insight-cli/build/install/library-insight/bin/library-insight mcp`.

---

### 2. Claude Desktop

To add the tool to the Claude Desktop client, edit your configuration file:

*   **macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
*   **Windows**: `%APPDATA%\Claude\claude_desktop_config.json`

Add `library-insight` under `mcpServers`:

```json
{
  "mcpServers": {
    "library-insight": {
      "command": "library-insight",
      "args": ["mcp"]
    }
  }
}
```

Restart Claude Desktop, and you should see the tool icon appear in your chat window.

---

## Verification

To verify that the MCP server is working, ask the AI in your IDE:
> "Use Library Insight to search for Retrofit and explain it."

The assistant will make a tool call to the MCP server, retrieve the Retrofit class structure, and print the results without having to index the entire codebase or read external documents.
