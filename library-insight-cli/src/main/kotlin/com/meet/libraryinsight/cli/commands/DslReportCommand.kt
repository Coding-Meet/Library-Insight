package com.meet.libraryinsight.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.meet.libraryinsight.cli.DatabaseHelper
import com.meet.libraryinsight.cli.DslReportGenerator
import java.io.File

class DslReportCommand : CliktCommand(
    name = "dsl-report",
    help = "Generate a Kotlin DSL surface report: type aliases, DSL scopes, extension functions, and lambda receivers."
) {
    private val db by option(
        "--db",
        help = "Index database JSON file path to read from"
    ).file().default(File("build/library-insight-index.json"))

    private val pkg by option(
        "--package", "-p",
        help = "Filter results to a specific package name (prefix match)"
    )

    override fun run() {
        val index = DatabaseHelper.loadIndex(db)
        if (index == null) {
            echo("Error: Index database file not found at ${db.absolutePath}. Please run 'scan' first.", err = true)
            return
        }

        echo(DslReportGenerator.generate(index, pkg))
    }
}
