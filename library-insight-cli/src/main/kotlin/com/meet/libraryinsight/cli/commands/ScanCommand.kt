package com.meet.libraryinsight.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.meet.libraryinsight.cli.DatabaseHelper
import com.meet.libraryinsight.common.LocalArtifacts
import com.meet.libraryinsight.common.MavenResolver
import com.meet.libraryinsight.core.LibraryAnalyzer
import com.meet.libraryinsight.common.Logger
import com.meet.libraryinsight.model.LibraryApiIndex
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
    val platform by option("-p", "--platform", help = "Target platform filter for BOM & KMP scans (android, jvm, ios, desktop, all). Defaults to android.").default("android")

    override fun run() {
        Logger.info("ScanCommand started with path/coordinate: $pathOrCoordinate (platform filter: $platform)")
        try {
            val index = if (MavenResolver.isCoordinate(pathOrCoordinate)) {
                echo("Detected Maven coordinate: $pathOrCoordinate")

                val parts = pathOrCoordinate.split(':')
                val name = libName ?: parts[1]
                val version = libVersion ?: parts[2]

                // 1. Check if coordinate is a BOM (Bill of Materials)
                val bomCoordinates = try {
                    MavenResolver.resolveBomCoordinates(pathOrCoordinate, repos) { progress ->
                        echo("  -> $progress")
                    }
                } catch (e: Exception) {
                    emptyList()
                }

                if (bomCoordinates.isNotEmpty()) {
                    val filteredBomCoords = MavenResolver.filterCoordinatesByPlatform(bomCoordinates, platform)
                    echo("Detected Bill of Materials (BOM) artifact containing ${bomCoordinates.size} managed libraries.")
                    if (filteredBomCoords.size < bomCoordinates.size) {
                        echo("Filtered to ${filteredBomCoords.size} libraries matching platform target '$platform' (use '--platform all' to scan all targets).")
                    }
                    val targetIndices = mutableListOf<LibraryApiIndex>()
                    val resolveErrors = mutableListOf<Pair<String, String>>()
                    for (targetCoord in filteredBomCoords) {
                        try {
                            echo("  -> Scanning BOM member: $targetCoord")
                            val resolvedTarget = MavenResolver.resolve(targetCoord, repos) { _ -> }
                            val targetParts = targetCoord.split(':')
                            val targetIndex = LibraryAnalyzer.analyze(
                                resolvedTarget.binaryFile,
                                targetParts[1],
                                targetParts[2],
                                resolvedTarget.sourcesFile
                            )
                            targetIndices.add(targetIndex)
                        } catch (e: Exception) {
                            resolveErrors.add(targetCoord to (e.message ?: "Unknown error"))
                        }
                    }
                    if (targetIndices.isNotEmpty()) {
                        echo("Successfully scanned and merged ${targetIndices.size} of ${filteredBomCoords.size} BOM member libraries.")
                        for (error in resolveErrors) {
                            Logger.info("Skipped BOM member ${error.first}: ${error.second}")
                        }
                        LibraryAnalyzer.mergeIndices(targetIndices).copy(
                            libraryName = name,
                            version = version
                        )
                    } else {
                        val resolved = MavenResolver.resolve(pathOrCoordinate, repos) { progress ->
                            echo("  -> $progress")
                        }
                        LibraryAnalyzer.analyze(resolved.binaryFile, name, version, resolved.sourcesFile)
                    }
                } else {
                    // 2. Check if KMP coordinate
                    val kmpCoordinates = try {
                        MavenResolver.resolveKmpCoordinates(pathOrCoordinate, repos) { progress ->
                            echo("  -> $progress")
                        }
                    } catch (e: Exception) {
                        emptyList()
                    }

                    if (kmpCoordinates.isNotEmpty() && kmpCoordinates != listOf(pathOrCoordinate)) {
                        val filteredKmpCoords = MavenResolver.filterCoordinatesByPlatform(kmpCoordinates, platform)
                        if (filteredKmpCoords.size < kmpCoordinates.size) {
                            echo("Filtered KMP variants to ${filteredKmpCoords.size} targets matching platform '$platform' (use '--platform all' for all targets).")
                        }
                        val targetIndices = mutableListOf<LibraryApiIndex>()
                        val resolveErrors = mutableListOf<Pair<String, String>>()
                        for (targetCoord in filteredKmpCoords) {
                            try {
                                val resolvedTarget = MavenResolver.resolve(targetCoord, repos) { _ -> }
                                val targetIndex = LibraryAnalyzer.analyze(
                                    resolvedTarget.binaryFile,
                                    name,
                                    version,
                                    resolvedTarget.sourcesFile
                                )
                                targetIndices.add(targetIndex)
                            } catch (e: Exception) {
                                resolveErrors.add(targetCoord to (e.message ?: "Unknown error"))
                            }
                        }
                        if (targetIndices.isNotEmpty()) {
                            echo("Detected Kotlin Multiplatform (KMP) library. Successfully resolved ${targetIndices.size} of ${filteredKmpCoords.size} platform targets:")
                            for (targetIndex in targetIndices) {
                                Logger.info("Resolved KMP variant: ${targetIndex.libraryName}")
                            }
                            for (error in resolveErrors) {
                                Logger.info("Failed to resolve KMP variant ${error.first}: ${error.second}")
                            }
                            LibraryAnalyzer.mergeIndices(targetIndices)
                        } else {
                            val resolved = MavenResolver.resolve(pathOrCoordinate, repos) { progress ->
                                echo("  -> $progress")
                            }
                            LibraryAnalyzer.analyze(resolved.binaryFile, name, version, resolved.sourcesFile)
                        }
                    } else {
                        val resolved = MavenResolver.resolve(pathOrCoordinate, repos) { progress ->
                            echo("  -> $progress")
                        }
                        Logger.info("Analyzing resolved binary file: ${resolved.binaryFile.absolutePath}")
                        LibraryAnalyzer.analyze(resolved.binaryFile, name, version, resolved.sourcesFile)
                    }
                }
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
                val parsed = LocalArtifacts.parseNameAndVersion(file.nameWithoutExtension)
                val name = libName ?: parsed.name
                val version = libVersion ?: parsed.version ?: "1.0.0"
                val sourcesFile = sources ?: LocalArtifacts.findSiblingSources(file)?.also {
                    echo("  -> Using sibling sources archive: ${it.name}")
                }
                LibraryAnalyzer.analyze(file, name, version, sourcesFile)
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
