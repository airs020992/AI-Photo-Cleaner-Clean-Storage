package com.air.cleaner.feature.photos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoDeletionResultTest {
    @Test
    fun marksSuccessfulSystemConfirmationAsDeleted() {
        val summary = PhotoDeletionSummary(
            itemCount = 2,
            bytesToDelete = 3_000L,
            contentUris = listOf("content://images/a", "content://images/b"),
        )

        val result = PhotoDeletionResult.fromSystemResult(summary, systemConfirmed = true)

        assertEquals(PhotoDeletionStatus.Deleted, result.status)
        assertEquals(2, result.itemCount)
        assertEquals(3_000L, result.bytes)
        assertTrue(result.shouldRefreshScan)
    }

    @Test
    fun marksCanceledSystemConfirmationWithoutRefreshingScan() {
        val summary = PhotoDeletionSummary(
            itemCount = 2,
            bytesToDelete = 3_000L,
            contentUris = listOf("content://images/a", "content://images/b"),
        )

        val result = PhotoDeletionResult.fromSystemResult(summary, systemConfirmed = false)

        assertEquals(PhotoDeletionStatus.Canceled, result.status)
        assertEquals(2, result.itemCount)
        assertEquals(3_000L, result.bytes)
        assertTrue(!result.shouldRefreshScan)
    }

    @Test
    fun createsBlockedResultWhenSystemDeleteCannotBeRequested() {
        val result = PhotoDeletionResult.blocked(
            summary = PhotoDeletionSummary(
                itemCount = 1,
                bytesToDelete = 1_000L,
                contentUris = emptyList(),
            ),
        )

        assertEquals(PhotoDeletionStatus.Blocked, result.status)
        assertEquals(1, result.itemCount)
        assertEquals(1_000L, result.bytes)
        assertTrue(!result.shouldRefreshScan)
    }
}
