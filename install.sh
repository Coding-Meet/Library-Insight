#!/bin/bash
set -e

VERSION="1.0.0"
INSTALL_DIR="$HOME/.library-insight"
ZIP_NAME="library-insight-cli-$VERSION.zip"
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
    unzip -o "$TEMP_ZIP" -d "$INSTALL_DIR/"
    
    # Check if files were extracted into a subdirectory and move them up
    if [ -d "$INSTALL_DIR/library-insight-cli-$VERSION" ]; then
        cp -R "$INSTALL_DIR/library-insight-cli-$VERSION"/* "$INSTALL_DIR/"
        rm -rf "$INSTALL_DIR/library-insight-cli-$VERSION"
    fi
    rm -f "$TEMP_ZIP"
    echo "Pre-compiled release installed successfully!"
else
    echo "--------------------------------------------------"
    echo " Note: Release download failed (e.g. tag not pushed yet)."
    echo " Falling back to compiling from local source code..."
    echo "--------------------------------------------------"
    ./gradlew :library-insight-cli:installDist
    cp -R library-insight-cli/build/install/library-insight/* "$INSTALL_DIR/"
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
