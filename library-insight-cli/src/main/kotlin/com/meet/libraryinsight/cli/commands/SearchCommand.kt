package com.meet.libraryinsight.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.meet.libraryinsight.cli.DatabaseHelper
import com.meet.libraryinsight.search.SearchEngine
import java.io.File

class SearchCommand : CliktCommand(
    name = "search",
    help = "Search for a package, class, method, or property name in the index."
) {
    val query by argument(help = "Search query string (case-insensitive)")

    val db by option(
        "--db",
        help = "Index database JSON file path to read from"
    ).file().default(File(".library-insight-index.json"))

    override fun run() {
        val index = DatabaseHelper.loadIndex(db)
        if (index == null) {
            echo("Error: Index database file not found at ${db.absolutePath}. Please run 'scan' first.", err = true)
            return
        }

        val results = SearchEngine.search(index, query)
        if (results.isEmpty()) {
            echo("No matching symbols found for '$query'.")
            return
        }

        echo("Found ${results.size} matches for '$query':")
        echo("--------------------------------------------------")
        for (res in results) {
            val typePrefix = "[${res.type.name}]".padEnd(10)
            echo("$typePrefix ${res.details}")
        }
        echo("--------------------------------------------------")
    }
}
