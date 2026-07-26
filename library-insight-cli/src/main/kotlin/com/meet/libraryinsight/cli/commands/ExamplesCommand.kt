package com.meet.libraryinsight.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.meet.libraryinsight.cli.DatabaseHelper
import com.meet.libraryinsight.model.ClassApi
import com.meet.libraryinsight.model.ClassKind
import com.meet.libraryinsight.model.MethodApi
import com.meet.libraryinsight.model.Visibility
import java.io.File

class ExamplesCommand : CliktCommand(
    name = "examples",
    help = "Generate idiomatic Kotlin code examples showing usage patterns of a specific class."
) {
    val className by argument(help = "Fully qualified or simple name of the class")

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
        val clazz = allClasses.firstOrNull { it.name == className || it.simpleName == className }

        if (clazz == null) {
            echo("Error: Class '$className' not found in the index.", err = true)
            return
        }

        val hasConstructor = clazz.constructors.any { it.visibility == Visibility.PUBLIC }
        
        // Detect nested or separate Builder
        val builderClass = allClasses.firstOrNull { 
            it.name == "${clazz.name}\$Builder" || it.name == "${clazz.name}.Builder" || it.simpleName == "${clazz.simpleName}Builder"
        }
        val hasBuilder = builderClass != null

        // Detect Factory methods (static methods returning target class or companion methods returning target class)
        val staticFactoryMethods = clazz.methods.filter { method ->
            method.visibility == Visibility.PUBLIC && method.flags.isStatic && 
            (method.returnType == clazz.name || method.returnType == clazz.simpleName || method.returnType.endsWith(".${clazz.simpleName}"))
        }
        val companionClass = allClasses.firstOrNull { it.name == "${clazz.name}\$Companion" }
        val companionFactoryMethods = companionClass?.methods?.filter { method ->
            method.visibility == Visibility.PUBLIC && 
            (method.returnType == clazz.name || method.returnType == clazz.simpleName || method.returnType.endsWith(".${clazz.simpleName}"))
        } ?: emptyList()
        val allFactoryMethods = staticFactoryMethods + companionFactoryMethods
        val hasFactory = allFactoryMethods.isNotEmpty()

        // Detect Singleton
        val isSingleton = clazz.kind == ClassKind.OBJECT || 
                           clazz.properties.any { it.name == "INSTANCE" && it.visibility == Visibility.PUBLIC && it.isConst } ||
                           clazz.properties.any { it.name == "Companion" && it.visibility == Visibility.PUBLIC }
        
        echo("==================================================")
        echo("  API USAGE EXAMPLES GENERATOR  —  ${clazz.simpleName}")
        echo("==================================================")
        echo("// Target API: ${clazz.name}")
        echo("\nDetected Usage Patterns:")
        echo("  ${if (hasConstructor) "✓" else "✗"} Constructor")
        echo("  ${if (hasBuilder) "✓" else "✗"} Builder")
        echo("  ${if (hasFactory) "✓" else "✗"} Factory")
        echo("  ${if (isSingleton) "✓" else "✗"} Singleton")
        echo("==================================================")

        // 0. Guide Examples (from README/Dokka markdown files)
        if (clazz.documentationExamples.isNotEmpty()) {
            echo("\n// Pattern: Guide Examples (from README/Dokka)")
            for (example in clazz.documentationExamples.take(3)) {
                echo(example)
                echo("")
            }
        }

        // 1. Singleton pattern
        if (isSingleton) {
            echo("\n// Pattern: Singleton Access")
            val nameRef = if (clazz.kind == ClassKind.OBJECT) clazz.name.replace('$', '.') else "${clazz.name.replace('$', '.')}.INSTANCE"
            echo("val instance = $nameRef")
        }

        // 2. Factory pattern
        if (hasFactory) {
            echo("\n// Pattern: Factory Instantiation")
            for (factory in allFactoryMethods.take(2)) {
                val args = factory.parameters.joinToString(", ") { "${it.name} = ${mockValueForType(it.type)}" }
                val isCompanionMethod = companionFactoryMethods.contains(factory)
                val callPrefix = if (isCompanionMethod) "${clazz.name.replace('$', '.')}" else "${clazz.name.replace('$', '.')}"
                echo("val ${clazz.simpleName.lowercase()} = $callPrefix.${factory.name}($args)")
            }
        }

        // 3. Builder pattern
        if (hasBuilder && builderClass != null) {
            echo("\n// Pattern: Builder Configuration")
            generateBuilderExample(clazz, builderClass)
        }

        // 4. Constructor pattern
        if (hasConstructor) {
            echo("\n// Pattern: Constructor Instantiation")
            val shortestCons = clazz.constructors.filter { it.visibility == Visibility.PUBLIC }.minByOrNull { it.parameters.size }!!
            val paramsStr = shortestCons.parameters.joinToString(", ") { param ->
                "${param.name} = ${mockValueForType(param.type)}"
            }
            echo("val ${clazz.simpleName.lowercase()} = ${clazz.name.replace('$', '.')}($paramsStr)")
        }

        // 5. Method calls demonstration
        val publicMethods = clazz.methods.filter { it.visibility == Visibility.PUBLIC && it.name != "<init>" && it.name != "<clinit>" && !it.flags.isStatic }
        val publicProps = clazz.properties.filter { it.visibility == Visibility.PUBLIC }

        if (publicProps.isNotEmpty() || publicMethods.isNotEmpty()) {
            echo("\n// API Invocation Examples")
            val varName = clazz.simpleName.lowercase()
            for (prop in publicProps.take(3)) {
                val accessor = if (prop.isMutable) "$varName.${prop.name} = ${mockValueForType(prop.type)}" else "val ${prop.name} = $varName.${prop.name}"
                echo("  $accessor // type: ${prop.type}")
            }
            for (method in publicMethods.take(4)) {
                val paramsStr = method.parameters.joinToString(", ") { param ->
                    "${param.name} = ${mockValueForType(param.type)}"
                }
                val receiver = method.extensionReceiverType
                val receiverPrefix = if (receiver != null) {
                    "${mockValueForType(receiver)}."
                } else {
                    "$varName."
                }
                echo("  ${receiverPrefix}${method.name}($paramsStr) // returns: ${method.returnType}")
            }
        }
        echo("==================================================")
    }

    private fun generateBuilderExample(targetClass: ClassApi, builderClass: ClassApi) {
        val targetName = targetClass.simpleName
        val builderMethods = builderClass.methods.filter { it.visibility == Visibility.PUBLIC && it.name != "<init>" && it.name != "<clinit>" }
        
        val buildMethod = builderMethods.firstOrNull { it.returnType == targetClass.name || it.returnType == targetClass.simpleName || it.name == "build" }
        val chainMethods = builderMethods.filter { it != buildMethod && (it.returnType == builderClass.name || it.returnType == builderClass.simpleName) }

        val sb = StringBuilder()
        sb.append("val ${targetName.lowercase()} = ${builderClass.name.replace('$', '.')}()\n")
        for (m in chainMethods.take(3)) {
            val args = m.parameters.joinToString(", ") { mockValueForType(it.type) }
            sb.append("    .${m.name}($args)\n")
        }
        if (buildMethod != null) {
            sb.append("    .${buildMethod.name}()")
        } else {
            sb.append("    .build()")
        }
        echo(sb.toString())
    }

    private fun mockValueForType(type: String): String {
        val t = type.lowercase()
        return when {
            t.contains("string") -> "\"example\""
            t.contains("boolean") -> "true"
            t.contains("int") -> "42"
            t.contains("long") -> "100L"
            t.contains("double") -> "3.14"
            t.contains("float") -> "1.0f"
            t.contains("list") -> "listOf(...)"
            t.contains("map") -> "mapOf(...)"
            t.contains("->") -> "{ }"
            else -> "..."
        }
    }
}
