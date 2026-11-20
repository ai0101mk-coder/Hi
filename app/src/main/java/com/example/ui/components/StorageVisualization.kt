package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.FormatUtils
import com.example.model.FileCategory
import com.example.model.StorageSegment
import com.example.model.StorageSummary
import com.example.ui.theme.LocalGlassTokens
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompactMyPhoneVisualization(
    summary: StorageSummary,
    onOpenAnalytics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    val segments = remember(summary) { summary.toUsedSegments() }
    val usedPercent = (summary.usedFraction * 100f).roundToInt().coerceIn(0, 100)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Compact Mini Donut Ring
            MiniStorageDonut(
                segments = segments,
                usedFraction = summary.usedFraction,
                usedPercent = usedPercent,
                modifier = Modifier.size(54.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                InteractiveSegmentedBar(
                    segments = segments,
                    usedFraction = summary.usedFraction,
                    selectedKey = null,
                    onSegmentTap = { onOpenAnalytics() },
                    barHeight = 10.dp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Top 4 non-zero category chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    segments.take(4).forEach { seg ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(seg.primaryColor)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = seg.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Expandable Analytics CTA pill
            Row(
                modifier = Modifier
                    .testTag("open_storage_analytics")
                    .clip(CircleShape)
                    .background(
                        if (tokens.isDark) Color.White.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.11f)
                    )
                    .border(
                        width = 0.8.dp,
                        color = tokens.glassBorderStart,
                        shape = CircleShape
                    )
                    .clickable(onClick = onOpenAnalytics)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Analytics,
                    contentDescription = "Open storage analytics",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Details",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun MiniStorageDonut(
    segments: List<StorageSegment>,
    usedFraction: Float,
    usedPercent: Int,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    val trackColor = if (tokens.isDark) {
        Color.White.copy(alpha = 0.10f)
    } else {
        Color(0xFF111827).copy(alpha = 0.08f)
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 6.dp.toPx()
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            val totalUsedSweep = (usedFraction * 360f).coerceIn(0f, 360f)
            var currentAngle = -90f
            val visibleSegments = segments.filter { it.fractionOfUsed > 0.01f }
            val gapDegrees = if (visibleSegments.size > 1) 4f else 0f
            val availableSweep = (totalUsedSweep - gapDegrees * visibleSegments.size).coerceAtLeast(0f)

            visibleSegments.forEach { seg ->
                val sweep = (seg.fractionOfUsed * availableSweep).coerceAtLeast(2f)
                drawArc(
                    brush = Brush.sweepGradient(listOf(seg.glowColor, seg.primaryColor)),
                    startAngle = currentAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
                currentAngle += sweep + gapDegrees
            }
        }

        Text(
            text = "$usedPercent%",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun InteractiveSegmentedBar(
    segments: List<StorageSegment>,
    usedFraction: Float,
    selectedKey: String?,
    onSegmentTap: (StorageSegment) -> Unit,
    modifier: Modifier = Modifier,
    barHeight: androidx.compose.ui.unit.Dp = 14.dp
) {
    val tokens = LocalGlassTokens.current
    val trackColor = if (tokens.isDark) {
        Color.White.copy(alpha = 0.10f)
    } else {
        Color(0xFF111827).copy(alpha = 0.08f)
    }

    val visibleSegments = remember(segments) {
        segments.filter { it.fractionOfTotal > 0.004f }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
            .clip(CircleShape)
            .background(trackColor)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (visibleSegments.isNotEmpty() && usedFraction > 0f) {
                visibleSegments.forEach { seg ->
                    val isSelected = selectedKey == seg.key
                    val dimmed = selectedKey != null && !isSelected
                    val weight = seg.fractionOfTotal.coerceAtLeast(0.015f)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(weight)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        seg.glowColor.copy(alpha = if (dimmed) 0.35f else 1f),
                                        seg.primaryColor.copy(alpha = if (dimmed) 0.35f else 1f)
                                    )
                                )
                            )
                            .clickable { onSegmentTap(seg) }
                    )
                }
            }
            val freeWeight = (1f - usedFraction).coerceAtLeast(0.02f)
            Spacer(modifier = Modifier.weight(freeWeight))
        }
    }
}

@Composable
fun InteractiveDonutChart(
    summary: StorageSummary,
    segments: List<StorageSegment>,
    selectedKey: String?,
    onSelectKey: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    var animateIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animateIn = true }

    val revealProgress by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0f,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "donut_reveal"
    )

    val trackColor = if (tokens.isDark) {
        Color.White.copy(alpha = 0.09f)
    } else {
        Color(0xFF111827).copy(alpha = 0.08f)
    }

    val nonZeroSegments = remember(segments) {
        segments.filter { it.fractionOfUsed > 0.005f }
    }

    // Precompute arc ranges for polar hit testing
    val arcRanges = remember(nonZeroSegments, summary.usedFraction) {
        val totalSweep = (summary.usedFraction * 360f).coerceIn(0f, 360f)
        val gap = if (nonZeroSegments.size > 1) 4.5f else 0f
        val usable = (totalSweep - gap * nonZeroSegments.size).coerceAtLeast(0f)
        var cursor = -90f
        nonZeroSegments.map { seg ->
            val sweep = (seg.fractionOfUsed * usable).coerceAtLeast(3f)
            val start = cursor
            val end = cursor + sweep
            cursor = end + gap
            Triple(seg, start, end)
        }
    }

    val selectedSegment = remember(selectedKey, segments) {
        segments.firstOrNull { it.key == selectedKey }
    }

    Box(
        modifier = modifier.size(220.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(arcRanges, selectedKey) {
                    detectTapGestures { offset ->
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dx = offset.x - cx
                        val dy = offset.y - cy
                        val dist = hypot(dx, dy)
                        val minR = size.width * 0.23f
                        val maxR = size.width * 0.50f
                        if (dist in minR..maxR) {
                            var deg = (atan2(dy, dx) * (180.0 / PI)).toFloat()
                            if (deg < -90f) deg += 360f
                            val hit = arcRanges.firstOrNull { (_, start, end) ->
                                deg in start..end
                            }?.first
                            if (hit != null) {
                                onSelectKey(if (selectedKey == hit.key) null else hit.key)
                            } else {
                                onSelectKey(null)
                            }
                        } else {
                            onSelectKey(null)
                        }
                    }
                }
        ) {
            val baseStroke = 24.dp.toPx()
            val selectedStroke = 30.dp.toPx()
            val inset = selectedStroke / 2f + 6.dp.toPx()
            val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
            val topLeft = Offset(inset, inset)

            // Background full ring track
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = baseStroke, cap = StrokeCap.Round)
            )

            arcRanges.forEach { (seg, start, end) ->
                val isSelected = selectedKey == seg.key
                val dimmed = selectedKey != null && !isSelected
                val sweep = (end - start) * revealProgress
                val strokeWidth = if (isSelected) selectedStroke else baseStroke
                val alpha = if (dimmed) 0.32f else 1f

                if (isSelected) {
                    drawArc(
                        color = seg.glowColor.copy(alpha = 0.28f),
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = selectedStroke + 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                drawArc(
                    brush = Brush.linearGradient(
                        listOf(
                            seg.glowColor.copy(alpha = alpha),
                            seg.primaryColor.copy(alpha = alpha)
                        )
                    ),
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // Center Callout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 34.dp)
        ) {
            if (selectedSegment != null) {
                Text(
                    text = selectedSegment.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = selectedSegment.primaryColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = FormatUtils.formatFileSize(selectedSegment.bytes),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                val pct = (selectedSegment.fractionOfUsed * 100f).roundToInt()
                Text(
                    text = "$pct% of used",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "${(summary.usedFraction * 100f).roundToInt()}%",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = FormatUtils.formatFileSize(summary.usedBytes),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "of ${FormatUtils.formatFileSize(summary.totalBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageAnalyticsSheet(
    summary: StorageSummary,
    onDismiss: () -> Unit,
    onOpenCategory: (FileCategory) -> Unit
) {
    val tokens = LocalGlassTokens.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val segments = remember(summary) { summary.toUsedSegments() }
    var selectedKey by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = if (tokens.isDark) Color(0xFF141626) else Color(0xFFF6F7FC)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Storage Breakdown",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${FormatUtils.formatFileSize(summary.freeBytes)} available · Tap any slice to inspect",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            InteractiveDonutChart(
                summary = summary,
                segments = segments,
                selectedKey = selectedKey,
                onSelectKey = { selectedKey = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            InteractiveSegmentedBar(
                segments = segments,
                usedFraction = summary.usedFraction,
                selectedKey = selectedKey,
                onSegmentTap = { seg ->
                    selectedKey = if (selectedKey == seg.key) null else seg.key
                },
                barHeight = 14.dp
            )

            Spacer(modifier = Modifier.height(18.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 310.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(segments, key = { it.key }) { seg ->
                    val isSelected = selectedKey == seg.key
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.015f else 1f,
                        animationSpec = spring(Spring.DampingRatioMediumBouncy),
                        label = "row_scale"
                    )
                    val shape = RoundedCornerShape(18.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clip(shape)
                            .background(
                                if (isSelected) seg.primaryColor.copy(alpha = 0.16f)
                                else tokens.glassFill
                            )
                            .border(
                                width = if (isSelected) 1.3.dp else 0.8.dp,
                                color = if (isSelected) seg.primaryColor else tokens.glassBorderStart,
                                shape = shape
                            )
                            .clickable {
                                if (seg.category != null) {
                                    if (isSelected) {
                                        onDismiss()
                                        onOpenCategory(seg.category)
                                    } else {
                                        selectedKey = seg.key
                                    }
                                } else {
                                    selectedKey = if (isSelected) null else seg.key
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (seg.category != null) {
                            CategoryGlowIcon(category = seg.category, size = 36.dp)
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(seg.primaryColor.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Storage,
                                    contentDescription = seg.label,
                                    tint = seg.primaryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = seg.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = FormatUtils.formatFileSize(seg.bytes),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(5.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(CircleShape)
                                    .background(seg.primaryColor.copy(alpha = 0.14f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(seg.fractionOfUsed.coerceIn(0.02f, 1f))
                                        .clip(CircleShape)
                                        .background(
                                            Brush.horizontalGradient(listOf(seg.glowColor, seg.primaryColor))
                                        )
                                )
                            }

                            Spacer(modifier = Modifier.height(3.dp))

                            val pct = (seg.fractionOfUsed * 100f).roundToInt()
                            val meta = if (seg.category != null) {
                                "${seg.itemCount} file${if (seg.itemCount == 1) "" else "s"} · $pct% of used"
                            } else {
                                "OS, apps & unindexed data · $pct% of used"
                            }
                            Text(
                                text = meta,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (seg.category != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = "Open ${seg.label}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(22.dp)
                                    .clickable {
                                        onDismiss()
                                        onOpenCategory(seg.category)
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}
