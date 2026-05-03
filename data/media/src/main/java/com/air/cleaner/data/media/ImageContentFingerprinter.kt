package com.air.cleaner.data.media

import android.graphics.BitmapFactory
import android.graphics.Bitmap
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

    fun averageHash(inputStream: InputStream): String? {
        val bitmap = BitmapFactory.decodeStream(inputStream) ?: return null
        return try {
            val scaled = Bitmap.createScaledBitmap(bitmap, AVERAGE_HASH_SIZE, AVERAGE_HASH_SIZE, true)
            try {
                val luma = IntArray(AVERAGE_HASH_SIZE * AVERAGE_HASH_SIZE)
                val pixels = IntArray(AVERAGE_HASH_SIZE * AVERAGE_HASH_SIZE)
                scaled.getPixels(pixels, 0, AVERAGE_HASH_SIZE, 0, 0, AVERAGE_HASH_SIZE, AVERAGE_HASH_SIZE)
                pixels.forEachIndexed { index, pixel ->
                    val red = pixel shr 16 and 0xff
                    val green = pixel shr 8 and 0xff
                    val blue = pixel and 0xff
                    luma[index] = (red * 299 + green * 587 + blue * 114) / 1000
                }
                averageHashFromLuma(luma)
            } finally {
                scaled.recycle()
            }
        } finally {
            bitmap.recycle()
        }
    }

    fun averageHashFromLuma(luma: IntArray): String {
        if (luma.isEmpty()) return ""
        val average = luma.average()
        val bits = StringBuilder()
        var currentNibble = 0
        luma.forEachIndexed { index, value ->
            currentNibble = currentNibble shl 1
            if (value >= average) {
                currentNibble = currentNibble or 1
            }
            if ((index + 1) % 4 == 0) {
                bits.append(currentNibble.toString(16))
                currentNibble = 0
            }
        }
        val remainder = luma.size % 4
        if (remainder != 0) {
            bits.append((currentNibble shl (4 - remainder)).toString(16))
        }
        return bits.toString()
    }

    fun hammingDistance(left: String, right: String): Int {
        if (left.length != right.length) return Int.MAX_VALUE
        return left.zip(right).sumOf { (leftChar, rightChar) ->
            val leftValue = leftChar.digitToIntOrNull(16) ?: return Int.MAX_VALUE
            val rightValue = rightChar.digitToIntOrNull(16) ?: return Int.MAX_VALUE
            Integer.bitCount(leftValue xor rightValue)
        }
    }

    private const val AVERAGE_HASH_SIZE = 16
}
