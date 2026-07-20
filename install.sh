#!/bin/bash
set -e

# Fetch latest release version tag from GitHub API if not set
if [ -z "$VERSION" ]; then
    LATEST_TAG=$(curl -s https://api.github.com/repos/Coding-Meet/Library-Insight/releases/latest | grep '"tag_name":' | sed -E 's/.*"v?([^"]+)".*/\1/')
    VERSION="${LATEST_TAG:-1.0.0}"
fi

INSTALL_DIR="$HOME/.library-insight"
ZIP_NAME="library-insight-$VERSION.zip"
RELEASE_URL="https://github.com/Coding-Meet/Library-Insight/releases/download/v$VERSION/$ZIP_NAME"

echo "=================================================="
echo " Installing Library Insight v$VERSION..."
echo "=================================================="

# Create install directory
mkdir -p "$INSTALL_DIR"

# Temporary download path
TEMP_ZIP="/tmp/$ZIP_NAME"

echo "Attempting to download pre-compiled release from GitHub..."
echo "URL: $RELEASE_URL"

# Download using curl
if curl -L --fail -o "$TEMP_ZIP" "$RELEASE_URL"; then
    echo "Download successful! Extracting..."
    unzip -o "$TEMP_ZIP" -d "$INSTALL_DIR"
    
    # Check if files were extracted into a subdirectory and move them up
    if [ -d "$INSTALL_DIR/library-insight-$VERSION" ]; then
        cp -R "$INSTALL_DIR/library-insight-$VERSION"/* "$INSTALL_DIR"
        rm -rf "$INSTALL_DIR/library-insight-$VERSION"
    elif [ -d "$INSTALL_DIR/library-insight-cli-$VERSION" ]; then
        cp -R "$INSTALL_DIR/library-insight-cli-$VERSION"/* "$INSTALL_DIR"
        rm -rf "$INSTALL_DIR/library-insight-cli-$VERSION"
    fi
    rm -f "$TEMP_ZIP"
    echo "Pre-compiled release installed successfully!"
else
    echo "--------------------------------------------------"
    echo " Note: Release zip download failed."
    echo "--------------------------------------------------"
    if [ -f "./gradlew" ]; then
        echo " Falling back to compiling from local source code..."
        ./gradlew :library-insight-cli:installDist
        cp -R library-insight-cli/build/install/library-insight/* "$INSTALL_DIR"
    else
        echo " ERROR: Could not download pre-compiled release and no local gradlew wrapper found."
        exit 1
    fi
fi

echo "Configuring executable permissions..."
chmod +x "$INSTALL_DIR/bin/library-insight"

echo "Creating global symlink..."
if [ -d "/usr/local/bin" ]; then
    echo "Creating symlink in /usr/local/bin/library-insight (may request password for sudo)..."
    sudo ln -sf "$INSTALL_DIR/bin/library-insight" /usr/local/bin/library-insight
    echo "=================================================="
    echo " SUCCESS: Library Insight installed globally!"
    echo " You can now run the 'library-insight' command."
    echo "=================================================="
else
    echo "WARNING: /usr/local/bin does not exist on your system."
    echo "Please add the following to your shell configuration (.zshrc or .bashrc):"
    echo "  export PATH=\"\$PATH:$INSTALL_DIR/bin\""
fi

echo ""
echo "=================================================="
echo " Distributing AI Agent Skill to Detected Configs..."
echo "=================================================="
SKILL_RAW_URL="https://raw.githubusercontent.com/Coding-Meet/Library-Insight/main/.agents/skills/library-insight/SKILL.md"

# Always download latest SKILL.md definition
echo "Fetching latest AI Agent Skill definition..."
curl -fsSL -o "$INSTALL_DIR/SKILL.md" "$SKILL_RAW_URL" || true

SKILL_SOURCE="$INSTALL_DIR/SKILL.md"
if [ ! -s "$SKILL_SOURCE" ] && [ -f ".agents/skills/library-insight/SKILL.md" ]; then
    SKILL_SOURCE=".agents/skills/library-insight/SKILL.md"
fi

if [ -f "$SKILL_SOURCE" ]; then
    PAIRS=(
        "$HOME/.claude:$HOME/.claude/skills/library-insight"
        "$HOME/.agents:$HOME/.agents/skills/library-insight"
        "$HOME/.codex:$HOME/.codex/skills/library-insight"
        "$HOME/.cursor:$HOME/.cursor/skills/library-insight"
        "$HOME/.gemini:$HOME/.gemini/skills/library-insight"
        "$HOME/.gemini:$HOME/.gemini/config/skills/library-insight"
        "$HOME/.copilot:$HOME/.copilot/skills/library-insight"
        "$HOME/.junie:$HOME/.junie/skills/library-insight"
    )

    COPIED_COUNT=0
    for pair in "${PAIRS[@]}"; do
        base_dir="${pair%%:*}"
        skill_dir="${pair#*:}"
        if [ -d "$base_dir" ]; then
            mkdir -p "$skill_dir"
            src_real=$(realpath "$SKILL_SOURCE" 2>/dev/null || echo "$SKILL_SOURCE")
            dst_real=$(realpath "$skill_dir/SKILL.md" 2>/dev/null || echo "$skill_dir/SKILL.md")
            if [ "$src_real" != "$dst_real" ]; then
                cp -f "$SKILL_SOURCE" "$skill_dir/SKILL.md"
            fi
            echo " -> Detected $base_dir - Updated AI Skill at: $skill_dir/SKILL.md"
            COPIED_COUNT=$((COPIED_COUNT + 1))
        fi
    done

    if [ $COPIED_COUNT -gt 0 ]; then
        echo "SUCCESS: AI Agent Skill distributed to $COPIED_COUNT detected config(s)!"
    else
        echo "Note: No global AI agent config folders detected in home directory."
    fi
else
    echo "Note: Local SKILL.md source file not found. Skipping global skill installation."
fi
