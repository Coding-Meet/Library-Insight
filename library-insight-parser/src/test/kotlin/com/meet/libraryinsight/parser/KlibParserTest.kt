package com.meet.libraryinsight.parser

import java.io.File
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KlibParserTest {

    @Test
    fun testIsKlibForNonExistentFile() {
        val file = File("non-existent-file-xyz.klib")
        assertFalse(KlibParser.isKlib(file))
    }

    @Test
    fun testIsKlibForDummyKlib() {
        val tempFile = File.createTempFile("test_lib", ".klib")
        try {
            ZipOutputStream(tempFile.outputStream()).use { zos ->
                zos.putNextEntry(ZipEntry("default/manifest"))
                zos.write("unique_name=test:library\ntargets=ios_arm64, ios_x64\n".toByteArray())
                zos.closeEntry()
            }
            assertTrue(KlibParser.isKlib(tempFile))
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun testParseDummyKlib() {
        val tempFile = File.createTempFile("test_lib", ".klib")
        try {
            ZipOutputStream(tempFile.outputStream()).use { zos ->
                zos.putNextEntry(ZipEntry("default/manifest"))
                zos.write("unique_name=test:library\ntargets=ios_arm64, ios_x64\n".toByteArray())
                zos.closeEntry()
                
                zos.putNextEntry(ZipEntry("default/linkdata/package_com_example_foo.knm"))
                zos.write(byteArrayOf(1, 2, 3))
                zos.closeEntry()
            }
            
            val index = KlibParser.parseKlib(tempFile)
            assertEquals("library", index.libraryName)
            assertEquals(listOf("ios"), index.targets)
            assertEquals(1, index.packages.size)
            assertEquals("com.example.foo", index.packages[0].name)
            assertEquals("Placeholder", index.packages[0].classes[0].simpleName)
            assertEquals(listOf("ios"), index.packages[0].classes[0].targets)
        } finally {
            tempFile.delete()
        }
    }
}
