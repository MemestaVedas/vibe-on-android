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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.navigation.NavController
import moe.memesta.vibeon.data.TrackInfo
import moe.memesta.vibeon.ui.components.AlbumCard
import moe.memesta.vibeon.ui.theme.Dimens
import moe.memesta.vibeon.ui.theme.MotionTokens
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import moe.memesta.vibeon.ui.utils.LocalDisplayLanguage
import moe.memesta.vibeon.ui.utils.getDisplayArtist
import moe.memesta.vibeon.ui.utils.getDisplayAlbum
import kotlinx.coroutines.launch
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
    val homeArtists by viewModel.homeArtists.collectAsState()
    
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
    
    // Get artist photo from HomeArtists list
    val artistInfo = remember(homeArtists, decodedArtistName) {
        homeArtists.find { it.name == decodedArtistName }
    }
    val photoUrl = artistInfo?.photoUrl ?: artistTracks.firstOrNull()?.coverUrl
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

    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var titleLayout by remember(displayArtistName) { mutableStateOf<TextLayoutResult?>(null) }
    val splitIndex = 2 + albumYearLanes.size
    var hasInitializedStartPosition by remember(decodedArtistName) { mutableStateOf(false) }

    var lastFlingVelocityY by remember { mutableStateOf(0f) }
    val snapVelocityThreshold = 2500f

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override suspend fun onPreFling(available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                lastFlingVelocityY = available.y
                return androidx.compose.ui.unit.Velocity.Zero
            }

            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: NestedScrollSource): androidx.compose.ui.geometry.Offset {
                if (source == NestedScrollSource.Drag) {
                    lastFlingVelocityY = 0f
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }

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

    fun currentPositionWeight(): Float {
        val indexWeight = scrollState.firstVisibleItemIndex.toFloat()
        val offsetWeight = scrollState.firstVisibleItemScrollOffset / 10000f
        return indexWeight + offsetWeight
    }

    fun snapToSection() {
        if (artistTracks.isEmpty()) return

        val currentWeight = currentPositionWeight()
        val albumsWeight = 0f
        val songsWeight = splitIndex.toFloat()

        val targetIndex = when {
            lastFlingVelocityY <= -snapVelocityThreshold -> splitIndex
            lastFlingVelocityY >= snapVelocityThreshold -> 0
            abs(currentWeight - albumsWeight) <= abs(currentWeight - songsWeight) -> 0
            else -> splitIndex
        }

        if (targetIndex != scrollState.firstVisibleItemIndex || scrollState.firstVisibleItemScrollOffset > 8) {
            scope.launch {
                scrollState.animateScrollToItem(targetIndex)
            }
        }
        lastFlingVelocityY = 0f
    }

    LaunchedEffect(scrollState.isScrollInProgress) {
        if (!scrollState.isScrollInProgress) {
            snapToSection()
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
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
            contentPadding = PaddingValues(
                top = 0.dp,
                bottom = contentPadding.calculateBottomPadding() + Dimens.SectionSpacing
            )
        ) {
            item {
                Spacer(modifier = Modifier.height(86.dp))
                ArtistTopZone(
                    displayArtistName = displayArtistName,
                    decodedArtistName = decodedArtistName,
                    photoUrl = photoUrl,
                    artistAlbumsCount = artistAlbums.size,
                    artistTracksCount = artistTracks.size,
                    splitProgress = splitProgress,
                    titleLayout = titleLayout,
                    onTitleLayout = { titleLayout = it },
                    onPlay = {
                        if (artistTracks.isNotEmpty()) {
                            viewModel.playArtist(decodedArtistName)
                            onTrackSelected(artistTracks.first())
                        }
                    },
                    onShuffle = {
                        if (artistTracks.isNotEmpty()) {
                            viewModel.playTrack(artistTracks.random(), artistTracks)
                            onTrackSelected(artistTracks.first())
                        }
                    },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            item {
                Text(
                    text = "Albums",
                    style = MaterialTheme.typography.titleLarge,
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
                SplitBoundary(
                    dividerColor = dividerColor,
                    songsLabelAlpha = songsLabelAlpha,
                    albumsCount = artistAlbums.size
                )
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
@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
private fun ArtistTopZone(
    displayArtistName: String,
    decodedArtistName: String,
    photoUrl: String?,
    artistAlbumsCount: Int,
    artistTracksCount: Int,
    splitProgress: Float,
    titleLayout: TextLayoutResult?,
    onTitleLayout: (TextLayoutResult) -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope?,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope?
) {
    val avatarScale by animateFloatAsState(
        targetValue = (1f - splitProgress * 0.14f).coerceIn(0.86f, 1f),
        animationSpec = MotionTokens.Spatial.standard(),
        label = "avatarScale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!photoUrl.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .graphicsLayer {
                        scaleX = avatarScale
                        scaleY = avatarScale
                    }
                    .then(
                        if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                            with(sharedTransitionScope) {
                                Modifier.sharedElement(
                                    sharedContentState = rememberSharedContentState(key = "artist-${decodedArtistName}"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                            }
                        } else Modifier
                    )
                    .clip(RoundedCornerShape(56.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = decodedArtistName,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val titleStyle = MaterialTheme.typography.displayMedium
            val maxLineWidthPx = constraints.maxWidth * 0.88f
            val measuredLineWidth = titleLayout?.getLineRight(0) ?: 0f
            val isWideTitle = measuredLineWidth > maxLineWidthPx
            val dynamicWeight = if (isWideTitle) FontWeight.Medium else FontWeight.Black
            val dynamicLetterSpacing = if (isWideTitle) (-0.1).sp else (-0.8).sp

            Text(
                text = displayArtistName,
                style = titleStyle,
                fontWeight = dynamicWeight,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                letterSpacing = dynamicLetterSpacing,
                onTextLayout = onTitleLayout
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Text(
                text = "$artistAlbumsCount Albums • $artistTracksCount Tracks",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = onPlay,
                contentPadding = PaddingValues(horizontal = 26.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Play")
            }

            Spacer(modifier = Modifier.width(14.dp))

            OutlinedButton(onClick = onShuffle) {
                Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Shuffle")
            }
        }
    }
}

@Composable
@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
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

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
    ) {
        items(lane.albums) { album ->
            AlbumCard(
                albumName = album.displayAlbum,
                coverUrl = album.coverUrl,
                onClick = {
                    navController.navigate("album/${java.net.URLEncoder.encode(album.albumId, "UTF-8")}")
                },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    }
}

@Composable
private fun SplitBoundary(
    dividerColor: Color,
    songsLabelAlpha: Float,
    albumsCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenPadding)
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = 1.dp,
                    color = dividerColor.copy(alpha = 0.38f),
                    shape = RoundedCornerShape(16.dp)
                )
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.26f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Albums $albumsCount",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(8.dp))

            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = dividerColor.copy(alpha = 0.45f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Songs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = songsLabelAlpha)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
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

