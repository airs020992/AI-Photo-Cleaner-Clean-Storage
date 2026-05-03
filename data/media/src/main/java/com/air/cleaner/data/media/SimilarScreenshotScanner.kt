package com.air.cleaner.data.media

import com.air.cleaner.domain.cleaning.DuplicateGroup
import com.air.cleaner.domain.cleaning.MediaItem
import kotlin.math.abs

data class SimilarScreenshotCandidate(
    val item: MediaItem,
    val contentKey: String,
    val relativePath: String?,
    val cacheDateMillis: Long = item.dateTakenMillis,
) {
    val fingerprintCacheKey: PerceptualFingerprintCacheKey
        get() = PerceptualFingerprintCacheKey(
            contentUri = contentKey,
            sizeBytes = item.sizeBytes,
            dateMillis = cacheDateMillis,
            width = item.width,
            height = item.height,
        )
}

data class SimilarScreenshotScanResult(
    val groups: List<DuplicateGroup>,
    val fingerprintCandidateCount: Int,
    val fingerprintSkippedCount: Int,
    val fingerprintTimeSkippedCount: Int = 0,
    val fingerprintSizeSkippedCount: Int = 0,
    val fingerprintCacheHitCount: Int = 0,
    val fingerprintCacheMissCount: Int = 0,
)

class SimilarScreenshotScanner(
    private val perceptualFingerprint: (SimilarScreenshotCandidate) -> String?,
    private val maxHashDistance: Int = DEFAULT_MAX_HASH_DISTANCE,
    private val maxNearbyCaptureGapMillis: Long = DEFAULT_MAX_NEARBY_CAPTURE_GAP_MILLIS,
    private val maxNearbySizeDeltaRatio: Double = DEFAULT_MAX_NEARBY_SIZE_DELTA_RATIO,
) {
    fun findSimilarGroups(candidates: List<SimilarScreenshotCandidate>): List<DuplicateGroup> {
        return findSimilarGroupResult(candidates).groups
    }

    fun findSimilarGroupResult(candidates: List<SimilarScreenshotCandidate>): SimilarScreenshotScanResult {
        var groupIndex = 0
        var fingerprintCandidateCount = 0
        var fingerprintSkippedCount = 0
        var fingerprintTimeSkippedCount = 0
        var fingerprintSizeSkippedCount = 0
        val groups = candidates
            .asSequence()
            .filter { it.isScreenshot() }
            .filter { it.item.width != null && it.item.height != null }
            .groupBy { "${it.item.width}:${it.item.height}" }
            .filterValues { it.size > 1 }
            .values
            .map { bucket ->
                val nearbyBucket = bucket.withNearbyCaptureNeighbors()
                val sizeNearbyBucket = nearbyBucket.withNearbySizeNeighbors()
                val timeSkippedCount = bucket.size - nearbyBucket.size
                val sizeSkippedCount = nearbyBucket.size - sizeNearbyBucket.size
                fingerprintTimeSkippedCount += timeSkippedCount
                fingerprintSizeSkippedCount += sizeSkippedCount
                fingerprintSkippedCount += timeSkippedCount + sizeSkippedCount
                sizeNearbyBucket
            }
            .filter { it.size > 1 }
            .flatMap { bucket ->
                fingerprintCandidateCount += bucket.size
                val fingerprinted = bucket.mapNotNull { candidate ->
                    perceptualFingerprint(candidate)?.let { hash ->
                        FingerprintedScreenshot(candidate.item, hash)
                    }
                }
                fingerprinted.toSimilarGroups(
                    nextKey = {
                        groupIndex += 1
                        "similar-screenshot:$groupIndex"
                    },
                )
            }
            .sortedWith(
                compareByDescending<DuplicateGroup> { it.recoverableBytes }
                    .thenByDescending { group -> group.items.maxOf { it.dateTakenMillis } },
            )
        return SimilarScreenshotScanResult(
            groups = groups,
            fingerprintCandidateCount = fingerprintCandidateCount,
            fingerprintSkippedCount = fingerprintSkippedCount,
            fingerprintTimeSkippedCount = fingerprintTimeSkippedCount,
            fingerprintSizeSkippedCount = fingerprintSizeSkippedCount,
        )
    }

    private fun List<FingerprintedScreenshot>.toSimilarGroups(nextKey: () -> String): List<DuplicateGroup> {
        val visited = mutableSetOf<String>()
        val groups = mutableListOf<DuplicateGroup>()

        forEach { seed ->
            if (seed.item.id in visited) return@forEach
            val group = filter { candidate ->
                candidate.item.id == seed.item.id ||
                    ImageContentFingerprinter.hammingDistance(seed.hash, candidate.hash) <= maxHashDistance
            }
            if (group.size > 1) {
                group.forEach { visited += it.item.id }
                groups += DuplicateGroup(
                    key = nextKey(),
                    items = group.map { it.item },
                )
            } else {
                visited += seed.item.id
            }
        }

        return groups
    }

    private fun List<SimilarScreenshotCandidate>.withNearbyCaptureNeighbors(): List<SimilarScreenshotCandidate> {
        return filter { candidate ->
            any { other ->
                other.item.id != candidate.item.id &&
                    abs(other.item.dateTakenMillis - candidate.item.dateTakenMillis) <= maxNearbyCaptureGapMillis
            }
        }
    }

    private fun List<SimilarScreenshotCandidate>.withNearbySizeNeighbors(): List<SimilarScreenshotCandidate> {
        return filter { candidate ->
            any { other ->
                other.item.id != candidate.item.id &&
                    candidate.hasNearbySize(other)
            }
        }
    }

    private fun SimilarScreenshotCandidate.hasNearbySize(other: SimilarScreenshotCandidate): Boolean {
        val largerSize = maxOf(item.sizeBytes, other.item.sizeBytes)
        val allowedDelta = maxOf(MIN_NEARBY_SIZE_DELTA_BYTES, (largerSize * maxNearbySizeDeltaRatio).toLong())
        return abs(item.sizeBytes - other.item.sizeBytes) <= allowedDelta
    }

    private fun SimilarScreenshotCandidate.isScreenshot(): Boolean {
        return item.displayName.contains("screenshot", ignoreCase = true) ||
            relativePath.orEmpty().contains("screenshot", ignoreCase = true)
    }

    private data class FingerprintedScreenshot(
        val item: MediaItem,
        val hash: String,
    )

    companion object {
        private const val DEFAULT_MAX_HASH_DISTANCE = 18
        private const val DEFAULT_MAX_NEARBY_CAPTURE_GAP_MILLIS = 10 * 60 * 1_000L
        private const val DEFAULT_MAX_NEARBY_SIZE_DELTA_RATIO = 0.25
        private const val MIN_NEARBY_SIZE_DELTA_BYTES = 256 * 1_024L
    }
}
