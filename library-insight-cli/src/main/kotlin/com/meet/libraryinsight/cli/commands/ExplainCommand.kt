package com.meet.libraryinsight.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.meet.libraryinsight.cli.DatabaseHelper
import java.io.File

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
