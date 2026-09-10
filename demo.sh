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

CLI_CMD="library-insight"
if command -v li &> /dev/null; then
    CLI_CMD="li"
fi

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
echo ">> 1. SCAN - Analyze library and build API index"
echo "   $CLI_CMD scan $LIBRARY"
echo ""
$CLI_CMD scan $LIBRARY
echo ""

# ------------------------------------------------------------------
# 2. SCAN-SOURCE
# Scan local raw Kotlin & Java source files directly without compilation
# ------------------------------------------------------------------
echo ">> 2. SCAN-SOURCE - Analyze local source directory and build API index"
echo "   $CLI_CMD scan-source sample/src/main/kotlin"
echo ""
$CLI_CMD scan-source sample/src/main/kotlin
echo ""

# ------------------------------------------------------------------
# 3. SEARCH
# Search for classes, interfaces, methods, or packages in the index
# ------------------------------------------------------------------
echo ">> 3. SEARCH - Find a class by name"
echo "   $CLI_CMD search Grid"
echo ""
$CLI_CMD search Grid
echo ""

# ------------------------------------------------------------------
# 4. EXPLAIN & DEEP DSL RESOLUTION
# Print detailed structure of a class/symbol & recursively expand DSL scopes
# ------------------------------------------------------------------
echo ">> 4. EXPLAIN & DEEP EXPLAIN - Inspect class structure and deep DSL scopes"
echo "   $CLI_CMD explain Grid --deep"
echo ""
$CLI_CMD explain Grid --deep
echo ""

# ------------------------------------------------------------------
# 5. EXPORT MARKDOWN
# Export the full API index to a readable Markdown reference sheet
# ------------------------------------------------------------------
echo ">> 5. EXPORT MARKDOWN - Save readable API reference to file"
echo "   $CLI_CMD export markdown"
echo ""
$CLI_CMD export markdown
echo ""

# ------------------------------------------------------------------
# 6. EXPORT JSON
# Export the full API index to raw JSON format
# ------------------------------------------------------------------
echo ">> 6. EXPORT JSON - Save raw JSON index to file"
echo "   $CLI_CMD export json"
echo ""
$CLI_CMD export json
echo ""

# ------------------------------------------------------------------
# 7. DIFF
# Compare two library JAR versions and detect breaking changes & severity
# ------------------------------------------------------------------
echo ">> 7. DIFF - Compare two library versions for breaking changes & severity"
echo "   $CLI_CMD diff $LIBRARY_OLD $LIBRARY_NEW"
echo ""
$CLI_CMD diff $LIBRARY_OLD $LIBRARY_NEW
echo ""

# ------------------------------------------------------------------
# 8. AI-EXPORT
# Generate compact per-class JSON files for AI token-efficient context
# ------------------------------------------------------------------
echo ">> 8. AI-EXPORT - Generate token-efficient AI context directory"
echo "   $CLI_CMD ai-export"
echo ""
$CLI_CMD ai-export
echo ""

# ------------------------------------------------------------------
# 9. INIT
# Write a workspace-scoped SKILL.md so local AI agents can discover the CLI
# ------------------------------------------------------------------
echo ">> 9. INIT - Initialize AI agent skill for this workspace"
echo "   $CLI_CMD init"
echo ""
mkdir -p "$DEMO_WORKSPACE"
(
  cd "$DEMO_WORKSPACE"
  $CLI_CMD init
)
echo ""

# ------------------------------------------------------------------
# 10. SKILLS ADD
# Install or update the agent SKILL.md in the current workspace
# ------------------------------------------------------------------
echo ">> 10. SKILLS ADD - Install AI agent skill to current workspace"
echo "   $CLI_CMD skills add"
echo ""
(
  cd "$DEMO_WORKSPACE"
  $CLI_CMD skills add
  $CLI_CMD skills list
)
echo ""

# ------------------------------------------------------------------
# 11. CLEAR-CACHE
# Delete all locally cached Maven artifacts to free up space
# ------------------------------------------------------------------
echo ">> 11. CLEAR-CACHE - Remove locally cached downloaded artifacts"
echo "   $CLI_CMD clear-cache"
echo ""
$CLI_CMD clear-cache
echo ""

# ------------------------------------------------------------------
# 12. AUDIT
# Scan and report deprecated APIs in project build files recursively
# ------------------------------------------------------------------
echo ">> 12. AUDIT - Scan project dependencies and audit deprecated APIs"
echo "   $CLI_CMD audit"
echo ""
$CLI_CMD audit
echo ""

# ------------------------------------------------------------------
# 13. MIGRATE
# Compare old/new coordinates and generate migration advisors with replacements
# ------------------------------------------------------------------
echo ">> 13. MIGRATE - Analyze version upgrade differences and suggest replacements"
echo "   $CLI_CMD migrate $LIBRARY_OLD $LIBRARY_NEW"
echo ""
$CLI_CMD migrate $LIBRARY_OLD $LIBRARY_NEW
echo ""

