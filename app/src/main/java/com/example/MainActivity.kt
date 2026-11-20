package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.model.BulkSelectionSummary
import com.example.model.FileItem
import com.example.ui.components.AuroraMeshBackground
import com.example.ui.components.BulkActionsSheet
import com.example.ui.components.FloatingBottomDock
import com.example.ui.components.FloatingOperationProgressCard
import com.example.ui.components.MainTab
import com.example.ui.components.RecentActivitySheet
import com.example.ui.components.StorageAnalyticsSheet
import com.example.ui.screens.BrowseScreen
import com.example.ui.screens.CompressToZipDialog
import com.example.ui.screens.CreateFolderDialog
import com.example.ui.screens.DeleteConfirmDialog
import com.example.ui.screens.FileActionSheet
import com.example.ui.screens.FileBrowserScreen
import com.example.ui.screens.FilePreviewModal
import com.example.ui.screens.FilePropertiesDialog
import com.example.ui.screens.FolderPickerSheet
import com.example.ui.screens.RecentScreen
import com.example.ui.screens.RenameDialog
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.ZipPasswordDialog
import com.example.ui.theme.VisualExplorerTheme
import com.example.viewmodel.FileExplorerViewModel
import com.example.viewmodel.TransferOperation

object Routes {
    const val BROWSE = "browse"
    const val RECENT = "recent"
    const val BROWSER = "browser"
    const val SEARCH = "search"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VisualExplorerApp(
                onLaunchExternalIntent = { intent ->
                    runCatching { startActivity(intent) }
                }
            )
        }
    }
}

