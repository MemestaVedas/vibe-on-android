package moe.memesta.vibeon.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import moe.memesta.vibeon.R
import moe.memesta.vibeon.core.presentation.ObserveAsEvents
import moe.memesta.vibeon.core.presentation.UiText
import org.koin.androidx.compose.koinViewModel
import moe.memesta.vibeon.ui.theme.Dimens

@Composable
fun ServerDetailsRoot(
    connectionViewModel: ConnectionViewModel,
    onBackPressed: () -> Unit,
    onDisconnect: () -> Unit,
    viewModel: ServerDetailsViewModel = koinViewModel()
) {
    val connectedDevice by connectionViewModel.connectedDevice.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(connectedDevice?.host, connectedDevice?.port) {
        viewModel.onAction(ServerDetailsAction.OnDeviceChanged(connectedDevice))
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            ServerDetailsEvent.DisconnectAndClose -> {
                onDisconnect()
                onBackPressed()
            }
        }
    }

    ServerDetailsScreen(
        state = state,
        onBackPressed = onBackPressed,
        onAction = viewModel::onAction
    )
}

@Composable
fun ServerDetailsScreen(
    state: ServerDetailsState,
    onBackPressed: () -> Unit,
    onAction: (ServerDetailsAction) -> Unit
) {
    val backDescription = stringResource(R.string.cd_back)
    val screenTitle = stringResource(R.string.server_details_title)
    val connectedTo = stringResource(R.string.server_details_connected_to)
    val unknownServer = stringResource(R.string.server_details_unknown_server)
    val favoriteLabel = stringResource(R.string.server_details_favorited)
    val addFavoriteLabel = stringResource(R.string.server_details_add_to_favorites)
    val disconnectLabel = stringResource(R.string.server_details_disconnect)
    val footerText = stringResource(R.string.server_details_footer)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Top Bar with Back Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.ScreenPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onBackPressed() },
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = backDescription
                        )
                    }
                }
                Text(
                    text = screenTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Server Connection Info
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.ScreenPadding),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(Dimens.CornerRadiusLarge)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = connectedTo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = state.connectedDevice?.name ?: unknownServer,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    if (state.connectedDevice != null) {
                        Text(
                            text = "${state.connectedDevice.host}:${state.connectedDevice.port}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Favorite Button
                OutlinedButton(
                    onClick = {
                        onAction(ServerDetailsAction.OnToggleFavorite)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isBusy,
                    shape = RoundedCornerShape(Dimens.CornerRadiusMedium)
                ) {
                    Icon(
                        imageVector = if (state.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (state.isFavorite) favoriteLabel else addFavoriteLabel)
                }

                // Disconnect Button
                Button(
                    onClick = {
                        onAction(ServerDetailsAction.OnDisconnect)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    enabled = !state.isBusy,
                    shape = RoundedCornerShape(Dimens.CornerRadiusMedium)
                ) {
                    Text(disconnectLabel)
                }
            }

            if (state.error != null) {
                val errorText = when (val uiText = state.error) {
                    is UiText.DynamicString -> uiText.value
                    is UiText.StringResource -> stringResource(uiText.id, *uiText.args.toTypedArray())
                    null -> ""
                }

                Text(
                    text = errorText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Placeholder text at bottom
            Text(
                text = footerText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ServerDetailsScreenPreview() {
    MaterialTheme {
        ServerDetailsScreen(
            state = ServerDetailsState(
                connectedDevice = null,
                isFavorite = false,
                isBusy = false,
                error = null
            ),
            onBackPressed = {},
            onAction = {}
        )
    }
}
