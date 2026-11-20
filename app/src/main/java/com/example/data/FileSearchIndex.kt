package com.example.data

import com.example.model.FileCategory
import com.example.model.FileItem
import java.util.EnumMap
import java.util.Locale

data class IndexedEntry(
    val item: FileItem,
    val nameLower: String,
    val pathLower: String,
    val extLower: String,
    val tokens: List<String>
)

data class SearchQueryResult(
    val results: List<FileItem>,
    val categoryHitCounts: Map<FileCategory, Int>,
    val totalMatches: Int
)

/**
 * In-memory searchable file index enabling instant multi-token and category-filtered
 * lookups across all indexed files and folders without re-walking storage per query.
 */
class FileSearchIndex {

    @Volatile
    private var entries: List<IndexedEntry> = emptyList()

    @Volatile
    private var categoryBuckets: Map<FileCategory, List<IndexedEntry>> = emptyMap()

    @Volatile
    var lastUpdatedMillis: Long = 0L
        private set

    val size: Int
        get() = entries.size

    fun rebuild(items: List<FileItem>) {
        val newEntries = ArrayList<IndexedEntry>(items.size)
        val buckets = EnumMap<FileCategory, MutableList<IndexedEntry>>(FileCategory::class.java)
        FileCategory.entries.forEach { buckets[it] = mutableListOf() }

        for (item in items) {
            val nameLower = item.name.lowercase(Locale.ROOT)
            val pathLower = item.path.lowercase(Locale.ROOT)
            val extLower = item.extension.lowercase(Locale.ROOT)
            val tokens = nameLower.split('_', '-', '.', ' ', '(', ')', '[', ']')
                .filter { it.isNotBlank() }

            val entry = IndexedEntry(
                item = item,
                nameLower = nameLower,
                pathLower = pathLower,
                extLower = extLower,
                tokens = tokens
            )
            newEntries.add(entry)
            item.category?.let { cat -> buckets[cat]?.add(entry) }
        }

        entries = newEntries
        categoryBuckets = buckets
        lastUpdatedMillis = System.currentTimeMillis()
    }

    fun updateFavourites(favourites: Set<String>) {
        if (entries.isEmpty()) return
        val updated = entries.map { entry ->
            val isFav = entry.item.path in favourites
            if (entry.item.isFavourite == isFav) entry
            else entry.copy(item = entry.item.copy(isFavourite = isFav))
        }
        val buckets = EnumMap<FileCategory, MutableList<IndexedEntry>>(FileCategory::class.java)
        FileCategory.entries.forEach { buckets[it] = mutableListOf() }
        for (entry in updated) {
            entry.item.category?.let { cat -> buckets[cat]?.add(entry) }
        }
        entries = updated
        categoryBuckets = buckets
    }

    fun search(
        query: String,
        categoryFilter: FileCategory? = null,
        limit: Int = 200
    ): SearchQueryResult {
        val trimmed = query.trim().lowercase(Locale.ROOT)
        val queryTerms = if (trimmed.isEmpty()) emptyList() else trimmed.split(Regex("\\s+")).filter { it.isNotEmpty() }

        val counts = EnumMap<FileCategory, Int>(FileCategory::class.java)
        FileCategory.entries.forEach { counts[it] = 0 }

        val allMatched = ArrayList<Pair<Int, FileItem>>()

        // Evaluate against the full index once so per-category chip counts are always accurate
        for (entry in entries) {
            val score = computeMatchScore(entry, trimmed, queryTerms)
            if (score <= 0) continue

            entry.item.category?.let { cat ->
                counts[cat] = (counts[cat] ?: 0) + 1
            }

            if (categoryFilter == null || entry.item.category == categoryFilter) {
                allMatched.add(score to entry.item)
            }
        }

        val sorted = allMatched
            .sortedWith(
                compareByDescending<Pair<Int, FileItem>> { it.first }
                    .thenByDescending { it.second.lastModifiedMillis }
            )
            .take(limit)
            .map { it.second }

        return SearchQueryResult(
            results = sorted,
            categoryHitCounts = counts.toMap(),
            totalMatches = counts.values.sum()
        )
    }

    private fun computeMatchScore(
        entry: IndexedEntry,
        fullQuery: String,
        terms: List<String>
    ): Int {
        if (terms.isEmpty()) return 1

        // Every term must match either filename, extension, or path
        for (term in terms) {
            val matchesTerm = entry.nameLower.contains(term) ||
                entry.extLower == term.removePrefix(".") ||
                entry.pathLower.contains(term)
            if (!matchesTerm) return 0
        }

        return when {
            entry.nameLower == fullQuery -> 100
            entry.nameLower.startsWith(fullQuery) -> 80
            entry.tokens.any { it.startsWith(terms.first()) } -> 60
            entry.nameLower.contains(fullQuery) -> 45
            else -> 20 // Multi-token or path match
        }
    }
}
