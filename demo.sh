#!/bin/bash
# ==========================================================================
# Library Insight - All CLI Commands Demo Script
# ==========================================================================
# This script demonstrates every available library-insight CLI command.
# Run it step by step or as a full walkthrough.
#
# Requirements:
#   - JDK 17+
#   - library-insight installed globally via: curl -fsSL https://raw.githubusercontent.com/Coding-Meet/Library-Insight/main/install.sh | bash
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
echo ">> [1/21] SCAN - Analyze library and build API index"
echo "   library-insight scan $LIBRARY"
echo ""
library-insight scan $LIBRARY
echo ""

# ------------------------------------------------------------------
# 2. SEARCH
# Search for classes, interfaces, methods, or packages in the index
# ------------------------------------------------------------------
echo ">> [2/21] SEARCH - Find a class by name"
echo "   library-insight search Retrofit"
echo ""
library-insight search Retrofit
echo ""

# ------------------------------------------------------------------
# 3. EXPLAIN
# Print detailed structure of a class (constructors, methods, javadoc)
# ------------------------------------------------------------------
echo ">> [3/21] EXPLAIN - Inspect class structure and method signatures"
echo "   library-insight explain Retrofit"
echo ""
library-insight explain Retrofit
echo ""

# ------------------------------------------------------------------
# 4. EXPORT MARKDOWN
# Export the full API index to a readable Markdown reference sheet
# ------------------------------------------------------------------
echo ">> [4/21] EXPORT MARKDOWN - Save readable API reference to file"
echo "   library-insight export markdown"
echo ""
library-insight export markdown
echo ""

# ------------------------------------------------------------------
# 5. EXPORT JSON
# Export the full API index to raw JSON format
# ------------------------------------------------------------------
echo ">> [5/21] EXPORT JSON - Save raw JSON index to file"
echo "   library-insight export json"
echo ""
library-insight export json
echo ""

# ------------------------------------------------------------------
# 6. DIFF
# Compare two library JAR versions and detect breaking changes & severity
# ------------------------------------------------------------------
echo ">> [6/21] DIFF - Compare two library versions for breaking changes & severity"
echo "   library-insight diff $LIBRARY_OLD $LIBRARY_NEW"
echo ""
library-insight diff $LIBRARY_OLD $LIBRARY_NEW
echo ""

# ------------------------------------------------------------------
# 7. AI-EXPORT
# Generate compact per-class JSON files for AI token-efficient context
# ------------------------------------------------------------------
echo ">> [7/21] AI-EXPORT - Generate token-efficient AI context directory"
echo "   library-insight ai-export"
echo ""
library-insight ai-export
echo ""

# ------------------------------------------------------------------
# 8. INIT
# Write a workspace-scoped SKILL.md so local AI agents can discover the CLI
# ------------------------------------------------------------------
echo ">> [8/21] INIT - Initialize AI agent skill for this workspace"
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
echo ">> [9/21] SKILLS ADD - Install AI agent skill to current workspace"
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
echo ">> [10/21] CLEAR-CACHE - Remove locally cached downloaded artifacts"
echo "   library-insight clear-cache"
echo ""
library-insight clear-cache
echo ""

# ------------------------------------------------------------------
# 11. AUDIT
# Scan and report deprecated APIs in project build files recursively
# ------------------------------------------------------------------
echo ">> [11/21] AUDIT - Scan project dependencies and audit deprecated APIs"
echo "   library-insight audit"
echo ""
library-insight audit
echo ""

# ------------------------------------------------------------------
# 12. MIGRATE
# Compare old/new coordinates and generate migration advisors with replacements
# ------------------------------------------------------------------
echo ">> [12/21] MIGRATE - Analyze version upgrade differences and suggest replacements"
echo "   library-insight migrate $LIBRARY_OLD $LIBRARY_NEW"
echo ""
library-insight migrate $LIBRARY_OLD $LIBRARY_NEW
echo ""

# ------------------------------------------------------------------
# 13. SEARCH MAVEN CENTRAL
# Search Maven Central repository for matching coordinates
# ------------------------------------------------------------------
echo ">> [13/21] SEARCH-CENTRAL - Search Maven Central for package metadata"
echo "   library-insight search-central clikt"
echo ""
library-insight search-central clikt
echo ""

