package com.air.cleaner.feature.photos

import com.air.cleaner.domain.cleaning.DuplicateGroup
import com.air.cleaner.domain.cleaning.MediaItem
import com.air.cleaner.domain.cleaning.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhotoPostDeleteStatusTest {
    @Test
    fun summarizesSuccessfulDeleteWhenDuplicatesRemain() {
        val result = PhotoDeletionResult(
            status = PhotoDeletionStatus.Deleted,
            itemCount = 2,
            bytes = 3_000L,
        )

        val status = PhotoPostDeleteStatus.from(
            result = result,
            currentGroups = listOf(duplicateGroup("a", "b")),
        )

        assertEquals("2 photos removed", status?.title)
        assertEquals(1, status?.remainingGroupCount)
        assertEquals(1_000L, status?.remainingRecoverableBytes)
        assertEquals("1 duplicate group still needs review", status?.message)
    }

    @Test
    fun summarizesSuccessfulDeleteWhenAllDuplicatesAreGone() {
        val result = PhotoDeletionResult(
            status = PhotoDeletionStatus.Deleted,
            itemCount = 3,
            bytes = 9_000L,
        )

        val status = PhotoPostDeleteStatus.from(
            result = result,
            currentGroups = emptyList(),
        )

        assertEquals("All duplicate photos cleared", status?.title)
        assertEquals(0, status?.remainingGroupCount)
        assertEquals(0L, status?.remainingRecoverableBytes)
        assertEquals("3 photos removed from this cleanup", status?.message)
    }

    @Test
    fun doesNotCreatePostDeleteStatusForCanceledOrBlockedResult() {
        assertNull(
            PhotoPostDeleteStatus.from(
                result = PhotoDeletionResult(PhotoDeletionStatus.Canceled, itemCount = 2, bytes = 3_000L),
                currentGroups = emptyList(),
            ),
        )
        assertNull(
            PhotoPostDeleteStatus.from(
                result = PhotoDeletionResult(PhotoDeletionStatus.Blocked, itemCount = 2, bytes = 3_000L),
                currentGroups = emptyList(),
            ),
        )
    }

    @Test
    fun summarizesReconciliationWhenSelectedPhotosStillNeedReview() {
        val status = PhotoPostDeleteStatus.from(
            reconciliation = PhotoDeleteReconciliation(
                requestedItemCount = 2,
                requestedBytes = 2_000L,
                resolvedItemCount = 1,
                stillNeedsReviewCount = 1,
                stillNeedsReviewUris = listOf("content://images/b"),
                remainingGroupCount = 1,
                remainingRecoverableBytes = 1_000L,
            ),
        )

        assertEquals("1 photo resolved", status?.title)
        assertEquals("1 selected photo still appears in duplicate review", status?.message)
        assertEquals(1, status?.remainingGroupCount)
        assertEquals(1_000L, status?.remainingRecoverableBytes)
    }

    @Test
    fun summarizesReconciliationWhenDuplicateReviewIsClear() {
        val status = PhotoPostDeleteStatus.from(
            reconciliation = PhotoDeleteReconciliation(
                requestedItemCount = 2,
                requestedBytes = 2_000L,
                resolvedItemCount = 2,
                stillNeedsReviewCount = 0,
                stillNeedsReviewUris = emptyList(),
                remainingGroupCount = 0,
                remainingRecoverableBytes = 0L,
            ),
        )

        assertEquals("All duplicate photos cleared", status?.title)
        assertEquals("2 photos no longer appear in duplicate review", status?.message)
        assertEquals(0, status?.remainingGroupCount)
        assertEquals(0L, status?.remainingRecoverableBytes)
    }

    private fun duplicateGroup(vararg ids: String): DuplicateGroup {
        return DuplicateGroup(
            key = ids.joinToString(),
            items = ids.mapIndexed { index, id ->
                MediaItem(
                    id = id,
                    displayName = "$id.jpg",
                    sizeBytes = 1_000L,
                    dateTakenMillis = index.toLong(),
                    contentHash = "same",
                    mediaType = MediaType.Image,
                )
            },
        )
    }
}
