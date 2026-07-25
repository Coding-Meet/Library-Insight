package com.meet.libraryinsight.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.meet.libraryinsight.common.MavenResolver
import com.meet.libraryinsight.core.LibraryAnalyzer
import com.meet.libraryinsight.core.diff.DiffEngine
import java.io.File

class CheckCompatCommand : CliktCommand(
    name = "check-compat",
    help = "Verify Semantic Versioning compliance by comparing code differences against version numbers."
) {
    val oldSource by argument(help = "Old version coordinates or JAR path")
    val newSource by argument(help = "New version coordinates or JAR path")

    override fun run() {
        try {
            val oldIndex = getIndexForSource(oldSource)
            val newIndex = getIndexForSource(newSource)

            val oldVer = SemVer.parse(oldIndex.version)
            val newVer = SemVer.parse(newIndex.version)

            if (oldVer == null || newVer == null) {
                echo("Error: Unable to parse Semantic Versioning from version strings: '${oldIndex.version}' or '${newIndex.version}'.", err = true)
                return
            }

            echo("==================================================")
            echo("     API Compatibility & SemVer Compliance")
            echo("==================================================")
            echo("Old version: ${oldIndex.version}")
            echo("New version: ${newIndex.version}")
            echo("==================================================\n")

            val report = DiffEngine.diff(oldIndex, newIndex)
            val versionBumpedMajor = newVer.major > oldVer.major
            val versionBumpedMinor = newVer.minor > oldVer.minor

            var isCompliant = true
            val issues = mutableListOf<String>()

            // 1. Breaking change check
            if (report.hasBreakingChanges) {
                if (!versionBumpedMajor) {
                    isCompliant = false
                    issues.add("❌ API Breaking Change detected but MAJOR version was not incremented! (Old: ${oldIndex.version}, New: ${newIndex.version})")
                }
            }

            // 2. Added public APIs check
            val hasAddedApis = report.addedClasses.isNotEmpty() || 
                               report.changedClasses.any { it.addedMethods.isNotEmpty() || it.addedProperties.isNotEmpty() }
            if (hasAddedApis) {
                if (!versionBumpedMajor && !versionBumpedMinor) {
                    // Added classes/methods require at least minor bump if not major bump
                    isCompliant = false
                    issues.add("⚠️ New APIs added but MINOR version was not incremented! (Old: ${oldIndex.version}, New: ${newIndex.version})")
                }
            }

            if (isCompliant) {
                echo("✅ SemVer Compliant: Version bump matches API modifications.")
                if (report.hasBreakingChanges) {
                    echo("  - Major bump correctly applied for breaking changes.")
                } else if (hasAddedApis) {
                    echo("  - Minor bump correctly applied for added APIs.")
                } else {
                    echo("  - Patch/No-change bump correctly applied.")
                }
            } else {
                echo("🚨 SemVer Violation: Version bump does not match API changes!")
                issues.forEach { echo("  $it") }
            }
            echo("\n==================================================")

        } catch (e: Exception) {
            echo("Error performing compatibility checks: ${e.message}", err = true)
        }
    }

    private fun getIndexForSource(source: String): com.meet.libraryinsight.model.LibraryApiIndex {
        return if (MavenResolver.isCoordinate(source)) {
            val resolved = MavenResolver.resolve(source)
            val parts = source.split(':')
            LibraryAnalyzer.analyze(resolved.binaryFile, parts[1], parts[2], resolved.sourcesFile)
        } else {
            val file = File(source)
            if (!file.exists()) {
                throw IllegalArgumentException("Source path '$source' does not exist.")
            }
            LibraryAnalyzer.analyze(file, file.nameWithoutExtension, "1.0.0", null)
        }
    }

    data class SemVer(val major: Int, val minor: Int, val patch: Int) {
        companion object {
            fun parse(version: String): SemVer? {
                val cleaned = version.substringBefore('-').substringBefore('+')
                val parts = cleaned.split('.')
                if (parts.size < 2) return null
                val major = parts[0].toIntOrNull() ?: return null
                val minor = parts[1].toIntOrNull() ?: return null
                val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
                return SemVer(major, minor, patch)
            }
        }
    }
}
