package com.meet.libraryinsight.core.diff

import com.meet.libraryinsight.model.*
import kotlinx.serialization.Serializable

object DiffEngine {

    @Serializable
    data class DiffReport(
        val oldLibrary: String,
        val oldVersion: String,
        val newLibrary: String,
        val newVersion: String,
        val addedClasses: List<String>,
        val removedClasses: List<String>,
        val changedClasses: List<ClassDiff>,
        val hasBreakingChanges: Boolean
    )

    @Serializable
    data class ClassDiff(
        val className: String,
        val kind: String,
        val addedConstructors: List<String> = emptyList(),
        val removedConstructors: List<String> = emptyList(),
        val addedMethods: List<String> = emptyList(),
        val removedMethods: List<String> = emptyList(),
        val changedMethods: List<MemberChange> = emptyList(),
        val addedProperties: List<String> = emptyList(),
        val removedProperties: List<String> = emptyList(),
        val changedProperties: List<MemberChange> = emptyList(),
        val generalChanges: List<String> = emptyList(),
        val isDeprecated: Boolean = false,
        val isBreaking: Boolean = false
    )

    @Serializable
    data class MemberChange(
        val name: String,
        val change: String,
        val isBreaking: Boolean
    )

    /**
     * Compares two library API indexes and returns a detailed report of changes.
     */
    fun diff(oldIndex: LibraryApiIndex, newIndex: LibraryApiIndex): DiffReport {
        val oldClassMap = oldIndex.packages.flatMap { it.classes }.associateBy { it.name }
        val newClassMap = newIndex.packages.flatMap { it.classes }.associateBy { it.name }

        val addedClasses = newClassMap.keys.filter { it !in oldClassMap.keys }
        val removedClasses = oldClassMap.keys.filter { it !in newClassMap.keys }

        val changedClasses = mutableListOf<ClassDiff>()

        for ((name, newClass) in newClassMap) {
            val oldClass = oldClassMap[name] ?: continue // Added classes are handled separately
            val classDiff = diffClass(oldClass, newClass)
            if (classDiff != null) {
                changedClasses.add(classDiff)
            }
        }

        // Removing a public class is a breaking change
        val breakingClassesRemoved = oldClassMap.filter { (name, clazz) ->
            name in removedClasses && (clazz.visibility == Visibility.PUBLIC || clazz.visibility == Visibility.PROTECTED)
        }.isNotEmpty()

        val hasBreakingChanges = breakingClassesRemoved || changedClasses.any { it.isBreaking }

        return DiffReport(
            oldLibrary = oldIndex.libraryName,
            oldVersion = oldIndex.version,
            newLibrary = newIndex.libraryName,
            newVersion = newIndex.version,
            addedClasses = addedClasses.sorted(),
            removedClasses = removedClasses.sorted(),
            changedClasses = changedClasses.sortedBy { it.className },
            hasBreakingChanges = hasBreakingChanges
        )
    }

