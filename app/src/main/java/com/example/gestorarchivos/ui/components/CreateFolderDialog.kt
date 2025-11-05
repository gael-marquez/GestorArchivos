package com.example.gestorarchivos.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CreateFolderDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Crear nueva carpeta")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = {
                        folderName = it
                        errorMessage = when {
                            it.isBlank() -> "El nombre no puede estar vacío"
                            it.contains("/") -> "El nombre no puede contener /"
                            it.contains("\\") -> "El nombre no puede contener \\"
                            else -> null
                        }
                    },
                    label = { Text("Nombre de la carpeta") },
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
                    if (folderName.isNotBlank() && errorMessage == null) {
                        onConfirm(folderName)
                    }
                },
                enabled = folderName.isNotBlank() && errorMessage == null
            ) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}