package com.air.cleaner.data.media

import com.air.cleaner.domain.cleaning.DuplicateGroup
import com.air.cleaner.domain.cleaning.MediaItem

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

class SimilarScreenshotScanner(
    private val perceptualFingerprint: (SimilarScreenshotCandidate) -> String?,
    private val maxHashDistance: Int = DEFAULT_MAX_HASH_DISTANCE,
) {
    fun findSimilarGroups(candidates: List<SimilarScreenshotCandidate>): List<DuplicateGroup> {
        var groupIndex = 0
        return candidates
            .asSequence()
            .filter { it.isScreenshot() }
            .filter { it.item.width != null && it.item.height != null }
            .groupBy { "${it.item.width}:${it.item.height}" }
            .filterValues { it.size > 1 }
            .values
            .flatMap { bucket ->
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
    }
}
