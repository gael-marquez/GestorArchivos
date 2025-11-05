package com.example.gestorarchivos.data.preferences
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.gestorarchivos.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {

    companion object {
        val THEME_KEY = stringPreferencesKey("app_theme")
        val SORT_ORDER_KEY = stringPreferencesKey("sort_order")
        val SHOW_HIDDEN_FILES_KEY = stringPreferencesKey("show_hidden_files")
        val VIEW_MODE_KEY = stringPreferencesKey("view_mode")
    }

    val appTheme: Flow<AppTheme> = context.dataStore.data.map { preferences ->
        val themeName = preferences[THEME_KEY] ?: AppTheme.GUINDA.name
        try {
            AppTheme.valueOf(themeName)
        } catch (e: IllegalArgumentException) {
            AppTheme.GUINDA
        }
    }

    suspend fun setAppTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme.name
        }
    }

    val sortOrder: Flow<SortOrder> = context.dataStore.data.map { preferences ->
        val orderName = preferences[SORT_ORDER_KEY] ?: SortOrder.NAME_ASC.name
        try {
            SortOrder.valueOf(orderName)
        } catch (e: IllegalArgumentException) {
            SortOrder.NAME_ASC
        }
    }

    suspend fun setSortOrder(order: SortOrder) {
        context.dataStore.edit { preferences ->
            preferences[SORT_ORDER_KEY] = order.name
        }
    }

    val showHiddenFiles: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_HIDDEN_FILES_KEY]?.toBoolean() ?: false
    }

    suspend fun setShowHiddenFiles(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_HIDDEN_FILES_KEY] = show.toString()
        }
    }

    val viewMode: Flow<ViewMode> = context.dataStore.data.map { preferences ->
        val modeName = preferences[VIEW_MODE_KEY] ?: ViewMode.LIST.name
        try {
            ViewMode.valueOf(modeName)
        } catch (e: IllegalArgumentException) {
            ViewMode.LIST
        }
    }

    suspend fun setViewMode(mode: ViewMode) {
        context.dataStore.edit { preferences ->
            preferences[VIEW_MODE_KEY] = mode.name
        }
    }
}

enum class SortOrder {
    NAME_ASC,
    NAME_DESC,
    SIZE_ASC,
    SIZE_DESC,
    DATE_ASC,
    DATE_DESC,
    TYPE
}

enum class ViewMode {
    LIST,
    GRID
}