package com.air.cleaner.feature.photos

import kotlin.math.roundToInt

data class SimilarPhotoValidationRun(
    val scannedItemCount: Int,
    val fingerprintCandidateCount: Int,
    val detectedGroupCount: Int,
    val cleanableGroupCount: Int,
    val selectedItemCount: Int,
    val requestedDeleteCount: Int,
    val deletedItemCount: Int,
    val stillExistsCount: Int,
    val remainingGroupCount: Int,
    val recoveredBytes: Long,
    val elapsedMillis: Long,
)

data class SimilarPhotoProductionReadiness(
    val status: SimilarPhotoReadinessStatus,
    val cleanableGroupPrecisionPercent: Int,
    val deleteCompletionPercent: Int,
    val moduleCompletionPercent: Int,
    val summary: String,
    val blockers: List<String>,
) {
    companion object {
        private const val TARGET_PRECISION_PERCENT = 95
        private const val TARGET_DELETE_COMPLETION_PERCENT = 100
        private const val TARGET_ELAPSED_MILLIS = 60_000L

        fun from(run: SimilarPhotoValidationRun): SimilarPhotoProductionReadiness {
            val precisionPercent = percent(
                numerator = run.cleanableGroupCount,
                denominator = run.detectedGroupCount,
                emptyValue = 100,
            )
            val deleteCompletionPercent = percent(
                numerator = run.deletedItemCount,
                denominator = run.requestedDeleteCount,
                emptyValue = 100,
            )
            val blockers = buildList {
                if (precisionPercent < TARGET_PRECISION_PERCENT) {
                    add("Cleanable group precision $precisionPercent% is below $TARGET_PRECISION_PERCENT% target")
                }
                if (deleteCompletionPercent < TARGET_DELETE_COMPLETION_PERCENT) {
                    add(
                        "Delete completion $deleteCompletionPercent% is below " +
                            "$TARGET_DELETE_COMPLETION_PERCENT% target",
                    )
                }
                if (run.stillExistsCount > 0) {
                    add(
                        "${run.stillExistsCount} requested ${photoLabel(run.stillExistsCount)} " +
                            "${if (run.stillExistsCount == 1) "still exists" else "still exist"} after deletion",
                    )
                }
                if (run.remainingGroupCount > 0) {
                    add(
                        "${run.remainingGroupCount} duplicate ${groupLabel(run.remainingGroupCount)} " +
                            "${if (run.remainingGroupCount == 1) "remains" else "remain"} after cleanup",
                    )
                }
                if (run.elapsedMillis > TARGET_ELAPSED_MILLIS) {
                    add("Scan time ${run.elapsedMillis}ms is above ${TARGET_ELAPSED_MILLIS}ms target")
                }
            }
            val status = if (blockers.isEmpty()) {
                SimilarPhotoReadinessStatus.Ready
            } else {
                SimilarPhotoReadinessStatus.Blocked
            }
            return SimilarPhotoProductionReadiness(
                status = status,
                cleanableGroupPrecisionPercent = precisionPercent,
                deleteCompletionPercent = deleteCompletionPercent,
                moduleCompletionPercent = if (status == SimilarPhotoReadinessStatus.Ready) 100 else 95,
                summary = if (status == SimilarPhotoReadinessStatus.Ready) {
                    "Ready for production candidate"
                } else {
                    "Blocked by ${blockers.size} production gate${if (blockers.size == 1) "" else "s"}"
                },
                blockers = blockers,
            )
        }

        private fun percent(
            numerator: Int,
            denominator: Int,
            emptyValue: Int,
        ): Int {
            if (denominator <= 0) return emptyValue
            return ((numerator.coerceAtLeast(0).toDouble() / denominator) * 100).roundToInt()
        }

        private fun photoLabel(count: Int): String = if (count == 1) "photo" else "photos"

        private fun groupLabel(count: Int): String = if (count == 1) "group" else "groups"
    }
}

enum class SimilarPhotoReadinessStatus {
    Ready,
    Blocked,
}
