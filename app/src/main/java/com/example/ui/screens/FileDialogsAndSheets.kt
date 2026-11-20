package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.FormatUtils
import com.example.model.FileItem
import com.example.model.ZipCompressionLevel
import com.example.ui.components.FileThumbnailOrBadge
import com.example.ui.theme.ArchivesCyan
import com.example.ui.theme.DocsAmber
import com.example.ui.theme.MusicCoral
import com.example.viewmodel.PendingTransfer
import com.example.viewmodel.TransferOperation
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileActionSheet(
    item: FileItem,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onToggleFavourite: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onRename: () -> Unit,
    onCompress: () -> Unit,
    onExtract: () -> Unit,
    onInstallApk: () -> Unit,
    onProperties: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FileThumbnailOrBadge(item = item, size = 48.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (item.isDirectory) "${item.childCount} items" else FormatUtils.formatFileSize(item.sizeBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            SheetActionRow(
                icon = Icons.AutoMirrored.Rounded.OpenInNew,
                label = if (item.isDirectory) "Open folder" else "Preview / Open",
                onClick = { onDismiss(); onOpen() }
            )

            if (item.extension.equals("apk", ignoreCase = true)) {
                SheetActionRow(
                    icon = Icons.Rounded.InstallMobile,
                    label = "Install APK",
                    onClick = { onDismiss(); onInstallApk() }
                )
            }

            if (item.extension.equals("zip", ignoreCase = true)) {
                SheetActionRow(
                    icon = Icons.Rounded.Unarchive,
                    label = "Extract here",
                    onClick = { onDismiss(); onExtract() }
                )
            }

            SheetActionRow(
                icon = if (item.isFavourite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                label = if (item.isFavourite) "Remove from Favourites" else "Add to Favourites",
                tint = if (item.isFavourite) MusicCoral else MaterialTheme.colorScheme.onSurface,
                onClick = { onDismiss(); onToggleFavourite() }
            )

            if (!item.isDirectory) {
                SheetActionRow(
                    icon = Icons.Rounded.Share,
                    label = "Share",
                    onClick = { onDismiss(); onShare() }
                )
            }

            SheetActionRow(
                icon = Icons.Rounded.ContentCopy,
                label = "Copy to…",
                onClick = { onDismiss(); onCopy() }
            )

            SheetActionRow(
                icon = Icons.AutoMirrored.Rounded.DriveFileMove,
                label = "Move to…",
                onClick = { onDismiss(); onMove() }
            )

            SheetActionRow(
                icon = Icons.Rounded.DriveFileRenameOutline,
                label = "Rename",
                onClick = { onDismiss(); onRename() }
            )

            SheetActionRow(
                icon = Icons.Rounded.Archive,
                label = "Compress to ZIP",
                onClick = { onDismiss(); onCompress() }
            )

            SheetActionRow(
                icon = Icons.Rounded.Info,
                label = "Properties",
                onClick = { onDismiss(); onProperties() }
            )

            SheetActionRow(
                icon = Icons.Rounded.Delete,
                label = "Delete",
                tint = Color(0xFFF43F5E),
                onClick = { onDismiss(); onDelete() }
            )
        }
    }
}

@Composable
private fun SheetActionRow(
    icon: ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = tint)
    }
}

