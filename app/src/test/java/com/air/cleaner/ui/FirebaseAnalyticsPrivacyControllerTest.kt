package com.air.cleaner.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FirebaseAnalyticsPrivacyControllerTest {
    @Test
    fun productAnalyticsToggleControlsFirebaseCollection() {
        val firebaseCollection = RecordingFirebaseAnalyticsCollection()
        val controller = FirebaseAnalyticsPrivacyController(firebaseCollection)

        controller.setProductAnalyticsEnabled(false)
        controller.setProductAnalyticsEnabled(true)

        assertEquals(listOf(false, true), firebaseCollection.enabledStates)
    }

    private class RecordingFirebaseAnalyticsCollection : FirebaseAnalyticsCollection {
        val enabledStates = mutableListOf<Boolean>()

        override fun setAnalyticsCollectionEnabled(enabled: Boolean) {
            enabledStates += enabled
        }
    }
}
