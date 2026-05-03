package com.air.cleaner.feature.photos

import com.air.cleaner.domain.cleaning.DuplicateGroup
import com.air.cleaner.domain.cleaning.MediaItem
import com.air.cleaner.domain.cleaning.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhotoDeleteReconciliationTest {
    @Test
    fun reconcilesRequestedUrisThatNoLongerAppearInDuplicateReview() {
        val reconciliation = PhotoDeleteReconciliation.from(
            summary = deleteSummary("content://images/a", "content://images/b", bytes = 2_000L),
            result = PhotoDeletionResult(PhotoDeletionStatus.Deleted, itemCount = 2, bytes = 2_000L),
            currentGroups = emptyList(),
        )

        assertEquals(2, reconciliation?.resolvedItemCount)
        assertEquals(0, reconciliation?.stillNeedsReviewCount)
        assertEquals(0, reconciliation?.remainingGroupCount)
        assertEquals(0L, reconciliation?.remainingRecoverableBytes)
    }

    @Test
    fun reportsRequestedUrisThatStillAppearInDuplicateReview() {
        val reconciliation = PhotoDeleteReconciliation.from(
            summary = deleteSummary("content://images/a", "content://images/b", bytes = 2_000L),
            result = PhotoDeletionResult(PhotoDeletionStatus.Deleted, itemCount = 2, bytes = 2_000L),
            currentGroups = listOf(
                duplicateGroup(
                    item("1", contentUri = "content://images/b"),
                    item("2", contentUri = "content://images/c"),
                ),
            ),
        )

        assertEquals(1, reconciliation?.resolvedItemCount)
        assertEquals(1, reconciliation?.stillNeedsReviewCount)
        assertEquals(listOf("content://images/b"), reconciliation?.stillNeedsReviewUris)
        assertEquals(1, reconciliation?.remainingGroupCount)
        assertEquals(1_000L, reconciliation?.remainingRecoverableBytes)
    }

    @Test
    fun prioritizesMediaStoreExistenceWhenAvailable() {
        val reconciliation = PhotoDeleteReconciliation.from(
            summary = deleteSummary("content://images/a", "content://images/b", bytes = 2_000L),
            result = PhotoDeletionResult(PhotoDeletionStatus.Deleted, itemCount = 2, bytes = 2_000L),
            currentGroups = emptyList(),
            stillExistingContentUris = listOf("content://images/b"),
        )

        assertEquals(1, reconciliation?.resolvedItemCount)
        assertEquals(1, reconciliation?.stillExistsCount)
        assertEquals(listOf("content://images/b"), reconciliation?.stillExistingUris)
        assertEquals(0, reconciliation?.stillNeedsReviewCount)
    }

    @Test
    fun ignoresCanceledOrBlockedResults() {
        assertNull(
            PhotoDeleteReconciliation.from(
                summary = deleteSummary("content://images/a", bytes = 1_000L),
                result = PhotoDeletionResult(PhotoDeletionStatus.Canceled, itemCount = 1, bytes = 1_000L),
                currentGroups = emptyList(),
            ),
        )
        assertNull(
            PhotoDeleteReconciliation.from(
                summary = deleteSummary("content://images/a", bytes = 1_000L),
                result = PhotoDeletionResult(PhotoDeletionStatus.Blocked, itemCount = 1, bytes = 1_000L),
                currentGroups = emptyList(),
            ),
        )
    }

    private fun deleteSummary(vararg contentUris: String, bytes: Long): PhotoDeletionSummary {
        return PhotoDeletionSummary(
            itemCount = contentUris.size,
            bytesToDelete = bytes,
            contentUris = contentUris.toList(),
        )
    }

    private fun duplicateGroup(vararg items: MediaItem): DuplicateGroup {
        return DuplicateGroup(key = "group", items = items.toList())
    }

    private fun item(id: String, contentUri: String): MediaItem {
        return MediaItem(
            id = id,
            displayName = "$id.jpg",
            sizeBytes = 1_000L,
            dateTakenMillis = id.toLong(),
            contentHash = "same",
            mediaType = MediaType.Image,
            contentUri = contentUri,
        )
    }
}
