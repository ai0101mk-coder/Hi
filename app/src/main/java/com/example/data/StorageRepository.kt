package com.example.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.model.ApkInfo
import com.example.model.FileCategory
import com.example.model.FileItem
import com.example.model.OperationProgressSnapshot
import com.example.model.SortDirection
import com.example.model.SortOption
import com.example.model.SourceShortcut
import com.example.model.SourceType
import com.example.model.StorageSummary
import com.example.model.ZipCompressionLevel
import com.example.model.ZipEntryInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import net.lingala.zip4j.progress.ProgressMonitor
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.EnumMap
import java.util.Locale

class StorageRepository(private val context: Context) {

    val searchIndex = FileSearchIndex()

    val internalRootPath: String
        get() = Environment.getExternalStorageDirectory().absolutePath

    val downloadsPath: String
        get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath

    fun hasAllFilesAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasScopedMediaAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun buildAllFilesPermissionIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
    }

    fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun buildUnknownSourcesSettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
    }

    suspend fun getStorageSummary(): StorageSummary = withContext(Dispatchers.IO) {
        val root = Environment.getExternalStorageDirectory()
        val stat = StatFs(root.path)
        val total = stat.totalBytes
        val free = stat.availableBytes
        val used = (total - free).coerceAtLeast(0L)

        val byteMap = EnumMap<FileCategory, Long>(FileCategory::class.java)
        val countMap = EnumMap<FileCategory, Int>(FileCategory::class.java)

        runCatching {
            val allIndexed = getIndexedFiles(emptySet(), showHidden = false)
            for (item in allIndexed) {
                val cat = item.category ?: continue
                byteMap[cat] = (byteMap[cat] ?: 0L) + item.sizeBytes
                countMap[cat] = (countMap[cat] ?: 0) + 1
            }
        }

        val categorizedSum = byteMap.values.sum()
        val otherBytes = (used - categorizedSum).coerceAtLeast(0L)
        StorageSummary(
            totalBytes = total,
            usedBytes = used,
            freeBytes = free,
            categoryBytes = byteMap.toMap(),
            categoryCounts = countMap.toMap(),
            otherBytes = otherBytes
        )
    }

    suspend fun getCategoryCounts(showHidden: Boolean): Map<FileCategory, Int> = withContext(Dispatchers.IO) {
        val allFiles = getIndexedFiles(emptySet(), showHidden)
        FileCategory.entries.associateWith { category ->
            allFiles.count { it.category == category }
        }
    }

    suspend fun getFilesByCategory(
        category: FileCategory,
        favourites: Set<String>,
        showHidden: Boolean,
        sortOption: SortOption,
        sortDirection: SortDirection
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val filtered = getIndexedFiles(favourites, showHidden)
            .filter { it.category == category }
        sortFiles(filtered, sortOption, sortDirection)
    }

    suspend fun getRecentFiles(
        favourites: Set<String>,
        showHidden: Boolean,
        limit: Int = 150
    ): List<FileItem> = withContext(Dispatchers.IO) {
        getIndexedFiles(favourites, showHidden)
            .sortedByDescending { it.lastModifiedMillis }
            .take(limit)
    }

    suspend fun getDirectoryContents(
        path: String,
        favourites: Set<String>,
        showHidden: Boolean,
        sortOption: SortOption,
        sortDirection: SortDirection
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) return@withContext emptyList()
        val children = dir.listFiles()?.toList().orEmpty()
            .filter { file -> showHidden || !file.name.startsWith(".") }
            .map { file -> file.toFileItem(favourites) }
        sortFiles(children, sortOption, sortDirection)
    }

    suspend fun getDownloads(
        favourites: Set<String>,
        showHidden: Boolean,
        sortOption: SortOption,
        sortDirection: SortDirection
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val direct = getDirectoryContents(downloadsPath, favourites, showHidden, sortOption, sortDirection)
        if (direct.isNotEmpty() || hasAllFilesAccess()) return@withContext direct

        // Fallback via MediaStore for scoped storage if All Files Access is not granted
        val fromStore = queryAllFilesFromMediaStore(favourites, showHidden)
            .filter { it.path.contains("/Download", ignoreCase = true) }
        sortFiles(fromStore, sortOption, sortDirection)
    }

    suspend fun getFavourites(
        favourites: Set<String>,
        sortOption: SortOption,
        sortDirection: SortDirection
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val items = favourites.mapNotNull { path ->
            val file = File(path)
            if (file.exists()) file.toFileItem(favourites) else null
        }
        sortFiles(items, sortOption, sortDirection)
    }

    suspend fun getSources(showHidden: Boolean): List<SourceShortcut> = withContext(Dispatchers.IO) {
        SourceType.entries.map { type ->
            val paths = candidateSourcePaths(type).filter { File(it).exists() && File(it).isDirectory }
            val count = paths.sumOf { dirPath ->
                File(dirPath).listFiles()
                    ?.count { file -> (showHidden || !file.name.startsWith(".")) }
                    ?: 0
            }
            SourceShortcut(
                type = type,
                itemCount = count,
                resolvedPaths = paths
            )
        }
    }

    suspend fun getFilesForSource(
        sourceType: SourceType,
        favourites: Set<String>,
        showHidden: Boolean,
        sortOption: SortOption,
        sortDirection: SortDirection
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val candidateDirs = candidateSourcePaths(sourceType)
            .map { File(it) }
            .filter { it.exists() && it.isDirectory }

        val collected = linkedMapOf<String, FileItem>()
        for (dir in candidateDirs) {
            dir.walkTopDown()
                .maxDepth(4)
                .onEnter { sub -> showHidden || !sub.name.startsWith(".") }
                .filter { it.isFile && (showHidden || !it.name.startsWith(".")) }
                .forEach { file ->
                    collected.putIfAbsent(file.absolutePath, file.toFileItem(favourites))
                }
        }

        if (collected.isEmpty() && !hasAllFilesAccess()) {
            // MediaStore fallback for Camera / Screenshots / Recorder
            val all = queryAllFilesFromMediaStore(favourites, showHidden)
            all.filter { item ->
                when (sourceType) {
                    SourceType.CAMERA -> item.path.contains("/DCIM/Camera", ignoreCase = true)
                    SourceType.SCREENSHOTS ->
                        item.path.contains("Screenshots", ignoreCase = true) ||
                            item.path.contains("Screen recordings", ignoreCase = true)
                    SourceType.RECORDER ->
                        item.path.contains("Recording", ignoreCase = true) ||
                            item.path.contains("sound_recorder", ignoreCase = true)
                    SourceType.DUAL_APPS -> item.path.contains("999")
                    SourceType.TELEGRAM -> item.path.contains("Telegram", ignoreCase = true)
                }
            }.forEach { collected.putIfAbsent(it.path, it) }
        }

        sortFiles(collected.values.toList(), sortOption, sortDirection)
    }

    suspend fun rebuildSearchIndex(
        favourites: Set<String>,
        showHidden: Boolean
    ): Int = withContext(Dispatchers.IO) {
        val indexed = getIndexedFiles(favourites, showHidden, includeDirs = true)
        searchIndex.rebuild(indexed)
        searchIndex.size
    }

    suspend fun searchFiles(
        query: String,
        categoryFilter: FileCategory?,
        favourites: Set<String>,
        showHidden: Boolean,
        forceRebuild: Boolean = false
    ): SearchQueryResult = withContext(Dispatchers.IO) {
        if (forceRebuild || searchIndex.size == 0) {
            val indexed = getIndexedFiles(favourites, showHidden, includeDirs = true)
            searchIndex.rebuild(indexed)
        } else {
            searchIndex.updateFavourites(favourites)
        }
        searchIndex.search(query = query, categoryFilter = categoryFilter)
    }

    suspend fun createFolder(parentPath: String, folderName: String): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val clean = folderName.trim()
            require(clean.isNotEmpty() && !clean.contains('/')) { "Invalid folder name" }
            val target = File(parentPath, clean)
            if (target.exists()) error("A folder with this name already exists")
            if (!target.mkdirs()) error("Unable to create folder (check storage permissions)")
            target
        }
    }

    suspend fun renameFile(item: FileItem, newName: String): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val clean = newName.trim()
            require(clean.isNotEmpty() && !clean.contains('/')) { "Invalid file name" }
            val source = File(item.path)
            val parent = source.parentFile ?: error("Missing parent directory")
            val destination = File(parent, clean)
            if (destination.exists()) error("Target name already exists")
            if (!source.renameTo(destination)) error("Rename failed — ensure All Files Access is granted")
            destination
        }
    }

    suspend fun deleteFiles(
        items: List<FileItem>,
        onProgress: ((OperationProgressSnapshot) -> Unit)? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCancellableCatching {
            val existingTargets = items.map { File(it.path) }.filter { it.exists() }
            val totalBytes = computeFilesSize(existingTargets)
            val totalItems = items.size
            var deletedCount = 0
            var deletedBytes = 0L

            onProgress?.invoke(
                OperationProgressSnapshot(
                    currentItemName = items.firstOrNull()?.name.orEmpty(),
                    processedItems = 0,
                    totalItems = totalItems,
                    processedBytes = 0L,
                    totalBytes = totalBytes
                )
            )

            for (item in items) {
                currentCoroutineContext().ensureActive()
                val target = File(item.path)
                val itemBytes = computeFilesSize(listOf(target))
                onProgress?.invoke(
                    OperationProgressSnapshot(
                        currentItemName = item.name,
                        processedItems = deletedCount,
                        totalItems = totalItems,
                        processedBytes = deletedBytes,
                        totalBytes = totalBytes
                    )
                )
                if (target.exists()) {
                    val deleted = deleteRecursivelyCancellable(target)
                    if (deleted) {
                        deletedCount++
                        deletedBytes += itemBytes
                    }
                }
                onProgress?.invoke(
                    OperationProgressSnapshot(
                        currentItemName = item.name,
                        processedItems = deletedCount,
                        totalItems = totalItems,
                        processedBytes = deletedBytes,
                        totalBytes = totalBytes
                    )
                )
            }
            if (deletedCount == 0 && items.isNotEmpty()) {
                error("Could not delete selected files — check storage permission")
            }
            deletedCount
        }
    }

    suspend fun copyFiles(
        items: List<FileItem>,
        destinationDirPath: String,
        onProgress: ((OperationProgressSnapshot) -> Unit)? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCancellableCatching {
            val destDir = File(destinationDirPath)
            require(destDir.exists() && destDir.isDirectory) { "Invalid destination folder" }
            val existingFiles = items.map { File(it.path) }.filter { it.exists() }
            val totalBytes = computeFilesSize(existingFiles)
            val totalItems = items.size
            var copied = 0
            var processedBytes = 0L

            onProgress?.invoke(
                OperationProgressSnapshot(
                    currentItemName = items.firstOrNull()?.name.orEmpty(),
                    processedItems = 0,
                    totalItems = totalItems,
                    processedBytes = 0L,
                    totalBytes = totalBytes
                )
            )

            for (item in items) {
                currentCoroutineContext().ensureActive()
                val src = File(item.path)
                if (!src.exists()) continue
                val target = uniqueDestinationFile(destDir, src.name)
                if (src.isDirectory) {
                    if (target.absolutePath.startsWith(src.absolutePath + File.separator)) {
                        error("Cannot copy a folder into itself")
                    }
                    copyDirectoryWithProgress(src, target) { fileName, chunkBytes ->
                        processedBytes += chunkBytes
                        onProgress?.invoke(
                            OperationProgressSnapshot(
                                currentItemName = fileName,
                                processedItems = copied,
                                totalItems = totalItems,
                                processedBytes = processedBytes,
                                totalBytes = totalBytes
                            )
                        )
                    }
                    copied++
                } else {
                    copySingleFileWithProgress(src, target) { chunkBytes ->
                        processedBytes += chunkBytes
                        onProgress?.invoke(
                            OperationProgressSnapshot(
                                currentItemName = src.name,
                                processedItems = copied,
                                totalItems = totalItems,
                                processedBytes = processedBytes,
                                totalBytes = totalBytes
                            )
                        )
                    }
                    copied++
                }
                onProgress?.invoke(
                    OperationProgressSnapshot(
                        currentItemName = src.name,
                        processedItems = copied,
                        totalItems = totalItems,
                        processedBytes = processedBytes,
                        totalBytes = totalBytes
                    )
                )
            }
            copied
        }
    }

    suspend fun moveFiles(
        items: List<FileItem>,
        destinationDirPath: String,
        onProgress: ((OperationProgressSnapshot) -> Unit)? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCancellableCatching {
            val destDir = File(destinationDirPath)
            require(destDir.exists() && destDir.isDirectory) { "Invalid destination folder" }
            val existingFiles = items.map { File(it.path) }.filter { it.exists() }
            val totalBytes = computeFilesSize(existingFiles)
            val totalItems = items.size
            var moved = 0
            var processedBytes = 0L

            onProgress?.invoke(
                OperationProgressSnapshot(
                    currentItemName = items.firstOrNull()?.name.orEmpty(),
                    processedItems = 0,
                    totalItems = totalItems,
                    processedBytes = 0L,
                    totalBytes = totalBytes
                )
            )

            for (item in items) {
                currentCoroutineContext().ensureActive()
                val src = File(item.path)
                if (!src.exists()) continue
                if (src.parentFile?.absolutePath == destDir.absolutePath) continue
                val target = uniqueDestinationFile(destDir, src.name)
                if (src.isDirectory && target.absolutePath.startsWith(src.absolutePath + File.separator)) {
                    error("Cannot move a folder into itself")
                }

                onProgress?.invoke(
                    OperationProgressSnapshot(
                        currentItemName = src.name,
                        processedItems = moved,
                        totalItems = totalItems,
                        processedBytes = processedBytes,
                        totalBytes = totalBytes
                    )
                )

                val itemSize = computeFilesSize(listOf(src))
                if (src.renameTo(target)) {
                    moved++
                    processedBytes += itemSize
                } else {
                    if (src.isDirectory) {
                        copyDirectoryWithProgress(src, target) { fileName, chunkBytes ->
                            processedBytes += chunkBytes
                            onProgress?.invoke(
                                OperationProgressSnapshot(
                                    currentItemName = fileName,
                                    processedItems = moved,
                                    totalItems = totalItems,
                                    processedBytes = processedBytes,
                                    totalBytes = totalBytes
                                )
                            )
                        }
                        src.deleteRecursively()
                    } else {
                        copySingleFileWithProgress(src, target) { chunkBytes ->
                            processedBytes += chunkBytes
                            onProgress?.invoke(
                                OperationProgressSnapshot(
                                    currentItemName = src.name,
                                    processedItems = moved,
                                    totalItems = totalItems,
                                    processedBytes = processedBytes,
                                    totalBytes = totalBytes
                                )
                            )
                        }
                        src.delete()
                    }
                    moved++
                }
                onProgress?.invoke(
                    OperationProgressSnapshot(
                        currentItemName = src.name,
                        processedItems = moved,
                        totalItems = totalItems,
                        processedBytes = processedBytes,
                        totalBytes = totalBytes
                    )
                )
            }
            moved
        }
    }

    suspend fun readTextPreview(path: String, maxChars: Int = 60_000): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(path)
            if (!file.exists()) error("File not found")
            file.bufferedReader().use { reader ->
                val buffer = CharArray(maxChars)
                val read = reader.read(buffer)
                if (read <= 0) "" else String(buffer, 0, read)
            }
        }
    }

    suspend fun compressToZip(
        items: List<FileItem>,
        destinationDirPath: String,
        archiveName: String,
        password: String? = null,
        compressionLevel: ZipCompressionLevel = ZipCompressionLevel.NORMAL,
        onProgress: ((OperationProgressSnapshot) -> Unit)? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        runCancellableCatching {
            require(items.isNotEmpty()) { "Select at least one file or folder to compress" }
            val destDir = File(destinationDirPath)
            if (!destDir.exists()) destDir.mkdirs()
            require(destDir.exists() && destDir.isDirectory) { "Destination folder is not writable" }

            val rawName = archiveName.trim().ifBlank { "Archive" }
            require(!rawName.contains('/')) { "Archive name cannot contain /" }
            val finalName = if (rawName.lowercase(Locale.ROOT).endsWith(".zip")) rawName else "$rawName.zip"
            val outFile = uniqueDestinationFile(destDir, finalName)

            val cleanPassword = password?.takeIf { it.isNotEmpty() }
            val zipFile = if (cleanPassword != null) {
                ZipFile(outFile, cleanPassword.toCharArray())
            } else {
                ZipFile(outFile)
            }
            zipFile.isRunInThread = true

            val params = ZipParameters().apply {
                compressionMethod = CompressionMethod.DEFLATE
                this.compressionLevel = when (compressionLevel) {
                    ZipCompressionLevel.FAST -> CompressionLevel.FASTEST
                    ZipCompressionLevel.NORMAL -> CompressionLevel.NORMAL
                    ZipCompressionLevel.MAXIMUM -> CompressionLevel.MAXIMUM
                }
                if (cleanPassword != null) {
                    isEncryptFiles = true
                    encryptionMethod = EncryptionMethod.AES
                    aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
                }
            }

            val existingSources = items.map { File(it.path) }.filter { it.exists() }
            val totalBytes = computeFilesSize(existingSources).coerceAtLeast(1L)
            val totalItems = existingSources.size
            var completedItems = 0
            var completedBytes = 0L

            onProgress?.invoke(
                OperationProgressSnapshot(
                    currentItemName = finalName,
                    processedItems = 0,
                    totalItems = totalItems,
                    processedBytes = 0L,
                    totalBytes = totalBytes,
                    fractionOverride = 0f
                )
            )

            try {
                for (src in existingSources) {
                    currentCoroutineContext().ensureActive()
                    val itemBytes = computeFilesSize(listOf(src)).coerceAtLeast(1L)
                    if (src.isDirectory) {
                        zipFile.addFolder(src, params)
                    } else {
                        zipFile.addFile(src, params)
                    }
                    awaitZip4jTask(zipFile) { percent, entryName ->
                        val frac = ((completedBytes + (itemBytes * percent / 100.0)) / totalBytes.toDouble())
                            .toFloat()
                            .coerceIn(0f, 1f)
                        onProgress?.invoke(
                            OperationProgressSnapshot(
                                currentItemName = entryName.ifBlank { src.name },
                                processedItems = completedItems,
                                totalItems = totalItems,
                                processedBytes = (totalBytes * frac).toLong(),
                                totalBytes = totalBytes,
                                fractionOverride = frac
                            )
                        )
                    }
                    completedItems++
                    completedBytes += itemBytes
                }
            } catch (ce: CancellationException) {
                runCatching { zipFile.progressMonitor.isCancelAllTasks = true }
                runCatching { outFile.delete() }
                throw ce
            } catch (t: Throwable) {
                runCatching { outFile.delete() }
                throw t
            }

            outFile
        }
    }

    suspend fun isZipEncrypted(path: String): Boolean = withContext(Dispatchers.IO) {
        runCatching { ZipFile(File(path)).isEncrypted }.getOrDefault(false)
    }

    suspend fun readZipEntries(path: String): Result<List<ZipEntryInfo>> = withContext(Dispatchers.IO) {
        runCatching {
            val zip = ZipFile(File(path))
            zip.fileHeaders.take(400).map { header ->
                ZipEntryInfo(
                    name = header.fileName,
                    sizeBytes = header.uncompressedSize.coerceAtLeast(0L),
                    isDirectory = header.isDirectory,
                    isEncrypted = header.isEncrypted
                )
            }
        }
    }

    suspend fun extractZip(
        zipFileItem: FileItem,
        targetParentPath: String,
        password: String? = null,
        onProgress: ((OperationProgressSnapshot) -> Unit)? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        runCancellableCatching {
            val src = File(zipFileItem.path)
            val zip = ZipFile(src)
            if (zip.isEncrypted) {
                require(!password.isNullOrEmpty()) { "Password required to extract encrypted archive" }
                zip.setPassword(password.toCharArray())
            }

            val baseName = src.nameWithoutExtension.ifBlank { "Extracted" }
            val outDir = uniqueDestinationFile(File(targetParentPath), baseName)
            if (!outDir.mkdirs()) error("Cannot create extraction directory")
            val canonicalOut = outDir.canonicalPath

            val headers = zip.fileHeaders
            for (header in headers) {
                val outFile = File(outDir, header.fileName)
                if (!outFile.canonicalPath.startsWith(canonicalOut + File.separator) &&
                    outFile.canonicalPath != canonicalOut
                ) {
                    outDir.deleteRecursively()
                    error("Blocked unsafe archive path: ${header.fileName}")
                }
            }

            val totalUncompressed = headers.sumOf { it.uncompressedSize.coerceAtLeast(0L) }
                .let { if (it > 0L) it else src.length().coerceAtLeast(1L) }
            val totalEntries = headers.size.coerceAtLeast(1)

            onProgress?.invoke(
                OperationProgressSnapshot(
                    currentItemName = zipFileItem.name,
                    processedItems = 0,
                    totalItems = totalEntries,
                    processedBytes = 0L,
                    totalBytes = totalUncompressed,
                    fractionOverride = 0f
                )
            )

            try {
                zip.isRunInThread = true
                zip.extractAll(outDir.absolutePath)
                awaitZip4jTask(zip) { percent, entryName ->
                    val frac = (percent / 100f).coerceIn(0f, 1f)
                    onProgress?.invoke(
                        OperationProgressSnapshot(
                            currentItemName = entryName.ifBlank { zipFileItem.name },
                            processedItems = (totalEntries * frac).toInt(),
                            totalItems = totalEntries,
                            processedBytes = (totalUncompressed * frac).toLong(),
                            totalBytes = totalUncompressed,
                            fractionOverride = frac
                        )
                    )
                }
            } catch (ce: CancellationException) {
                runCatching { zip.progressMonitor.isCancelAllTasks = true }
                runCatching { outDir.deleteRecursively() }
                throw ce
            } catch (err: Throwable) {
                runCatching { outDir.deleteRecursively() }
                throw err
            }
            outDir
        }
    }

    private inline fun <T> runCancellableCatching(block: () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (ce: CancellationException) {
            throw ce
        } catch (err: Throwable) {
            Result.failure(err)
        }
    }

    private fun computeFilesSize(files: List<File>): Long {
        var total = 0L
        for (file in files) {
            if (!file.exists()) continue
            if (file.isDirectory) {
                file.walkTopDown().forEach { entry ->
                    if (entry.isFile) total += entry.length()
                }
            } else {
                total += file.length()
            }
        }
        return total
    }

    private suspend fun deleteRecursivelyCancellable(target: File): Boolean {
        currentCoroutineContext().ensureActive()
        if (target.isDirectory) {
            val children = target.listFiles().orEmpty()
            for (child in children) {
                deleteRecursivelyCancellable(child)
            }
        }
        return target.delete()
    }

    private suspend fun copyDirectoryWithProgress(
        sourceDir: File,
        destDir: File,
        onChunk: suspend (fileName: String, bytesCopied: Long) -> Unit
    ) {
        currentCoroutineContext().ensureActive()
        if (!destDir.exists()) destDir.mkdirs()
        val children = sourceDir.listFiles().orEmpty()
        for (child in children) {
            currentCoroutineContext().ensureActive()
            val targetChild = File(destDir, child.name)
            if (child.isDirectory) {
                copyDirectoryWithProgress(child, targetChild, onChunk)
            } else {
                copySingleFileWithProgress(child, targetChild) { bytes ->
                    onChunk(child.name, bytes)
                }
            }
        }
    }

    private suspend fun copySingleFileWithProgress(
        src: File,
        target: File,
        onChunk: suspend (Long) -> Unit
    ) {
        target.parentFile?.let { if (!it.exists()) it.mkdirs() }
        val buffer = ByteArray(64 * 1024)
        var completed = false
        try {
            FileInputStream(src).use { input ->
                FileOutputStream(target).use { output ->
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        if (read > 0) onChunk(read.toLong())
                    }
                    output.flush()
                }
            }
            completed = true
        } finally {
            if (!completed && target.exists()) {
                runCatching { target.delete() }
            }
        }
    }

    private suspend fun awaitZip4jTask(
        zipFile: ZipFile,
        onPoll: (percentDone: Int, fileName: String) -> Unit
    ) {
        val monitor = zipFile.progressMonitor
        try {
            while (monitor.state == ProgressMonitor.State.BUSY) {
                currentCoroutineContext().ensureActive()
                val name = monitor.fileName?.substringAfterLast('/')?.substringAfterLast('\\').orEmpty()
                onPoll(monitor.percentDone.coerceIn(0, 100), name)
                delay(60L)
            }
            currentCoroutineContext().ensureActive()
            when (monitor.result) {
                ProgressMonitor.Result.ERROR -> {
                    throw (monitor.exception ?: IllegalStateException("Archive operation failed"))
                }
                ProgressMonitor.Result.CANCELLED -> {
                    throw CancellationException("Archive operation cancelled")
                }
                else -> {
                    val name = monitor.fileName?.substringAfterLast('/')?.substringAfterLast('\\').orEmpty()
                    onPoll(100, name)
                }
            }
        } catch (ce: CancellationException) {
            monitor.isCancelAllTasks = true
            throw ce
        }
    }

    suspend fun getApkInfo(item: FileItem): Result<ApkInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val pm = context.packageManager
            val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageArchiveInfo(
                    item.path,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(item.path, PackageManager.GET_PERMISSIONS)
            } ?: error("Invalid or corrupted APK package")

            pkgInfo.applicationInfo?.apply {
                sourceDir = item.path
                publicSourceDir = item.path
            }
            val label = pkgInfo.applicationInfo?.let { pm.getApplicationLabel(it).toString() }
                ?: item.name.removeSuffix(".apk")
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.versionCode.toLong()
            }
            val perms = pkgInfo.requestedPermissions?.toList().orEmpty()
                .map { it.substringAfterLast('.') }
            ApkInfo(
                file = item,
                appName = label,
                packageName = pkgInfo.packageName.ifBlank { "unknown.package" },
                versionName = pkgInfo.versionName ?: "1.0",
                versionCode = versionCode,
                permissions = perms
            )
        }
    }

    fun getApkIconDrawable(path: String): Drawable? {
        return runCatching {
            val pm = context.packageManager
            val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageArchiveInfo(path, PackageManager.PackageInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(path, 0)
            } ?: return null
            pkgInfo.applicationInfo?.let { appInfo ->
                appInfo.sourceDir = path
                appInfo.publicSourceDir = path
                pm.getApplicationIcon(appInfo)
            }
        }.getOrNull()
    }

    fun getShareUri(item: FileItem): Uri {
        val file = File(item.path)
        return if (file.exists()) {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } else if (item.contentUriString != null) {
            Uri.parse(item.contentUriString)
        } else {
            Uri.fromFile(file)
        }
    }

    fun buildShareIntent(items: List<FileItem>): Intent {
        return if (items.size == 1) {
            val item = items.first()
            val uri = getShareUri(item)
            Intent(Intent.ACTION_SEND).apply {
                type = item.mimeType.ifBlank { "*/*" }
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            val uris = ArrayList<Uri>(items.map { getShareUri(it) })
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }

    fun buildInstallApkIntent(item: FileItem): Intent {
        val uri = getShareUri(item)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun buildOpenExternalIntent(item: FileItem): Intent {
        val uri = getShareUri(item)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, item.mimeType.ifBlank { "*/*" })
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun getIndexedFiles(
        favourites: Set<String>,
        showHidden: Boolean,
        includeDirs: Boolean = false
    ): List<FileItem> {
        val map = linkedMapOf<String, FileItem>()

        // 1. MediaStore query (fast and works with scoped media permissions too)
        queryAllFilesFromMediaStore(favourites, showHidden).forEach {
            map[it.path] = it
        }

        // 2. Direct filesystem walk when All Files Access is enabled (captures APKs, archives, docs, hidden files)
        if (hasAllFilesAccess()) {
            val root = Environment.getExternalStorageDirectory()
            if (root.exists() && root.canRead()) {
                root.walkTopDown()
                    .onEnter { dir ->
                        val name = dir.name
                        val skipRestricted = name == "Android" && dir.parentFile?.absolutePath == root.absolutePath
                        !skipRestricted && (showHidden || !name.startsWith("."))
                    }
                    .forEach { file ->
                        if (!showHidden && file.name.startsWith(".")) return@forEach
                        if (file.absolutePath == root.absolutePath) return@forEach
                        if (file.isFile || (includeDirs && file.isDirectory)) {
                            map.putIfAbsent(file.absolutePath, file.toFileItem(favourites))
                        }
                    }
            }
        }

        return map.values.toList()
    }

    private fun queryAllFilesFromMediaStore(
        favourites: Set<String>,
        showHidden: Boolean
    ): List<FileItem> {
        val result = mutableListOf<FileItem>()
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MIME_TYPE
        )
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        runCatching {
            context.contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val path = cursor.getString(dataCol) ?: continue
                    val file = File(path)
                    if (file.isDirectory) continue
                    val name = cursor.getString(nameCol) ?: file.name
                    if (!showHidden && name.startsWith(".")) continue
                    val id = cursor.getLong(idCol)
                    val size = cursor.getLong(sizeCol).let { if (it > 0) it else file.length() }
                    val dateMillis = cursor.getLong(dateCol) * 1000L
                    val ext = name.substringAfterLast('.', "")
                    val mime = cursor.getString(mimeCol) ?: FormatUtils.resolveMimeType(ext)
                    val category = FormatUtils.resolveCategory(ext, mime)
                    val contentUri = ContentUris.withAppendedId(uri, id).toString()

                    result.add(
                        FileItem(
                            id = path,
                            name = name,
                            path = path,
                            sizeBytes = size,
                            lastModifiedMillis = dateMillis,
                            isDirectory = false,
                            mimeType = mime,
                            extension = ext,
                            category = category,
                            isFavourite = path in favourites,
                            contentUriString = contentUri
                        )
                    )
                }
            }
        }
        return result
    }

    private fun candidateSourcePaths(type: SourceType): List<String> {
        val root = internalRootPath
        return when (type) {
            SourceType.CAMERA -> listOf(
                "$root/DCIM/Camera",
                "$root/DCIM/100ANDRO"
            )
            SourceType.SCREENSHOTS -> listOf(
                "$root/Pictures/Screenshots",
                "$root/DCIM/Screenshots",
                "$root/Movies/Screen recordings",
                "$root/Movies/ScreenRecorder",
                "$root/DCIM/Screen recordings",
                "$root/Pictures/Screen recordings"
            )
            SourceType.RECORDER -> listOf(
                "$root/Recordings",
                "$root/Music/Recordings",
                "$root/MIUI/sound_recorder",
                "$root/Sounds",
                "$root/Voice Recorder"
            )
            SourceType.DUAL_APPS -> listOf(
                "/storage/emulated/999",
                "$root/DualApp"
            )
            SourceType.TELEGRAM -> listOf(
                "$root/Telegram",
                "$root/Android/media/org.telegram.messenger/Telegram",
                "$root/Download/Telegram",
                "$root/Downloads/Telegram",
                "$root/Pictures/Telegram",
                "$root/Movies/Telegram"
            )
        }
    }

    private fun File.toFileItem(favourites: Set<String>): FileItem {
        val ext = if (isDirectory) "" else name.substringAfterLast('.', "")
        val mime = if (isDirectory) "inode/directory" else FormatUtils.resolveMimeType(ext)
        val count = if (isDirectory) listFiles()?.size ?: 0 else 0
        return FileItem(
            id = absolutePath,
            name = name,
            path = absolutePath,
            sizeBytes = if (isDirectory) 0L else length(),
            lastModifiedMillis = lastModified(),
            isDirectory = isDirectory,
            mimeType = mime,
            extension = ext,
            category = if (isDirectory) null else FormatUtils.resolveCategory(ext, mime),
            childCount = count,
            isFavourite = absolutePath in favourites
        )
    }

    private fun sortFiles(
        list: List<FileItem>,
        sortOption: SortOption,
        sortDirection: SortDirection
    ): List<FileItem> {
        val comparator = when (sortOption) {
            SortOption.NAME -> compareBy<FileItem> { it.name.lowercase(Locale.getDefault()) }
            SortOption.DATE -> compareBy<FileItem> { it.lastModifiedMillis }
            SortOption.SIZE -> compareBy<FileItem> { it.sizeBytes }
            SortOption.TYPE -> compareBy<FileItem> { it.extension.lowercase(Locale.getDefault()) }
                .thenBy { it.name.lowercase(Locale.getDefault()) }
        }
        val ordered = if (sortDirection == SortDirection.ASCENDING) {
            list.sortedWith(comparator)
        } else {
            list.sortedWith(comparator.reversed())
        }
        // Keep folders grouped at the top
        return ordered.sortedByDescending { it.isDirectory }
    }

    private fun uniqueDestinationFile(dir: File, originalName: String): File {
        var candidate = File(dir, originalName)
        if (!candidate.exists()) return candidate
        val dotIndex = originalName.lastIndexOf('.')
        val base = if (dotIndex > 0) originalName.substring(0, dotIndex) else originalName
        val ext = if (dotIndex > 0) originalName.substring(dotIndex) else ""
        var counter = 1
        while (candidate.exists()) {
            candidate = File(dir, "$base ($counter)$ext")
            counter++
        }
        return candidate
    }
}