# ------------------------------------------------------------------
# 14. SEARCH MAVEN CENTRAL
# Search Maven Central repository for matching coordinates
# ------------------------------------------------------------------
echo ">> 14. SEARCH-CENTRAL - Search Maven Central for package metadata"
echo "   $CLI_CMD search-central clikt"
echo ""
$CLI_CMD search-central clikt
echo ""

# ------------------------------------------------------------------
# 15. DEPENDENCY GRAPH
# Generate an ASCII recursive transitive dependency graph
# ------------------------------------------------------------------
echo ">> 15. DEPENDENCY-GRAPH - Generate recursive transitive dependency tree"
echo "   $CLI_CMD dependency-graph com.github.ajalt.clikt:clikt-jvm:4.4.0"
echo ""
$CLI_CMD dependency-graph com.github.ajalt.clikt:clikt-jvm:4.4.0
echo ""

# ------------------------------------------------------------------
# 16. SEMVER COMPLIANCE CHECKER
# Verify that library modifications comply with SemVer version numbers
# ------------------------------------------------------------------
echo ">> 16. SEMVER - Verify Semantic Versioning compliance"
echo "   $CLI_CMD semver $LIBRARY_OLD $LIBRARY_NEW"
echo ""
$CLI_CMD semver $LIBRARY_OLD $LIBRARY_NEW
echo ""

# ------------------------------------------------------------------
# 17. MCP (Model Context Protocol) Server Test
# Start MCP server and feed it a tools/list request to verify stdio integration
# ------------------------------------------------------------------
echo ">> 17. MCP - Test Model Context Protocol tools list interface"
echo "   echo '{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\",\"params\":{}}' | $CLI_CMD mcp"
echo ""
echo '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}' | $CLI_CMD mcp
echo ""

# ------------------------------------------------------------------
# 18. DSL REPORT
# Generate Kotlin DSL surface report: type aliases, @DslMarker scopes, etc.
# ------------------------------------------------------------------
echo ">> 18. DSL-REPORT - Generate Kotlin DSL surface report"
echo "   $CLI_CMD dsl-report"
echo ""
$CLI_CMD dsl-report
echo ""

# ------------------------------------------------------------------
# 19. EXAMPLES GENERATOR
# Auto-generate usage boilerplate examples for classes & extract markdown guides
# ------------------------------------------------------------------
echo ">> 19. EXAMPLES - Auto-generate library usage snippets & extract guide examples"
echo "   $CLI_CMD scan sample/build/libs/sample-1.1.0.jar --sources sample"
$CLI_CMD scan sample/build/libs/sample-1.1.0.jar --sources sample > /dev/null
echo "   $CLI_CMD examples HtmlBuilder"
echo ""
$CLI_CMD examples HtmlBuilder
echo ""

# ------------------------------------------------------------------
# 20. PACKAGE HEALTH REPORT
# Print public API counts, deprecation ratio, and complexity indices
# ------------------------------------------------------------------
echo ">> 20. HEALTH - Generate Package Health & API Complexity Report"
echo "   $CLI_CMD health"
echo ""
$CLI_CMD health
echo ""

# ------------------------------------------------------------------
# 21. TRANSITIVE ABI CONFLICT DETECTOR
# Check transitive classpath dependencies for linkage error risks
# ------------------------------------------------------------------
echo ">> 21. DEPENDENCY-CHECK - Audit classpath for linkage/ABI conflicts"
echo "   $CLI_CMD dependency-check"
echo ""
$CLI_CMD dependency-check
echo ""

# ------------------------------------------------------------------
# 22. METHOD CALL GRAPH GENERATOR
# Trace recursive internal method invocations
# ------------------------------------------------------------------
echo ">> 22. CALLGRAPH - Renders recursive method invocation tree"
echo "   $CLI_CMD callgraph HtmlBuilder.div"
echo ""
$CLI_CMD callgraph HtmlBuilder.div
echo ""

# ------------------------------------------------------------------
# BONUS: DOCTOR
# Run full diagnostic checks - Java, caches, agent skill status
# ------------------------------------------------------------------
echo ">> BONUS. DOCTOR - Run system diagnostics and check tool health"
echo "   $CLI_CMD doctor"
echo ""
$CLI_CMD doctor
echo ""

# ------------------------------------------------------------------
# 23. KMP SCAN & EXPLAIN
# Resolve target platforms and print platform indicators
# ------------------------------------------------------------------
echo ">> 23. KMP SCAN & EXPLAIN - Resolve KMP targets and merge API indexes"
echo "   $CLI_CMD scan io.ktor:ktor-client-core:3.0.0 --db build/ktor-index.json"
$CLI_CMD scan io.ktor:ktor-client-core:3.0.0 --db build/ktor-index.json > /dev/null
echo "   $CLI_CMD explain io.ktor.client.HttpClient --db build/ktor-index.json"
echo ""
$CLI_CMD explain io.ktor.client.HttpClient --db build/ktor-index.json | head -n 35
echo ""

# Cleanup
rm -rf "$DEMO_WORKSPACE"
rm -f build/ktor-index.json

echo "$SEPARATOR"
echo " All 23 commands completed successfully!"
echo "$SEPARATOR"
echo ""
