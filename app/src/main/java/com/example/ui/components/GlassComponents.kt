package com.example.ui.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.FormatUtils
import com.example.model.FileCategory
import com.example.model.FileItem
import com.example.ui.theme.DocsAmber
import com.example.ui.theme.LocalGlassTokens
import com.example.ui.theme.MusicCoral
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AuroraMeshBackground(modifier: Modifier = Modifier) {
    val tokens = LocalGlassTokens.current
    val infiniteTransition = rememberInfiniteTransition(label = "aurora_mesh")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mesh_phase"
    )

    val blob0 by animateColorAsState(tokens.blobColors[0], tween(500), label = "b0")
    val blob1 by animateColorAsState(tokens.blobColors[1], tween(500), label = "b1")
    val blob2 by animateColorAsState(tokens.blobColors[2], tween(500), label = "b2")
    val blob3 by animateColorAsState(tokens.blobColors[3], tween(500), label = "b3")
    val baseColor by animateColorAsState(tokens.backgroundBase, tween(500), label = "base")

    val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.blur(72.dp, BlurredEdgeTreatment.Unbounded)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseColor)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(blurModifier)
        ) {
            val w = size.width
            val h = size.height
            val blobAlpha = if (tokens.isDark) 0.50f else 0.42f

            // Top-left Purple/Violet orb
            val c1 = Offset(
                x = w * (0.18f + 0.08f * cos(phase)),
                y = h * (0.14f + 0.06f * sin(phase))
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(blob0.copy(alpha = blobAlpha), blob0.copy(alpha = 0f)),
                    center = c1,
                    radius = w * 0.65f
                ),
                radius = w * 0.65f,
                center = c1
            )

            // Top-right Electric Blue orb
            val c2 = Offset(
                x = w * (0.85f + 0.07f * sin(phase * 0.9f)),
                y = h * (0.24f + 0.07f * cos(phase * 0.9f))
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(blob1.copy(alpha = blobAlpha), blob1.copy(alpha = 0f)),
                    center = c2,
                    radius = w * 0.60f
                ),
                radius = w * 0.60f,
                center = c2
            )

            // Mid-left Warm Pink/Magenta orb
            val c3 = Offset(
                x = w * (0.22f + 0.09f * sin(phase + 1.4f)),
                y = h * (0.58f + 0.08f * cos(phase + 1.4f))
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(blob2.copy(alpha = blobAlpha * 0.92f), blob2.copy(alpha = 0f)),
                    center = c3,
                    radius = w * 0.62f
                ),
                radius = w * 0.62f,
                center = c3
            )

            // Bottom-right Cyan / Peach orb
            val c4 = Offset(
                x = w * (0.80f + 0.08f * cos(phase + 2.2f)),
                y = h * (0.84f + 0.06f * sin(phase + 2.2f))
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(blob3.copy(alpha = blobAlpha * 0.85f), blob3.copy(alpha = 0f)),
                    center = c4,
                    radius = w * 0.64f
                ),
                radius = w * 0.64f,
                center = c4
            )
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    elevated: Boolean = false,
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val tokens = LocalGlassTokens.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.965f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "glass_scale"
    )

    val fillColor = if (elevated) tokens.glassFillElevated else tokens.glassFill
    val borderBrush = Brush.linearGradient(
        colors = listOf(tokens.glassBorderStart, tokens.glassBorderEnd)
    )

    val clickMod = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = androidx.compose.material3.ripple(),
            onClick = onClick
        )
    } else {
        Modifier
    }

    val tagMod = if (testTag != null) Modifier.testTag(testTag) else Modifier

    Box(
        modifier = modifier
            .then(tagMod)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (elevated) 14.dp else 8.dp,
                shape = shape,
                ambientColor = tokens.glassShadow,
                spotColor = tokens.glassShadow
            )
            .clip(shape)
            .background(fillColor)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (tokens.isDark) 0.08f else 0.25f),
                        Color.Transparent
                    )
                )
            )
            .border(width = 1.dp, brush = borderBrush, shape = shape)
            .then(clickMod),
        content = content
    )
}

@Composable
fun CategoryGlowIcon(
    category: FileCategory,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val icon = categoryIcon(category)
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Ambient glow halo
        Box(
            modifier = Modifier
                .size(size)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            category.glowColor.copy(alpha = 0.45f),
                            category.primaryColor.copy(alpha = 0f)
                        )
                    ),
                    shape = CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(size * 0.84f)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(12.dp),
                    ambientColor = category.primaryColor,
                    spotColor = category.primaryColor
                )
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(category.glowColor, category.primaryColor)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = category.label,
                tint = Color.White,
                modifier = Modifier.size(size * 0.48f)
            )
        }
    }
}

fun categoryIcon(category: FileCategory): ImageVector = when (category) {
    FileCategory.DOCS -> Icons.Rounded.Description
    FileCategory.IMAGES -> Icons.Rounded.Image
    FileCategory.VIDEOS -> Icons.Rounded.Videocam
    FileCategory.MUSIC -> Icons.Rounded.MusicNote
    FileCategory.ARCHIVES -> Icons.Rounded.Archive
    FileCategory.APKS -> Icons.Rounded.Android
}

