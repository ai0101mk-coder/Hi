package com.example.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FormatUtils
import com.example.data.PreferencesRepository
import com.example.data.StorageRepository
import com.example.model.ApkInfo
import com.example.model.BrowseMode
import com.example.model.BrowserDestination
import com.example.model.FileActivityEvent
import com.example.model.FileCategory
import com.example.model.FileItem
import com.example.model.FileOperationType
import com.example.model.OperationPhase
import com.example.model.OperationProgress
import com.example.model.OperationProgressSnapshot
import com.example.model.SortDirection
import com.example.model.SortOption
import com.example.model.SourceShortcut
import com.example.model.SourceType
import com.example.model.StorageSummary
import com.example.model.ThemePreference
import com.example.model.ZipCompressionLevel
import com.example.model.ZipEntryInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

sealed interface PreviewTarget {
    val file: FileItem

    data class Image(override val file: FileItem) : PreviewTarget
    data class Video(override val file: FileItem) : PreviewTarget
    data class Audio(override val file: FileItem) : PreviewTarget
    data class Pdf(override val file: FileItem) : PreviewTarget
    data class Text(override val file: FileItem, val content: String, val isLoading: Boolean = false) : PreviewTarget
    data class Archive(override val file: FileItem, val entries: List<ZipEntryInfo>, val isLoading: Boolean = false) : PreviewTarget
    data class Apk(override val file: FileItem, val info: ApkInfo?, val isLoading: Boolean = false, val error: String? = null) : PreviewTarget
    data class Generic(override val file: FileItem) : PreviewTarget
}

enum class TransferOperation { COPY, MOVE }

data class PendingTransfer(
    val operation: TransferOperation,
    val items: List<FileItem>,
    val currentPickerPath: String
)

data class DashboardUiState(
    val hasAllFilesAccess: Boolean = false,
    val hasScopedAccess: Boolean = false,
    val isLoading: Boolean = true,
    val storageSummary: StorageSummary = StorageSummary(),
    val categoryCounts: Map<FileCategory, Int> = emptyMap(),
    val downloadsCount: Int = 0,
    val favouritesCount: Int = 0,
    val sources: List<SourceShortcut> = emptyList(),
    val recentFiles: List<FileItem> = emptyList()
)

data class BrowserUiState(
    val destination: BrowserDestination? = null,
    val currentDirectoryPath: String = "",
    val breadcrumbs: List<Pair<String, String>> = emptyList(),
    val files: List<FileItem> = emptyList(),
    val isLoading: Boolean = false,
    val isGridView: Boolean = false
)

data class SearchUiState(
    val query: String = "",
    val selectedCategory: FileCategory? = null,
    val results: List<FileItem> = emptyList(),
    val categoryHitCounts: Map<FileCategory, Int> = emptyMap(),
    val indexedTotalCount: Int = 0,
    val recentQueries: List<String> = emptyList(),
    val isSearching: Boolean = false
)

class FileExplorerViewModel(application: Application) : AndroidViewModel(application) {

    val storageRepository = StorageRepository(application)
    private val prefsRepository = PreferencesRepository(application)