@Composable
fun RenameDialog(
    item: FileItem,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember(item.path) { mutableStateOf(item.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("File or folder name") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank() && name != item.name
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Folder") },
        text = {
            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it },
                singleLine = true,
                label = { Text("Folder name") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(folderName) },
                enabled = folderName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun DeleteConfirmDialog(
    items: List<FileItem>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val summary = remember(items) { com.example.model.BulkSelectionSummary.from(items) }
    val title = if (items.size == 1) {
        "Delete \"${items.first().name}\"?"
    } else {
        "Batch Delete ${items.size} items?"
    }
    val sizeText = FormatUtils.formatFileSize(summary.totalBytes)
    val previewNames = remember(items) {
        val shown = items.take(3).joinToString(", ") { it.name }
        if (items.size > 3) "$shown, and ${items.size - 3} more" else shown
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (items.size == 1) {
                        "This permanently removes this item ($sizeText) from device storage."
                    } else {
                        "This permanently removes ${summary.formatBreakdown()} ($sizeText total) from device storage."
                    }
                )
                if (items.size > 1) {
                    Text(
                        text = previewNames,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E)),
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text(
                    text = if (items.size > 1) "Delete All (${items.size})" else "Delete",
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun FilePropertiesDialog(
    item: FileItem,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Properties") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PropertyLine("Name", item.name)
                PropertyLine("Type", if (item.isDirectory) "Folder" else item.mimeType)
                PropertyLine("Size", if (item.isDirectory) "${item.childCount} items" else FormatUtils.formatFileSize(item.sizeBytes))
                PropertyLine("Modified", FormatUtils.formatDateTime(item.lastModifiedMillis))
                PropertyLine("Path", item.path)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun PropertyLine(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderPickerSheet(
    transfer: PendingTransfer,
    folders: List<FileItem>,
    rootPath: String,
    onNavigateInto: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onConfirmHere: () -> Unit,
    onDismiss: () -> Unit
) {
    val verb = if (transfer.operation == TransferOperation.COPY) "Copy" else "Move"
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (transfer.currentPickerPath != rootPath) {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Parent folder")
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$verb ${transfer.items.size} item${if (transfer.items.size == 1) "" else "s"} to",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = transfer.currentPickerPath.removePrefix(rootPath).ifBlank { "/Internal Storage" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp, max = 320.dp)
            ) {
                if (folders.isEmpty()) {
                    item {
                        Text(
                            text = "No subfolders here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                } else {
                    items(folders, key = { it.path }) { folder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateInto(folder.path) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Folder,
                                contentDescription = null,
                                tint = DocsAmber,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = folder.name,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = onConfirmHere,
                    shape = CircleShape,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("$verb Here")
                }
            }
        }
    }
}

@Composable
fun CompressToZipDialog(
    items: List<FileItem>,
    destinationDirPath: String,
    onDismiss: () -> Unit,
    onConfirm: (archiveName: String, password: String?, level: ZipCompressionLevel) -> Unit
) {
    val defaultName = remember(items) {
        if (items.size == 1) {
            val single = items.first().name
            val base = if (items.first().isDirectory) single else single.substringBeforeLast('.', single)
            base.ifBlank { "Archive" }
        } else {
            val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
            "Archive_$stamp"
        }
    }

    var archiveName by remember(items) { mutableStateOf(defaultName) }
    var compressionLevel by remember { mutableStateOf(ZipCompressionLevel.NORMAL) }
    var usePassword by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    val destFolderLabel = remember(destinationDirPath) {
        File(destinationDirPath).name.ifBlank { "Internal Storage" }
    }
    val canConfirm = archiveName.isNotBlank() && (!usePassword || password.isNotEmpty())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Archive,
                    contentDescription = null,
                    tint = ArchivesCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Compress to ZIP")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${items.size} item${if (items.size == 1) "" else "s"} · Saving to $destFolderLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = archiveName,
                    onValueChange = { archiveName = it },
                    singleLine = true,
                    label = { Text("Archive name") },
                    suffix = { Text(".zip") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("compress_name_input")
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Compression level",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ZipCompressionLevel.entries.forEach { level ->
                            val selected = compressionLevel == level
                            FilterChip(
                                selected = selected,
                                onClick = { compressionLevel = level },
                                label = { Text(level.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ArchivesCyan,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = if (usePassword) ArchivesCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Password protection",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "AES-256 encryption",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = usePassword,
                        onCheckedChange = { usePassword = it },
                        modifier = Modifier.testTag("compress_password_switch")
                    )
                }

                if (usePassword) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        singleLine = true,
                        label = { Text("Archive password") },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = if (showPassword) "Hide password" else "Show password"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("compress_password_input")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        archiveName,
                        if (usePassword) password else null,
                        compressionLevel
                    )
                },
                enabled = canConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = ArchivesCyan),
                modifier = Modifier.testTag("compress_confirm_button")
            ) {
                Text("Compress", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ZipPasswordDialog(
    archiveName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = ArchivesCyan,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Encrypted Archive")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Enter the password to extract \"$archiveName\".",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    singleLine = true,
                    label = { Text("Password") },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (showPassword) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(password) },
                enabled = password.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = ArchivesCyan)
            ) {
                Text("Extract", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