    private fun diffClass(oldClass: ClassApi, newClass: ClassApi): ClassDiff? {
        val addedConstructors = mutableListOf<String>()
        val removedConstructors = mutableListOf<String>()
        val addedMethods = mutableListOf<String>()
        val removedMethods = mutableListOf<String>()
        val changedMethods = mutableListOf<MemberChange>()
        val addedProperties = mutableListOf<String>()
        val removedProperties = mutableListOf<String>()
        val changedProperties = mutableListOf<MemberChange>()
        val generalChanges = mutableListOf<String>()
        var isBreaking = false

        // Check general changes
        if (oldClass.visibility != newClass.visibility) {
            generalChanges.add("Visibility changed from ${oldClass.visibility} to ${newClass.visibility}")
            if (oldClass.visibility == Visibility.PUBLIC && newClass.visibility != Visibility.PUBLIC) {
                isBreaking = true
            }
        }
        if (oldClass.kind != newClass.kind) {
            generalChanges.add("Kind changed from ${oldClass.kind} to ${newClass.kind}")
            isBreaking = true
        }

        // Compare constructors
        val oldConsMap = oldClass.constructors.associateBy { it.signature }
        val newConsMap = newClass.constructors.associateBy { it.signature }
        for (sig in newConsMap.keys - oldConsMap.keys) {
            val cons = newConsMap[sig]!!
            addedConstructors.add("constructor(${cons.parameters.joinToString { it.type }})")
        }
        for (sig in oldConsMap.keys - newConsMap.keys) {
            val cons = oldConsMap[sig]!!
            removedConstructors.add("constructor(${cons.parameters.joinToString { it.type }})")
            if (cons.visibility == Visibility.PUBLIC || cons.visibility == Visibility.PROTECTED) {
                isBreaking = true
            }
        }

        // Compare methods (by signature)
        val oldMethodMap = oldClass.methods.associateBy { it.signature }
        val newMethodMap = newClass.methods.associateBy { it.signature }

        for (sig in newMethodMap.keys - oldMethodMap.keys) {
            val method = newMethodMap[sig]!!
            // Check if method was added
            addedMethods.add("fun ${method.name}(${method.parameters.joinToString { it.type }}): ${method.returnType}")
        }

        for (sig in oldMethodMap.keys - newMethodMap.keys) {
            val method = oldMethodMap[sig]!!
            // Removing a public/protected method is a breaking change
            removedMethods.add("fun ${method.name}(${method.parameters.joinToString { it.type }}): ${method.returnType}")
            if (method.visibility == Visibility.PUBLIC || method.visibility == Visibility.PROTECTED) {
                isBreaking = true
            }
        }

        // Check changed methods (where signature is the same, but modifiers/visibility changed)
        for (sig in newMethodMap.keys intersect oldMethodMap.keys) {
            val oldM = oldMethodMap[sig]!!
            val newM = newMethodMap[sig]!!
            val changes = mutableListOf<String>()
            var methodBreaking = false

            if (oldM.visibility != newM.visibility) {
                changes.add("visibility changed from ${oldM.visibility} to ${newM.visibility}")
                if (oldM.visibility == Visibility.PUBLIC && newM.visibility != Visibility.PUBLIC) {
                    methodBreaking = true
                }
            }
            if (oldM.flags.isSuspend != newM.flags.isSuspend) {
                changes.add("suspend modifier changed (was suspend: ${oldM.flags.isSuspend}, now: ${newM.flags.isSuspend})")
                methodBreaking = true
            }

            if (changes.isNotEmpty()) {
                changedMethods.add(
                    MemberChange(
                        name = "fun ${newM.name}",
                        change = changes.joinToString(", "),
                        isBreaking = methodBreaking
                    )
                )
                if (methodBreaking) isBreaking = true
            }
        }

        // Compare properties
        val oldPropMap = oldClass.properties.associateBy { it.name }
        val newPropMap = newClass.properties.associateBy { it.name }

        for (name in newPropMap.keys - oldPropMap.keys) {
            val prop = newPropMap[name]!!
            addedProperties.add("val/var ${prop.name}: ${prop.type}")
        }

        for (name in oldPropMap.keys - newPropMap.keys) {
            val prop = oldPropMap[name]!!
            removedProperties.add("val/var ${prop.name}: ${prop.type}")
            if (prop.visibility == Visibility.PUBLIC || prop.visibility == Visibility.PROTECTED) {
                isBreaking = true
            }
        }

        for (name in newPropMap.keys intersect oldPropMap.keys) {
            val oldP = oldPropMap[name]!!
            val newP = newPropMap[name]!!
            val changes = mutableListOf<String>()
            var propBreaking = false

            if (oldP.visibility != newP.visibility) {
                changes.add("visibility changed from ${oldP.visibility} to ${newP.visibility}")
                if (oldP.visibility == Visibility.PUBLIC && newP.visibility != Visibility.PUBLIC) {
                    propBreaking = true
                }
            }
            if (oldP.isMutable != newP.isMutable) {
                changes.add("mutability changed (was var: ${oldP.isMutable}, now: ${newP.isMutable})")
                if (oldP.isMutable && !newP.isMutable) {
                    // Changing var to val is a breaking change for writes!
                    propBreaking = true
                }
            }
            if (oldP.type != newP.type) {
                changes.add("type changed from ${oldP.type} to ${newP.type}")
                propBreaking = true
            }

            if (changes.isNotEmpty()) {
                changedProperties.add(
                    MemberChange(
                        name = newP.name,
                        change = changes.joinToString(", "),
                        isBreaking = propBreaking
                    )
                )
                if (propBreaking) isBreaking = true
            }
        }

        // Check if class newly deprecated
        val oldIsDep = oldClass.annotations.any { it.name == "kotlin.Deprecated" || it.name == "java.lang.Deprecated" }
        val newIsDep = newClass.annotations.any { it.name == "kotlin.Deprecated" || it.name == "java.lang.Deprecated" }
        val newlyDeprecated = !oldIsDep && newIsDep

        val hasChanges = addedConstructors.isNotEmpty() || removedConstructors.isNotEmpty() ||
                addedMethods.isNotEmpty() || removedMethods.isNotEmpty() || changedMethods.isNotEmpty() ||
                addedProperties.isNotEmpty() || removedProperties.isNotEmpty() || changedProperties.isNotEmpty() ||
                generalChanges.isNotEmpty() || newlyDeprecated

        if (!hasChanges) return null

        return ClassDiff(
            className = newClass.name,
            kind = newClass.kind.name.lowercase(),
            addedConstructors = addedConstructors,
            removedConstructors = removedConstructors,
            addedMethods = addedMethods,
            removedMethods = removedMethods,
            changedMethods = changedMethods,
            addedProperties = addedProperties,
            removedProperties = removedProperties,
            changedProperties = changedProperties,
            generalChanges = generalChanges,
            isDeprecated = newlyDeprecated,
            isBreaking = isBreaking
        )
    }