    val themePreference: StateFlow<ThemePreference> = prefsRepository.themePreference
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemePreference.SYSTEM)

    val favouritePaths: StateFlow<Set<String>> = prefsRepository.favouritePaths
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val showHiddenFiles: StateFlow<Boolean> = prefsRepository.showHiddenFiles
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val sortOption: StateFlow<SortOption> = prefsRepository.sortOption
        .stateIn(viewModelScope, SharingStarted.Eagerly, SortOption.DATE)

    val sortDirection: StateFlow<SortDirection> = prefsRepository.sortDirection
        .stateIn(viewModelScope, SharingStarted.Eagerly, SortDirection.DESCENDING)

    val activityEvents: StateFlow<List<FileActivityEvent>> = prefsRepository.activityEvents
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _dashboardState = MutableStateFlow(DashboardUiState())
    val dashboardState: StateFlow<DashboardUiState> = _dashboardState.asStateFlow()

    private val _browserState = MutableStateFlow(BrowserUiState())
    val browserState: StateFlow<BrowserUiState> = _browserState.asStateFlow()

    private val _searchState = MutableStateFlow(SearchUiState())
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    private val _selectedPaths = MutableStateFlow<Set<String>>(emptySet())
    val selectedPaths: StateFlow<Set<String>> = _selectedPaths.asStateFlow()

    private val _activePreview = MutableStateFlow<PreviewTarget?>(null)
    val activePreview: StateFlow<PreviewTarget?> = _activePreview.asStateFlow()

    private val _pendingTransfer = MutableStateFlow<PendingTransfer?>(null)
    val pendingTransfer: StateFlow<PendingTransfer?> = _pendingTransfer.asStateFlow()

    private val _pickerFolders = MutableStateFlow<List<FileItem>>(emptyList())
    val pickerFolders: StateFlow<List<FileItem>> = _pickerFolders.asStateFlow()

    private val _operationProgress = MutableStateFlow<OperationProgress?>(null)
    val operationProgress: StateFlow<OperationProgress?> = _operationProgress.asStateFlow()

    private val _pendingPasswordArchive = MutableStateFlow<FileItem?>(null)
    val pendingPasswordArchive: StateFlow<FileItem?> = _pendingPasswordArchive.asStateFlow()

    private var activeOperationJob: Job? = null
    private var progressDismissJob: Job? = null
    private var lastProgressUiUpdateMs: Long = 0L

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessages = _snackbarMessages.asSharedFlow()

    private val _launchIntentEvents = MutableSharedFlow<Intent>(extraBufferCapacity = 4)
    val launchIntentEvents = _launchIntentEvents.asSharedFlow()

    init {
        refreshAll()
        viewModelScope.launch {
            combine(favouritePaths, showHiddenFiles, sortOption, sortDirection) { _, _, _, _ -> Unit }
                .collect {
                    refreshDashboard()
                    reloadCurrentBrowser()
                }
        }
    }

    fun refreshAll() {
        refreshPermissionsState()
        refreshDashboard()
        reloadCurrentBrowser()
    }

    fun refreshPermissionsState() {
        _dashboardState.update {
            it.copy(
                hasAllFilesAccess = storageRepository.hasAllFilesAccess(),
                hasScopedAccess = storageRepository.hasScopedMediaAccess()
            )
        }
    }

    fun refreshDashboard() {
        viewModelScope.launch {
            val allFilesAccess = storageRepository.hasAllFilesAccess()
            val scopedAccess = storageRepository.hasScopedMediaAccess()
            val hidden = showHiddenFiles.first()
            val favs = favouritePaths.first()
            val summary = storageRepository.getStorageSummary()
            val counts = storageRepository.getCategoryCounts(hidden)
            val sources = storageRepository.getSources(hidden)
            val downloads = storageRepository.getDownloads(favs, hidden, SortOption.DATE, SortDirection.DESCENDING)
            val recent = storageRepository.getRecentFiles(favs, hidden)

            _dashboardState.value = DashboardUiState(
                hasAllFilesAccess = allFilesAccess,
                hasScopedAccess = scopedAccess,
                isLoading = false,
                storageSummary = summary,
                categoryCounts = counts,
                downloadsCount = downloads.size,
                favouritesCount = favs.count { File(it).exists() },
                sources = sources,
                recentFiles = recent
            )

            val indexedCount = storageRepository.rebuildSearchIndex(favs, hidden)
            _searchState.update { it.copy(indexedTotalCount = indexedCount) }
        }
    }

    fun cycleDarkMode(currentlyDark: Boolean) {
        viewModelScope.launch {
            val next = if (currentlyDark) ThemePreference.LIGHT else ThemePreference.DARK
            prefsRepository.setThemePreference(next)
        }
    }

    fun setThemePreference(preference: ThemePreference) {
        viewModelScope.launch {
            prefsRepository.setThemePreference(preference)
        }
    }

    fun toggleShowHiddenFiles() {
        viewModelScope.launch {
            prefsRepository.setShowHiddenFiles(!showHiddenFiles.value)
        }
    }

    fun selectSortOption(option: SortOption) {
        val nextDirection = if (sortOption.value == option) {
            if (sortDirection.value == SortDirection.ASCENDING) {
                SortDirection.DESCENDING
            } else {
                SortDirection.ASCENDING
            }
        } else {
            option.defaultDirection
        }
        setSort(option, nextDirection)
    }

    fun setSort(option: SortOption, direction: SortDirection) {
        viewModelScope.launch {
            prefsRepository.setSort(option, direction)
        }
    }

    fun toggleGridView() {
        _browserState.update { it.copy(isGridView = !it.isGridView) }
    }

    fun openCategory(category: FileCategory) {
        _selectedPaths.value = emptySet()
        val dest = BrowserDestination(
            mode = BrowseMode.CATEGORY,
            title = category.label,
            category = category
        )
        loadDestination(dest)
    }

    fun openDownloads() {
        _selectedPaths.value = emptySet()
        val dest = BrowserDestination(
            mode = BrowseMode.DOWNLOADS,
            title = "Downloads",
            directoryPath = storageRepository.downloadsPath
        )
        loadDestination(dest)
    }

    fun openFavourites() {
        _selectedPaths.value = emptySet()
        val dest = BrowserDestination(
            mode = BrowseMode.FAVOURITES,
            title = "Favourites"
        )
        loadDestination(dest)
    }

    fun openInternalStorage() {
        _selectedPaths.value = emptySet()
        val root = storageRepository.internalRootPath
        val dest = BrowserDestination(
            mode = BrowseMode.DIRECTORY,
            title = "My Phone",
            directoryPath = root
        )
        loadDestination(dest)
    }

    fun openSource(sourceType: SourceType) {
        _selectedPaths.value = emptySet()
        val dest = BrowserDestination(
            mode = BrowseMode.SOURCE,
            title = sourceType.label,
            sourceType = sourceType
        )
        loadDestination(dest)
    }

    fun navigateToDirectory(path: String) {
        _selectedPaths.value = emptySet()
        val currentDest = _browserState.value.destination
        val dest = if (currentDest?.mode == BrowseMode.DIRECTORY) {
            currentDest.copy(directoryPath = path)
        } else {
            BrowserDestination(
                mode = BrowseMode.DIRECTORY,
                title = File(path).name.ifBlank { "My Phone" },
                directoryPath = path
            )
        }
        loadDestination(dest)
    }

    fun navigateUpDirectory(): Boolean {
        val state = _browserState.value
        val dest = state.destination ?: return false
        if (dest.mode != BrowseMode.DIRECTORY) return false
        val root = storageRepository.internalRootPath
        val current = state.currentDirectoryPath
        if (current.isBlank() || current == root) return false
        val parent = File(current).parentFile?.absolutePath ?: return false
        if (!parent.startsWith(root)) return false
        navigateToDirectory(parent)
        return true
    }

    private fun loadDestination(dest: BrowserDestination) {
        viewModelScope.launch {
            val dirPath = dest.directoryPath ?: storageRepository.internalRootPath
            _browserState.update {
                it.copy(
                    destination = dest,
                    currentDirectoryPath = dirPath,
                    breadcrumbs = buildBreadcrumbs(dirPath),
                    isLoading = true
                )
            }
            val favs = favouritePaths.value
            val hidden = showHiddenFiles.value
            val sort = sortOption.value
            val dir = sortDirection.value

            val files = when (dest.mode) {
                BrowseMode.CATEGORY -> storageRepository.getFilesByCategory(
                    category = dest.category ?: FileCategory.DOCS,
                    favourites = favs,
                    showHidden = hidden,
                    sortOption = sort,
                    sortDirection = dir
                )
                BrowseMode.DIRECTORY -> storageRepository.getDirectoryContents(
                    path = dirPath,
                    favourites = favs,
                    showHidden = hidden,
                    sortOption = sort,
                    sortDirection = dir
                )
                BrowseMode.DOWNLOADS -> storageRepository.getDownloads(favs, hidden, sort, dir)
                BrowseMode.FAVOURITES -> storageRepository.getFavourites(favs, sort, dir)
                BrowseMode.SOURCE -> storageRepository.getFilesForSource(
                    sourceType = dest.sourceType ?: SourceType.CAMERA,
                    favourites = favs,
                    showHidden = hidden,
                    sortOption = sort,
                    sortDirection = dir
                )
            }
            _browserState.update {
                it.copy(
                    files = files,
                    isLoading = false
                )
            }
        }
    }

    fun reloadCurrentBrowser() {
        val dest = _browserState.value.destination ?: return
        loadDestination(dest)
    }

    private fun buildBreadcrumbs(path: String): List<Pair<String, String>> {
        val root = storageRepository.internalRootPath
        if (!path.startsWith(root)) return listOf("My Phone" to root)
        val relative = path.removePrefix(root).trim('/')
        val result = mutableListOf("My Phone" to root)
        if (relative.isNotEmpty()) {
            var acc = root
            for (segment in relative.split('/')) {
                acc = "$acc/$segment"
                result.add(segment to acc)
            }
        }
        return result
    }

    // Global Indexed Search
    fun openGlobalSearch() {
        val currentCount = storageRepository.searchIndex.size
        if (currentCount == 0) {
            executeSearch(forceRebuild = true)
        } else {
            _searchState.update { it.copy(indexedTotalCount = currentCount) }
            executeSearch(forceRebuild = false)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchState.update { it.copy(query = query) }
        executeSearch(forceRebuild = false)
    }

    fun triggerSearchSubmit() {
        val trimmed = _searchState.value.query.trim()
        if (trimmed.isNotEmpty()) {
            _searchState.update { state ->
                val updated = (listOf(trimmed) + state.recentQueries.filterNot {
                    it.equals(trimmed, ignoreCase = true)
                }).take(5)
                state.copy(recentQueries = updated)
            }
        }
        executeSearch(forceRebuild = storageRepository.searchIndex.size == 0)
    }

    fun refreshSearchIndex() {
        executeSearch(forceRebuild = true)
    }

    fun toggleSearchCategory(category: FileCategory?) {
        val next = if (_searchState.value.selectedCategory == category) null else category
        _searchState.update { it.copy(selectedCategory = next) }
        executeSearch(forceRebuild = false)
    }

    fun executeSearch(forceRebuild: Boolean = false) {
        viewModelScope.launch {
            val state = _searchState.value
            if (forceRebuild || storageRepository.searchIndex.size == 0) {
                _searchState.update { it.copy(isSearching = true) }
            }
            val queryResult = storageRepository.searchFiles(
                query = state.query,
                categoryFilter = state.selectedCategory,
                favourites = favouritePaths.value,
                showHidden = showHiddenFiles.value,
                forceRebuild = forceRebuild
            )
            _searchState.update {
                it.copy(
                    results = queryResult.results,
                    categoryHitCounts = queryResult.categoryHitCounts,
                    indexedTotalCount = storageRepository.searchIndex.size,
                    isSearching = false
                )
            }
        }
    }

    // Selection
    fun toggleSelection(path: String) {
        _selectedPaths.update { current ->
            if (path in current) current - path else current + path
        }
    }

    fun selectAll(items: List<FileItem>) {
        _selectedPaths.value = items.map { it.path }.toSet()
    }

    fun selectAllInCurrentBrowser() {
        _selectedPaths.value = _browserState.value.files.map { it.path }.toSet()
    }

    fun clearSelection() {
        _selectedPaths.value = emptySet()
    }

    fun getSelectedItems(fromList: List<FileItem>): List<FileItem> {
        val selected = _selectedPaths.value
        return fromList.filter { it.path in selected }
    }

    // File Operations
    fun toggleFavourite(item: FileItem) {
        viewModelScope.launch {
            prefsRepository.toggleFavourite(item.path)
            val nowFav = item.path !in favouritePaths.value
            _snackbarMessages.tryEmit(if (nowFav) "Added to Favourites" else "Removed from Favourites")
            if (_searchState.value.query.isNotBlank() || _searchState.value.selectedCategory != null) {
                executeSearch()
            }
        }
    }

    fun toggleFavouritesBulk(items: List<FileItem>) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            val favs = favouritePaths.value
            val summary = com.example.model.BulkSelectionSummary.from(items, favs)
            val makeFavourite = !summary.allAreFavourites
            prefsRepository.setFavouritesBulk(items.map { it.path }, makeFavourite)
            val label = "${items.size} item${if (items.size == 1) "" else "s"}"
            _snackbarMessages.tryEmit(
                if (makeFavourite) "Added $label to Favourites" else "Removed $label from Favourites"
            )
            clearSelection()
            if (_searchState.value.query.isNotBlank() || _searchState.value.selectedCategory != null) {
                executeSearch()
            }
        }
    }

    fun createFolder(parentPath: String, name: String) {
        viewModelScope.launch {
            storageRepository.createFolder(parentPath, name)
                .onSuccess { created ->
                    _snackbarMessages.tryEmit("Created folder \"$name\"")
                    recordActivity(
                        type = FileOperationType.CREATE_FOLDER,
                        title = "Created folder \"${created.name}\"",
                        detail = relativeDisplayPath(parentPath),
                        targetFolderPath = created.absolutePath,
                        itemCount = 1,
                        totalBytes = 0L
                    )
                    refreshAll()
                }
                .onFailure {
                    _snackbarMessages.tryEmit(it.message ?: "Failed to create folder")
                }
        }
    }

    fun renameFile(item: FileItem, newName: String) {
        viewModelScope.launch {
            storageRepository.renameFile(item, newName)
                .onSuccess { dest ->
                    _snackbarMessages.tryEmit("Renamed to \"$newName\"")
                    val parentDir = dest.parentFile?.absolutePath ?: storageRepository.internalRootPath
                    recordActivity(
                        type = FileOperationType.RENAME,
                        title = "Renamed \"${item.name}\" to \"${dest.name}\"",
                        detail = relativeDisplayPath(parentDir),
                        targetFolderPath = parentDir,
                        itemCount = 1,
                        totalBytes = item.sizeBytes
                    )
                    clearSelection()
                    refreshAll()
                    if (_searchState.value.query.isNotBlank() || _searchState.value.selectedCategory != null) {
                        executeSearch(forceRebuild = true)
                    }
                }
                .onFailure {
                    _snackbarMessages.tryEmit(it.message ?: "Rename failed")
                }
        }
    }

    fun deleteItems(items: List<FileItem>) {
        if (items.isEmpty()) return
        if (activeOperationJob?.isActive == true) {
            _snackbarMessages.tryEmit("Another file operation is already in progress")
            return
        }
        progressDismissJob?.cancel()
        val opId = UUID.randomUUID().toString()
        val initialTotalBytes = items.sumOf { if (it.isDirectory) 0L else it.sizeBytes.coerceAtLeast(0L) }
        val parentDir = items.firstOrNull()?.let { File(it.path).parentFile?.absolutePath }
            ?: storageRepository.internalRootPath
        _operationProgress.value = OperationProgress(
            id = opId,
            operationType = FileOperationType.DELETE,
            phase = OperationPhase.RUNNING,
            title = if (items.size == 1) "Deleting \"${items.first().name}\"" else "Deleting ${items.size} items",
            statusDetail = "Preparing items…",
            currentItemName = items.first().name,
            destinationLabel = relativeDisplayPath(parentDir),
            processedItems = 0,
            totalItems = items.size,
            processedBytes = 0L,
            totalBytes = initialTotalBytes
        )
        clearSelection()

        activeOperationJob = viewModelScope.launch {
            try {
                storageRepository.deleteFiles(
                    items = items,
                    onProgress = { snapshot -> updateRunningProgress(opId, snapshot, "Deleting…") }
                )
                    .onSuccess { count ->
                        val totalSize = items.sumOf { it.sizeBytes.coerceAtLeast(0L) }
                        recordActivity(
                            type = FileOperationType.DELETE,
                            title = if (count == 1) "Deleted \"${items.first().name}\"" else "Deleted $count items",
                            detail = summarizeItemNames(items) + if (totalSize > 0L) " · ${FormatUtils.formatFileSize(totalSize)}" else "",
                            targetFolderPath = parentDir,
                            itemCount = count,
                            totalBytes = totalSize
                        )
                        _operationProgress.update { current ->
                            if (current?.id == opId) {
                                current.copy(
                                    phase = OperationPhase.COMPLETED,
                                    title = if (count == 1) "Deleted \"${items.first().name}\"" else "Deleted $count items",
                                    statusDetail = "Completed · ${relativeDisplayPath(parentDir)}",
                                    processedItems = count,
                                    totalItems = count.coerceAtLeast(current.totalItems),
                                    processedBytes = current.totalBytes,
                                    fractionOverride = 1f
                                )
                            } else {
                                current
                            }
                        }
                        scheduleProgressAutoDismiss(2800L)
                        refreshAll()
                        if (_searchState.value.query.isNotBlank() || _searchState.value.selectedCategory != null) {
                            executeSearch(forceRebuild = true)
                        }
                    }
                    .onFailure { err ->
                        val msg = err.message ?: "Delete failed"
                        _operationProgress.update { current ->
                            if (current?.id == opId) {
                                current.copy(
                                    phase = OperationPhase.FAILED,
                                    title = "Delete failed",
                                    statusDetail = msg
                                )
                            } else {
                                current
                            }
                        }
                        scheduleProgressAutoDismiss(3600L)
                        _snackbarMessages.tryEmit(msg)
                    }
            } catch (ce: CancellationException) {
                handleOperationCancelled(opId)
                throw ce
            }
        }
    }

    fun startTransfer(operation: TransferOperation, items: List<FileItem>) {
        val startPath = storageRepository.internalRootPath
        _pendingTransfer.value = PendingTransfer(operation, items, startPath)
        loadPickerDirectory(startPath)
    }

    fun navigatePickerDirectory(path: String) {
        _pendingTransfer.update { it?.copy(currentPickerPath = path) }
        loadPickerDirectory(path)
    }

    fun navigatePickerUp() {
        val current = _pendingTransfer.value?.currentPickerPath ?: return
        val root = storageRepository.internalRootPath
        if (current == root) return
        val parent = File(current).parentFile?.absolutePath ?: return
        if (parent.startsWith(root)) navigatePickerDirectory(parent)
    }

    fun cancelTransfer() {
        _pendingTransfer.value = null
    }

    fun confirmTransferHere() {
        val transfer = _pendingTransfer.value ?: return
        if (activeOperationJob?.isActive == true) {
            _snackbarMessages.tryEmit("Another file operation is already in progress")
            return
        }
        _pendingTransfer.value = null
        clearSelection()
        progressDismissJob?.cancel()

        val opId = UUID.randomUUID().toString()
        val isCopy = transfer.operation == TransferOperation.COPY
        val opType = if (isCopy) FileOperationType.COPY else FileOperationType.MOVE
        val activeVerb = if (isCopy) "Copying" else "Moving"
        val pastVerb = if (isCopy) "Copied" else "Moved"
        val destDisplay = relativeDisplayPath(transfer.currentPickerPath)
        val initialBytes = transfer.items.sumOf { if (it.isDirectory) 0L else it.sizeBytes.coerceAtLeast(0L) }

        _operationProgress.value = OperationProgress(
            id = opId,
            operationType = opType,
            phase = OperationPhase.RUNNING,
            title = if (transfer.items.size == 1) {
                "$activeVerb \"${transfer.items.first().name}\""
            } else {
                "$activeVerb ${transfer.items.size} items"
            },
            statusDetail = "$activeVerb to $destDisplay",
            currentItemName = transfer.items.firstOrNull()?.name.orEmpty(),
            destinationLabel = destDisplay,
            processedItems = 0,
            totalItems = transfer.items.size,
            processedBytes = 0L,
            totalBytes = initialBytes
        )

        activeOperationJob = viewModelScope.launch {
            try {
                val progressCallback: (OperationProgressSnapshot) -> Unit = { snapshot ->
                    updateRunningProgress(opId, snapshot, "$activeVerb to $destDisplay")
                }
                val result = if (isCopy) {
                    storageRepository.copyFiles(transfer.items, transfer.currentPickerPath, progressCallback)
                } else {
                    storageRepository.moveFiles(transfer.items, transfer.currentPickerPath, progressCallback)
                }

                result
                    .onSuccess { count ->
                        val totalSize = transfer.items.sumOf { it.sizeBytes.coerceAtLeast(0L) }
                        recordActivity(
                            type = opType,
                            title = if (count == 1) "$pastVerb \"${transfer.items.first().name}\"" else "$pastVerb $count items",
                            detail = "${summarizeItemNames(transfer.items)} → $destDisplay",
                            targetFolderPath = transfer.currentPickerPath,
                            itemCount = count,
                            totalBytes = totalSize
                        )
                        _operationProgress.update { current ->
                            if (current?.id == opId) {
                                current.copy(
                                    phase = OperationPhase.COMPLETED,
                                    title = if (count == 1) "$pastVerb \"${transfer.items.first().name}\"" else "$pastVerb $count items",
                                    statusDetail = "Saved in $destDisplay",
                                    processedItems = count,
                                    totalItems = count.coerceAtLeast(current.totalItems),
                                    processedBytes = current.totalBytes,
                                    fractionOverride = 1f
                                )
                            } else {
                                current
                            }
                        }
                        scheduleProgressAutoDismiss(2800L)
                        refreshAll()
                        if (_searchState.value.query.isNotBlank() || _searchState.value.selectedCategory != null) {
                            executeSearch(forceRebuild = true)
                        }
                    }
                    .onFailure { err ->
                        val msg = err.message ?: "Operation failed"
                        _operationProgress.update { current ->
                            if (current?.id == opId) {
                                current.copy(
                                    phase = OperationPhase.FAILED,
                                    title = "${opType.label} failed",
                                    statusDetail = msg
                                )
                            } else {
                                current
                            }
                        }
                        scheduleProgressAutoDismiss(3600L)
                        _snackbarMessages.tryEmit(msg)
                    }
            } catch (ce: CancellationException) {
                handleOperationCancelled(opId)
                throw ce
            }
        }
    }

    private fun loadPickerDirectory(path: String) {
        viewModelScope.launch {
            val entries = storageRepository.getDirectoryContents(
                path = path,
                favourites = emptySet(),
                showHidden = showHiddenFiles.value,
                sortOption = SortOption.NAME,
                sortDirection = SortDirection.ASCENDING
            ).filter { it.isDirectory }
            _pickerFolders.value = entries
        }
    }

    fun shareItems(items: List<FileItem>) {
        val fileItems = items.filter { !it.isDirectory }
        if (fileItems.isEmpty()) {
            _snackbarMessages.tryEmit("Folders cannot be shared directly")
            return
        }
        runCatching {
            val intent = storageRepository.buildShareIntent(fileItems)
            _launchIntentEvents.tryEmit(Intent.createChooser(intent, "Share via"))
        }.onFailure {
            _snackbarMessages.tryEmit("Unable to share selected file(s)")
        }
    }

    fun resolveDefaultArchiveDirectory(isBrowserRoute: Boolean = true): String {
        val state = _browserState.value
        val dest = state.destination
        return if (isBrowserRoute && dest != null &&
            (dest.mode == BrowseMode.DIRECTORY || dest.mode == BrowseMode.DOWNLOADS) &&
            state.currentDirectoryPath.isNotBlank()
        ) {
            state.currentDirectoryPath
        } else {
            storageRepository.downloadsPath
        }
    }

    fun compressItems(
        items: List<FileItem>,
        archiveName: String,
        password: String?,
        compressionLevel: ZipCompressionLevel,
        destinationDirPath: String
    ) {
        if (items.isEmpty()) return
        if (activeOperationJob?.isActive == true) {
            _snackbarMessages.tryEmit("Another file operation is already in progress")
            return
        }
        progressDismissJob?.cancel()
        val opId = UUID.randomUUID().toString()
        val isEncrypted = !password.isNullOrEmpty()
        val displayArchiveName = if (archiveName.trim().lowercase().endsWith(".zip")) {
            archiveName.trim()
        } else {
            "${archiveName.trim().ifBlank { "Archive" }}.zip"
        }
        val destDisplay = relativeDisplayPath(destinationDirPath)
        val initialBytes = items.sumOf { if (it.isDirectory) 0L else it.sizeBytes.coerceAtLeast(0L) }

        _operationProgress.value = OperationProgress(
            id = opId,
            operationType = FileOperationType.COMPRESS,
            phase = OperationPhase.RUNNING,
            title = "Compressing $displayArchiveName",
            statusDetail = if (isEncrypted) "AES-256 encrypted · $destDisplay" else "Saving to $destDisplay",
            currentItemName = items.firstOrNull()?.name ?: displayArchiveName,
            destinationLabel = destDisplay,
            processedItems = 0,
            totalItems = items.size,
            processedBytes = 0L,
            totalBytes = initialBytes
        )
        clearSelection()

        activeOperationJob = viewModelScope.launch {
            try {
                storageRepository.compressToZip(
                    items = items,
                    destinationDirPath = destinationDirPath,
                    archiveName = archiveName,
                    password = password,
                    compressionLevel = compressionLevel,
                    onProgress = { snapshot ->
                        updateRunningProgress(
                            opId,
                            snapshot,
                            if (isEncrypted) "AES-256 encrypted · $destDisplay" else "Saving to $destDisplay"
                        )
                    }
                ).onSuccess { outFile ->
                    recordActivity(
                        type = FileOperationType.COMPRESS,
                        title = "Created ${outFile.name}",
                        detail = buildString {
                            append("${items.size} item${if (items.size == 1) "" else "s"}")
                            if (isEncrypted) append(" · AES-256")
                            append(" · ${FormatUtils.formatFileSize(outFile.length())}")
                        },
                        targetFolderPath = destinationDirPath,
                        itemCount = items.size,
                        totalBytes = outFile.length()
                    )
                    _operationProgress.update { current ->
                        if (current?.id == opId) {
                            current.copy(
                                phase = OperationPhase.COMPLETED,
                                title = "Created ${outFile.name}",
                                statusDetail = if (isEncrypted) {
                                    "AES-256 encrypted · ${FormatUtils.formatFileSize(outFile.length())}"
                                } else {
                                    "${FormatUtils.formatFileSize(outFile.length())} · $destDisplay"
                                },
                                processedItems = items.size,
                                totalItems = items.size.coerceAtLeast(current.totalItems),
                                processedBytes = current.totalBytes,
                                fractionOverride = 1f
                            )
                        } else {
                            current
                        }
                    }
                    scheduleProgressAutoDismiss(2800L)
                    refreshAll()
                    if (_searchState.value.query.isNotBlank() || _searchState.value.selectedCategory != null) {
                        executeSearch(forceRebuild = true)
                    }
                }.onFailure { err ->
                    val msg = err.message ?: "Compression failed"
                    _operationProgress.update { current ->
                        if (current?.id == opId) {
                            current.copy(
                                phase = OperationPhase.FAILED,
                                title = "Compression failed",
                                statusDetail = msg
                            )
                        } else {
                            current
                        }
                    }
                    scheduleProgressAutoDismiss(3600L)
                    _snackbarMessages.tryEmit(msg)
                }
            } catch (ce: CancellationException) {
                handleOperationCancelled(opId)
                throw ce
            }
        }
    }

    fun extractArchive(item: FileItem, password: String? = null) {
        if (activeOperationJob?.isActive == true) {
            _snackbarMessages.tryEmit("Another file operation is already in progress")
            return
        }
        viewModelScope.launch {
            if (password == null && storageRepository.isZipEncrypted(item.path)) {
                _pendingPasswordArchive.value = item
                return@launch
            }
            if (activeOperationJob?.isActive == true) {
                _snackbarMessages.tryEmit("Another file operation is already in progress")
                return@launch
            }
            progressDismissJob?.cancel()
            val parent = File(item.path).parentFile?.absolutePath ?: storageRepository.downloadsPath
            val destDisplay = relativeDisplayPath(parent)
            val opId = UUID.randomUUID().toString()

            _operationProgress.value = OperationProgress(
                id = opId,
                operationType = FileOperationType.EXTRACT,
                phase = OperationPhase.RUNNING,
                title = "Extracting ${item.name}",
                statusDetail = "Unpacking to $destDisplay",
                currentItemName = item.name,
                destinationLabel = destDisplay,
                processedItems = 0,
                totalItems = 1,
                processedBytes = 0L,
                totalBytes = item.sizeBytes.coerceAtLeast(0L)
            )

            activeOperationJob = viewModelScope.launch {
                try {
                    storageRepository.extractZip(
                        zipFileItem = item,
                        targetParentPath = parent,
                        password = password,
                        onProgress = { snapshot ->
                            updateRunningProgress(opId, snapshot, "Unpacking to $destDisplay")
                        }
                    )
                        .onSuccess { outDir ->
                            _pendingPasswordArchive.value = null
                            recordActivity(
                                type = FileOperationType.EXTRACT,
                                title = "Extracted \"${item.name}\"",
                                detail = "To ${outDir.name} · ${relativeDisplayPath(outDir.absolutePath)}",
                                targetFolderPath = outDir.absolutePath,
                                itemCount = 1,
                                totalBytes = item.sizeBytes
                            )
                            _operationProgress.update { current ->
                                if (current?.id == opId) {
                                    current.copy(
                                        phase = OperationPhase.COMPLETED,
                                        title = "Extracted to ${outDir.name}",
                                        statusDetail = "Ready in ${relativeDisplayPath(outDir.absolutePath)}",
                                        processedItems = current.totalItems.coerceAtLeast(1),
                                        processedBytes = current.totalBytes,
                                        fractionOverride = 1f
                                    )
                                } else {
                                    current
                                }
                            }
                            scheduleProgressAutoDismiss(2800L)
                            refreshAll()
                            if (_searchState.value.query.isNotBlank() || _searchState.value.selectedCategory != null) {
                                executeSearch(forceRebuild = true)
                            }
                        }
                        .onFailure { err ->
                            val msg = err.message ?: "Extraction failed"
                            if (msg.contains("password", ignoreCase = true) || msg.contains("Wrong", ignoreCase = true)) {
                                _operationProgress.value = null
                                _pendingPasswordArchive.value = item
                                _snackbarMessages.tryEmit("Incorrect password — please try again")
                            } else {
                                _pendingPasswordArchive.value = null
                                _operationProgress.update { current ->
                                    if (current?.id == opId) {
                                        current.copy(
                                            phase = OperationPhase.FAILED,
                                            title = "Extraction failed",
                                            statusDetail = msg
                                        )
                                    } else {
                                        current
                                    }
                                }
                                scheduleProgressAutoDismiss(3600L)
                                _snackbarMessages.tryEmit(msg)
                            }
                        }
                } catch (ce: CancellationException) {
                    handleOperationCancelled(opId)
                    throw ce
                }
            }
        }
    }

    fun cancelActiveOperation() {
        val job = activeOperationJob ?: return
        if (!job.isActive) return
        _operationProgress.update { current ->
            if (current != null && current.phase == OperationPhase.RUNNING) {
                current.copy(statusDetail = "Cancelling…")
            } else {
                current
            }
        }
        job.cancel()
    }

    fun dismissOperationProgress() {
        progressDismissJob?.cancel()
        if (_operationProgress.value?.phase != OperationPhase.RUNNING) {
            _operationProgress.value = null
        }
    }

    private fun updateRunningProgress(
        opId: String,
        snapshot: OperationProgressSnapshot,
        defaultDetail: String
    ) {
        val now = System.currentTimeMillis()
        val current = _operationProgress.value ?: return
        if (current.id != opId || current.phase != OperationPhase.RUNNING) return
        val itemCountChanged = snapshot.processedItems != current.processedItems
        val isBoundary = snapshot.processedBytes == 0L ||
            (snapshot.totalBytes > 0L && snapshot.processedBytes >= snapshot.totalBytes)
        if (!itemCountChanged && !isBoundary && now - lastProgressUiUpdateMs < 60L) {
            return
        }
        lastProgressUiUpdateMs = now
        _operationProgress.update { latest ->
            if (latest == null || latest.id != opId || latest.phase != OperationPhase.RUNNING) {
                latest
            } else {
                latest.copy(
                    currentItemName = snapshot.currentItemName.ifBlank { latest.currentItemName },
                    processedItems = snapshot.processedItems,
                    totalItems = snapshot.totalItems.coerceAtLeast(1),
                    processedBytes = snapshot.processedBytes,
                    totalBytes = snapshot.totalBytes.coerceAtLeast(0L),
                    fractionOverride = snapshot.fractionOverride,
                    statusDetail = if (latest.statusDetail == "Cancelling…") {
                        latest.statusDetail
                    } else {
                        defaultDetail
                    }
                )
            }
        }
    }

    private fun handleOperationCancelled(opId: String) {
        _operationProgress.update { current ->
            if (current != null && current.id == opId) {
                val summary = if (current.totalItems > 0) {
                    "Cancelled (${current.processedItems} of ${current.totalItems} finished)"
                } else {
                    "Operation cancelled"
                }
                current.copy(
                    phase = OperationPhase.CANCELLED,
                    title = "${current.operationType.label} cancelled",
                    statusDetail = summary
                )
            } else {
                current
            }
        }
        scheduleProgressAutoDismiss(2400L)
        refreshAll()
        if (_searchState.value.query.isNotBlank() || _searchState.value.selectedCategory != null) {
            executeSearch(forceRebuild = true)
        }
    }

    private fun scheduleProgressAutoDismiss(delayMillis: Long) {
        progressDismissJob?.cancel()
        progressDismissJob = viewModelScope.launch {
            delay(delayMillis)
            _operationProgress.update { current ->
                if (current != null && current.phase != OperationPhase.RUNNING) null else current
            }
        }
    }

    fun dismissPasswordPrompt() {
        _pendingPasswordArchive.value = null
    }

    fun clearActivityHistory() {
        viewModelScope.launch {
            prefsRepository.clearActivityHistory()
            _snackbarMessages.tryEmit("Cleared recent activity history")
        }
    }

    fun jumpToActivityFolder(targetFolderPath: String): Boolean {
        val dir = File(targetFolderPath)
        if (!dir.exists() || !dir.isDirectory) {
            _snackbarMessages.tryEmit("Folder no longer exists on device")
            return false
        }
        navigateToDirectory(dir.absolutePath)
        return true
    }

    private suspend fun recordActivity(
        type: FileOperationType,
        title: String,
        detail: String,
        targetFolderPath: String,
        itemCount: Int,
        totalBytes: Long
    ) {
        prefsRepository.appendActivityEvent(
            FileActivityEvent(
                id = UUID.randomUUID().toString(),
                type = type,
                title = title,
                detail = detail,
                targetFolderPath = targetFolderPath,
                itemCount = itemCount,
                totalBytes = totalBytes,
                timestampMillis = System.currentTimeMillis()
            )
        )
    }

    private fun summarizeItemNames(items: List<FileItem>): String {
        val firstTwo = items.take(2).joinToString(", ") { it.name }
        return if (items.size > 2) "$firstTwo +${items.size - 2} more" else firstTwo
    }

    private fun relativeDisplayPath(path: String): String {
        val root = storageRepository.internalRootPath
        val rel = path.removePrefix(root).trim('/')
        return if (rel.isEmpty()) "/My Phone" else "/$rel"
    }

    // Preview & Open
    fun openItem(item: FileItem) {
        if (item.isDirectory) {
            navigateToDirectory(item.path)
            return
        }
        val ext = item.extension.lowercase()
        when {
            item.category == FileCategory.IMAGES || item.mimeType.startsWith("image/") -> {
                _activePreview.value = PreviewTarget.Image(item)
            }
            item.category == FileCategory.VIDEOS || item.mimeType.startsWith("video/") -> {
                _activePreview.value = PreviewTarget.Video(item)
            }
            item.category == FileCategory.MUSIC || item.mimeType.startsWith("audio/") -> {
                _activePreview.value = PreviewTarget.Audio(item)
            }
            ext == "pdf" -> {
                _activePreview.value = PreviewTarget.Pdf(item)
            }
            FormatUtils.isTextPreviewable(ext, item.mimeType) -> {
                _activePreview.value = PreviewTarget.Text(item, "", isLoading = true)
                viewModelScope.launch {
                    storageRepository.readTextPreview(item.path)
                        .onSuccess { text ->
                            _activePreview.value = PreviewTarget.Text(item, text, isLoading = false)
                        }
                        .onFailure { err ->
                            _activePreview.value = PreviewTarget.Text(
                                item,
                                "Unable to read text preview: ${err.message}",
                                isLoading = false
                            )
                        }
                }
            }
            ext == "zip" -> {
                _activePreview.value = PreviewTarget.Archive(item, emptyList(), isLoading = true)
                viewModelScope.launch {
                    storageRepository.readZipEntries(item.path)
                        .onSuccess { entries ->
                            _activePreview.value = PreviewTarget.Archive(item, entries, isLoading = false)
                        }
                        .onFailure {
                            _activePreview.value = PreviewTarget.Archive(item, emptyList(), isLoading = false)
                            _snackbarMessages.tryEmit("Could not read archive contents")
                        }
                }
            }
            ext == "apk" -> {
                _activePreview.value = PreviewTarget.Apk(item, null, isLoading = true)
                viewModelScope.launch {
                    storageRepository.getApkInfo(item)
                        .onSuccess { info ->
                            _activePreview.value = PreviewTarget.Apk(item, info, isLoading = false)
                        }
                        .onFailure { err ->
                            _activePreview.value = PreviewTarget.Apk(
                                item,
                                null,
                                isLoading = false,
                                error = err.message ?: "Unable to parse APK package"
                            )
                        }
                }
            }
            else -> {
                _activePreview.value = PreviewTarget.Generic(item)
            }
        }
    }

    fun closePreview() {
        _activePreview.value = null
    }

    fun installApk(item: FileItem) {
        if (!storageRepository.canRequestPackageInstalls()) {
            _snackbarMessages.tryEmit("Allow 'Install unknown apps' for Visual Explorer, then tap Install again")
            _launchIntentEvents.tryEmit(storageRepository.buildUnknownSourcesSettingsIntent())
            return
        }
        runCatching {
            _launchIntentEvents.tryEmit(storageRepository.buildInstallApkIntent(item))
        }.onFailure {
            _snackbarMessages.tryEmit("Could not launch APK installer")
        }
    }

    fun openExternally(item: FileItem) {
        runCatching {
            _launchIntentEvents.tryEmit(Intent.createChooser(storageRepository.buildOpenExternalIntent(item), "Open with"))
        }.onFailure {
            _snackbarMessages.tryEmit("No installed app can open this file type")
        }
    }
}
