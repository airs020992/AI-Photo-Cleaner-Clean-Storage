package com.air.cleaner.data.media

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimilarPhotoScanScopeTest {
    @Test
    fun acceptsEveryPathWhenPrefixIsNotSet() {
        val scope = SimilarPhotoScanScope()

        assertTrue(scope.accepts(relativePath = null))
        assertTrue(scope.accepts(relativePath = "DCIM/Camera/"))
    }

    @Test
    fun limitsSimilarPhotosToTheConfiguredRelativePathPrefix() {
        val scope = SimilarPhotoScanScope(
            relativePathPrefix = "DCIM/Camera/AIPhotoCleanerTest/",
        )

        assertTrue(scope.accepts(relativePath = "DCIM/Camera/AIPhotoCleanerTest/"))
        assertTrue(scope.accepts(relativePath = "dcim/camera/aiphotocleanertest/"))
        assertFalse(scope.accepts(relativePath = "DCIM/Camera/"))
        assertFalse(scope.accepts(relativePath = null))
    }
}
