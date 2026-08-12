package com.meet.libraryinsight.core

import com.meet.libraryinsight.common.ArchiveUtils
import com.meet.libraryinsight.common.Logger
import com.meet.libraryinsight.kotlin.KotlinMetadataEnricher
import com.meet.libraryinsight.kotlin.KotlinMetadataParser
import com.meet.libraryinsight.model.*
import com.meet.libraryinsight.parser.BytecodeParser
import com.meet.libraryinsight.parser.RawAnnotation
import com.meet.libraryinsight.parser.RawClassData
import java.io.File

object LibraryAnalyzer {

    private val logger = Logger

    /** JVM descriptor of kotlin.DslMarker annotation. */
    private const val DSL_MARKER_DESC = "Lkotlin/DslMarker;"

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
        if (com.meet.libraryinsight.parser.KlibParser.isKlib(file)) {
            return com.meet.libraryinsight.parser.KlibParser.parseKlib(file, sourcesFile).copy(
                libraryName = libraryName,
                version = version
            )
        }
        val classBytesMap = ArchiveUtils.extractClasses(file)

        // --- Pass 1: Parse all raw class data ---
        val rawClasses = mutableListOf<RawClassData>()
        for ((_, bytes) in classBytesMap) {
            try {
                rawClasses.add(BytecodeParser.parseClass(bytes))
            } catch (e: Exception) {
                logger.warn("Failed to parse class bytes: ${e.message}")
            }
        }

        // --- Pass 1b: Build @DslMarker annotation descriptor set ---
        // An annotation class is "DslMarker-annotated" if its own visible/invisible annotations
        // include Lkotlin/DslMarker;. We build a set of their descriptors for O(1) lookup.
        val dslMarkerAnnotationDescs: Set<String> = rawClasses
            .filter { raw ->
                raw.annotations.any { it.desc == DSL_MARKER_DESC }
            }
            .map { raw -> "L${raw.internalName};" }
            .toSet()

        logger.info("Found ${dslMarkerAnnotationDescs.size} @DslMarker annotation types: $dslMarkerAnnotationDescs")

        // --- Pass 1c: Enrich RawAnnotations with meta-annotations for @DslMarker ---
        // Re-annotate each raw class's annotation list with metaAnnotations populated.
        val enrichedRawClasses = rawClasses.map { rawClass ->
            rawClass.copy(
                annotations = rawClass.annotations.map { annotation ->
                    if (annotation.desc in dslMarkerAnnotationDescs) {
                        annotation.copy(
                            metaAnnotations = listOf(RawAnnotation(DSL_MARKER_DESC, emptyMap()))
                        )
                    } else {
                        annotation
                    }
                }
            )
        }

        // --- Pass 2: Build ClassApis + collect typeAliases ---
        val classApis = mutableListOf<ClassApi>()
        // Map: packageName -> list of type aliases found in FileFacade classes in that package
        val packageTypeAliases = mutableMapOf<String, MutableList<TypeAliasApi>>()

        for (rawClass in enrichedRawClasses) {
            try {
                val metadata = KotlinMetadataParser.parseMetadata(rawClass)

                val classApi = if (metadata != null) {
                    // Extract type aliases from FileFacade/MultiFileClassPart
                    val aliases = KotlinMetadataEnricher.extractTypeAliases(rawClass, metadata)
                    if (aliases.isNotEmpty()) {
                        val pkgName = rawClass.name.substringBeforeLast('.')
                        packageTypeAliases.getOrPut(pkgName) { mutableListOf() }.addAll(aliases)
                    }
                    KotlinMetadataEnricher.enrich(rawClass, metadata)
                } else {
                    KotlinMetadataEnricher.fallbackToJava(rawClass)
                }

                classApis.add(classApi)
            } catch (e: Exception) {
                logger.warn("Failed to enrich class ${rawClass.name}: ${e.message}")
            }
        }

        // --- Pass 3: Enrich from sources if provided ---
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

        // Centralized annotation clean-up to remove compiler-internal metadata
        val cleanedClassApis = finalClassApis.map { cleanClassAnnotations(it) }

        // Scan for guide/README markdown examples and associate them
        val markdownTexts = extractMarkdownContents(sourcesFile)
        val enrichedClassApis = associateMarkdownExamples(cleanedClassApis, markdownTexts)

