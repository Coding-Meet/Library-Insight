package com.meet.libraryinsight.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.meet.libraryinsight.cli.DatabaseHelper
import com.meet.libraryinsight.model.ClassApi
import java.io.File

class HealthCommand : CliktCommand(
    name = "health",
    help = "Generate a comprehensive Package Health & API Complexity Report for the scanned library."
) {
    val db by option(
        "--db",
        help = "Index database JSON file path to read from"
    ).file().default(File("build/library-insight-index.json"))

    override fun run() {
        val index = DatabaseHelper.loadIndex(db)
        if (index == null) {
            echo("Error: Index database file not found at ${db.absolutePath}. Please run 'scan' first.", err = true)
            return
        }

        val allClasses = index.packages.flatMap { it.classes }
        if (allClasses.isEmpty()) {
            echo("The library index is empty. Please check the scan target.", err = true)
            return
        }

        val totalClasses = allClasses.size
        val totalMethods = allClasses.sumOf { it.methods.size }
        val totalProperties = allClasses.sumOf { it.properties.size }
        val totalConstructors = allClasses.sumOf { it.constructors.size }
        val totalTypeAliases = index.packages.sumOf { it.typeAliases.size }
        val totalApis = totalClasses + totalMethods + totalProperties + totalConstructors + totalTypeAliases

        // Deprecations & Experimental counts
        val deprecatedClasses = allClasses.count { c -> c.annotations.any { it.name.contains("Deprecated") } }
        val deprecatedMethods = allClasses.sumOf { c -> c.methods.count { m -> m.annotations.any { it.name.contains("Deprecated") } } }
        val deprecatedProps = allClasses.sumOf { c -> c.properties.count { p -> p.annotations.any { it.name.contains("Deprecated") } } }
        val totalDeprecated = deprecatedClasses + deprecatedMethods + deprecatedProps

        val experimentalClasses = allClasses.count { c -> c.annotations.any { it.name.contains("Experimental") || it.name.contains("RequiresOptIn") } }
        val experimentalMethods = allClasses.sumOf { c -> c.methods.count { m -> m.annotations.any { it.name.contains("Experimental") || it.name.contains("RequiresOptIn") } } }
        val totalExperimental = experimentalClasses + experimentalMethods

        // Grading logic based on deprecation percentage
        val deprecationPercentage = if (totalApis > 0) (totalDeprecated.toDouble() / totalApis.toDouble()) * 100.0 else 0.0
        val healthGrade = when {
            deprecationPercentage < 1.0 -> "A+"
            deprecationPercentage < 3.0 -> "A"
            deprecationPercentage < 6.0 -> "B"
            deprecationPercentage < 10.0 -> "C"
            deprecationPercentage < 20.0 -> "D"
            else -> "F"
        }

        // Packages Analysis
        val largestPkg = index.packages.maxByOrNull { it.classes.size }
        val mostDeprecatedPkg = index.packages.maxByOrNull { pkg ->
            pkg.classes.sumOf { c -> c.methods.count { m -> m.annotations.any { it.name.contains("Deprecated") } } }
        }

        // Complexity Metrics (Feature 6)
        val largestClass = allClasses.maxByOrNull { it.methods.size }
        val longestSignatureMethod = allClasses.flatMap { c -> c.methods.map { c to it } }.maxByOrNull { it.second.parameters.size }
        val deepestInheritanceClass = allClasses.maxByOrNull { it.superTypes.size }
        val mostGenericClass = allClasses.maxByOrNull { it.typeParameters.size }

        echo("==================================================")
        echo("    PACKAGE HEALTH & COMPLEXITY REPORT")
        echo("==================================================")
        echo("Library Target : ${index.libraryName} (${index.version})")
        echo("API Health Grade: $healthGrade (Deprecation ratio: ${String.format("%.2f", deprecationPercentage)}%)")
        echo("==================================================\n")

        echo("▶ API Distribution")
        echo("  Total Public APIs   : $totalApis")
        echo("  ├─ Classes/Objects  : $totalClasses")
        echo("  ├─ Constructors     : $totalConstructors")
        echo("  ├─ Methods          : $totalMethods")
        echo("  ├─ Properties       : $totalProperties")
        echo("  └─ Type Aliases     : $totalTypeAliases")
        echo("  Deprecated APIs     : $totalDeprecated")
        echo("  Experimental APIs   : $totalExperimental\n")

        echo("▶ Package Topology")
        if (largestPkg != null) {
            echo("  Largest Package     : ${largestPkg.name} (${largestPkg.classes.size} classes)")
        }
        if (mostDeprecatedPkg != null) {
            val depCount = mostDeprecatedPkg.classes.sumOf { c -> c.methods.count { m -> m.annotations.any { it.name.contains("Deprecated") } } }
            echo("  Most Deprecated Pkg : ${mostDeprecatedPkg.name} ($depCount deprecated methods)")
        }
        echo("")

        echo("▶ API Complexity Metrics")
        if (largestClass != null) {
            echo("  Largest Class       : ${largestClass.name} (${largestClass.methods.size} methods)")
        }
        if (longestSignatureMethod != null) {
            val (clazz, method) = longestSignatureMethod
            echo("  Longest Signature   : ${clazz.name}.${method.name} (${method.parameters.size} parameters)")
        }
        if (deepestInheritanceClass != null) {
            echo("  Deepest Inheritance : ${deepestInheritanceClass.name} (${deepestInheritanceClass.superTypes.size} supertypes: ${deepestInheritanceClass.superTypes.joinToString(", ")})")
        }
        if (mostGenericClass != null) {
            echo("  Most Generic Class  : ${mostGenericClass.name} (${mostGenericClass.typeParameters.size} parameters: <${mostGenericClass.typeParameters.joinToString { it.name }}>)")
        }
        echo("==================================================")
    }
}
