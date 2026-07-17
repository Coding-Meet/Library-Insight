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
        if (skillContent == null) {
            echo("Error: Custom AI Skill resource file not found inside the CLI binary.", err = true)
            return
        }

        val destDir = File(".agents/skills/library-insight")
        destDir.mkdirs()
        val destFile = File(destDir, "SKILL.md")
        destFile.writeText(skillContent)
        
        echo("SUCCESS: AI Agent Skill initialized at: ${destFile.absolutePath}")
    }
}
