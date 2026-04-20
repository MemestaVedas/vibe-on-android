package moe.memesta.vibeon.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import moe.memesta.vibeon.data.TrackInfo
import moe.memesta.vibeon.ui.AlbumTrackRow
import moe.memesta.vibeon.ui.image.AppImageLoader
import moe.memesta.vibeon.ui.theme.Dimens
import moe.memesta.vibeon.ui.theme.MotionTokens
import moe.memesta.vibeon.ui.theme.bouncyClickable
import moe.memesta.vibeon.ui.utils.PaletteUtils
import moe.memesta.vibeon.ui.utils.ThemeColors
import java.net.URLEncoder
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import moe.memesta.vibeon.ui.utils.LocalDisplayLanguage
import moe.memesta.vibeon.ui.utils.getDisplayArtist
import moe.memesta.vibeon.ui.utils.getDisplayAlbum
import androidx.compose.foundation.gestures.scrollBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun ArtistDetailScreen(
    artistName: String,
    viewModel: LibraryViewModel,
    navController: NavController,
    onBackClick: () -> Unit,
    onTrackSelected: (TrackInfo) -> Unit,
    contentPadding: PaddingValues,
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null
) {
    val displayLanguage = LocalDisplayLanguage.current
    val tracks by viewModel.tracks.collectAsState()
    
    // Decode URL-encoded artist name
    val decodedArtistName = remember(artistName) {
        try {
            URLDecoder.decode(artistName, StandardCharsets.UTF_8.toString())
        } catch (e: Exception) {
            artistName
        }
    }
    
    val artistTracks = remember(tracks, decodedArtistName) {
        tracks.filter { it.artist == decodedArtistName }
    }

    val displayArtistName = artistTracks.firstOrNull()?.getDisplayArtist(displayLanguage) ?: decodedArtistName
    
    // Derived Albums
    val artistAlbums = remember(artistTracks, displayLanguage) {
        artistTracks.groupBy { it.album }
            .map { (albumName, groupedTracks) ->
                val first = groupedTracks.firstOrNull()
                ArtistAlbumGroup(
                    albumId = albumName,
                    displayAlbum = first?.getDisplayAlbum(displayLanguage) ?: albumName,
                    coverUrl = first?.coverUrl,
                    albumMainColor = first?.albumMainColor,
                    year = groupedTracks.mapNotNull { it.year }.maxOrNull()
                        ?: inferYearFromAlbumOrPath(albumName, groupedTracks.firstOrNull()?.path),
                    tracks = groupedTracks.sortedWith(
                        compareBy<TrackInfo> { it.year ?: Int.MIN_VALUE }
                            .thenBy { it.discNumber ?: 1 }
                            .thenBy { it.trackNumber ?: Int.MAX_VALUE }
                            .thenBy { it.title.lowercase() }
                    )
                )
            }
            .sortedWith(
                compareBy<ArtistAlbumGroup> { it.year ?: Int.MIN_VALUE }
                    .thenBy { it.displayAlbum.lowercase() }
            )
    }

    val albumYearLanes = remember(artistAlbums) {
        artistAlbums
            .groupBy { it.year }
            .toList()
            .sortedBy { (year, _) -> year ?: Int.MIN_VALUE }
            .map { (year, albums) ->
                ArtistAlbumYearLane(
                    year = year,
                    albums = albums.sortedBy { it.displayAlbum.lowercase() }
                )
            }
    }

    val songsAlbumOrder = remember(albumYearLanes) {
        albumYearLanes.flatMap { it.albums }
    }

    val albumThemeCache = remember { mutableStateMapOf<String, ThemeColors>() }

    val scrollState = rememberLazyListState()
    val splitIndex = 1 + albumYearLanes.size
    var hasInitializedStartPosition by remember(decodedArtistName) { mutableStateOf(false) }

    val splitProgress by remember {
        derivedStateOf {
            when {
                splitIndex <= 0 -> 0f
                scrollState.firstVisibleItemIndex <= 0 -> 0f
                scrollState.firstVisibleItemIndex >= splitIndex -> 1f
                else -> {
                    val indexFraction = scrollState.firstVisibleItemIndex / splitIndex.toFloat()
                    val offsetFraction = (scrollState.firstVisibleItemScrollOffset / 1200f).coerceIn(0f, 1f)
                    (indexFraction + offsetFraction / splitIndex).coerceIn(0f, 1f)
                }
            }
        }
    }

    suspend fun alignSplitBoundaryToCenter() {
        repeat(6) {
            val layoutInfo = scrollState.layoutInfo
            val splitAnchor = layoutInfo.visibleItemsInfo.firstOrNull { it.key == "split-anchor" }
            if (splitAnchor != null) {
                val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
                val desiredCenterOffset = layoutInfo.viewportStartOffset + ((viewportHeight - splitAnchor.size) / 2)
                val delta = splitAnchor.offset - desiredCenterOffset
                if (abs(delta) > 1) {
                    scrollState.scrollBy(delta.toFloat())
                }
                return
            }
            yield()
        }
    }

    val albumsLabelAlpha by animateFloatAsState(
        targetValue = (1f - splitProgress * 0.55f).coerceIn(0.55f, 1f),
        animationSpec = MotionTokens.Effects.standard(),
        label = "albumsLabelAlpha"
    )

    val songsLabelAlpha by animateFloatAsState(
        targetValue = (0.65f + splitProgress * 0.35f).coerceIn(0.65f, 1f),
        animationSpec = MotionTokens.Effects.standard(),
        label = "songsLabelAlpha"
    )

    val albumsHeaderOffsetY by animateFloatAsState(
        targetValue = -splitProgress * 14f,
        animationSpec = MotionTokens.Spatial.standard(),
        label = "albumsHeaderOffset"
    )

    // Open directly at the Albums/Songs boundary for the split experience.
    LaunchedEffect(splitIndex, artistTracks.size, hasInitializedStartPosition) {
        if (!hasInitializedStartPosition && artistTracks.isNotEmpty()) {
            scrollState.scrollToItem(splitIndex)
            alignSplitBoundaryToCenter()
            hasInitializedStartPosition = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 72.dp,
                bottom = contentPadding.calculateBottomPadding() + Dimens.SectionSpacing
            )
        ) {
            item {
                Text(
                    text = "Albums",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = albumsLabelAlpha),
                    modifier = Modifier
                        .offset { IntOffset(0, albumsHeaderOffsetY.toInt()) }
                        .padding(horizontal = Dimens.ScreenPadding)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            itemsIndexed(albumYearLanes) { _, lane ->
                YearAlbumLane(
                    lane = lane,
                    albumThemeCache = albumThemeCache,
                    navController = navController,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            item(key = "split-anchor") {
                Spacer(modifier = Modifier.height(24.dp))
                SquigglyLineSeparator(
                    modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Songs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = songsLabelAlpha),
                    modifier = Modifier.padding(horizontal = Dimens.ScreenPadding)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }



            songsAlbumOrder.forEach { album ->
                item(key = "album-header-${album.albumId}") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimens.ScreenPadding, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = album.displayAlbum,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        SquigglyLineSeparator(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = album.tracks.size.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                itemsIndexed(
                    items = album.tracks,
                    key = { _, track -> track.path }
                ) { index, track ->
                    val alpha by animateFloatAsState(
                        targetValue = 1f,
                        animationSpec = MotionTokens.staggeredEffects(index),
                        label = "itemAlpha"
                    )
                    val slide by animateFloatAsState(
                        targetValue = 0f,
                        animationSpec = MotionTokens.staggeredEffects(index),
                        label = "itemSlide"
                    )

                    Box(
                        modifier = Modifier.graphicsLayer {
                            this.alpha = alpha
                            this.translationY = slide
                        }
                    ) {
                        AlbumTrackRow(
                            index = track.trackNumber ?: (index + 1),
                            track = track,
                            onClick = {
                                viewModel.playTrack(track, artistTracks)
                                onTrackSelected(track)
                            }
                        )
                    }
                }
            }
        }

        val showTitle by remember {
            derivedStateOf {
                scrollState.firstVisibleItemIndex > 0 || scrollState.firstVisibleItemScrollOffset > 220
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (showTitle) {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
            } else {
                MaterialTheme.colorScheme.background.copy(alpha = 0.92f)
            }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    AnimatedVisibility(
                        visible = showTitle,
                        enter = fadeIn() + slideInVertically { 20 },
                        exit = fadeOut()
                    ) {
                        Text(
                            text = displayArtistName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class)
private fun YearAlbumLane(
    lane: ArtistAlbumYearLane,
    albumThemeCache: SnapshotStateMap<String, ThemeColors>,
    navController: NavController,
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope?,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope?
) {
    Text(
        text = lane.year?.toString() ?: "Unknown",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
        modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = 4.dp)
    )

    val carouselState = rememberCarouselState { lane.albums.size }
    val context = LocalContext.current

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val preferredItemWidth: Dp = maxWidth * 0.75f

        HorizontalUncontainedCarousel(
            state = carouselState,
            itemWidth = preferredItemWidth,
            modifier = Modifier
                .fillMaxWidth()
                .height(preferredItemWidth),
            itemSpacing = Dimens.ItemSpacing,
            contentPadding = PaddingValues(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding)
        ) { index ->
            val album = lane.albums[index]
            val themeCacheKey = album.coverUrl ?: "album:${album.albumId}"
            var albumTheme by remember(themeCacheKey) { mutableStateOf(albumThemeCache[themeCacheKey] ?: ThemeColors()) }
            val albumOverlay = if (albumTheme.muted != Color.Transparent) {
                albumTheme.muted
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
            val albumTextColor = if (albumTheme.vibrant != Color.Transparent) {
                albumTheme.onVibrant
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            }

            LaunchedEffect(themeCacheKey, album.coverUrl) {
                val cachedTheme = albumThemeCache[themeCacheKey]
                if (cachedTheme != null) {
                    albumTheme = cachedTheme
                } else {
                    val extractedTheme = PaletteUtils.colorsFromMainColor(album.albumMainColor)
                    albumThemeCache[themeCacheKey] = extractedTheme
                    albumTheme = extractedTheme
                }
            }

            val encodedAlbumId = remember(album.albumId) {
                URLEncoder.encode(album.albumId, StandardCharsets.UTF_8.toString())
            }

            Box(
                modifier = Modifier
                    .maskClip(RoundedCornerShape(40.dp))
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .bouncyClickable(scaleDown = 0.96f, indication = null) {
                        navController.navigate("album/$encodedAlbumId")
                    }
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
            if (album.coverUrl != null) {
                val request = remember(album.coverUrl) {
                    ImageRequest.Builder(context)
                        .data(album.coverUrl)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .crossfade(true)
                        .build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = album.displayAlbum,
                    modifier = Modifier.fillMaxSize()
                        .then(
                            if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                                with(sharedTransitionScope) {
                                    Modifier.sharedElement(
                                        sharedContentState = rememberSharedContentState(key = "album-${album.displayAlbum}"),
                                        animatedVisibilityScope = animatedVisibilityScope
                                    )
                                }
                            } else Modifier
                        ),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = album.displayAlbum.take(1).uppercase(),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = albumTextColor.copy(alpha = 0.52f)
                )
            }

            // Dark gradient overlay for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                albumOverlay.copy(alpha = 0.9f)
                            ),
                            startY = 150f
                        )
                    )
            )

            Text(
                text = album.displayAlbum,
                style = MaterialTheme.typography.labelLarge,
                color = albumTextColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            )
        }
        }
    }
}

@Composable
private fun SquigglyLineSeparator(
    color: Color,
    modifier: Modifier = Modifier,
    waveHeight: Dp = 3.dp,
    strokeWidth: Dp = 3.dp,
    waves: Float = 7.6f,
    animate: Boolean = true,
    phase: Float = 0f,
    animationDurationMillis: Int = 2000,
    alpha: Float = 0.92f,
    samples: Int = 400
) {
    val density = LocalDensity.current
    val currentPhase = if (animate) {
        val infiniteTransition = rememberInfiniteTransition(label = "ArtistSplitWave")
        val animatedPhase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 2f * PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = animationDurationMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "artistSplitPhase"
        )
        animatedPhase
    } else {
        phase
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(18.dp)
    ) {
        if (samples < 2 || size.width <= 0f) return@Canvas

        val centerY = size.height / 2f
        val strokePx = with(density) { strokeWidth.toPx() }
        val amplitudePx = with(density) { waveHeight.toPx() }
        val path = Path().apply {
            val step = size.width / (samples - 1)
            moveTo(0f, centerY + (amplitudePx * sin(currentPhase)))
            for (i in 1 until samples) {
                val x = i * step
                val theta = (x / size.width) * (2f * PI.toFloat() * waves) + currentPhase
                val y = centerY + amplitudePx * sin(theta)
                lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokePx,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            ),
            alpha = alpha
        )
    }
}

private data class ArtistAlbumGroup(
    val albumId: String,
    val displayAlbum: String,
    val coverUrl: String?,
    val albumMainColor: Int?,
    val year: Int?,
    val tracks: List<TrackInfo>
)

private data class ArtistAlbumYearLane(
    val year: Int?,
    val albums: List<ArtistAlbumGroup>
)

private fun inferYearFromAlbumOrPath(albumName: String, samplePath: String?): Int? {
    val regex = Regex("(19|20)\\d{2}")
    val fromAlbum = regex.find(albumName)?.value?.toIntOrNull()
    if (fromAlbum in 1900..2099) return fromAlbum
    val fromPath = samplePath?.let { regex.find(it)?.value?.toIntOrNull() }
    return fromPath?.takeIf { it in 1900..2099 }
}

