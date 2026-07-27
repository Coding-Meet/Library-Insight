package com.meet.libraryinsight.cli

import com.meet.libraryinsight.model.LibraryApiIndex
import com.meet.libraryinsight.model.MethodApi
import com.meet.libraryinsight.model.Visibility

/**
 * Builds the Kotlin DSL surface report shared by the `dsl-report` CLI command
 * and the `dsl_report` MCP tool.
 */
object DslReportGenerator {

    private const val SEP = "=================================================="

    fun generate(index: LibraryApiIndex, packageFilter: String? = null): String = buildString {
        val filteredPackages = if (packageFilter != null) {
            index.packages.filter { it.name.startsWith(packageFilter) }
        } else {
            index.packages
        }

        val allClasses = filteredPackages.flatMap { it.classes }
        val allMethods = allClasses.flatMap { clazz ->
            clazz.methods.map { method -> clazz to method }
        }

        appendLine(SEP)
        appendLine("  DSL SURFACE REPORT  —  ${index.libraryName} ${index.version}")
        if (packageFilter != null) appendLine("  Package filter: $packageFilter")
        appendLine(SEP)

        // ── 1. Type Aliases ──────────────────────────────────────
        val allTypeAliases = filteredPackages.flatMap { it.typeAliases }
        appendLine("\n▶ Type Aliases (${allTypeAliases.size})")
        if (allTypeAliases.isEmpty()) {
            appendLine("  (none found)")
        } else {
            for (alias in allTypeAliases.sortedBy { it.name }) {
                val typeParams = if (alias.typeParameters.isNotEmpty()) {
                    "<${alias.typeParameters.joinToString(", ") { it.name }}>"
                } else ""
                appendLine("  typealias ${alias.name}$typeParams = ${alias.expandedType}")
            }
        }

        // ── 2. DSL Scopes (@DslMarker) ───────────────────────────
        val dslScopeClasses = allClasses.filter { it.dslMarkerAnnotations.isNotEmpty() }
        val dslScopesByMarker = dslScopeClasses
            .flatMap { clazz -> clazz.dslMarkerAnnotations.map { marker -> marker to clazz.simpleName } }
            .groupBy({ it.first }, { it.second })

        appendLine("\n▶ DSL Scopes — @DslMarker annotated classes (${dslScopeClasses.size})")
        if (dslScopesByMarker.isEmpty()) {
            appendLine("  (none found)")
        } else {
            for ((marker, classes) in dslScopesByMarker.entries.sortedBy { it.key }) {
                appendLine("  @$marker → ${classes.sorted().joinToString(", ")}")
            }
        }

        // ── 3. Extension Functions ───────────────────────────────
        val extensionFunctions = allMethods.filter { (_, method) ->
            method.extensionReceiverType != null
        }
        appendLine("\n▶ Extension Functions (${extensionFunctions.size})")
        if (extensionFunctions.isEmpty()) {
            appendLine("  (none found)")
        } else {
            for ((_, method) in extensionFunctions.sortedBy { it.second.extensionReceiverType }) {
                appendLine("  ${formatExtensionMethod(method)}")
            }
        }

        // ── 4. Lambda Receivers (DSL builder functions) ──────────
        val lambdaReceiverFunctions = allMethods.filter { (_, method) ->
            method.parameters.any { it.isLambdaReceiver }
        }
        appendLine("\n▶ Lambda-with-Receiver Parameters — DSL builder functions (${lambdaReceiverFunctions.size})")
        if (lambdaReceiverFunctions.isEmpty()) {
            appendLine("  (none found)")
        } else {
            for ((_, method) in lambdaReceiverFunctions.sortedBy { (_, m) -> m.name }) {
                val params = method.parameters.joinToString { param ->
                    if (param.isLambdaReceiver) "[${param.name}: ${param.type}]" else "${param.name}: ${param.type}"
                }
                val receiver = if (method.extensionReceiverType != null) "${method.extensionReceiverType}." else ""
                appendLine("  fun $receiver${method.name}($params): ${method.returnType}")
            }
        }

        // ── 5. Inline Reified Functions ──────────────────────────
        val reifiedFunctions = allMethods.filter { (_, method) ->
            method.flags.isInline && method.typeParameters.any { it.isReified }
        }
        appendLine("\n▶ Inline Reified Functions (${reifiedFunctions.size})")
        if (reifiedFunctions.isEmpty()) {
            appendLine("  (none found)")
        } else {
            for ((_, method) in reifiedFunctions.sortedBy { (_, m) -> m.name }) {
                val typeParams = method.typeParameters.joinToString(", ") { param ->
                    val reifiedPrefix = if (param.isReified) "reified " else ""
                    "$reifiedPrefix${param.name}"
                }
                val params = method.parameters.joinToString { "${it.name}: ${it.type}" }
                val receiver = if (method.extensionReceiverType != null) "${method.extensionReceiverType}." else ""
                appendLine("  inline fun <$typeParams> $receiver${method.name}($params): ${method.returnType}")
            }
        }

        // ── 6. Fluent / Builder APIs ──────────────────────────────
        val fluentClasses = allClasses.filter { clazz ->
            val fluentMethods = clazz.methods.filter { method ->
                method.visibility == Visibility.PUBLIC &&
                (method.returnType == clazz.name || method.returnType == clazz.simpleName ||
                 method.returnType.endsWith(".${clazz.simpleName}") || method.returnType.contains("Builder"))
            }
            fluentMethods.size >= 2
        }
        appendLine("\n▶ Fluent / Builder APIs (${fluentClasses.size})")
        if (fluentClasses.isEmpty()) {
            appendLine("  (none found)")
        } else {
            for (clazz in fluentClasses.sortedBy { it.simpleName }) {
                appendLine("  class ${clazz.name}  → provides fluent builder methods")
            }
        }

        appendLine("\n$SEP")
        appendLine("  Tip: run 'explain <ClassName>' for full API details on any class above.")
        appendLine(SEP)
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
