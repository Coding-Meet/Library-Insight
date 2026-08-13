package com.meet.libraryinsight.parser

import com.meet.libraryinsight.model.*
import java.io.File
import java.util.Properties
import java.util.zip.ZipFile

object KlibParser {

    fun isKlib(file: File): Boolean {
        if (!file.exists() || file.isDirectory) return false
        return try {
            ZipFile(file).use { zip ->
                zip.getEntry("default/manifest") != null
            }
        } catch (e: Exception) {
            false
        }
    }

    fun parseKlib(file: File, sourcesFile: File? = null): LibraryApiIndex {
        val properties = Properties()
        val packages = mutableSetOf<String>()

        ZipFile(file).use { zip ->
            // 1. Read manifest properties
            val manifestEntry = zip.getEntry("default/manifest")
            if (manifestEntry != null) {
                zip.getInputStream(manifestEntry).use { properties.load(it) }
            }

            // 2. Discover package names from linkdata folder structure
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.name.startsWith("default/linkdata/")) {
                    val relative = entry.name.removePrefix("default/linkdata/")
                    if (relative.startsWith("package_") && relative.endsWith(".knm")) {
                        val pkgPart = relative.removePrefix("package_").removeSuffix(".knm")
                        val pkgName = pkgPart.replace('_', '.')
                        if (pkgName.isNotEmpty()) {
                            packages.add(pkgName)
                        }
                    }
                }
            }
        }

        val libName = properties.getProperty("unique_name")?.substringAfter(':') ?: file.nameWithoutExtension
        val rawTargets = properties.getProperty("targets") ?: properties.getProperty("platform") ?: "common"
        val targets = rawTargets.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { mapTargetToPlatform(it) }
            .distinct()

        // 3. If sources JAR is available, parse all classes and methods
        val classApis = mutableListOf<ClassApi>()
        if (sourcesFile != null && sourcesFile.exists()) {
            try {
                ZipFile(sourcesFile).use { zip ->
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        if (!entry.isDirectory && entry.name.endsWith(".kt")) {
                            // Extract file to temporary file and parse
                            val tempFile = File.createTempFile("klib_src_", ".kt")
                            try {
                                zip.getInputStream(entry).use { input ->
                                    tempFile.writeBytes(input.readBytes())
                                }
                                val parsedClasses = KotlinSourceParser.parse(tempFile)
                                classApis.addAll(parsedClasses.map { cls ->
                                    cls.copy(
                                        targets = targets,
                                        methods = cls.methods.map { it.copy(targets = targets) },
                                        properties = cls.properties.map { it.copy(targets = targets) },
                                        constructors = cls.constructors.map { it.copy(targets = targets) }
                                    )
                                })
                            } finally {
                                tempFile.delete()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Source parsing fallback
            }
        }

        // If no classes were parsed (e.g. no sources available), create skeleton classes for packages
        if (classApis.isEmpty()) {
            for (pkg in packages) {
                classApis.add(
                    ClassApi(
                        name = "$pkg.Placeholder",
                        simpleName = "Placeholder",
                        visibility = Visibility.PUBLIC,
                        kind = ClassKind.CLASS,
                        modifiers = emptyList(),
                        superTypes = emptyList(),
                        annotations = emptyList(),
                        constructors = emptyList(),
                        methods = emptyList(),
                        properties = emptyList(),
                        nestedClasses = emptyList(),
                        doc = "Skeleton package marker class for platform target compilation.",
                        targets = targets
                    )
                )
            }
        }

        // Group into packages
        val packageMap = classApis.groupBy { classApi ->
            val fullName = classApi.name
            if (fullName.contains('.')) {
                fullName.substringBeforeLast('.')
            } else {
                ""
            }
        }

        val packageApis = packageMap.map { (pkgName, classes) ->
            PackageApi(
                name = pkgName,
                classes = classes.sortedBy { it.name }
            )
        }.sortedBy { it.name }

        return LibraryApiIndex(
            libraryName = libName,
            version = "1.0.0",
            packages = packageApis,
            scanMode = ScanMode.SOURCE,
            targets = targets
        )
    }

    private fun mapTargetToPlatform(rawTarget: String): String {
        val lower = rawTarget.lowercase()
        return when {
            lower.contains("jvm") || lower.contains("android") -> "jvm"
            lower.contains("ios") -> "ios"
            lower.contains("js") -> "js"
            lower.contains("wasm") -> "wasm"
            lower.contains("linux") -> "linux"
            lower.contains("macos") || lower.contains("osx") -> "macos"
            lower.contains("windows") || lower.contains("mingw") -> "windows"
            else -> "common"
        }
    }
}
