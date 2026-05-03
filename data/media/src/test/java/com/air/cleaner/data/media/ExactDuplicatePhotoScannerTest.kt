package com.air.cleaner.data.media

import com.air.cleaner.domain.cleaning.MediaItem
import com.air.cleaner.domain.cleaning.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExactDuplicatePhotoScannerTest {
    @Test
    fun groupsOnlyItemsWithMatchingContentFingerprints() {
        val scanner = ExactDuplicatePhotoScanner(
            contentFingerprint = { key ->
                when (key) {
                    "uri-a" -> "same-hash"
                    "uri-b" -> "same-hash"
                    "uri-c" -> "different-hash"
                    else -> null
                }
            },
        )
        val candidates = listOf(
            candidate("1", "uri-a", sizeBytes = 100),
            candidate("2", "uri-b", sizeBytes = 100),
            candidate("3", "uri-c", sizeBytes = 100),
        )

        val groups = scanner.findDuplicateGroups(candidates)

        assertEquals(1, groups.size)
        assertEquals(listOf("1", "2"), groups.single().items.map { it.id }.sorted())
    }

    @Test
    fun skipsContentReadsForImagesWithUniqueFileSizes() {
        val requestedKeys = mutableListOf<String>()
        val scanner = ExactDuplicatePhotoScanner(
            contentFingerprint = { key ->
                requestedKeys += key
                "hash-$key"
            },
        )
        val candidates = listOf(
            candidate("1", "uri-a", sizeBytes = 100),
            candidate("2", "uri-b", sizeBytes = 200),
        )

        val groups = scanner.findDuplicateGroups(candidates)

        assertTrue(groups.isEmpty())
        assertTrue(requestedKeys.isEmpty())
    }

    @Test
    fun comparesImagesWithSameDimensionsEvenWhenFileSizesDiffer() {
        val requestedKeys = mutableListOf<String>()
        val scanner = ExactDuplicatePhotoScanner(
            contentFingerprint = { key ->
                requestedKeys += key
                "same-pixels"
            },
        )
        val candidates = listOf(
            candidate("1", "uri-a", sizeBytes = 1_912_800L, width = 1440, height = 3120),
            candidate("2", "uri-b", sizeBytes = 1_912_759L, width = 1440, height = 3120),
        )

        val groups = scanner.findDuplicateGroups(candidates)

        assertEquals(listOf("uri-a", "uri-b"), requestedKeys.sorted())
        assertEquals(listOf("1", "2"), groups.single().items.map { it.id }.sorted())
    }

    @Test
    fun keepsOriginalContentUriForThumbnailRendering() {
        val scanner = ExactDuplicatePhotoScanner(
            contentFingerprint = { "same-hash" },
        )
        val candidates = listOf(
            candidate("1", "content://media/images/1", sizeBytes = 100),
            candidate("2", "content://media/images/2", sizeBytes = 100),
        )

        val groupedItems = scanner.findDuplicateGroups(candidates).single().items

        assertEquals(
            listOf("content://media/images/1", "content://media/images/2"),
            groupedItems.mapNotNull { it.contentUri }.sorted(),
        )
    }

    private fun candidate(
        id: String,
        contentKey: String,
        sizeBytes: Long,
        width: Int? = null,
        height: Int? = null,
    ): DuplicatePhotoCandidate {
        return DuplicatePhotoCandidate(
            item = MediaItem(
                id = id,
                displayName = "$id.jpg",
                sizeBytes = sizeBytes,
                dateTakenMillis = 1_000L,
                contentHash = null,
                mediaType = MediaType.Image,
                contentUri = contentKey,
                width = width,
                height = height,
            ),
            contentKey = contentKey,
        )
    }
}