# ------------------------------------------------------------------
# 14. DEPENDENCY GRAPH
# Generate an ASCII recursive transitive dependency graph
# ------------------------------------------------------------------
echo ">> [14/21] DEPENDENCY-GRAPH - Generate recursive transitive dependency tree"
echo "   library-insight dependency-graph com.github.ajalt.clikt:clikt-jvm:4.4.0"
echo ""
library-insight dependency-graph com.github.ajalt.clikt:clikt-jvm:4.4.0
echo ""

# ------------------------------------------------------------------
# 15. SEMVER COMPLIANCE CHECKER
# Verify that library modifications comply with SemVer version numbers
# ------------------------------------------------------------------
echo ">> [15/21] SEMVER - Verify Semantic Versioning compliance"
echo "   library-insight semver $LIBRARY_OLD $LIBRARY_NEW"
echo ""
library-insight semver $LIBRARY_OLD $LIBRARY_NEW
echo ""

# ------------------------------------------------------------------
# 16. MCP (Model Context Protocol) Server Test
# Start MCP server and feed it a tools/list request to verify stdio integration
# ------------------------------------------------------------------
echo ">> [16/21] MCP - Test Model Context Protocol tools list interface"
echo "   echo '{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\",\"params\":{}}' | library-insight mcp"
echo ""
echo '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}' | library-insight mcp
echo ""

# ------------------------------------------------------------------
# 17. DSL REPORT
# Generate Kotlin DSL surface report: type aliases, @DslMarker scopes, etc.
# ------------------------------------------------------------------
echo ">> [17/21] DSL-REPORT - Generate Kotlin DSL surface report"
echo "   library-insight dsl-report"
echo ""
library-insight dsl-report
echo ""

# ------------------------------------------------------------------
# 18. EXAMPLES GENERATOR
# Auto-generate usage boilerplate examples for classes & extract markdown guides
# ------------------------------------------------------------------
echo ">> [18/21] EXAMPLES - Auto-generate library usage snippets & extract guide examples"
echo "   library-insight scan sample/build/libs/sample-1.1.0.jar --sources sample"
library-insight scan sample/build/libs/sample-1.1.0.jar --sources sample > /dev/null
echo "   library-insight examples HtmlBuilder"
echo ""
library-insight examples HtmlBuilder
echo ""

# ------------------------------------------------------------------
# 19. PACKAGE HEALTH REPORT
# Print public API counts, deprecation ratio, and complexity indices
# ------------------------------------------------------------------
echo ">> [19/21] HEALTH - Generate Package Health & API Complexity Report"
echo "   library-insight health"
echo ""
library-insight health
echo ""

# ------------------------------------------------------------------
# 20. TRANSITIVE ABI CONFLICT DETECTOR
# Check transitive classpath dependencies for linkage error risks
# ------------------------------------------------------------------
echo ">> [20/21] DEPENDENCY-CHECK - Audit classpath for linkage/ABI conflicts"
echo "   library-insight dependency-check"
echo ""
library-insight dependency-check
echo ""

# ------------------------------------------------------------------
# 21. METHOD CALL GRAPH GENERATOR
# Trace recursive internal method invocations
# ------------------------------------------------------------------
echo ">> [21/21] CALLGRAPH - Renders recursive method invocation tree"
echo "   library-insight callgraph Retrofit.Builder.build"
echo ""
library-insight callgraph Retrofit.Builder.build
echo ""

# ------------------------------------------------------------------
# BONUS: DOCTOR
# Run full diagnostic checks - Java, caches, agent skill status
# ------------------------------------------------------------------
echo ">> [BONUS] DOCTOR - Run system diagnostics and check tool health"
echo "   library-insight doctor"
echo ""
library-insight doctor
echo ""

# Cleanup
rm -rf "$DEMO_WORKSPACE"

echo "$SEPARATOR"
echo " All 21 commands completed successfully!"
echo "$SEPARATOR"
echo ""
