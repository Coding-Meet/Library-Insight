package com.meet.libraryinsight.common

import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readRawBytes
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node

object MavenResolver {

    private val client = HttpClient(Java) {
        followRedirects = true
    }

    data class CentralSearchResult(
        val coordinate: String,
        val groupId: String,
        val artifactId: String,
        val latestVersion: String,
        val repository: String
    )

    /**
     * Queries Maven Central Solr API for matching coordinates.
     */
    fun searchCentral(query: String, rows: Int = 10): List<CentralSearchResult> {
        try {
            val responseText = runBlocking {
                client.get("https://search.maven.org/solrsearch/select?q=$query&rows=$rows&wt=json").bodyAsText()
            }
            val json = Json.parseToJsonElement(responseText).jsonObject
            val responseObj = json["response"]?.jsonObject ?: return emptyList()
            val docs = responseObj["docs"]?.jsonArray ?: return emptyList()

            return docs.map { docElement ->
                val doc = docElement.jsonObject
                val id = doc["id"]?.jsonPrimitive?.content ?: ""
                val g = doc["g"]?.jsonPrimitive?.content ?: ""
                val a = doc["a"]?.jsonPrimitive?.content ?: ""
                val latest = doc["latestVersion"]?.jsonPrimitive?.content ?: ""
                val repo = doc["repositoryId"]?.jsonPrimitive?.content ?: "central"
                CentralSearchResult(id, g, a, latest, repo)
            }
        } catch (e: Exception) {
            return emptyList()
        }
    }

    data class ResolvedArtifact(
        val binaryFile: File,
        val sourcesFile: File?
    )

    private val REPOSITORIES = listOf(
        "https://repo1.maven.org/maven2/",
        "https://dl.google.com/dl/android/maven2/",
    )

    val cacheDir: File
        get() {
            val localBuild = File("build/library-insight/cache")
            return if (File("build").exists() || File("settings.gradle").exists() || File("settings.gradle.kts").exists()) {
                localBuild
            } else {
                File(System.getProperty("user.home"), ".library-insight/cache")
            }
        }

    /**
     * Checks if the coordinate is in the format groupId:artifactId:version
     */
    fun isCoordinate(input: String): Boolean {
        val parts = input.split(':')
        return parts.size == 3 && parts.all { it.trim().isNotEmpty() }
    }

    /**
     * Resolves constituent library coordinates from a Maven Bill of Materials (BOM) coordinate.
     * Parses the BOM's POM XML file and extracts all managed dependencies in <dependencyManagement>.
     */
    fun resolveBomCoordinates(
        coordinate: String,
        customRepos: List<String> = emptyList(),
        progressReporter: (String) -> Unit = {}
    ): List<String> {
        val parts = coordinate.split(':')
        if (parts.size != 3) return emptyList()
        val groupId = parts[0].trim()
        val artifactId = parts[1].trim()
        val version = parts[2].trim()

        val groupPath = groupId.replace('.', '/')
        val basePath = "$groupPath/$artifactId/$version"
        val pomName = "$artifactId-$version.pom"
        val cachedPom = File(cacheDir, "$basePath/$pomName")

        progressReporter("Resolving BOM POM for $coordinate...")

        var pomText: String? = null
        if (cachedPom.exists()) {
            pomText = cachedPom.readText(Charsets.UTF_8)
        } else {
            val userHome = System.getProperty("user.home")
            if (userHome != null) {
                val gradleCacheDir = File(userHome, ".gradle/caches/modules-2/files-2.1/$groupId/$artifactId/$version")
                if (gradleCacheDir.exists()) {
                    gradleCacheDir.walkBottomUp().filter { it.isFile && it.name.endsWith(".pom") }.firstOrNull()?.let { pomFile ->
                        val content = pomFile.readText(Charsets.UTF_8)
                        pomText = content
                        try {
                            cachedPom.parentFile.mkdirs()
                            cachedPom.writeText(content, Charsets.UTF_8)
                        } catch (_: Exception) {}
                    }
                }
            }

            if (pomText == null) {
                val allRepos = customRepos + REPOSITORIES
                for (repo in allRepos) {
                    val repoUrl = repo.removeSuffix("/")
                    val pomUrl = "$repoUrl/$basePath/$pomName"
                    if (downloadFile(pomUrl, cachedPom)) {
                        pomText = cachedPom.readText(Charsets.UTF_8)
                        break
                    }
                }
            }
        }

        val text = pomText ?: return emptyList()
        if (text.isBlank()) return emptyList()

        return parseBomPom(text, artifactId, version, progressReporter)
    }

