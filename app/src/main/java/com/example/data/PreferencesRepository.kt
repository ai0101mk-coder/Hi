package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.FileActivityEvent
import com.example.model.FileOperationType
import com.example.model.SortDirection
import com.example.model.SortOption
import com.example.model.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "visual_explorer_prefs")

object ActivityLogSerializer {
    const val MAX_EVENTS = 100

    fun encode(events: List<FileActivityEvent>): String {
        val array = JSONArray()
        events.take(MAX_EVENTS).forEach { event ->
            val obj = JSONObject().apply {
                put("id", event.id)
                put("type", event.type.name)
                put("title", event.title)
                put("detail", event.detail)
                put("targetFolderPath", event.targetFolderPath)
                put("itemCount", event.itemCount)
                put("totalBytes", event.totalBytes)
                put("timestampMillis", event.timestampMillis)
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun decode(raw: String?): List<FileActivityEvent> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            val result = ArrayList<FileActivityEvent>(array.length())
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val typeName = obj.optString("type", "")
                val opType = runCatching { FileOperationType.valueOf(typeName) }.getOrNull() ?: continue
                result.add(
                    FileActivityEvent(
                        id = obj.optString("id", ""),
                        type = opType,
                        title = obj.optString("title", ""),
                        detail = obj.optString("detail", ""),
                        targetFolderPath = obj.optString("targetFolderPath", ""),
                        itemCount = obj.optInt("itemCount", 1),
                        totalBytes = obj.optLong("totalBytes", 0L),
                        timestampMillis = obj.optLong("timestampMillis", 0L)
                    )
                )
            }
            result.take(MAX_EVENTS)
        }.getOrDefault(emptyList())
    }
}

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
        val FAVOURITE_PATHS = stringSetPreferencesKey("favourite_paths")
        val SHOW_HIDDEN = booleanPreferencesKey("show_hidden")
        val SORT_OPTION = stringPreferencesKey("sort_option")
        val SORT_DIRECTION = stringPreferencesKey("sort_direction")
        val ACTIVITY_HISTORY_JSON = stringPreferencesKey("activity_history_json")
    }

    val themePreference: Flow<ThemePreference> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.THEME_PREFERENCE] ?: ThemePreference.SYSTEM.name
        runCatching { ThemePreference.valueOf(raw) }.getOrDefault(ThemePreference.SYSTEM)
    }

    val favouritePaths: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.FAVOURITE_PATHS] ?: emptySet()
    }

    val showHiddenFiles: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SHOW_HIDDEN] ?: false
    }

    val sortOption: Flow<SortOption> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.SORT_OPTION] ?: SortOption.DATE.name
        runCatching { SortOption.valueOf(raw) }.getOrDefault(SortOption.DATE)
    }

    val sortDirection: Flow<SortDirection> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.SORT_DIRECTION] ?: SortDirection.DESCENDING.name
        runCatching { SortDirection.valueOf(raw) }.getOrDefault(SortDirection.DESCENDING)
    }

    val activityEvents: Flow<List<FileActivityEvent>> = context.dataStore.data.map { prefs ->
        ActivityLogSerializer.decode(prefs[Keys.ACTIVITY_HISTORY_JSON])
    }

    suspend fun setThemePreference(preference: ThemePreference) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_PREFERENCE] = preference.name
        }
    }

    suspend fun toggleFavourite(path: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.FAVOURITE_PATHS] ?: emptySet()
            prefs[Keys.FAVOURITE_PATHS] = if (path in current) {
                current - path
            } else {
                current + path
            }
        }
    }

    suspend fun setFavouritesBulk(paths: Collection<String>, makeFavourite: Boolean) {
        if (paths.isEmpty()) return
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.FAVOURITE_PATHS] ?: emptySet()
            prefs[Keys.FAVOURITE_PATHS] = if (makeFavourite) {
                current + paths
            } else {
                current - paths.toSet()
            }
        }
    }

    suspend fun setShowHiddenFiles(show: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SHOW_HIDDEN] = show
        }
    }

    suspend fun setSort(option: SortOption, direction: SortDirection) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SORT_OPTION] = option.name
            prefs[Keys.SORT_DIRECTION] = direction.name
        }
    }

    suspend fun appendActivityEvent(event: FileActivityEvent) {
        context.dataStore.edit { prefs ->
            val existing = ActivityLogSerializer.decode(prefs[Keys.ACTIVITY_HISTORY_JSON])
            val updated = (listOf(event) + existing).take(ActivityLogSerializer.MAX_EVENTS)
            prefs[Keys.ACTIVITY_HISTORY_JSON] = ActivityLogSerializer.encode(updated)
        }
    }

    suspend fun clearActivityHistory() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.ACTIVITY_HISTORY_JSON)
        }
    }
}
