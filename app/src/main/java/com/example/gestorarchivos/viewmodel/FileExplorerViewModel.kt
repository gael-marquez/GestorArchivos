package com.example.gestorarchivos.viewmodel

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestorarchivos.data.preferences.PreferencesManager
import com.example.gestorarchivos.data.preferences.SortOrder
import com.example.gestorarchivos.data.preferences.ViewMode
import com.example.gestorarchivos.data.repository.FileRepository
import com.example.gestorarchivos.model.FileItem
import com.example.gestorarchivos.ui.theme.AppTheme
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

data class FileExplorerUiState(
    val currentDirectory: File = Environment.getExternalStorageDirectory(),
    val files: List<FileItem> = emptyList(),
    val navigationStack: List<File> = listOf(Environment.getExternalStorageDirectory()),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedFiles: Set<FileItem> = emptySet(),
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<FileItem> = emptyList(),
    val sortOrder: SortOrder = SortOrder.NAME_ASC,
    val viewMode: ViewMode = ViewMode.LIST,
    val showHiddenFiles: Boolean = false,
    val appTheme: AppTheme = AppTheme.GUINDA
)

class FileExplorerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FileRepository(application)
    private val preferencesManager = PreferencesManager(application)

    private val _uiState = MutableStateFlow(FileExplorerUiState())
    val uiState: StateFlow<FileExplorerUiState> = _uiState.asStateFlow()

    val recentFiles = repository.getRecentFiles()
    val favoriteFiles = repository.getFavorites()

    init {
        loadPreferences()
        loadDirectory(_uiState.value.currentDirectory)
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            preferencesManager.appTheme.collect { theme ->
                _uiState.update { it.copy(appTheme = theme) }
            }
        }

        viewModelScope.launch {
            preferencesManager.sortOrder.collect { order ->
                _uiState.update { it.copy(sortOrder = order) }
                sortFiles(order)
            }
        }

        viewModelScope.launch {
            preferencesManager.viewMode.collect { mode ->
                _uiState.update { it.copy(viewMode = mode) }
            }
        }

        viewModelScope.launch {
            preferencesManager.showHiddenFiles.collect { show ->
                _uiState.update { it.copy(showHiddenFiles = show) }
                loadDirectory(_uiState.value.currentDirectory)
            }
        }
    }

    fun loadDirectory(directory: File) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.getFilesInDirectory(directory).fold(
                onSuccess = { files ->
                    var filteredFiles = files

                    // Filtrar archivos ocultos si es necesario
                    if (!_uiState.value.showHiddenFiles) {
                        filteredFiles = filteredFiles.filter { !it.name.startsWith(".") }
                    }

                    _uiState.update {
                        it.copy(
                            currentDirectory = directory,
                            files = sortFilesByOrder(filteredFiles, it.sortOrder),
                            isLoading = false,
                            error = null
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Error al cargar el directorio"
                        )
                    }
                }
            )
        }
    }

    fun navigateToDirectory(directory: File) {
        val currentStack = _uiState.value.navigationStack.toMutableList()
        currentStack.add(directory)
        _uiState.update { it.copy(navigationStack = currentStack) }
        loadDirectory(directory)
    }

    fun navigateBack(): Boolean {
        val currentStack = _uiState.value.navigationStack
        return if (currentStack.size > 1) {
            val newStack = currentStack.dropLast(1)
            val previousDirectory = newStack.last()
            _uiState.update { it.copy(navigationStack = newStack) }
            loadDirectory(previousDirectory)
            true
        } else {
            false
        }
    }

    fun navigateToPath(index: Int) {
        val currentStack = _uiState.value.navigationStack
        if (index in currentStack.indices) {
            val newStack = currentStack.subList(0, index + 1)
            val directory = newStack.last()
            _uiState.update { it.copy(navigationStack = newStack) }
            loadDirectory(directory)
        }
    }

    fun searchFiles(query: String) {
        if (query.isBlank()) {
            _uiState.update { it.copy(searchQuery = "", isSearching = false, searchResults = emptyList()) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(searchQuery = query, isSearching = true) }

            val results = repository.searchFiles(query, _uiState.value.currentDirectory)

            _uiState.update { it.copy(searchResults = results, isSearching = false) }
        }
    }

    fun clearSearch() {
        _uiState.update { it.copy(searchQuery = "", searchResults = emptyList()) }
    }

    fun toggleFileSelection(fileItem: FileItem) {
        val currentSelected = _uiState.value.selectedFiles.toMutableSet()
        if (currentSelected.contains(fileItem)) {
            currentSelected.remove(fileItem)
        } else {
            currentSelected.add(fileItem)
        }
        _uiState.update { it.copy(selectedFiles = currentSelected) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedFiles = emptySet()) }
    }

    fun deleteFile(file: File) {
        viewModelScope.launch {
            repository.deleteFile(file).fold(
                onSuccess = {
                    loadDirectory(_uiState.value.currentDirectory)
                },
                onFailure = { exception ->
                    _uiState.update { it.copy(error = exception.message ?: "Error al eliminar") }
                }
            )
        }
    }

    fun renameFile(file: File, newName: String) {
        viewModelScope.launch {
            repository.renameFile(file, newName).fold(
                onSuccess = {
                    loadDirectory(_uiState.value.currentDirectory)
                },
                onFailure = { exception ->
                    _uiState.update { it.copy(error = exception.message ?: "Error al renombrar") }
                }
            )
        }
    }

    fun createDirectory(name: String) {
        viewModelScope.launch {
            repository.createDirectory(_uiState.value.currentDirectory, name).fold(
                onSuccess = {
                    loadDirectory(_uiState.value.currentDirectory)
                },
                onFailure = { exception ->
                    _uiState.update { it.copy(error = exception.message ?: "Error al crear carpeta") }
                }
            )
        }
    }

    fun addToRecent(fileItem: FileItem) {
        viewModelScope.launch {
            repository.addRecentFile(fileItem)
        }
    }

    fun toggleFavorite(fileItem: FileItem) {
        viewModelScope.launch {
            if (repository.isFavorite(fileItem.path)) {
                repository.removeFavorite(fileItem.path)
            } else {
                repository.addFavorite(fileItem)
            }
        }
    }

    suspend fun isFavorite(path: String): Boolean {
        return repository.isFavorite(path)
    }

    fun setSortOrder(order: SortOrder) {
        viewModelScope.launch {
            preferencesManager.setSortOrder(order)
        }
    }

    fun setViewMode(mode: ViewMode) {
        viewModelScope.launch {
            preferencesManager.setViewMode(mode)
        }
    }

    fun setAppTheme(theme: AppTheme) {
        viewModelScope.launch {
            preferencesManager.setAppTheme(theme)
        }
    }

    fun setShowHiddenFiles(show: Boolean) {
        viewModelScope.launch {
            preferencesManager.setShowHiddenFiles(show)
        }
    }

    private fun sortFiles(order: SortOrder) {
        _uiState.update { state ->
            state.copy(files = sortFilesByOrder(state.files, order))
        }
    }

    private fun sortFilesByOrder(files: List<FileItem>, order: SortOrder): List<FileItem> {
        return when (order) {
            SortOrder.NAME_ASC -> files.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            SortOrder.NAME_DESC -> files.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })).reversed()
            SortOrder.SIZE_ASC -> files.sortedWith(compareBy({ !it.isDirectory }, { it.size }))
            SortOrder.SIZE_DESC -> files.sortedWith(compareBy({ !it.isDirectory }, { -it.size }))
            SortOrder.DATE_ASC -> files.sortedWith(compareBy({ !it.isDirectory }, { it.lastModified }))
            SortOrder.DATE_DESC -> files.sortedWith(compareBy({ !it.isDirectory }, { -it.lastModified }))
            SortOrder.TYPE -> files.sortedWith(compareBy({ !it.isDirectory }, { it.extension }))
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}