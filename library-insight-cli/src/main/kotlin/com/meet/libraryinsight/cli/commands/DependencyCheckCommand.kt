package com.meet.libraryinsight.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.meet.libraryinsight.common.MavenResolver
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import java.io.File
import java.util.jar.JarFile

class DependencyCheckCommand : CliktCommand(
    name = "dependency-check",
    help = "Verify all project dependencies for binary ABI compatibility and potential LinkageError/runtime conflicts."
) {
    val dir by option(
        "--dir",
        help = "Target project directory to scan (defaults to current directory)"
    ).file(mustExist = true, canBeFile = false).default(File("."))

    override fun run() {
        echo("==================================================")
        echo("    DEPENDENCY CONFLICT & ABI DETECTOR")
        echo("==================================================")

        val catalogFile = File(dir, "gradle/libs.versions.toml")
        val catalogLibraries = mutableMapOf<String, String>()
        if (catalogFile.exists()) {
            parseVersionCatalog(catalogFile, catalogLibraries)
        }

        val buildFiles = dir.walkTopDown()
            .filter { it.isFile && (it.name == "build.gradle.kts" || it.name == "build.gradle") }
            .filter { !it.absolutePath.contains("/build/") && !it.absolutePath.contains("/.gradle/") && !it.absolutePath.contains("/.git/") }
            .toList()

        if (buildFiles.isEmpty()) {
            echo("Error: No build.gradle or build.gradle.kts found in ${dir.absolutePath}.", err = true)
            return
        }

        val dependencies = mutableSetOf<String>()
        for (buildFile in buildFiles) {
            parseBuildFile(buildFile, catalogLibraries, dependencies)
        }

        if (dependencies.isEmpty()) {
            echo("No dependencies found to verify.")
            return
        }

        echo("Analyzing ${dependencies.size} dependencies on classpath...")

        // Sets/Maps to hold defined signatures
        val definedClasses = mutableSetOf<String>()
        val definedMethods = mutableSetOf<String>() // format: "owner.name desc"
        val definedFields = mutableSetOf<String>()  // format: "owner.name desc"

        // List of references to verify
        val referencedMethods = mutableListOf<ReferencedMember>()
        val referencedFields = mutableListOf<ReferencedMember>()

        for (coord in dependencies) {
            try {
                val resolved = MavenResolver.resolve(coord) { /* silent progress */ }
                val jarFile = JarFile(resolved.binaryFile)
                val entries = jarFile.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.name.endsWith(".class")) {
                        jarFile.getInputStream(entry).use { stream ->
                            val bytes = stream.readBytes()
                            val classReader = ClassReader(bytes)
                            val classNode = ClassNode()
                            // Do NOT skip code so we can analyze instructions
                            classReader.accept(classNode, 0)

                            val classInternalName = classNode.name
                            definedClasses.add(classInternalName)

                            // Record defined fields
                            classNode.fields?.forEach { field ->
                                definedFields.add("$classInternalName.${field.name} ${field.desc}")
                            }

                            // Record defined methods
                            classNode.methods?.forEach { method ->
                                definedMethods.add("$classInternalName.${method.name} ${method.desc}")

                                // Scan instructions for external references
                                method.instructions?.iterator()?.forEachRemaining { insn ->
                                    if (insn is MethodInsnNode) {
                                        referencedMethods.add(
                                            ReferencedMember(
                                                sourceClass = classInternalName,
                                                targetOwner = insn.owner,
                                                name = insn.name,
                                                desc = insn.desc,
                                                dependencySource = resolved.binaryFile.name
                                            )
                                        )
                                    } else if (insn is FieldInsnNode) {
                                        referencedFields.add(
                                            ReferencedMember(
                                                sourceClass = classInternalName,
                                                targetOwner = insn.owner,
                                                name = insn.name,
                                                desc = insn.desc,
                                                dependencySource = resolved.binaryFile.name
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Skip unresolvable or system libs silently
            }
        }

        echo("Defined classes in classpath: ${definedClasses.size}")
        echo("Defined methods: ${definedMethods.size}")
        echo("Analyzing references for ABI linkage conflicts...")

        var conflictCount = 0

        // Filter and check method references
        // We only check references whose owner belongs to our scanned classpath (definedClasses)
        val methodConflicts = referencedMethods
            .filter { it.targetOwner in definedClasses }
            .filter { "${it.targetOwner}.${it.name} ${it.desc}" !in definedMethods }
            // Filter out common compiler generated/synthetic stuff or JDK defaults not caught by filter
            .filter { !it.name.startsWith("access$") && it.name != "<clinit>" }
            .distinctBy { "${it.sourceClass} -> ${it.targetOwner}.${it.name}" }

        if (methodConflicts.isNotEmpty()) {
            echo("\n🚨 Potential ABI Method Conflicts (LinkageError risk):")
            for (conflict in methodConflicts.take(20)) {
                echo("  [Method Missing] class ${formatInternalName(conflict.sourceClass)} (from ${conflict.dependencySource})")
                echo("   └── Calls missing method: ${formatInternalName(conflict.targetOwner)}.${conflict.name}${conflict.desc}")
                conflictCount++
            }
            if (methodConflicts.size > 20) {
                echo("  ... and ${methodConflicts.size - 20} more method conflicts.")
            }
        }

        // Filter and check field references
        val fieldConflicts = referencedFields
            .filter { it.targetOwner in definedClasses }
            .filter { "${it.targetOwner}.${it.name} ${it.desc}" !in definedFields }
            .distinctBy { "${it.sourceClass} -> ${it.targetOwner}.${it.name}" }

        if (fieldConflicts.isNotEmpty()) {
            echo("\n🚨 Potential ABI Field Conflicts (NoSuchFieldError risk):")
            for (conflict in fieldConflicts.take(15)) {
                echo("  [Field Missing] class ${formatInternalName(conflict.sourceClass)} (from ${conflict.dependencySource})")
                echo("   └── Accesses missing field: ${formatInternalName(conflict.targetOwner)}.${conflict.name} : ${conflict.desc}")
                conflictCount++
            }
            if (fieldConflicts.size > 15) {
                echo("  ... and ${fieldConflicts.size - 15} more field conflicts.")
            }
        }

        echo("\n==================================================")
        if (conflictCount > 0) {
            echo("Analysis Complete: ❌ $conflictCount potential linkage conflicts detected.")
            echo("Check for transitive dependency version mismatches.")
        } else {
            echo("Analysis Complete: ✅ No ABI conflicts detected between dependencies.")
        }
        echo("==================================================")
    }

    private data class ReferencedMember(
        val sourceClass: String,
        val targetOwner: String,
        val name: String,
        val desc: String,
        val dependencySource: String
    )

    private fun formatInternalName(name: String): String = name.replace('/', '.')

    private fun parseVersionCatalog(file: File, map: MutableMap<String, String>) {
        file.forEachLine { line ->
            val trimmed = line.trim()
            if (trimmed.contains("=") && !trimmed.startsWith("#")) {
                val parts = trimmed.split("=")
                val key = parts[0].trim()
                val value = parts[1].trim().replace("\"", "").replace("'", "")
                if (key.isNotEmpty() && value.isNotEmpty()) {
                    map[key] = value
                }
            }
        }
    }

    private fun parseBuildFile(file: File, catalog: Map<String, String>, dependencies: MutableSet<String>) {
        file.forEachLine { line ->
            val trimmed = line.trim()
            if ((trimmed.startsWith("implementation") || trimmed.startsWith("api") || trimmed.startsWith("compileOnly")) && !trimmed.contains("project(")) {
                val match = Regex("[\"']([^\"']+)[\"']").find(trimmed)
                if (match != null) {
                    val coord = match.groupValues[1]
                    if (coord.count { it == ':' } == 2) {
                        dependencies.add(coord)
                    }
                } else if (trimmed.contains("libs.")) {
                    val aliasMatch = Regex("libs\\.([^\\s\\)\\.]+)").find(trimmed)
                    if (aliasMatch != null) {
                        val alias = aliasMatch.groupValues[1].replace('_', '-')
                        val versionKey = catalog.keys.firstOrNull { it.startsWith(alias) }
                        if (versionKey != null) {
                            val coord = catalog[versionKey]
                            if (coord != null && coord.count { it == ':' } == 2) {
                                dependencies.add(coord)
                            }
                        }
                    }
                }
            }
        }
    }
}
