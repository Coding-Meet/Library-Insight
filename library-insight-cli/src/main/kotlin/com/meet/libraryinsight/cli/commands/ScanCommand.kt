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
import java.io.File

class ScanCommand : CliktCommand(
    name = "scan",
    help = "Scan a JAR/AAR, local directory, or Maven coordinates (group:artifact:version)."
) {
    val pathOrCoordinate by argument(name = "path", help = "Path to AAR, JAR, directory, or Maven coordinate (e.g. com.aldebaran:qisdk:1.7.5)")
    
    val db by option(
        "--db",
        help = "Output index database JSON file path"
    ).file().default(File(".library-insight-index.json"))

    val libName by option("--lib-name", help = "Name of the library (defaults to filename)")
    val libVersion by option("--lib-version", help = "Version of the library")
    val repos by option("--repo", help = "Additional Maven repository URLs to resolve coordinate artifacts").multiple()
    val sources by option("-s", "--sources", help = "Path to the sources JAR/directory (for local scans)").file(mustExist = true)

    override fun run() {
        val index = if (MavenResolver.isCoordinate(pathOrCoordinate)) {
            echo("Detected Maven coordinate: $pathOrCoordinate")
            val resolved = MavenResolver.resolve(pathOrCoordinate, repos) { progress ->
                echo("  -> $progress")
            }
            val parts = pathOrCoordinate.split(':')
            val name = libName ?: parts[1]
            val version = libVersion ?: parts[2]
            LibraryAnalyzer.analyze(resolved.binaryFile, name, version, resolved.sourcesFile)
        } else {
            val file = File(pathOrCoordinate)
            if (!file.exists()) {
                echo("Error: path '$pathOrCoordinate' does not exist.", err = true)
                return
            }
            echo("Scanning: ${file.absolutePath}")
            val name = libName ?: file.nameWithoutExtension
            val version = libVersion ?: "1.0.0"
            LibraryAnalyzer.analyze(file, name, version, sources)
        }
        
        val classesCount = index.packages.flatMap { it.classes }.size
        echo("Scan complete! Found $classesCount classes across ${index.packages.size} packages.")
        
        DatabaseHelper.saveIndex(index, db)
        echo("Saved API index to: ${db.absolutePath}")
    }
}
