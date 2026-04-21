@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.toShape
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt
import moe.memesta.vibeon.data.TrackInfo
import moe.memesta.vibeon.ui.components.AlbumCard
import moe.memesta.vibeon.ui.components.ArtistPill
import moe.memesta.vibeon.ui.components.VibeContainedLoadingIndicator
import moe.memesta.vibeon.ui.components.SectionHeader
import moe.memesta.vibeon.ui.components.SquareTrackCard
import moe.memesta.vibeon.ui.theme.Dimens
import moe.memesta.vibeon.ui.theme.bouncyClickable
import moe.memesta.vibeon.ui.shapes.SheetShape
import moe.memesta.vibeon.ui.utils.LocalDisplayLanguage
import moe.memesta.vibeon.ui.utils.getDisplayName

@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
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
    val viewModelQuery by viewModel.searchQuery.collectAsState()
    
    // FIX: Using TextFieldValue for stable cursor
    var searchQuery by rememberSaveable(stateSaver = TextFieldValue.Saver) { 
        mutableStateOf(TextFieldValue(viewModelQuery)) 
    }
    
    // Track active state for interactions
    var isVisible by rememberSaveable { mutableStateOf(false) }
    
    // Focus requester for the search input
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        isVisible = true
        delay(150)
        focusRequester.requestFocus()
    }

    // Sync local state when viewModel query changes
    LaunchedEffect(viewModelQuery) {
        if (searchQuery.text != viewModelQuery) {
            searchQuery = searchQuery.copy(text = viewModelQuery)
        }
    }

    val songResults by viewModel.songResults.collectAsState()
    val albumResults by viewModel.albumResults.collectAsState()
    val artistResults by viewModel.artistResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val displayLanguage = LocalDisplayLanguage.current

    val hasResults = songResults.isNotEmpty() || albumResults.isNotEmpty() || artistResults.isNotEmpty()
    val hasSearched = searchQuery.text.isNotBlank()

    val accentColor = MaterialTheme.colorScheme.primary

    // Animation values for the bottom sheet
    val sheetSlideY by animateDpAsState(
        targetValue = if (isVisible) 0.dp else 400.dp,
        animationSpec = tween(durationMillis = 350, easing = CubicBezierEasing(0.1f, 0.9f, 0.2f, 1f)),
        label = "sheetSlide"
    )
    
    val bgAlpha by animateFloatAsState(
        targetValue = if (isVisible) 0.6f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "bgAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = bgAlpha))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { 
                    isVisible = false
                    // Simple delay to let animation finish
                    onClose()
                }
            )
    ) {
        // Arc-Style Container
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .offset(y = sheetSlideY)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(Color(0xFF1C1C1E)) // Dark Arc-style background
                .clickable(enabled = true, onClick = {}) // Consume clicks
                .padding(top = 16.dp, bottom = 24.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Drag Handle / Top Indicator (Subtle)
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.3f))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // The Rounded Search Bar (TOP of the sheet)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        viewModel.searchLibrary(it.text)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .focusRequester(focusRequester),
                    placeholder = { 
                        Text(
                            "Search...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray.copy(alpha = 0.7f)
                        ) 
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = if (hasSearched) accentColor else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    trailingIcon = {
                        if (hasSearched) {
                            IconButton(onClick = {
                                searchQuery = TextFieldValue("")
                                viewModel.searchLibrary("")
                            }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = Color.Gray
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(30.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF2C2C2E),
                        unfocusedContainerColor = Color(0xFF2C2C2E),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = accentColor,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Results area (BELOW the search bar)
            AnimatedVisibility(
                visible = hasSearched || isLoading,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp)
                ) {
                    ResultsContent(
                        state = when {
                            isLoading -> "loading"
                            hasResults -> "results"
                            error != null -> "error"
                            else -> "empty"
                        },
                        searchQuery = searchQuery.text,
                        songResults = songResults,
                        albumResults = albumResults,
                        artistResults = artistResults,
                        error = error,
                        displayLanguage = displayLanguage,
                        onTrackSelected = onTrackSelected,
                        onAlbumSelected = onAlbumSelected,
                        onArtistSelected = onArtistSelected,
                        viewModel = viewModel,
                        contentPadding = contentPadding
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ResultsContent(
    state: String,
    searchQuery: String,
    songResults: List<TrackInfo>,
    albumResults: List<moe.memesta.vibeon.data.AlbumInfo>,
    artistResults: List<moe.memesta.vibeon.data.ArtistItemData>,
    error: String?,
    displayLanguage: moe.memesta.vibeon.data.local.DisplayLanguage,
    onTrackSelected: (TrackInfo) -> Unit,
    onAlbumSelected: (String) -> Unit,
    onArtistSelected: (String) -> Unit,
    viewModel: LibraryViewModel,
    contentPadding: PaddingValues
) {
    Crossfade(targetState = state, label = "resultsTransition") { s ->
        when (s) {
            "loading" -> {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    VibeContainedLoadingIndicator(label = "Thinking...")
                }
            }
            "results" -> {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 16.dp, top = 8.dp),
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
                                    items(items = songResults.take(6), key = { it.path }) { track ->
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
                                    items(items = albumResults, key = { it.name }) { album ->
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
                                    items(items = artistResults, key = { it.name }) { artist ->
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
            "empty" -> EmptySearchState(query = searchQuery)
            "error" -> ErrorSearchState(error = error ?: "Unknown error")
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
