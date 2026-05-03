package com.air.cleaner

import android.app.Application
import com.air.cleaner.ui.FirebaseAnalyticsCollectionAdapter
import com.air.cleaner.ui.FirebaseAnalyticsPrivacyController
import com.air.cleaner.ui.analyticsEnabled
import com.air.cleaner.ui.cleanerPreferences
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseAnalyticsPrivacyController(
            FirebaseAnalyticsCollectionAdapter(FirebaseAnalytics.getInstance(this)),
        ).setProductAnalyticsEnabled(cleanerPreferences().analyticsEnabled())
    }
}
