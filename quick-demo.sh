#!/bin/bash
# ==========================================================================
# Library Insight — Quick Demo (2–3 min)
# ==========================================================================
# A concise walkthrough of the 6 most important commands.
# Great for onboarding, YouTube demos, and new user exploration.
#
# Requirements:
#   - JDK 17+
#   - library-insight installed: curl -fsSL https://raw.githubusercontent.com/Coding-Meet/Library-Insight/main/install.sh | bash
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
echo " LIBRARY INSIGHT — QUICK DEMO"
echo "$SEP"
echo ""

# ------------------------------------------------------------------
# 1. SCAN
# ------------------------------------------------------------------
echo ">> [1/6] SCAN — Index the library API from Maven"
echo "   library-insight scan $LIBRARY"
echo ""
library-insight scan $LIBRARY
echo ""

# ------------------------------------------------------------------
# 2. SEARCH
# ------------------------------------------------------------------
echo ">> [2/6] SEARCH — Find a class by name"
echo "   library-insight search Retrofit"
echo ""
library-insight search Retrofit
echo ""

# ------------------------------------------------------------------
# 3. EXPLAIN
# ------------------------------------------------------------------
echo ">> [3/6] EXPLAIN — Inspect a class's full API"
echo "   library-insight explain Retrofit"
echo ""
library-insight explain Retrofit
echo ""

# ------------------------------------------------------------------
# 4. AUDIT
# ------------------------------------------------------------------
echo ">> [4/6] AUDIT — Scan project dependencies for deprecated APIs"
echo "   library-insight audit"
echo ""
library-insight audit
echo ""

# ------------------------------------------------------------------
# 5. MIGRATE
# ------------------------------------------------------------------
echo ">> [5/6] MIGRATE — Get a migration report between two versions"
echo "   library-insight migrate $LIBRARY_OLD $LIBRARY_NEW"
echo ""
library-insight migrate $LIBRARY_OLD $LIBRARY_NEW
echo ""

# ------------------------------------------------------------------
# 6. MCP
# ------------------------------------------------------------------
echo ">> [6/6] MCP — Test MCP server tools/list endpoint"
echo "   echo '{...}' | library-insight mcp"
echo ""
echo '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}' | library-insight mcp
echo ""

echo "$SEP"
echo " Done! Run demo.sh for the full 16-command walkthrough."
echo "$SEP"
echo ""
