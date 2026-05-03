package com.air.cleaner.data.media

import org.junit.Assert.assertEquals
import org.junit.Test

class PerceptualFingerprintCacheTest {
    @Test
    fun reusesCachedFingerprintForUnchangedMediaIdentity() {
        val cache = InMemoryPerceptualFingerprintCache()
        var computeCount = 0
        val key = PerceptualFingerprintCacheKey(
            contentUri = "content://media/images/1",
            sizeBytes = 1_912_800L,
            dateMillis = 1_000L,
            width = 1440,
            height = 3120,
        )

        val first = cache.getOrPut(key) {
            computeCount += 1
            "same-hash"
        }
        val second = cache.getOrPut(key) {
            computeCount += 1
            "new-hash"
        }

        assertEquals("same-hash", first)
        assertEquals("same-hash", second)
        assertEquals(1, computeCount)
    }

    @Test
    fun recomputesFingerprintWhenMediaIdentityChanges() {
        val cache = InMemoryPerceptualFingerprintCache()
        val original = PerceptualFingerprintCacheKey(
            contentUri = "content://media/images/1",
            sizeBytes = 1_912_800L,
            dateMillis = 1_000L,
            width = 1440,
            height = 3120,
        )
        val modified = original.copy(dateMillis = 2_000L)
        var computeCount = 0

        cache.getOrPut(original) {
            computeCount += 1
            "old-hash"
        }
        val updated = cache.getOrPut(modified) {
            computeCount += 1
            "updated-hash"
        }

        assertEquals("updated-hash", updated)
        assertEquals(2, computeCount)
    }
}
