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
            perceptualFingerprint = { key ->
                when (key) {
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
            perceptualFingerprint = { key ->
                requestedKeys += key
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
            perceptualFingerprint = { key ->
                when (key) {
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
            perceptualFingerprint = { key ->
                requestedKeys += key
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

    private fun candidate(
        id: String,
        displayName: String,
        contentKey: String,
        relativePath: String = "Pictures/Screenshots/",
        width: Int = 1440,
        height: Int = 3120,
    ): SimilarScreenshotCandidate {
        return SimilarScreenshotCandidate(
            item = MediaItem(
                id = id,
                displayName = displayName,
                sizeBytes = 1_000_000L,
                dateTakenMillis = id.toLong() * 1_000L,
                contentHash = null,
                mediaType = MediaType.Image,
                contentUri = contentKey,
                width = width,
                height = height,
            ),
            contentKey = contentKey,
            relativePath = relativePath,
        )
    }
}
