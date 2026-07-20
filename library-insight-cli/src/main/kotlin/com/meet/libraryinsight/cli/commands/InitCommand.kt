package com.meet.libraryinsight.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import java.io.File

class InitCommand : CliktCommand(
    name = "init",
    help = "Initialize the current directory with the Library Insight AI agent skill."
) {
    override fun run() {
        echo("Initializing Library Insight agent environment...")
        val skillContent = javaClass.getResourceAsStream("/SKILL.md")?.bufferedReader()?.use { it.readText() }
        val scriptContent = javaClass.getResourceAsStream("/scripts/install-cli.sh")?.bufferedReader()?.use { it.readText() }
        
        if (skillContent == null || scriptContent == null) {
            echo("Error: Custom AI Skill resource files not found inside the CLI binary.", err = true)
            return
        }

        val destDir = File(".agents/skills/library-insight")
        val isUpdate = File(destDir, "SKILL.md").exists()
        destDir.mkdirs()
        
        // Write SKILL.md (always overwrites with latest bundled version)
        val destFile = File(destDir, "SKILL.md")
        destFile.writeText(skillContent)
        
        // Write scripts/install-cli.sh (always overwrites with latest bundled version)
        val destScriptFile = File(destDir, "scripts/install-cli.sh")
        destScriptFile.parentFile.mkdirs()
        destScriptFile.writeText(scriptContent)
        destScriptFile.setExecutable(true, false)
        
        val statusMsg = if (isUpdate) "updated to latest version" else "initialized"
        echo("SUCCESS: AI Agent Skill $statusMsg at: ${destFile.absolutePath}")
    }
}
