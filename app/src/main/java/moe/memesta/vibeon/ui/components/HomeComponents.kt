package moe.memesta.vibeon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import moe.memesta.vibeon.data.TrackInfo
import moe.memesta.vibeon.ui.theme.Dimens
import moe.memesta.vibeon.ui.theme.bouncyClickable
import moe.memesta.vibeon.ui.theme.shimmerEffect

@Composable
fun SquareTrackCard(
    track: TrackInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    allowImageLoad: Boolean = true
) {
    Column(
        modifier = modifier
            .width(Dimens.StandardCardWidth) // 160.dp
            .bouncyClickable(onClick = onClick)
    ) {
        // Art
        Box(
            modifier = Modifier
                .size(Dimens.StandardCardWidth) // 160.dp
                .clip(RoundedCornerShape(Dimens.CornerRadiusLarge))
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
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = track.title,
            style = MaterialTheme.typography.bodyMedium, // 15sp Medium
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artist,
            style = MaterialTheme.typography.bodySmall, // 13sp Normal
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun GridTrackCard(
    track: TrackInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    allowImageLoad: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.ListItemHeight) // 72.dp
            .bouncyClickable(onClick = onClick, scaleDown = 0.98f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Art
        Box(
            modifier = Modifier
                .size(56.dp)
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
        
        Spacer(modifier = Modifier.width(Dimens.ItemSpacing)) // 12.dp
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyMedium, // 15sp Medium
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodySmall, // 13sp
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
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
    Column(
        modifier = modifier
            .width(Dimens.StandardCardWidth) // 160.dp
            .bouncyClickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.StandardCardWidth)
                .clip(RoundedCornerShape(Dimens.CornerRadiusLarge))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
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
            } else if (coverUrl != null && !allowImageLoad) {
                // Placeholder during fling
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
            } else {
                Text(
                    text = albumName.take(1).uppercase(),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = albumName,
            style = MaterialTheme.typography.bodyMedium, // 15sp Medium
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
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = CircleShape,
        modifier = modifier.bouncyClickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
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
                    // Placeholder during fling
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
                } else {
                    Text(
                        text = artistName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(Dimens.ItemSpacing))
            Text(
                text = artistName,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

// SKELETONS

@Composable
fun SkeletonSquareCard(
    modifier: Modifier = Modifier,
    tintColor: Color? = null
) {
    Column(
        modifier = modifier.width(Dimens.StandardCardWidth)
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.StandardCardWidth)
                .clip(RoundedCornerShape(Dimens.CornerRadiusLarge))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .shimmerEffect(tintColor)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .height(14.dp)
                .fillMaxWidth(0.8f)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .shimmerEffect(tintColor)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .height(12.dp)
                .fillMaxWidth(0.55f)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .shimmerEffect(tintColor)
        )
    }
}

@Composable
fun SkeletonAlbumCard(
    modifier: Modifier = Modifier,
    tintColor: Color? = null
) {
    Column(
        modifier = modifier.width(Dimens.StandardCardWidth)
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.StandardCardWidth)
                .clip(RoundedCornerShape(Dimens.CornerRadiusLarge))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .shimmerEffect(tintColor)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .height(14.dp)
                .fillMaxWidth(0.7f)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .shimmerEffect(tintColor)
        )
    }
}

@Composable
fun SkeletonArtistPill(
    modifier: Modifier = Modifier,
    tintColor: Color? = null
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .width(160.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .shimmerEffect(tintColor)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .height(12.dp)
                .fillMaxWidth(0.6f)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
        )
    }
}
