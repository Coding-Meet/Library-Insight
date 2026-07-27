# AI Agent Skill Integration

**Library Insight** bundles a Custom AI Agent Skill (`SKILL.md`) that teaches AI assistants (like Claude, Gemini, Cursor, Copilot, Junie, etc.) to verify real dependency APIs before writing code.

The skill is designed around one core rule: **do not guess from web examples when the installed library version can be scanned directly.** Agents should use `search`, `explain`, `diff`, and `ai-export` to confirm what exists in the actual artifact.

---

## 1. Global Auto-Integration

When you install the CLI globally via the recommended shell installer (`install.sh`), the script automatically copies the agent skill file into your user profile configurations:

*   `~/.cursor/skills/library-insight`
*   `~/.gemini/config/skills/library-insight`
*   `~/.claude/skills/library-insight`
*   `~/.agents/skills/library-insight`
*   `~/.copilot/skills/library-insight`
*   `~/.junie/skills/library-insight`

Any active AI agent running on your computer will instantly discover and utilize the `library-insight` command tree when you ask a question.

---

## 2. Project Workspace Scoping

If you want to install the skill scoped *only* to your current project directory (so that any developer working in the repository gets the skill context), run the following command in the project root:

```bash
library-insight init
# or
library-insight skills add
```

This creates `.agents/skills/library-insight/SKILL.md` inside your project root. AI coding assistants loaded in the workspace will automatically read this skill file to understand how to interact with the Library Insight tool tree.
