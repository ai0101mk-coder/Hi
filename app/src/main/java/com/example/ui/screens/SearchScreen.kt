package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import com.example.viewmodel.SearchUiState

@Composable
fun SearchScreen(
    state: SearchUiState,
    selectedPaths: Set<String>,
    favouritePaths: Set<String>,
    onQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onRefreshIndex: () -> Unit,
    onToggleCategory: (FileCategory?) -> Unit,
    onBackClick: () -> Unit,
    onToggleSelection: (String) -> Unit,
    onSelectAll: (List<FileItem>) -> Unit,
    onClearSelection: () -> Unit,
    onOpenBulkSheet: () -> Unit,
    onMoveSelected: () -> Unit,
    onCopySelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onItemClick: (FileItem) -> Unit,
    onItemMoreClick: (FileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val isSelectionMode = selectedPaths.isNotEmpty()
    val selectedSummary = remember(selectedPaths, state.results, favouritePaths) {
        BulkSelectionSummary.from(state.results.filter { it.path in selectedPaths }, favouritePaths)
    }

    if (isSelectionMode) {
        BackHandler { onClearSelection() }
    }

    LaunchedEffect(Unit) {
        if (!isSelectionMode) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .statusBarsPadding()
        ) {
            if (isSelectionMode) {
                // Contextual Selection Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClearSelection,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("search_clear_selection")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Cancel selection",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${selectedSummary.totalCount} selected",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${selectedSummary.formatBreakdown()} · ${FormatUtils.formatFileSize(selectedSummary.totalBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = { onSelectAll(state.results) },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SelectAll,
                            contentDescription = "Select all search results",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            } else {
                // Glassmorphic Search Bar with explicit Search trigger button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .shadow(10.dp, CircleShape, ambientColor = tokens.glassShadow)
                            .clip(CircleShape)
                            .background(tokens.glassFillElevated)
                            .border(
                                1.dp,
                                Brush.linearGradient(listOf(tokens.glassBorderStart, tokens.glassBorderEnd)),
                                CircleShape
                            )
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .testTag("search_back_button")
                                .minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (state.query.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.search_hint),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            BasicTextField(
                                value = state.query,
                                onValueChange = onQueryChange,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        keyboardController?.hide()
                                        onSearchSubmit()
                                    }
                                ),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                                    .testTag("search_input")
                            )
                        }

                        if (state.query.isNotEmpty()) {
                            IconButton(
                                onClick = { onQueryChange("") },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Clickable Search Action Button
                        Box(
                            modifier = Modifier
                                .testTag("search_trigger_button")
                                .padding(end = 4.dp)
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable {
                                    keyboardController?.hide()
                                    onSearchSubmit()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Run search",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Index status & quick re-index row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (state.isSearching) {
                        "Indexing device storage…"
                    } else {
                        "${state.results.size} shown · ${state.indexedTotalCount} indexed items"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onRefreshIndex)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Rebuild search index",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Re-index",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Category Filter Chips with live hit counts across all categories
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    val totalCatHits = state.categoryHitCounts.values.sum()
                    FilterChip(
                        selected = state.selectedCategory == null,
                        onClick = { onToggleCategory(null) },
                        label = { Text("All ($totalCatHits)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                items(FileCategory.entries) { category ->
                    val selected = state.selectedCategory == category
                    val hitCount = state.categoryHitCounts[category] ?: 0
                    FilterChip(
                        selected = selected,
                        onClick = { onToggleCategory(category) },
                        label = { Text("${category.label} ($hitCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = category.primaryColor,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Recent Search Queries
            if (state.recentQueries.isNotEmpty() && state.query.isEmpty() && !isSelectionMode) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(state.recentQueries) { recent ->
                        AssistChip(
                            onClick = {
                                onQueryChange(recent)
                                onSearchSubmit()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(recent) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

            if (state.isSearching) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.results.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GlassCard(shape = RoundedCornerShape(24.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SearchOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No matching files",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try another filename, extension, or category filter above.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = navBottom + if (isSelectionMode) 148.dp else 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    items(state.results, key = { it.path }) { item ->
                        FileListRow(
                            item = item,
                            isSelectionMode = isSelectionMode,
                            isSelected = item.path in selectedPaths,
                            onClick = {
                                if (isSelectionMode) {
                                    onToggleSelection(item.path)
                                } else {
                                    onItemClick(item)
                                }
                            },
                            onLongClick = { onToggleSelection(item.path) },
                            onMoreClick = { onItemMoreClick(item) }
                        )
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
