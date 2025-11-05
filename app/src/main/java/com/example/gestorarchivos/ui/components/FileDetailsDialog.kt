package com.example.gestorarchivos.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gestorarchivos.model.FileItem

@Composable
fun FileDetailsDialog(
    fileItem: FileItem,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Detalles")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailRow(label = "Nombre:", value = fileItem.name)
                DetailRow(label = "Tipo:", value = if (fileItem.isDirectory) "Carpeta" else fileItem.getFileType().name)
                DetailRow(label = "Tamaño:", value = fileItem.getFormattedSize())
                DetailRow(label = "Ruta:", value = fileItem.path)
                DetailRow(label = "Última modificación:", value = fileItem.getFormattedDate())
                DetailRow(
                    label = "Permisos:",
                    value = buildString {
                        if (fileItem.canRead) append("Lectura ")
                        if (fileItem.canWrite) append("Escritura")
                        if (!fileItem.canRead && !fileItem.canWrite) append("Sin permisos")
                    }
                )
                if (!fileItem.isDirectory && fileItem.extension.isNotEmpty()) {
                    DetailRow(label = "Extensión:", value = ".${fileItem.extension}")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}