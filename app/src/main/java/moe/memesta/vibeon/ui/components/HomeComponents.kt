package moe.memesta.vibeon.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import coil.compose.AsyncImage
import coil.request.ImageRequest
import moe.memesta.vibeon.data.TrackInfo
import moe.memesta.vibeon.ui.theme.Dimens
import moe.memesta.vibeon.ui.theme.SongCoverShape
import moe.memesta.vibeon.ui.theme.ArtistCoverShape
import moe.memesta.vibeon.ui.utils.LocalDisplayLanguage
import moe.memesta.vibeon.ui.utils.getDisplayArtist
import moe.memesta.vibeon.ui.utils.getDisplayName


// Unified accent
private val CardBorderColor = Color.White.copy(alpha = 0.09f)
// Accent color — now uses MaterialTheme.colorScheme.primary for dynamic theming

/**
 * Scale + border on press — replaces bouncyClickable for card scale up UX.
 */
@Composable
private fun scaleModifier(pressed: Boolean): Float {
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1.0f,
        animationSpec = tween(200),
        label = "cardScale"
    )
    return scale
}

@Composable
fun GridTrackCard(
    track: TrackInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    allowImageLoad: Boolean = true
) {
    val displayLanguage = LocalDisplayLanguage.current
    val title = track.getDisplayName(displayLanguage)
    val artist = track.getDisplayArtist(displayLanguage)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = scaleModifier(isPressed)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.ListItemHeight)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(Dimens.CornerRadiusMedium))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Art with inner shadow-like border
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(Dimens.CornerRadiusSmall))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        ) {
            if (track.coverUrl != null && allowImageLoad) {
                val context = LocalContext.current
                val request = remember(track.coverUrl) {
                    ImageRequest.Builder(context)
                        .data(track.coverUrl)
                        .crossfade(true)
                        .build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.width(Dimens.ItemSpacing))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SquareTrackCard(
    track: TrackInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    allowImageLoad: Boolean = true
) {
    val displayLanguage = LocalDisplayLanguage.current
    val title = track.getDisplayName(displayLanguage)
    val artist = track.getDisplayArtist(displayLanguage)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = scaleModifier(isPressed)

    Column(
        modifier = modifier
            .width(Dimens.StandardCardWidth)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.StandardCardWidth)
                .clip(SongCoverShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .border(1.dp, CardBorderColor, SongCoverShape)
        ) {
            if (track.coverUrl != null && allowImageLoad) {
                val context = LocalContext.current
                val request = remember(track.coverUrl) {
                    ImageRequest.Builder(context)
                        .data(track.coverUrl)
                        .crossfade(true)
                        .build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AlbumCard(
    albumName: String,
    coverUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    allowImageLoad: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = scaleModifier(isPressed)

    Column(
        modifier = modifier
            .width(Dimens.StandardCardWidth)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.StandardCardWidth)
                .clip(AlbumSquircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .border(1.dp, CardBorderColor, AlbumSquircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (coverUrl != null && allowImageLoad) {
                val context = LocalContext.current
                val request = remember(coverUrl) {
                    ImageRequest.Builder(context)
                        .data(coverUrl)
                        .crossfade(true)
                        .build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = albumName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                /* Subtle inner top glow removed for matte look */
            } else if (coverUrl != null && !allowImageLoad) {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
            } else {
                Text(
                    text = albumName.take(1).uppercase(),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = albumName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ArtistPill(
    artistName: String,
    photoUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    allowImageLoad: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = scaleModifier(isPressed)

    Surface(
        color = Color.Transparent,
        shape = CircleShape,
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
    ) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .border(1.dp, CardBorderColor, CircleShape)
                .clickable(interactionSource = interactionSource, indication = null) { onClick() }
                .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), ArtistCoverShape)
                    .clip(ArtistCoverShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (photoUrl != null && allowImageLoad) {
                    val context = LocalContext.current
                    val request = remember(photoUrl) {
                        ImageRequest.Builder(context)
                            .data(photoUrl)
                            .crossfade(true)
                            .build()
                    }
                    AsyncImage(
                        model = request,
                        contentDescription = artistName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (photoUrl != null && !allowImageLoad) {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
                } else {
                    Text(
                        text = artistName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(Dimens.ItemSpacing))
            Text(
                text = artistName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

