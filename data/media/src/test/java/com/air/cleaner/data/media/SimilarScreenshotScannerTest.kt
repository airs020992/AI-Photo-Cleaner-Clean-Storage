package com.air.cleaner.data.media

import com.air.cleaner.domain.cleaning.MediaItem
import com.air.cleaner.domain.cleaning.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimilarScreenshotScannerTest {
    @Test
    fun groupsScreenshotsWithNearbyPerceptualHashes() {
        val scanner = SimilarScreenshotScanner(
            perceptualFingerprint = { candidate ->
                when (candidate.contentKey) {
                    "uri-a" -> "0000000000000000"
                    "uri-b" -> "0000000000000001"
                    else -> null
                }
            },
            maxHashDistance = 1,
        )
        val candidates = listOf(
            candidate("1", "Screenshot_1.png", "uri-a", relativePath = "Pictures/Screenshots/"),
            candidate("2", "Screenshot_2.png", "uri-b", relativePath = "Pictures/Screenshots/"),
        )

        val groups = scanner.findSimilarGroups(candidates)

        assertEquals(1, groups.size)
        assertEquals("similar-screenshot:1", groups.single().key)
        assertEquals(listOf("1", "2"), groups.single().items.map { it.id }.sorted())
    }

    @Test
    fun ignoresNonScreenshotImagesEvenWhenHashesAreClose() {
        val requestedKeys = mutableListOf<String>()
        val scanner = SimilarScreenshotScanner(
            perceptualFingerprint = { candidate ->
                requestedKeys += candidate.contentKey
                "0000000000000000"
            },
        )
        val candidates = listOf(
            candidate("1", "IMG_1.jpg", "uri-a", relativePath = "DCIM/Camera/"),
            candidate("2", "IMG_2.jpg", "uri-b", relativePath = "DCIM/Camera/"),
        )

        val groups = scanner.findSimilarGroups(candidates)

        assertTrue(groups.isEmpty())
        assertTrue(requestedKeys.isEmpty())
    }

    @Test
    fun keepsDistantScreenshotHashesInSeparateGroups() {
        val scanner = SimilarScreenshotScanner(
            perceptualFingerprint = { candidate ->
                when (candidate.contentKey) {
                    "uri-a" -> "0000000000000000"
                    "uri-b" -> "ffffffffffffffff"
                    else -> null
                }
            },
            maxHashDistance = 4,
        )
        val candidates = listOf(
            candidate("1", "Screenshot_1.png", "uri-a", relativePath = "Pictures/Screenshots/"),
            candidate("2", "Screenshot_2.png", "uri-b", relativePath = "Pictures/Screenshots/"),
        )

        val groups = scanner.findSimilarGroups(candidates)

        assertTrue(groups.isEmpty())
    }

    @Test
    fun doesNotCompareScreenshotsWithDifferentDimensions() {
        val requestedKeys = mutableListOf<String>()
        val scanner = SimilarScreenshotScanner(
            perceptualFingerprint = { candidate ->
                requestedKeys += candidate.contentKey
                "0000000000000000"
            },
        )
        val candidates = listOf(
            candidate("1", "Screenshot_1.png", "uri-a", width = 1440, height = 3120),
            candidate("2", "Screenshot_2.png", "uri-b", width = 1080, height = 2400),
        )

        val groups = scanner.findSimilarGroups(candidates)

        assertTrue(groups.isEmpty())
        assertTrue(requestedKeys.isEmpty())
    }

    @Test
    fun providesCandidateMetadataToFingerprintReader() {
        val observedKeys = mutableListOf<PerceptualFingerprintCacheKey>()
        val scanner = SimilarScreenshotScanner(
            perceptualFingerprint = { candidate ->
                observedKeys += candidate.fingerprintCacheKey
                "0000000000000000"
            },
        )
        val candidates = listOf(
            candidate(
                id = "1",
                displayName = "Screenshot_1.png",
                contentKey = "uri-a",
                sizeBytes = 1_912_800L,
                dateMillis = 1_000L,
            ),
            candidate(
                id = "2",
                displayName = "Screenshot_2.png",
                contentKey = "uri-b",
                sizeBytes = 1_912_759L,
                dateMillis = 2_000L,
            ),
        )

        scanner.findSimilarGroups(candidates)

        assertEquals(
            listOf(
                PerceptualFingerprintCacheKey(
                    contentUri = "uri-a",
                    sizeBytes = 1_912_800L,
                    dateMillis = 1_000L,
                    width = 1440,
                    height = 3120,
                ),
                PerceptualFingerprintCacheKey(
                    contentUri = "uri-b",
                    sizeBytes = 1_912_759L,
                    dateMillis = 2_000L,
                    width = 1440,
                    height = 3120,
                ),
            ),
            observedKeys,
        )
    }

    @Test
    fun skipsFingerprintingScreenshotsWithoutNearbyCaptureNeighbors() {
        val requestedKeys = mutableListOf<String>()
        val scanner = SimilarScreenshotScanner(
            perceptualFingerprint = { candidate ->
                requestedKeys += candidate.contentKey
                "0000000000000000"
            },
            maxNearbyCaptureGapMillis = 10 * 60 * 1_000L,
        )
        val candidates = listOf(
            candidate("1", "Screenshot_1.png", "uri-a", dateMillis = 1_000L),
            candidate("2", "Screenshot_2.png", "uri-b", dateMillis = 30 * 60 * 1_000L),
        )

        val result = scanner.findSimilarGroupResult(candidates)

        assertTrue(result.groups.isEmpty())
        assertEquals(0, result.fingerprintCandidateCount)
        assertEquals(2, result.fingerprintSkippedCount)
        assertTrue(requestedKeys.isEmpty())
    }

    @Test
    fun skipsFingerprintingNearbyScreenshotsWithoutSimilarFileSizeNeighbors() {
        val requestedKeys = mutableListOf<String>()
        val scanner = SimilarScreenshotScanner(
            perceptualFingerprint = { candidate ->
                requestedKeys += candidate.contentKey
                when (candidate.contentKey) {
                    "similar-a", "similar-b" -> "0000000000000000"
                    else -> "ffffffffffffffff"
                }
            },
            maxNearbyCaptureGapMillis = 10 * 60 * 1_000L,
            maxNearbySizeDeltaRatio = 0.25,
        )
        val candidates = listOf(
            candidate("1", "Screenshot_1.png", "similar-a", sizeBytes = 1_000_000L),
            candidate("2", "Screenshot_2.png", "similar-b", sizeBytes = 1_100_000L),
            candidate("3", "Screenshot_3.png", "far-a", sizeBytes = 2_500_000L),
            candidate("4", "Screenshot_4.png", "far-b", sizeBytes = 4_000_000L),
        )

        val result = scanner.findSimilarGroupResult(candidates)

        assertEquals(listOf("similar-a", "similar-b"), requestedKeys)
        assertEquals(2, result.fingerprintCandidateCount)
        assertEquals(2, result.fingerprintSkippedCount)
        assertEquals(listOf(listOf("1", "2")), result.groups.map { group -> group.items.map { it.id }.sorted() })
    }

    @Test
    fun sortsGroupsByRecoverableSpaceThenRecentCaptureTime() {
        val scanner = SimilarScreenshotScanner(
            perceptualFingerprint = { candidate ->
                when (candidate.contentKey) {
                    "old-a", "old-b" -> "0000000000000000"
                    "recent-a", "recent-b" -> "00000000000000ff"
                    "largest-a", "largest-b" -> "000000000000ffff"
                    else -> null
                }
            },
            maxHashDistance = 1,
        )
        val candidates = listOf(
            candidate("101", "Screenshot_old_a.png", "old-a", sizeBytes = 2_000L, dateMillis = 1_000L),
            candidate("102", "Screenshot_old_b.png", "old-b", sizeBytes = 1_000L, dateMillis = 2_000L),
            candidate("201", "Screenshot_recent_a.png", "recent-a", sizeBytes = 2_000L, dateMillis = 5_000L),
            candidate("202", "Screenshot_recent_b.png", "recent-b", sizeBytes = 1_000L, dateMillis = 6_000L),
            candidate("301", "Screenshot_largest_a.png", "largest-a", sizeBytes = 4_000L, dateMillis = 3_000L),
            candidate("302", "Screenshot_largest_b.png", "largest-b", sizeBytes = 2_000L, dateMillis = 4_000L),
        )

        val groups = scanner.findSimilarGroups(candidates)

        assertEquals(
            listOf(
                listOf("301", "302"),
                listOf("201", "202"),
                listOf("101", "102"),
            ),
            groups.map { group -> group.items.map { it.id }.sorted() },
        )
    }

    private fun candidate(
        id: String,
        displayName: String,
        contentKey: String,
        relativePath: String = "Pictures/Screenshots/",
        sizeBytes: Long = 1_000_000L,
        dateMillis: Long = id.toLong() * 1_000L,
        width: Int = 1440,
        height: Int = 3120,
    ): SimilarScreenshotCandidate {
        return SimilarScreenshotCandidate(
            item = MediaItem(
                id = id,
                displayName = displayName,
                sizeBytes = sizeBytes,
                dateTakenMillis = dateMillis,
                contentHash = null,
                mediaType = MediaType.Image,
                contentUri = contentKey,
                width = width,
                height = height,
            ),
            contentKey = contentKey,
            relativePath = relativePath,
            cacheDateMillis = dateMillis,
        )
    }
}
