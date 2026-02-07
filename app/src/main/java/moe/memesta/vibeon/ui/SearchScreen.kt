package moe.memesta.vibeon.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import moe.memesta.vibeon.data.AlbumInfo
import moe.memesta.vibeon.data.ArtistItemData
import moe.memesta.vibeon.data.TrackInfo
import moe.memesta.vibeon.ui.components.AlbumCard
import moe.memesta.vibeon.ui.components.ArtistPill
import moe.memesta.vibeon.ui.components.SectionHeader
import moe.memesta.vibeon.ui.components.SkeletonAlbumCard
import moe.memesta.vibeon.ui.components.SkeletonArtistPill
import moe.memesta.vibeon.ui.components.SkeletonSquareCard
import moe.memesta.vibeon.ui.components.SquareTrackCard

@Composable
fun SearchScreen(
    viewModel: LibraryViewModel,
    onTrackSelected: (TrackInfo) -> Unit,
    onAlbumSelected: (String) -> Unit,
    onArtistSelected: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    // Use rememberSaveable to preserve search state across navigation
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    
    // Collect search results
    val songResults by viewModel.songResults.collectAsState()
    val albumResults by viewModel.albumResults.collectAsState()
    val artistResults by viewModel.artistResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    val hasResults = songResults.isNotEmpty() || albumResults.isNotEmpty() || artistResults.isNotEmpty()
    val hasSearched = searchQuery.isNotBlank()

    // Accent color matching the app theme
    val accentColor = Color(0xFFE57373) // Salmon/Red

    // Edge-to-edge layout: No statusBarsPadding on outer Column
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header with gradient extending to the top edge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .statusBarsPadding() // Apply padding here to avoid notch
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 12.dp)
        ) {
            Column {
                Text(
                    text = "Search",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Find your favorite songs, albums & artists",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Search Bar with modern styling
                TextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        viewModel.searchLibrary(it)
                    },
                    placeholder = { 
                        Text(
                            "What do you want to listen to?",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        ) 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .focusRequester(focusRequester),
                    leadingIcon = { 
                        Icon(
                            Icons.Default.Search, 
                            contentDescription = null,
                            tint = if (searchQuery.isNotEmpty()) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                        ) 
                    },
                    trailingIcon = {
                        AnimatedVisibility(
                            visible = searchQuery.isNotEmpty(),
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut()
                        ) {
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
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = accentColor
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    singleLine = true
                )
            }
        }

        // Results or Empty State
        AnimatedContent(
            targetState = when {
                isLoading && hasSearched -> "loading"
                hasResults -> "results"
                hasSearched && !hasResults && error != null -> "error"
                hasSearched && !hasResults -> "empty"
                else -> "initial"
            },
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(200))
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
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Column {
                                SectionHeader("Songs")
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(4) { SkeletonSquareCard() }
                                }
                            }
                        }
                        item {
                            Column {
                                SectionHeader("Albums")
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(3) { SkeletonAlbumCard() }
                                }
                            }
                        }
                        item {
                            Column {
                                SectionHeader("Artists")
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Songs Section
                        if (songResults.isNotEmpty()) {
                            item {
                                SearchAnimatedSection(visible = true, delayMillis = 0) {
                                    Column {
                                        SectionHeader("Songs")
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                        }
                        
                        // Albums Section
                        if (albumResults.isNotEmpty()) {
                            item {
                                SearchAnimatedSection(visible = true, delayMillis = 100) {
                                    Column {
                                        SectionHeader("Albums")
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            items(albumResults) { album ->
                                                AlbumCard(
                                                    albumName = album.name,
                                                    coverUrl = album.coverUrl,
                                                    onClick = { onAlbumSelected(album.name) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Artists Section
                        if (artistResults.isNotEmpty()) {
                            item {
                                SearchAnimatedSection(visible = true, delayMillis = 200) {
                                    Column {
                                        SectionHeader("Artists")
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            items(artistResults) { artist ->
                                                ArtistPill(
                                                    artistName = artist.name,
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
                }
                
                "empty" -> {
                    EmptySearchState(query = searchQuery, accentColor = accentColor)
                }
                
                "error" -> {
                    ErrorSearchState(error = error ?: "Something went wrong", accentColor = accentColor)
                }
                
                else -> {
                    InitialSearchState(accentColor = accentColor)
                }
            }
        }
    }
}

@Composable
private fun InitialSearchState(accentColor: Color) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(48.dp)
        ) {
            // Animated search icon
            val infiniteTransition = rememberInfiniteTransition(label = "searchIconAnim")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = EaseInOutCubic),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "searchIconScale"
            )
            
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale },
                tint = accentColor.copy(alpha = 0.6f)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "Discover Music",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Search for songs, artists or albums",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmptySearchState(query: String, accentColor: Color) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(48.dp)
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
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Try different keywords or check spelling",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ErrorSearchState(error: String, accentColor: Color) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(48.dp)
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

@Composable
private fun SearchAnimatedSection(
    visible: Boolean,
    delayMillis: Int,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 400, delayMillis = delayMillis)
        ) + slideInVertically(
            initialOffsetY = { 40 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    ) {
        content()
    }
}

private val EaseInOutCubic = CubicBezierEasing(0.645f, 0.045f, 0.355f, 1f)
