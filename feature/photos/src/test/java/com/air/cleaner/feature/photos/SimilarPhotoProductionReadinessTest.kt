package com.air.cleaner.feature.photos

import org.junit.Assert.assertEquals
import org.junit.Test

class SimilarPhotoProductionReadinessTest {
    @Test
    fun marksRealSampleRunReadyWhenEveryCleanableGroupDeletesSuccessfully() {
        val readiness = SimilarPhotoProductionReadiness.from(
            SimilarPhotoValidationRun(
                scannedItemCount = 1_457,
                fingerprintCandidateCount = 213,
                detectedGroupCount = 19,
                cleanableGroupCount = 19,
                selectedItemCount = 22,
                requestedDeleteCount = 22,
                deletedItemCount = 22,
                stillExistsCount = 0,
                remainingGroupCount = 0,
                recoveredBytes = 125_376_075L,
                elapsedMillis = 45_540L,
            ),
        )

        assertEquals(SimilarPhotoReadinessStatus.Ready, readiness.status)
        assertEquals(100, readiness.cleanableGroupPrecisionPercent)
        assertEquals(100, readiness.deleteCompletionPercent)
        assertEquals(100, readiness.moduleCompletionPercent)
        assertEquals("Ready for production candidate", readiness.summary)
        assertEquals(emptyList<String>(), readiness.blockers)
    }

    @Test
    fun blocksReleaseWhenPrecisionDropsBelowProductionThreshold() {
        val readiness = SimilarPhotoProductionReadiness.from(
            SimilarPhotoValidationRun(
                scannedItemCount = 100,
                fingerprintCandidateCount = 40,
                detectedGroupCount = 10,
                cleanableGroupCount = 8,
                selectedItemCount = 16,
                requestedDeleteCount = 16,
                deletedItemCount = 16,
                stillExistsCount = 0,
                remainingGroupCount = 0,
                recoveredBytes = 50_000_000L,
                elapsedMillis = 20_000L,
            ),
        )

        assertEquals(SimilarPhotoReadinessStatus.Blocked, readiness.status)
        assertEquals(80, readiness.cleanableGroupPrecisionPercent)
        assertEquals(95, readiness.moduleCompletionPercent)
        assertEquals(
            listOf("Cleanable group precision 80% is below 95% target"),
            readiness.blockers,
        )
    }

    @Test
    fun blocksReleaseWhenDeletionDoesNotFullyReconcile() {
        val readiness = SimilarPhotoProductionReadiness.from(
            SimilarPhotoValidationRun(
                scannedItemCount = 100,
                fingerprintCandidateCount = 40,
                detectedGroupCount = 10,
                cleanableGroupCount = 10,
                selectedItemCount = 16,
                requestedDeleteCount = 16,
                deletedItemCount = 14,
                stillExistsCount = 2,
                remainingGroupCount = 1,
                recoveredBytes = 40_000_000L,
                elapsedMillis = 20_000L,
            ),
        )

        assertEquals(SimilarPhotoReadinessStatus.Blocked, readiness.status)
        assertEquals(88, readiness.deleteCompletionPercent)
        assertEquals(95, readiness.moduleCompletionPercent)
        assertEquals(
            listOf(
                "Delete completion 88% is below 100% target",
                "2 requested photos still exist after deletion",
                "1 duplicate group remains after cleanup",
            ),
            readiness.blockers,
        )
    }
}
