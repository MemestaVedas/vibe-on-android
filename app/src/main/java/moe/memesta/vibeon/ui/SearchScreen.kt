package moe.memesta.vibeon.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import moe.memesta.vibeon.data.TrackInfo
import moe.memesta.vibeon.ui.components.AlbumCard
import moe.memesta.vibeon.ui.components.ArtistPill
import moe.memesta.vibeon.ui.components.SectionHeader
import moe.memesta.vibeon.ui.components.SkeletonAlbumCard
import moe.memesta.vibeon.ui.components.SkeletonArtistPill
import moe.memesta.vibeon.ui.components.SkeletonSquareCard
import moe.memesta.vibeon.ui.components.SquareTrackCard
import moe.memesta.vibeon.ui.theme.Dimens
import moe.memesta.vibeon.ui.utils.LocalDisplayLanguage
import moe.memesta.vibeon.ui.utils.getDisplayName

class WavyTopShape(private val waveHeight: androidx.compose.ui.unit.Dp, private val waveFrequency: Float) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(size: androidx.compose.ui.geometry.Size, layoutDirection: androidx.compose.ui.unit.LayoutDirection, density: androidx.compose.ui.unit.Density): androidx.compose.ui.graphics.Outline {
        val width = size.width
        val height = size.height
        val amplitude = with(density) { waveHeight.toPx() }
        val freq = waveFrequency * 2f * Math.PI.toFloat() / width

        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, amplitude)
            for (x in 0..width.toInt() step 5) {
                val angle: Double = ((x.toFloat() * freq)).toDouble()
                val sinValue: Float = kotlin.math.sin(angle).toFloat()
                val waveFactor: Float = (sinValue + 1f) * 0.5f 
                val y: Float = amplitude - (waveFactor * amplitude)
                if (x == width.toInt()) {
                    lineTo(width, y)
                } else {
                    lineTo(x.toFloat(), y)
                }
            }
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

@Composable
fun SearchScreen(
    viewModel: LibraryViewModel,
    onTrackSelected: (TrackInfo) -> Unit,
    onAlbumSelected: (String) -> Unit,
    onArtistSelected: (String) -> Unit,
    onClose: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    
    val songResults by viewModel.songResults.collectAsState()
    val albumResults by viewModel.albumResults.collectAsState()
    val artistResults by viewModel.artistResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val displayLanguage = LocalDisplayLanguage.current
    
    val hasResults = songResults.isNotEmpty() || albumResults.isNotEmpty() || artistResults.isNotEmpty()
    val hasSearched = searchQuery.isNotBlank()

    val accentColor = MaterialTheme.colorScheme.primary
    val overlayBackgroundColor = Color.Black.copy(alpha = 0.5f)

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(overlayBackgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.15f)
            ) {}

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.85f)
                    .clip(WavyTopShape(12.dp, 4.5f))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { }
                    )
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(modifier = Modifier.height(28.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimens.ScreenPadding)
                    ) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { 
                                searchQuery = it
                                viewModel.searchLibrary(it)
                            },
                            placeholder = { 
                                Text(
                                    "Search...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                ) 
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CircleShape)
                                .focusRequester(focusRequester),
                            leadingIcon = { 
                                Icon(
                                    Icons.Default.Search, 
                                    contentDescription = null,
                                    tint = if (searchQuery.isNotEmpty()) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                                ) 
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { 
                                        searchQuery = ""
                                        viewModel.searchLibrary("")
                                    }) {
                                        Icon(
                                            Icons.Default.Close, 
                                            contentDescription = "Clear",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = accentColor
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        Crossfade(
                            targetState = when {
                                isLoading && hasSearched -> "loading"
                                hasResults -> "results"
                                hasSearched && !hasResults && error != null -> "error"
                                hasSearched && !hasResults -> "empty"
                                else -> "initial"
                            },
                            label = "searchStateTransition"
                        ) { state ->
                            when (state) {
                                "loading" -> {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(
                                            bottom = contentPadding.calculateBottomPadding() + 24.dp
                                        ),
                                        verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing)
                                    ) {
                                        item {
                                            Column {
                                                SectionHeader("Songs")
                                                LazyRow(
                                                    contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding),
                                                    horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
                                                ) {
                                                    items(4) { SkeletonSquareCard() }
                                                }
                                            }
                                        }
                                        item {
                                            Column {
                                                SectionHeader("Albums")
                                                LazyRow(
                                                    contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding),
                                                    horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
                                                ) {
                                                    items(3) { SkeletonAlbumCard() }
                                                }
                                            }
                                        }
                                        item {
                                            Column {
                                                SectionHeader("Artists")
                                                LazyRow(
                                                    contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding),
                                                    horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
                                                ) {
                                                    items(3) { SkeletonArtistPill() }
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                "results" -> {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(
                                            bottom = contentPadding.calculateBottomPadding() + 24.dp
                                        ),
                                        verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing)
                                    ) {
                                        if (songResults.isNotEmpty()) {
                                            item {
                                                Column {
                                                    SectionHeader("Songs")
                                                    LazyRow(
                                                        contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding),
                                                        horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
                                                    ) {
                                                        items(songResults.take(10)) { track ->
                                                            SquareTrackCard(
                                                                track = track,
                                                                onClick = { 
                                                                    viewModel.playTrack(track, songResults)
                                                                    onTrackSelected(track) 
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        
                                        if (albumResults.isNotEmpty()) {
                                            item {
                                                Column {
                                                    SectionHeader("Albums")
                                                    LazyRow(
                                                        contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding),
                                                        horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
                                                    ) {
                                                        items(albumResults) { album ->
                                                            AlbumCard(
                                                                albumName = album.getDisplayName(displayLanguage),
                                                                coverUrl = album.coverUrl,
                                                                onClick = { onAlbumSelected(album.name) }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        
                                        if (artistResults.isNotEmpty()) {
                                            item {
                                                Column {
                                                    SectionHeader("Artists")
                                                    LazyRow(
                                                        contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding),
                                                        horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
                                                    ) {
                                                        items(artistResults) { artist ->
                                                            ArtistPill(
                                                                artistName = artist.getDisplayName(displayLanguage),
                                                                photoUrl = artist.photoUrl,
                                                                onClick = { onArtistSelected(artist.name) }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                "empty" -> {
                                    EmptySearchState(query = searchQuery)
                                }
                                
                                "error" -> {
                                    ErrorSearchState(error = error ?: "Something went wrong")
                                }
                                
                                else -> {
                                    Box(modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptySearchState(query: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 48.dp, start = 48.dp, end = 48.dp)
        ) {
            Text(
                "🔍",
                style = MaterialTheme.typography.displayLarge
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "No results for \"$query\"",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ErrorSearchState(error: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 48.dp, start = 48.dp, end = 48.dp)
        ) {
            Text(
                "😵",
                style = MaterialTheme.typography.displayLarge
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Oops! Something went wrong",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
