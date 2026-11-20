package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.ApksEmerald
import com.example.ui.theme.ApksGreenGlow
import com.example.ui.theme.ArchivesCyan
import com.example.ui.theme.ArchivesSkyGlow
import com.example.ui.theme.DocsAmber
import com.example.ui.theme.DocsAmberGlow
import com.example.ui.theme.ImagesBlueGlow
import com.example.ui.theme.ImagesIndigo
import com.example.ui.theme.MusicCoral
import com.example.ui.theme.MusicPinkGlow
import com.example.ui.theme.SourceCameraSlate
import com.example.ui.theme.SourceDualAppsLime
import com.example.ui.theme.SourceRecorderCharcoal
import com.example.ui.theme.SourceScreenshotsViolet
import com.example.ui.theme.SourceTelegramSky
import com.example.ui.theme.VideosPurpleGlow
import com.example.ui.theme.VideosViolet

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK
}

enum class FileCategory(
    val label: String,
    val primaryColor: Color,
    val glowColor: Color,
    val extensions: Set<String>
) {
    DOCS(
        label = "Docs",
        primaryColor = DocsAmber,
        glowColor = DocsAmberGlow,
        extensions = setOf(
            "pdf", "doc", "docx", "txt", "md", "rtf", "csv",
            "xls", "xlsx", "ppt", "pptx", "odt", "ods", "json", "xml", "log", "kt", "java", "py", "js", "html"
        )
    ),
    IMAGES(
        label = "Images",
        primaryColor = ImagesIndigo,
        glowColor = ImagesBlueGlow,
        extensions = setOf("jpg", "jpeg", "png", "webp", "gif", "heic", "heif", "bmp", "svg", "dng")
    ),
    VIDEOS(
        label = "Videos",
        primaryColor = VideosViolet,
        glowColor = VideosPurpleGlow,
        extensions = setOf("mp4", "mkv", "mov", "webm", "avi", "3gp", "m4v", "ts")
    ),
    MUSIC(
        label = "Music",
        primaryColor = MusicCoral,
        glowColor = MusicPinkGlow,
        extensions = setOf("mp3", "wav", "flac", "m4a", "ogg", "aac", "opus", "amr", "wma")
    ),
    ARCHIVES(
        label = "Archives",
        primaryColor = ArchivesCyan,
        glowColor = ArchivesSkyGlow,
        extensions = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz", "tgz")
    ),
    APKS(
        label = "APKs",
        primaryColor = ApksEmerald,
        glowColor = ApksGreenGlow,
        extensions = setOf("apk", "apks", "xapk")
    )
}

enum class SourceType(
    val label: String,
    val accentColor: Color
) {
    CAMERA("Camera", SourceCameraSlate),
    SCREENSHOTS("Screenshots & screen recordings", SourceScreenshotsViolet),
    RECORDER("Recorder", SourceRecorderCharcoal),
    DUAL_APPS("Dual apps", SourceDualAppsLime),
    TELEGRAM("Telegram", SourceTelegramSky)
}

data class SourceShortcut(
    val type: SourceType,
    val itemCount: Int,
    val resolvedPaths: List<String>
)

data class FileItem(
    val id: String,
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val lastModifiedMillis: Long,
    val isDirectory: Boolean,
    val mimeType: String,
    val extension: String,
    val category: FileCategory?,
    val childCount: Int = 0,
    val isFavourite: Boolean = false,
    val contentUriString: String? = null
)

data class StorageSegment(
    val key: String,
    val label: String,
    val bytes: Long,
    val itemCount: Int,
    val fractionOfTotal: Float,
    val fractionOfUsed: Float,
    val primaryColor: Color,
    val glowColor: Color,
    val category: FileCategory? = null
)

data class StorageSummary(
    val totalBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val categoryBytes: Map<FileCategory, Long> = emptyMap(),
    val categoryCounts: Map<FileCategory, Int> = emptyMap(),
    val otherBytes: Long = 0L
) {
    val usedFraction: Float
        get() = if (totalBytes > 0L) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

    fun toUsedSegments(): List<StorageSegment> {
        val total = totalBytes.coerceAtLeast(1L).toFloat()
        val used = usedBytes.coerceAtLeast(1L).toFloat()
        val catSegments = FileCategory.entries.mapNotNull { cat ->
            val bytes = categoryBytes[cat] ?: 0L
            val count = categoryCounts[cat] ?: 0
            if (bytes <= 0L && count == 0) null
            else StorageSegment(
                key = cat.name,
                label = cat.label,
                bytes = bytes,
                itemCount = count,
                fractionOfTotal = (bytes / total).coerceIn(0f, 1f),
                fractionOfUsed = (bytes / used).coerceIn(0f, 1f),
                primaryColor = cat.primaryColor,
                glowColor = cat.glowColor,
                category = cat
            )
        }.sortedByDescending { it.bytes }

        val otherSegment = if (otherBytes > 0L) {
            listOf(
                StorageSegment(
                    key = "OTHER",
                    label = "System & Other",
                    bytes = otherBytes,
                    itemCount = 0,
                    fractionOfTotal = (otherBytes / total).coerceIn(0f, 1f),
                    fractionOfUsed = (otherBytes / used).coerceIn(0f, 1f),
                    primaryColor = Color(0xFF64748B),
                    glowColor = Color(0xFF94A3B8),
                    category = null
                )
            )
        } else {
            emptyList()
        }

        return catSegments + otherSegment
    }
}

