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
     * Optionally enriches classes and members with comments and source snippets from a sources file/JAR.
     */
    fun analyze(
        file: File,
        libraryName: String = file.nameWithoutExtension,
        version: String = "1.0.0",
        sourcesFile: File? = null
    ): LibraryApiIndex {
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

        // 4. Enrich from sources if provided
        val finalClassApis = if (sourcesFile != null) {
            val sourcesMap = ArchiveUtils.extractSources(sourcesFile)
            classApis.map { clazz ->
                val lookupKey = when {
                    clazz.name.endsWith("Kt") && !sourcesMap.containsKey(clazz.name) -> clazz.name.removeSuffix("Kt")
                    clazz.name.contains('$') -> clazz.name.substringBefore('$')
                    else -> clazz.name
                }
                val sourceText = sourcesMap[lookupKey]
                if (sourceText != null) {
                    val parsed = SourcesParser.parse(sourceText)
                    val enrichedMethods = clazz.methods.map { method ->
                        val overloads = parsed.methods[method.name] ?: emptyList()
                        val match = overloads.firstOrNull { it.paramCount == method.parameters.size } ?: overloads.firstOrNull()
                        if (match != null) {
                            method.copy(doc = match.doc, sourceCode = match.sourceCode)
                        } else {
                            method
                        }
                    }
                    val enrichedProperties = clazz.properties.map { prop ->
                        val match = parsed.properties[prop.name]
                        if (match != null) {
                            prop.copy(doc = match.doc)
                        } else {
                            prop
                        }
                    }
                    clazz.copy(
                        doc = parsed.classes[clazz.simpleName]?.doc,
                        sourceCode = parsed.classSource,
                        methods = enrichedMethods,
                        properties = enrichedProperties
                    )
                } else {
                    clazz
                }
            }
        } else {
            classApis
        }

        // Group by package
        val packageMap = finalClassApis.groupBy { classApi ->
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
