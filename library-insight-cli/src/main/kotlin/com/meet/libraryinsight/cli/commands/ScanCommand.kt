package com.meet.libraryinsight.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.meet.libraryinsight.cli.DatabaseHelper
import com.meet.libraryinsight.common.MavenResolver
import com.meet.libraryinsight.core.LibraryAnalyzer
import com.meet.libraryinsight.common.Logger
import java.io.File

class ScanCommand : CliktCommand(
    name = "scan",
    help = "Scan a JAR/AAR, local directory, or Maven coordinates (group:artifact:version)."
) {
    val pathOrCoordinate by argument(name = "path", help = "Path to AAR, JAR, directory, or Maven coordinate (e.g. com.aldebaran:qisdk:1.7.5)")
    
    val db by option(
        "--db",
        help = "Output index database JSON file path"
    ).file().default(File("build/library-insight-index.json"))

    val libName by option("--lib-name", help = "Name of the library (defaults to filename)")
    val libVersion by option("--lib-version", help = "Version of the library")
    val repos by option("--repo", help = "Additional Maven repository URLs to resolve coordinate artifacts").multiple()
    val sources by option("-s", "--sources", help = "Path to the sources JAR/directory (for local scans)").file(mustExist = true)

    override fun run() {
        Logger.info("ScanCommand started with path/coordinate: $pathOrCoordinate")
        try {
            val index = if (MavenResolver.isCoordinate(pathOrCoordinate)) {
                echo("Detected Maven coordinate: $pathOrCoordinate")
                val resolved = MavenResolver.resolve(pathOrCoordinate, repos) { progress ->
                    echo("  -> $progress")
                }
                val parts = pathOrCoordinate.split(':')
                val name = libName ?: parts[1]
                val version = libVersion ?: parts[2]
                Logger.info("Analyzing resolved binary file: ${resolved.binaryFile.absolutePath}")
                LibraryAnalyzer.analyze(resolved.binaryFile, name, version, resolved.sourcesFile)
            } else {
                val file = File(pathOrCoordinate)
                if (!file.exists()) {
                    val err = "Error: path '$pathOrCoordinate' does not exist."
                    echo(err, err = true)
                    Logger.warn(err)
                    if (!pathOrCoordinate.contains('/') && !pathOrCoordinate.contains('\\')) {
                        suggestCentralCoordinates(pathOrCoordinate)
                    }
                    return
                }
                echo("Scanning: ${file.absolutePath}")
                Logger.info("Analyzing local path: ${file.absolutePath}")
                val name = libName ?: file.nameWithoutExtension
                val version = libVersion ?: "1.0.0"
                LibraryAnalyzer.analyze(file, name, version, sources)
            }
            
            val classesCount = index.packages.flatMap { it.classes }.size
            echo("Scan complete! Found $classesCount classes across ${index.packages.size} packages.")
            Logger.info("Scan completed successfully for $pathOrCoordinate. Found $classesCount classes.")
            
            DatabaseHelper.saveIndex(index, db)
            echo("Saved API index to: ${db.absolutePath}", err = true)
            Logger.info("Saved index database to ${db.absolutePath}")
        } catch (e: Exception) {
            val errMsg = "Scan failed for '$pathOrCoordinate': ${e.message}"
            echo(errMsg, err = true)
            Logger.error(errMsg, e)
        }
    }

    private fun suggestCentralCoordinates(query: String) {
        val results = MavenResolver.searchCentral(query, rows = 3)
        if (results.isNotEmpty()) {
            echo("\nDid you mean one of these Maven Central coordinates?")
            for (result in results) {
                echo("  - ${result.coordinate}:${result.latestVersion}")
            }
            echo("Run: library-insight scan <coordinate>")
        }
    }
}
