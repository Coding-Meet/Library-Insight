package com.meet.libraryinsight.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.meet.libraryinsight.cli.commands.*
import com.meet.libraryinsight.common.Logger

class LibraryInsightCommand : CliktCommand(
    name = "library-insight",
    help = "Library Insight: JVM API Explorer & MCP Server — accurate library APIs for AI IDEs."
) {
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
                CallGraphCommand()
            )
            .main(args)
        Logger.info("Library-Insight CLI completed successfully")
    } catch (e: Exception) {
        Logger.error("Uncaught exception in CLI main", e)
        throw e
    }
}
