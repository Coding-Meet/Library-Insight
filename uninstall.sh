#!/bin/bash
# ==========================================================================
# Library Insight - Uninstaller Script
# ==========================================================================
# This script removes the global library-insight binary symlink, the
# installation directory ($HOME/.library-insight), and all registered AI skills.
#
# Usage:
#   curl -fsSL https://raw.githubusercontent.com/Coding-Meet/Library-Insight/main/uninstall.sh | bash
#   or
#   ./uninstall.sh
# ==========================================================================

set -e

INSTALL_DIR="$HOME/.library-insight"
SYMLINK="/usr/local/bin/library-insight"
SYMLINK_LI="/usr/local/bin/li"

echo "=================================================="
echo " Uninstalling Library Insight..."
echo "=================================================="

# 1. Remove global symlinks
for sym in "$SYMLINK" "$SYMLINK_LI"; do
    if [ -L "$sym" ] || [ -f "$sym" ]; then
        echo "Removing global symlink at $sym..."
        if [ -w "/usr/local/bin" ]; then
            rm -f "$sym"
        else
            sudo rm -f "$sym"
        fi
        echo " -> Symlink $sym removed."
    fi
done

# 2. Remove installation directory and caches
if [ -d "$INSTALL_DIR" ]; then
    echo "Removing installation directory ($INSTALL_DIR)..."
    rm -rf "$INSTALL_DIR"
    echo " -> Installation directory removed."
fi

# 3. Remove global AI Agent Skill registrations
echo "Removing registered global AI Agent Skills..."
SKILL_PATHS=(
    "$HOME/.claude/skills/library-insight"
    "$HOME/.agents/skills/library-insight"
    "$HOME/.codex/skills/library-insight"
    "$HOME/.cursor/skills/library-insight"
    "$HOME/.gemini/skills/library-insight"
    "$HOME/.gemini/config/skills/library-insight"
    "$HOME/.copilot/skills/library-insight"
    "$HOME/.junie/skills/library-insight"
)

REMOVED_SKILLS=0
for path in "${SKILL_PATHS[@]}"; do
    if [ -d "$path" ]; then
        rm -rf "$path"
        echo " -> Removed AI Skill at: $path"
        REMOVED_SKILLS=$((REMOVED_SKILLS + 1))
    fi
done

echo ""
echo "=================================================="
echo " SUCCESS: Library Insight has been completely uninstalled!"
echo "=================================================="
echo ""
