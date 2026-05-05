package com.air.cleaner.ui

import com.air.cleaner.core.permissions.MediaPermissionState
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaAccessDiagnosticsTest {
    @Test
    fun reportsLimitedPhotoAccessWithActionableNextStep() {
        val labels = MediaPermissionState(
            sdkInt = 35,
            readImagesGranted = false,
            readVideoGranted = true,
            visualUserSelectedGranted = true,
            legacyReadGranted = false,
        ).toMediaAccessDiagnosticsLabels()

        assertEquals(
            listOf(
                "Media access: limited selected photos.",
                "Permission gate: full photo access is missing. Next: allow full Photos access before trusting empty scan results.",
            ),
            labels,
        )
    }

    @Test
    fun reportsFullPhotoAccessAsReadyForLibraryScan() {
        val labels = MediaPermissionState(
            sdkInt = 35,
            readImagesGranted = true,
            readVideoGranted = true,
            visualUserSelectedGranted = false,
            legacyReadGranted = false,
        ).toMediaAccessDiagnosticsLabels()

        assertEquals(
            listOf("Media access: full library."),
            labels,
        )
    }
}
