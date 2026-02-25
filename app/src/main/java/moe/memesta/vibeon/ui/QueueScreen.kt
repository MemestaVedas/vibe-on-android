package moe.memesta.vibeon.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.memesta.vibeon.data.QueueItem
import moe.memesta.vibeon.ui.theme.Dimens
import moe.memesta.vibeon.ui.utils.LocalDisplayLanguage
import moe.memesta.vibeon.ui.utils.getDisplayArtist
import moe.memesta.vibeon.ui.utils.getDisplayName
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QueueScreen(
    viewModel: ConnectionViewModel,
    modifier: Modifier = Modifier,
    showCloseButton: Boolean = false,
    onClose: (() -> Unit)? = null
) {
    val queue by viewModel.queue.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isShuffled by viewModel.isShuffled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()

    var localQueue by remember { mutableStateOf(queue) }
    var isReordering by remember { mutableStateOf(false) }

    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var itemHeightPx by remember { mutableFloatStateOf(0f) }

    val scope = rememberCoroutineScope()
    var undoSnapshot by remember { mutableStateOf<List<QueueItem>?>(null) }
    var showUndo by remember { mutableStateOf(false) }
    var undoJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(queue) {
        if (!isReordering) {
            localQueue = queue
        }
    }

    fun commitQueue(newQueue: List<QueueItem>) {
        localQueue = newQueue
        viewModel.setQueue(newQueue.map { it.path })
    }

    fun removeAt(index: Int) {
        if (index <= currentIndex || index !in localQueue.indices) return
        val snapshot = localQueue
        val newQueue = localQueue.toMutableList().apply { removeAt(index) }
        commitQueue(newQueue)

        undoSnapshot = snapshot
        showUndo = true
        undoJob?.cancel()
        undoJob = scope.launch {
            delay(3500)
            showUndo = false
            undoSnapshot = null
        }
    }

    fun startDrag(index: Int) {
        if (index <= currentIndex || index !in localQueue.indices) return
        draggingIndex = index
        dragOffsetY = 0f
        isReordering = true
    }

    fun updateDrag(deltaY: Float) {
        if (draggingIndex !in localQueue.indices || itemHeightPx <= 0f) return
        dragOffsetY += deltaY
        val shift = (dragOffsetY / itemHeightPx).roundToInt()
        if (shift == 0) return

        val fromIndex = draggingIndex
        val toIndex = (fromIndex + shift).coerceIn(currentIndex + 1, localQueue.lastIndex)
        if (toIndex == fromIndex) return

        val updatedQueue = localQueue.toMutableList()
        val movedItem = updatedQueue.removeAt(fromIndex)
        updatedQueue.add(toIndex, movedItem)
        localQueue = updatedQueue
        draggingIndex = toIndex
        dragOffsetY -= (toIndex - fromIndex) * itemHeightPx
    }

    fun endDrag() {
        if (draggingIndex != -1) {
            val updatedPaths = localQueue.map { it.path }
            val currentPaths = queue.map { it.path }
            if (updatedPaths != currentPaths) {
                viewModel.setQueue(updatedPaths)
            }
        }
        draggingIndex = -1
        dragOffsetY = 0f
        isReordering = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = Dimens.ScreenPadding)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimens.SectionSpacing, bottom = Dimens.ItemSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Queue",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Up Next",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (showCloseButton && onClose != null) {
                IconButton(
                    onClick = onClose,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close queue")
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QueueActionButton(
                active = isShuffled,
                onClick = { viewModel.toggleShuffle() }
            ) {
                Icon(
                    Icons.Rounded.Shuffle,
                    contentDescription = "Shuffle",
                    modifier = Modifier.size(20.dp)
                )
            }
            QueueActionButton(
                active = repeatMode != "off",
                onClick = { viewModel.toggleRepeat() }
            ) {
                Icon(
                    if (repeatMode == "one") Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    contentDescription = "Repeat",
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            QueueActionButton(
                active = false,
                onClick = { commitQueue(emptyList()) }
            ) {
                Icon(
                    Icons.Rounded.ClearAll,
                    contentDescription = "Clear queue",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (localQueue.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Queue is empty",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(
                        localQueue,
                        key = { index, item -> "${item.path}-$index" }
                    ) { index, item ->
                        val isCurrent = index == currentIndex
                        val canReorder = index > currentIndex
                        val isDraggingItem = index == draggingIndex

                        QueueRow(
                            item = item,
                            isCurrent = isCurrent,
                            isPlaying = isPlaying,
                            canRemove = index > currentIndex,
                            canReorder = canReorder,
                            isDragging = isDraggingItem,
                            dragOffsetY = if (isDraggingItem) dragOffsetY else 0f,
                            onPlay = { viewModel.playTrack(item.path) },
                            onRemove = { removeAt(index) },
                            onDragStart = { startDrag(index) },
                            onDrag = { updateDrag(it) },
                            onDragEnd = { endDrag() },
                            onItemMeasured = { height -> if (itemHeightPx == 0f) itemHeightPx = height },
                            modifier = Modifier.animateItemPlacement()
                        )
                    }
                }

                if (showUndo && undoSnapshot != null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.inverseSurface,
                        tonalElevation = 6.dp,
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Track removed",
                                color = MaterialTheme.colorScheme.inverseOnSurface,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Undo",
                                color = MaterialTheme.colorScheme.inversePrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.inverseSurface)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                    .pointerInput(Unit) {
                                        detectTapGestures {
                                            val snapshot = undoSnapshot ?: return@detectTapGestures
                                            undoJob?.cancel()
                                            commitQueue(snapshot)
                                            showUndo = false
                                            undoSnapshot = null
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueActionButton(
    active: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        contentColor = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            content()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QueueRow(
    item: QueueItem,
    isCurrent: Boolean,
    isPlaying: Boolean,
    canRemove: Boolean,
    canReorder: Boolean,
    isDragging: Boolean,
    dragOffsetY: Float,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onItemMeasured: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayLanguage = LocalDisplayLanguage.current
    val title = item.getDisplayName(displayLanguage)
    val artist = item.getDisplayArtist(displayLanguage)

    val density = LocalDensity.current
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    val maxSwipe = 96.dp
    val maxSwipePx = with(density) { maxSwipe.toPx() }

    val backgroundColor by animateColorAsState(
        targetValue = if (isCurrent) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = tween(180),
        label = "queueRowBackground"
    )
    val contentColor = if (isCurrent) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface
    val subTextColor = if (isCurrent) MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { size -> onItemMeasured(size.height.toFloat()) }
    ) {
        if (swipeOffset < 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier
                        .padding(end = 20.dp)
                        .size(20.dp)
                )
            }
        }

        Surface(
            modifier = Modifier
                .graphicsLayer {
                    translationX = swipeOffset
                    if (isDragging) {
                        translationY = dragOffsetY
                        scaleX = 1.02f
                        scaleY = 1.02f
                    }
                }
                .clip(RoundedCornerShape(if (isCurrent) 28.dp else 20.dp))
                .clickable(enabled = swipeOffset == 0f) { onPlay() }
                .pointerInput(canRemove, isDragging) {
                    if (!canRemove || isDragging) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (swipeOffset <= -maxSwipePx * 0.6f) {
                                onRemove()
                            }
                            swipeOffset = 0f
                        },
                        onDragCancel = { swipeOffset = 0f }
                    ) { change, dragAmount ->
                        change.consume()
                        swipeOffset = (swipeOffset + dragAmount).coerceIn(-maxSwipePx, 0f)
                    }
                },
            color = backgroundColor,
            tonalElevation = if (isDragging) 4.dp else 1.dp,
            shadowElevation = if (isDragging) 4.dp else 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isCurrent) {
                    Icon(
                        Icons.Rounded.GraphicEq,
                        contentDescription = if (isPlaying) "Playing" else "Paused",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Spacer(modifier = Modifier.width(26.dp))
                }

                AsyncImage(
                    model = item.coverUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = contentColor,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = artist,
                        color = subTextColor,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (canReorder) {
                    IconButton(
                        onClick = {},
                        modifier = Modifier
                            .size(36.dp)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { onDragStart() },
                                    onDragEnd = { onDragEnd() },
                                    onDragCancel = { onDragEnd() }
                                ) { change, dragAmount ->
                                    change.consume()
                                    onDrag(dragAmount.y)
                                }
                            }
                    ) {
                        Icon(
                            Icons.Rounded.DragIndicator,
                            contentDescription = "Reorder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (canRemove) {
                    IconButton(
                        onClick = onRemove,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Remove")
                    }
                }
            }
        }
    }
}
