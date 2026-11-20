package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.FormatUtils
import com.example.model.BulkSelectionSummary
import com.example.model.FileCategory
import com.example.model.FileItem
import com.example.ui.components.BulkActionsDock
import com.example.ui.components.FileListRow
import com.example.ui.components.GlassCard
import com.example.ui.theme.LocalGlassTokens

@Composable
fun RecentScreen(
    recentFiles: List<FileItem>,
    selectedPaths: Set<String>,
    favouritePaths: Set<String>,
    onToggleDarkMode: (Boolean) -> Unit,
    onSelectAll: (List<FileItem>) -> Unit,
    onClearSelection: () -> Unit,
    onOpenBulkSheet: () -> Unit,
    onMoveSelected: () -> Unit,
    onCopySelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onItemClick: (FileItem) -> Unit,
    onItemLongClick: (FileItem) -> Unit,
    onItemMoreClick: (FileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    var filterCategory by remember { mutableStateOf<FileCategory?>(null) }
    val isSelectionMode = selectedPaths.isNotEmpty()

    if (isSelectionMode) {
        BackHandler { onClearSelection() }
    }

    val filtered = remember(recentFiles, filterCategory) {
        if (filterCategory == null) recentFiles else recentFiles.filter { it.category == filterCategory }
    }
    val grouped = remember(filtered) {
        filtered.groupBy { FormatUtils.recentDateBucket(it.lastModifiedMillis) }
    }
    val selectedSummary = remember(selectedPaths, recentFiles, favouritePaths) {
        BulkSelectionSummary.from(recentFiles.filter { it.path in selectedPaths }, favouritePaths)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 12.dp,
                bottom = if (isSelectionMode) 152.dp else 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp)
                    ) {
                        Text(
                            text = if (isSelectionMode) "${selectedSummary.totalCount} selected" else stringResource(R.string.recent_title),
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isSelectionMode) {
                            Text(
                                text = "${selectedSummary.formatBreakdown()} · ${FormatUtils.formatFileSize(selectedSummary.totalBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (isSelectionMode) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onSelectAll(filtered) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SelectAll,
                                    contentDescription = "Select all recent files",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            IconButton(
                                onClick = onClearSelection,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear selection",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    } else {
                        GlassCard(
                            shape = CircleShape,
                            onClick = { onToggleDarkMode(tokens.isDark) },
                            testTag = "recent_dark_mode_toggle"
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (tokens.isDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                    contentDescription = "Toggle dark mode",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (tokens.isDark) "Light" else "Dark",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = filterCategory == null,
                            onClick = { filterCategory = null },
                            label = { Text("All") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    items(FileCategory.entries) { cat ->
                        FilterChip(
                            selected = filterCategory == cat,
                            onClick = { filterCategory = if (filterCategory == cat) null else cat },
                            label = { Text(cat.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = cat.primaryColor,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    GlassCard(
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No recent files found",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Files you download, capture, or edit will appear here chronologically.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                listOf("Today", "Yesterday", "Earlier").forEach { bucket ->
                    val bucketItems = grouped[bucket].orEmpty()
                    if (bucketItems.isNotEmpty()) {
                        item(key = "header_$bucket") {
                            Text(
                                text = bucket,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 6.dp, top = 6.dp, bottom = 2.dp)
                            )
                        }
                        items(bucketItems, key = { it.path }) { item ->
                            FileListRow(
                                item = item,
                                isSelectionMode = isSelectionMode,
                                isSelected = item.path in selectedPaths,
                                onClick = { onItemClick(item) },
                                onLongClick = { onItemLongClick(item) },
                                onMoreClick = { onItemMoreClick(item) }
                            )
                        }
                    }
                }
            }
        }

        BulkActionsDock(
            summary = selectedSummary,
            onOpenBulkSheet = onOpenBulkSheet,
            onMoveSelected = onMoveSelected,
            onCopySelected = onCopySelected,
            onDeleteSelected = onDeleteSelected,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
