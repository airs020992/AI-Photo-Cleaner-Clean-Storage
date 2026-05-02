package com.air.cleaner.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.air.cleaner.core.permissions.MediaPermissionAccess
import com.air.cleaner.core.ui.theme.CleanerTheme

@Composable
fun OnboardingScreen(
    mediaAccess: MediaPermissionAccess,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text(
                text = "Free up space safely",
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = "Find duplicate photos, blurry shots, screenshots, and large videos. Nothing is deleted without your confirmation.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            TrustPoint(
                title = "Scan first, pay later",
                body = "See what is wasting space before any upgrade prompt.",
            )
            TrustPoint(
                title = "Review before deleting",
                body = "You choose what stays and what gets cleaned.",
            )
            TrustPoint(
                title = "Built for your photos",
                body = "The app asks only for media access needed to find storage waste.",
            )
            PermissionStatus(mediaAccess = mediaAccess)
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (mediaAccess == MediaPermissionAccess.None) "Scan my photos" else "Continue to scan")
            }
            Text(
                text = permissionHelperText(mediaAccess),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PermissionStatus(
    mediaAccess: MediaPermissionAccess,
    modifier: Modifier = Modifier,
) {
    val (title, body) = when (mediaAccess) {
        MediaPermissionAccess.Full -> "Media access ready" to
            "The app can scan your full photo and video library for storage waste."
        MediaPermissionAccess.SelectedOnly -> "Selected photos access" to
            "The app can scan only the photos and videos you selected. Full access finds more cleanup opportunities."
        MediaPermissionAccess.None -> "Permission requested next" to
            "Android may ask you to allow photos and videos after you tap the button."
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

private fun permissionHelperText(mediaAccess: MediaPermissionAccess): String {
    return when (mediaAccess) {
        MediaPermissionAccess.Full -> "Ready to scan. You still review everything before deleting."
        MediaPermissionAccess.SelectedOnly -> "Limited access is safe, but full access gives a better cleanup scan."
        MediaPermissionAccess.None -> "Android may ask you to allow photos and videos next."
    }
}

@Composable
private fun TrustPoint(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "✓",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    CleanerTheme {
        OnboardingScreen(
            mediaAccess = MediaPermissionAccess.None,
            onRequestPermission = {},
        )
    }
}
