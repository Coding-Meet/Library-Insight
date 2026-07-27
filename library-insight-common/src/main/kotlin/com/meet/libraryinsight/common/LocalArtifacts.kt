package com.meet.libraryinsight.common

import java.io.File

/**
 * Helpers for scanning local JAR/AAR files: derive the library name and
 * version from the file name and locate a sibling sources archive.
 */
object LocalArtifacts {

    // "retrofit-2.11.0" → name "retrofit", version "2.11.0".
    // The version must start with a digit, so "transformer-core-jvm" stays a plain name.
    private val NAME_VERSION_REGEX = Regex("^(.+?)-(\\d[A-Za-z0-9._\\-]*)$")

    data class NameAndVersion(val name: String, val version: String?)

    /**
     * Splits a file name (without extension) into library name and version.
     * Returns a null version when the name has no `-<digit...>` suffix.
     */
    fun parseNameAndVersion(fileNameWithoutExtension: String): NameAndVersion {
        val match = NAME_VERSION_REGEX.matchEntire(fileNameWithoutExtension)
            ?: return NameAndVersion(fileNameWithoutExtension, null)
        val (name, version) = match.destructured
        return NameAndVersion(name, version)
    }

    /**
     * Finds a `-sources` archive next to the binary,
     * e.g. `retrofit-2.11.0-sources.jar` for `retrofit-2.11.0.jar`.
     */
    fun findSiblingSources(binary: File): File? {
        if (!binary.isFile) return null
        // binary may be a bare relative path like "lib-1.0.jar" whose parentFile is null
        val parent = binary.absoluteFile.parentFile ?: return null
        val base = binary.nameWithoutExtension
        return listOf("jar", "aar", "zip")
            .map { ext -> File(parent, "$base-sources.$ext") }
            .firstOrNull { it.isFile }
    }
}
