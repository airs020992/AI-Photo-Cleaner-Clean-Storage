package com.air.cleaner.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsDiagnosticsTelemetryTest {
    @Test
    fun recorderKeepsMostRecentEventsFirstWithinLimit() {
        val snapshots = mutableListOf<List<CleanerTelemetryEvent>>()
        val telemetry = AnalyticsDiagnosticsTelemetry(
            maxEvents = 2,
            onEventsChanged = { snapshots += it },
        )

        telemetry.track(CleanerTelemetryEvent("first", mapOf("count" to 1L)))
        telemetry.track(CleanerTelemetryEvent("second", mapOf("count" to 2L)))
        telemetry.track(CleanerTelemetryEvent("third", mapOf("count" to 3L)))

        assertEquals(
            listOf("third", "second"),
            snapshots.last().map { it.name },
        )
    }
}
