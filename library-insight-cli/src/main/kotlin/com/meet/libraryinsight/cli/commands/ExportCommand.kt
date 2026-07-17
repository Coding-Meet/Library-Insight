package com.meet.libraryinsight.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import com.meet.libraryinsight.cli.DatabaseHelper
import com.meet.libraryinsight.export.JsonExporter
import com.meet.libraryinsight.export.MarkdownExporter
import java.io.File

enum class ExportFormat {
    JSON, MARKDOWN
}

class ExportCommand : CliktCommand(
    name = "export",
    help = "Export the saved library index to JSON or Markdown format."
) {
    val format by argument(help = "Output format").enum<ExportFormat>()
    val outputFile by argument(help = "Output file path (prints to stdout if omitted)").file().optional()

    val db by option(
        "--db",
        help = "Index database JSON file path to read from"
    ).file().default(File(".library-insight-index.json"))

    override fun run() {
        val index = DatabaseHelper.loadIndex(db)
        if (index == null) {
            echo("Error: Index database file not found or corrupted at ${db.absolutePath}. Please run 'scan' first.", err = true)
            return
        }

        val outputContent = when (format) {
            ExportFormat.JSON -> JsonExporter.export(index)
            ExportFormat.MARKDOWN -> MarkdownExporter.export(index)
        }

        val file = outputFile
        if (file != null) {
            file.writeText(outputContent)
            echo("Exported ${format} to: ${file.absolutePath}")
        } else {
            echo(outputContent)
        }
    }
}
