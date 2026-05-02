package com.air.cleaner.data.media

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream

class ImageContentFingerprinterTest {
    @Test
    fun createsStableSha256FingerprintFromStreamContent() {
        val fingerprint = ImageContentFingerprinter.sha256(
            ByteArrayInputStream("abc".toByteArray()),
        )

        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            fingerprint,
        )
    }
}
