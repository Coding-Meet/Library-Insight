package com.meet.libraryinsight.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.meet.libraryinsight.common.MavenResolver
import com.meet.libraryinsight.core.LibraryAnalyzer
import com.meet.libraryinsight.core.diff.DiffEngine
import java.io.File

class DiffCommand : CliktCommand(
    name = "diff",
    help = "Compare two versions of a library (supports local files and Maven coordinates) and check for API differences."
) {
    val oldLib by argument(help = "Path to old version JAR/AAR or Maven coordinate (groupId:artifactId:version)")
    val newLib by argument(help = "Path to new version JAR/AAR or Maven coordinate (groupId:artifactId:version)")

    override fun run() {
        val oldFile = resolveLib(oldLib, "old") ?: return
        val newFile = resolveLib(newLib, "new") ?: return

        echo("Analyzing old version: ${oldFile.name}")
        val oldIndex = LibraryAnalyzer.analyze(oldFile, libraryName = oldFile.nameWithoutExtension, version = "old")
        
        echo("Analyzing new version: ${newFile.name}")
        val newIndex = LibraryAnalyzer.analyze(newFile, libraryName = newFile.nameWithoutExtension, version = "new")

        echo("Diffing versions...")
        val report = DiffEngine.diff(oldIndex, newIndex)
        val formatted = DiffEngine.formatReport(report)
        echo(formatted)
    }

    private fun resolveLib(input: String, label: String): File? {
        return if (MavenResolver.isCoordinate(input)) {
            echo("Resolving Maven coordinate for $label version: $input")
            try {
                val resolved = MavenResolver.resolve(input) { progress ->
                    echo("  -> $progress")
                }
                resolved.binaryFile
            } catch (e: Exception) {
                echo("Error resolving coordinate '$input': ${e.message}", err = true)
                null
            }
        } else {
            val file = File(input)
            if (file.exists()) {
                file
            } else {
                // Search for the filename inside the local Maven cache directory
                val cachedFile = MavenResolver.cacheDir.walkBottomUp()
                    .filter { it.isFile }
                    .find { it.name == input }
                
                if (cachedFile != null) {
                    echo("Found cached file for $label version: ${cachedFile.absolutePath}")
                    cachedFile
                } else {
                    echo("Error: file '$input' ($label version) does not exist locally or in cache.", err = true)
                    null
                }
            }
        }
    }
}
