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
import androidx.compose.ui.res.stringResource
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
                text = stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = stringResource(R.string.onboarding_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            TrustPoint(
                title = stringResource(R.string.onboarding_trust_scan_first_title),
                body = stringResource(R.string.onboarding_trust_scan_first_body),
            )
            TrustPoint(
                title = stringResource(R.string.onboarding_trust_review_title),
                body = stringResource(R.string.onboarding_trust_review_body),
            )
            TrustPoint(
                title = stringResource(R.string.onboarding_trust_media_title),
                body = stringResource(R.string.onboarding_trust_media_body),
            )
            PermissionStatus(mediaAccess = mediaAccess)
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (mediaAccess == MediaPermissionAccess.None) {
                        stringResource(R.string.onboarding_cta_scan)
                    } else {
                        stringResource(R.string.onboarding_cta_continue)
                    }
                )
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
        MediaPermissionAccess.Full -> stringResource(R.string.permission_status_full_title) to
            stringResource(R.string.permission_status_full_body)
        MediaPermissionAccess.SelectedOnly -> stringResource(R.string.permission_status_selected_title) to
            stringResource(R.string.permission_status_selected_body)
        MediaPermissionAccess.None -> stringResource(R.string.permission_status_none_title) to
            stringResource(R.string.permission_status_none_body)
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

@Composable
private fun permissionHelperText(mediaAccess: MediaPermissionAccess): String {
    return when (mediaAccess) {
        MediaPermissionAccess.Full -> stringResource(R.string.permission_helper_full)
        MediaPermissionAccess.SelectedOnly -> stringResource(R.string.permission_helper_selected)
        MediaPermissionAccess.None -> stringResource(R.string.permission_helper_none)
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
                text = "OK",
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
