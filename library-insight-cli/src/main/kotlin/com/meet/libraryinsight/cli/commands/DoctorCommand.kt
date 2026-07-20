package com.meet.libraryinsight.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import java.io.File

class DoctorCommand : CliktCommand(
    name = "doctor",
    help = "Run system diagnostic checks for Library Insight CLI and AI Agent configurations."
) {
    override fun run() {
        echo("==================================================")
        echo("      Library Insight Diagnostics & Doctor")
        echo("==================================================")
        echo("")

        // 1. JDK Check
        val javaVersion = System.getProperty("java.version") ?: "Unknown"
        val javaVendor = System.getProperty("java.vendor") ?: "Unknown"
        echo("1. Java Runtime Environment (JRE):")
        echo("   - Version: $javaVersion")
        echo("   - Vendor: $javaVendor")
        val isOk = javaVersion.startsWith("17") || javaVersion.startsWith("21") || javaVersion.startsWith("22") || javaVersion.startsWith("23") || (javaVersion.split(".").firstOrNull()?.toIntOrNull() ?: 0) >= 17
        if (isOk) {
            echo("   - Status: OK (Java 17+ verified)")
        } else {
            echo("   - Status: WARNING (Recommended Java 17+; found $javaVersion)")
        }
        echo("")

        // 2. Local Cache Directory
        val userHome = System.getProperty("user.home") ?: ""
        val cacheDir = File(File(userHome, ".library-insight"), "cache")
        echo("2. Local Download Cache:")
        echo("   - Path: ${cacheDir.absolutePath}")
        if (cacheDir.exists()) {
            val fileCount = cacheDir.walkBottomUp().filter { it.isFile }.count()
            echo("   - Cache files: $fileCount files cached")
        } else {
            echo("   - Cache files: Directory not created yet (will be created on first scan)")
        }
        echo("")

        // 3. Global AI Skills Registry Check
        echo("3. Global AI Agent Skill Configurations:")
        val agentPaths = mapOf(
            "Cursor" to Pair(File(userHome, ".cursor"), File(userHome, ".cursor/skills/library-insight/SKILL.md")),
            "Gemini Config" to Pair(File(userHome, ".gemini"), File(userHome, ".gemini/config/skills/library-insight/SKILL.md")),
            "Claude Desktop" to Pair(File(userHome, ".claude"), File(userHome, ".claude/skills/library-insight/SKILL.md")),
            "Antigravity Agents" to Pair(File(userHome, ".agents"), File(userHome, ".agents/skills/library-insight/SKILL.md")),
            "GitHub Copilot" to Pair(File(userHome, ".copilot"), File(userHome, ".copilot/skills/library-insight/SKILL.md")),
            "Junie Agent" to Pair(File(userHome, ".junie"), File(userHome, ".junie/skills/library-insight/SKILL.md")),
            "Codex AI" to Pair(File(userHome, ".codex"), File(userHome, ".codex/skills/library-insight/SKILL.md"))
        )

        agentPaths.forEach { (agent, pair) ->
            val (baseFolder, skillFile) = pair
            val status = when {
                skillFile.exists() -> "INSTALLED (Verified)"
                baseFolder.exists() -> "NOT INSTALLED (folder detected)"
                else -> "NOT INSTALLED (no base folder)"
            }
            echo(String.format("   %-22s : %s", "- $agent", status))
        }
        echo("")
        echo("==================================================")
        echo("Diagnostics completed.")
    }
}
