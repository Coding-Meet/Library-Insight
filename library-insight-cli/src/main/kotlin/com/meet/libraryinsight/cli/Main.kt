package com.meet.libraryinsight.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.meet.libraryinsight.cli.commands.*

class LibraryInsightCommand : CliktCommand(
    name = "library-insight",
    help = "Library Insight: Inspect and index compiled Java/Kotlin library APIs (JAR/AAR)."
) {
    override fun run() = Unit
}

fun main(args: Array<String>) {
    LibraryInsightCommand()
        .subcommands(
            ScanCommand(),
            ExportCommand(),
            SearchCommand(),
            ExplainCommand(),
            DiffCommand(),
            AiExportCommand(),
            ClearCacheCommand()
        )
        .main(args)
}
