package com.air.cleaner.ui

import android.content.Context
import android.content.SharedPreferences

internal const val PREF_ANALYTICS_ENABLED = "analytics_enabled"

private const val CLEANER_PREFS = "ai_photo_cleaner"

internal fun Context.cleanerPreferences(): SharedPreferences {
    return getSharedPreferences(CLEANER_PREFS, Context.MODE_PRIVATE)
}

internal fun SharedPreferences.analyticsEnabled(): Boolean {
    return getBoolean(PREF_ANALYTICS_ENABLED, false)
}
