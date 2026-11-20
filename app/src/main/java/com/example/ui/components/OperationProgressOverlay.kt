package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PauseCircleOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.FormatUtils
import com.example.model.FileOperationType
import com.example.model.OperationPhase
import com.example.model.OperationProgress
import com.example.ui.theme.ApksEmerald
import com.example.ui.theme.DocsAmber
import com.example.ui.theme.LocalGlassTokens
import com.example.ui.theme.MusicCoral

@Composable
fun FloatingOperationProgressCard(
    progress: OperationProgress?,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var cachedProgress by remember { mutableStateOf(progress) }
    if (progress != null) {
        cachedProgress = progress
    }

    var isExpanded by remember { mutableStateOf(true) }
    LaunchedEffect(progress?.id) {
        if (progress != null) {
            isExpanded = true
        }
    }

    AnimatedVisibility(
        visible = progress != null,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
        ) + fadeIn(tween(220)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
        ) + fadeOut(tween(180)),
        modifier = modifier
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        val display = progress ?: cachedProgress ?: return@AnimatedVisibility
        OperationProgressCardContent(
            progress = display,
            isExpanded = isExpanded,
            onToggleExpand = { isExpanded = !isExpanded },
            onCancel = onCancel,
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun OperationProgressCardContent(
    progress: OperationProgress,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    val shape = RoundedCornerShape(24.dp)

    val targetAccent = when (progress.phase) {
        OperationPhase.RUNNING -> progress.operationType.accentColor
        OperationPhase.COMPLETED -> ApksEmerald
        OperationPhase.CANCELLED -> DocsAmber
        OperationPhase.FAILED -> MusicCoral
    }
    val accentColor by animateColorAsState(
        targetValue = targetAccent,
        animationSpec = tween(250),
        label = "op_accent"
    )
    val animatedFraction by androidx.compose.animation.core.animateFloatAsState(
        targetValue = progress.progressFraction,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "op_fraction"
    )

    val isCancelling = progress.phase == OperationPhase.RUNNING &&
        progress.statusDetail.equals("Cancelling…", ignoreCase = true)

    Column(
        modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .shadow(
                elevation = 20.dp,
                shape = shape,
                ambientColor = tokens.glassShadow,
                spotColor = tokens.glassShadow
            )
            .clip(shape)
            .background(tokens.dockFill)
            .background(accentColor.copy(alpha = if (tokens.isDark) 0.10f else 0.06f))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        accentColor.copy(alpha = 0.45f),
                        tokens.glassBorderStart,
                        tokens.glassBorderEnd
                    )
                ),
                shape = shape
            )
            .animateContentSize(animationSpec = tween(220, easing = FastOutSlowInEasing))
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("operation_progress_card")
    ) {
        // Header Row: Status badge + Title + Percentage + Expand + Cancel/Dismiss
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                if (progress.phase == OperationPhase.RUNNING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(34.dp),
                        strokeWidth = 2.dp,
                        color = accentColor.copy(alpha = 0.65f),
                        trackColor = Color.Transparent
                    )
                }
                Icon(
                    imageVector = resolveOperationIcon(progress),
                    contentDescription = progress.operationType.label,
                    tint = accentColor,
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleExpand
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = progress.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (progress.phase == OperationPhase.RUNNING || progress.phase == OperationPhase.COMPLETED) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.16f))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${(animatedFraction * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                    }
                }

                if (!isExpanded) {
                    val collapsedSubtitle = when (progress.phase) {
                        OperationPhase.RUNNING -> buildString {
                            if (progress.currentItemName.isNotBlank()) append(progress.currentItemName)
                            if (progress.totalItems > 0) {
                                if (isNotEmpty()) append(" · ")
                                append("${progress.processedItems}/${progress.totalItems}")
                            }
                        }
                        else -> progress.statusDetail
                    }
                    if (collapsedSubtitle.isNotBlank()) {
                        Text(
                            text = collapsedSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            IconButton(
                onClick = onToggleExpand,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("operation_progress_expand_button")
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse progress details" else "Expand progress details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (progress.phase == OperationPhase.RUNNING) {
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            MusicCoral.copy(alpha = if (isCancelling) 0.08f else 0.15f)
                        )
                        .clickable(
                            enabled = !isCancelling,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(),
                            onClick = onCancel
                        )
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                        .testTag("operation_progress_cancel_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isCancelling) "Stopping" else "Cancel",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isCancelling) MusicCoral.copy(alpha = 0.55f) else MusicCoral
                    )
                }
            } else {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("operation_progress_dismiss_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Dismiss notification",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LinearProgressIndicator(
            progress = { animatedFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = accentColor,
            trackColor = tokens.dividerColor,
            strokeCap = StrokeCap.Round
        )

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(tween(200)) + fadeIn(tween(180)),
            exit = shrinkVertically(tween(180)) + fadeOut(tween(150))
        ) {
            Column(modifier = Modifier.padding(top = 10.dp)) {
                val primaryDetail = when (progress.phase) {
                    OperationPhase.RUNNING -> {
                        if (isCancelling) {
                            "Cancelling and cleaning up partial files…"
                        } else if (progress.currentItemName.isNotBlank()) {
                            progress.currentItemName
                        } else {
                            progress.statusDetail
                        }
                    }
                    else -> progress.statusDetail
                }

                if (primaryDetail.isNotBlank()) {
                    Text(
                        text = primaryDetail,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val countLabel = if (progress.totalItems > 0) {
                        "${progress.processedItems} / ${progress.totalItems} item${if (progress.totalItems == 1) "" else "s"}"
                    } else {
                        progress.operationType.label
                    }
                    val destinationSuffix = if (progress.destinationLabel.isNotBlank() &&
                        progress.phase == OperationPhase.RUNNING
                    ) {
                        " · ${progress.destinationLabel}"
                    } else {
                        ""
                    }
                    Text(
                        text = "$countLabel$destinationSuffix",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (progress.totalBytes > 0L) {
                        Spacer(modifier = Modifier.width(8.dp))
                        val clampedProcessed = progress.processedBytes.coerceIn(0L, progress.totalBytes)
                        Text(
                            text = "${FormatUtils.formatFileSize(clampedProcessed)} / ${FormatUtils.formatFileSize(progress.totalBytes)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun resolveOperationIcon(progress: OperationProgress): ImageVector {
    return when (progress.phase) {
        OperationPhase.COMPLETED -> Icons.Rounded.CheckCircle
        OperationPhase.CANCELLED -> Icons.Rounded.PauseCircleOutline
        OperationPhase.FAILED -> Icons.Rounded.ErrorOutline
        OperationPhase.RUNNING -> when (progress.operationType) {
            FileOperationType.COPY -> Icons.Rounded.ContentCopy
            FileOperationType.MOVE -> Icons.AutoMirrored.Rounded.DriveFileMove
            FileOperationType.DELETE -> Icons.Rounded.DeleteSweep
            FileOperationType.COMPRESS -> Icons.Rounded.Archive
            FileOperationType.EXTRACT -> Icons.Rounded.FolderZip
            else -> Icons.Rounded.Archive
        }
    }
}