        // Group by package and attach typeAliases
        val packageMap = enrichedClassApis.groupBy { classApi ->
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
                classes = classes.sortedBy { it.name },
                typeAliases = packageTypeAliases[pkgName]?.distinctBy { it.name } ?: emptyList()
            )
        }.sortedBy { it.name }

        logger.info("Analysis complete: ${packages.size} packages, ${enrichedClassApis.size} classes, " +
                "${packageTypeAliases.values.sumOf { it.size }} type aliases")

        return LibraryApiIndex(
            libraryName = libraryName,
            version = version,
            packages = packages,
            targets = listOf("jvm")
        )
    }

    private fun extractMarkdownContents(sourcesFile: File?): List<String> {
        val markdownTexts = mutableListOf<String>()
        
        // 1. Try extracting from local directory if the workspace is local
        val localDocsDirs = listOf(File("."), File(".."), File("./docs"), File("../docs"))
        for (dir in localDocsDirs) {
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    if (file.isFile && file.extension == "md") {
                        try {
                            markdownTexts.add(file.readText(Charsets.UTF_8))
                        } catch (e: Exception) {}
                    }
                }
            }
        }

        // 2. Extract from the sourcesFile directory if provided
        if (sourcesFile != null && sourcesFile.exists() && sourcesFile.isDirectory) {
            sourcesFile.walkTopDown().forEach { file ->
                if (file.isFile && file.extension == "md") {
                    try {
                        markdownTexts.add(file.readText(Charsets.UTF_8))
                    } catch (e: Exception) {}
                }
            }
        }

        // 3. Extract from the sourcesFile JAR/ZIP if provided
        if (sourcesFile != null && sourcesFile.exists() && 
            (sourcesFile.name.endsWith(".jar") || sourcesFile.name.endsWith(".zip") || sourcesFile.name.endsWith(".aar"))) {
            try {
                java.util.zip.ZipFile(sourcesFile).use { zip ->
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        if (!entry.isDirectory && entry.name.endsWith(".md", ignoreCase = true)) {
                            zip.getInputStream(entry).use { stream ->
                                markdownTexts.add(String(stream.readBytes(), Charsets.UTF_8))
                            }
                        }
                    }
                }
            } catch (e: Exception) {}
        }
        return markdownTexts
    }

    private fun associateMarkdownExamples(classes: List<ClassApi>, markdownTexts: List<String>): List<ClassApi> {
        if (markdownTexts.isEmpty()) return classes

        val codeBlockRegex = Regex("```(?:kotlin|java)\\n([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
        val allExamples = markdownTexts.flatMap { text ->
            codeBlockRegex.findAll(text).map { it.groupValues[1].trim() }
        }.filter { it.isNotEmpty() }

        if (allExamples.isEmpty()) return classes

        return classes.map { clazz ->
            val matchingExamples = allExamples.filter { example ->
                example.contains(clazz.simpleName, ignoreCase = true) || 
                example.contains(clazz.name.replace('/', '.'), ignoreCase = true)
            }
            if (matchingExamples.isNotEmpty()) {
                clazz.copy(documentationExamples = clazz.documentationExamples + matchingExamples)
            } else {
                clazz
            }
        }
    }

    private fun cleanClassAnnotations(clazz: ClassApi): ClassApi {
        val targets = clazz.targets.ifEmpty { listOf("jvm") }
        return clazz.copy(
            targets = targets,
            annotations = clazz.annotations.filter { isUserFacingAnnotation(it.name) },
            constructors = clazz.constructors.map { cons ->
                cons.copy(
                    targets = cons.targets.ifEmpty { targets },
                    annotations = cons.annotations.filter { isUserFacingAnnotation(it.name) },
                    parameters = cons.parameters.map { param ->
                        param.copy(annotations = param.annotations.filter { isUserFacingAnnotation(it.name) })
                    }
                )
            },
            methods = clazz.methods.map { method ->
                method.copy(
                    targets = method.targets.ifEmpty { targets },
                    annotations = method.annotations.filter { isUserFacingAnnotation(it.name) },
                    parameters = method.parameters.map { param ->
                        param.copy(annotations = param.annotations.filter { isUserFacingAnnotation(it.name) })
                    }
                )
            },
            properties = clazz.properties.map { prop ->
                prop.copy(
                    targets = prop.targets.ifEmpty { targets },
                    annotations = prop.annotations.filter { isUserFacingAnnotation(it.name) }
                )
            }
        )
    }

    private fun isUserFacingAnnotation(name: String): Boolean {
        val normalized = name.replace('/', '.')
        return normalized != "kotlin.Metadata" && !normalized.startsWith("kotlin.jvm.internal")
    }

    /**
     * Merges multiple platform target indices together into a single unified index.
     */
    fun mergeIndices(indices: List<LibraryApiIndex>): LibraryApiIndex {
        if (indices.isEmpty()) throw IllegalArgumentException("No indices to merge")
        if (indices.size == 1) return indices.first()

        val first = indices.first()
        val allPackages = indices.flatMap { it.packages }

        val mergedPackages = allPackages.groupBy { it.name }.map { (pkgName, pkgList) ->
            val allClasses = pkgList.flatMap { it.classes }
            val mergedClasses = allClasses.groupBy { it.name }.map { (className, classList) ->
                val representative = classList.first()
                
                val allMethods = classList.flatMap { it.methods }
                val mergedMethods = allMethods.groupBy { it.signature }.map { (_, methodList) ->
                    val repMethod = methodList.first()
                    val unionTargets = methodList.flatMap { it.targets }.distinct().sorted()
                    repMethod.copy(targets = unionTargets)
                }

                val allProperties = classList.flatMap { it.properties }
                val mergedProperties = allProperties.groupBy { it.name }.map { (_, propList) ->
                    val repProp = propList.first()
                    val unionTargets = propList.flatMap { it.targets }.distinct().sorted()
                    repProp.copy(targets = unionTargets)
                }

                val allConstructors = classList.flatMap { it.constructors }
                val mergedConstructors = allConstructors.groupBy { it.signature }.map { (_, consList) ->
                    val repCons = consList.first()
                    val unionTargets = consList.flatMap { it.targets }.distinct().sorted()
                    repCons.copy(targets = unionTargets)
                }

                val unionTargets = classList.flatMap { it.targets }.distinct().sorted()
                representative.copy(
                    methods = mergedMethods,
                    properties = mergedProperties,
                    constructors = mergedConstructors,
                    targets = unionTargets
                )
            }

            val allTypeAliases = pkgList.flatMap { it.typeAliases }
            val mergedTypeAliases = allTypeAliases.distinctBy { it.name }

            PackageApi(
                name = pkgName,
                classes = mergedClasses.sortedBy { it.name },
                typeAliases = mergedTypeAliases
            )
        }

        val unionTargets = indices.flatMap { it.targets }.distinct().sorted()
        val scanMode = if (indices.any { it.scanMode == ScanMode.SOURCE }) ScanMode.SOURCE else ScanMode.BYTECODE

        return LibraryApiIndex(
            libraryName = first.libraryName,
            version = first.version,
            packages = mergedPackages.sortedBy { it.name },
            scanMode = scanMode,
            targets = unionTargets
        )
    }
}
