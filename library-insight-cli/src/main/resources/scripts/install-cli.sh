#!/bin/bash
# Check if library-insight is already available
if command -v library-insight &> /dev/null; then
    echo "library-insight is already installed and available on PATH."
    exit 0
fi

echo "library-insight not found. Installing via official installer script..."
curl -fsSL https://raw.githubusercontent.com/Coding-Meet/Library-Insight/main/install.sh | bash
