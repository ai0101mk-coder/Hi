package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.model.FileItem
import com.example.ui.theme.ArchivesCyan
import com.example.ui.theme.ImagesIndigo
import com.example.ui.theme.LocalGlassTokens
import com.example.ui.theme.MusicCoral
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

enum class SwipeSettleTarget {
    COLLAPSED,
    REVEALED_RIGHT,
    TRIGGER_DELETE
}

object SwipeActionMath {
    fun clampSwipeOffset(
        candidateOffsetPx: Float,
        maxLeftSwipePx: Float,
        maxRightOpenPx: Float
    ): Float {
        val rubberBandLimit = maxLeftSwipePx * 1.25f
        return when {
            candidateOffsetPx > maxRightOpenPx -> {
                val excess = candidateOffsetPx - maxRightOpenPx
                maxRightOpenPx + (excess * 0.25f).coerceAtMost(maxRightOpenPx * 0.25f)
            }
            candidateOffsetPx < -maxLeftSwipePx -> {
                val excess = candidateOffsetPx + maxLeftSwipePx
                (-maxLeftSwipePx + excess * 0.25f).coerceAtLeast(-rubberBandLimit)
            }
            else -> candidateOffsetPx
        }
    }

    fun resolveSettleTarget(
        offsetPx: Float,
        velocityPxPerSec: Float,
        rightRevealWidthPx: Float,
        deleteTriggerPx: Float,
        velocityThresholdPxPerSec: Float = 900f
    ): SwipeSettleTarget {
        return when {
            offsetPx <= -deleteTriggerPx ||
                (velocityPxPerSec <= -velocityThresholdPxPerSec && offsetPx <= -deleteTriggerPx * 0.35f) -> {
                SwipeSettleTarget.TRIGGER_DELETE
            }
            offsetPx >= rightRevealWidthPx * 0.45f ||
                (velocityPxPerSec >= velocityThresholdPxPerSec && offsetPx > 12f) -> {
                SwipeSettleTarget.REVEALED_RIGHT
            }
            else -> SwipeSettleTarget.COLLAPSED
        }
    }
}

