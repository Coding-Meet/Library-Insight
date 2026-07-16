package com.meet.libraryinsight.export

import com.meet.libraryinsight.model.*

object MarkdownExporter {

    /**
     * Exports the library API index to a readable Markdown reference documentation.
     */
    fun export(index: LibraryApiIndex): String {
        val sb = StringBuilder()
        sb.append("# Library Reference: ${index.libraryName}\n\n")
        sb.append("- **Version**: ${index.version}\n")
        sb.append("- **Packages Count**: ${index.packages.size}\n\n")

        sb.append("## Table of Contents\n\n")
        for (pkg in index.packages) {
            sb.append("- **Package [${pkg.name}](#package-${pkg.name.replace('.', '-')})**\n")
            for (clazz in pkg.classes) {
                sb.append("  - [${clazz.simpleName}](#class-${clazz.name.replace('.', '-').replace('$', '-')}) (${clazz.kind.name.lowercase()})\n")
            }
        }
        sb.append("\n---\n\n")

        for (pkg in index.packages) {
            sb.append("## Package ${pkg.name}\n\n")

            for (clazz in pkg.classes) {
                sb.append("### Class ${clazz.name}\n\n")
                
                // Show annotations
                if (clazz.annotations.isNotEmpty()) {
                    sb.append("```kotlin\n")
                    clazz.annotations.forEach { anno ->
                        sb.append(formatAnnotation(anno)).append("\n")
                    }
                    sb.append("```\n")
                }

                // Declaration signature
                sb.append("```kotlin\n")
                val vis = clazz.visibility.name.lowercase()
                val mods = clazz.modifiers.joinToString(" ")
                val kind = when (clazz.kind) {
                    ClassKind.COMPANION_OBJECT -> "companion object"
                    ClassKind.OBJECT -> "object"
                    ClassKind.ANNOTATION -> "annotation class"
                    ClassKind.ENUM -> "enum class"
                    ClassKind.INTERFACE -> "interface"
                    ClassKind.CLASS -> "class"
                }
                val generics = if (clazz.typeParameters.isNotEmpty()) {
                    clazz.typeParameters.joinToString(prefix = "<", postfix = ">") { param ->
                        param.name + if (param.upperBounds.isNotEmpty()) " : " + param.upperBounds.joinToString(" & ") else ""
                    }
                } else ""
                
                val inheritance = if (clazz.superTypes.isNotEmpty()) " : " + clazz.superTypes.joinToString(", ") else ""
                sb.append("$vis $mods $kind ${clazz.simpleName}$generics$inheritance\n")
                sb.append("```\n\n")

                // Properties
                if (clazz.properties.isNotEmpty()) {
                    sb.append("#### Properties\n\n")
                    sb.append("| Name | Type | Mutability | Visibility | Other |\n")
                    sb.append("| --- | --- | --- | --- | --- |\n")
                    for (prop in clazz.properties) {
                        val mutability = if (prop.isMutable) "var" else "val"
                        val other = mutableListOf<String>()
                        if (prop.isConst) other.add("const")
                        if (prop.isLateinit) other.add("lateinit")
                        if (prop.annotations.isNotEmpty()) {
                            other.add(prop.annotations.joinToString { formatAnnotation(it) })
                        }
                        sb.append("| `${prop.name}` | `${prop.type}` | `$mutability` | `${prop.visibility.name.lowercase()}` | ${other.joinToString(", ")} |\n")
                    }
                    sb.append("\n")
                }

                // Constructors
                if (clazz.constructors.isNotEmpty()) {
                    sb.append("#### Constructors\n\n")
                    for (cons in clazz.constructors) {
                        val params = cons.parameters.joinToString { param ->
                            val defaultVal = if (param.hasDefaultValue) " = ..." else ""
                            "${param.name}: ${param.type}$defaultVal"
                        }
                        sb.append("- `${cons.visibility.name.lowercase()} constructor($params)`\n")
                    }
                    sb.append("\n")
                }

                // Methods
                if (clazz.methods.isNotEmpty()) {
                    sb.append("#### Methods\n\n")
                    for (method in clazz.methods) {
                        val visMethod = method.visibility.name.lowercase()
                        val methodMods = mutableListOf<String>()
                        if (method.flags.isStatic) methodMods.add("static")
                        if (method.flags.isSuspend) methodMods.add("suspend")
                        if (method.flags.isInline) methodMods.add("inline")
                        if (method.flags.isOperator) methodMods.add("operator")
                        if (method.flags.isInfix) methodMods.add("infix")
                        if (method.flags.isAbstract) methodMods.add("abstract")
                        if (method.flags.isOpen) methodMods.add("open")
                        
                        val modsStr = if (methodMods.isNotEmpty()) methodMods.joinToString(" ") + " " else ""
                        val params = method.parameters.joinToString { param ->
                            val defaultVal = if (param.hasDefaultValue) " = ..." else ""
                            "${param.name}: ${param.type}$defaultVal"
                        }
                        
                        val receiver = if (method.extensionReceiverType != null) "${method.extensionReceiverType}." else ""
                        val nameAndParams = "$receiver${method.name}($params)"
                        
                        sb.append("- `$visMethod ${modsStr}fun $nameAndParams: ${method.returnType}`\n")
                        if (method.annotations.isNotEmpty()) {
                            sb.append("  *Annotations: ${method.annotations.joinToString { formatAnnotation(it) }}*\n")
                        }
                    }
                    sb.append("\n")
                }

                sb.append("---\n\n")
            }
        }

        return sb.toString()
    }

    private fun formatAnnotation(anno: AnnotationApi): String {
        val args = if (anno.arguments.isNotEmpty()) {
            anno.arguments.map { "${it.key} = ${it.value}" }.joinToString()
        } else ""
        return "@${anno.name.substringAfterLast('.')}${if (args.isNotEmpty()) "($args)" else ""}"
    }
}
