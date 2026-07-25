package com.meet.libraryinsight.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.meet.libraryinsight.common.MavenResolver
import com.meet.libraryinsight.core.LibraryAnalyzer
import com.meet.libraryinsight.model.LibraryApiIndex
import java.io.File

class AuditCommand : CliktCommand(
    name = "audit",
    help = "Scan build.gradle.kts dependencies and audit them for deprecated APIs and version issues."
) {
    override fun run() {
        echo("==================================================")
        echo("      Library Insight Dependency Audit")
        echo("==================================================")

        val catalogFile = File("gradle/libs.versions.toml")

        // 1. Parse Version Catalog (libs.versions.toml) if present
        val catalogLibraries = mutableMapOf<String, String>()
        if (catalogFile.exists()) {
            echo("Detected Gradle Version Catalog at gradle/libs.versions.toml")
            parseVersionCatalog(catalogFile, catalogLibraries)
        }

        // 2. Scan recursively for all build files
        val buildFiles = File(".").walkTopDown()
            .filter { it.isFile && (it.name == "build.gradle.kts" || it.name == "build.gradle") }
            .filter { !it.absolutePath.contains("/build/") && !it.absolutePath.contains("/.gradle/") && !it.absolutePath.contains("/.git/") }
            .toList()

        if (buildFiles.isEmpty()) {
            echo("Error: No build.gradle or build.gradle.kts found in the current directory or its subdirectories.", err = true)
            return
        }

        val dependencies = mutableSetOf<String>()
        echo("Scanning ${buildFiles.size} Gradle build file(s)...")
        for (buildFile in buildFiles) {
            parseBuildFile(buildFile, catalogLibraries, dependencies)
        }

        if (dependencies.isEmpty()) {
            echo("No dependencies detected in build files.")
            return
        }

        echo("\nFound ${dependencies.size} dependencies to audit:")
        dependencies.sorted().forEach { echo("  - $it") }
        echo("")

        // 3. Scan each dependency and audit API deprecations
        var auditedCount = 0
        var totalDeprecations = 0

        for (coord in dependencies) {
            echo("--------------------------------------------------")
            echo("Auditing $coord...")
            try {
                val resolved = MavenResolver.resolve(coord) { /* silent progress */ }
                val parts = coord.split(':')
                val index = LibraryAnalyzer.analyze(resolved.binaryFile, parts[1], parts[2], resolved.sourcesFile)
                
                val classes = index.packages.flatMap { it.classes }
                var deprecatedClasses = 0
                var deprecatedMethods = 0
                var deprecatedProperties = 0

                for (clazz in classes) {
                    val isClassDeprecated = clazz.annotations.any { isDeprecatedAnnotation(it.name) }
                    if (isClassDeprecated) {
                        deprecatedClasses++
                    }
                    
                    deprecatedMethods += clazz.methods.count { m ->
                        m.annotations.any { isDeprecatedAnnotation(it.name) }
                    }
                    
                    deprecatedProperties += clazz.properties.count { p ->
                        p.annotations.any { isDeprecatedAnnotation(it.name) }
                    }
                }

                val depSum = deprecatedClasses + deprecatedMethods + deprecatedProperties
                totalDeprecations += depSum
                auditedCount++

                echo("  - Total classes: ${classes.size}")
                echo("  - Status: ${if (depSum > 0) "⚠️  Deprecations detected" else "✅  Clean (0 deprecations)"}")
                if (depSum > 0) {
                    echo("    * Deprecated Classes    : $deprecatedClasses")
                    echo("    * Deprecated Methods    : $deprecatedMethods")
                    echo("    * Deprecated Properties : $deprecatedProperties")
                }
            } catch (e: Exception) {
                echo("  - Error: Failed to resolve or analyze dependency (${e.message})", err = true)
            }
        }

        echo("==================================================")
        echo("Audit Summary: Scanned $auditedCount libraries successfully.")
        echo("Total Deprecated APIs found: $totalDeprecations")
        echo("==================================================")
    }

    private fun isDeprecatedAnnotation(name: String): Boolean {
        val normalized = name.replace('.', '/').lowercase()
        return normalized.contains("deprecated")
    }

    private fun parseVersionCatalog(file: File, catalog: MutableMap<String, String>) {
        val versions = mutableMapOf<String, String>()
        var currentSection = ""
        
        file.forEachLine { line ->
            val cleaned = line.trim()
            if (cleaned.startsWith('#') || cleaned.isEmpty()) return@forEachLine
            
            if (cleaned.startsWith('[') && cleaned.endsWith(']')) {
                currentSection = cleaned.substring(1, cleaned.length - 1).trim().lowercase()
                return@forEachLine
            }

            val parts = cleaned.split('=', limit = 2)
            if (parts.size != 2) return@forEachLine
            val key = parts[0].trim()
            val value = parts[1].trim()

            when (currentSection) {
                "versions" -> {
                    val verVal = value.replace("\"", "")
                    versions[key] = verVal
                }
                "libraries" -> {
                    // Extract module and version info
                    // Format A: "group:artifact:version"
                    // Format B: { module = "group:artifact", version.ref = "versionName" }
                    // Format C: { group = "group", name = "artifact", version = "version" }
                    if (value.startsWith('{') && value.endsWith('}')) {
                        val properties = value.substring(1, value.length - 1).split(',')
                            .associate { prop ->
                                val kv = prop.split('=', limit = 2)
                                kv[0].trim() to kv[1].trim().replace("\"", "")
                            }
                        
                        val module = properties["module"]
                        val group = properties["group"]
                        val name = properties["name"]
                        val versionVal = properties["version"]
                        val versionRef = properties["version.ref"]

                        val resolvedVersion = versionVal ?: versions[versionRef] ?: "1.0.0"
                        if (module != null) {
                            catalog[key] = "$module:$resolvedVersion"
                        } else if (group != null && name != null) {
                            catalog[key] = "$group:$name:$resolvedVersion"
                        }
                    } else {
                        val coord = value.replace("\"", "")
                        catalog[key] = coord
                    }
                }
            }
        }
    }

    private fun parseBuildFile(file: File, catalog: Map<String, String>, dependencies: MutableSet<String>) {
        file.forEachLine { line ->
            val cleaned = line.trim()
            if (cleaned.startsWith("//") || cleaned.isEmpty()) return@forEachLine

            // 1. Direct coordinate declaration (e.g. "testImplementation"("group:artifact:version") or api("group:artifact:version"))
            val directMatch = Regex("""(?:implementation|api|compileOnly|runtimeOnly|testImplementation|"[a-zA-Z]+")\s*\(?\s*["']([^"']+)["']\s*\)?""")
                .find(cleaned)
            if (directMatch != null) {
                val coord = directMatch.groupValues[1]
                if (coord.split(':').size == 3) {
                    dependencies.add(coord)
                    return@forEachLine
                }
            }

            // 2. Catalog dependencies (e.g. implementation(libs.retrofit))
            val catalogMatch = Regex("""(?:implementation|api|compileOnly|runtimeOnly|testImplementation|"[a-zA-Z]+")\s*\(?\s*libs\.([a-zA-Z0-9.\-_]+)\s*\)?""")
                .find(cleaned)
            if (catalogMatch != null) {
                val alias = catalogMatch.groupValues[1].replace(".", "").replace("-", "").lowercase()
                // Find matching catalog reference by converting keys to flat lowercase names
                val resolved = catalog.entries.firstOrNull { 
                    it.key.replace("-", "").lowercase() == alias
                }?.value
                if (resolved != null) {
                    dependencies.add(resolved)
                }
            }
        }
    }
}