    private fun parseBomPom(
        pomText: String,
        artifactId: String,
        bomVersion: String,
        progressReporter: (String) -> Unit
    ): List<String> {
        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = false
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(pomText.byteInputStream(Charsets.UTF_8))
            doc.documentElement.normalize()

            val properties = mutableMapOf<String, String>()
            properties["project.version"] = bomVersion
            properties["version"] = bomVersion

            val propertiesNodes = doc.getElementsByTagName("properties")
            if (propertiesNodes.length > 0) {
                val propsElem = propertiesNodes.item(0) as? Element
                if (propsElem != null) {
                    val childNodes = propsElem.childNodes
                    for (i in 0 until childNodes.length) {
                        val node = childNodes.item(i)
                        if (node.nodeType == Node.ELEMENT_NODE) {
                            properties[node.nodeName.trim()] = node.textContent.trim()
                        }
                    }
                }
            }

            val packagingNodes = doc.getElementsByTagName("packaging")
            val packaging = if (packagingNodes.length > 0) packagingNodes.item(0).textContent.trim().lowercase() else "jar"
            val isExplicitBom = artifactId.lowercase().endsWith("-bom") || artifactId.lowercase().contains("bom")

            if (packaging != "pom" && !isExplicitBom) {
                return emptyList()
            }

            val depMgmtNodes = doc.getElementsByTagName("dependencyManagement")
            if (depMgmtNodes.length == 0) return emptyList()

            val depMgmtElem = depMgmtNodes.item(0) as Element
            val dependencyNodes = depMgmtElem.getElementsByTagName("dependency")
            val coordinates = mutableListOf<String>()

            for (i in 0 until dependencyNodes.length) {
                val depNode = dependencyNodes.item(i)
                if (depNode is Element) {
                    val gNode = depNode.getElementsByTagName("groupId").item(0)
                    val aNode = depNode.getElementsByTagName("artifactId").item(0)
                    val vNode = depNode.getElementsByTagName("version").item(0)
                    val typeNode = depNode.getElementsByTagName("type").item(0)
                    val scopeNode = depNode.getElementsByTagName("scope").item(0)

                    val type = typeNode?.textContent?.trim()
                    val scope = scopeNode?.textContent?.trim()

                    if (type == "pom" && scope == "import") continue

                    val g = gNode?.textContent?.trim() ?: ""
                    val a = aNode?.textContent?.trim() ?: ""
                    var v = vNode?.textContent?.trim() ?: bomVersion

                    if (v.startsWith("\${") && v.endsWith("}")) {
                        val propKey = v.substring(2, v.length - 1).trim()
                        v = properties[propKey] ?: bomVersion
                    }

                    if (g.isNotEmpty() && a.isNotEmpty() && v.isNotEmpty()) {
                        coordinates.add("$g:$a:$v")
                    }
                }
            }

            val distinctCoords = coordinates.distinct()
            if (distinctCoords.isNotEmpty()) {
                progressReporter("Parsed ${distinctCoords.size} managed libraries from BOM dependencyManagement.")
            }
            return distinctCoords
        } catch (e: Exception) {
            Logger.warn("Failed to parse BOM POM XML: ${e.message}")
            return emptyList()
        }
    }

    /**
     * Resolves and downloads the library binary and sources from repositories.
     * Caches the files to avoid redundant downloads.
     */
    /**
     * Resolves KMP target split coordinates (e.g. io.ktor:ktor-client-core-iosarm64:3.0.0)
     * from the root KMP library coordinate (e.g. io.ktor:ktor-client-core:3.0.0).
     */
    fun resolveKmpCoordinates(
        coordinate: String,
        customRepos: List<String> = emptyList(),
        progressReporter: (String) -> Unit = {}
    ): List<String> {
        val parts = coordinate.split(':')
        if (parts.size != 3) return emptyList()
        val groupId = parts[0].trim()
        val artifactId = parts[1].trim()
        val version = parts[2].trim()

        val groupPath = groupId.replace('.', '/')
        val basePath = "$groupPath/$artifactId/$version"
        val moduleName = "$artifactId-$version.module"
        val cachedModule = File(cacheDir, "$basePath/$moduleName")

        progressReporter("Resolving KMP coordinates for $coordinate...")
        
        // 1. Try to download or use cached .module file
        var moduleText: String? = null
        if (cachedModule.exists()) {
            moduleText = cachedModule.readText(Charsets.UTF_8)
        } else {
            val allRepos = customRepos + REPOSITORIES
            for (repo in allRepos) {
                val repoUrl = repo.removeSuffix("/")
                val moduleUrl = "$repoUrl/$basePath/$moduleName"
                if (downloadFile(moduleUrl, cachedModule)) {
                    moduleText = cachedModule.readText(Charsets.UTF_8)
                    break
                }
            }
        }

        if (moduleText != null) {
            try {
                val json = Json.parseToJsonElement(moduleText).jsonObject
                val variants = json["variants"]?.jsonArray
                if (variants != null) {
                    val coords = mutableListOf<String>()
                    for (variantElement in variants) {
                        val variant = variantElement.jsonObject
                        val availableAt = variant["available-at"]?.jsonObject
                        if (availableAt != null) {
                            val g = availableAt["group"]?.jsonPrimitive?.content ?: groupId
                            val m = availableAt["module"]?.jsonPrimitive?.content
                            val v = availableAt["version"]?.jsonPrimitive?.content ?: version
                            if (m != null) {
                                coords.add("$g:$m:$v")
                            }
                        }
                    }
                    if (coords.isNotEmpty()) {
                        val distinctCoords = coords.distinct()
                        progressReporter("Resolved ${distinctCoords.size} KMP platform variants from Gradle module metadata.")
                        return distinctCoords
                    }
                }
            } catch (e: Exception) {
                Logger.warn("Failed to parse Gradle module metadata: ${e.message}")
            }
        }

        // 2. Fallback: Search Maven Central Solr API for artifacts in the same group starting with $artifactId-
        progressReporter("Fallback: Querying Maven Central for variants of $groupId:$artifactId...")
        val searchResults = searchCentral("g:\"$groupId\" AND a:\"$artifactId-*\"", rows = 50)
        val matches = searchResults.filter { it.groupId == groupId && it.artifactId.startsWith("$artifactId-") }
        if (matches.isNotEmpty()) {
            val coords = matches.map { "${it.groupId}:${it.artifactId}:$version" }
            progressReporter("Resolved ${coords.size} KMP platform variants from Maven Central search.")
            return coords
        }

        // 3. Fallback: Suffix probing
        val commonSuffixes = listOf(
            "jvm", "js", "wasm-js", "android", "iosarm64", "iosx64",
            "macosarm64", "macosx64", "linuxx64", "mingwx64"
        )
        progressReporter("Fallback: Probing common target suffixes...")
        val coords = mutableListOf<String>()
        // Let's add standard JVM variant if not already resolved
        coords.add("$groupId:$artifactId-jvm:$version")
        for (suffix in commonSuffixes) {
            coords.add("$groupId:$artifactId-$suffix:$version")
        }
        return coords.distinct()
    }

    fun resolve(
        coordinate: String,
        customRepos: List<String> = emptyList(),
        progressReporter: (String) -> Unit = {}
    ): ResolvedArtifact {
        Logger.info("Resolving coordinate: $coordinate")
        val parts = coordinate.split(':')
        val groupId = parts[0].trim()
        val artifactId = parts[1].trim()
        val version = parts[2].trim()

        val groupPath = groupId.replace('.', '/')
        val basePath = "$groupPath/$artifactId/$version"

        val aarName = "$artifactId-$version.aar"
        val klibName = "$artifactId-$version.klib"
        val jarName = "$artifactId-$version.jar"
        val sourcesName = "$artifactId-$version-sources.jar"

        // Local cache files
        val cachedAar = File(cacheDir, "$basePath/$aarName")
        val cachedKlib = File(cacheDir, "$basePath/$klibName")
        val cachedJar = File(cacheDir, "$basePath/$jarName")
        val cachedSources = File(cacheDir, "$basePath/$sourcesName")

        // 1. Check cache first
        if (cachedAar.exists()) {
            Logger.info("Cache hit (AAR) for $coordinate")
            progressReporter("Found cached binary AAR: ${cachedAar.name}")
            return ResolvedArtifact(cachedAar, if (cachedSources.exists()) cachedSources else null)
        }
        if (cachedKlib.exists()) {
            Logger.info("Cache hit (KLIB) for $coordinate")
            progressReporter("Found cached binary KLIB: ${cachedKlib.name}")
            return ResolvedArtifact(cachedKlib, if (cachedSources.exists()) cachedSources else null)
        }
        if (cachedJar.exists()) {
            Logger.info("Cache hit (JAR) for $coordinate")
            progressReporter("Found cached binary JAR: ${cachedJar.name}")
            return ResolvedArtifact(cachedJar, if (cachedSources.exists()) cachedSources else null)
        }

        // 1.5 Check local Gradle cache folder as fallback before internet download
        val gradleCacheResult = tryResolveFromGradleCache(groupId, artifactId, version, cachedJar, cachedAar, cachedKlib, cachedSources, progressReporter)
        if (gradleCacheResult != null) {
            Logger.info("Resolved $coordinate from Gradle local cache")
            return gradleCacheResult
        }

        progressReporter("Resolving $coordinate from repositories...")
        Logger.info("Resolving $coordinate from remote repositories...")

        // 2. Iterate repositories
        val allRepos = customRepos + REPOSITORIES
        for (repo in allRepos) {
            val repoUrl = repo.removeSuffix("/")
            val binaryAarUrl = "$repoUrl/$basePath/$aarName"
            val binaryKlibUrl = "$repoUrl/$basePath/$klibName"
            val binaryJarUrl = "$repoUrl/$basePath/$jarName"
            val sourcesUrl = "$repoUrl/$basePath/$sourcesName"

            // Try AAR first
            progressReporter("Checking AAR in $repo...")
            Logger.info("Checking remote URL: $binaryAarUrl")
            if (downloadFile(binaryAarUrl, cachedAar)) {
                progressReporter("Successfully downloaded AAR!")
                Logger.info("Successfully downloaded AAR from $repo")
                // Try downloading sources
                progressReporter("Checking sources JAR...")
                val hasSources = downloadFile(sourcesUrl, cachedSources)
                if (hasSources) {
                    progressReporter("Successfully downloaded sources JAR!")
                    Logger.info("Successfully downloaded sources JAR from $repo")
                }
                return ResolvedArtifact(cachedAar, if (hasSources) cachedSources else null)
            }

            // Try KLIB next
            progressReporter("Checking KLIB in $repo...")
            Logger.info("Checking remote URL: $binaryKlibUrl")
            if (downloadFile(binaryKlibUrl, cachedKlib)) {
                progressReporter("Successfully downloaded KLIB!")
                Logger.info("Successfully downloaded KLIB from $repo")
                // Try downloading sources
                progressReporter("Checking sources JAR...")
                val hasSources = downloadFile(sourcesUrl, cachedSources)
                if (hasSources) {
                    progressReporter("Successfully downloaded sources JAR!")
                    Logger.info("Successfully downloaded sources JAR from $repo")
                }
                return ResolvedArtifact(cachedKlib, if (hasSources) cachedSources else null)
            }

            // Try JAR next
            progressReporter("Checking JAR in $repo...")
            Logger.info("Checking remote URL: $binaryJarUrl")
            if (downloadFile(binaryJarUrl, cachedJar)) {
                progressReporter("Successfully downloaded JAR!")
                Logger.info("Successfully downloaded JAR from $repo")
                // Try downloading sources
                progressReporter("Checking sources JAR...")
                val hasSources = downloadFile(sourcesUrl, cachedSources)
                if (hasSources) {
                    progressReporter("Successfully downloaded sources JAR!")
                    Logger.info("Successfully downloaded sources JAR from $repo")
                }
                return ResolvedArtifact(cachedJar, if (hasSources) cachedSources else null)
            }
        }

        val err = "Could not resolve maven coordinate '$coordinate' in any of the registered repositories: $REPOSITORIES"
        Logger.error(err)
        throw IllegalArgumentException(err)
    }

    /**
     * Clears all cached artifacts from the local cache directory.
     */
    fun clearCache(): Long {
        var bytesDeleted = 0L
        if (cacheDir.exists()) {
            cacheDir.walkBottomUp().forEach {
                if (it.isFile) {
                    bytesDeleted += it.length()
                }
                it.delete()
            }
        }
        return bytesDeleted
    }

    private fun downloadFile(urlStr: String, destination: File): Boolean {
        return runBlocking {
            try {
                val response: HttpResponse = client.get(urlStr)
                if (response.status.value == 200) {
                    destination.parentFile.mkdirs()
                    val bytes = response.readRawBytes()
                    destination.writeBytes(bytes)
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun tryResolveFromGradleCache(
        groupId: String,
        artifactId: String,
        version: String,
        cachedJar: File,
        cachedAar: File,
        cachedKlib: File,
        cachedSources: File,
        progressReporter: (String) -> Unit
    ): ResolvedArtifact? {
        val userHome = System.getProperty("user.home") ?: return null
        val gradleCacheDir = File(userHome, ".gradle/caches/modules-2/files-2.1/$groupId/$artifactId/$version")
        if (!gradleCacheDir.exists()) return null

        progressReporter("Found local Gradle cache directory for $groupId:$artifactId:$version")

        var jarFile: File? = null
        var aarFile: File? = null
        var klibFile: File? = null
        var sourcesFile: File? = null

        gradleCacheDir.walkBottomUp().filter { it.isFile }.forEach { file ->
            if (file.name.endsWith("-sources.jar")) {
                sourcesFile = file
            } else if (file.name.endsWith(".klib")) {
                klibFile = file
            } else if (file.name.endsWith(".jar")) {
                jarFile = file
            } else if (file.name.endsWith(".aar")) {
                aarFile = file
            }
        }

        if (aarFile != null) {
            progressReporter("Using cached binary AAR from Gradle cache: ${aarFile.name}")
            if (sourcesFile != null) {
                progressReporter("Using cached sources JAR from Gradle cache: ${sourcesFile.name}")
            }
            return ResolvedArtifact(aarFile, sourcesFile)
        }

        if (klibFile != null) {
            progressReporter("Using cached binary KLIB from Gradle cache: ${klibFile.name}")
            if (sourcesFile != null) {
                progressReporter("Using cached sources JAR from Gradle cache: ${sourcesFile.name}")
            }
            return ResolvedArtifact(klibFile, sourcesFile)
        }

        if (jarFile != null) {
            progressReporter("Using cached binary JAR from Gradle cache: ${jarFile.name}")
            if (sourcesFile != null) {
                progressReporter("Using cached sources JAR from Gradle cache: ${sourcesFile.name}")
            }
            return ResolvedArtifact(jarFile, sourcesFile)
        }

        return null
    }
}
