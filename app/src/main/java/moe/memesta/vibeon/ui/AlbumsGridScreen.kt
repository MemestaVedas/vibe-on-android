package moe.memesta.vibeon.ui

import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import moe.memesta.vibeon.data.SortOption
import moe.memesta.vibeon.data.local.LibraryViewStyle
import moe.memesta.vibeon.ui.components.SortBottomSheet
import moe.memesta.vibeon.ui.theme.Dimens
import moe.memesta.vibeon.ui.utils.LocalDisplayLanguage
import moe.memesta.vibeon.ui.utils.getDisplayArtist
import moe.memesta.vibeon.ui.utils.getDisplayName
import moe.memesta.vibeon.ui.utils.LocalAlbumViewStyle

@androidx.compose.animation.ExperimentalSharedTransitionApi
@Composable
fun AlbumsGridScreen(
    viewModel: LibraryViewModel,
    onBackClick: () -> Unit,
    onSidebarClick: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onPlayAlbum: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null
) {
    val albums by viewModel.filteredAlbums.collectAsState()
    val currentSortOption by viewModel.currentAlbumSortOption.collectAsState()
    val albumViewStyle = LocalAlbumViewStyle.current
    val displayLanguage = LocalDisplayLanguage.current
    var searchQuery by remember { mutableStateOf("") }
    var showSortSheet by remember { mutableStateOf(false) }

    val visibleAlbums = remember(albums, searchQuery, displayLanguage) {
        if (searchQuery.isBlank()) {
            albums
        } else {
            albums.filter {
                it.getDisplayName(displayLanguage).contains(searchQuery, ignoreCase = true) ||
                    it.getDisplayArtist(displayLanguage).contains(searchQuery, ignoreCase = true) ||
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val spotlightAlbum = remember(visibleAlbums) {
        visibleAlbums.maxByOrNull { it.songCount }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AlbumsSearchTopBar(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            onSidebarClick = onSidebarClick,
            onSortClick = { showSortSheet = true }
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = Dimens.ScreenPadding,
                end = Dimens.ScreenPadding,
                top = 8.dp,
                bottom = contentPadding.calculateBottomPadding() + Dimens.SectionSpacing
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (albumViewStyle == LibraryViewStyle.MODERN && spotlightAlbum != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    AlbumSpotlightCard(
                        albumName = spotlightAlbum.getDisplayName(displayLanguage),
                        artistName = spotlightAlbum.getDisplayArtist(displayLanguage),
                        songCount = spotlightAlbum.songCount,
                        coverUrl = spotlightAlbum.coverUrl,
                        onClick = { onAlbumClick(spotlightAlbum.name) },
                        onPlay = { onPlayAlbum(spotlightAlbum.name) }
                    )
                }
            }

            items(
                items = if (albumViewStyle == LibraryViewStyle.MODERN) {
                    visibleAlbums.filter { it.name != spotlightAlbum?.name }
                } else {
                    visibleAlbums
                },
                key = { it.name }
            ) { album ->
                AlbumGridItem(
                    albumName = album.getDisplayName(displayLanguage),
                    artistName = album.getDisplayArtist(displayLanguage),
                    coverUrl = album.coverUrl,
                    songCount = album.songCount,
                    onClick = { onAlbumClick(album.name) },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope
                )
            }
        }
    }

    if (showSortSheet) {
        SortBottomSheet(
            title = "Sort by",
            options = SortOption.ALBUMS,
            selectedOption = currentSortOption,
            onDismiss = { showSortSheet = false },
            onOptionSelected = { option ->
                viewModel.setAlbumSortOption(option)
                showSortSheet = false
            }
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun AlbumsSearchTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSidebarClick: () -> Unit,
    onSortClick: () -> Unit
) {
    var isSearchFocused by remember { mutableStateOf(false) }
    val searchScale by animateFloatAsState(
        targetValue = if (isSearchFocused) 1f else 0.985f,
        animationSpec = spring(),
        label = "albumsSearchScale"
    )

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
            IconButton(onClick = onSidebarClick, modifier = Modifier.size(40.dp)) {
                Image(
                    painter = painterResource(id = R.drawable.ic_vibe_logo),
                    contentDescription = "Vibe-On",
                    modifier = Modifier.size(26.dp)
                )
            }

            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer(scaleX = searchScale, scaleY = searchScale)
                    .onFocusChanged { isSearchFocused = it.isFocused },
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    AnimatedVisibility(visible = searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear")
                        }
                    }
                },
                placeholder = { Text("Search albums or artists") },
                shape = RoundedCornerShape(32.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            IconButton(onClick = onSortClick, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Rounded.Sort, contentDescription = "Sort")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Albums",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun AlbumSpotlightCard(
    albumName: String,
    artistName: String,
    songCount: Int,
    coverUrl: String?,
    onClick: () -> Unit,
    onPlay: () -> Unit
) {
    var dominantColor by remember(coverUrl) { mutableStateOf<Color?>(null) }
    val gradientColor = dominantColor ?: MaterialTheme.colorScheme.primary
    val onGradientColor = dominantColor?.let { color ->
        val r = (color.red * 255).toInt()
        val g = (color.green * 255).toInt()
        val b = (color.blue * 255).toInt()
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255
        if (luminance > 0.5) Color.Black else Color.White
    } ?: MaterialTheme.colorScheme.onPrimary

    val context = LocalContext.current
    LaunchedEffect(coverUrl) {
        if (coverUrl != null) {
            try {
                withContext(Dispatchers.IO) {
                    val request = ImageRequest.Builder(context)
                        .data(coverUrl)
                        .allowHardware(false)
                        .build()
                    val result = context.imageLoader.execute(request)
                    if (result is SuccessResult) {
                        val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                        bitmap?.let {
                            val palette = Palette.from(it).generate()
                            palette.dominantSwatch?.let { swatch ->
                                dominantColor = Color(swatch.rgb)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("AlbumsGridScreen", "Failed to extract dominant color for album card", e)
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (coverUrl != null) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = albumName,
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
                    text = "Memory Highlight",
                    style = MaterialTheme.typography.labelMedium,
                    color = onGradientColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = albumName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = onGradientColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$artistName • $songCount songs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = onGradientColor.copy(alpha = 0.92f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            FilledIconButton(
                onClick = onPlay,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = "Play album")
            }
        }
    }
}
