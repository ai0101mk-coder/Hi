package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.FormatUtils
import com.example.model.FileCategory
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Visual Explorer", appName)
    }

    @Test
    fun `category resolution maps extensions accurately`() {
        assertEquals(FileCategory.DOCS, FormatUtils.resolveCategory("pdf"))
        assertEquals(FileCategory.IMAGES, FormatUtils.resolveCategory("png"))
        assertEquals(FileCategory.VIDEOS, FormatUtils.resolveCategory("mp4"))
        assertEquals(FileCategory.MUSIC, FormatUtils.resolveCategory("mp3"))
        assertEquals(FileCategory.ARCHIVES, FormatUtils.resolveCategory("zip"))
        assertEquals(FileCategory.APKS, FormatUtils.resolveCategory("apk"))
    }

    @Test
    fun `file size formatting formats bytes cleanly`() {
        assertEquals("0 B", FormatUtils.formatFileSize(0L))
        assertEquals("512 B", FormatUtils.formatFileSize(512L))
        assertEquals("1.0 KB", FormatUtils.formatFileSize(1024L))
        assertTrue(FormatUtils.formatFileSize(5L * 1024L * 1024L).contains("MB"))
    }

    @Test
    fun `storage summary generates sorted category segments plus other`() {
        val summary = com.example.model.StorageSummary(
            totalBytes = 1000L,
            usedBytes = 800L,
            freeBytes = 200L,
            categoryBytes = mapOf(
                FileCategory.IMAGES to 300L,
                FileCategory.VIDEOS to 400L,
                FileCategory.DOCS to 50L
            ),
            categoryCounts = mapOf(
                FileCategory.IMAGES to 12,
                FileCategory.VIDEOS to 4,
                FileCategory.DOCS to 9
            ),
            otherBytes = 50L
        )
        val segments = summary.toUsedSegments()
        assertEquals(4, segments.size)
        assertEquals(FileCategory.VIDEOS, segments[0].category)
        assertEquals(400L, segments[0].bytes)
        assertEquals(FileCategory.IMAGES, segments[1].category)
        assertEquals("OTHER", segments[3].key)
    }

    @Test
    fun `file search index filters across categories and ranks prefix matches`() {
        val index = com.example.data.FileSearchIndex()
        val items = listOf(
            com.example.model.FileItem(
                id = "/1", name = "vacation_photo.jpg", path = "/DCIM/vacation_photo.jpg",
                sizeBytes = 1200, lastModifiedMillis = 100, isDirectory = false,
                mimeType = "image/jpeg", extension = "jpg", category = FileCategory.IMAGES
            ),
            com.example.model.FileItem(
                id = "/2", name = "vacation_budget.pdf", path = "/Documents/vacation_budget.pdf",
                sizeBytes = 800, lastModifiedMillis = 200, isDirectory = false,
                mimeType = "application/pdf", extension = "pdf", category = FileCategory.DOCS
            ),
            com.example.model.FileItem(
                id = "/3", name = "summer_vacation_clip.mp4", path = "/Movies/summer_vacation_clip.mp4",
                sizeBytes = 5000, lastModifiedMillis = 300, isDirectory = false,
                mimeType = "video/mp4", extension = "mp4", category = FileCategory.VIDEOS
            )
        )
        index.rebuild(items)

        val allVacation = index.search("vacation")
        assertEquals(3, allVacation.totalMatches)
        assertEquals(1, allVacation.categoryHitCounts[FileCategory.DOCS])
        assertEquals(1, allVacation.categoryHitCounts[FileCategory.IMAGES])
        assertEquals(1, allVacation.categoryHitCounts[FileCategory.VIDEOS])
        // Prefix matches ("vacation_*") rank above substring match ("summer_vacation_clip.mp4")
        assertTrue(allVacation.results.first().name.startsWith("vacation"))

        val docsOnly = index.search("vacation", categoryFilter = FileCategory.DOCS)
        assertEquals(1, docsOnly.results.size)
        assertEquals("vacation_budget.pdf", docsOnly.results.first().name)
    }

    @Test
    fun `sort options expose intuitive default directions`() {
        assertEquals(com.example.model.SortDirection.ASCENDING, com.example.model.SortOption.NAME.defaultDirection)
        assertEquals(com.example.model.SortDirection.ASCENDING, com.example.model.SortOption.TYPE.defaultDirection)
        assertEquals(com.example.model.SortDirection.DESCENDING, com.example.model.SortOption.DATE.defaultDirection)
        assertEquals(com.example.model.SortDirection.DESCENDING, com.example.model.SortOption.SIZE.defaultDirection)
    }

    @Test
    fun `compressToZip creates archive and extracts files cleanly`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = com.example.data.StorageRepository(context)
        val tempRoot = java.nio.file.Files.createTempDirectory("zip_plain_test").toFile()
        try {
            val fileA = java.io.File(tempRoot, "notes.txt").apply { writeText("Hello Visual Explorer") }
            val fileB = java.io.File(tempRoot, "budget.csv").apply { writeText("item,cost\ncoffee,5") }
            val items = listOf(fileA, fileB).map { f ->
                com.example.model.FileItem(
                    id = f.absolutePath,
                    name = f.name,
                    path = f.absolutePath,
                    sizeBytes = f.length(),
                    lastModifiedMillis = f.lastModified(),
                    isDirectory = false,
                    mimeType = "text/plain",
                    extension = f.extension,
                    category = FileCategory.DOCS
                )
            }

            val zipResult = repo.compressToZip(
                items = items,
                destinationDirPath = tempRoot.absolutePath,
                archiveName = "Bundle",
                password = null,
                compressionLevel = com.example.model.ZipCompressionLevel.FAST
            )
            assertTrue(zipResult.isSuccess)
            val zipFile = zipResult.getOrThrow()
            assertEquals("Bundle.zip", zipFile.name)
            assertEquals(false, repo.isZipEncrypted(zipFile.absolutePath))

            val entries = repo.readZipEntries(zipFile.absolutePath).getOrThrow()
            assertEquals(listOf("notes.txt", "budget.csv").sorted(), entries.map { it.name }.sorted())
            assertTrue(entries.none { it.isEncrypted })

            val zipItem = com.example.model.FileItem(
                id = zipFile.absolutePath,
                name = zipFile.name,
                path = zipFile.absolutePath,
                sizeBytes = zipFile.length(),
                lastModifiedMillis = zipFile.lastModified(),
                isDirectory = false,
                mimeType = "application/zip",
                extension = "zip",
                category = FileCategory.ARCHIVES
            )
            val extractedDir = repo.extractZip(zipItem, tempRoot.absolutePath, null).getOrThrow()
            assertEquals("Hello Visual Explorer", java.io.File(extractedDir, "notes.txt").readText())
            assertEquals("item,cost\ncoffee,5", java.io.File(extractedDir, "budget.csv").readText())
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    @Test
    fun `compressToZip with password uses AES-256 encryption and supports password-aware extraction`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = com.example.data.StorageRepository(context)
        val tempRoot = java.nio.file.Files.createTempDirectory("zip_aes_test").toFile()
        try {
            val secretFile = java.io.File(tempRoot, "secret.txt").apply { writeText("Top Secret Payload") }
            val items = listOf(
                com.example.model.FileItem(
                    id = secretFile.absolutePath,
                    name = secretFile.name,
                    path = secretFile.absolutePath,
                    sizeBytes = secretFile.length(),
                    lastModifiedMillis = secretFile.lastModified(),
                    isDirectory = false,
                    mimeType = "text/plain",
                    extension = "txt",
                    category = FileCategory.DOCS
                )
            )

            val zipFile = repo.compressToZip(
                items = items,
                destinationDirPath = tempRoot.absolutePath,
                archiveName = "Vault.zip",
                password = "MyStrongPassword!",
                compressionLevel = com.example.model.ZipCompressionLevel.MAXIMUM
            ).getOrThrow()

            assertEquals("Vault.zip", zipFile.name)
            assertTrue(repo.isZipEncrypted(zipFile.absolutePath))

            val entries = repo.readZipEntries(zipFile.absolutePath).getOrThrow()
            assertEquals(1, entries.size)
            assertTrue(entries.first().isEncrypted)

            val zipItem = com.example.model.FileItem(
                id = zipFile.absolutePath,
                name = zipFile.name,
                path = zipFile.absolutePath,
                sizeBytes = zipFile.length(),
                lastModifiedMillis = zipFile.lastModified(),
                isDirectory = false,
                mimeType = "application/zip",
                extension = "zip",
                category = FileCategory.ARCHIVES
            )

            val noPasswordResult = repo.extractZip(zipItem, tempRoot.absolutePath, null)
            assertTrue(noPasswordResult.isFailure)

            val wrongPasswordResult = repo.extractZip(zipItem, tempRoot.absolutePath, "WrongPass")
            assertTrue(wrongPasswordResult.isFailure)

            val okDir = repo.extractZip(zipItem, tempRoot.absolutePath, "MyStrongPassword!").getOrThrow()
            assertEquals("Top Secret Payload", java.io.File(okDir, "secret.txt").readText())
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    @Test
    fun `bulk selection summary computes breakdown size favourites and shareable state`() {
        val file1 = com.example.model.FileItem(
            id = "/a.jpg", name = "a.jpg", path = "/a.jpg", sizeBytes = 1500L,
            lastModifiedMillis = 10L, isDirectory = false, mimeType = "image/jpeg",
            extension = "jpg", category = FileCategory.IMAGES
        )
        val file2 = com.example.model.FileItem(
            id = "/b.pdf", name = "b.pdf", path = "/b.pdf", sizeBytes = 2500L,
            lastModifiedMillis = 20L, isDirectory = false, mimeType = "application/pdf",
            extension = "pdf", category = FileCategory.DOCS
        )
        val folder = com.example.model.FileItem(
            id = "/docs", name = "docs", path = "/docs", sizeBytes = 0L,
            lastModifiedMillis = 30L, isDirectory = true, mimeType = "inode/directory",
            extension = "", category = null
        )

        val mixed = com.example.model.BulkSelectionSummary.from(
            items = listOf(file1, file2, folder),
            favouritePaths = setOf("/a.jpg", "/b.pdf", "/docs")
        )
        assertEquals(3, mixed.totalCount)
        assertEquals(2, mixed.fileCount)
        assertEquals(1, mixed.folderCount)
        assertEquals(4000L, mixed.totalBytes)
        assertTrue(mixed.allAreFavourites)
        assertTrue(mixed.hasShareableFiles)
        assertEquals("2 files, 1 folder", mixed.formatBreakdown())

        val folderOnly = com.example.model.BulkSelectionSummary.from(
            items = listOf(folder),
            favouritePaths = emptySet()
        )
        assertEquals("1 folder", folderOnly.formatBreakdown())
        assertEquals(false, folderOnly.hasShareableFiles)
        assertEquals(false, folderOnly.allAreFavourites)
    }

    @Test
    fun `batch copy move delete and bulk favourites work cleanly`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = com.example.data.StorageRepository(context)
        val tempRoot = java.nio.file.Files.createTempDirectory("bulk_ops_test").toFile()
        try {
            val srcDir = java.io.File(tempRoot, "source").apply { mkdirs() }
            val copyDest = java.io.File(tempRoot, "copyDest").apply { mkdirs() }
            val moveDest = java.io.File(tempRoot, "moveDest").apply { mkdirs() }

            val f1 = java.io.File(srcDir, "one.txt").apply { writeText("one") }
            val f2 = java.io.File(srcDir, "two.txt").apply { writeText("two") }
            val items = listOf(f1, f2).map { f ->
                com.example.model.FileItem(
                    id = f.absolutePath, name = f.name, path = f.absolutePath,
                    sizeBytes = f.length(), lastModifiedMillis = f.lastModified(),
                    isDirectory = false, mimeType = "text/plain", extension = "txt",
                    category = FileCategory.DOCS
                )
            }

            // Batch Copy
            val copiedCount = repo.copyFiles(items, copyDest.absolutePath).getOrThrow()
            assertEquals(2, copiedCount)
            assertTrue(java.io.File(copyDest, "one.txt").exists())
            assertTrue(java.io.File(copyDest, "two.txt").exists())

            // Batch Move
            val movedCount = repo.moveFiles(items, moveDest.absolutePath).getOrThrow()
            assertEquals(2, movedCount)
            assertEquals(false, f1.exists())
            assertEquals(false, f2.exists())
            val movedItems = listOf(
                java.io.File(moveDest, "one.txt"),
                java.io.File(moveDest, "two.txt")
            ).map { f ->
                com.example.model.FileItem(
                    id = f.absolutePath, name = f.name, path = f.absolutePath,
                    sizeBytes = f.length(), lastModifiedMillis = f.lastModified(),
                    isDirectory = false, mimeType = "text/plain", extension = "txt",
                    category = FileCategory.DOCS
                )
            }
            assertTrue(movedItems.all { java.io.File(it.path).exists() })

            // Batch Delete
            val deletedCount = repo.deleteFiles(movedItems).getOrThrow()
            assertEquals(2, deletedCount)
            assertTrue(movedItems.none { java.io.File(it.path).exists() })
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    @Test
    fun `activity log serializer roundtrips events and enforces 100-event cap`() {
        val sampleEvents = (1..120).map { i ->
            com.example.model.FileActivityEvent(
                id = "evt_$i",
                type = com.example.model.FileOperationType.entries[i % com.example.model.FileOperationType.entries.size],
                title = "Operation $i",
                detail = "Detail $i",
                targetFolderPath = "/storage/emulated/0/Download",
                itemCount = i,
                totalBytes = i * 1024L,
                timestampMillis = 1_700_000_000_000L + i
            )
        }

        val json = com.example.data.ActivityLogSerializer.encode(sampleEvents)
        val decoded = com.example.data.ActivityLogSerializer.decode(json)

        assertEquals(com.example.data.ActivityLogSerializer.MAX_EVENTS, decoded.size)
        assertEquals("evt_1", decoded.first().id)
        assertEquals("Operation 1", decoded.first().title)
        assertEquals("/storage/emulated/0/Download", decoded.first().targetFolderPath)
        assertEquals("evt_100", decoded.last().id)
    }

    @Test
    fun `relative time formatter produces concise human labels`() {
        val now = 1_700_000_000_000L
        assertEquals("Just now", FormatUtils.formatRelativeTime(now - 15_000L, now))
        assertEquals("5m ago", FormatUtils.formatRelativeTime(now - 5 * 60_000L, now))
        assertEquals("3h ago", FormatUtils.formatRelativeTime(now - 3 * 3_600_000L, now))
        assertEquals("Yesterday", FormatUtils.formatRelativeTime(now - 30 * 3_600_000L, now))
    }

    @Test
    fun `operation progress computes fraction from phase override bytes or item count`() {
        val byteDriven = com.example.model.OperationProgress(
            id = "op1",
            operationType = com.example.model.FileOperationType.COPY,
            phase = com.example.model.OperationPhase.RUNNING,
            title = "Copying",
            processedItems = 1,
            totalItems = 4,
            processedBytes = 500L,
            totalBytes = 1000L
        )
        assertEquals(0.5f, byteDriven.progressFraction, 0.001f)

        val itemDriven = byteDriven.copy(processedBytes = 0L, totalBytes = 0L, processedItems = 3, totalItems = 4)
        assertEquals(0.75f, itemDriven.progressFraction, 0.001f)

        val overridden = byteDriven.copy(fractionOverride = 0.85f)
        assertEquals(0.85f, overridden.progressFraction, 0.001f)

        val completed = byteDriven.copy(phase = com.example.model.OperationPhase.COMPLETED, fractionOverride = 0.4f)
        assertEquals(1f, completed.progressFraction, 0.001f)
    }

    @Test
    fun `copyFiles and deleteFiles emit real-time progress snapshots and support cancellation`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = com.example.data.StorageRepository(context)
        val tempRoot = java.nio.file.Files.createTempDirectory("progress_ops_test").toFile()
        try {
            val srcDir = java.io.File(tempRoot, "src").apply { mkdirs() }
            val destDir = java.io.File(tempRoot, "dest").apply { mkdirs() }
            val payloadA = ByteArray(128 * 1024) { 0x41 }
            val payloadB = ByteArray(128 * 1024) { 0x42 }
            val fileA = java.io.File(srcDir, "alpha.bin").apply { writeBytes(payloadA) }
            val fileB = java.io.File(srcDir, "beta.bin").apply { writeBytes(payloadB) }

            val items = listOf(fileA, fileB).map { f ->
                com.example.model.FileItem(
                    id = f.absolutePath, name = f.name, path = f.absolutePath,
                    sizeBytes = f.length(), lastModifiedMillis = f.lastModified(),
                    isDirectory = false, mimeType = "application/octet-stream",
                    extension = "bin", category = null
                )
            }

            val copySnapshots = mutableListOf<com.example.model.OperationProgressSnapshot>()
            val copied = repo.copyFiles(items, destDir.absolutePath) { snap ->
                copySnapshots.add(snap)
            }.getOrThrow()

            assertEquals(2, copied)
            assertTrue(copySnapshots.size >= 3)
            assertEquals(0, copySnapshots.first().processedItems)
            assertEquals(2, copySnapshots.last().processedItems)
            assertEquals(payloadA.size.toLong() + payloadB.size.toLong(), copySnapshots.last().processedBytes)

            val deleteSnapshots = mutableListOf<com.example.model.OperationProgressSnapshot>()
            val deleted = repo.deleteFiles(items) { snap ->
                deleteSnapshots.add(snap)
            }.getOrThrow()

            assertEquals(2, deleted)
            assertTrue(deleteSnapshots.isNotEmpty())
            assertEquals(2, deleteSnapshots.last().processedItems)
            assertEquals(false, fileA.exists())
            assertEquals(false, fileB.exists())

            // Cancellation cleanup test on compressToZip
            val bigFile = java.io.File(srcDir, "large_random.bin").apply {
                val rnd = java.util.Random(42L)
                val bytes = ByteArray(2 * 1024 * 1024)
                rnd.nextBytes(bytes)
                writeBytes(bytes)
            }
            val bigItem = listOf(
                com.example.model.FileItem(
                    id = bigFile.absolutePath, name = bigFile.name, path = bigFile.absolutePath,
                    sizeBytes = bigFile.length(), lastModifiedMillis = bigFile.lastModified(),
                    isDirectory = false, mimeType = "application/octet-stream",
                    extension = "bin", category = null
                )
            )

            var caughtCancellation = false
            val job = launch(kotlinx.coroutines.Dispatchers.Default) {
                try {
                    repo.compressToZip(
                        items = bigItem,
                        destinationDirPath = destDir.absolutePath,
                        archiveName = "CancelledBundle.zip",
                        compressionLevel = com.example.model.ZipCompressionLevel.MAXIMUM
                    )
                } catch (ce: kotlinx.coroutines.CancellationException) {
                    caughtCancellation = true
                    throw ce
                }
            }
            kotlinx.coroutines.delay(15L)
            job.cancel()
            job.join()

            assertTrue(job.isCancelled)
            // Either it caught CancellationException mid-flight or completed instantaneously before 15ms;
            // if cancelled mid-flight, no corrupt archive may remain.
            if (caughtCancellation) {
                assertEquals(false, java.io.File(destDir, "CancelledBundle.zip").exists())
            }
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    @Test
    fun `swipe action math resolves delete trigger right reveal and collapsed states cleanly`() {
        val rightRevealPx = 300f
        val deleteTriggerPx = 180f

        // Right swipe past 45% or high velocity snaps open Move/Share tray
        assertEquals(
            com.example.ui.components.SwipeSettleTarget.REVEALED_RIGHT,
            com.example.ui.components.SwipeActionMath.resolveSettleTarget(
                offsetPx = 150f,
                velocityPxPerSec = 0f,
                rightRevealWidthPx = rightRevealPx,
                deleteTriggerPx = deleteTriggerPx
            )
        )
        assertEquals(
            com.example.ui.components.SwipeSettleTarget.REVEALED_RIGHT,
            com.example.ui.components.SwipeActionMath.resolveSettleTarget(
                offsetPx = 30f,
                velocityPxPerSec = 1000f,
                rightRevealWidthPx = rightRevealPx,
                deleteTriggerPx = deleteTriggerPx
            )
        )

        // Small partial drag settles back collapsed
        assertEquals(
            com.example.ui.components.SwipeSettleTarget.COLLAPSED,
            com.example.ui.components.SwipeActionMath.resolveSettleTarget(
                offsetPx = 60f,
                velocityPxPerSec = 0f,
                rightRevealWidthPx = rightRevealPx,
                deleteTriggerPx = deleteTriggerPx
            )
        )
        assertEquals(
            com.example.ui.components.SwipeSettleTarget.COLLAPSED,
            com.example.ui.components.SwipeActionMath.resolveSettleTarget(
                offsetPx = -70f,
                velocityPxPerSec = 0f,
                rightRevealWidthPx = rightRevealPx,
                deleteTriggerPx = deleteTriggerPx
            )
        )

        // Left swipe past delete threshold or brisk leftward fling triggers delete confirmation
        assertEquals(
            com.example.ui.components.SwipeSettleTarget.TRIGGER_DELETE,
            com.example.ui.components.SwipeActionMath.resolveSettleTarget(
                offsetPx = -190f,
                velocityPxPerSec = 0f,
                rightRevealWidthPx = rightRevealPx,
                deleteTriggerPx = deleteTriggerPx
            )
        )
        assertEquals(
            com.example.ui.components.SwipeSettleTarget.TRIGGER_DELETE,
            com.example.ui.components.SwipeActionMath.resolveSettleTarget(
                offsetPx = -80f,
                velocityPxPerSec = -1100f,
                rightRevealWidthPx = rightRevealPx,
                deleteTriggerPx = deleteTriggerPx
            )
        )

        // Rubber-band clamping bounds over-drag smoothly
        val clampedRight = com.example.ui.components.SwipeActionMath.clampSwipeOffset(
            candidateOffsetPx = 500f,
            maxLeftSwipePx = 220f,
            maxRightOpenPx = 300f
        )
        assertTrue(clampedRight > 300f && clampedRight <= 375f)

        val clampedLeft = com.example.ui.components.SwipeActionMath.clampSwipeOffset(
            candidateOffsetPx = -600f,
            maxLeftSwipePx = 220f,
            maxRightOpenPx = 300f
        )
        assertTrue(clampedLeft < -220f && clampedLeft >= -275f)
    }
}


