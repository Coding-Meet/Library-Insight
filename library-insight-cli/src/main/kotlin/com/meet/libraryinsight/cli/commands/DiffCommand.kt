package com.meet.libraryinsight.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import com.meet.libraryinsight.core.LibraryAnalyzer
import com.meet.libraryinsight.core.diff.DiffEngine
import java.io.File

class DiffCommand : CliktCommand(
    name = "diff",
    help = "Compare two versions of a library and check for API differences."
) {
    val oldLib by argument(help = "Path to old version JAR/AAR").file(mustExist = true)
    val newLib by argument(help = "Path to new version JAR/AAR").file(mustExist = true)

    override fun run() {
        echo("Analyzing old version: ${oldLib.name}")
        val oldIndex = LibraryAnalyzer.analyze(oldLib, libraryName = oldLib.nameWithoutExtension, version = "old")
        
        echo("Analyzing new version: ${newLib.name}")
        val newIndex = LibraryAnalyzer.analyze(newLib, libraryName = newLib.nameWithoutExtension, version = "new")

        echo("Diffing versions...")
        val report = DiffEngine.diff(oldIndex, newIndex)
        val formatted = DiffEngine.formatReport(report)
        echo(formatted)
    }
}
