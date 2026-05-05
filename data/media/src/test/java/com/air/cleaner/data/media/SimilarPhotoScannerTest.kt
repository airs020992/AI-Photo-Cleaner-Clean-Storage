package com.air.cleaner.data.media

import com.air.cleaner.domain.cleaning.MediaItem
import com.air.cleaner.domain.cleaning.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimilarPhotoScannerTest {
    @Test
    fun groupsCameraPhotosWithNearbyPerceptualHashes() {
        val scanner = SimilarPhotoScanner(
            perceptualFingerprint = { candidate ->
                when (candidate.contentKey) {
                    "uri-a" -> "0000000000000000"
                    "uri-b" -> "0000000000000003"
                    else -> null
                }
            },
            maxHashDistance = 2,
            maxFingerprintCandidateGapMillis = 2 * 60 * 1_000L,
            minFingerprintCaptureClusterSize = 2,
        )
        val candidates = listOf(
            candidate("1", "IMG_1001.jpg", "uri-a", dateMillis = 1_000L),
            candidate("2", "IMG_1002.jpg", "uri-b", dateMillis = 61_000L),
        )

        val result = scanner.findSimilarGroupResult(candidates)

        assertEquals(1, result.groups.size)
        assertEquals("similar-photo:1", result.groups.single().key)
        assertEquals(listOf("1", "2"), result.groups.single().items.map { it.id }.sorted())
        assertEquals(2, result.fingerprintCandidateCount)
        assertEquals(0, result.fingerprintSkippedCount)
    }

    @Test
    fun ignoresScreenshotsSoTheDedicatedScreenshotFlowCanUseItsOwnKeepStrategy() {
        val requestedKeys = mutableListOf<String>()
        val scanner = SimilarPhotoScanner(
            perceptualFingerprint = { candidate ->
                requestedKeys += candidate.contentKey
                "0000000000000000"
            },
        )
        val candidates = listOf(
            candidate("1", "Screenshot_1.png", "uri-a", relativePath = "Pictures/Screenshots/"),
            candidate("2", "Screenshot_2.png", "uri-b", relativePath = "Pictures/Screenshots/"),
        )

        val result = scanner.findSimilarGroupResult(candidates)

        assertTrue(result.groups.isEmpty())
        assertEquals(0, result.fingerprintCandidateCount)
        assertTrue(requestedKeys.isEmpty())
    }

    @Test
    fun skipsFingerprintingPhotosWithoutNearbyCaptureNeighbors() {
        val requestedKeys = mutableListOf<String>()
        val scanner = SimilarPhotoScanner(
            perceptualFingerprint = { candidate ->
                requestedKeys += candidate.contentKey
                "0000000000000000"
            },
            maxNearbyCaptureGapMillis = 10 * 60 * 1_000L,
        )
        val candidates = listOf(
            candidate("1", "IMG_1001.jpg", "uri-a", dateMillis = 1_000L),
            candidate("2", "IMG_1002.jpg", "uri-b", dateMillis = 45 * 60 * 1_000L),
        )

        val result = scanner.findSimilarGroupResult(candidates)

        assertTrue(result.groups.isEmpty())
        assertEquals(0, result.fingerprintCandidateCount)
        assertEquals(2, result.fingerprintSkippedCount)
        assertEquals(2, result.fingerprintTimeSkippedCount)
        assertEquals(0, result.fingerprintSizeSkippedCount)
        assertTrue(requestedKeys.isEmpty())
    }

    @Test
    fun skipsFingerprintingPhotosOutsideTheDefaultRepeatedMomentWindow() {
        val requestedKeys = mutableListOf<String>()
        val scanner = SimilarPhotoScanner(
            perceptualFingerprint = { candidate ->
                requestedKeys += candidate.contentKey
                "0000000000000000"
            },
        )
        val candidates = listOf(
            candidate("1", "IMG_1001.jpg", "uri-a", dateMillis = 1_000L),
            candidate("2", "IMG_1002.jpg", "uri-b", dateMillis = 3 * 60 * 1_000L),
        )

        val result = scanner.findSimilarGroupResult(candidates)

        assertTrue(result.groups.isEmpty())
        assertEquals(0, result.fingerprintCandidateCount)
        assertEquals(2, result.fingerprintSkippedCount)
        assertEquals(2, result.fingerprintTimeSkippedCount)
        assertTrue(requestedKeys.isEmpty())
    }

    @Test
    fun skipsFingerprintingNearbyPhotosWithoutSimilarFileSizeNeighbors() {
        val requestedKeys = mutableListOf<String>()
        val scanner = SimilarPhotoScanner(
            perceptualFingerprint = { candidate ->
                requestedKeys += candidate.contentKey
                "0000000000000000"
            },
            maxFingerprintCandidateGapMillis = 1_000L,
            minFingerprintCaptureClusterSize = 2,
            maxNearbySizeDeltaRatio = 0.18,
        )
        val candidates = listOf(
            candidate("1", "IMG_1001.jpg", "uri-a", sizeBytes = 1_000_000L),
            candidate("2", "IMG_1002.jpg", "uri-b", sizeBytes = 1_700_000L),
        )

        val result = scanner.findSimilarGroupResult(candidates)

        assertTrue(result.groups.isEmpty())
        assertEquals(0, result.fingerprintCandidateCount)
        assertEquals(2, result.fingerprintSkippedCount)
        assertEquals(0, result.fingerprintTimeSkippedCount)
        assertEquals(2, result.fingerprintSizeSkippedCount)
        assertTrue(requestedKeys.isEmpty())
    }

    @Test
    fun keepsVisuallyDifferentNearbyPhotosOutOfGroups() {
        val scanner = SimilarPhotoScanner(
            perceptualFingerprint = { candidate ->
                when (candidate.contentKey) {
                    "uri-a" -> "0000000000000000"
                    "uri-b" -> "ffffffffffffffff"
                    else -> null
                }
            },
            maxHashDistance = 4,
            maxFingerprintCandidateGapMillis = 1_000L,
            minFingerprintCaptureClusterSize = 2,
        )
        val candidates = listOf(
            candidate("1", "IMG_1001.jpg", "uri-a", dateMillis = 1_000L),
            candidate("2", "IMG_1002.jpg", "uri-b", dateMillis = 2_000L),
        )

        val result = scanner.findSimilarGroupResult(candidates)

        assertTrue(result.groups.isEmpty())
        assertEquals(2, result.fingerprintCandidateCount)
    }

    @Test
    fun keepsSimilarPhotosFromDifferentCaptureSessionsInSeparateGroups() {
        val scanner = SimilarPhotoScanner(
            perceptualFingerprint = { "0000000000000000" },
            maxNearbyCaptureGapMillis = 10 * 60 * 1_000L,
            maxFingerprintCandidateGapMillis = 1_000L,
            minFingerprintCaptureClusterSize = 2,
        )
        val candidates = listOf(
            candidate("1", "IMG_1001.jpg", "uri-a", dateMillis = 1_000L),
            candidate("2", "IMG_1002.jpg", "uri-b", dateMillis = 2_000L),
            candidate("3", "IMG_2001.jpg", "uri-c", dateMillis = 45 * 60 * 1_000L),
            candidate("4", "IMG_2002.jpg", "uri-d", dateMillis = 45 * 60 * 1_000L + 1_000L),
        )

        val result = scanner.findSimilarGroupResult(candidates)

        assertEquals(
            listOf(listOf("3", "4"), listOf("1", "2")),
            result.groups.map { group -> group.items.map { it.id }.sorted() },
        )
    }

    @Test
    fun doesNotRepeatTheSamePhotoAcrossOverlappingSimilarityGroups() {
        val scanner = SimilarPhotoScanner(
            perceptualFingerprint = { candidate ->
                when (candidate.contentKey) {
                    "uri-a" -> "0000000000000000"
                    "uri-b" -> "0000000000000001"
                    "uri-c" -> "0000000000000003"
                    else -> null
                }
            },
            maxHashDistance = 1,
            maxFingerprintCandidateGapMillis = 1_000L,
            minFingerprintCaptureClusterSize = 2,
        )
        val candidates = listOf(
            candidate("1", "IMG_1001.jpg", "uri-a", dateMillis = 1_000L),
            candidate("2", "IMG_1002.jpg", "uri-b", dateMillis = 2_000L),
            candidate("3", "IMG_1003.jpg", "uri-c", dateMillis = 3_000L),
        )

        val result = scanner.findSimilarGroupResult(candidates)

        assertEquals(
            result.groups.flatMap { group -> group.items.map { it.id } }.distinct(),
            result.groups.flatMap { group -> group.items.map { it.id } },
        )
    }

    @Test
    fun keepsBorderlineDifferentPhotosOutOfDeletionGroups() {
        val scanner = SimilarPhotoScanner(
            perceptualFingerprint = { candidate ->
                when (candidate.contentKey) {
                    "uri-a" -> "0000000000000000"
                    "uri-b" -> "00000000000007ff"
                    else -> null
                }
            },
            maxHashDistance = 10,
            maxFingerprintCandidateGapMillis = 1_000L,
            minFingerprintCaptureClusterSize = 2,
        )
        val candidates = listOf(
            candidate("1", "IMG_1001.jpg", "uri-a", dateMillis = 1_000L),
            candidate("2", "IMG_1002.jpg", "uri-b", dateMillis = 2_000L),
        )

        val result = scanner.findSimilarGroupResult(candidates)

        assertTrue(result.groups.isEmpty())
        assertEquals(2, result.fingerprintCandidateCount)
    }

    @Test
    fun doesNotFingerprintLargePhotoSizeJumpsFromTheSameMoment() {
        val requestedKeys = mutableListOf<String>()
        val scanner = SimilarPhotoScanner(
            perceptualFingerprint = { candidate ->
                requestedKeys += candidate.contentKey
                "0000000000000000"
            },
            minFingerprintCaptureClusterSize = 2,
            maxNearbySizeDeltaRatio = 0.18,
        )
        val candidates = listOf(
            candidate("1", "IMG_1001.jpg", "uri-a", sizeBytes = 1_000_000L, dateMillis = 1_000L),
            candidate("2", "IMG_1002.jpg", "uri-b", sizeBytes = 1_565_000L, dateMillis = 1_000L),
        )

        val result = scanner.findSimilarGroupResult(candidates)

        assertTrue(result.groups.isEmpty())
        assertEquals(0, result.fingerprintCandidateCount)
        assertEquals(2, result.fingerprintSkippedCount)
        assertEquals(2, result.fingerprintSizeSkippedCount)
        assertTrue(requestedKeys.isEmpty())
    }

    @Test
    fun prunesDenseTravelSessionsBeforeFingerprintingButKeepsBurstPairs() {
        val requestedKeys = mutableListOf<String>()
        val scanner = SimilarPhotoScanner(
            perceptualFingerprint = { candidate ->
                requestedKeys += candidate.contentKey
                "0000000000000000"
            },
        )
        val denseSession = (1..20).map { index ->
            candidate(
                id = index.toString(),
                displayName = "DSCF${7000 + index}.JPG",
                contentKey = "uri-$index",
                dateMillis = index * 20_000L,
                sizeBytes = 5_000_000L + index,
            )
        }
        val burstPair = listOf(
            candidate("101", "DSCF8001.JPG", "uri-burst-a", dateMillis = 900_000L, sizeBytes = 5_400_000L),
            candidate("102", "DSCF8002.JPG", "uri-burst-b", dateMillis = 900_000L, sizeBytes = 5_410_000L),
            candidate("103", "DSCF8003.JPG", "uri-burst-c", dateMillis = 900_000L, sizeBytes = 5_405_000L),
        )

        val result = scanner.findSimilarGroupResult(denseSession + burstPair)

        assertEquals(1, result.groups.size)
        assertEquals(listOf("101", "102", "103"), result.groups.single().items.map { it.id }.sorted())
        assertEquals(3, result.fingerprintCandidateCount)
        assertEquals(20, result.fingerprintSkippedCount)
        assertEquals(listOf("uri-burst-a", "uri-burst-b", "uri-burst-c"), requestedKeys.sorted())
    }

    private fun candidate(
        id: String,
        displayName: String,
        contentKey: String,
        relativePath: String = "DCIM/Camera/",
        sizeBytes: Long = 1_500_000L,
        dateMillis: Long = id.toLong() * 1_000L,
        width: Int = 3024,
        height: Int = 4032,
    ): SimilarPhotoCandidate {
        return SimilarPhotoCandidate(
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
