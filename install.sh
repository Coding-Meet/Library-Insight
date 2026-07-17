#!/bin/bash
set -e

echo "=================================================="
echo " Building Library Insight CLI Distribution..."
echo "=================================================="
./gradlew :library-insight-cli:installDist

INSTALL_DIR="$HOME/.library-insight"
echo "Installing binaries into $INSTALL_DIR..."
mkdir -p "$INSTALL_DIR"
cp -R library-insight-cli/build/install/library-insight/* "$INSTALL_DIR/"

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
