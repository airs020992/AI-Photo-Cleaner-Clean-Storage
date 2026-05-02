package com.air.cleaner.data.media

import com.air.cleaner.domain.cleaning.DuplicateGroup
import com.air.cleaner.domain.cleaning.DuplicatePhotoDetector
import com.air.cleaner.domain.cleaning.MediaItem

data class DuplicatePhotoCandidate(
    val item: MediaItem,
    val contentKey: String,
)

class ExactDuplicatePhotoScanner(
    private val contentFingerprint: (String) -> String?,
) {
    fun findDuplicateGroups(candidates: List<DuplicatePhotoCandidate>): List<DuplicateGroup> {
        val fingerprintedItems = candidates
            .groupBy { it.item.sizeBytes }
            .filterValues { it.size > 1 }
            .values
            .flatten()
            .mapNotNull { candidate ->
                contentFingerprint(candidate.contentKey)?.let { fingerprint ->
                    candidate.item.copy(contentHash = "sha256:$fingerprint")
                }
            }

        return DuplicatePhotoDetector().findDuplicates(fingerprintedItems)
    }
}
