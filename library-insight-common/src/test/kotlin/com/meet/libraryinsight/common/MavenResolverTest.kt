package com.meet.libraryinsight.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MavenResolverTest {

    @Test
    fun testFilterCoordinatesByPlatformAndroid() {
        val sampleCoords = listOf(
            "androidx.compose.animation:animation:1.12.0",
            "androidx.compose.animation:animation-android:1.12.0",
            "androidx.compose.animation:animation-desktop:1.7.0",
            "androidx.compose.animation:animation-core-jvmstubs:1.12.0",
            "androidx.compose.animation:animation-core-lint:1.12.0",
            "androidx.compose.runtime:runtime-annotation-iosarm64:1.12.0",
            "androidx.compose.runtime:runtime-annotation-wasm-js:1.12.0",
            "androidx.compose.runtime:runtime-annotation-mingwx64:1.12.0",
            "androidx.compose.foundation:foundation-layout-android:1.12.0"
        )

        val filtered = MavenResolver.filterCoordinatesByPlatform(sampleCoords, "android")

        assertEquals(3, filtered.size)
        assertTrue(filtered.contains("androidx.compose.animation:animation:1.12.0"))
        assertTrue(filtered.contains("androidx.compose.animation:animation-android:1.12.0"))
        assertTrue(filtered.contains("androidx.compose.foundation:foundation-layout-android:1.12.0"))
    }

    @Test
    fun testFilterCoordinatesByPlatformAll() {
        val sampleCoords = listOf(
            "androidx.compose.animation:animation:1.12.0",
            "androidx.compose.runtime:runtime-annotation-iosarm64:1.12.0",
            "androidx.compose.runtime:runtime-annotation-wasm-js:1.12.0"
        )

        val filtered = MavenResolver.filterCoordinatesByPlatform(sampleCoords, "all")
        assertEquals(3, filtered.size)
    }
}
