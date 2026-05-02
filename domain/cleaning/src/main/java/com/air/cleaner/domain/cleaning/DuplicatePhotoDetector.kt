package com.air.cleaner.domain.cleaning

class DuplicatePhotoDetector {
    fun findDuplicates(items: List<MediaItem>): List<DuplicateGroup> {
        return items
            .asSequence()
            .filter { it.mediaType == MediaType.Image }
            .filter { !it.contentHash.isNullOrBlank() }
            .groupBy { it.contentHash.orEmpty() }
            .filterValues { it.size > 1 }
            .map { (hash, groupItems) ->
                DuplicateGroup(
                    key = hash,
                    items = groupItems.sortedWith(
                        compareByDescending<MediaItem> { it.sizeBytes }
                            .thenBy { it.dateTakenMillis },
                    ),
                )
            }
            .sortedByDescending { it.recoverableBytes }
    }
}