@Composable
fun VisualExplorerApp(
    onLaunchExternalIntent: (Intent) -> Unit,
    viewModel: FileExplorerViewModel = viewModel()
) {
    val themePreference by viewModel.themePreference.collectAsStateWithLifecycle()
    val showHiddenFiles by viewModel.showHiddenFiles.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val sortDirection by viewModel.sortDirection.collectAsStateWithLifecycle()
    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val browserState by viewModel.browserState.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val selectedPaths by viewModel.selectedPaths.collectAsStateWithLifecycle()
    val favouritePaths by viewModel.favouritePaths.collectAsStateWithLifecycle()
    val activityEvents by viewModel.activityEvents.collectAsStateWithLifecycle()
    val activePreview by viewModel.activePreview.collectAsStateWithLifecycle()
    val pendingTransfer by viewModel.pendingTransfer.collectAsStateWithLifecycle()
    val pickerFolders by viewModel.pickerFolders.collectAsStateWithLifecycle()
    val operationProgress by viewModel.operationProgress.collectAsStateWithLifecycle()
    val pendingPasswordArchive by viewModel.pendingPasswordArchive.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Routes.BROWSE

    // Action Dialog / Sheet States
    var actionSheetItem by remember { mutableStateOf<FileItem?>(null) }
    var renameTarget by remember { mutableStateOf<FileItem?>(null) }
    var createFolderParent by remember { mutableStateOf<String?>(null) }
    var deleteTargets by remember { mutableStateOf<List<FileItem>?>(null) }
    var compressTargets by remember { mutableStateOf<List<FileItem>?>(null) }
    var propertiesTarget by remember { mutableStateOf<FileItem?>(null) }
    var showStorageAnalytics by remember { mutableStateOf(false) }
    var showBulkActionsSheet by remember { mutableStateOf(false) }
    var showRecentActivitySheet by remember { mutableStateOf(false) }

    LaunchedEffect(selectedPaths.isEmpty()) {
        if (selectedPaths.isEmpty()) {
            showBulkActionsSheet = false
        }
    }

    // Runtime Storage Permission Launcher (for Android <= 10 or Scoped Media fallback)
    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshAll()
    }

    val requestStorageAccess: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            onLaunchExternalIntent(viewModel.storageRepository.buildAllFilesPermissionIntent())
        } else {
            legacyPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    // Automatically request runtime media permission on Android 13+ if All Files isn't granted yet
    LaunchedEffect(Unit) {
        if (!viewModel.storageRepository.hasAllFilesAccess() &&
            !viewModel.storageRepository.hasScopedMediaAccess()
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                legacyPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO,
                        Manifest.permission.READ_MEDIA_AUDIO
                    )
                )
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                legacyPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                )
            }
        }
    }

    // Refresh permissions & storage scan whenever user returns from Settings / Installer
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshAll()
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.launchIntentEvents.collect { intent ->
            onLaunchExternalIntent(intent)
        }
    }

    VisualExplorerTheme(themePreference = themePreference) {
        Box(modifier = Modifier.fillMaxSize()) {
            AuroraMeshBackground()

            NavHost(
                navController = navController,
                startDestination = Routes.BROWSE,
                enterTransition = { fadeIn(tween(220)) },
                exitTransition = { fadeOut(tween(180)) },
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Routes.BROWSE) {
                    BrowseScreen(
                        state = dashboardState,
                        activityEvents = activityEvents,
                        themePreference = themePreference,
                        showHiddenFiles = showHiddenFiles,
                        onToggleDarkMode = { isDark -> viewModel.cycleDarkMode(isDark) },
                        onSetThemePreference = { viewModel.setThemePreference(it) },
                        onToggleHiddenFiles = { viewModel.toggleShowHiddenFiles() },
                        onRequestAllFilesAccess = requestStorageAccess,
                        onRefresh = { viewModel.refreshAll() },
                        onCategoryClick = { category ->
                            viewModel.openCategory(category)
                            navController.navigate(Routes.BROWSER)
                        },
                        onDownloadsClick = {
                            viewModel.openDownloads()
                            navController.navigate(Routes.BROWSER)
                        },
                        onFavouritesClick = {
                            viewModel.openFavourites()
                            navController.navigate(Routes.BROWSER)
                        },
                        onMyPhoneClick = {
                            viewModel.openInternalStorage()
                            navController.navigate(Routes.BROWSER)
                        },
                        onOpenStorageAnalytics = {
                            showStorageAnalytics = true
                        },
                        onOpenRecentActivity = {
                            showRecentActivitySheet = true
                        },
                        onSourceClick = { source ->
                            viewModel.openSource(source)
                            navController.navigate(Routes.BROWSER)
                        }
                    )
                }

                composable(Routes.RECENT) {
                    RecentScreen(
                        recentFiles = dashboardState.recentFiles,
                        selectedPaths = selectedPaths,
                        favouritePaths = favouritePaths,
                        onToggleDarkMode = { isDark -> viewModel.cycleDarkMode(isDark) },
                        onSelectAll = { visibleItems -> viewModel.selectAll(visibleItems) },
                        onClearSelection = { viewModel.clearSelection() },
                        onOpenBulkSheet = { showBulkActionsSheet = true },
                        onMoveSelected = {
                            val items = viewModel.getSelectedItems(dashboardState.recentFiles)
                            if (items.isNotEmpty()) viewModel.startTransfer(TransferOperation.MOVE, items)
                        },
                        onCopySelected = {
                            val items = viewModel.getSelectedItems(dashboardState.recentFiles)
                            if (items.isNotEmpty()) viewModel.startTransfer(TransferOperation.COPY, items)
                        },
                        onDeleteSelected = {
                            val items = viewModel.getSelectedItems(dashboardState.recentFiles)
                            if (items.isNotEmpty()) deleteTargets = items
                        },
                        onItemClick = { item ->
                            if (selectedPaths.isNotEmpty()) {
                                viewModel.toggleSelection(item.path)
                            } else {
                                viewModel.openItem(item)
                            }
                        },
                        onItemLongClick = { item -> viewModel.toggleSelection(item.path) },
                        onItemMoreClick = { item -> actionSheetItem = item }
                    )
                }

                composable(Routes.BROWSER) {
                    FileBrowserScreen(
                        state = browserState,
                        selectedPaths = selectedPaths,
                        favouritePaths = favouritePaths,
                        sortOption = sortOption,
                        sortDirection = sortDirection,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateUpDirectory = { viewModel.navigateUpDirectory() },
                        onBreadcrumbClick = { path -> viewModel.navigateToDirectory(path) },
                        onSelectSortOption = { opt -> viewModel.selectSortOption(opt) },
                        onToggleGridView = { viewModel.toggleGridView() },
                        onSelectAll = { viewModel.selectAllInCurrentBrowser() },
                        onClearSelection = { viewModel.clearSelection() },
                        onOpenBulkSheet = { showBulkActionsSheet = true },
                        onItemClick = { item ->
                            if (selectedPaths.isNotEmpty()) {
                                viewModel.toggleSelection(item.path)
                            } else {
                                viewModel.openItem(item)
                            }
                        },
                        onItemLongClick = { item -> viewModel.toggleSelection(item.path) },
                        onItemMoreClick = { item -> actionSheetItem = item },
                        onCreateFolderClick = { parent -> createFolderParent = parent },
                        onCopySelected = {
                            val items = viewModel.getSelectedItems(browserState.files)
                            if (items.isNotEmpty()) viewModel.startTransfer(TransferOperation.COPY, items)
                        },
                        onMoveSelected = {
                            val items = viewModel.getSelectedItems(browserState.files)
                            if (items.isNotEmpty()) viewModel.startTransfer(TransferOperation.MOVE, items)
                        },
                        onDeleteSelected = {
                            val items = viewModel.getSelectedItems(browserState.files)
                            if (items.isNotEmpty()) deleteTargets = items
                        },
                        onSwipeMoveItem = { item ->
                            viewModel.startTransfer(TransferOperation.MOVE, listOf(item))
                        },
                        onSwipeShareItem = { item ->
                            viewModel.shareItems(listOf(item))
                        },
                        onSwipeDeleteItem = { item ->
                            deleteTargets = listOf(item)
                        }
                    )
                }

                composable(Routes.SEARCH) {
                    SearchScreen(
                        state = searchState,
                        selectedPaths = selectedPaths,
                        favouritePaths = favouritePaths,
                        onQueryChange = { viewModel.updateSearchQuery(it) },
                        onSearchSubmit = { viewModel.triggerSearchSubmit() },
                        onRefreshIndex = { viewModel.refreshSearchIndex() },
                        onToggleCategory = { viewModel.toggleSearchCategory(it) },
                        onBackClick = {
                            viewModel.clearSelection()
                            navController.popBackStack()
                        },
                        onToggleSelection = { path -> viewModel.toggleSelection(path) },
                        onSelectAll = { visibleItems -> viewModel.selectAll(visibleItems) },
                        onClearSelection = { viewModel.clearSelection() },
                        onOpenBulkSheet = { showBulkActionsSheet = true },
                        onMoveSelected = {
                            val items = viewModel.getSelectedItems(searchState.results)
                            if (items.isNotEmpty()) viewModel.startTransfer(TransferOperation.MOVE, items)
                        },
                        onCopySelected = {
                            val items = viewModel.getSelectedItems(searchState.results)
                            if (items.isNotEmpty()) viewModel.startTransfer(TransferOperation.COPY, items)
                        },
                        onDeleteSelected = {
                            val items = viewModel.getSelectedItems(searchState.results)
                            if (items.isNotEmpty()) deleteTargets = items
                        },
                        onItemClick = { item ->
                            if (item.isDirectory) {
                                viewModel.navigateToDirectory(item.path)
                                navController.navigate(Routes.BROWSER)
                            } else {
                                viewModel.openItem(item)
                            }
                        },
                        onItemMoreClick = { item -> actionSheetItem = item }
                    )
                }
            }

            // Floating Glass Bottom Dock (shown on Recent & Browse tabs)
            val showDock = (currentRoute == Routes.BROWSE || currentRoute == Routes.RECENT) &&
                activePreview == null &&
                selectedPaths.isEmpty()

            AnimatedVisibility(
                visible = showDock,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                FloatingBottomDock(
                    selectedTab = if (currentRoute == Routes.RECENT) MainTab.RECENT else MainTab.BROWSE,
                    onTabSelected = { tab ->
                        val targetRoute = if (tab == MainTab.RECENT) Routes.RECENT else Routes.BROWSE
                        if (currentRoute != targetRoute) {
                            navController.navigate(targetRoute) {
                                popUpTo(Routes.BROWSE) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onSearchClick = {
                        viewModel.openGlobalSearch()
                        navController.navigate(Routes.SEARCH) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            // Snackbar above bottom dock
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = if (showDock) 92.dp else 20.dp, start = 16.dp, end = 16.dp)
            )

            // Built-in Full Preview Modal
            activePreview?.let { previewTarget ->
                FilePreviewModal(
                    target = previewTarget,
                    apkIconProvider = { path -> viewModel.storageRepository.getApkIconDrawable(path) },
                    onDismiss = { viewModel.closePreview() },
                    onShare = { viewModel.shareItems(listOf(it)) },
                    onOpenExternal = { viewModel.openExternally(it) },
                    onInstallApk = { viewModel.installApk(it) },
                    onExtractZip = {
                        viewModel.extractArchive(it)
                        viewModel.closePreview()
                    }
                )
            }

            // Action Bottom Sheet
            actionSheetItem?.let { item ->
                FileActionSheet(
                    item = item,
                    onDismiss = { actionSheetItem = null },
                    onOpen = {
                        if (item.isDirectory) {
                            viewModel.navigateToDirectory(item.path)
                            if (currentRoute != Routes.BROWSER) {
                                navController.navigate(Routes.BROWSER)
                            }
                        } else {
                            viewModel.openItem(item)
                        }
                    },
                    onToggleFavourite = { viewModel.toggleFavourite(item) },
                    onShare = { viewModel.shareItems(listOf(item)) },
                    onCopy = { viewModel.startTransfer(TransferOperation.COPY, listOf(item)) },
                    onMove = { viewModel.startTransfer(TransferOperation.MOVE, listOf(item)) },
                    onRename = { renameTarget = item },
                    onCompress = { compressTargets = listOf(item) },
                    onExtract = { viewModel.extractArchive(item) },
                    onInstallApk = { viewModel.installApk(item) },
                    onProperties = { propertiesTarget = item },
                    onDelete = { deleteTargets = listOf(item) }
                )
            }

            // Rename Dialog
            renameTarget?.let { item ->
                RenameDialog(
                    item = item,
                    onDismiss = { renameTarget = null },
                    onConfirm = { newName ->
                        renameTarget = null
                        viewModel.renameFile(item, newName)
                    }
                )
            }

            // Create Folder Dialog
            createFolderParent?.let { parentPath ->
                CreateFolderDialog(
                    onDismiss = { createFolderParent = null },
                    onConfirm = { folderName ->
                        createFolderParent = null
                        viewModel.createFolder(parentPath, folderName)
                    }
                )
            }

            // Delete Confirmation Dialog
            deleteTargets?.let { targets ->
                DeleteConfirmDialog(
                    items = targets,
                    onDismiss = { deleteTargets = null },
                    onConfirm = {
                        deleteTargets = null
                        viewModel.deleteItems(targets)
                    }
                )
            }

            // Properties Dialog
            propertiesTarget?.let { item ->
                FilePropertiesDialog(
                    item = item,
                    onDismiss = { propertiesTarget = null }
                )
            }

            // Destination Folder Picker Sheet for Copy/Move
            pendingTransfer?.let { transfer ->
                FolderPickerSheet(
                    transfer = transfer,
                    folders = pickerFolders,
                    rootPath = viewModel.storageRepository.internalRootPath,
                    onNavigateInto = { path -> viewModel.navigatePickerDirectory(path) },
                    onNavigateUp = { viewModel.navigatePickerUp() },
                    onConfirmHere = { viewModel.confirmTransferHere() },
                    onDismiss = { viewModel.cancelTransfer() }
                )
            }

            // Compress to ZIP Dialog
            compressTargets?.let { targets ->
                val destDir = remember(targets, currentRoute, browserState.currentDirectoryPath) {
                    viewModel.resolveDefaultArchiveDirectory(isBrowserRoute = currentRoute == Routes.BROWSER)
                }
                CompressToZipDialog(
                    items = targets,
                    destinationDirPath = destDir,
                    onDismiss = { compressTargets = null },
                    onConfirm = { archiveName, password, level ->
                        compressTargets = null
                        viewModel.compressItems(
                            items = targets,
                            archiveName = archiveName,
                            password = password,
                            compressionLevel = level,
                            destinationDirPath = destDir
                        )
                    }
                )
            }

            // Password Prompt for Encrypted ZIP Extraction
            pendingPasswordArchive?.let { archive ->
                ZipPasswordDialog(
                    archiveName = archive.name,
                    onDismiss = { viewModel.dismissPasswordPrompt() },
                    onConfirm = { password ->
                        viewModel.extractArchive(archive, password)
                    }
                )
            }

            // Floating Glassmorphic Operation Progress Card (Copy, Move, Delete, ZIP Compress & Extract)
            FloatingOperationProgressCard(
                progress = operationProgress,
                onCancel = { viewModel.cancelActiveOperation() },
                onDismiss = { viewModel.dismissOperationProgress() },
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Full Bulk Actions Menu Sheet
            if (showBulkActionsSheet) {
                val activeVisibleItems = when (currentRoute) {
                    Routes.BROWSER -> browserState.files
                    Routes.RECENT -> dashboardState.recentFiles
                    Routes.SEARCH -> searchState.results
                    else -> emptyList()
                }
                val selectedItems = viewModel.getSelectedItems(activeVisibleItems)
                if (selectedItems.isNotEmpty()) {
                    val summary = BulkSelectionSummary.from(selectedItems, favouritePaths)
                    BulkActionsSheet(
                        summary = summary,
                        visibleItemCount = activeVisibleItems.size,
                        onDismiss = { showBulkActionsSheet = false },
                        onMoveSelected = { viewModel.startTransfer(TransferOperation.MOVE, selectedItems) },
                        onCopySelected = { viewModel.startTransfer(TransferOperation.COPY, selectedItems) },
                        onCompressSelected = { compressTargets = selectedItems },
                        onShareSelected = { viewModel.shareItems(selectedItems) },
                        onToggleFavourites = { viewModel.toggleFavouritesBulk(selectedItems) },
                        onSelectAll = { viewModel.selectAll(activeVisibleItems) },
                        onClearSelection = { viewModel.clearSelection() },
                        onDeleteSelected = { deleteTargets = selectedItems }
                    )
                }
            }

            if (showRecentActivitySheet) {
                RecentActivitySheet(
                    events = activityEvents,
                    internalRootPath = viewModel.storageRepository.internalRootPath,
                    onDismiss = { showRecentActivitySheet = false },
                    onJumpToFolder = { targetPath ->
                        showRecentActivitySheet = false
                        if (viewModel.jumpToActivityFolder(targetPath)) {
                            navController.navigate(Routes.BROWSER)
                        }
                    },
                    onClearHistory = {
                        viewModel.clearActivityHistory()
                    }
                )
            }

            if (showStorageAnalytics) {
                StorageAnalyticsSheet(
                    summary = dashboardState.storageSummary,
                    onDismiss = { showStorageAnalytics = false },
                    onOpenCategory = { category ->
                        showStorageAnalytics = false
                        viewModel.openCategory(category)
                        navController.navigate(Routes.BROWSER)
                    }
                )
            }
        }
    }
}
