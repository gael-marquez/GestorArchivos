package com.example.gestorarchivos.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gestorarchivos.model.FileItem
import com.example.gestorarchivos.model.FileType

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItemCard(
    fileItem: FileItem,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono del archivo
            Icon(
                imageVector = getFileIcon(fileItem.getFileType()),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = getFileIconColor(fileItem.getFileType())
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Información del archivo
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = fileItem.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = fileItem.getFormattedSize(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = fileItem.getFormattedDate(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Indicador de selección
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Seleccionado",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)  // ← AGREGUÉ ESTA LÍNEA
@Composable
fun FileItemGrid(
    fileItem: FileItem,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = getFileIcon(fileItem.getFileType()),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = getFileIconColor(fileItem.getFileType())
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = fileItem.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = fileItem.getFormattedSize(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isSelected) {
                Spacer(modifier = Modifier.height(4.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Seleccionado",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun getFileIcon(fileType: FileType): ImageVector {
    return when (fileType) {
        FileType.DIRECTORY -> Icons.Default.Folder
        FileType.IMAGE -> Icons.Default.Image
        FileType.TEXT -> Icons.Default.Description
        FileType.JSON, FileType.XML -> Icons.Default.Code
        FileType.VIDEO -> Icons.Default.VideoFile
        FileType.AUDIO -> Icons.Default.AudioFile
        FileType.PDF -> Icons.Default.PictureAsPdf
        FileType.ARCHIVE -> Icons.Default.FolderZip
        FileType.APK -> Icons.Default.Android
        FileType.UNKNOWN -> Icons.Default.InsertDriveFile
    }
}

@Composable
private fun getFileIconColor(fileType: FileType): androidx.compose.ui.graphics.Color {
    return when (fileType) {
        FileType.DIRECTORY -> MaterialTheme.colorScheme.primary
        FileType.IMAGE -> MaterialTheme.colorScheme.tertiary
        FileType.TEXT, FileType.JSON, FileType.XML -> MaterialTheme.colorScheme.secondary
        FileType.VIDEO -> MaterialTheme.colorScheme.error
        FileType.AUDIO -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}