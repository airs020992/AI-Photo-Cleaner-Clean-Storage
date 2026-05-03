package com.air.cleaner.ui

import com.google.firebase.analytics.FirebaseAnalytics

internal interface FirebaseAnalyticsCollection {
    fun setAnalyticsCollectionEnabled(enabled: Boolean)
}

internal class FirebaseAnalyticsPrivacyController(
    private val collection: FirebaseAnalyticsCollection,
) {
    fun setProductAnalyticsEnabled(enabled: Boolean) {
        collection.setAnalyticsCollectionEnabled(enabled)
    }
}

internal class FirebaseAnalyticsCollectionAdapter(
    private val firebaseAnalytics: FirebaseAnalytics,
) : FirebaseAnalyticsCollection {
    override fun setAnalyticsCollectionEnabled(enabled: Boolean) {
        firebaseAnalytics.setAnalyticsCollectionEnabled(enabled)
    }
}
