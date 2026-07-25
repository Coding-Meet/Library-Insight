package com.meet.libraryinsight.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.meet.libraryinsight.common.MavenResolver
import com.meet.libraryinsight.core.LibraryAnalyzer
import com.meet.libraryinsight.model.*
import java.io.File

class MigrateCommand : CliktCommand(
    name = "migrate",
    help = "Compare two library versions and output a migration advisor report."
) {
    val oldSource by argument(help = "Old version JAR/AAR file path or Maven coordinate")
    val newSource by argument(help = "New version JAR/AAR file path or Maven coordinate")

    val repos by option("--repo", help = "Additional Maven repository URLs to resolve coordinates").multiple()

    override fun run() {
        try {
            val oldIndex = getIndexForSource(oldSource)
            val newIndex = getIndexForSource(newSource)

            echo("==================================================")
            echo("        Library Insight Migration Report")
            echo("==================================================")
            echo("Old Version : ${oldIndex.libraryName}:${oldIndex.version}")
            echo("New Version : ${newIndex.libraryName}:${newIndex.version}")
            echo("==================================================\n")

            val oldClasses = oldIndex.packages.flatMap { it.classes }
            val oldClassMap = oldClasses.associateBy { it.name }
            val newClasses = newIndex.packages.flatMap { it.classes }
            val newClassMap = newClasses.associateBy { it.name }

            val removedClassNames = oldClassMap.keys.filter { it !in newClassMap.keys }
            val newlyDeprecatedClasses = newClasses.filter {
                isDeprecated(it.annotations) &&
                (oldClassMap[it.name] == null || !isDeprecated(oldClassMap[it.name]!!.annotations))
            }

            // 1. Removed Classes
            if (removedClassNames.isNotEmpty()) {
                echo("❌ Removed Classes")
                echo("-----------------")
                for (name in removedClassNames.sorted()) {
                    val oldClass = oldClassMap[name]!!
                    val hint = getReplacementHint(oldClass.annotations, oldClass.doc)
                    val hintSuffix = if (hint != null) "  -> Replacement: $hint" else ""
                    echo("- $name$hintSuffix")
                }
                echo("")
            }

            // 2. Newly Deprecated Classes
            if (newlyDeprecatedClasses.isNotEmpty()) {
                echo("⚠️ Newly Deprecated Classes")
                echo("--------------------------")
                for (clazz in newlyDeprecatedClasses.sortedBy { it.name }) {
                    val hint = getReplacementHint(clazz.annotations, clazz.doc)
                    val hintSuffix = if (hint != null) "  -> Replacement: $hint" else ""
                    echo("- ${clazz.name}$hintSuffix")
                }
                echo("")
            }

            // Methods & Properties comparisons
            val removedMethods = mutableListOf<String>()
            val deprecatedMethods = mutableListOf<String>()
            val removedProperties = mutableListOf<String>()
            val deprecatedProperties = mutableListOf<String>()
            var hasBreaking = removedClassNames.isNotEmpty()

            for (name in oldClassMap.keys intersect newClassMap.keys) {
                val oldClass = oldClassMap[name]!!
                val newClass = newClassMap[name]!!

                // Methods
                val oldMethodsMap = oldClass.methods.associateBy { it.signature }
                val newMethodsMap = newClass.methods.associateBy { it.signature }
                
                // Check removed methods from old version
                for (oldMethod in oldClass.methods) {
                    val isRemoved = oldMethod.signature !in newMethodsMap
                    if (isRemoved) {
                        val isPublicOrProtected = oldMethod.visibility == Visibility.PUBLIC || oldMethod.visibility == Visibility.PROTECTED
                        if (isPublicOrProtected) {
                            val hint = getReplacementHint(oldMethod.annotations, oldMethod.doc)
                            val hintSuffix = if (hint != null) "  -> Replacement: $hint" else ""
                            val paramsStr = oldMethod.parameters.joinToString { it.type }
                            removedMethods.add("- fun ${oldClass.name}.${oldMethod.name}($paramsStr)$hintSuffix")
                            hasBreaking = true
                        }
                    }
                }
                
                // Check newly deprecated methods in new version
                for (newMethod in newClass.methods) {
                    val isDep = isDeprecated(newMethod.annotations)
                    if (isDep) {
                        val oldMethod = oldMethodsMap[newMethod.signature]
                        val wasAlreadyDep = oldMethod != null && isDeprecated(oldMethod.annotations)
                        if (!wasAlreadyDep) {
                            val hint = getReplacementHint(newMethod.annotations, newMethod.doc)
                            val hintSuffix = if (hint != null) "  -> Replacement: $hint" else ""
                            val paramsStr = newMethod.parameters.joinToString { it.type }
                            deprecatedMethods.add("- fun ${newClass.name}.${newMethod.name}($paramsStr)$hintSuffix")
                        }
                    }
                }

                // Properties
                val oldPropsMap = oldClass.properties.associateBy { it.name }
                val newPropsMap = newClass.properties.associateBy { it.name }
                
                // Check removed properties from old version
                for (oldProp in oldClass.properties) {
                    val isRemoved = oldProp.name !in newPropsMap
                    if (isRemoved) {
                        val isPublicOrProtected = oldProp.visibility == Visibility.PUBLIC || oldProp.visibility == Visibility.PROTECTED
                        if (isPublicOrProtected) {
                            val hint = getReplacementHint(oldProp.annotations, oldProp.doc)
                            val hintSuffix = if (hint != null) "  -> Replacement: $hint" else ""
                            removedProperties.add("- property ${oldClass.name}.${oldProp.name}: ${oldProp.type}$hintSuffix")
                            hasBreaking = true
                        }
                    }
                }
                
                // Check newly deprecated properties in new version
                for (newProp in newClass.properties) {
                    val isDep = isDeprecated(newProp.annotations)
                    if (isDep) {
                        val oldProp = oldPropsMap[newProp.name]
                        val wasAlreadyDep = oldProp != null && isDeprecated(oldProp.annotations)
                        if (!wasAlreadyDep) {
                            val hint = getReplacementHint(newProp.annotations, newProp.doc)
                            val hintSuffix = if (hint != null) "  -> Replacement: $hint" else ""
                            deprecatedProperties.add("- property ${newClass.name}.${newProp.name}: ${newProp.type}$hintSuffix")
                        }
                    }
                }
            }

            // 3. Removed Methods
            if (removedMethods.isNotEmpty()) {
                echo("❌ Removed Methods")
                echo("-----------------")
                removedMethods.sorted().forEach { echo(it) }
                echo("")
            }

            // 4. Deprecated Methods
            if (deprecatedMethods.isNotEmpty()) {
                echo("⚠️ Deprecated Methods")
                echo("--------------------")
                deprecatedMethods.sorted().forEach { echo(it) }
                echo("")
            }

            // 5. Removed Properties
            if (removedProperties.isNotEmpty()) {
                echo("❌ Removed Properties")
                echo("--------------------")
                removedProperties.sorted().forEach { echo(it) }
                echo("")
            }

            // 6. Deprecated Properties
            if (deprecatedProperties.isNotEmpty()) {
                echo("⚠️ Deprecated Properties")
                echo("-----------------------")
                deprecatedProperties.sorted().forEach { echo(it) }
                echo("")
            }

            echo("--------------------------------------------------")
            if (hasBreaking) {
                echo("Binary Compatibility: ❌ BREAKING CHANGES DETECTED")
            } else {
                echo("Binary Compatibility: ✅ COMPATIBLE (NO BREAKING CHANGES DETECTED)")
            }
            echo("==================================================")

        } catch (e: Exception) {
            echo("Error running migration advisor: ${e.message}", err = true)
        }
    }

    private fun getIndexForSource(source: String): LibraryApiIndex {
        return if (MavenResolver.isCoordinate(source)) {
            echo("Resolving Maven coordinate: $source")
            val resolved = MavenResolver.resolve(source, repos) { progress ->
                echo("  -> $progress")
            }
            val parts = source.split(':')
            LibraryAnalyzer.analyze(resolved.binaryFile, parts[1], parts[2], resolved.sourcesFile)
        } else {
            val file = File(source)
            if (!file.exists()) {
                throw IllegalArgumentException("Source path '$source' does not exist.")
            }
            echo("Scanning file: ${file.absolutePath}")
            LibraryAnalyzer.analyze(file, file.nameWithoutExtension, "1.0.0", null)
        }
    }

    private fun isDeprecated(annotations: List<AnnotationApi>): Boolean {
        return annotations.any {
            val name = it.name.replace('/', '.')
            name == "kotlin.Deprecated" || name == "java.lang.Deprecated"
        }
    }

    private fun getReplacementHint(annotations: List<AnnotationApi>, doc: String?): String? {
        // 1. Try Kotlin @ReplaceWith expression
        val deprecatedAnno = annotations.firstOrNull {
            val name = it.name.replace('/', '.')
            name == "kotlin.Deprecated" || name == "java.lang.Deprecated"
        }
        if (deprecatedAnno != null) {
            val replaceWithVal = deprecatedAnno.arguments["replaceWith"]
            if (replaceWithVal != null) {
                val regex = """expression\s*=\s*"([^"]+)"""".toRegex()
                val match = regex.find(replaceWithVal)?.groupValues?.getOrNull(1)
                if (match != null && match.isNotEmpty()) {
                    return "$match"
                }
            }
            val msgVal = deprecatedAnno.arguments["message"]
            if (msgVal != null && msgVal.trim().isNotEmpty()) {
                return msgVal.removeSurrounding("\"")
            }
        }

        // 2. Try Javadoc @deprecated parsing
        if (doc != null) {
            val index = doc.indexOf("@deprecated", ignoreCase = true)
            if (index != -1) {
                val tagText = doc.substring(index + "@deprecated".length).trim().lines().firstOrNull() ?: ""
                val linkRegex = """(?:Use\s+\{@link\s+([^}]+)\}|Use\s+([a-zA-Z0-9_\.\(\)\#\s]+)(?:instead)?)""".toRegex(RegexOption.IGNORE_CASE)
                val match = linkRegex.find(tagText)
                val extracted = match?.groupValues?.getOrNull(1)?.trim() ?: match?.groupValues?.getOrNull(2)?.trim()
                if (extracted != null && extracted.isNotEmpty()) {
                    return "Use $extracted"
                }
                if (tagText.isNotEmpty()) {
                    return tagText
                }
            }
        }

        return null
    }
}
