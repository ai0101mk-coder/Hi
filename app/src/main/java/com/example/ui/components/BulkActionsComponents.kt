package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.FormatUtils
import com.example.model.BulkSelectionSummary
import com.example.ui.theme.ArchivesCyan
import com.example.ui.theme.LocalGlassTokens
import com.example.ui.theme.MusicCoral

@Composable
fun BulkActionsDock(
    summary: BulkSelectionSummary,
    onOpenBulkSheet: () -> Unit,
    onMoveSelected: () -> Unit,
    onCopySelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    val shape = RoundedCornerShape(26.dp)

    AnimatedVisibility(
        visible = summary.totalCount > 0,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .shadow(18.dp, shape, ambientColor = tokens.glassShadow, spotColor = tokens.glassShadow)
                .clip(shape)
                .background(tokens.dockFill)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(tokens.glassBorderStart, tokens.glassBorderEnd)),
                    shape = shape
                )
        ) {
            // Selection Summary & Bulk Actions Trigger Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                        onClick = onOpenBulkSheet
                    )
                    .padding(horizontal = 14.dp, vertical = 9.dp)
                    .testTag("bulk_actions_menu_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = summary.totalCount.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "${summary.formatBreakdown()} · ${FormatUtils.formatFileSize(summary.totalBytes)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bulk Actions",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowUp,
                        contentDescription = "Open Bulk Actions menu",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            HorizontalDivider(color = tokens.dividerColor, thickness = 0.8.dp)

            // Quick Batch Operations Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DockQuickButton(
                    icon = Icons.AutoMirrored.Rounded.DriveFileMove,
                    label = "Move",
                    onClick = onMoveSelected,
                    testTag = "bulk_dock_move"
                )
                DockQuickButton(
                    icon = Icons.Rounded.ContentCopy,
                    label = "Copy",
                    onClick = onCopySelected,
                    testTag = "bulk_dock_copy"
                )
                DockQuickButton(
                    icon = Icons.Rounded.Delete,
                    label = "Delete",
                    onClick = onDeleteSelected,
                    tint = Color(0xFFF43F5E),
                    testTag = "bulk_dock_delete"
                )
                DockQuickButton(
                    icon = Icons.Rounded.Tune,
                    label = "More",
                    onClick = onOpenBulkSheet,
                    tint = MaterialTheme.colorScheme.primary,
                    testTag = "bulk_dock_more"
                )
            }
        }
    }
}

@Composable
private fun DockQuickButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    testTag: String
) {
    Column(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkActionsSheet(
    summary: BulkSelectionSummary,
    visibleItemCount: Int,
    onDismiss: () -> Unit,
    onMoveSelected: () -> Unit,
    onCopySelected: () -> Unit,
    onCompressSelected: () -> Unit,
    onShareSelected: () -> Unit,
    onToggleFavourites: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    val isAllSelected = visibleItemCount > 0 && summary.totalCount >= visibleItemCount

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
                .testTag("bulk_actions_sheet")
        ) {
            // Selection Metadata Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Checklist,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bulk Actions (${summary.totalCount})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${summary.formatBreakdown()} · ${FormatUtils.formatFileSize(summary.totalBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(
                color = tokens.dividerColor,
                modifier = Modifier.padding(vertical = 6.dp)
            )

            BulkSheetActionRow(
                icon = Icons.AutoMirrored.Rounded.DriveFileMove,
                title = "Move to…",
                subtitle = "Relocate ${summary.totalCount} item${if (summary.totalCount == 1) "" else "s"} to another folder",
                tint = MaterialTheme.colorScheme.primary,
                testTag = "bulk_sheet_move",
                onClick = { onDismiss(); onMoveSelected() }
            )

            BulkSheetActionRow(
                icon = Icons.Rounded.ContentCopy,
                title = "Copy to…",
                subtitle = "Duplicate ${summary.totalCount} item${if (summary.totalCount == 1) "" else "s"} to another folder",
                tint = MaterialTheme.colorScheme.primary,
                testTag = "bulk_sheet_copy",
                onClick = { onDismiss(); onCopySelected() }
            )

            BulkSheetActionRow(
                icon = Icons.Rounded.Archive,
                title = "Compress to ZIP",
                subtitle = "Bundle into an archive with optional AES-256 lock",
                tint = ArchivesCyan,
                testTag = "bulk_sheet_compress",
                onClick = { onDismiss(); onCompressSelected() }
            )

            BulkSheetActionRow(
                icon = Icons.Rounded.Share,
                title = "Share files",
                subtitle = if (summary.hasShareableFiles) {
                    "Send ${summary.fileCount} file${if (summary.fileCount == 1) "" else "s"} via another app"
                } else {
                    "Select at least one file (folders cannot be shared directly)"
                },
                enabled = summary.hasShareableFiles,
                testTag = "bulk_sheet_share",
                onClick = { onDismiss(); onShareSelected() }
            )

            BulkSheetActionRow(
                icon = if (summary.allAreFavourites) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                title = if (summary.allAreFavourites) "Remove from Favourites" else "Add to Favourites",
                subtitle = if (summary.allAreFavourites) {
                    "Unstar all selected items"
                } else {
                    "Star all selected items for quick access"
                },
                tint = MusicCoral,
                testTag = "bulk_sheet_favourite",
                onClick = { onDismiss(); onToggleFavourites() }
            )

            HorizontalDivider(
                color = tokens.dividerColor,
                modifier = Modifier.padding(vertical = 6.dp)
            )

            if (!isAllSelected && visibleItemCount > 0) {
                BulkSheetActionRow(
                    icon = Icons.Rounded.SelectAll,
                    title = "Select all ($visibleItemCount)",
                    subtitle = "Include every item currently visible in this list",
                    testTag = "bulk_sheet_select_all",
                    onClick = { onSelectAll() }
                )
            }

            BulkSheetActionRow(
                icon = Icons.Rounded.Close,
                title = "Clear selection",
                subtitle = "Exit multi-select mode",
                testTag = "bulk_sheet_clear",
                onClick = { onDismiss(); onClearSelection() }
            )

            BulkSheetActionRow(
                icon = Icons.Rounded.Delete,
                title = "Batch Delete",
                subtitle = "Permanently remove ${summary.totalCount} item${if (summary.totalCount == 1) "" else "s"} (${FormatUtils.formatFileSize(summary.totalBytes)})",
                tint = Color(0xFFF43F5E),
                testTag = "bulk_sheet_delete",
                onClick = { onDismiss(); onDeleteSelected() }
            )
        }
    }
}

@Composable
private fun BulkSheetActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    testTag: String,
    onClick: () -> Unit
) {
    val contentTint = if (enabled) tint else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    val textTint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val subTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 0.9f else 0.45f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 20.dp, vertical = 11.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(contentTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (tint == Color(0xFFF43F5E) && enabled) tint else textTint
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = subTint,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
