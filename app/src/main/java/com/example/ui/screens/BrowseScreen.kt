package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.FormatUtils
import com.example.model.FileActivityEvent
import com.example.model.FileCategory
import com.example.model.SourceShortcut
import com.example.model.SourceType
import com.example.model.ThemePreference
import com.example.ui.components.CategoryGlowIcon
import com.example.ui.components.CompactMyPhoneVisualization
import com.example.ui.components.GlassCard
import com.example.ui.components.RecentActivityBrowseCard
import com.example.ui.theme.LocalGlassTokens
import com.example.ui.theme.MusicCoral
import com.example.viewmodel.DashboardUiState

@Composable
fun BrowseScreen(
    state: DashboardUiState,
    activityEvents: List<FileActivityEvent>,
    themePreference: ThemePreference,
    showHiddenFiles: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onSetThemePreference: (ThemePreference) -> Unit,
    onToggleHiddenFiles: () -> Unit,
    onRequestAllFilesAccess: () -> Unit,
    onRefresh: () -> Unit,
    onCategoryClick: (FileCategory) -> Unit,
    onDownloadsClick: () -> Unit,
    onFavouritesClick: () -> Unit,
    onMyPhoneClick: () -> Unit,
    onOpenStorageAnalytics: () -> Unit,
    onOpenRecentActivity: () -> Unit,
    onSourceClick: (SourceType) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    var menuExpanded by remember { mutableStateOf(false) }

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
                top = 8.dp,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Header Actions + Bold "Browse" Title
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick Dark Mode Toggle Pill
                        GlassCard(
                            shape = CircleShape,
                            onClick = { onToggleDarkMode(tokens.isDark) },
                            testTag = "dark_mode_toggle"
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

                        Spacer(modifier = Modifier.width(6.dp))

                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier
                                    .testTag("browse_overflow_menu")
                                    .minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = "Browse options",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Refresh storage scan") },
                                    leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onRefresh()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (showHiddenFiles) "Hide dot files" else "Show hidden files") },
                                    leadingIcon = {
                                        Icon(
                                            if (showHiddenFiles) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onToggleHiddenFiles()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Follow system theme") },
                                    leadingIcon = {
                                        if (themePreference == ThemePreference.SYSTEM) {
                                            Icon(Icons.Rounded.Check, contentDescription = null)
                                        }
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onSetThemePreference(ThemePreference.SYSTEM)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Storage permissions") },
                                    leadingIcon = { Icon(Icons.Rounded.Security, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onRequestAllFilesAccess()
                                    }
                                )
                            }
                        }
                    }

                    Text(
                        text = stringResource(R.string.browse_title),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                    )
                }
            }

            // All Files Access Permission Banner (if not yet granted)
            item {
                AnimatedVisibility(
                    visible = !state.hasAllFilesAccess,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    GlassCard(
                        shape = RoundedCornerShape(22.dp),
                        elevated = true,
                        onClick = onRequestAllFilesAccess,
                        testTag = "permission_banner"
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.grant_storage_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.grant_storage_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = "Allow",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 2x3 Category Grid
            item {
                val categories = FileCategory.entries
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (rowIndex in 0 until 3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (colIndex in 0 until 2) {
                                val category = categories[rowIndex * 2 + colIndex]
                                val count = state.categoryCounts[category] ?: 0
                                CategoryGridCard(
                                    category = category,
                                    count = count,
                                    onClick = { onCategoryClick(category) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Access Pill Cards (Downloads & Favourites)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickAccessPillCard(
                        title = stringResource(R.string.qa_downloads),
                        subtitle = if (state.downloadsCount > 0) "${state.downloadsCount} items" else "Saved downloads",
                        icon = Icons.Rounded.Download,
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = onDownloadsClick,
                        testTag = "quick_access_downloads"
                    )
                    QuickAccessPillCard(
                        title = stringResource(R.string.qa_favourites),
                        subtitle = if (state.favouritesCount > 0) "${state.favouritesCount} pinned" else "Pinned files & folders",
                        icon = Icons.Rounded.FavoriteBorder,
                        iconTint = MusicCoral,
                        onClick = onFavouritesClick,
                        testTag = "quick_access_favourites"
                    )
                }
            }

            // Separate My Phone (Internal Storage) Card with Colorful Usage Bar
            item {
                GlassCard(
                    shape = RoundedCornerShape(24.dp),
                    onClick = onMyPhoneClick,
                    testTag = "quick_access_my_phone"
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 15.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Smartphone,
                                    contentDescription = "My Phone",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.qa_my_phone),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${FormatUtils.formatFileSize(state.storageSummary.usedBytes)} used of ${FormatUtils.formatFileSize(state.storageSummary.totalBytes)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = "Open internal storage",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        CompactMyPhoneVisualization(
                            summary = state.storageSummary,
                            onOpenAnalytics = onOpenStorageAnalytics
                        )
                    }
                }
            }

            // Recent Activity Quick Card
            item {
                RecentActivityBrowseCard(
                    events = activityEvents,
                    onOpenPanel = onOpenRecentActivity
                )
            }

            // Sources Section
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.sources_heading),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp, top = 6.dp, bottom = 8.dp)
                    )

                    GlassCard(
                        shape = RoundedCornerShape(26.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            val sources = state.sources.ifEmpty {
                                SourceType.entries.map { SourceShortcut(it, 0, emptyList()) }
                            }
                            sources.forEachIndexed { index, shortcut ->
                                SourceRowItem(
                                    shortcut = shortcut,
                                    onClick = { onSourceClick(shortcut.type) }
                                )
                                if (index < sources.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 66.dp, end = 16.dp),
                                        thickness = 0.7.dp,
                                        color = tokens.dividerColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryGridCard(
    category: FileCategory,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.height(78.dp),
        shape = RoundedCornerShape(22.dp),
        onClick = onClick,
        testTag = "category_${category.name.lowercase()}"
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryGlowIcon(category = category, size = 44.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuickAccessPillCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    testTag: String
) {
    GlassCard(
        shape = RoundedCornerShape(24.dp),
        onClick = onClick,
        testTag = testTag
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(iconTint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(21.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "Open $title",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SourceRowItem(
    shortcut: SourceShortcut,
    onClick: () -> Unit
) {
    val icon = when (shortcut.type) {
        SourceType.CAMERA -> Icons.Rounded.PhotoCamera
        SourceType.SCREENSHOTS -> Icons.Rounded.Screenshot
        SourceType.RECORDER -> Icons.Rounded.GraphicEq
        SourceType.DUAL_APPS -> Icons.Rounded.Layers
        SourceType.TELEGRAM -> Icons.AutoMirrored.Rounded.Send
    }
    val accent = shortcut.type.accentColor

    Row(
        modifier = Modifier
            .testTag("source_${shortcut.type.name.lowercase()}")
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(10.dp),
                    ambientColor = accent,
                    spotColor = accent
                )
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        listOf(accent.copy(alpha = 0.85f), accent)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = shortcut.type.label,
                tint = Color.White,
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = shortcut.type.label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (shortcut.itemCount > 0) {
                Text(
                    text = "${shortcut.itemCount} item${if (shortcut.itemCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = "Open ${shortcut.type.label}",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
