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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.memesta.vibeon.data.TrackInfo
import moe.memesta.vibeon.ui.components.AlbumCard
import moe.memesta.vibeon.ui.components.ArtistPill
import moe.memesta.vibeon.ui.components.VibeContainedLoadingIndicator
import moe.memesta.vibeon.ui.components.SectionHeader
import moe.memesta.vibeon.ui.components.SquareTrackCard
import moe.memesta.vibeon.ui.theme.Dimens
import moe.memesta.vibeon.ui.utils.LocalDisplayLanguage
import moe.memesta.vibeon.ui.utils.getDisplayName

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

    // --- Spring slide-up animation ---
    val slideOffset = remember { Animatable(1f) }   // 1 = off-screen bottom, 0 = resting
    val scrimAlpha  = remember { Animatable(0f) }

    val scope = rememberCoroutineScope()

    // Ensure dialog window is transparent to avoid "purple bar" (primary color) leaks
    val currentView = LocalView.current
    androidx.compose.runtime.SideEffect {
        val window = (currentView.parent as? DialogWindowProvider)?.window
        window?.let {
            it.statusBarColor = android.graphics.Color.TRANSPARENT
            it.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(it, currentView).isAppearanceLightStatusBars = false
        }
    }

    // Enter animation
    LaunchedEffect(Unit) {
        launch {
            slideOffset.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.68f,       // slightly bouncy
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            scrimAlpha.animateTo(1f, tween(320))
        }
        delay(80) // small delay so the sheet is partially visible before keyboard opens
        focusRequester.requestFocus()
    }

    // Animated dismiss helper
    val animatedClose: () -> Unit = remember(onClose) {
        {
            scope.launch {
                launch {
                    slideOffset.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(260, easing = FastOutSlowInEasing)
                    )
                }
                launch { scrimAlpha.animateTo(0f, tween(220)) }
                delay(270)
                onClose()
            }
        }
    }

    val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    // Scrim + sheet container
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.36f * scrimAlpha.value))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = animatedClose
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        // The popup sheet
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .graphicsLayer {
                    translationY = slideOffset.value * size.height
                }
                .shadow(
                    elevation = 24.dp,
                    shape = sheetShape,
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.2f),
                    spotColor = Color.Black.copy(alpha = 0.3f)
                )
                .clip(sheetShape)
                .background(MaterialTheme.colorScheme.surface)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { } // consume clicks so they don't dismiss
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Drag-handle pill
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)
                            )
                    )
                }

                // Search field
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
                                tint = if (searchQuery.isNotEmpty()) accentColor
                                       else MaterialTheme.colorScheme.onSurfaceVariant
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

                // Results area
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
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(bottom = contentPadding.calculateBottomPadding() + 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    VibeContainedLoadingIndicator(label = "Searching your library...")
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
