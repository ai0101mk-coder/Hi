package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.FormatUtils
import com.example.model.BrowseMode
import com.example.model.BulkSelectionSummary
import com.example.model.FileItem
import com.example.model.SortDirection
import com.example.model.SortOption
import com.example.ui.components.BulkActionsDock
import com.example.ui.components.FileThumbnailOrBadge
import com.example.ui.components.GlassCard
import com.example.ui.components.SwipeableFileListRow
import com.example.ui.theme.LocalGlassTokens
import com.example.viewmodel.BrowserUiState

@Composable
fun FileBrowserScreen(
    state: BrowserUiState,
    selectedPaths: Set<String>,
    favouritePaths: Set<String>,
    sortOption: SortOption,
    sortDirection: SortDirection,
    onNavigateBack: () -> Unit,
    onNavigateUpDirectory: () -> Boolean,
    onBreadcrumbClick: (String) -> Unit,
    onSelectSortOption: (SortOption) -> Unit,
    onToggleGridView: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onOpenBulkSheet: () -> Unit,
    onItemClick: (FileItem) -> Unit,
    onItemLongClick: (FileItem) -> Unit,
    onItemMoreClick: (FileItem) -> Unit,
    onCreateFolderClick: (String) -> Unit,
    onCopySelected: () -> Unit,
    onMoveSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onSwipeMoveItem: (FileItem) -> Unit,
    onSwipeShareItem: (FileItem) -> Unit,
    onSwipeDeleteItem: (FileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelectionMode = selectedPaths.isNotEmpty()
    var sortMenuOpen by remember { mutableStateOf(false) }
    var revealedSwipePath by remember(state.currentDirectoryPath, state.destination) {
        mutableStateOf<String?>(null)
    }
    LaunchedEffect(isSelectionMode, state.isGridView) {
        if (isSelectionMode || state.isGridView) {
            revealedSwipePath = null
        }
    }
    val selectedSummary = remember(selectedPaths, state.files, favouritePaths) {
        BulkSelectionSummary.from(state.files.filter { it.path in selectedPaths }, favouritePaths)
    }

    BackHandler {
        if (revealedSwipePath != null) {
            revealedSwipePath = null
        } else if (isSelectionMode) {
            onClearSelection()
        } else if (!onNavigateUpDirectory()) {
            onNavigateBack()
        }
    }

    val dest = state.destination
    val isDirectoryWritable = dest?.mode == BrowseMode.DIRECTORY || dest?.mode == BrowseMode.DOWNLOADS
    val headerTitle = if (isSelectionMode) {
        "${selectedPaths.size} selected"
    } else if (dest?.mode == BrowseMode.DIRECTORY) {
        state.breadcrumbs.lastOrNull()?.first ?: dest.title
    } else {
        dest?.title ?: "Files"
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .statusBarsPadding()
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (isSelectionMode) {
                            onClearSelection()
                        } else if (!onNavigateUpDirectory()) {
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier
                        .testTag("browser_back_button")
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = if (isSelectionMode) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = if (isSelectionMode) "Cancel selection" else "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = headerTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isSelectionMode) {
                            "${selectedSummary.formatBreakdown()} · ${FormatUtils.formatFileSize(selectedSummary.totalBytes)}"
                        } else {
                            "${state.files.size} item${if (state.files.size == 1) "" else "s"}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isSelectionMode) {
                    IconButton(
                        onClick = onSelectAll,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SelectAll,
                            contentDescription = "Select all",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                } else {
                    IconButton(
                        onClick = onToggleGridView,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = if (state.isGridView) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                            contentDescription = "Toggle grid or list view",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Box {
                        val arrowRotation by animateFloatAsState(
                            targetValue = if (sortDirection == SortDirection.ASCENDING) 0f else 180f,
                            animationSpec = tween(220),
                            label = "sort_arrow_rotation"
                        )

                        GlassCard(
                            shape = CircleShape,
                            onClick = { sortMenuOpen = true },
                            testTag = "category_sort_pill",
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Sort,
                                    contentDescription = "Sort files",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = sortOption.shortLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.Rounded.ArrowUpward,
                                    contentDescription = if (sortDirection == SortDirection.ASCENDING) "Ascending" else "Descending",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(15.dp)
                                        .graphicsLayer { rotationZ = arrowRotation }
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = sortMenuOpen,
                            onDismissRequest = { sortMenuOpen = false }
                        ) {
                            SortOption.entries.forEach { option ->
                                val isCurrent = sortOption == option
                                val optionIcon = when (option) {
                                    SortOption.NAME -> Icons.Rounded.SortByAlpha
                                    SortOption.DATE -> Icons.Rounded.Schedule
                                    SortOption.SIZE -> Icons.Rounded.DataUsage
                                    SortOption.TYPE -> Icons.Rounded.Category
                                }
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = optionIcon,
                                            contentDescription = null,
                                            tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    text = {
                                        Text(
                                            text = option.label,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    trailingIcon = {
                                        if (isCurrent) {
                                            Icon(
                                                imageVector = Icons.Rounded.ArrowUpward,
                                                contentDescription = if (sortDirection == SortDirection.ASCENDING) "Ascending" else "Descending",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .graphicsLayer { rotationZ = arrowRotation }
                                            )
                                        }
                                    },
                                    onClick = {
                                        sortMenuOpen = false
                                        onSelectSortOption(option)
                                    },
                                    modifier = Modifier.testTag("sort_option_${option.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }

            // Breadcrumbs for Directory Mode
            if (dest?.mode == BrowseMode.DIRECTORY && state.breadcrumbs.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(state.breadcrumbs.size) { index ->
                        val (label, path) = state.breadcrumbs[index]
                        val isLast = index == state.breadcrumbs.lastIndex
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                                color = if (isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onBreadcrumbClick(path) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                            if (!isLast) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // File Content Area
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.files.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GlassCard(shape = RoundedCornerShape(26.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Nothing here yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "This folder or category has no matching files on your device.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else if (state.isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 152.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = if (isSelectionMode) 148.dp else 112.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.files, key = { it.path }) { item ->
                        FileGridCard(
                            item = item,
                            isSelected = item.path in selectedPaths,
                            onClick = { onItemClick(item) },
                            onLongClick = { onItemLongClick(item) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = if (isSelectionMode) 148.dp else 112.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    items(state.files, key = { it.path }) { item ->
                        SwipeableFileListRow(
                            item = item,
                            enabled = !isSelectionMode,
                            isSelectionMode = isSelectionMode,
                            isSelected = item.path in selectedPaths,
                            isRevealed = revealedSwipePath == item.path,
                            onRevealChange = { revealed ->
                                revealedSwipePath = if (revealed) {
                                    item.path
                                } else if (revealedSwipePath == item.path) {
                                    null
                                } else {
                                    revealedSwipePath
                                }
                            },
                            onSwipeStart = {
                                if (revealedSwipePath != item.path) {
                                    revealedSwipePath = null
                                }
                            },
                            onClick = { onItemClick(item) },
                            onLongClick = { onItemLongClick(item) },
                            onMoreClick = { onItemMoreClick(item) },
                            onSwipeMove = { onSwipeMoveItem(item) },
                            onSwipeShare = { onSwipeShareItem(item) },
                            onSwipeDelete = { onSwipeDeleteItem(item) }
                        )
                    }
                }
            }
        }

        // Create Folder FAB when browsing a writable folder and not in selection mode
        if (isDirectoryWritable && !isSelectionMode) {
            FloatingActionButton(
                onClick = { onCreateFolderClick(state.currentDirectoryPath) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(20.dp)
                    .testTag("create_folder_fab")
            ) {
                Icon(
                    imageVector = Icons.Rounded.CreateNewFolder,
                    contentDescription = "New folder"
                )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileGridCard(
    item: FileItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, shape, ambientColor = tokens.glassShadow)
            .clip(shape)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else tokens.glassFill
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else tokens.glassBorderStart,
                shape = shape
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FileThumbnailOrBadge(item = item, size = 64.dp)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = item.name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = if (item.isDirectory) "${item.childCount} items" else FormatUtils.formatFileSize(item.sizeBytes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}
