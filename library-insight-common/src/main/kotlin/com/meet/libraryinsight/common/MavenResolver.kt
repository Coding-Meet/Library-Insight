package com.meet.libraryinsight.common

import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object MavenResolver {

    data class ResolvedArtifact(
        val binaryFile: File,
        val sourcesFile: File?
    )

    private val REPOSITORIES = listOf(
        "https://repo1.maven.org/maven2/",
        "https://dl.google.com/dl/android/maven2/",
        "https://qisdk.softbankrobotics.com/sdk/maven/"
    )

    val cacheDir = File(System.getProperty("user.home"), ".library-insight/cache")

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
        try {
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.instanceFollowRedirects = true
            
            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_OK) {
                destination.parentFile.mkdirs()
                conn.inputStream.use { input ->
                    destination.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                return true
            }
        } catch (e: Exception) {
            // Ignore failure to check next repo or binary format
        }
        return false
    }
}
