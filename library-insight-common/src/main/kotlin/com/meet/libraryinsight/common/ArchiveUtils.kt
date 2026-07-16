package com.meet.libraryinsight.common

import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

object ArchiveUtils {

    /**
     * Scans a file or directory and extracts all JVM class bytecode as byte arrays.
     * Maps the class internal name (e.g., "com/meet/libraryinsight/MyClass") to its bytecode.
     */
    fun extractClasses(file: File): Map<String, ByteArray> {
        val classes = mutableMapOf<String, ByteArray>()
        if (!file.exists()) return classes

        if (file.isDirectory) {
            file.walkTopDown().forEach { child ->
                if (child.isFile) {
                    when {
                        child.name.endsWith(".class") -> {
                            child.inputStream().use { input ->
                                val bytes = input.readBytes()
                                val className = getClassNameFromClassBytes(bytes) ?: child.nameWithoutExtension
                                classes[className] = bytes
                            }
                        }
                        child.name.endsWith(".jar") -> {
                            classes.putAll(extractFromJar(child.inputStream()))
                        }
                        child.name.endsWith(".aar") -> {
                            classes.putAll(extractFromAar(child.inputStream()))
                        }
                    }
                }
            }
        } else {
            when {
                file.name.endsWith(".class") -> {
                    file.inputStream().use { input ->
                        val bytes = input.readBytes()
                        val className = getClassNameFromClassBytes(bytes) ?: file.nameWithoutExtension
                        classes[className] = bytes
                    }
                }
                file.name.endsWith(".jar") -> {
                    classes.putAll(extractFromJar(file.inputStream()))
                }
                file.name.endsWith(".aar") -> {
                    classes.putAll(extractFromAar(file.inputStream()))
                }
            }
        }
        return classes
    }

    /**
     * Extracts classes from a JAR input stream.
     */
    fun extractFromJar(inputStream: InputStream): Map<String, ByteArray> {
        val classes = mutableMapOf<String, ByteArray>()
        val zip = ZipInputStream(inputStream)
        var entry = zip.nextEntry
        while (entry != null) {
            if (!entry.isDirectory && entry.name.endsWith(".class")) {
                val bytes = zip.readBytes()
                val className = getClassNameFromClassBytes(bytes) ?: entry.name.removeSuffix(".class")
                classes[className] = bytes
            }
            entry = zip.nextEntry
        }
        return classes
    }

    /**
     * Extracts classes from an AAR input stream.
     * AAR files are zip files containing `classes.jar` and potentially other JAR files in `libs/`.
     */
    fun extractFromAar(inputStream: InputStream): Map<String, ByteArray> {
        val classes = mutableMapOf<String, ByteArray>()
        val zip = ZipInputStream(inputStream)
        var entry = zip.nextEntry
        while (entry != null) {
            if (!entry.isDirectory) {
                if (entry.name == "classes.jar" || (entry.name.startsWith("libs/") && entry.name.endsWith(".jar"))) {
                    // Open the nested JAR input stream. ZipInputStream read() provides bytes of the current entry.
                    // We must wrap it without closing the parent ZipInputStream.
                    val nestedJarStream = NonClosingInputStream(zip)
                    classes.putAll(extractFromJar(nestedJarStream))
                }
            }
            entry = zip.nextEntry
        }
        return classes
    }

    /**
     * Quick bytecode parsing to extract the class name (internal name) from the first few bytes.
     * JVM Class File Format:
     * - Magic number: 0xCAFEBABE (4 bytes)
     * - Minor version: 2 bytes
     * - Major version: 2 bytes
     * - Constant pool count: 2 bytes
     * - Constant pool tags/info...
     */
    private fun getClassNameFromClassBytes(bytes: ByteArray): String? {
        if (bytes.size < 8) return null
        // We could write a small parser, but since we have ASM available,
        // we can let ASM extract the class name. However, since ArchiveUtils is in
        // library-insight-common which does NOT depend on ASM, we can do a very simple check
        // or just return null and let the parser module rename/verify classes based on ASM later,
        // OR we can read the class name using standard JVM binary structure parsing.
        // Let's implement a simple constant pool reader to find the class name, OR just return null
        // and default to the zip entry name, since ASM parser will read the real class name anyway.
        // Returning null and relying on the entry path is safe. But wait, in directories, child.nameWithoutExtension
        // does not include the package path!
        // To get the full class name including package, we can do a lightweight parsing of class bytes.
        // Let's implement class name extraction from class bytes to guarantee accuracy.
        try {
            val magic = ((bytes[0].toInt() and 0xFF) shl 24) or
                        ((bytes[1].toInt() and 0xFF) shl 16) or
                        ((bytes[2].toInt() and 0xFF) shl 8) or
                        (bytes[3].toInt() and 0xFF)
            if (magic != 0xCAFEBABE.toInt()) return null

            val constantPoolCount = ((bytes[8].toInt() and 0xFF) shl 8) or (bytes[9].toInt() and 0xFF)
            var offset = 10
            val cpTags = IntArray(constantPoolCount)
            val cpUtf8 = arrayOfNulls<String>(constantPoolCount)
            val cpClassInfo = IntArray(constantPoolCount)

            var i = 1
            while (i < constantPoolCount) {
                val tag = bytes[offset].toInt() and 0xFF
                cpTags[i] = tag
                offset++
                when (tag) {
                    7 -> { // CONSTANT_Class
                        cpClassInfo[i] = ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)
                        offset += 2
                    }
                    9, 10, 11, 12, 18 -> { // Fieldref, Methodref, InterfaceMethodref, NameAndType, InvokeDynamic
                        offset += 4
                    }
                    3, 4 -> { // Integer, Float
                        offset += 4
                    }
                    5, 6 -> { // Long, Double
                        offset += 8
                        i++ // Long and Double occupy 2 entries
                    }
                    1 -> { // CONSTANT_Utf8
                        val length = ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)
                        offset += 2
                        cpUtf8[i] = String(bytes, offset, length, Charsets.UTF_8)
                        offset += length
                    }
                    15 -> { // MethodHandle
                        offset += 3
                    }
                    16 -> { // MethodType
                        offset += 2
                    }
                    19, 20 -> { // Module, Package
                        offset += 2
                    }
                    else -> {
                        // Unknown tag or malformed class
                        return null
                    }
                }
                i++
            }

            val accessFlags = ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)
            val thisClassIndex = ((bytes[offset + 2].toInt() and 0xFF) shl 8) or (bytes[offset + 3].toInt() and 0xFF)
            val utf8Index = cpClassInfo[thisClassIndex]
            return cpUtf8[utf8Index]
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * An input stream that prevents closing the underlying stream.
     */
    private class NonClosingInputStream(private val delegate: InputStream) : InputStream() {
        override fun read(): Int = delegate.read()
        override fun read(b: ByteArray): Int = delegate.read(b)
        override fun read(b: ByteArray, off: Int, len: Int): Int = delegate.read(b, off, len)
        override fun skip(n: Long): Long = delegate.skip(n)
        override fun available(): Int = delegate.available()
        override fun close() {
            // Do not close the delegate
        }
    }
}
