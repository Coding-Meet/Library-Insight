package com.meet.libraryinsight.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.meet.libraryinsight.cli.commands.*
import com.meet.libraryinsight.common.Logger

import com.github.ajalt.clikt.parameters.options.versionOption

private fun getVersion(): String {
    val properties = java.util.Properties()
    val stream = LibraryInsightCommand::class.java.getResourceAsStream("/version.properties")
        ?: error("version.properties not found in resources")
    properties.load(stream)
    return properties.getProperty("version") ?: error("version key not found in version.properties")
}

class LibraryInsightCommand : CliktCommand(
    name = "library-insight",
    help = "Library Insight: API Explorer & MCP Server for Java, Kotlin & KMP — accurate library APIs for AI IDEs."
) {
    init {
        versionOption(getVersion(), names = setOf("-v", "--version"))
    }
    override fun run() = Unit
}

fun main(args: Array<String>) {
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn")
    Logger.info("Library-Insight CLI invocation with args: ${args.joinToString(" ")}")
    try {
        LibraryInsightCommand()
            .subcommands(
                ScanCommand(),
                ScanSourceCommand(),
                ExportCommand(),
                SearchCommand(),
                ExplainCommand(),
                DiffCommand(),
                AiExportCommand(),
                ClearCacheCommand(),
                InitCommand(),
                SkillsCommand(),
                DoctorCommand(),
                McpCommand(),
                AuditCommand(),
                MigrateCommand(),
                SearchCentralCommand(),
                GraphCommand(),
                CheckCompatCommand(),
                DslReportCommand(),
                ExamplesCommand(),
                HealthCommand(),
                DependencyCheckCommand(),
                CallGraphCommand(),
                UpdateCommand()
            )
            .main(args)
        Logger.info("Library-Insight CLI completed successfully")
    } catch (e: Exception) {
        Logger.error("Uncaught exception in CLI main", e)
        throw e
    }
}
