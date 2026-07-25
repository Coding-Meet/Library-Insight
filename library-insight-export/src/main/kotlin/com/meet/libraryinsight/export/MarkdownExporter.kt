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
                
                // Declaration signature (including annotations in a single clean code block)
                sb.append("```kotlin\n")
                if (clazz.annotations.isNotEmpty()) {
                    clazz.annotations.forEach { anno ->
                        val formatted = formatAnnotation(anno)
                        if (formatted.isNotEmpty()) {
                            sb.append(formatted).append("\n")
                        }
                    }
                }
                val vis = clazz.visibility.name.lowercase()
                val mods = if (clazz.modifiers.isNotEmpty()) clazz.modifiers.joinToString(" ") + " " else ""
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
                sb.append("$vis $mods$kind ${clazz.simpleName}$generics$inheritance\n")
                sb.append("```\n\n")

                // Show class documentation as blockquote
                val cDoc = clazz.doc
                if (cDoc != null) {
                    sb.append("> ${cDoc.trim().replace("\n", "\n> ")}\n\n")
                }

                // Properties
                if (clazz.properties.isNotEmpty()) {
                    sb.append("#### Properties\n\n")
                    sb.append("| Name | Type | Mutability | Visibility | Description |\n")
                    sb.append("| --- | --- | --- | --- | --- |\n")
                    for (prop in clazz.properties) {
                        val mutability = if (prop.isMutable) "var" else "val"
                        val other = mutableListOf<String>()
                        if (prop.isConst) other.add("const")
                        if (prop.isLateinit) other.add("lateinit")
                        
                        val annoText = if (prop.annotations.isNotEmpty()) {
                            prop.annotations.map { formatAnnotation(it) }.filter { it.isNotEmpty() }.joinToString { it }
                        } else ""

                        val docText = prop.doc?.trim()?.replace("\n", " ") ?: ""
                        val descriptionParts = mutableListOf<String>()
                        if (annoText.isNotEmpty()) descriptionParts.add(annoText)
                        if (other.isNotEmpty()) descriptionParts.add(other.joinToString(", "))
                        if (docText.isNotEmpty()) descriptionParts.add(docText)
                        val description = descriptionParts.joinToString("; ")

                        sb.append("| `${prop.name}` | `${prop.type}` | `$mutability` | `${prop.visibility.name.lowercase()}` | $description |\n")
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
                        
                        val mDoc = method.doc
                        if (mDoc != null) {
                            sb.append("  > ${mDoc.trim().replace("\n", "\n  > ")}\n")
                        }
                        if (method.annotations.isNotEmpty()) {
                            val methodAnnos = method.annotations.map { formatAnnotation(it) }.filter { it.isNotEmpty() }
                            if (methodAnnos.isNotEmpty()) {
                                sb.append("  *Annotations: ${methodAnnos.joinToString()}*\n")
                            }
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
        val normalizedName = anno.name.replace('/', '.')
        if (normalizedName == "kotlin.Metadata" || normalizedName.startsWith("kotlin.jvm.internal")) {
            return ""
        }
        val args = if (anno.arguments.isNotEmpty()) {
            anno.arguments.map { "${it.key} = ${it.value}" }.joinToString()
        } else ""
        return "@${anno.name.substringAfterLast('.')}${if (args.isNotEmpty()) "($args)" else ""}"
    }
}
