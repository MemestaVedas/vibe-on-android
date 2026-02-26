package moe.memesta.vibeon.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import moe.memesta.vibeon.data.TrackInfo
import moe.memesta.vibeon.ui.theme.bouncyClickable
import moe.memesta.vibeon.ui.theme.Dimens
import moe.memesta.vibeon.ui.theme.VibeBackground
import moe.memesta.vibeon.ui.utils.LocalDisplayLanguage
import moe.memesta.vibeon.ui.utils.getDisplayArtist
import moe.memesta.vibeon.ui.utils.getDisplayName

enum class PlaylistShape {
    Circle, Rounded, Square
}

data class PlaylistCustomization(
    val type: PlaylistCustomizationType = PlaylistCustomizationType.Default,
    val color: Int? = null,
    val imageUri: Uri? = null,
    val iconName: String = "MusicNote"
)

enum class PlaylistCustomizationType {
    Default, Image, Icon
}

@Composable
fun PlaylistCreationWizard(
    songs: List<TrackInfo>,
    onCreatePlaylist: (name: String, songs: List<String>, customization: PlaylistCustomization) -> Unit,
    onDismiss: () -> Unit,
    contentPadding: PaddingValues = PaddingValues()
) {
    var currentStep by remember { mutableStateOf(0) }
    var playlistName by remember { mutableStateOf("") }
    var selectedSongPaths by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var customization by remember { mutableStateOf(PlaylistCustomization()) }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            customization = customization.copy(
                type = PlaylistCustomizationType.Image,
                imageUri = it
            )
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    AnimatedContent(
                        targetState = currentStep,
                        label = "Title Animation",
                        transitionSpec = {
                            slideInHorizontally { width -> width } + fadeIn() togetherWith
                                    slideOutHorizontally { width -> -width } + fadeOut()
                        }
                    ) { step ->
                        Text(
                            if (step == 0) "New Playlist" else "Add Songs",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep == 1) {
                            currentStep = 0
                        } else {
                            onDismiss()
                        }
                    }) {
                        Icon(
                            if (currentStep == 1) Icons.AutoMirrored.Rounded.ArrowBack else Icons.Rounded.Close,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(if (currentStep == 0) "Next" else "Create") },
                icon = {
                    Icon(
                        if (currentStep == 0) Icons.AutoMirrored.Rounded.ArrowForward else Icons.Rounded.Check,
                        contentDescription = null
                    )
                },
                onClick = {
                    if (currentStep == 0) {
                        if (playlistName.isNotBlank()) {
                            currentStep = 1
                        }
                    } else {
                        val selectedPaths = selectedSongPaths.filter { it.value }.keys.toList()
                        onCreatePlaylist(playlistName, selectedPaths, customization)
                    }
                },
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = contentPadding.calculateBottomPadding() + 96.dp
                ),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        }
    ) { padding ->
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                } else {
                    slideInHorizontally { width -> -width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> width } + fadeOut()
                }
            },
            modifier = Modifier.padding(padding),
            label = "Step Transition"
        ) { step ->
            when (step) {
                0 -> {
                    PlaylistNameAndAppearanceStep(
                        playlistName = playlistName,
                        onNameChange = { playlistName = it },
                        customization = customization,
                        onCustomizationChange = { customization = it },
                        onImagePicked = { imagePickerLauncher.launch("image/*") },
                        contentPadding = contentPadding
                    )
                }
                1 -> {
                    PlaylistSongSelectionStep(
                        songs = songs,
                        selectedSongPaths = selectedSongPaths,
                        onSelectionChange = { path, selected ->
                            selectedSongPaths = selectedSongPaths.toMutableMap().apply { this[path] = selected }
                        },
                        contentPadding = contentPadding
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistNameAndAppearanceStep(
    playlistName: String,
    onNameChange: (String) -> Unit,
    customization: PlaylistCustomization,
    onCustomizationChange: (PlaylistCustomization) -> Unit,
    onImagePicked: () -> Unit,
    contentPadding: PaddingValues = PaddingValues()
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VibeBackground)
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 120.dp)
    ) {
        // Playlist Name Input
        item {
            Text(
                "Playlist Name",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = playlistName,
                onValueChange = onNameChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                placeholder = { Text("Enter playlist name") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Appearance Section
        item {
            Text(
                "Appearance",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        // Tabs: Default, Image, Icon
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Default", "Image", "Icon").forEachIndexed { index, label ->
                    FilterChip(
                        selected = (customization.type.ordinal == index),
                        onClick = {
                            onCustomizationChange(
                                customization.copy(
                                    type = PlaylistCustomizationType.values()[index]
                                )
                            )
                        },
                        label = { Text(label) },
                        modifier = Modifier.height(40.dp)
                    )
                }
            }
        }

        // Content based on selected tab
        when (customization.type) {
            PlaylistCustomizationType.Default -> {
                item {
                    PlaylistColorPalette(
                        selectedColor = customization.color,
                        onColorSelect = { color ->
                            onCustomizationChange(customization.copy(color = color))
                        }
                    )
                }
            }
            PlaylistCustomizationType.Image -> {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (customization.imageUri != null) {
                            AsyncImage(
                                model = customization.imageUri,
                                contentDescription = "Selected image",
                                modifier = Modifier
                                    .size(150.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onImagePicked() },
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        Button(
                            onClick = { onImagePicked() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (customization.imageUri != null) "Change Image" else "Select Image")
                        }
                    }
                }
            }
            PlaylistCustomizationType.Icon -> {
                item {
                    PlaylistIconSelector(
                        selectedIcon = customization.iconName,
                        selectedColor = customization.color,
                        onIconSelect = { icon ->
                            onCustomizationChange(customization.copy(iconName = icon))
                        },
                        onColorSelect = { color ->
                            onCustomizationChange(customization.copy(color = color))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistColorPalette(
    selectedColor: Int?,
    onColorSelect: (Int) -> Unit
) {
    val colors = listOf(
        Color(0xFFE8B4F5), Color(0xFFB39DDB), Color(0xFF9FA8DA), Color(0xFF90CAF9),
        Color(0xFF81D4FA), Color(0xFF80DEEA), Color(0xFF80CBC4), Color(0xFFA5D6A7),
        Color(0xFFC8E6C9), Color(0xFFDCEDC8), Color(0xFFFFF9C4), Color(0xFFFFE0B2),
        Color(0xFFFFCC80), Color(0xFFFFAB91), Color(0xFFEF9A9A), Color(0xFFF8BBD0)
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Background Color", style = MaterialTheme.typography.labelMedium)
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            colors.forEach { color ->
                val isSelected = selectedColor == color.toArgb()
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(color)
                        .border(
                            width = if (isSelected) 3.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onColorSelect(color.toArgb()) }
                )
            }
        }
    }
}

@Composable
fun PlaylistIconSelector(
    selectedIcon: String,
    selectedColor: Int?,
    onIconSelect: (String) -> Unit,
    onColorSelect: (Int) -> Unit
) {
    val icons = listOf(
        "MusicNote" to Icons.Rounded.MusicNote,
        "Headphones" to Icons.Rounded.Headphones,
        "Favorite" to Icons.Rounded.Favorite,
        "Piano" to Icons.Rounded.Piano,
        "Speaker" to Icons.Rounded.Speaker,
        "Album" to Icons.Rounded.Album,
        "GraphicEq" to Icons.Rounded.GraphicEq,
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Color Selection
        PlaylistColorPalette(
            selectedColor = selectedColor,
            onColorSelect = onColorSelect
        )

        // Icon Selection
        Column {
            Text("Icon", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                icons.forEach { (iconName, icon) ->
                    val isSelected = selectedIcon == iconName
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { onIconSelect(iconName) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = iconName,
                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistSongSelectionStep(
    songs: List<TrackInfo>,
    selectedSongPaths: Map<String, Boolean>,
    onSelectionChange: (String, Boolean) -> Unit,
    contentPadding: PaddingValues = PaddingValues()
) {
    var searchQuery by remember { mutableStateOf("") }

    val displayLanguage = LocalDisplayLanguage.current

    val filteredSongs = remember(displayLanguage, searchQuery, songs) {
        if (searchQuery.isBlank()) songs
        else songs.filter {
            it.getDisplayName(displayLanguage).contains(searchQuery, ignoreCase = true) ||
                    it.getDisplayArtist(displayLanguage).contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VibeBackground)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.ScreenPadding),
            label = { Text("Search songs") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Rounded.Clear, null) } }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(
                top = Dimens.ItemSpacing,
                bottom = contentPadding.calculateBottomPadding() + 120.dp
            )
        ) {
            items(filteredSongs) { song ->
                val isSelected = selectedSongPaths[song.path] ?: false
                SongSelectionItem(
                    song = song,
                    isSelected = isSelected,
                    onSelectionChange = { selected ->
                        onSelectionChange(song.path, selected)
                    }
                )
            }
        }
    }
}

@Composable
fun SongSelectionItem(
    song: TrackInfo,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit
) {
    val displayLanguage = LocalDisplayLanguage.current
    val title = song.getDisplayName(displayLanguage)
    val artist = song.getDisplayArtist(displayLanguage)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .bouncyClickable(onClick = { onSelectionChange(!isSelected) })
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = onSelectionChange,
            modifier = Modifier.size(24.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