@Composable
fun SwipeableFileListRow(
    item: FileItem,
    enabled: Boolean,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    isRevealed: Boolean,
    onRevealChange: (Boolean) -> Unit,
    onSwipeStart: () -> Unit = {},
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit,
    onSwipeMove: () -> Unit,
    onSwipeShare: () -> Unit,
    onSwipeDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val tokens = LocalGlassTokens.current

    val showShareButton = !item.isDirectory
    val rightRevealWidthDp = if (showShareButton) 148.dp else 82.dp
    val rightRevealWidthPx = with(density) { rightRevealWidthDp.toPx() }
    val deleteTriggerPx = with(density) { 92.dp.toPx() }
    val maxLeftSwipePx = with(density) { 128.dp.toPx() }
    val velocityThresholdPx = with(density) { 420.dp.toPx() }
    val fadeDistancePx = with(density) { 28.dp.toPx() }

    val offsetX = remember(item.path) { Animatable(0f) }
    var deleteHapticFired by remember(item.path) { mutableStateOf(false) }

    LaunchedEffect(isRevealed, enabled, rightRevealWidthPx) {
        val target = if (enabled && isRevealed) rightRevealWidthPx else 0f
        if (abs(offsetX.value - target) > 0.5f) {
            offsetX.animateTo(
                targetValue = target,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    val shape = RoundedCornerShape(18.dp)
    val currentOffset = offsetX.value
    val backdropAlpha = (abs(currentOffset) / fadeDistancePx).coerceIn(0f, 1f)
    val pastDeleteThreshold = -currentOffset >= deleteTriggerPx

    val deleteIconScale by animateFloatAsState(
        targetValue = if (pastDeleteThreshold) 1.18f else 0.96f,
        animationSpec = tween(140),
        label = "swipe_delete_icon_scale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(shape)
            .testTag("swipe_action_row_${item.name}")
    ) {
        // Revealed right-swipe tray (Move + Share for files, Move for folders)
        if (currentOffset > 0.5f) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = backdropAlpha }
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                ImagesIndigo.copy(alpha = if (tokens.isDark) 0.28f else 0.16f),
                                ArchivesCyan.copy(alpha = if (tokens.isDark) 0.14f else 0.08f)
                            )
                        )
                    )
                    .border(1.dp, tokens.glassBorderStart, shape)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                SwipeTrayActionButton(
                    label = "Move",
                    icon = Icons.AutoMirrored.Rounded.DriveFileMove,
                    accentColor = ImagesIndigo,
                    testTag = "swipe_move_button_${item.name}",
                    onClick = {
                        onRevealChange(false)
                        scope.launch {
                            offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                        }
                        onSwipeMove()
                    }
                )
                if (showShareButton) {
                    Spacer(modifier = Modifier.width(6.dp))
                    SwipeTrayActionButton(
                        label = "Share",
                        icon = Icons.Rounded.Share,
                        accentColor = ArchivesCyan,
                        testTag = "swipe_share_button_${item.name}",
                        onClick = {
                            onRevealChange(false)
                            scope.launch {
                                offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                            }
                            onSwipeShare()
                        }
                    )
                }
            }
        }

        // Revealed left-swipe Delete backdrop
        if (currentOffset < -0.5f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = backdropAlpha }
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MusicCoral.copy(alpha = if (tokens.isDark) 0.18f else 0.10f),
                                MusicCoral.copy(
                                    alpha = if (pastDeleteThreshold) {
                                        if (tokens.isDark) 0.52f else 0.34f
                                    } else {
                                        if (tokens.isDark) 0.32f else 0.20f
                                    }
                                )
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = MusicCoral.copy(alpha = if (pastDeleteThreshold) 0.7f else 0.35f),
                        shape = shape
                    )
                    .padding(horizontal = 20.dp)
                    .testTag("swipe_delete_backdrop_${item.name}"),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.graphicsLayer {
                        scaleX = deleteIconScale
                        scaleY = deleteIconScale
                    }
                ) {
                    Text(
                        text = if (pastDeleteThreshold) "Release to delete" else "Delete",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MusicCoral
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete ${item.name}",
                        tint = MusicCoral,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Foreground File Row
        FileListRow(
            item = item,
            isSelectionMode = isSelectionMode,
            isSelected = isSelected,
            onClick = {
                if (isRevealed || abs(offsetX.value) > 4f) {
                    onRevealChange(false)
                    scope.launch {
                        offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                    }
                } else {
                    onClick()
                }
            },
            onLongClick = {
                if (isRevealed) {
                    onRevealChange(false)
                }
                onLongClick()
            },
            onMoreClick = {
                if (isRevealed) {
                    onRevealChange(false)
                } else {
                    onMoreClick()
                }
            },
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .draggable(
                    enabled = enabled,
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        val next = SwipeActionMath.clampSwipeOffset(
                            candidateOffsetPx = offsetX.value + delta,
                            maxLeftSwipePx = maxLeftSwipePx,
                            maxRightOpenPx = rightRevealWidthPx
                        )
                        val crossedDelete = -next >= deleteTriggerPx
                        if (crossedDelete && !deleteHapticFired) {
                            deleteHapticFired = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        } else if (!crossedDelete) {
                            deleteHapticFired = false
                        }
                        scope.launch {
                            offsetX.snapTo(next)
                        }
                    },
                    onDragStarted = {
                        onSwipeStart()
                        deleteHapticFired = -offsetX.value >= deleteTriggerPx
                    },
                    onDragStopped = { velocity ->
                        val target = SwipeActionMath.resolveSettleTarget(
                            offsetPx = offsetX.value,
                            velocityPxPerSec = velocity,
                            rightRevealWidthPx = rightRevealWidthPx,
                            deleteTriggerPx = deleteTriggerPx,
                            velocityThresholdPxPerSec = velocityThresholdPx
                        )
                        when (target) {
                            SwipeSettleTarget.TRIGGER_DELETE -> {
                                onRevealChange(false)
                                scope.launch {
                                    offsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                }
                                onSwipeDelete()
                            }
                            SwipeSettleTarget.REVEALED_RIGHT -> {
                                onRevealChange(true)
                                scope.launch {
                                    offsetX.animateTo(
                                        targetValue = rightRevealWidthPx,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                            }
                            SwipeSettleTarget.COLLAPSED -> {
                                onRevealChange(false)
                                scope.launch {
                                    offsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                }
                            }
                        }
                    }
                )
        )
    }
}

@Composable
private fun SwipeTrayActionButton(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    val buttonShape = RoundedCornerShape(14.dp)

    Column(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .width(62.dp)
            .fillMaxHeight()
            .clip(buttonShape)
            .background(
                if (tokens.isDark) {
                    accentColor.copy(alpha = 0.24f)
                } else {
                    Color.White.copy(alpha = 0.72f)
                }
            )
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.45f),
                shape = buttonShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = accentColor),
                onClick = onClick
            )
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = accentColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = accentColor,
            maxLines = 1
        )
    }
}
