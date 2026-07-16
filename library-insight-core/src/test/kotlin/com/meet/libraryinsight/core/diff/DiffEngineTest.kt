package com.meet.libraryinsight.core.diff

import com.meet.libraryinsight.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiffEngineTest {

    private fun createIndex(classes: List<ClassApi>): LibraryApiIndex {
        return LibraryApiIndex(
            libraryName = "test-lib",
            version = "1.0.0",
            packages = listOf(
                PackageApi(
                    name = "com.example",
                    classes = classes
                )
            )
        )
    }

    private fun createClass(
        name: String,
        visibility: Visibility = Visibility.PUBLIC,
        methods: List<MethodApi> = emptyList(),
        properties: List<PropertyApi> = emptyList()
    ): ClassApi {
        return ClassApi(
            name = name,
            simpleName = name.substringAfterLast('.'),
            visibility = visibility,
            kind = ClassKind.CLASS,
            modifiers = emptyList(),
            superTypes = emptyList(),
            annotations = emptyList(),
            constructors = emptyList(),
            methods = methods,
            properties = properties,
            nestedClasses = emptyList()
        )
    }

    @Test
    fun testClassAddedAndRemoved() {
        val oldIndex = createIndex(listOf(createClass("com.example.OldClass")))
        val newIndex = createIndex(listOf(createClass("com.example.NewClass")))

        val report = DiffEngine.diff(oldIndex, newIndex)

        assertEquals(listOf("com.example.NewClass"), report.addedClasses)
        assertEquals(listOf("com.example.OldClass"), report.removedClasses)
        assertTrue(report.hasBreakingChanges) // Removing a public class is a breaking change
    }

    @Test
    fun testMethodChanges() {
        val oldMethod = MethodApi(
            name = "foo",
            visibility = Visibility.PUBLIC,
            returnType = "void",
            parameters = emptyList(),
            annotations = emptyList(),
            flags = MethodFlags(),
            signature = "()V"
        )
        val oldIndex = createIndex(listOf(createClass("com.example.MyClass", methods = listOf(oldMethod))))

        // Case 1: Method removed
        val newIndexRemoved = createIndex(listOf(createClass("com.example.MyClass", methods = emptyList())))
        val reportRemoved = DiffEngine.diff(oldIndex, newIndexRemoved)
        assertTrue(reportRemoved.hasBreakingChanges)
        assertEquals(1, reportRemoved.changedClasses.size)
        assertEquals(listOf("fun foo(): void"), reportRemoved.changedClasses[0].removedMethods)

        // Case 2: Method visibility reduced
        val newMethodPrivate = oldMethod.copy(visibility = Visibility.PRIVATE)
        val newIndexPrivate = createIndex(listOf(createClass("com.example.MyClass", methods = listOf(newMethodPrivate))))
        val reportPrivate = DiffEngine.diff(oldIndex, newIndexPrivate)
        assertTrue(reportPrivate.hasBreakingChanges)
        assertEquals(1, reportPrivate.changedClasses[0].changedMethods.size)
        assertEquals("fun foo", reportPrivate.changedClasses[0].changedMethods[0].name)
    }
}
