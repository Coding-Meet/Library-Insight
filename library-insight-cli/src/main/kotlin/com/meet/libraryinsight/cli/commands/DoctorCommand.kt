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

        // 2. Node Check
        echo("2. Node.js Environment:")
        try {
            val process = Runtime.getRuntime().exec(arrayOf("node", "-v"))
            val version = process.inputStream.bufferedReader().use { it.readText() }.trim()
            echo("   - Version: $version")
            echo("   - Status: OK")
        } catch (e: Exception) {
            echo("   - Version: Not found on system PATH")
            echo("   - Status: Note (npm global install will not be available; use install.sh instead)")
        }
        echo("")

        // 3. Local Cache Directory
        val userHome = System.getProperty("user.home") ?: ""
        val cacheDir = File(userHome, ".library-insight-cache")
        echo("3. Local Download Cache:")
        echo("   - Path: ${cacheDir.absolutePath}")
        if (cacheDir.exists()) {
            val fileCount = cacheDir.walkBottomUp().filter { it.isFile }.count()
            echo("   - Cache files: $fileCount files cached")
        } else {
            echo("   - Cache files: Directory not created yet (will be created on first scan)")
        }
        echo("")

        // 4. Global AI Skills Registry Check
        echo("4. Global AI Agent Skill Configurations:")
        val paths = mapOf(
            "Cursor" to File(userHome, ".cursor/skills/library-insight/SKILL.md"),
            "Gemini Config" to File(userHome, ".gemini/config/skills/library-insight/SKILL.md"),
            "Claude Desktop" to File(userHome, ".claude/skills/library-insight/SKILL.md"),
            "Antigravity Agents" to File(userHome, ".agents/skills/library-insight/SKILL.md"),
            "GitHub Copilot" to File(userHome, ".copilot/skills/library-insight/SKILL.md"),
            "Junie Agent" to File(userHome, ".junie/skills/library-insight/SKILL.md"),
            "Codex AI" to File(userHome, ".codex/skills/library-insight/SKILL.md")
        )

        paths.forEach { (agent, file) ->
            val status = if (file.exists()) "INSTALLED (Verified)" else "NOT INSTALLED"
            echo(String.format("   %-22s : %s", "- $agent", status))
        }
        echo("")
        echo("==================================================")
        echo("Diagnostics completed.")
    }
}
