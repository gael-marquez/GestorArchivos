package com.example.gestorarchivos.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gestorarchivos.model.BluetoothDeviceInfo
import java.io.File

data class FileItem(
    val name: String,
    val path: String,
    val size: Long,
    val isDirectory: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileSelectionScreen(
    connectedDevice: BluetoothDeviceInfo,
    onFileSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    var selectedFiles by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var currentDirectory by remember { mutableStateOf("/storage/emulated/0/Download") }
    var files by remember { mutableStateOf<List<FileItem>>(emptyList()) }

    val context = LocalContext.current

    // Launcher para seleccionar archivos del sistema
    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Convertir URI a path real
            val path = getRealPathFromURI(context, uri)
            if (path != null) {
                onFileSelected(path)
            }
        }
    }

    val multipleFilePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            val path = getRealPathFromURI(context, uri)
            if (path != null) {
                onFileSelected(path)
            }
        }
    }

    // Cargar archivos del directorio actual
    LaunchedEffect(currentDirectory) {
        loadFilesFromDirectory(currentDirectory) { fileList ->
            files = fileList
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        CenterAlignedTopAppBar(
            title = {
                Text(
                    "Enviar a ${connectedDevice.name}",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Botones de selección rápida
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { filePickerLauncher.launch("*/*") },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Description, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Archivo")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { filePickerLauncher.launch("image/*") },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Imagen")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { multipleFilePickerLauncher.launch("*/*") },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.SelectAll, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Múltiples")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Explorador de archivos básico
        Text(
            "Explorador de Archivos",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Directorio: $currentDirectory",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Botón para ir al directorio padre
            if (currentDirectory != "/") {
                item {
                    FileItemCard(
                        file = FileItem("..", "", 0, true),
                        onClick = {
                            val parentDir = File(currentDirectory).parent
                            if (parentDir != null) {
                                currentDirectory = parentDir
                            }
                        }
                    )
                }
            }

            items(files) { file ->
                FileItemCard(
                    file = file,
                    onClick = {
                        if (file.isDirectory) {
                            currentDirectory = file.path
                        } else {
                            onFileSelected(file.path)
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileItemCard(
    file: FileItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (file.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (file.isDirectory)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                if (!file.isDirectory && file.size > 0) {
                    Text(
                        formatFileSize(file.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                Icons.Default.Send,
                contentDescription = "Enviar",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// Helper functions
private fun loadFilesFromDirectory(
    directoryPath: String,
    onResult: (List<FileItem>) -> Unit
) {
    try {
        val directory = File(directoryPath)
        if (directory.exists() && directory.canRead()) {
            val filesList = directory.listFiles()?.mapNotNull { file ->
                try {
                    FileItem(
                        name = file.name,
                        path = file.absolutePath,
                        size = if (file.isFile) file.length() else 0,
                        isDirectory = file.isDirectory
                    )
                } catch (e: Exception) {
                    null
                }
            }?.sortedWith(compareBy<FileItem> { !it.isDirectory }.thenBy { it.name }) ?: emptyList()

            onResult(filesList)
        } else {
            onResult(emptyList())
        }
    } catch (e: Exception) {
        onResult(emptyList())
    }
}

private fun getRealPathFromURI(context: android.content.Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val tempFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}")
        inputStream?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        tempFile.absolutePath
    } catch (e: Exception) {
        null
    }
}

private fun formatFileSize(bytes: Long): String {
    val units = arrayOf("B", "KB", "MB", "GB")
    var size = bytes.toDouble()
    var unitIndex = 0

    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }

    return "%.1f %s".format(size, units[unitIndex])
}