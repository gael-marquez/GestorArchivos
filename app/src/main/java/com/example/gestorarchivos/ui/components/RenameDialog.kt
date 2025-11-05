package com.example.gestorarchivos.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gestorarchivos.model.FileItem

@Composable
fun RenameDialog(
    fileItem: FileItem,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf(fileItem.name) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Renombrar ${if (fileItem.isDirectory) "carpeta" else "archivo"}")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = newName,
                    onValueChange = {
                        newName = it
                        errorMessage = when {
                            it.isBlank() -> "El nombre no puede estar vacío"
                            it.contains("/") -> "El nombre no puede contener /"
                            it.contains("\\") -> "El nombre no puede contener \\"
                            else -> null
                        }
                    },
                    label = { Text("Nuevo nombre") },
                    isError = errorMessage != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newName.isNotBlank() && errorMessage == null) {
                        onConfirm(newName)
                    }
                },
                enabled = newName.isNotBlank() && errorMessage == null
            ) {
                Text("Renombrar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}