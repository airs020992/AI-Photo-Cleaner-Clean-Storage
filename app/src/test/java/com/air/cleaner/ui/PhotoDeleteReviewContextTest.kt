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
            similarPhotoGroups = listOf(group("similar-photo")),
            similarScreenshotGroups = similarScreenshotGroups,
        )

        assertEquals(listOf("duplicate"), groups.map { it.key })
    }

    @Test
    fun similarPhotoDeletesReconcileAgainstSimilarPhotoGroups() {
        val similarPhotoGroups = listOf(group("similar-photo"))

        val groups = PhotoDeleteReviewContext.SimilarPhotos.groupsForReconciliation(
            duplicatePhotoGroups = listOf(group("duplicate")),
            similarPhotoGroups = similarPhotoGroups,
            similarScreenshotGroups = listOf(group("similar-screenshot")),
        )

        assertEquals(listOf("similar-photo"), groups.map { it.key })
    }

    @Test
    fun similarScreenshotDeletesReconcileAgainstSimilarScreenshotGroups() {
        val duplicateGroups = listOf(group("duplicate"))
        val similarScreenshotGroups = listOf(group("similar"))

        val groups = PhotoDeleteReviewContext.SimilarScreenshots.groupsForReconciliation(
            duplicatePhotoGroups = duplicateGroups,
            similarPhotoGroups = listOf(group("similar-photo")),
            similarScreenshotGroups = similarScreenshotGroups,
        )

        assertEquals(listOf("similar"), groups.map { it.key })
    }

    @Test
    fun largeVideoDeletesDoNotReconcileAgainstPhotoGroups() {
        val groups = PhotoDeleteReviewContext.LargeVideos.groupsForReconciliation(
            duplicatePhotoGroups = listOf(group("duplicate")),
            similarPhotoGroups = listOf(group("similar-photo")),
            similarScreenshotGroups = listOf(group("similar-screenshot")),
        )

        assertEquals(emptyList<DuplicateGroup>(), groups)
    }

    @Test
    fun largeVideoDeleteCopyUsesVideoLanguage() {
        assertEquals("Delete selected videos?", PhotoDeleteReviewContext.LargeVideos.deleteDialogTitle)
        assertEquals("Videos", PhotoDeleteReviewContext.LargeVideos.deleteDialogItemLabel)
        assertEquals(
            "Your library was refreshed. Continue reviewing any remaining large videos.",
            PhotoDeleteReviewContext.LargeVideos.deleteResultDeletedMessage,
        )
        assertEquals(
            "No videos were removed. Your previous selection is still available for review.",
            PhotoDeleteReviewContext.LargeVideos.deleteResultCanceledMessage,
        )
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
