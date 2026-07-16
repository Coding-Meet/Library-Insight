package com.meet.libraryinsight.core

import com.meet.libraryinsight.common.ArchiveUtils
import com.meet.libraryinsight.kotlin.KotlinMetadataEnricher
import com.meet.libraryinsight.kotlin.KotlinMetadataParser
import com.meet.libraryinsight.model.ClassApi
import com.meet.libraryinsight.model.LibraryApiIndex
import com.meet.libraryinsight.model.PackageApi
import com.meet.libraryinsight.parser.BytecodeParser
import java.io.File

object LibraryAnalyzer {

    /**
     * Scans and parses a library input (JAR, AAR, or Directory) and constructs a complete [LibraryApiIndex].
     */
    fun analyze(file: File, libraryName: String = file.nameWithoutExtension, version: String = "1.0.0"): LibraryApiIndex {
        val classBytesMap = ArchiveUtils.extractClasses(file)
        val classApis = mutableListOf<ClassApi>()

        for ((_, bytes) in classBytesMap) {
            try {
                // 1. Parse raw bytecode structures
                val rawClass = BytecodeParser.parseClass(bytes)

                // 2. Parse Kotlin metadata if available
                val metadata = KotlinMetadataParser.parseMetadata(rawClass)

                // 3. Build enriched ClassApi
                val classApi = if (metadata != null) {
                    KotlinMetadataEnricher.enrich(rawClass, metadata)
                } else {
                    KotlinMetadataEnricher.fallbackToJava(rawClass)
                }
                
                classApis.add(classApi)
            } catch (e: Exception) {
                // Skip or log corrupted classes
            }
        }

        // Group by package
        val packageMap = classApis.groupBy { classApi ->
            val fullName = classApi.name
            if (fullName.contains('.')) {
                fullName.substringBeforeLast('.')
            } else {
                "" // Default package
            }
        }

        val packages = packageMap.map { (pkgName, classes) ->
            PackageApi(
                name = pkgName,
                classes = classes.sortedBy { it.name }
            )
        }.sortedBy { it.name }

        return LibraryApiIndex(
            libraryName = libraryName,
            version = version,
            packages = packages
        )
    }
}
