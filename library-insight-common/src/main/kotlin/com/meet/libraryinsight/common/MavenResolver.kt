package com.meet.libraryinsight.common

import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readRawBytes
import kotlinx.coroutines.runBlocking
import java.io.File

object MavenResolver {

    private val client = HttpClient(Java) {
        followRedirects = true
    }

    data class ResolvedArtifact(
        val binaryFile: File,
        val sourcesFile: File?
    )

    private val REPOSITORIES = listOf(
        "https://repo1.maven.org/maven2/",
        "https://dl.google.com/dl/android/maven2/",
        "https://qisdk.softbankrobotics.com/sdk/maven/"
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
     * Resolves and downloads the library binary and sources from repositories.
     * Caches the files to avoid redundant downloads.
     */
    fun resolve(
        coordinate: String,
        customRepos: List<String> = emptyList(),
        progressReporter: (String) -> Unit = {}
    ): ResolvedArtifact {
        val parts = coordinate.split(':')
        val groupId = parts[0].trim()
        val artifactId = parts[1].trim()
        val version = parts[2].trim()

        val groupPath = groupId.replace('.', '/')
        val basePath = "$groupPath/$artifactId/$version"

        val aarName = "$artifactId-$version.aar"
        val jarName = "$artifactId-$version.jar"
        val sourcesName = "$artifactId-$version-sources.jar"

        // Local cache files
        val cachedAar = File(cacheDir, "$basePath/$aarName")
        val cachedJar = File(cacheDir, "$basePath/$jarName")
        val cachedSources = File(cacheDir, "$basePath/$sourcesName")

        // 1. Check cache first
        if (cachedAar.exists()) {
            progressReporter("Found cached binary AAR: ${cachedAar.name}")
            return ResolvedArtifact(cachedAar, if (cachedSources.exists()) cachedSources else null)
        }
        if (cachedJar.exists()) {
            progressReporter("Found cached binary JAR: ${cachedJar.name}")
            return ResolvedArtifact(cachedJar, if (cachedSources.exists()) cachedSources else null)
        }

        // 1.5 Check local Gradle cache folder as fallback before internet download
        val gradleCacheResult = tryResolveFromGradleCache(groupId, artifactId, version, cachedJar, cachedAar, cachedSources, progressReporter)
        if (gradleCacheResult != null) {
            return gradleCacheResult
        }

        progressReporter("Resolving $coordinate from repositories...")

        // 2. Iterate repositories
        val allRepos = customRepos + REPOSITORIES
        for (repo in allRepos) {
            val repoUrl = repo.removeSuffix("/")
            val binaryAarUrl = "$repoUrl/$basePath/$aarName"
            val binaryJarUrl = "$repoUrl/$basePath/$jarName"
            val sourcesUrl = "$repoUrl/$basePath/$sourcesName"

            // Try AAR first
            progressReporter("Checking AAR in $repo...")
            if (downloadFile(binaryAarUrl, cachedAar)) {
                progressReporter("Successfully downloaded AAR!")
                // Try downloading sources
                progressReporter("Checking sources JAR...")
                val hasSources = downloadFile(sourcesUrl, cachedSources)
                if (hasSources) {
                    progressReporter("Successfully downloaded sources JAR!")
                }
                return ResolvedArtifact(cachedAar, if (hasSources) cachedSources else null)
            }

            // Try JAR next
            progressReporter("Checking JAR in $repo...")
            if (downloadFile(binaryJarUrl, cachedJar)) {
                progressReporter("Successfully downloaded JAR!")
                // Try downloading sources
                progressReporter("Checking sources JAR...")
                val hasSources = downloadFile(sourcesUrl, cachedSources)
                if (hasSources) {
                    progressReporter("Successfully downloaded sources JAR!")
                }
                return ResolvedArtifact(cachedJar, if (hasSources) cachedSources else null)
            }
        }

        throw IllegalArgumentException("Could not resolve maven coordinate '$coordinate' in any of the registered repositories: $REPOSITORIES")
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
        cachedSources: File,
        progressReporter: (String) -> Unit
    ): ResolvedArtifact? {
        val userHome = System.getProperty("user.home") ?: return null
        val gradleCacheDir = File(userHome, ".gradle/caches/modules-2/files-2.1/$groupId/$artifactId/$version")
        if (!gradleCacheDir.exists()) return null

        progressReporter("Found local Gradle cache directory for $groupId:$artifactId:$version")

        var jarFile: File? = null
        var aarFile: File? = null
        var sourcesFile: File? = null

        gradleCacheDir.walkBottomUp().filter { it.isFile }.forEach { file ->
            if (file.name.endsWith("-sources.jar")) {
                sourcesFile = file
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
