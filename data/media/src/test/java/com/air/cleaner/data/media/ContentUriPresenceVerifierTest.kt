package com.air.cleaner.data.media

import com.air.cleaner.domain.cleaning.DuplicateGroup
import com.air.cleaner.domain.cleaning.MediaItem
import com.air.cleaner.domain.cleaning.MediaType
import org.junit.Assert.assertEquals
import org.junit.Test

class ContentUriPresenceVerifierTest {
    @Test
    fun returnsOnlyUrisThatStillExist() {
        val verifier = ContentUriPresenceVerifier(
            exists = { uri -> uri == "content://images/kept" },
        )

        val stillPresent = verifier.stillPresent(
            listOf(
                "content://images/deleted",
                "content://images/kept",
            ),
        )

        assertEquals(listOf("content://images/kept"), stillPresent)
    }

    @Test
    fun preservesRequestedOrderAndRemovesDuplicates() {
        val verifier = ContentUriPresenceVerifier(
            exists = { uri -> uri != "content://images/deleted" },
        )

        val stillPresent = verifier.stillPresent(
            listOf(
                "content://images/b",
                "content://images/a",
                "content://images/b",
                "content://images/deleted",
            ),
        )

        assertEquals(
            listOf("content://images/b", "content://images/a"),
            stillPresent,
        )
    }

    @Test
    fun filtersCachedGroupsToExistingItemsAndDropsSingleItemGroups() {
        val verifier = ContentUriPresenceVerifier(
            exists = { uri -> uri != "content://images/deleted" },
        )

        val groups = verifier.stillPresentGroups(
            listOf(
                DuplicateGroup(
                    key = "kept-group",
                    items = listOf(
                        item("1", "content://images/a"),
                        item("2", "content://images/deleted"),
                        item("3", "content://images/c"),
                    ),
                ),
                DuplicateGroup(
                    key = "single-left-group",
                    items = listOf(
                        item("4", "content://images/deleted"),
                        item("5", "content://images/e"),
                    ),
                ),
            ),
        )

        assertEquals(1, groups.size)
        assertEquals("kept-group", groups.single().key)
        assertEquals(
            listOf("content://images/a", "content://images/c"),
            groups.single().items.map { it.contentUri },
        )
    }

    private fun item(id: String, contentUri: String): MediaItem {
        return MediaItem(
            id = id,
            displayName = "Screenshot_$id.png",
            sizeBytes = 1_000_000L,
            dateTakenMillis = id.toLong(),
            contentHash = null,
            mediaType = MediaType.Image,
            contentUri = contentUri,
        )
    }
}
