package com.air.cleaner.data.media

import android.content.Context
import android.content.SharedPreferences

data class PerceptualFingerprintCacheKey(
    val contentUri: String,
    val sizeBytes: Long,
    val dateMillis: Long,
    val width: Int?,
    val height: Int?,
) {
    fun storageKey(): String {
        return listOf(
            contentUri,
            sizeBytes.toString(),
            dateMillis.toString(),
            width?.toString().orEmpty(),
            height?.toString().orEmpty(),
        ).joinToString(separator = "|")
    }
}

interface PerceptualFingerprintCache {
    fun getOrPut(
        key: PerceptualFingerprintCacheKey,
        compute: () -> String?,
    ): String?
}

class InMemoryPerceptualFingerprintCache : PerceptualFingerprintCache {
    private val fingerprints = mutableMapOf<String, String>()

    override fun getOrPut(
        key: PerceptualFingerprintCacheKey,
        compute: () -> String?,
    ): String? {
        val storageKey = key.storageKey()
        fingerprints[storageKey]?.let { return it }
        val fingerprint = compute() ?: return null
        fingerprints[storageKey] = fingerprint
        return fingerprint
    }
}

class SharedPreferencesPerceptualFingerprintCache(
    context: Context,
) : PerceptualFingerprintCache {
    private val preferences: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    override fun getOrPut(
        key: PerceptualFingerprintCacheKey,
        compute: () -> String?,
    ): String? {
        val storageKey = key.storageKey()
        preferences.getString(storageKey, null)?.let { return it }
        val fingerprint = compute() ?: return null
        preferences.edit().putString(storageKey, fingerprint).apply()
        return fingerprint
    }

    private companion object {
        private const val PREFERENCES_NAME = "similar_screenshot_fingerprint_cache"
    }
}
