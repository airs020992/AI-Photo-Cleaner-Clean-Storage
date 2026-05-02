package com.air.cleaner.core.permissions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPermissionStateTest {
    @Test
    fun android13RequiresImageAndVideoPermissionsForFullLibrary() {
        val state = MediaPermissionState(
            sdkInt = 33,
            readImagesGranted = true,
            readVideoGranted = false,
            visualUserSelectedGranted = false,
            legacyReadGranted = false,
        )

        assertEquals(MediaPermissionAccess.None, state.access)
        assertFalse(state.canScanAnyMedia)
    }

    @Test
    fun android13FullLibraryWhenImagesAndVideosAreGranted() {
        val state = MediaPermissionState(
            sdkInt = 33,
            readImagesGranted = true,
            readVideoGranted = true,
            visualUserSelectedGranted = false,
            legacyReadGranted = false,
        )

        assertEquals(MediaPermissionAccess.Full, state.access)
        assertTrue(state.canScanFullLibrary)
    }

    @Test
    fun android14SelectedPhotosAccessAllowsLimitedScan() {
        val state = MediaPermissionState(
            sdkInt = 34,
            readImagesGranted = false,
            readVideoGranted = false,
            visualUserSelectedGranted = true,
            legacyReadGranted = false,
        )

        assertEquals(MediaPermissionAccess.SelectedOnly, state.access)
        assertTrue(state.canScanAnyMedia)
        assertFalse(state.canScanFullLibrary)
    }

    @Test
    fun legacyAndroidUsesReadExternalStorage() {
        val state = MediaPermissionState(
            sdkInt = 32,
            readImagesGranted = false,
            readVideoGranted = false,
            visualUserSelectedGranted = false,
            legacyReadGranted = true,
        )

        assertEquals(MediaPermissionAccess.Full, state.access)
        assertTrue(state.canScanFullLibrary)
    }
}
