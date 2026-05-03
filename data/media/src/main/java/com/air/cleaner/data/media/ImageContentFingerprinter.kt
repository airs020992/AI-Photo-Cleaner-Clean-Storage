package com.air.cleaner.data.media

import android.graphics.BitmapFactory
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

object ImageContentFingerprinter {
    fun sha256(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = inputStream.read(buffer)
            if (read == -1) break
            digest.update(buffer, 0, read)
        }
        return digest.digest().joinToString(separator = "") { byte ->
            "%02x".format(byte)
        }
    }

    fun sha256DecodedPixels(inputStream: InputStream): String? {
        val bitmap = BitmapFactory.decodeStream(inputStream) ?: return null
        return try {
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            sha256Pixels(bitmap.width, bitmap.height, pixels)
        } finally {
            bitmap.recycle()
        }
    }

    fun sha256Pixels(
        width: Int,
        height: Int,
        argbPixels: IntArray,
    ): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val header = ByteBuffer.allocate(Int.SIZE_BYTES * 2)
            .order(ByteOrder.BIG_ENDIAN)
            .putInt(width)
            .putInt(height)
            .array()
        digest.update(header)

        val buffer = ByteBuffer.allocate(Int.SIZE_BYTES)
            .order(ByteOrder.BIG_ENDIAN)
        argbPixels.forEach { pixel ->
            buffer.clear()
            buffer.putInt(pixel)
            digest.update(buffer.array())
        }

        return digest.digest().joinToString(separator = "") { byte ->
            "%02x".format(byte)
        }
    }
}
