package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.graphics.pdf.PdfRenderer
import android.media.MediaPlayer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.ImageView
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.data.FormatUtils
import com.example.model.FileItem
import com.example.ui.components.GlassCard
import com.example.ui.theme.ApksEmerald
import com.example.ui.theme.ArchivesCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalGlassTokens
import com.example.ui.theme.MusicCoral
import com.example.ui.theme.MusicPinkGlow
import com.example.viewmodel.PreviewTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

@Composable
fun FilePreviewModal(
    target: PreviewTarget,
    apkIconProvider: (String) -> Drawable?,
    onDismiss: () -> Unit,
    onShare: (FileItem) -> Unit,
    onOpenExternal: (FileItem) -> Unit,
    onInstallApk: (FileItem) -> Unit,
    onExtractZip: (FileItem) -> Unit
) {
    val tokens = LocalGlassTokens.current
    BackHandler { onDismiss() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (tokens.isDark) Color(0xEE080912) else Color(0xEEF4F5FB)
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("preview_modal")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Preview Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .testTag("preview_close_button")
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close preview",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = target.file.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${FormatUtils.formatFileSize(target.file.sizeBytes)} · ${FormatUtils.formatDateTime(target.file.lastModifiedMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                IconButton(
                    onClick = { onShare(target.file) },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                IconButton(
                    onClick = { onOpenExternal(target.file) },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = "Open externally",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // Content Area by Target Type
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                when (target) {
                    is PreviewTarget.Image -> ZoomableImagePreview(target.file)
                    is PreviewTarget.Video -> VideoPreviewPlayer(target.file)
                    is PreviewTarget.Audio -> AudioPreviewPlayer(target.file)
                    is PreviewTarget.Pdf -> PdfDocumentPreview(target.file)
                    is PreviewTarget.Text -> TextDocumentPreview(target)
                    is PreviewTarget.Archive -> ArchiveZipPreview(target, onExtractZip)
                    is PreviewTarget.Apk -> ApkInstallerPreview(
                        target = target,
                        apkIconProvider = apkIconProvider,
                        onInstallClick = { onInstallApk(target.file) }
                    )
                    is PreviewTarget.Generic -> GenericFilePreview(
                        file = target.file,
                        onOpenExternal = { onOpenExternal(target.file) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoomableImagePreview(file: FileItem) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                    scale = newScale
                    if (newScale > 1f) {
                        offsetX += pan.x
                        offsetY += pan.y
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    scale = 1f
                    offsetX = 0f
                    offsetY = 0f
                })
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = file.contentUriString ?: File(file.path),
            contentDescription = file.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
        )
    }
}

@Composable
private fun VideoPreviewPlayer(file: FileItem) {
    val context = LocalContext.current
    GlassCard(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 10f)
    ) {
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    val controller = MediaController(ctx)
                    controller.setAnchorView(this)
                    setMediaController(controller)
                    val uri = file.contentUriString?.let { Uri.parse(it) } ?: Uri.fromFile(File(file.path))
                    setVideoURI(uri)
                    setOnPreparedListener { mp ->
                        mp.isLooping = false
                        start()
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun AudioPreviewPlayer(file: FileItem) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosMs by remember { mutableIntStateOf(0) }
    var durationMs by remember { mutableIntStateOf(1) }

    val mediaPlayer = remember(file.path) {
        MediaPlayer().apply {
            runCatching {
                val uri = file.contentUriString?.let { Uri.parse(it) } ?: Uri.fromFile(File(file.path))
                setDataSource(context, uri)
                prepare()
                durationMs = duration.coerceAtLeast(1)
            }
        }
    }

    DisposableEffect(file.path) {
        mediaPlayer.setOnCompletionListener {
            isPlaying = false
            currentPosMs = 0
        }
        onDispose {
            runCatching {
                if (mediaPlayer.isPlaying) mediaPlayer.stop()
                mediaPlayer.release()
            }
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            runCatching { currentPosMs = mediaPlayer.currentPosition }
            delay(250)
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "audio_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPlaying) 1.09f else 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse_scale"
    )

    GlassCard(
        shape = RoundedCornerShape(28.dp),
        elevated = true,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(MusicPinkGlow, MusicCoral))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = file.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(18.dp))

            Slider(
                value = (currentPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f),
                onValueChange = { frac ->
                    val target = (frac * durationMs).toInt()
                    currentPosMs = target
                    runCatching { mediaPlayer.seekTo(target) }
                },
                colors = SliderDefaults.colors(
                    thumbColor = MusicCoral,
                    activeTrackColor = MusicCoral
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDurationMs(currentPosMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatDurationMs(durationMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    runCatching {
                        if (mediaPlayer.isPlaying) {
                            mediaPlayer.pause()
                            isPlaying = false
                        } else {
                            mediaPlayer.start()
                            isPlaying = true
                        }
                    }
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = MusicCoral),
                modifier = Modifier.size(64.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun PdfDocumentPreview(file: FileItem) {
    var pages by remember(file.path) { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var errorMsg by remember(file.path) { mutableStateOf<String?>(null) }
    var isLoading by remember(file.path) { mutableStateOf(true) }

    LaunchedEffect(file.path) {
        isLoading = true
        withContext(Dispatchers.IO) {
            runCatching {
                val pfd = ParcelFileDescriptor.open(File(file.path), ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                val rendered = mutableListOf<ImageBitmap>()
                val limit = renderer.pageCount.coerceAtMost(30)
                for (i in 0 until limit) {
                    val page = renderer.openPage(i)
                    val width = (page.width * 2).coerceIn(600, 1600)
                    val height = ((page.height.toFloat() / page.width.toFloat()) * width).toInt()
                    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(android.graphics.Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    rendered.add(bmp.asImageBitmap())
                }
                renderer.close()
                pfd.close()
                rendered
            }.onSuccess {
                pages = it
                isLoading = false
            }.onFailure {
                errorMsg = it.message ?: "Unable to render PDF"
                isLoading = false
            }
        }
    }

    if (isLoading) {
        CircularProgressIndicator()
    } else if (errorMsg != null) {
        Text(
            text = errorMsg!!,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(pages.size) { index ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        bitmap = pages[index],
                        contentDescription = "Page ${index + 1}",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Page ${index + 1} of ${pages.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TextDocumentPreview(target: PreviewTarget.Text) {
    if (target.isLoading) {
        CircularProgressIndicator()
    } else {
        GlassCard(
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    text = target.content.ifEmpty { "(Empty file)" },
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ArchiveZipPreview(
    target: PreviewTarget.Archive,
    onExtractZip: (FileItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        GlassCard(
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Archive,
                    contentDescription = null,
                    tint = ArchivesCyan,
                    modifier = Modifier.size(34.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val hasEncrypted = target.entries.any { it.isEncrypted }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ZIP Archive",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (hasEncrypted) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = "Password protected",
                                tint = ArchivesCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = "${target.entries.size} entries${if (hasEncrypted) " · Encrypted" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { onExtractZip(target.file) },
                    colors = ButtonDefaults.buttonColors(containerColor = ArchivesCyan),
                    shape = CircleShape
                ) {
                    Icon(Icons.Rounded.Unarchive, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Extract", color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (target.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(target.entries) { entry ->
                    GlassCard(shape = RoundedCornerShape(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (entry.isDirectory) Icons.Rounded.Folder else Icons.AutoMirrored.Rounded.InsertDriveFile,
                                contentDescription = null,
                                tint = if (entry.isDirectory) ArchivesCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = entry.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (entry.isEncrypted) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = "Encrypted entry",
                                    tint = ArchivesCyan,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            if (!entry.isDirectory && entry.sizeBytes > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = FormatUtils.formatFileSize(entry.sizeBytes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ApkInstallerPreview(
    target: PreviewTarget.Apk,
    apkIconProvider: (String) -> Drawable?,
    onInstallClick: () -> Unit
) {
    if (target.isLoading) {
        CircularProgressIndicator()
        return
    }

    val info = target.info
    val iconDrawable = remember(target.file.path) { apkIconProvider(target.file.path) }

    GlassCard(
        shape = RoundedCornerShape(28.dp),
        elevated = true,
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (iconDrawable != null) {
                AndroidView(
                    factory = { ctx ->
                        ImageView(ctx).apply {
                            setImageDrawable(iconDrawable)
                            scaleType = ImageView.ScaleType.FIT_CENTER
                        }
                    },
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(20.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(ApksEmerald.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Android,
                        contentDescription = null,
                        tint = ApksEmerald,
                        modifier = Modifier.size(46.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = info?.appName ?: target.file.name,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (info != null) {
                Text(
                    text = info.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Version ${info.versionName} (${info.versionCode}) · ${FormatUtils.formatFileSize(target.file.sizeBytes)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (target.error != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = target.error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (info != null && info.permissions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Requested Permissions (${info.permissions.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    info.permissions.take(12).forEach { perm ->
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = perm,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onInstallClick,
                colors = ButtonDefaults.buttonColors(containerColor = ApksEmerald),
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("apk_install_button")
            ) {
                Icon(Icons.Rounded.InstallMobile, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Install APK",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun GenericFilePreview(
    file: FileItem,
    onOpenExternal: () -> Unit
) {
    GlassCard(
        shape = RoundedCornerShape(26.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.InsertDriveFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = file.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${file.mimeType} · ${FormatUtils.formatFileSize(file.sizeBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onOpenExternal,
                shape = CircleShape
            ) {
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open with another app")
            }
        }
    }
}

private fun formatDurationMs(ms: Int): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format(Locale.US, "%d:%02d", min, sec)
}
