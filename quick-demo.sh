#!/bin/bash
# ==========================================================================
# Library Insight — Quick Demo (3 min)
# ==========================================================================
# A concise walkthrough of the 12 most important commands.
# Great for onboarding, YouTube demos, and new user exploration.
#
# Requirements:
#   - JDK 17+
#   - library-insight installed:
#     curl -fsSL https://raw.githubusercontent.com/Coding-Meet/Library-Insight/main/install.sh | bash
#
# Usage:
#   chmod +x quick-demo.sh
#   ./quick-demo.sh
# ==========================================================================

set -e

LIBRARY="com.squareup.retrofit2:retrofit:2.11.0"
LIBRARY_OLD="com.squareup.retrofit2:retrofit:2.9.0"
LIBRARY_NEW="com.squareup.retrofit2:retrofit:2.11.0"
SEP="=================================================="

CLI_CMD="library-insight"
if command -v li &> /dev/null; then
    CLI_CMD="li"
fi

echo ""
echo "$SEP"
echo " LIBRARY INSIGHT — QUICK DEMO  (v1.4.2 Suite)"
echo "$SEP"
echo ""

# ------------------------------------------------------------------
# 1. SCAN
# ------------------------------------------------------------------
echo ">> 1. SCAN — Index compiled dependency API from Maven"
echo "   $CLI_CMD scan $LIBRARY"
echo ""
$CLI_CMD scan $LIBRARY
echo ""

# ------------------------------------------------------------------
# 2. SCAN-SOURCE
# ------------------------------------------------------------------
echo ">> 2. SCAN-SOURCE — Index local raw Java & Kotlin source files"
echo "   $CLI_CMD scan-source sample/src/main/kotlin"
echo ""
$CLI_CMD scan-source sample/src/main/kotlin
echo ""

# ------------------------------------------------------------------
# 3. SEARCH
# ------------------------------------------------------------------
echo ">> 3. SEARCH — Find class, methods, or sourceLocation annotations"
echo "   $CLI_CMD search \"anno:Keep\""
echo ""
$CLI_CMD search "anno:Keep"
echo ""

# ------------------------------------------------------------------
# 4. EXPLAIN & DEEP DSL RESOLUTION
# ------------------------------------------------------------------
echo ">> 4. EXPLAIN — Deep inspect class structure, source pointers, DSL scopes, & receivers"
echo "   $CLI_CMD explain Grid --deep"
echo ""
$CLI_CMD explain Grid --deep
echo ""

# ------------------------------------------------------------------
# 5. DSL REPORT
# ------------------------------------------------------------------
echo ">> 5. DSL-REPORT — Kotlin DSL scopes, aliases, and extension receivers"
echo "   $CLI_CMD dsl-report"
echo ""
$CLI_CMD dsl-report
echo ""

# ------------------------------------------------------------------
# 6. EXAMPLES
# ------------------------------------------------------------------
echo ">> 6. EXAMPLES — Generate usage patterns & extract guide examples"
echo "   $CLI_CMD scan sample/build/libs/sample-1.1.0.jar --sources sample"
$CLI_CMD scan sample/build/libs/sample-1.1.0.jar --sources sample > /dev/null
echo "   $CLI_CMD examples HtmlBuilder"
echo ""
$CLI_CMD examples HtmlBuilder
echo ""

# ------------------------------------------------------------------
# 7. HEALTH
# ------------------------------------------------------------------
echo ">> 7. HEALTH — Generate Package Health & API Complexity Report"
echo "   $CLI_CMD health"
echo ""
$CLI_CMD health
echo ""

# ------------------------------------------------------------------
# 8. AUDIT
# ------------------------------------------------------------------
echo ">> 8. AUDIT — Scan dependencies for deprecated APIs recursively"
echo "   $CLI_CMD audit"
echo ""
$CLI_CMD audit
echo ""

# ------------------------------------------------------------------
# 9. MIGRATE
# ------------------------------------------------------------------
echo ">> 9. MIGRATE — Get migration advisor report with replacements"
echo "   $CLI_CMD migrate $LIBRARY_OLD $LIBRARY_NEW"
echo ""
$CLI_CMD migrate $LIBRARY_OLD $LIBRARY_NEW
echo ""

# ------------------------------------------------------------------
# 10. DEPENDENCY-CHECK
# ------------------------------------------------------------------
echo ">> 10. DEPENDENCY-CHECK — Scan classpath for Linkage/ABI conflicts"
echo "   $CLI_CMD dependency-check"
echo ""
$CLI_CMD dependency-check
echo ""

# ------------------------------------------------------------------
# 11. CALLGRAPH
# ------------------------------------------------------------------
echo ">> 11. CALLGRAPH — Generate method call graph visual tree"
echo "   $CLI_CMD callgraph HtmlBuilder.div"
echo ""
$CLI_CMD callgraph HtmlBuilder.div
echo ""

# ------------------------------------------------------------------
# 12. MCP
# ------------------------------------------------------------------
echo ">> 12. MCP — Test Model Context Protocol tools list interface"
echo "   echo '{...}' | $CLI_CMD mcp"
echo ""
echo '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}' | $CLI_CMD mcp
echo ""

# ------------------------------------------------------------------
# 13. KMP SCAN & EXPLAIN — Kotlin Multiplatform support
# ------------------------------------------------------------------
echo ">> 13. KMP SCAN & EXPLAIN — Multi-target platform variant resolution"
echo "   $CLI_CMD scan io.ktor:ktor-client-core:3.0.0 --db build/ktor-index.json"
$CLI_CMD scan io.ktor:ktor-client-core:3.0.0 --db build/ktor-index.json > /dev/null
echo "   $CLI_CMD explain io.ktor.client.HttpClient --db build/ktor-index.json"
echo ""
$CLI_CMD explain io.ktor.client.HttpClient --db build/ktor-index.json | head -n 35
echo ""

# Cleanup
rm -f build/ktor-index.json

echo "$SEP"
echo " Done! Run demo.sh for the full 23-command walkthrough."
echo "$SEP"
echo ""
