package com.air.cleaner.ui

import com.air.cleaner.core.permissions.MediaPermissionAccess
import com.air.cleaner.core.permissions.MediaPermissionState

internal fun MediaPermissionState.toMediaAccessDiagnosticsLabels(): List<String> {
    return when (access) {
        MediaPermissionAccess.Full -> listOf("Media access: full library.")
        MediaPermissionAccess.SelectedOnly -> listOf(
            "Media access: limited selected photos.",
            "Permission gate: full photo access is missing. Next: allow full Photos access before trusting empty scan results.",
        )
        MediaPermissionAccess.None -> listOf(
            "Media access: none.",
            "Permission gate: Photos access is missing. Next: grant Photos permission before scanning.",
        )
    }
}