    /**
     * Helper to render the DiffReport into a user-friendly human-readable text string.
     */
    fun formatReport(report: DiffReport): String {
        val sb = StringBuilder()
        sb.append("==================================================\n")
        sb.append(" LIBRARY INSIGHT API DIFF REPORT\n")
        sb.append("==================================================\n")
        sb.append("Old: ${report.oldLibrary} (${report.oldVersion})\n")
        sb.append("New: ${report.newLibrary} (${report.newVersion})\n")
        sb.append("Breaking Changes Found: ${if (report.hasBreakingChanges) "YES 🚨" else "NO ✅"}\n")
        sb.append("==================================================\n\n")

        if (report.addedClasses.isNotEmpty()) {
            sb.append("➕ Added Classes:\n")
            report.addedClasses.forEach { sb.append("  - $it\n") }
            sb.append("\n")
        }

        if (report.removedClasses.isNotEmpty()) {
            sb.append("➖ Removed Classes:\n")
            report.removedClasses.forEach { sb.append("  - $it\n") }
            sb.append("\n")
        }

        if (report.changedClasses.isNotEmpty()) {
            sb.append("📝 Changed Classes:\n")
            for (cDiff in report.changedClasses) {
                val breakingBadge = if (cDiff.isBreaking) " [🚨 BREAKING]" else ""
                val depBadge = if (cDiff.isDeprecated) " [DEPRECATED]" else ""
                sb.append("  Class ${cDiff.className}$breakingBadge$depBadge\n")

                cDiff.generalChanges.forEach { sb.append("    * $it\n") }

                if (cDiff.addedConstructors.isNotEmpty()) {
                    sb.append("    Added Constructors:\n")
                    cDiff.addedConstructors.forEach { sb.append("      + $it\n") }
                }
                if (cDiff.removedConstructors.isNotEmpty()) {
                    sb.append("    Removed Constructors:\n")
                    cDiff.removedConstructors.forEach { sb.append("      - $it\n") }
                }
                if (cDiff.addedProperties.isNotEmpty()) {
                    sb.append("    Added Properties:\n")
                    cDiff.addedProperties.forEach { sb.append("      + $it\n") }
                }
                if (cDiff.removedProperties.isNotEmpty()) {
                    sb.append("    Removed Properties:\n")
                    cDiff.removedProperties.forEach { sb.append("      - $it\n") }
                }
                if (cDiff.changedProperties.isNotEmpty()) {
                    sb.append("    Changed Properties:\n")
                    cDiff.changedProperties.forEach {
                        val brk = if (it.isBreaking) " [🚨 BREAKING]" else ""
                        sb.append("      * property ${it.name}: ${it.change}$brk\n")
                    }
                }
                if (cDiff.addedMethods.isNotEmpty()) {
                    sb.append("    Added Methods:\n")
                    cDiff.addedMethods.forEach { sb.append("      + $it\n") }
                }
                if (cDiff.removedMethods.isNotEmpty()) {
                    sb.append("    Removed Methods:\n")
                    cDiff.removedMethods.forEach { sb.append("      - $it\n") }
                }
                if (cDiff.changedMethods.isNotEmpty()) {
                    sb.append("    Changed Methods:\n")
                    cDiff.changedMethods.forEach {
                        val brk = if (it.isBreaking) " [🚨 BREAKING]" else ""
                        sb.append("      * ${it.name}: ${it.change}$brk\n")
                    }
                }
                sb.append("\n")
            }
        }

        if (report.addedClasses.isEmpty() && report.removedClasses.isEmpty() && report.changedClasses.isEmpty()) {
            sb.append("No public API changes detected. The interfaces are identical. ✅\n")
        }

        return sb.toString()
    }
}
