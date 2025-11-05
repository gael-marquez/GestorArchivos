package com.example.gestorarchivos.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.gestorarchivos.model.FileItem

@Composable
fun DeleteConfirmationDialog(
    fileItem: FileItem,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(text = "¿Eliminar ${if (fileItem.isDirectory) "carpeta" else "archivo"}?")
        },
        text = {
            Text(
                text = if (fileItem.isDirectory) {
                    "Se eliminará la carpeta \"${fileItem.name}\" y todo su contenido. Esta acción no se puede deshacer."
                } else {
                    "Se eliminará el archivo \"${fileItem.name}\". Esta acción no se puede deshacer."
                }
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Eliminar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}