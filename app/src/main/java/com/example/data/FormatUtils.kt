package com.example.data

import android.webkit.MimeTypeMap
import com.example.model.FileCategory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

object FormatUtils {

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
            .coerceIn(0, units.lastIndex)
        val value = bytes / 1024.0.pow(digitGroups.toDouble())
        return if (digitGroups == 0) {
            "$bytes B"
        } else {
            String.format(Locale.US, "%.1f %s", value, units[digitGroups])
        }
    }

    fun formatShortDate(millis: Long): String {
        if (millis <= 0L) return ""
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun formatDateTime(millis: Long): String {
        if (millis <= 0L) return ""
        val sdf = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun recentDateBucket(millis: Long): String {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = millis }
        val sameYear = now.get(Calendar.YEAR) == target.get(Calendar.YEAR)
        val dayDiff = now.get(Calendar.DAY_OF_YEAR) - target.get(Calendar.DAY_OF_YEAR)
        return when {
            sameYear && dayDiff == 0 -> "Today"
            sameYear && dayDiff == 1 -> "Yesterday"
            else -> "Earlier"
        }
    }

    fun formatRelativeTime(millis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        if (millis <= 0L) return ""
        val diff = (nowMillis - millis).coerceAtLeast(0L)
        val minuteMs = 60_000L
        val hourMs = 3_600_000L
        val dayMs = 86_400_000L
        return when {
            diff < minuteMs -> "Just now"
            diff < hourMs -> "${diff / minuteMs}m ago"
            diff < dayMs -> "${diff / hourMs}h ago"
            diff < 2 * dayMs -> "Yesterday"
            else -> formatShortDate(millis)
        }
    }

    fun resolveCategory(extension: String, mimeType: String = ""): FileCategory? {
        val ext = extension.lowercase(Locale.ROOT)
        FileCategory.entries.firstOrNull { ext in it.extensions }?.let { return it }
        return when {
            mimeType.startsWith("image/") -> FileCategory.IMAGES
            mimeType.startsWith("video/") -> FileCategory.VIDEOS
            mimeType.startsWith("audio/") -> FileCategory.MUSIC
            mimeType == "application/vnd.android.package-archive" -> FileCategory.APKS
            mimeType.contains("zip") || mimeType.contains("rar") || mimeType.contains("7z") || mimeType.contains("tar") ->
                FileCategory.ARCHIVES
            mimeType.startsWith("text/") || mimeType == "application/pdf" -> FileCategory.DOCS
            else -> null
        }
    }

    fun resolveMimeType(extension: String): String {
        val ext = extension.lowercase(Locale.ROOT)
        if (ext == "apk") return "application/vnd.android.package-archive"
        if (ext == "md" || ext == "kt" || ext == "json" || ext == "log") return "text/plain"
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }

    fun isTextPreviewable(extension: String, mimeType: String): Boolean {
        val ext = extension.lowercase(Locale.ROOT)
        return ext in setOf("txt", "md", "log", "json", "xml", "csv", "kt", "java", "py", "js", "ts", "html", "css", "yaml", "yml", "ini", "prop", "sh") ||
            mimeType.startsWith("text/")
    }
}
