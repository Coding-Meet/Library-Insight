package com.meet.libraryinsight.common

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LocalArtifactsTest {

    @Test
    fun testParseNameAndVersion() {
        assertEquals(
            LocalArtifacts.NameAndVersion("retrofit", "2.11.0"),
            LocalArtifacts.parseNameAndVersion("retrofit-2.11.0")
        )
        assertEquals(
            LocalArtifacts.NameAndVersion("kotlinx-coroutines-core", "1.8.1"),
            LocalArtifacts.parseNameAndVersion("kotlinx-coroutines-core-1.8.1")
        )
        assertEquals(
            LocalArtifacts.NameAndVersion("retrofit", "2.11.0-RC1"),
            LocalArtifacts.parseNameAndVersion("retrofit-2.11.0-RC1")
        )
    }

    @Test
    fun testParseNameWithoutVersion() {
        val parsed = LocalArtifacts.parseNameAndVersion("transformer-core-jvm")
        assertEquals("transformer-core-jvm", parsed.name)
        assertNull(parsed.version)
    }

    @Test
    fun testFindSiblingSources() {
        val dir = java.nio.file.Files.createTempDirectory("local-artifacts-test").toFile()
        try {
            val binary = File(dir, "retrofit-2.11.0.jar").apply { writeText("binary") }
            assertNull(LocalArtifacts.findSiblingSources(binary))

            val sources = File(dir, "retrofit-2.11.0-sources.jar").apply { writeText("sources") }
            assertEquals(sources, LocalArtifacts.findSiblingSources(binary))
        } finally {
            dir.deleteRecursively()
        }
    }
}
