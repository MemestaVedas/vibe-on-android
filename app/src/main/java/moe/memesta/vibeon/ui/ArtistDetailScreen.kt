package moe.memesta.vibeon.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.*
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.carousel.CarouselItemScope
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import moe.memesta.vibeon.data.TrackInfo
import moe.memesta.vibeon.ui.components.AlbumCard
import moe.memesta.vibeon.ui.AlbumTrackRow
import moe.memesta.vibeon.ui.theme.Dimens
import moe.memesta.vibeon.ui.theme.MotionTokens
import moe.memesta.vibeon.ui.theme.bouncyClickable
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import moe.memesta.vibeon.ui.utils.LocalDisplayLanguage
import moe.memesta.vibeon.ui.utils.getDisplayArtist
import moe.memesta.vibeon.ui.utils.getDisplayAlbum
import androidx.compose.foundation.gestures.scrollBy
import kotlinx.coroutines.yield
import kotlin.math.abs

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
                    year = groupedTracks.mapNotNull { it.year }.maxOrNull()
                        ?: inferYearFromAlbumOrPath(albumName, groupedTracks.firstOrNull()?.path),
                    tracks = groupedTracks
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
        albumYearLanes.asReversed().flatMap { it.albums }
    }

    val latestAlbum = remember(artistAlbums) {
        artistAlbums.maxWithOrNull(
            compareBy<ArtistAlbumGroup> { it.year ?: Int.MIN_VALUE }
                .thenBy { it.displayAlbum }
        )
    }

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

    val dividerColor = lerp(
        MaterialTheme.colorScheme.outlineVariant,
        MaterialTheme.colorScheme.primary,
        splitProgress
    )

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
                top = 0.dp,
                bottom = contentPadding.calculateBottomPadding() + Dimens.SectionSpacing
            )
        ) {
            item {
                FeaturedLatestAlbumBanner(
                    artistName = displayArtistName,
                    latestAlbumName = latestAlbum?.displayAlbum,
                    albumsCount = artistAlbums.size,
                    songsCount = artistTracks.size,
                    coverUrl = latestAlbum?.coverUrl,
                    onPlayLatest = {
                        val latestTracks = latestAlbum?.tracks.orEmpty()
                        if (latestTracks.isNotEmpty()) {
                            val firstTrack = latestTracks.first()
                            viewModel.playTrack(firstTrack, latestTracks)
                            onTrackSelected(firstTrack)
                        } else if (artistTracks.isNotEmpty()) {
                            val firstTrack = artistTracks.first()
                            viewModel.playTrack(firstTrack, artistTracks)
                            onTrackSelected(firstTrack)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
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
                    navController = navController,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            item(key = "split-anchor") {
                Spacer(modifier = Modifier.height(24.dp))
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
                            text = if (album.year != null) "${album.year} • ${album.displayAlbum}" else album.displayAlbum,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
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
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .then(
                        if (showTitle) Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
                        else Modifier
                    ),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
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

    HorizontalMultiBrowseCarousel(
        state = carouselState,
        preferredItemWidth = Dimens.StandardCardWidth + 24.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.StandardCardWidth + 24.dp),
        itemSpacing = Dimens.ItemSpacing,
        contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding)
    ) { index ->
        val album = lane.albums[index]
        Box(
            modifier = Modifier
                .maskClip(MaterialTheme.shapes.extraLarge)
                .fillMaxSize()
                .bouncyClickable(scaleDown = 0.96f, indication = null) {
                    navController.navigate("album/${java.net.URLEncoder.encode(album.albumId, "UTF-8")}")
                }
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            if (album.coverUrl != null) {
                val context = LocalContext.current
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                            ),
                            startY = 150f
                        )
                    )
            )

            Text(
                text = album.displayAlbum,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
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

@Composable
private fun FeaturedLatestAlbumBanner(
    artistName: String,
    latestAlbumName: String?,
    albumsCount: Int,
    songsCount: Int,
    coverUrl: String?,
    onPlayLatest: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(410.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = Dimens.ScreenPadding)
                .height(290.dp)
                .clip(RoundedCornerShape(bottomStart = 48.dp, bottomEnd = 48.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
        ) {
            if (coverUrl != null) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f)
                                )
                            )
                        )
                )
            }

            Text(
                text = "$albumsCount albums",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.92f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .rotate(90f)
                    .padding(end = 20.dp)
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
            ) {
                Text(
                    text = latestAlbumName ?: artistName,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = artistName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
        
        Text(
            text = "$songsCount songs",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = Dimens.ScreenPadding, bottom = 6.dp)
        )
        
        FloatingActionButton(
            onClick = onPlayLatest,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Dimens.ScreenPadding + 6.dp, bottom = 6.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = "Play latest album")
        }
    }
}



private data class ArtistAlbumGroup(
    val albumId: String,
    val displayAlbum: String,
    val coverUrl: String?,
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

