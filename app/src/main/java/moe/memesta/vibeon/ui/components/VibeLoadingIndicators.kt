package moe.memesta.vibeon.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.ui.unit.dp

/**
 * Thin wrappers around Material3 loading indicators to keep call-sites stable.
 */

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VibeLoadingIndicator(
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    label: String = "Loading..."
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LoadingIndicator()
        if (showLabel) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VibeContainedLoadingIndicator(
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    label: String = "Loading..."
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ContainedLoadingIndicator(containerShape = MaterialShapes.Pill.toShape())
        if (showLabel) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}