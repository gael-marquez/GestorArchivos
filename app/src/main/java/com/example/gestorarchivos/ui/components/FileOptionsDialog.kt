package com.example.gestorarchivos.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gestorarchivos.model.FileItem

@Composable
fun FileOptionsDialog(
    fileItem: FileItem,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onOpenWith: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShowDetails: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = fileItem.name,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Abrir
                if (!fileItem.isDirectory) {
                    FileOptionItem(
                        icon = Icons.Default.OpenInNew,
                        text = "Abrir",
                        onClick = {
                            onOpen()
                            onDismiss()
                        }
                    )
                }

                // Abrir con
                if (!fileItem.isDirectory) {
                    FileOptionItem(
                        icon = Icons.Default.Launch,
                        text = "Abrir con...",
                        onClick = {
                            onOpenWith()
                            onDismiss()
                        }
                    )
                }

                // Compartir
                if (!fileItem.isDirectory) {
                    FileOptionItem(
                        icon = Icons.Default.Share,
                        text = "Compartir",
                        onClick = {
                            onShare()
                            onDismiss()
                        }
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Favorito
                FileOptionItem(
                    icon = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    text = if (isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                    onClick = {
                        onToggleFavorite()
                        onDismiss()
                    }
                )

                // Renombrar
                if (fileItem.canWrite) {
                    FileOptionItem(
                        icon = Icons.Default.Edit,
                        text = "Renombrar",
                        onClick = {
                            onRename()
                            onDismiss()
                        }
                    )
                }

                // Detalles
                FileOptionItem(
                    icon = Icons.Default.Info,
                    text = "Detalles",
                    onClick = {
                        onShowDetails()
                        onDismiss()
                    }
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Eliminar
                if (fileItem.canWrite) {
                    FileOptionItem(
                        icon = Icons.Default.Delete,
                        text = "Eliminar",
                        onClick = {
                            onDelete()
                            onDismiss()
                        },
                        isDestructive = true
                    )
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
private fun FileOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isDestructive) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}