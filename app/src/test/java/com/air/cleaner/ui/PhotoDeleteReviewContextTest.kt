package com.air.cleaner.ui

import com.air.cleaner.domain.cleaning.DuplicateGroup
import com.air.cleaner.domain.cleaning.MediaItem
import com.air.cleaner.domain.cleaning.MediaType
import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoDeleteReviewContextTest {
    @Test
    fun duplicatePhotoDeletesReconcileAgainstDuplicateGroups() {
        val duplicateGroups = listOf(group("duplicate"))
        val similarScreenshotGroups = listOf(group("similar"))

        val groups = PhotoDeleteReviewContext.DuplicatePhotos.groupsForReconciliation(
            duplicatePhotoGroups = duplicateGroups,
            similarScreenshotGroups = similarScreenshotGroups,
        )

        assertEquals(listOf("duplicate"), groups.map { it.key })
    }

    @Test
    fun similarScreenshotDeletesReconcileAgainstSimilarScreenshotGroups() {
        val duplicateGroups = listOf(group("duplicate"))
        val similarScreenshotGroups = listOf(group("similar"))

        val groups = PhotoDeleteReviewContext.SimilarScreenshots.groupsForReconciliation(
            duplicatePhotoGroups = duplicateGroups,
            similarScreenshotGroups = similarScreenshotGroups,
        )

        assertEquals(listOf("similar"), groups.map { it.key })
    }

    private fun group(key: String): DuplicateGroup {
        return DuplicateGroup(
            key = key,
            items = listOf(
                MediaItem(
                    id = "$key-a",
                    displayName = "$key-a.jpg",
                    sizeBytes = 2_000L,
                    dateTakenMillis = 1_000L,
                    contentHash = key,
                    mediaType = MediaType.Image,
                    contentUri = "content://images/$key-a",
                ),
                MediaItem(
                    id = "$key-b",
                    displayName = "$key-b.jpg",
                    sizeBytes = 1_000L,
                    dateTakenMillis = 2_000L,
                    contentHash = key,
                    mediaType = MediaType.Image,
                    contentUri = "content://images/$key-b",
                ),
            ),
        )
    }
}
