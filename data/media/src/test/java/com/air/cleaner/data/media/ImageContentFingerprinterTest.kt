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

    @Test
    fun createsStableSha256FingerprintFromDecodedPixels() {
        val fingerprint = ImageContentFingerprinter.sha256Pixels(
            width = 2,
            height = 1,
            argbPixels = intArrayOf(0xFFFF0000.toInt(), 0xFF00FF00.toInt()),
        )

        assertEquals(
            "827e1b4c22cf15eb7d6c1f141695555c8e34bf05715ab73f9b3206b24eb800dd",
            fingerprint,
        )
    }
}
