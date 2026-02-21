package moe.memesta.vibeon.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import moe.memesta.vibeon.R
import moe.memesta.vibeon.data.AlbumInfo
import moe.memesta.vibeon.data.TrackInfo
import moe.memesta.vibeon.ui.components.*
import moe.memesta.vibeon.ui.theme.Dimens
import moe.memesta.vibeon.ui.theme.VibeAnimations
import moe.memesta.vibeon.ui.utils.LocalDisplayLanguage
import moe.memesta.vibeon.ui.utils.getDisplayArtist
import moe.memesta.vibeon.ui.utils.getDisplayName

// Accent color — now uses MaterialTheme.colorScheme.primary for dynamic theming

@Composable
fun HomeScreen(
    viewModel: LibraryViewModel,
    onTrackSelected: (TrackInfo) -> Unit,
    onAlbumSelected: (String) -> Unit,
    onArtistSelected: (String) -> Unit,
    onSearchClick: () -> Unit,
    onViewAllSongs: () -> Unit,
    contentPadding: PaddingValues,
    connectionViewModel: ConnectionViewModel
) {
    val tracks by viewModel.tracks.collectAsState()
    val albums by viewModel.homeAlbums.collectAsState()
    val artists by viewModel.homeArtists.collectAsState()
    val featuredAlbums by viewModel.featuredAlbums.collectAsState()
    val connectionState by connectionViewModel.connectionState.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val displayLanguage = LocalDisplayLanguage.current

    val isLoading = tracks.isEmpty() && connectionState == ConnectionState.CONNECTED

    // Define a lighter surface color to make wavy separator transitions distinct
    val sectionSurface = Color.White.copy(alpha = 0.08f).compositeOver(MaterialTheme.colorScheme.surface)
    val appBackground = MaterialTheme.colorScheme.background

    val listState = rememberLazyListState()
    val isDragged by listState.interactionSource.collectIsDraggedAsState()

    // Pre-calculate chunks for Recently Added grid
    val recentChunks = remember(tracks) {
        tracks.take(20).chunked(4)
    }

    // Page-load entrance animation
    var pageVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(50)
        pageVisible = true
    }

    val infiniteTransition = rememberInfiniteTransition(label = "logo_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "logo_angle"
    )

    Box(modifier = Modifier.fillMaxSize().background(sectionSurface)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                top = 72.dp,
                bottom = contentPadding.calculateBottomPadding() + Dimens.SectionSpacing
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Section: Recently Added
            item(key = "section_recent") {
                AnimatedVisibility(
                    visible = pageVisible && !isLoading,
                    enter = fadeIn(tween(300, delayMillis = 80)) + slideInHorizontally(
                        initialOffsetX = { -80 },
                        animationSpec = tween(350, delayMillis = 80)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .background(sectionSurface)
                            .padding(bottom = 24.dp)
                    ) {
                        SectionHeader(
                            title = "Your Songs", 
                            onSeeAllClick = onViewAllSongs,
                            modifier = Modifier.padding(top = Dimens.SectionPadding)
                        )
                        FadeEdgeLazyRow(
                            contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(
                                items = recentChunks,
                                key = { chunk -> chunk.joinToString("-") { it.path } }
                            ) { columnTracks ->
                                Column(
                                    modifier = Modifier.width(280.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    columnTracks.forEach { track ->
                                        GridTrackCard(
                                            track = track,
                                            onClick = {
                                                viewModel.playTrack(track)
                                                onTrackSelected(track)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (isLoading) {
                    Column(modifier = Modifier.padding(top = Dimens.SectionSpacing)) {
                        SectionHeader("Your Songs")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
                        ) {
                            items(5, key = { "skeleton_recent_$it" }) { SkeletonSquareCard() }
                        }
                    }
                }
            }

            // Section: Featured Carousel
            item(key = "section_featured_carousel") {
                if (featuredAlbums.isNotEmpty() && !isLoading) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        HeroHeader(
                            albums = featuredAlbums,
                            onPlayClick = { album -> onAlbumSelected(album.name) },
                            displayLanguage = displayLanguage,
                            topWaveColor = sectionSurface
                        )
                    }
                }
            }

            // Section: Albums
            item(key = "section_albums") {
                AnimatedVisibility(
                    visible = pageVisible && !isLoading,
                    enter = fadeIn(tween(300, delayMillis = 160)) + slideInHorizontally(
                        initialOffsetX = { -80 },
                        animationSpec = tween(350, delayMillis = 160)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .background(sectionSurface)
                            .padding(bottom = 24.dp)
                    ) {
                        SectionHeader(
                            "Albums", 
                            onSeeAllClick = { /* Navigate */ },
                            modifier = Modifier.padding(top = Dimens.SectionPadding)
                        )
                        FadeEdgeLazyRow(
                            contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
                        ) {
                            items(
                                items = albums.take(5),
                                key = { "album_${it.name}" }
                            ) { album ->
                                AlbumCard(
                                    albumName = album.getDisplayName(displayLanguage),
                                    coverUrl = album.coverUrl,
                                    onClick = { onAlbumSelected(album.name) }
                                )
                            }
                        }
                    }
                }
                if (isLoading) {
                    Column(modifier = Modifier.padding(top = Dimens.SectionSpacing)) {
                        SectionHeader("Albums")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
                        ) {
                            items(5, key = { "skeleton_album_$it" }) { SkeletonAlbumCard() }
                        }
                    }
                }
            }

            item(key = "sep_albums_artists") {
                WavySeparator(
                    colorTop = sectionSurface,
                    colorBottom = appBackground
                )
            }

            // Section: Artists
            item(key = "section_artists") {
                AnimatedVisibility(
                    visible = pageVisible && !isLoading,
                    enter = fadeIn(tween(300, delayMillis = 240)) + slideInHorizontally(
                        initialOffsetX = { -80 },
                        animationSpec = tween(350, delayMillis = 240)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .background(appBackground)
                            .padding(bottom = 24.dp)
                    ) {
                        SectionHeader(
                            "Artists", 
                            onSeeAllClick = { /* Navigate */ },
                            modifier = Modifier.padding(top = Dimens.SectionPadding)
                        )
                        FadeEdgeLazyRow(
                            contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
                        ) {
                            items(
                                items = artists.take(5),
                                key = { "artist_${it.name}" }
                            ) { artist ->
                                ArtistPill(
                                    artistName = artist.getDisplayName(displayLanguage),
                                    photoUrl = artist.photoUrl,
                                    onClick = { onArtistSelected(artist.name) }
                                )
                            }
                        }
                    }
                }
                if (isLoading) {
                    Column(modifier = Modifier.padding(top = Dimens.SectionSpacing)) {
                        SectionHeader("Artists")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
                        ) {
                            items(5, key = { "skeleton_artist_$it" }) { SkeletonArtistPill() }
                        }
                    }
                }
            }

            item(key = "sep_artists_stats") {
                WavySeparator(
                    colorTop = appBackground,
                    colorBottom = sectionSurface
                )
            }

            // Section: Statistics
            item(key = "section_stats") {
                AnimatedVisibility(
                    visible = pageVisible && !isLoading,
                    enter = fadeIn(tween(300, delayMillis = 320)) + slideInHorizontally(
                        initialOffsetX = { -80 },
                        animationSpec = tween(350, delayMillis = 320)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .background(sectionSurface)
                            .fillMaxWidth()
                            .padding(bottom = 40.dp)
                    ) {
                        Box(modifier = Modifier.padding(top = Dimens.SectionPadding)) {
                            StatisticsSection(stats = stats)
                        }
                    }
                }
            }
        }

        // Rotating Logo Overlay
        Image(
            painter = painterResource(id = R.drawable.ic_vibe_logo),
            contentDescription = "Vibe-On Logo",
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(Dimens.ScreenPadding)
                .size(40.dp)
                .graphicsLayer { rotationZ = rotationAngle }
        )

        // Search + Connection Status Overlay
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(Dimens.ScreenPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ConnectionStatusIndicator(
                connectionState = connectionState,
                modifier = Modifier
            )

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onSearchClick) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// Custom easing
private val EaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)

@Composable
fun FadeEdgeLazyRow(
    contentPadding: PaddingValues,
    horizontalArrangement: Arrangement.Horizontal,
    content: LazyListScope.() -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            contentPadding = contentPadding,
            horizontalArrangement = horizontalArrangement,
            content = content,
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}

@Composable
fun HeroHeader(
    albums: List<AlbumInfo>,
    onPlayClick: (AlbumInfo) -> Unit,
    displayLanguage: moe.memesta.vibeon.data.local.DisplayLanguage = moe.memesta.vibeon.data.local.DisplayLanguage.ORIGINAL,
    topWaveColor: Color = Color.Transparent
) {
    if (albums.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { albums.size })
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()

    // Auto-scroll
    LaunchedEffect(pagerState, isDragged) {
        if (!isDragged && albums.isNotEmpty()) {
            while (true) {
                delay(7000)
                try {
                    if (albums.isNotEmpty()) {
                        val nextPage = (pagerState.currentPage + 1) % albums.size
                        pagerState.animateScrollToPage(
                            nextPage,
                            animationSpec = tween(durationMillis = VibeAnimations.HeroDuration, easing = FastOutSlowInEasing)
                        )
                    }
                } catch (_: Exception) { }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val album = albums.getOrNull(page) ?: return@HorizontalPager

            // Full-width edge-to-edge container, clipped at the bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .clip(WavyBottomShape(12.dp, 6.5f))
            ) {
                val context = LocalContext.current
                val request = remember(album.coverUrl) {
                    ImageRequest.Builder(context)
                        .data(album.coverUrl)
                        .crossfade(true)
                        .build()
                }

                // Album art background - NO dark overlay
                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    model = request,
                    contentDescription = null
                )

                // Dark overlay for legibility - Solid Matte
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0A0A0C).copy(alpha = 0.65f))
                )

                // Content Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Dimens.ScreenPadding, vertical = 24.dp)
                ) {
                    // Content
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(bottom = 24.dp), // adjust padding to avoid clipping by the bottom wave
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            // "New for you" badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(50))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "✦  New for you",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = album.getDisplayName(displayLanguage),
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = Color.White
                            )

                            Text(
                                text = album.getDisplayArtist(displayLanguage),
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.65f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Petal (Sun) Play Button at Bottom Right
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(PetalShape(petals = 8, depth = 0.15f))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable { onPlayClick(album) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                // Top Wavy Separator (drops stalactites INTO the image)
                WavySeparator(
                    colorTop = topWaveColor,
                    colorBottom = Color.Transparent,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }
}
/**
 * "See All" button to show more items in a horizontal scrollable list
 */
@Composable
fun SeeAllButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Color.White.copy(alpha = 0.06f),
                RoundedCornerShape(16.dp)
            )
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "→",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = "See\nAll",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

class WavyBottomShape(private val waveHeight: androidx.compose.ui.unit.Dp, private val waveFrequency: Float) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(size: androidx.compose.ui.geometry.Size, layoutDirection: androidx.compose.ui.unit.LayoutDirection, density: androidx.compose.ui.unit.Density): androidx.compose.ui.graphics.Outline {
        val width = size.width
        val height = size.height
        val amplitude = with(density) { waveHeight.toPx() }
        val freq = waveFrequency * 2f * Math.PI.toFloat() / width

        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, 0f)
            lineTo(width, 0f)
            
            val waveBottomBase = height - amplitude
            for (x in width.toInt() downTo 0 step 5) {
                val angle: Double = ((x.toFloat() * freq)).toDouble()
                val sinValue: Float = kotlin.math.sin(angle).toFloat()
                val waveFactor: Float = (sinValue + 1f) * 0.5f 
                val y: Float = waveBottomBase + (waveFactor * amplitude)
                
                if (x == width.toInt()) {
                    lineTo(width, y)
                } else {
                    lineTo(x.toFloat(), y)
                }
            }
            lineTo(0f, waveBottomBase + ((kotlin.math.sin(0.0).toFloat() + 1f) * 0.5f * amplitude))
            close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

class PetalShape(private val petals: Int = 8, private val depth: Float = 0.15f) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(size: androidx.compose.ui.geometry.Size, layoutDirection: androidx.compose.ui.unit.LayoutDirection, density: androidx.compose.ui.unit.Density): androidx.compose.ui.graphics.Outline {
        val path = androidx.compose.ui.graphics.Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val baseRadius = size.width / 2f * (1f - depth)
        val variation = size.width / 2f * depth

        for (i in 0..360 step 5) {
            val angle = Math.toRadians(i.toDouble())
            val r = baseRadius + variation * kotlin.math.sin(angle * petals)
            val x = cx + r * kotlin.math.cos(angle).toFloat()
            val y = cy + r * kotlin.math.sin(angle).toFloat()
            if (i == 0) {
                path.moveTo(x.toFloat(), y.toFloat())
            } else {
                path.lineTo(x.toFloat(), y.toFloat())
            }
        }
        path.close()
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}