enum class MainTab { RECENT, BROWSE }

@Composable
fun FloatingBottomDock(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    val borderBrush = Brush.linearGradient(
        colors = listOf(tokens.glassBorderStart, tokens.glassBorderEnd)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pill-shaped glassmorphic tab bar (Recent | Browse)
        BoxWithConstraints(
            modifier = Modifier
                .width(236.dp)
                .height(64.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    ambientColor = tokens.glassShadow,
                    spotColor = tokens.glassShadow
                )
                .clip(CircleShape)
                .background(tokens.dockFill)
                .border(1.dp, borderBrush, CircleShape)
                .padding(5.dp)
        ) {
            val tabWidth = maxWidth / 2
            val indicatorOffset by animateDpAsState(
                targetValue = if (selectedTab == MainTab.RECENT) 0.dp else tabWidth,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "tab_pill_offset"
            )

            // Active glowing pill indicator
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(tabWidth)
                    .fillMaxHeight()
                    .shadow(
                        elevation = 6.dp,
                        shape = CircleShape,
                        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    )
                    .clip(CircleShape)
                    .background(tokens.activePillFill)
                    .border(
                        width = 0.8.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = if (tokens.isDark) 0.45f else 0.18f),
                        shape = CircleShape
                    )
            )

            Row(modifier = Modifier.fillMaxSize()) {
                DockTabItem(
                    label = "Recent",
                    icon = Icons.Rounded.History,
                    selected = selectedTab == MainTab.RECENT,
                    testTag = "nav_tab_recent",
                    onClick = { onTabSelected(MainTab.RECENT) },
                    modifier = Modifier.weight(1f)
                )
                DockTabItem(
                    label = "Browse",
                    icon = Icons.Rounded.FolderOpen,
                    selected = selectedTab == MainTab.BROWSE,
                    testTag = "nav_tab_browse",
                    onClick = { onTabSelected(MainTab.BROWSE) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Separate circular floating Search button on the right
        val searchInteraction = remember { MutableInteractionSource() }
        val searchPressed by searchInteraction.collectIsPressedAsState()
        val searchScale by animateFloatAsState(
            targetValue = if (searchPressed) 0.92f else 1f,
            animationSpec = spring(Spring.DampingRatioMediumBouncy),
            label = "search_fab_scale"
        )

        Box(
            modifier = Modifier
                .testTag("floating_search_button")
                .size(64.dp)
                .graphicsLayer {
                    scaleX = searchScale
                    scaleY = searchScale
                }
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    ambientColor = tokens.glassShadow,
                    spotColor = tokens.glassShadow
                )
                .clip(CircleShape)
                .background(tokens.dockFill)
                .border(1.dp, borderBrush, CircleShape)
                .clickable(
                    interactionSource = searchInteraction,
                    indication = androidx.compose.material3.ripple(),
                    onClick = onSearchClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Search files",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun DockTabItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "tab_color"
    )
    Column(
        modifier = modifier
            .testTag(testTag)
            .fillMaxHeight()
            .clip(CircleShape)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListRow(
    item: FileItem,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    val highlightColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = if (tokens.isDark) 0.24f else 0.14f)
    } else {
        tokens.glassFill
    }
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier = modifier
            .testTag("file_item_${item.name}")
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = shape,
                ambientColor = tokens.glassShadow,
                spotColor = tokens.glassShadow
            )
            .clip(shape)
            .background(highlightColor)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else tokens.glassBorderStart,
                shape = shape
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FileThumbnailOrBadge(item = item, size = 46.dp)

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (item.isFavourite) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = "Favourite",
                        tint = MusicCoral,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            val subtitle = if (item.isDirectory) {
                "${item.childCount} item${if (item.childCount == 1) "" else "s"} · ${FormatUtils.formatShortDate(item.lastModifiedMillis)}"
            } else {
                "${FormatUtils.formatFileSize(item.sizeBytes)} · ${FormatUtils.formatShortDate(item.lastModifiedMillis)}"
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isSelectionMode) {
            Icon(
                imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = if (isSelected) "Selected" else "Not selected",
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(22.dp)
            )
        } else {
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "File options for ${item.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FileThumbnailOrBadge(
    item: FileItem,
    size: Dp = 46.dp
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(12.dp)
    val isVisualMedia = !item.isDirectory &&
        (item.category == FileCategory.IMAGES || item.category == FileCategory.VIDEOS)

    if (isVisualMedia) {
        val modelData: Any = item.contentUriString ?: File(item.path)
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(modelData)
                .crossfade(true)
                .build(),
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    } else if (item.isDirectory) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        listOf(DocsAmber.copy(alpha = 0.22f), DocsAmber.copy(alpha = 0.10f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Folder,
                contentDescription = "Folder",
                tint = DocsAmber,
                modifier = Modifier.size(size * 0.56f)
            )
        }
    } else {
        val cat = item.category
        if (cat != null) {
            CategoryGlowIcon(category = cat, size = size)
        } else {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.InsertDriveFile,
                    contentDescription = item.name,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(size * 0.52f)
                )
            }
        }
    }
}
