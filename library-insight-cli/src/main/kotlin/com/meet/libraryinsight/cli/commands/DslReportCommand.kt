package com.meet.libraryinsight.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.meet.libraryinsight.cli.DatabaseHelper
import com.meet.libraryinsight.model.MethodApi
import java.io.File

class DslReportCommand : CliktCommand(
    name = "dsl-report",
    help = "Generate a Kotlin DSL surface report: type aliases, DSL scopes, extension functions, and lambda receivers."
) {
    private val db by option(
        "--db",
        help = "Index database JSON file path to read from"
    ).file().default(File("build/library-insight-index.json"))

    private val pkg by option(
        "--package", "-p",
        help = "Filter results to a specific package name (prefix match)"
    )

    override fun run() {
        val index = DatabaseHelper.loadIndex(db)
        if (index == null) {
            echo("Error: Index database file not found at ${db.absolutePath}. Please run 'scan' first.", err = true)
            return
        }

        val SEP = "=================================================="
        val filterPkg = pkg

        val filteredPackages = if (filterPkg != null) {
            index.packages.filter { it.name.startsWith(filterPkg) }
        } else {
            index.packages
        }

        val allClasses = filteredPackages.flatMap { it.classes }
        val allMethods = allClasses.flatMap { clazz ->
            clazz.methods.map { method -> clazz to method }
        }

        echo(SEP)
        echo("  DSL SURFACE REPORT  —  ${index.libraryName} ${index.version}")
        if (filterPkg != null) echo("  Package filter: $filterPkg")
        echo(SEP)

        // ── 1. Type Aliases ──────────────────────────────────────
        val allTypeAliases = filteredPackages.flatMap { it.typeAliases }
        echo("\n▶ Type Aliases (${allTypeAliases.size})")
        if (allTypeAliases.isEmpty()) {
            echo("  (none found)")
        } else {
            for (alias in allTypeAliases.sortedBy { it.name }) {
                val typeParams = if (alias.typeParameters.isNotEmpty()) {
                    "<${alias.typeParameters.joinToString(", ") { it.name }}>"
                } else ""
                echo("  typealias ${alias.name}$typeParams = ${alias.expandedType}")
            }
        }

        // ── 2. DSL Scopes (@DslMarker) ───────────────────────────
        val dslScopeClasses = allClasses.filter { it.dslMarkerAnnotations.isNotEmpty() }
        val dslScopesByMarker = dslScopeClasses
            .flatMap { clazz -> clazz.dslMarkerAnnotations.map { marker -> marker to clazz.simpleName } }
            .groupBy({ it.first }, { it.second })

        echo("\n▶ DSL Scopes — @DslMarker annotated classes (${dslScopeClasses.size})")
        if (dslScopesByMarker.isEmpty()) {
            echo("  (none found)")
        } else {
            for ((marker, classes) in dslScopesByMarker.entries.sortedBy { it.key }) {
                echo("  @$marker → ${classes.sorted().joinToString(", ")}")
            }
        }

        // ── 3. Extension Functions ───────────────────────────────
        val extensionFunctions = allMethods.filter { (_, method) ->
            method.extensionReceiverType != null
        }
        echo("\n▶ Extension Functions (${extensionFunctions.size})")
        if (extensionFunctions.isEmpty()) {
            echo("  (none found)")
        } else {
            for ((_, method) in extensionFunctions.sortedBy { it.second.extensionReceiverType }) {
                echo("  ${formatExtensionMethod(method)}")
            }
        }

        // ── 4. Lambda Receivers (DSL builder functions) ──────────
        val lambdaReceiverFunctions = allMethods.filter { (_, method) ->
            method.parameters.any { it.isLambdaReceiver }
        }
        echo("\n▶ Lambda-with-Receiver Parameters — DSL builder functions (${lambdaReceiverFunctions.size})")
        if (lambdaReceiverFunctions.isEmpty()) {
            echo("  (none found)")
        } else {
            for ((_, method) in lambdaReceiverFunctions.sortedBy { (_, m) -> m.name }) {
                val params = method.parameters.joinToString { param ->
                    if (param.isLambdaReceiver) "[${param.name}: ${param.type}]" else "${param.name}: ${param.type}"
                }
                val receiver = if (method.extensionReceiverType != null) "${method.extensionReceiverType}." else ""
                echo("  fun $receiver${method.name}($params): ${method.returnType}")
            }
        }

        // ── 5. Inline Reified Functions ──────────────────────────
        val reifiedFunctions = allMethods.filter { (_, method) ->
            method.flags.isInline && method.typeParameters.any { it.isReified }
        }
        echo("\n▶ Inline Reified Functions (${reifiedFunctions.size})")
        if (reifiedFunctions.isEmpty()) {
            echo("  (none found)")
        } else {
            for ((_, method) in reifiedFunctions.sortedBy { (_, m) -> m.name }) {
                val typeParams = method.typeParameters.joinToString(", ") { param ->
                    val reifiedPrefix = if (param.isReified) "reified " else ""
                    "$reifiedPrefix${param.name}"
                }
                val params = method.parameters.joinToString { "${it.name}: ${it.type}" }
                val receiver = if (method.extensionReceiverType != null) "${method.extensionReceiverType}." else ""
                echo("  inline fun <$typeParams> $receiver${method.name}($params): ${method.returnType}")
            }
        }

        // ── 6. Fluent / Builder APIs ──────────────────────────────
        val fluentClasses = allClasses.filter { clazz ->
            val fluentMethods = clazz.methods.filter { method ->
                method.visibility == com.meet.libraryinsight.model.Visibility.PUBLIC &&
                (method.returnType == clazz.name || method.returnType == clazz.simpleName ||
                 method.returnType.endsWith(".${clazz.simpleName}") || method.returnType.contains("Builder"))
            }
            fluentMethods.size >= 2
        }
        echo("\n▶ Fluent / Builder APIs (${fluentClasses.size})")
        if (fluentClasses.isEmpty()) {
            echo("  (none found)")
        } else {
            for (clazz in fluentClasses.sortedBy { it.simpleName }) {
                echo("  class ${clazz.name}  → provides fluent builder methods")
            }
        }

        echo("\n$SEP")
        echo("  Tip: run 'explain <ClassName>' for full API details on any class above.")
        echo(SEP)
    }

    private fun formatExtensionMethod(method: MethodApi): String {
        val mods = buildString {
            if (method.flags.isSuspend) append("suspend ")
            if (method.flags.isInline) append("inline ")
            if (method.flags.isInfix) append("infix ")
            if (method.flags.isOperator) append("operator ")
        }
        val typeParams = if (method.typeParameters.isNotEmpty()) {
            "<${method.typeParameters.joinToString(", ") { param ->
                val reifiedPrefix = if (param.isReified) "reified " else ""
                "$reifiedPrefix${param.name}"
            }}> "
        } else ""
        val params = method.parameters.joinToString { "${it.name}: ${it.type}" }
        return "${mods}fun $typeParams${method.extensionReceiverType}.${method.name}($params): ${method.returnType}"
    }
}
