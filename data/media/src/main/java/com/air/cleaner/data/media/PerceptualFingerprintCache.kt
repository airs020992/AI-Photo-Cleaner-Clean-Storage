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
    ): String? = getOrPutResult(key, compute).fingerprint

    fun getOrPutResult(
        key: PerceptualFingerprintCacheKey,
        compute: () -> String?,
    ): PerceptualFingerprintCacheResult
}

data class PerceptualFingerprintCacheResult(
    val fingerprint: String?,
    val cacheHit: Boolean,
)

class InMemoryPerceptualFingerprintCache : PerceptualFingerprintCache {
    private val fingerprints = mutableMapOf<String, String>()

    override fun getOrPutResult(
        key: PerceptualFingerprintCacheKey,
        compute: () -> String?,
    ): PerceptualFingerprintCacheResult {
        val storageKey = key.storageKey()
        fingerprints[storageKey]?.let {
            return PerceptualFingerprintCacheResult(
                fingerprint = it,
                cacheHit = true,
            )
        }
        val fingerprint = compute()
            ?: return PerceptualFingerprintCacheResult(
                fingerprint = null,
                cacheHit = false,
            )
        fingerprints[storageKey] = fingerprint
        return PerceptualFingerprintCacheResult(
            fingerprint = fingerprint,
            cacheHit = false,
        )
    }
}

class SharedPreferencesPerceptualFingerprintCache(
    context: Context,
) : PerceptualFingerprintCache {
    private val preferences: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    override fun getOrPutResult(
        key: PerceptualFingerprintCacheKey,
        compute: () -> String?,
    ): PerceptualFingerprintCacheResult {
        val storageKey = key.storageKey()
        preferences.getString(storageKey, null)?.let {
            return PerceptualFingerprintCacheResult(
                fingerprint = it,
                cacheHit = true,
            )
        }
        val fingerprint = compute()
            ?: return PerceptualFingerprintCacheResult(
                fingerprint = null,
                cacheHit = false,
            )
        preferences.edit().putString(storageKey, fingerprint).apply()
        return PerceptualFingerprintCacheResult(
            fingerprint = fingerprint,
            cacheHit = false,
        )
    }

    private companion object {
        private const val PREFERENCES_NAME = "similar_screenshot_fingerprint_cache"
    }
}
