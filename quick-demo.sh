#!/bin/bash
# ==========================================================================
# Library Insight — Quick Demo (3 min)
# ==========================================================================
# A concise walkthrough of the 11 most important commands.
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
echo " LIBRARY INSIGHT — QUICK DEMO  (v1.2.0 Suite)"
echo "$SEP"
echo ""

# ------------------------------------------------------------------
# 1. SCAN
# ------------------------------------------------------------------
echo ">> [1/11] SCAN — Index the library API from Maven"
echo "   library-insight scan $LIBRARY"
echo ""
library-insight scan $LIBRARY
echo ""

# ------------------------------------------------------------------
# 2. SEARCH
# ------------------------------------------------------------------
echo ">> [2/11] SEARCH — Find class or signature (supports types & anno: prefix)"
echo "   library-insight search \"anno:Keep\""
echo ""
library-insight search "anno:Keep"
echo ""

# ------------------------------------------------------------------
# 3. EXPLAIN
# ------------------------------------------------------------------
echo ">> [3/11] EXPLAIN — Inspect class structure, DSL scopes, & receivers"
echo "   library-insight explain Retrofit"
echo ""
library-insight explain Retrofit
echo ""

# ------------------------------------------------------------------
# 4. DSL REPORT
# ------------------------------------------------------------------
echo ">> [4/11] DSL-REPORT — Kotlin DSL scopes, aliases, and fluent API list"
echo "   library-insight dsl-report"
echo ""
library-insight dsl-report
echo ""

# ------------------------------------------------------------------
# 5. EXAMPLES
# ------------------------------------------------------------------
echo ">> [5/11] EXAMPLES — Generate usage patterns & extract guide examples"
echo "   library-insight scan sample/build/libs/sample-1.1.0.jar --sources sample"
library-insight scan sample/build/libs/sample-1.1.0.jar --sources sample > /dev/null
echo "   library-insight examples HtmlBuilder"
echo ""
library-insight examples HtmlBuilder
echo ""

# ------------------------------------------------------------------
# 6. HEALTH
# ------------------------------------------------------------------
echo ">> [6/11] HEALTH — Generate Package Health & API Complexity Report"
echo "   library-insight health"
echo ""
library-insight health
echo ""

# ------------------------------------------------------------------
# 7. AUDIT
# ------------------------------------------------------------------
echo ">> [7/11] AUDIT — Scan dependencies for deprecated APIs recursively"
echo "   library-insight audit"
echo ""
library-insight audit
echo ""

# ------------------------------------------------------------------
# 8. MIGRATE
# ------------------------------------------------------------------
echo ">> [8/11] MIGRATE — Get migration advisor report with replacements"
echo "   library-insight migrate $LIBRARY_OLD $LIBRARY_NEW"
echo ""
library-insight migrate $LIBRARY_OLD $LIBRARY_NEW
echo ""

# ------------------------------------------------------------------
# 9. DEPENDENCY-CHECK
# ------------------------------------------------------------------
echo ">> [9/11] DEPENDENCY-CHECK — Scan classpath for Linkage/ABI conflicts"
echo "   library-insight dependency-check"
echo ""
library-insight dependency-check
echo ""

# ------------------------------------------------------------------
# 10. CALLGRAPH
# ------------------------------------------------------------------
echo ">> [10/11] CALLGRAPH — Generate method call graph visual tree"
echo "   library-insight callgraph Retrofit.Builder.build"
echo ""
library-insight callgraph Retrofit.Builder.build
echo ""

# ------------------------------------------------------------------
# 11. MCP
# ------------------------------------------------------------------
echo ">> [11/11] MCP — Test Model Context Protocol tools list interface"
echo "   echo '{...}' | library-insight mcp"
echo ""
echo '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}' | library-insight mcp
echo ""

echo "$SEP"
echo " Done! Run demo.sh for the full 21-command walkthrough."
echo "$SEP"
echo ""
