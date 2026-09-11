package com.meet.libraryinsight.search

import com.meet.libraryinsight.model.ClassApi
import com.meet.libraryinsight.model.ClassKind
import com.meet.libraryinsight.model.LibraryApiIndex
import com.meet.libraryinsight.model.PackageApi
import com.meet.libraryinsight.model.Visibility
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchEngineTest {

    @Test
    fun testSearchDotToDollarCompanionNormalization() {
        val companionClass = ClassApi(
            name = "androidx.compose.foundation.layout.GridTrackSize\$Companion",
            simpleName = "GridTrackSize\$Companion",
            kind = ClassKind.COMPANION_OBJECT,
            visibility = Visibility.PUBLIC,
            modifiers = emptyList(),
            superTypes = emptyList(),
            annotations = emptyList(),
            constructors = emptyList(),
            methods = emptyList(),
            properties = emptyList(),
            nestedClasses = emptyList()
        )

        val index = LibraryApiIndex(
            libraryName = "test-lib",
            version = "1.0.0",
            packages = listOf(
                PackageApi(
                    name = "androidx.compose.foundation.layout",
                    classes = listOf(companionClass)
                )
            )
        )

        // Querying with dot notation "GridTrackSize.Companion"
        val results = SearchEngine.search(index, "GridTrackSize.Companion")

        assertEquals(1, results.size)
        assertEquals("GridTrackSize\$Companion", results[0].name)
        assertEquals(SearchEngine.MatchType.CLASS, results[0].type)
    }
}
