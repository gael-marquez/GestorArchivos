package com.example.gestorarchivos.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gestorarchivos.data.preferences.SortOrder
import com.example.gestorarchivos.data.preferences.ViewMode
import com.example.gestorarchivos.model.FileItem
import com.example.gestorarchivos.model.FileType
import com.example.gestorarchivos.ui.components.*
import com.example.gestorarchivos.ui.theme.AppTheme
import com.example.gestorarchivos.util.FileUtils
import com.example.gestorarchivos.viewmodel.FileExplorerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileExplorerScreen(
    viewModel: FileExplorerViewModel = viewModel(),
    onNavigateToViewer: (FileItem) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }
    var selectedFile by remember { mutableStateOf<FileItem?>(null) }
    var showFileOptions by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var fileToDelete by remember { mutableStateOf<FileItem?>(null) }

    // Manejar botón back del sistema
    BackHandler {
        if (uiState.selectedFiles.isNotEmpty()) {
            viewModel.clearSelection()
        } else if (!viewModel.navigateBack()) {
            // Si no hay más directorios en el stack, salir de la app
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.selectedFiles.isEmpty()) {
                            "Gestor de Archivos"
                        } else {
                            "${uiState.selectedFiles.size} seleccionado(s)"
                        }
                    )
                },
                navigationIcon = {
                    if (uiState.selectedFiles.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, "Cancelar selección")
                        }
                    } else if (uiState.navigationStack.size > 1) {
                        IconButton(onClick = { viewModel.navigateBack() }) {
                            Icon(Icons.Default.ArrowBack, "Atrás")
                        }
                    }
                },
                actions = {
                    if (uiState.selectedFiles.isEmpty()) {
                        // Buscar
                        IconButton(onClick = { /* TODO: Implementar búsqueda */ }) {
                            Icon(Icons.Default.Search, "Buscar")
                        }

                        // Cambiar vista
                        IconButton(onClick = {
                            viewModel.setViewMode(
                                if (uiState.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
                            )
                        }) {
                            Icon(
                                if (uiState.viewMode == ViewMode.LIST) Icons.Default.GridView else Icons.Default.ViewList,
                                "Cambiar vista"
                            )
                        }

                        // Menú
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "Más opciones")
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Crear carpeta") },
                                onClick = {
                                    showMenu = false
                                    showCreateFolderDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.CreateNewFolder, null)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Ordenar por") },
                                onClick = {
                                    showMenu = false
                                    showSortMenu = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Sort, null)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Tema") },
                                onClick = {
                                    showMenu = false
                                    showThemeMenu = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Palette, null)
                                }
                            )

                            Divider()

                            DropdownMenuItem(
                                text = { Text(if (uiState.showHiddenFiles) "Ocultar archivos ocultos" else "Mostrar archivos ocultos") },
                                onClick = {
                                    viewModel.setShowHiddenFiles(!uiState.showHiddenFiles)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        if (uiState.showHiddenFiles) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        null
                                    )
                                }
                            )
                        }

                        // Menú de ordenamiento
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortOrder.values().forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(getSortOrderName(order)) },
                                    onClick = {
                                        viewModel.setSortOrder(order)
                                        showSortMenu = false
                                    },
                                    trailingIcon = {
                                        if (uiState.sortOrder == order) {
                                            Icon(Icons.Default.Check, null)
                                        }
                                    }
                                )
                            }
                        }

                        // Menú de temas
                        DropdownMenu(
                            expanded = showThemeMenu,
                            onDismissRequest = { showThemeMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Guinda (IPN)") },
                                onClick = {
                                    viewModel.setAppTheme(AppTheme.GUINDA)
                                    showThemeMenu = false
                                },
                                trailingIcon = {
                                    if (uiState.appTheme == AppTheme.GUINDA) {
                                        Icon(Icons.Default.Check, null)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Azul (ESCOM)") },
                                onClick = {
                                    viewModel.setAppTheme(AppTheme.AZUL)
                                    showThemeMenu = false
                                },
                                trailingIcon = {
                                    if (uiState.appTheme == AppTheme.AZUL) {
                                        Icon(Icons.Default.Check, null)
                                    }
                                }
                            )
                        }
                    } else {
                        // Acciones para archivos seleccionados
                        IconButton(onClick = {
                            uiState.selectedFiles.forEach { file ->
                                viewModel.deleteFile(file.file)
                            }
                            viewModel.clearSelection()
                        }) {
                            Icon(Icons.Default.Delete, "Eliminar seleccionados")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (uiState.selectedFiles.isEmpty()) {
                FloatingActionButton(
                    onClick = { showCreateFolderDialog = true }
                ) {
                    Icon(Icons.Default.CreateNewFolder, "Crear carpeta")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Breadcrumb navigation
            BreadcrumbNavigationBar(
                navigationStack = uiState.navigationStack,
                onNavigateToPath = { index -> viewModel.navigateToPath(index) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Contenido principal
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = uiState.error!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Button(onClick = { viewModel.loadDirectory(uiState.currentDirectory) }) {
                                Text("Reintentar")
                            }
                        }
                    }
                }
                uiState.files.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Esta carpeta está vacía",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    if (uiState.viewMode == ViewMode.LIST) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.files, key = { it.path }) { fileItem ->
                                FileItemCard(
                                    fileItem = fileItem,
                                    isSelected = uiState.selectedFiles.contains(fileItem),
                                    onClick = {
                                        handleFileClick(
                                            fileItem = fileItem,
                                            context = context,
                                            viewModel = viewModel,
                                            onNavigateToViewer = onNavigateToViewer,
                                            onShowOptions = {
                                                selectedFile = fileItem
                                                showFileOptions = true
                                            }
                                        )
                                    },
                                    onLongClick = {
                                        viewModel.toggleFileSelection(fileItem)
                                    }
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.files, key = { it.path }) { fileItem ->
                                FileItemGrid(
                                    fileItem = fileItem,
                                    isSelected = uiState.selectedFiles.contains(fileItem),
                                    onClick = {
                                        handleFileClick(
                                            fileItem = fileItem,
                                            context = context,
                                            viewModel = viewModel,
                                            onNavigateToViewer = onNavigateToViewer,
                                            onShowOptions = {
                                                selectedFile = fileItem
                                                showFileOptions = true
                                            }
                                        )
                                    },
                                    onLongClick = {
                                        viewModel.toggleFileSelection(fileItem)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogos
    selectedFile?.let { file ->
        if (showFileOptions) {
            var isFav by remember { mutableStateOf(false) }
            LaunchedEffect(file.path) {
                isFav = viewModel.isFavorite(file.path)
            }

            FileOptionsDialog(
                fileItem = file,
                isFavorite = isFav,
                onDismiss = { showFileOptions = false },
                onOpen = {
                    if (FileUtils.canBeOpenedInternally(file.getFileType())) {
                        onNavigateToViewer(file)
                    } else {
                        FileUtils.openFileWithExternalApp(context, file.file)
                    }
                },
                onOpenWith = {
                    FileUtils.openFileWithExternalApp(context, file.file)
                },
                onShare = {
                    FileUtils.shareFile(context, file.file)
                },
                onRename = {
                    showRenameDialog = true
                },
                onDelete = {
                    fileToDelete = file
                    showDeleteDialog = true
                },
                onToggleFavorite = {
                    viewModel.toggleFavorite(file)
                },
                onShowDetails = {
                    showDetailsDialog = true
                }
            )
        }

        if (showRenameDialog) {
            RenameDialog(
                fileItem = file,
                onConfirm = { newName ->
                    viewModel.renameFile(file.file, newName)
                    showRenameDialog = false
                    showFileOptions = false
                },
                onDismiss = { showRenameDialog = false }
            )
        }

        if (showDetailsDialog) {
            FileDetailsDialog(
                fileItem = file,
                onDismiss = {
                    showDetailsDialog = false
                    showFileOptions = false
                }
            )
        }
    }

    fileToDelete?.let { file ->
        if (showDeleteDialog) {
            DeleteConfirmationDialog(
                fileItem = file,
                onConfirm = {
                    viewModel.deleteFile(file.file)
                    showDeleteDialog = false
                    showFileOptions = false
                    fileToDelete = null
                },
                onDismiss = {
                    showDeleteDialog = false
                    fileToDelete = null
                }
            )
        }
    }

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onConfirm = { name ->
                viewModel.createDirectory(name)
                showCreateFolderDialog = false
            },
            onDismiss = { showCreateFolderDialog = false }
        )
    }
}

private fun handleFileClick(
    fileItem: FileItem,
    context: Context,
    viewModel: FileExplorerViewModel,
    onNavigateToViewer: (FileItem) -> Unit,
    onShowOptions: () -> Unit
) {
    if (fileItem.isDirectory) {
        viewModel.navigateToDirectory(fileItem.file)
    } else {
        viewModel.addToRecent(fileItem)

        when (fileItem.getFileType()) {
            FileType.IMAGE, FileType.TEXT, FileType.JSON, FileType.XML -> {
                onNavigateToViewer(fileItem)
            }
            else -> {
                onShowOptions()
            }
        }
    }
}

private fun getSortOrderName(order: SortOrder): String {
    return when (order) {
        SortOrder.NAME_ASC -> "Nombre (A-Z)"
        SortOrder.NAME_DESC -> "Nombre (Z-A)"
        SortOrder.SIZE_ASC -> "Tamaño (menor a mayor)"
        SortOrder.SIZE_DESC -> "Tamaño (mayor a menor)"
        SortOrder.DATE_ASC -> "Fecha (antigua a reciente)"
        SortOrder.DATE_DESC -> "Fecha (reciente a antigua)"
        SortOrder.TYPE -> "Tipo"
    }
}