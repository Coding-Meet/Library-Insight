package com.meet.libraryinsight.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.meet.libraryinsight.common.MavenResolver

class SearchCentralCommand : CliktCommand(
    name = "search-central",
    help = "Search Maven Central for packages and coordinates matching the query."
) {
    val query by argument(help = "Search query (e.g. retrofit)")

    override fun run() {
        echo("Searching Maven Central for '$query'...")
        val results = MavenResolver.searchCentral(query)

        if (results.isEmpty()) {
            echo("No matching libraries found on Maven Central.")
            return
        }

        echo("\nFound ${results.size} matching libraries on Maven Central:\n")
        for (result in results) {
            echo("📦 Coordinate: ${result.coordinate}:${result.latestVersion}")
            echo("   Repository: ${result.repository}")
            echo("   Group:      ${result.groupId}")
            echo("   Artifact:   ${result.artifactId}")
            echo("--------------------------------------------------")
        }
    }
}
