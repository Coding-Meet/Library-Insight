package com.meet.libraryinsight.core

import com.meet.libraryinsight.model.ClassApi
import com.meet.libraryinsight.model.ClassKind
import com.meet.libraryinsight.model.LibraryApiIndex
import com.meet.libraryinsight.model.PackageApi
import com.meet.libraryinsight.model.Visibility
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryAnalyzerTest {

    @Test
    fun testMergeIndicesParallelChunks() {
        // Create 12 dummy indices to trigger parallel chunked merging (> 8)
        val indices = (1..12).map { i ->
            LibraryApiIndex(
                libraryName = "lib-$i",
                version = "1.0.0",
                packages = listOf(
                    PackageApi(
                        name = "com.sample.pkg",
                        classes = listOf(
                            ClassApi(
                                name = "com.sample.pkg.Class$i",
                                simpleName = "Class$i",
                                visibility = Visibility.PUBLIC,
                                kind = ClassKind.CLASS,
                                modifiers = emptyList(),
                                superTypes = emptyList(),
                                annotations = emptyList(),
                                constructors = emptyList(),
                                methods = emptyList(),
                                properties = emptyList(),
                                nestedClasses = emptyList()
                            )
                        )
                    )
                )
            )
        }

        val merged = LibraryAnalyzer.mergeIndices(indices)

        assertEquals("lib-1", merged.libraryName)
        assertEquals(1, merged.packages.size)
        val pkg = merged.packages.first()
        assertEquals("com.sample.pkg", pkg.name)
        assertEquals(12, pkg.classes.size)
    }
}
