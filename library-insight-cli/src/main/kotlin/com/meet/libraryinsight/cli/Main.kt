package com.meet.libraryinsight.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import com.meet.libraryinsight.core.LibraryAnalyzer
import com.meet.libraryinsight.core.diff.DiffEngine
import com.meet.libraryinsight.export.AiExporter
import com.meet.libraryinsight.export.JsonExporter
import com.meet.libraryinsight.export.MarkdownExporter
import com.meet.libraryinsight.model.LibraryApiIndex
import com.meet.libraryinsight.search.SearchEngine
import java.io.File

class LibraryInsightCommand : CliktCommand(
    name = "library-insight",
    help = "Library Insight: Inspect and index compiled Java/Kotlin library APIs (JAR/AAR)."
) {
    override fun run() = Unit
}

class ScanCommand : CliktCommand(
    name = "scan",
    help = "Scan a JAR/AAR or directory of libraries to generate an API index."
) {
    val path by argument(help = "Path to AAR, JAR, or directory").file(mustExist = true)
    
    val db by option(
        "--db",
        help = "Output index database JSON file path"
    ).file().default(File(".library-insight-index.json"))

    val libName by option("--lib-name", help = "Name of the library (defaults to filename)")
    val libVersion by option("--lib-version", help = "Version of the library").default("1.0.0")
    val sources by option("-s", "--sources", help = "Path to the sources JAR/directory").file(mustExist = true)

    override fun run() {
        echo("Scanning: ${path.absolutePath}")
        val name = libName ?: path.nameWithoutExtension
        val index = LibraryAnalyzer.analyze(path, name, libVersion, sourcesFile = sources)
        
        val classesCount = index.packages.flatMap { it.classes }.size
        echo("Scan complete! Found $classesCount classes across ${index.packages.size} packages.")
        
        DatabaseHelper.saveIndex(index, db)
        echo("Saved API index to: ${db.absolutePath}")
    }
}

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

class ExplainCommand : CliktCommand(
    name = "explain",
    help = "Print detailed API information about a specific class name."
) {
    val className by argument(help = "Fully qualified or simple name of the class")

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

        // Find the class matching the given name (FQCN or simple name)
        val allClasses = index.packages.flatMap { it.classes }
        val clazz = allClasses.firstOrNull { it.name == className || it.simpleName == className }

        if (clazz == null) {
            echo("Error: Class '$className' not found in the index.", err = true)
            return
        }

        val pkgName = index.packages.first { it.classes.contains(clazz) }.name

        echo("==================================================")
        echo(" CLASS EXPLAIN REPORT")
        echo("==================================================")
        echo("Class:       ${clazz.name}")
        echo("Package:     $pkgName")
        echo("Kind:        ${clazz.kind.name.lowercase()}")
        echo("Visibility:  ${clazz.visibility.name.lowercase()}")
        if (clazz.modifiers.isNotEmpty()) {
            echo("Modifiers:   ${clazz.modifiers.joinToString(", ")}")
        }
        if (clazz.superTypes.isNotEmpty()) {
            echo("Supertypes:  ${clazz.superTypes.joinToString(", ")}")
        }
        if (clazz.annotations.isNotEmpty()) {
            echo("Annotations: ${clazz.annotations.joinToString { "@" + it.name.substringAfterLast('.') }}")
        }
        val classDoc = clazz.doc
        if (classDoc != null) {
            echo("--------------------------------------------------")
            echo("Documentation:\n${classDoc.trim().lines().joinToString("\n") { "  * $it" }}")
        }
        echo("==================================================\n")

        if (clazz.constructors.isNotEmpty()) {
            echo("Constructors:")
            for (cons in clazz.constructors) {
                val params = cons.parameters.joinToString { "${it.name}: ${it.type}" }
                echo("  - ${cons.visibility.name.lowercase()} constructor($params)")
            }
            echo("")
        }

        if (clazz.properties.isNotEmpty()) {
            echo("Properties:")
            for (prop in clazz.properties) {
                val propDoc = prop.doc
                if (propDoc != null) {
                    echo("  // ${propDoc.trim().replace("\n", "\n  // ")}")
                }
                val mut = if (prop.isMutable) "var" else "val"
                val constStr = if (prop.isConst) "const " else ""
                echo("  - ${prop.visibility.name.lowercase()} ${constStr}$mut ${prop.name}: ${prop.type}")
            }
            echo("")
        }

        if (clazz.methods.isNotEmpty()) {
            echo("Methods:")
            for (method in clazz.methods) {
                val methodDoc = method.doc
                if (methodDoc != null) {
                    echo("  // ${methodDoc.trim().replace("\n", "\n  // ")}")
                }
                val methodMods = mutableListOf<String>()
                if (method.flags.isSuspend) methodMods.add("suspend")
                if (method.flags.isInline) methodMods.add("inline")
                if (method.flags.isOperator) methodMods.add("operator")
                if (method.flags.isInfix) methodMods.add("infix")
                if (method.flags.isStatic) methodMods.add("static")
                val modsStr = if (methodMods.isNotEmpty()) methodMods.joinToString(" ") + " " else ""
                val params = method.parameters.joinToString { "${it.name}: ${it.type}" }
                val receiver = if (method.extensionReceiverType != null) "${method.extensionReceiverType}." else ""
                echo("  - ${method.visibility.name.lowercase()} ${modsStr}fun $receiver${method.name}($params): ${method.returnType}")
            }
            echo("")
        }

        if (clazz.nestedClasses.isNotEmpty()) {
            echo("Nested Classes:")
            for (nested in clazz.nestedClasses) {
                echo("  - ${nested.substringAfterLast('$')}")
            }
            echo("")
        }
    }
}

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

class AiExportCommand : CliktCommand(
    name = "ai-export",
    help = "Generate a compact, token-efficient context file (ai-context.json) for LLMs."
) {
    val outputFile by argument(help = "Output file path").file().optional()

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

        val file = outputFile ?: File("ai-context.json")
        val aiContextJson = AiExporter.export(index)
        file.writeText(aiContextJson)
        echo("Generated compact LLM context file: ${file.absolutePath}")
    }
}

fun main(args: Array<String>) {
    LibraryInsightCommand()
        .subcommands(
            ScanCommand(),
            ExportCommand(),
            SearchCommand(),
            ExplainCommand(),
            DiffCommand(),
            AiExportCommand()
        )
        .main(args)
}
