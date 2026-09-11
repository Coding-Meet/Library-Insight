package com.meet.libraryinsight.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.meet.libraryinsight.cli.DatabaseHelper
import com.meet.libraryinsight.model.ClassApi
import com.meet.libraryinsight.model.LibraryApiIndex
import java.io.File

class ExplainCommand : CliktCommand(
    name = "explain",
    help = "Print detailed API information about a specific class name."
) {
    val className by argument(help = "Fully qualified or simple name of the class")

    val db by option(
        "--db",
        help = "Index database JSON file path to read from"
    ).file().default(File("build/library-insight-index.json"))

    val deep by option(
        "-d", "--deep",
        help = "Recursively explain referenced parameter types, DSL receiver scopes, and return types."
    ).flag(default = false)

    override fun run() {
        val index = DatabaseHelper.loadIndex(db)
        if (index == null) {
            echo("Error: Index database file not found at ${db.absolutePath}. Please run 'scan' first.", err = true)
            return
        }

        // Find the class matching the given name (FQCN or simple name)
        val allClasses = index.packages.flatMap { it.classes }

        // 1. Exact match (case-sensitive or FQCN)
        var clazz: ClassApi? = allClasses.firstOrNull { it.name == className || it.simpleName == className }

        // 2. Case-insensitive exact match
        if (clazz == null) {
            clazz = allClasses.firstOrNull {
                it.name.equals(className, ignoreCase = true) || it.simpleName.equals(className, ignoreCase = true)
            }
        }

        // 2b. Companion or Inner class dot-notation normalization fallback (e.g. Outer.Companion -> Outer$Companion)
        if (clazz == null && className.contains(".")) {
            val dollarClassName = className.replace(".", "$")
            clazz = allClasses.firstOrNull {
                it.name == dollarClassName ||
                it.simpleName == dollarClassName ||
                it.name.endsWith(".$dollarClassName") ||
                it.name.endsWith(dollarClassName) ||
                it.name.substringAfterLast('.').equals(dollarClassName.substringAfterLast('.'), ignoreCase = true)
            }
            if (clazz != null) {
                echo("Note: Resolved '$className' to companion/inner class '${clazz.simpleName}'.\n")
            }
        }

        // 3. Kotlin top-level function facade fallback (${className}Kt)
        if (clazz == null) {
            val ktClassName = "${className}Kt"
            clazz = allClasses.firstOrNull {
                it.simpleName == ktClassName || it.name.endsWith(".$ktClassName") || it.simpleName.equals(ktClassName, ignoreCase = true)
            }
            if (clazz != null) {
                echo("Note: Resolved '$className' to Kotlin top-level facade class '${clazz.simpleName}'.\n")
            }
        }

        // 4. Method or Property lookup matching className
        if (clazz == null) {
            val classesWithMatchingMember = allClasses.filter { c ->
                c.methods.any { it.name.equals(className, ignoreCase = true) } ||
                c.properties.any { it.name.equals(className, ignoreCase = true) }
            }
            if (classesWithMatchingMember.isNotEmpty()) {
                clazz = classesWithMatchingMember.first()
                if (classesWithMatchingMember.size == 1) {
                    echo("Note: Resolved '$className' to member in class '${clazz.simpleName}'.\n")
                } else {
                    val otherClasses = classesWithMatchingMember.joinToString(", ") { it.simpleName }
                    echo("Note: '$className' is a member in multiple classes ($otherClasses). Showing report for '${clazz.simpleName}'.\n")
                }
            }
        }

        // 5. Suggestions fallback if still not found
        if (clazz == null) {
            echo("Error: Class or member '$className' not found in the index.", err = true)

            val suggestions = mutableListOf<String>()

            // Substring match on class simpleName
            allClasses.filter { it.simpleName.contains(className, ignoreCase = true) }
                .take(5)
                .forEach { suggestions.add("${it.simpleName} (${it.kind.name.lowercase()})") }

            // Substring match on methods / properties
            allClasses.forEach { c ->
                val matchingMethods = c.methods.filter { it.name.contains(className, ignoreCase = true) }
                for (m in matchingMethods.take(3)) {
                    val entry = "${c.simpleName}.${m.name}() (method)"
                    if (!suggestions.contains(entry)) suggestions.add(entry)
                }
            }

            if (suggestions.isEmpty()) {
                fun levenshtein(s1: String, s2: String): Int {
                    val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
                    for (i in 0..s1.length) dp[i][0] = i
                    for (j in 0..s2.length) dp[0][j] = j
                    for (i in 1..s1.length) {
                        for (j in 1..s2.length) {
                            val cost = if (s1[i - 1].equals(s2[j - 1], ignoreCase = true)) 0 else 1
                            dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
                        }
                    }
                    return dp[s1.length][s2.length]
                }

                val targetLow = className.lowercase()
                val fuzzyMatches = allClasses.flatMap { c ->
                    listOf(c.simpleName to "class") + c.methods.map { "${c.simpleName}.${it.name}()" to "method" }
                }
                .map { (name, kind) ->
                    val token = name.substringBefore('(').substringAfterLast('.')
                    name to levenshtein(targetLow, token.lowercase())
                }
                .filter { it.second <= 2 }
                .sortedBy { it.second }
                .map { it.first }

                suggestions.addAll(fuzzyMatches)
            }

            if (suggestions.isNotEmpty()) {
                echo("\nDid you mean one of these?", err = true)
                suggestions.distinct().take(8).forEach { suggestion ->
                    echo("  • $suggestion", err = true)
                }
            }
            return
        }

        val printedClasses = mutableSetOf<String>()
        printReportForClass(index, clazz, isPrimary = true)
        printedClasses.add(clazz.name)

        if (deep) {
            val referencedClasses = findReferencedClasses(clazz, allClasses, printedClasses)
            if (referencedClasses.isNotEmpty()) {
                echo("\n==================================================")
                echo(" DEEP EXPLAIN: REFERENCED TYPES & DSL SCOPES (${referencedClasses.size})")
                echo("==================================================\n")

                for (refClazz in referencedClasses) {
                    if (printedClasses.contains(refClazz.name)) continue
                    echo("--------------------------------------------------")
                    echo(" REFERENCED SCOPE / TYPE: ${refClazz.simpleName}")
                    echo("--------------------------------------------------")
                    printReportForClass(index, refClazz, isPrimary = false)
                    printedClasses.add(refClazz.name)
                }
            }
        }
    }

    private fun printReportForClass(index: LibraryApiIndex, clazz: ClassApi, isPrimary: Boolean) {
        val pkgName = index.packages.firstOrNull { it.classes.contains(clazz) }?.name ?: ""

        if (isPrimary) {
            echo("==================================================")
            echo(" CLASS EXPLAIN REPORT")
            echo("==================================================")
        }
        echo("Class:       ${clazz.name}")
        echo("Package:     $pkgName")
        echo("Kind:        ${clazz.kind.name.lowercase()}")
        echo("Visibility:  ${clazz.visibility.name.lowercase()}")
        if (clazz.targets.isNotEmpty()) {
            echo("Targets:     ${clazz.targets.joinToString(", ")}")
        }
        clazz.sourceLocation?.let { loc ->
            echo("Source:      ${loc.file}:${loc.line}")
        }
        if (clazz.modifiers.isNotEmpty()) {
            echo("Modifiers:   ${clazz.modifiers.joinToString(", ")}")
        }
        if (clazz.typeParameters.isNotEmpty()) {
            val typeParamStr = clazz.typeParameters.joinToString(", ") { param ->
                val reifiedPrefix = if (param.isReified) "reified " else ""
                val boundsStr = if (param.upperBounds.isNotEmpty()) " : ${param.upperBounds.joinToString(" & ")}" else ""
                "$reifiedPrefix${param.name}$boundsStr"
            }
            echo("TypeParams:  <$typeParamStr>")
        }
        if (clazz.superTypes.isNotEmpty()) {
            echo("Supertypes:  ${clazz.superTypes.joinToString(", ")}")
        }
        if (clazz.annotations.isNotEmpty()) {
            echo("Annotations: ${clazz.annotations.joinToString { "@" + it.name.substringAfterLast('.') }}")
        }
        if (clazz.dslMarkerAnnotations.isNotEmpty()) {
            echo("DSL Scopes:  ${clazz.dslMarkerAnnotations.joinToString(", ") { "@$it" }}  ← @DslMarker")
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
                val targetStr = if (cons.targets.isNotEmpty() && cons.targets != clazz.targets) " [${cons.targets.joinToString(", ")}]" else ""
                echo("  - ${cons.visibility.name.lowercase()} constructor($params)$targetStr")
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
                val locStr = prop.sourceLocation?.let { " (${it.file}:${it.line})" } ?: ""
                val targetStr = if (prop.targets.isNotEmpty() && prop.targets != clazz.targets) " [${prop.targets.joinToString(", ")}]" else ""
                echo("  - ${prop.visibility.name.lowercase()} ${constStr}$mut ${prop.name}: ${prop.type}$locStr$targetStr")
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

                // Type parameters with reified keyword
                val typeParamStr = if (method.typeParameters.isNotEmpty()) {
                    "<${method.typeParameters.joinToString(", ") { param ->
                        val reifiedPrefix = if (param.isReified) "reified " else ""
                        "$reifiedPrefix${param.name}"
                    }}> "
                } else ""

                val params = method.parameters.joinToString { param ->
                    val lambdaHint = if (param.isLambdaReceiver) " /*receiver*/" else ""
                    "${param.name}: ${param.type}$lambdaHint"
                }
                val receiver = if (method.extensionReceiverType != null) "${method.extensionReceiverType}." else ""
                val locStr = method.sourceLocation?.let { " (${it.file}:${it.line})" } ?: ""
                val targetStr = if (method.targets.isNotEmpty() && method.targets != clazz.targets) " [${method.targets.joinToString(", ")}]" else ""
                echo("  - ${method.visibility.name.lowercase()} ${modsStr}fun $typeParamStr$receiver${method.name}($params): ${method.returnType}$locStr$targetStr")
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

        if (clazz.imports.isNotEmpty()) {
            echo("Imports:")
            for (imp in clazz.imports) {
                echo("  - $imp")
            }
            echo("")
        }

        if (clazz.documentationExamples.isNotEmpty()) {
            echo("Guide Examples (from README/Dokka):")
            for (example in clazz.documentationExamples.take(2)) {
                echo("  ```kotlin")
                echo(example.lines().joinToString("\n") { "  $it" })
                echo("  ```")
            }
            echo("")
        }
    }

    private fun findReferencedClasses(
        targetClass: ClassApi,
        allClasses: List<ClassApi>,
        alreadyPrinted: Set<String>
    ): List<ClassApi> {
        val builtins = setOf(
            "Unit", "Int", "String", "Boolean", "Any", "Object", "List", "Array", "Set", "Map",
            "Double", "Float", "Long", "Byte", "Char", "Short", "Nothing", "Modifier", "Function0",
            "Function1", "Function2", "Throwable", "Exception", "Class"
        )

        val rawTypeStrings = mutableSetOf<String>()
        rawTypeStrings.addAll(targetClass.superTypes)

        for (method in targetClass.methods) {
            method.extensionReceiverType?.let { rawTypeStrings.add(it) }
            rawTypeStrings.add(method.returnType)
            for (param in method.parameters) {
                rawTypeStrings.add(param.type)
            }
        }

        for (prop in targetClass.properties) {
            rawTypeStrings.add(prop.type)
        }

        val tokens = mutableSetOf<String>()
        val tokenRegex = Regex("[A-Za-z0-9_]+")
        for (raw in rawTypeStrings) {
            tokenRegex.findAll(raw).forEach { match ->
                val t = match.value
                if (t !in builtins && t.length > 2) {
                    tokens.add(t)
                }
            }
        }

        val result = mutableListOf<ClassApi>()
        for (token in tokens) {
            val matched = allClasses.firstOrNull {
                (it.simpleName == token || it.name == token) && !alreadyPrinted.contains(it.name)
            }
            if (matched != null && !result.contains(matched) && matched.name != targetClass.name) {
                result.add(matched)
            }
        }

        return result
    }
}
