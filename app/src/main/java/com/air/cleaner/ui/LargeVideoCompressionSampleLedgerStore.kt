package com.air.cleaner.ui

import android.content.Context
import android.content.SharedPreferences

internal interface LargeVideoCompressionSampleLedgerStore {
    fun load(): LargeVideoCompressionSampleLedger
    fun save(ledger: LargeVideoCompressionSampleLedger)
}

internal class SharedPreferencesLargeVideoCompressionSampleLedgerStore(
    context: Context,
    private val preferences: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    ),
) : LargeVideoCompressionSampleLedgerStore {
    override fun load(): LargeVideoCompressionSampleLedger {
        val rows = preferences.getStringSet(KEY_SAMPLES, emptySet()).orEmpty()
        return LargeVideoCompressionSampleLedger(
            samples = rows
                .mapNotNull { row -> row.toLargeVideoSampleEvidenceOrNull() }
                .sortedBy { sample -> sample.capturedAtMillis },
        )
    }

    override fun save(ledger: LargeVideoCompressionSampleLedger) {
        preferences.edit()
            .putStringSet(
                KEY_SAMPLES,
                ledger.samples.map { sample -> sample.toPreferenceRow() }.toSet(),
            )
            .apply()
    }

    private fun LargeVideoCompressionSampleEvidence.toPreferenceRow(): String {
        return listOf(
            sourceId,
            sourceOrigin,
            profile.name,
            result.outputPath,
            result.originalBytes.toString(),
            result.outputBytes.toString(),
            result.audioRemoved.toString(),
            capturedAtMillis.toString(),
        ).joinToString(DELIMITER) { value -> value.replace(DELIMITER, " ") }
    }

    private fun String.toLargeVideoSampleEvidenceOrNull(): LargeVideoCompressionSampleEvidence? {
        val parts = split(DELIMITER)
        if (parts.size !in 7..8) return null
        val profile = runCatching { LargeVideoCompressionProfile.valueOf(parts[2]) }.getOrNull() ?: return null
        val originalBytes = parts[4].toLongOrNull() ?: return null
        val outputBytes = parts[5].toLongOrNull() ?: return null
        return LargeVideoCompressionSampleEvidence(
            sourceId = parts[0],
            sourceOrigin = parts[1],
            profile = profile,
            capturedAtMillis = parts.getOrNull(7)?.toLongOrNull() ?: 0L,
            result = LargeVideoCompressionResult(
                sourceId = parts[0],
                outputPath = parts[3],
                originalBytes = originalBytes,
                outputBytes = outputBytes,
                audioRemoved = parts[6].toBooleanStrictOrNull() ?: false,
            ),
        )
    }

    private companion object {
        const val PREFS_NAME = "large_video_compression_sample_ledger"
        const val KEY_SAMPLES = "samples"
        const val DELIMITER = "\u001F"
    }
}