enum class SortOption(
    val label: String,
    val shortLabel: String,
    val defaultDirection: SortDirection
) {
    NAME("Name", "Name", SortDirection.ASCENDING),
    DATE("Date modified", "Date", SortDirection.DESCENDING),
    SIZE("Size", "Size", SortDirection.DESCENDING),
    TYPE("Type", "Type", SortDirection.ASCENDING)
}

enum class SortDirection {
    ASCENDING,
    DESCENDING
}

enum class BrowseMode {
    CATEGORY,
    DIRECTORY,
    DOWNLOADS,
    FAVOURITES,
    SOURCE
}

data class BrowserDestination(
    val mode: BrowseMode,
    val title: String,
    val category: FileCategory? = null,
    val sourceType: SourceType? = null,
    val directoryPath: String? = null
)

data class ApkInfo(
    val file: FileItem,
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val permissions: List<String>
)

enum class ZipCompressionLevel(val label: String) {
    FAST("Fast"),
    NORMAL("Normal"),
    MAXIMUM("Maximum")
}

data class ZipEntryInfo(
    val name: String,
    val sizeBytes: Long,
    val isDirectory: Boolean,
    val isEncrypted: Boolean = false
)

data class BulkSelectionSummary(
    val totalCount: Int = 0,
    val fileCount: Int = 0,
    val folderCount: Int = 0,
    val totalBytes: Long = 0L,
    val allAreFavourites: Boolean = false,
    val hasShareableFiles: Boolean = false
) {
    fun formatBreakdown(): String {
        if (totalCount == 0) return "No items selected"
        val parts = buildList {
            if (fileCount > 0) add("$fileCount file${if (fileCount == 1) "" else "s"}")
            if (folderCount > 0) add("$folderCount folder${if (folderCount == 1) "" else "s"}")
        }
        return parts.joinToString(", ")
    }

    companion object {
        fun from(
            items: List<FileItem>,
            favouritePaths: Set<String> = emptySet()
        ): BulkSelectionSummary {
            if (items.isEmpty()) return BulkSelectionSummary()
            var files = 0
            var folders = 0
            var bytes = 0L
            var favs = 0
            items.forEach { item ->
                if (item.isDirectory) folders++ else files++
                bytes += item.sizeBytes.coerceAtLeast(0L)
                if (item.isFavourite || item.path in favouritePaths) favs++
            }
            return BulkSelectionSummary(
                totalCount = items.size,
                fileCount = files,
                folderCount = folders,
                totalBytes = bytes,
                allAreFavourites = favs == items.size,
                hasShareableFiles = files > 0
            )
        }
    }
}

enum class ActivityFilterGroup(val label: String) {
    MOVES("Moves"),
    COPIES("Copies"),
    DELETES("Deletes"),
    ARCHIVES("Archives"),
    ORGANIZE("Folders & Renames")
}

enum class FileOperationType(
    val label: String,
    val accentColor: Color,
    val filterGroup: ActivityFilterGroup
) {
    MOVE("Moved", ImagesIndigo, ActivityFilterGroup.MOVES),
    COPY("Copied", VideosViolet, ActivityFilterGroup.COPIES),
    DELETE("Deleted", MusicCoral, ActivityFilterGroup.DELETES),
    COMPRESS("Archive Created", ArchivesCyan, ActivityFilterGroup.ARCHIVES),
    EXTRACT("Extracted", Color(0xFF06B6D4), ActivityFilterGroup.ARCHIVES),
    RENAME("Renamed", DocsAmber, ActivityFilterGroup.ORGANIZE),
    CREATE_FOLDER("New Folder", ApksEmerald, ActivityFilterGroup.ORGANIZE)
}

data class FileActivityEvent(
    val id: String,
    val type: FileOperationType,
    val title: String,
    val detail: String,
    val targetFolderPath: String,
    val itemCount: Int = 1,
    val totalBytes: Long = 0L,
    val timestampMillis: Long = System.currentTimeMillis()
)

enum class OperationPhase {
    RUNNING,
    COMPLETED,
    CANCELLED,
    FAILED
}

data class OperationProgressSnapshot(
    val currentItemName: String = "",
    val processedItems: Int = 0,
    val totalItems: Int = 0,
    val processedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val fractionOverride: Float? = null
)

data class OperationProgress(
    val id: String,
    val operationType: FileOperationType,
    val phase: OperationPhase = OperationPhase.RUNNING,
    val title: String,
    val statusDetail: String = "",
    val currentItemName: String = "",
    val destinationLabel: String = "",
    val processedItems: Int = 0,
    val totalItems: Int = 0,
    val processedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val fractionOverride: Float? = null
) {
    val progressFraction: Float
        get() = when {
            phase == OperationPhase.COMPLETED -> 1f
            fractionOverride != null -> fractionOverride.coerceIn(0f, 1f)
            totalBytes > 0L -> (processedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
            totalItems > 0 -> (processedItems.toFloat() / totalItems.toFloat()).coerceIn(0f, 1f)
            else -> 0f
        }
}




