#!/bin/bash
# ==========================================================================
# Library Insight — Quick Demo (2–3 min)
# ==========================================================================
# A concise walkthrough of the 7 most important commands.
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
echo " LIBRARY INSIGHT — QUICK DEMO  (v1.2.0 + Kotlin DSL Support)"
echo "$SEP"
echo ""

# ------------------------------------------------------------------
# 1. SCAN
# ------------------------------------------------------------------
echo ">> [1/7] SCAN — Index the library API from Maven"
echo "   library-insight scan $LIBRARY"
echo ""
library-insight scan $LIBRARY
echo ""

# ------------------------------------------------------------------
# 2. SEARCH
# ------------------------------------------------------------------
echo ">> [2/7] SEARCH — Find a class by name"
echo "   library-insight search Retrofit"
echo ""
library-insight search Retrofit
echo ""

# ------------------------------------------------------------------
# 3. EXPLAIN
# ------------------------------------------------------------------
echo ">> [3/7] EXPLAIN — Inspect a class's full API (with reified, DSL scopes, lambda receivers)"
echo "   library-insight explain Retrofit"
echo ""
library-insight explain Retrofit
echo ""

# ------------------------------------------------------------------
# 4. DSL REPORT
# ------------------------------------------------------------------
echo ">> [4/7] DSL-REPORT — Kotlin DSL surface: type aliases, scopes, extensions, lambda receivers"
echo "   library-insight dsl-report"
echo ""
library-insight dsl-report
echo ""

# ------------------------------------------------------------------
# 5. AUDIT
# ------------------------------------------------------------------
echo ">> [5/7] AUDIT — Scan project dependencies for deprecated APIs"
echo "   library-insight audit"
echo ""
library-insight audit
echo ""

# ------------------------------------------------------------------
# 6. MIGRATE
# ------------------------------------------------------------------
echo ">> [6/7] MIGRATE — Get a migration report between two versions"
echo "   library-insight migrate $LIBRARY_OLD $LIBRARY_NEW"
echo ""
library-insight migrate $LIBRARY_OLD $LIBRARY_NEW
echo ""

# ------------------------------------------------------------------
# 7. MCP
# ------------------------------------------------------------------
echo ">> [7/7] MCP — Test MCP server tools/list endpoint"
echo "   echo '{...}' | library-insight mcp"
echo ""
echo '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}' | library-insight mcp
echo ""

echo "$SEP"
echo " Done! Run demo.sh for the full 17-command walkthrough."
echo "$SEP"
echo ""
