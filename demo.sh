#!/bin/bash
# ==========================================================================
# Library Insight - All CLI Commands Demo Script
# ==========================================================================
# This script demonstrates every available library-insight CLI command.
# Run it step by step or as a full walkthrough.
#
# Requirements:
#   - JDK 17+
#   - library-insight installed globally via: npm install -g library-insight
#
# Usage:
#   chmod +x demo.sh
#   ./demo.sh
# ==========================================================================

set -e

SEPARATOR="=================================================="
LIBRARY="com.squareup.retrofit2:retrofit:2.11.0"
LIBRARY_OLD="com.squareup.retrofit2:retrofit:2.9.0"
LIBRARY_NEW="com.squareup.retrofit2:retrofit:2.11.0"
DEMO_WORKSPACE="/tmp/library-insight-demo-workspace-$$"

echo ""
echo "$SEPARATOR"
echo " LIBRARY INSIGHT - CLI COMMANDS DEMO"
echo "$SEPARATOR"
echo ""

# ------------------------------------------------------------------
# 1. SCAN
# Scan a Maven coordinate, local JAR, or AAR from Gradle cache
# ------------------------------------------------------------------
echo ">> [1/10] SCAN - Analyze library and build API index"
echo "   library-insight scan $LIBRARY"
echo ""
library-insight scan $LIBRARY
echo ""

# ------------------------------------------------------------------
# 2. SEARCH
# Search for classes, interfaces, methods, or packages in the index
# ------------------------------------------------------------------
echo ">> [2/10] SEARCH - Find a class by name"
echo "   library-insight search Retrofit"
echo ""
library-insight search Retrofit
echo ""

# ------------------------------------------------------------------
# 3. EXPLAIN
# Print detailed structure of a class (constructors, methods, javadoc)
# ------------------------------------------------------------------
echo ">> [3/10] EXPLAIN - Inspect class structure and method signatures"
echo "   library-insight explain Retrofit"
echo ""
library-insight explain Retrofit
echo ""

# ------------------------------------------------------------------
# 4. EXPORT MARKDOWN
# Export the full API index to a readable Markdown reference sheet
# (Warning: can be very large for big libraries - use ai-export for AI prompts)
# ------------------------------------------------------------------
echo ">> [4/10] EXPORT MARKDOWN - Save readable API reference to file"
echo "   library-insight export markdown"
echo ""
library-insight export markdown
echo ""

# ------------------------------------------------------------------
# 5. EXPORT JSON
# Export the full API index to raw JSON format
# ------------------------------------------------------------------
echo ">> [5/10] EXPORT JSON - Save raw JSON index to file"
echo "   library-insight export json"
echo ""
library-insight export json
echo ""

# ------------------------------------------------------------------
# 6. DIFF
# Compare two library JAR versions and detect breaking changes
# ------------------------------------------------------------------
echo ">> [6/10] DIFF - Compare two library versions for breaking changes"
echo "   library-insight diff $LIBRARY_OLD $LIBRARY_NEW"
echo ""
library-insight diff $LIBRARY_OLD $LIBRARY_NEW
echo ""

# ------------------------------------------------------------------
# 7. AI-EXPORT
# Generate compact per-class JSON files for AI token-efficient context
# Use this for AI prompts instead of loading the large API_REFERENCE.md
# ------------------------------------------------------------------
echo ">> [7/10] AI-EXPORT - Generate token-efficient AI context directory"
echo "   library-insight ai-export"
echo ""
library-insight ai-export
echo ""

# ------------------------------------------------------------------
# 8. INIT
# Write a workspace-scoped SKILL.md so local AI agents can discover the CLI
# ------------------------------------------------------------------
echo ">> [8/10] INIT - Initialize AI agent skill for this workspace"
echo "   library-insight init"
echo ""
mkdir -p "$DEMO_WORKSPACE"
(
  cd "$DEMO_WORKSPACE"
  library-insight init
)
echo ""

# ------------------------------------------------------------------
# 9. SKILLS ADD
# Install or update the agent SKILL.md in the current workspace
# ------------------------------------------------------------------
echo ">> [9/10] SKILLS ADD - Install AI agent skill to current workspace"
echo "   library-insight skills add"
echo ""
(
  cd "$DEMO_WORKSPACE"
  library-insight skills add
  library-insight skills list
)
echo ""

# ------------------------------------------------------------------
# 10. CLEAR-CACHE
# Delete all locally cached Maven artifacts to free up space
# ------------------------------------------------------------------
echo ">> [10/10] CLEAR-CACHE - Remove locally cached downloaded artifacts"
echo "   library-insight clear-cache"
echo ""
library-insight clear-cache
echo ""

# ------------------------------------------------------------------
# BONUS: DOCTOR
# Run full diagnostic checks - Java, Node.js, caches, agent skill status
# ------------------------------------------------------------------
echo ">> [BONUS] DOCTOR - Run system diagnostics and check tool health"
echo "   library-insight doctor"
echo ""
library-insight doctor
echo ""

echo "$SEPARATOR"
echo " All commands completed successfully!"
echo "$SEPARATOR"
echo ""
