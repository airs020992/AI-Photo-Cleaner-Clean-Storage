package com.air.cleaner.data.media

import com.air.cleaner.domain.cleaning.DuplicateGroup
import com.air.cleaner.domain.cleaning.MediaItem
import kotlin.math.abs

data class SimilarPhotoCandidate(
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

data class SimilarPhotoScanResult(
    val groups: List<DuplicateGroup>,
    val fingerprintCandidateCount: Int,
    val fingerprintSkippedCount: Int,
    val fingerprintTimeSkippedCount: Int = 0,
    val fingerprintSizeSkippedCount: Int = 0,
    val fingerprintCacheHitCount: Int = 0,
    val fingerprintCacheMissCount: Int = 0,
)

class SimilarPhotoScanner(
    private val perceptualFingerprint: (SimilarPhotoCandidate) -> String?,
    private val maxHashDistance: Int = DEFAULT_MAX_HASH_DISTANCE,
    private val maxNearbyCaptureGapMillis: Long = DEFAULT_MAX_NEARBY_CAPTURE_GAP_MILLIS,
    private val maxFingerprintCandidateGapMillis: Long = DEFAULT_MAX_FINGERPRINT_CANDIDATE_GAP_MILLIS,
    private val minFingerprintCaptureClusterSize: Int = DEFAULT_MIN_FINGERPRINT_CAPTURE_CLUSTER_SIZE,
    private val maxNearbySizeDeltaRatio: Double = DEFAULT_MAX_NEARBY_SIZE_DELTA_RATIO,
) {
    fun findSimilarGroups(candidates: List<SimilarPhotoCandidate>): List<DuplicateGroup> {
        return findSimilarGroupResult(candidates).groups
    }

    fun findSimilarGroupResult(candidates: List<SimilarPhotoCandidate>): SimilarPhotoScanResult {
        var groupIndex = 0
        var fingerprintCandidateCount = 0
        var fingerprintSkippedCount = 0
        var fingerprintTimeSkippedCount = 0
        var fingerprintSizeSkippedCount = 0
        val groups = candidates
            .asSequence()
            .filterNot { it.isScreenshot() }
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
                        FingerprintedPhoto(candidate, hash)
                    }
                }
                fingerprinted.toSimilarGroups(
                    nextKey = {
                        groupIndex += 1
                        "similar-photo:$groupIndex"
                    },
                )
            }
            .sortedWith(
                compareByDescending<DuplicateGroup> { it.recoverableBytes }
                    .thenByDescending { group -> group.items.maxOf { it.dateTakenMillis } },
            )
        return SimilarPhotoScanResult(
            groups = groups,
            fingerprintCandidateCount = fingerprintCandidateCount,
            fingerprintSkippedCount = fingerprintSkippedCount,
            fingerprintTimeSkippedCount = fingerprintTimeSkippedCount,
            fingerprintSizeSkippedCount = fingerprintSizeSkippedCount,
        )
    }

    private fun List<FingerprintedPhoto>.toSimilarGroups(nextKey: () -> String): List<DuplicateGroup> {
        val visited = mutableSetOf<String>()
        val groups = mutableListOf<DuplicateGroup>()

        forEach { seed ->
            if (seed.item.id in visited) return@forEach
            val group = filter { candidate ->
                candidate.item.id !in visited && candidate.isSimilarTo(seed)
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

    private fun FingerprintedPhoto.isSimilarTo(seed: FingerprintedPhoto): Boolean {
        return item.id == seed.item.id ||
            (
                seed.candidate.canGroupWith(candidate) &&
                    ImageContentFingerprinter.hammingDistance(seed.hash, hash) <= maxHashDistance
                )
    }

    private fun List<SimilarPhotoCandidate>.withNearbyCaptureNeighbors(): List<SimilarPhotoCandidate> {
        return filter { candidate ->
            count { other ->
                other.item.id != candidate.item.id &&
                    abs(other.item.dateTakenMillis - candidate.item.dateTakenMillis) <= maxFingerprintCandidateGapMillis
            } >= minFingerprintCaptureClusterSize - 1
        }
    }

    private fun List<SimilarPhotoCandidate>.withNearbySizeNeighbors(): List<SimilarPhotoCandidate> {
        return filter { candidate ->
            any { other ->
                other.item.id != candidate.item.id &&
                    candidate.hasNearbySize(other)
            }
        }
    }

    private fun SimilarPhotoCandidate.hasNearbySize(other: SimilarPhotoCandidate): Boolean {
        val largerSize = maxOf(item.sizeBytes, other.item.sizeBytes)
        val allowedDelta = maxOf(MIN_NEARBY_SIZE_DELTA_BYTES, (largerSize * maxNearbySizeDeltaRatio).toLong())
        return abs(item.sizeBytes - other.item.sizeBytes) <= allowedDelta
    }

    private fun SimilarPhotoCandidate.canGroupWith(other: SimilarPhotoCandidate): Boolean {
        return abs(item.dateTakenMillis - other.item.dateTakenMillis) <= maxNearbyCaptureGapMillis &&
            hasNearbySize(other)
    }

    private fun SimilarPhotoCandidate.isScreenshot(): Boolean {
        return item.displayName.contains("screenshot", ignoreCase = true) ||
            relativePath.orEmpty().contains("screenshot", ignoreCase = true)
    }

    private data class FingerprintedPhoto(
        val candidate: SimilarPhotoCandidate,
        val hash: String,
    ) {
        val item: MediaItem = candidate.item
    }

    companion object {
        private const val DEFAULT_MAX_HASH_DISTANCE = 10
        private const val DEFAULT_MAX_NEARBY_CAPTURE_GAP_MILLIS = 2 * 60 * 1_000L
        private const val DEFAULT_MAX_FINGERPRINT_CANDIDATE_GAP_MILLIS = 0L
        private const val DEFAULT_MIN_FINGERPRINT_CAPTURE_CLUSTER_SIZE = 3
        private const val DEFAULT_MAX_NEARBY_SIZE_DELTA_RATIO = 0.18
        private const val MIN_NEARBY_SIZE_DELTA_BYTES = 384 * 1_024L
    }
}
