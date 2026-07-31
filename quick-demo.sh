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

echo ""
echo "$SEP"
echo " LIBRARY INSIGHT — QUICK DEMO  (v1.3.0 Suite)"
echo "$SEP"
echo ""

# ------------------------------------------------------------------
# 1. SCAN
# ------------------------------------------------------------------
echo ">> 1. SCAN — Index compiled dependency API from Maven"
echo "   library-insight scan $LIBRARY"
echo ""
library-insight scan $LIBRARY
echo ""

# ------------------------------------------------------------------
# 2. SCAN-SOURCE
# ------------------------------------------------------------------
echo ">> 2. SCAN-SOURCE — Index local raw Java & Kotlin source files"
echo "   library-insight scan-source sample/src/main/kotlin"
echo ""
library-insight scan-source sample/src/main/kotlin
echo ""

# ------------------------------------------------------------------
# 3. SEARCH
# ------------------------------------------------------------------
echo ">> 3. SEARCH — Find class, methods, or sourceLocation annotations"
echo "   library-insight search \"anno:Keep\""
echo ""
library-insight search "anno:Keep"
echo ""

# ------------------------------------------------------------------
# 4. EXPLAIN
# ------------------------------------------------------------------
echo ">> 4. EXPLAIN — Inspect class structure, source pointers, DSL scopes, & receivers"
echo "   library-insight explain HtmlBuilder"
echo ""
library-insight explain HtmlBuilder
echo ""

# ------------------------------------------------------------------
# 5. DSL REPORT
# ------------------------------------------------------------------
echo ">> 5. DSL-REPORT — Kotlin DSL scopes, aliases, and extension receivers"
echo "   library-insight dsl-report"
echo ""
library-insight dsl-report
echo ""

# ------------------------------------------------------------------
# 6. EXAMPLES
# ------------------------------------------------------------------
echo ">> 6. EXAMPLES — Generate usage patterns & extract guide examples"
echo "   library-insight scan sample/build/libs/sample-1.1.0.jar --sources sample"
library-insight scan sample/build/libs/sample-1.1.0.jar --sources sample > /dev/null
echo "   library-insight examples HtmlBuilder"
echo ""
library-insight examples HtmlBuilder
echo ""

# ------------------------------------------------------------------
# 7. HEALTH
# ------------------------------------------------------------------
echo ">> 7. HEALTH — Generate Package Health & API Complexity Report"
echo "   library-insight health"
echo ""
library-insight health
echo ""

# ------------------------------------------------------------------
# 8. AUDIT
# ------------------------------------------------------------------
echo ">> 8. AUDIT — Scan dependencies for deprecated APIs recursively"
echo "   library-insight audit"
echo ""
library-insight audit
echo ""

# ------------------------------------------------------------------
# 9. MIGRATE
# ------------------------------------------------------------------
echo ">> 9. MIGRATE — Get migration advisor report with replacements"
echo "   library-insight migrate $LIBRARY_OLD $LIBRARY_NEW"
echo ""
library-insight migrate $LIBRARY_OLD $LIBRARY_NEW
echo ""

# ------------------------------------------------------------------
# 10. DEPENDENCY-CHECK
# ------------------------------------------------------------------
echo ">> 10. DEPENDENCY-CHECK — Scan classpath for Linkage/ABI conflicts"
echo "   library-insight dependency-check"
echo ""
library-insight dependency-check
echo ""

# ------------------------------------------------------------------
# 11. CALLGRAPH
# ------------------------------------------------------------------
echo ">> 11. CALLGRAPH — Generate method call graph visual tree"
echo "   library-insight callgraph HtmlBuilder.div"
echo ""
library-insight callgraph HtmlBuilder.div
echo ""

# ------------------------------------------------------------------
# 12. MCP
# ------------------------------------------------------------------
echo ">> 12. MCP — Test Model Context Protocol tools list interface"
echo "   echo '{...}' | library-insight mcp"
echo ""
echo '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}' | library-insight mcp
echo ""

echo "$SEP"
echo " Done! Run demo.sh for the full 22-command walkthrough."
echo "$SEP"
echo ""
