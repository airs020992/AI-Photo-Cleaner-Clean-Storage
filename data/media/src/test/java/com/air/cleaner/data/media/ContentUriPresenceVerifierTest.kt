package com.air.cleaner.data.media

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
}
