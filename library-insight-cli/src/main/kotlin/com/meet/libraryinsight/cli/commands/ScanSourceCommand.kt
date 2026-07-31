package com.meet.libraryinsight.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.meet.libraryinsight.cli.DatabaseHelper
import com.meet.libraryinsight.parser.SourceParser
import java.io.File

class ScanSourceCommand : CliktCommand(
    name = "scan-source",
    help = "Scan a local raw source directory containing Kotlin (.kt) and Java (.java) files to build an API index."
) {
    val sourceDir by argument(help = "Path to the local source directory to scan")

    val db by option(
        "--db",
        help = "Output index database JSON file path"
    ).file().default(File("build/library-insight-index.json"))

    val libName by option("--lib-name", help = "Name of the library (defaults to source directory name)")
    val libVersion by option("--lib-version", help = "Version of the library")

    override fun run() {
        val srcDir = File(sourceDir)
        if (!srcDir.exists() || !srcDir.isDirectory) {
            echo("Error: Source directory '$sourceDir' does not exist or is not a directory.", err = true)
            return
        }

        val name = libName ?: srcDir.name
        val version = libVersion ?: "1.0.0"

        var ktFilesCount = 0
        var javaFilesCount = 0
        srcDir.walkTopDown().forEach { file ->
            if (file.isFile) {
                if (file.name.endsWith(".kt")) ktFilesCount++
                if (file.name.endsWith(".java")) javaFilesCount++
            }
        }

        echo("Scanning source directory: ${srcDir.absolutePath}")
        echo("")
        echo("Detected:")
        echo("  • Kotlin files : $ktFilesCount")
        echo("  • Java files   : $javaFilesCount")
        echo("")

        try {
            val index = SourceParser.parseDirectory(srcDir, name, version)
            
            val classCount = index.packages.flatMap { it.classes }.size
            val packageCount = index.packages.size
            
            DatabaseHelper.saveIndex(index, db)
            
            echo("Scan complete!")
            echo("Found $classCount classes across $packageCount packages.")
            echo("")
            echo("Saved API index to:")
            echo("${db.absolutePath}")
        } catch (e: Exception) {
            echo("Error scanning source directory: ${e.message}", err = true)
        }
    }
}
