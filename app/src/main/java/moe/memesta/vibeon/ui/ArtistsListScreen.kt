package moe.memesta.vibeon.ui

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.memesta.vibeon.R
import moe.memesta.vibeon.data.ArtistItemData
import moe.memesta.vibeon.data.SortOption
import moe.memesta.vibeon.ui.components.ArtistGridItem
import moe.memesta.vibeon.ui.components.SortBottomSheet
import moe.memesta.vibeon.ui.theme.Dimens
import moe.memesta.vibeon.ui.utils.AlbumArtColorCache
import moe.memesta.vibeon.ui.utils.LocalArtistViewStyle
import moe.memesta.vibeon.ui.utils.LocalDisplayLanguage
import moe.memesta.vibeon.ui.utils.getDisplayName
import moe.memesta.vibeon.data.local.LibraryViewStyle

@androidx.compose.animation.ExperimentalSharedTransitionApi
@Composable
fun ArtistsListScreen(
    viewModel: LibraryViewModel,
    onBackClick: () -> Unit,
    onSidebarClick: () -> Unit,
    onArtistClick: (String) -> Unit,
    onPlayArtist: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null
) {
    val tracks by viewModel.tracks.collectAsState()
    val currentSortOption by viewModel.currentArtistSortOption.collectAsState()
    val artistViewStyle = LocalArtistViewStyle.current
    val displayLanguage = LocalDisplayLanguage.current
    var showSortSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val artists = remember(tracks, searchQuery, currentSortOption, displayLanguage) {
        tracks.groupBy { it.artist }
            .map { (artist, artistTracks) ->
                val firstTrack = artistTracks.firstOrNull()
                ArtistItemData(
                    name = artist,
                    followerCount = "${artistTracks.size} Tracks",
                    photoUrl = firstTrack?.coverUrl,
                    nameRomaji = firstTrack?.artistRomaji,
                    nameEn = firstTrack?.artistEn
                )
            }
            .filter {
                searchQuery.isEmpty() ||
                it.getDisplayName(displayLanguage).contains(searchQuery, ignoreCase = true) ||
                it.name.contains(searchQuery, ignoreCase = true)
            }
            .let { items ->
                when (currentSortOption) {
                    SortOption.ArtistNameAZ -> items.sortedBy { it.getDisplayName(displayLanguage).lowercase() }
                    SortOption.ArtistNameZA -> items.sortedByDescending { it.getDisplayName(displayLanguage).lowercase() }
                    SortOption.ArtistTrackCountDesc -> items.sortedByDescending {
                        it.followerCount.substringBefore(' ').toIntOrNull() ?: 0
                    }
                    SortOption.ArtistTrackCountAsc -> items.sortedBy {
                        it.followerCount.substringBefore(' ').toIntOrNull() ?: 0
                    }
                    else -> items.sortedBy { it.name.lowercase() }
                }
            }
    }
    val spotlightArtist = remember(artists) {
        artists.maxByOrNull { it.followerCount.substringBefore(' ').toIntOrNull() ?: 0 }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ArtistsSearchTopBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onSidebarClick = onSidebarClick,
                onSortClick = { showSortSheet = true }
            )
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + contentPadding.calculateBottomPadding() + 24.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (artistViewStyle == LibraryViewStyle.MODERN && spotlightArtist != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ArtistSpotlightCard(
                        artistName = spotlightArtist.getDisplayName(displayLanguage),
                        trackCount = spotlightArtist.followerCount,
                        photoUrl = spotlightArtist.photoUrl,
                        onClick = { onArtistClick(spotlightArtist.name) },
                        onPlay = { onPlayArtist(spotlightArtist.name) }
                    )
                }
            }

            items(
                if (artistViewStyle == LibraryViewStyle.MODERN) {
                    artists.filter { it.name != spotlightArtist?.name }
                } else {
                    artists
                }
            ) { artist ->
                ArtistGridItem(
                    artistName = artist.getDisplayName(displayLanguage),
                    photoUrl = artist.photoUrl,
                    onClick = { onArtistClick(artist.name) },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope
                )
            }
        }
    }

    if (showSortSheet) {
        SortBottomSheet(
            title = "Sort artists",
            options = SortOption.ARTISTS,
            selectedOption = currentSortOption,
            onDismiss = { showSortSheet = false },
            onOptionSelected = { option ->
                viewModel.setArtistSortOption(option)
                showSortSheet = false
            }
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ArtistsSearchTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSidebarClick: () -> Unit,
    onSortClick: () -> Unit
) {
    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = Dimens.ScreenPadding, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AnimatedVisibility(visible = !isSearchExpanded) {
                IconButton(onClick = onSidebarClick, modifier = Modifier.size(40.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_vibe_logo),
                        contentDescription = "Vibe-On",
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            SearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                onSearch = { isSearchExpanded = false },
                active = isSearchExpanded,
                onActiveChange = { isSearchExpanded = it },
                modifier = Modifier
                    .then(if (isSearchExpanded) Modifier.fillMaxWidth() else Modifier.weight(1f)),
                leadingIcon = {
                    if (isSearchExpanded) {
                        IconButton(onClick = { isSearchExpanded = false }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    } else {
                        Icon(Icons.Rounded.Search, contentDescription = null)
                    }
                },
                trailingIcon = {
                    AnimatedVisibility(visible = searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear")
                        }
                    }
                },
                placeholder = { Text("Search artists") },
                shape = RoundedCornerShape(32.dp),
                tonalElevation = 0.dp,
                colors = SearchBarDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    dividerColor = Color.Transparent
                )
            ) { }

            AnimatedVisibility(visible = !isSearchExpanded) {
                IconButton(onClick = onSortClick, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Rounded.Sort, contentDescription = "Sort")
                }
            }
        }

        AnimatedVisibility(visible = !isSearchExpanded) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Artists",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ArtistSpotlightCard(
    artistName: String,
    trackCount: String,
    photoUrl: String?,
    onClick: () -> Unit,
    onPlay: () -> Unit
) {
    var dominantColor by remember(photoUrl) { mutableStateOf(photoUrl?.let { AlbumArtColorCache.get(it) }) }
    val gradientColor = dominantColor ?: MaterialTheme.colorScheme.primary
    val onGradientColor = dominantColor?.let { color ->
        val r = (color.red * 255).toInt()
        val g = (color.green * 255).toInt()
        val b = (color.blue * 255).toInt()
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255
        if (luminance > 0.5) Color.Black else Color.White
    } ?: MaterialTheme.colorScheme.onPrimary

    val context = LocalContext.current
    LaunchedEffect(photoUrl) {
        if (photoUrl != null) {
            AlbumArtColorCache.get(photoUrl)?.let {
                dominantColor = it
                return@LaunchedEffect
            }
            try {
                withContext(Dispatchers.IO) {
                    val request = ImageRequest.Builder(context)
                        .data(photoUrl)
                        .allowHardware(false)
                        .build()
                    val result = context.imageLoader.execute(request)
                    if (result is SuccessResult) {
                        val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                        bitmap?.let {
                            val palette = Palette.from(it).generate()
                            palette.dominantSwatch?.let { swatch ->
                                val extracted = Color(swatch.rgb)
                                dominantColor = extracted
                                AlbumArtColorCache.put(photoUrl, extracted)
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (photoUrl != null) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = artistName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                    gradientColor.copy(alpha = 0.88f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Spotlight Artist",
                    style = MaterialTheme.typography.labelMedium,
                    color = onGradientColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = artistName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = onGradientColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = trackCount,
                    style = MaterialTheme.typography.bodyMedium,
                    color = onGradientColor.copy(alpha = 0.92f)
                )
            }

            FilledIconButton(
                onClick = onPlay,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(14.dp)
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = "Play artist")
            }
        }
    }
}
