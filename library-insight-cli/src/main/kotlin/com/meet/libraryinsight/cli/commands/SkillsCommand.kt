package com.meet.libraryinsight.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import java.io.File

class SkillsCommand : CliktCommand(
    name = "skills",
    help = "Manage Library Insight AI agent skills for the current workspace."
) {
    init {
        subcommands(
            AddSkillCommand(),
            RemoveSkillCommand(),
            ListSkillsCommand()
        )
    }

    override fun run() = Unit
}

class AddSkillCommand : CliktCommand(
    name = "add",
    help = "Install the Library Insight AI agent skill into the current workspace (.agents/skills/)."
) {
    override fun run() {
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
        
        val statusMsg = if (isUpdate) "updated to latest version" else "installed successfully"
        echo("SUCCESS: AI Agent Skill $statusMsg at: ${destFile.absolutePath}")
    }
}

class RemoveSkillCommand : CliktCommand(
    name = "remove",
    help = "Remove the Library Insight AI agent skill from the current workspace."
) {
    override fun run() {
        val destDir = File(".agents/skills/library-insight")
        if (destDir.exists()) {
            destDir.deleteRecursively()
            echo("SUCCESS: AI Agent Skill removed successfully.")
        } else {
            echo("Note: AI Agent Skill is not installed in the current workspace.")
        }
    }
}

class ListSkillsCommand : CliktCommand(
    name = "list",
    help = "List installed Library Insight agent skills in the current workspace."
) {
    override fun run() {
        val destFile = File(".agents/skills/library-insight/SKILL.md")
        echo("Workspace AI Agent Skills:")
        if (destFile.exists()) {
            echo("  - [Installed] library-insight (${destFile.absolutePath})")
        } else {
            echo("  - [Not Installed] library-insight")
        }
    }
}
