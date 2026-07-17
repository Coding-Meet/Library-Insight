package com.meet.libraryinsight.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.meet.libraryinsight.cli.DatabaseHelper
import com.meet.libraryinsight.export.AiExporter
import java.io.File

class AiExportCommand : CliktCommand(
    name = "ai-export",
    help = "Generate a compact, token-efficient split context folder structure (build/ai-context/) for LLMs."
) {
    val outputDir by argument(help = "Output directory path").file().optional()

    val db by option(
        "--db",
        help = "Index database JSON file path to read from"
    ).file().default(File("build/library-insight-index.json"))

    override fun run() {
        val index = DatabaseHelper.loadIndex(db)
        if (index == null) {
            echo("Error: Index database file not found at ${db.absolutePath}. Please run 'scan' first.", err = true)
            return
        }

        val dir = outputDir ?: File("build/ai-context")
        AiExporter.exportSplit(index, dir)
        echo("Generated compact LLM context directory structure at: ${dir.absolutePath}")
    }
}
