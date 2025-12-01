package com.example.gestorarchivos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gestorarchivos.model.FileTransfer
import com.example.gestorarchivos.model.TransferProgress
import com.example.gestorarchivos.model.TransferStatus
import com.example.gestorarchivos.viewmodel.BluetoothViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransferManagerDialog(
    viewModel: BluetoothViewModel,
    onDismiss: () -> Unit
) {
    val activeTransfers by viewModel.activeTransfers.collectAsState()
    val transferHistory by viewModel.transferHistory.collectAsState()
    val transferProgress by viewModel.transferProgress.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Gestor de Transferencias",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                // Tabs
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Activas") },
                        icon = { Icon(Icons.Default.CloudSync, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Historial") },
                        icon = { Icon(Icons.Default.History, contentDescription = null) }
                    )
                }

                // Content
                when (selectedTab) {
                    0 -> ActiveTransfersTab(
                        transfers = activeTransfers,
                        progress = transferProgress,
                        onCancelTransfer = { viewModel.cancelTransfer(it) }
                    )
                    1 -> TransferHistoryTab(
                        history = transferHistory
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveTransfersTab(
    transfers: List<FileTransfer>,
    progress: Map<String, TransferProgress>,
    onCancelTransfer: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (transfers.isEmpty()) {
            item {
                EmptyStateCard("No hay transferencias activas")
            }
        } else {
            items(transfers) { transfer ->
                ActiveTransferCard(
                    transfer = transfer,
                    progress = progress[transfer.id],
                    onCancel = { onCancelTransfer(transfer.id) }
                )
            }
        }
    }
}

@Composable
fun TransferHistoryTab(
    history: List<FileTransfer>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (history.isEmpty()) {
            item {
                EmptyStateCard("No hay historial de transferencias")
            }
        } else {
            items(history) { transfer ->
                HistoryTransferCard(transfer = transfer)
            }
        }
    }
}

@Composable
fun ActiveTransferCard(
    transfer: FileTransfer,
    progress: TransferProgress?,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        transfer.fileName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        if (transfer.isIncoming) "De: ${transfer.deviceName}" else "Para: ${transfer.deviceName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        formatFileSize(transfer.fileSize),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Status icon and cancel button
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (transfer.status) {
                        TransferStatus.TRANSFERRING -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        TransferStatus.CONNECTING -> {
                            Icon(
                                Icons.Default.BluetoothSearching,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        TransferStatus.PENDING, TransferStatus.QUEUED -> {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        else -> {}
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (transfer.status in listOf(
                            TransferStatus.PENDING,
                            TransferStatus.CONNECTING,
                            TransferStatus.TRANSFERRING,
                            TransferStatus.QUEUED
                        )) {
                        IconButton(
                            onClick = onCancel,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Cancel,
                                contentDescription = "Cancelar",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            if (transfer.status == TransferStatus.TRANSFERRING) {
                Column {
                    LinearProgressIndicator(
                        progress = transfer.progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${(transfer.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall
                        )

                        progress?.let {
                            Text(
                                formatSpeed(it.speedBytesPerSecond),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            } else {
                // Status text
                Text(
                    getStatusText(transfer.status),
                    style = MaterialTheme.typography.bodySmall,
                    color = getStatusColor(transfer.status),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun HistoryTransferCard(
    transfer: FileTransfer
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        transfer.fileName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        if (transfer.isIncoming) "De: ${transfer.deviceName}" else "Para: ${transfer.deviceName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        formatFileSize(transfer.fileSize),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        formatDate(transfer.startTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Status icon
                Icon(
                    when (transfer.status) {
                        TransferStatus.COMPLETED -> Icons.Default.CheckCircle
                        TransferStatus.FAILED -> Icons.Default.Error
                        TransferStatus.CANCELLED -> Icons.Default.Cancel
                        else -> Icons.Default.Info
                    },
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = getStatusColor(transfer.status)
                )
            }

            if (transfer.status != TransferStatus.COMPLETED) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    getStatusText(transfer.status),
                    style = MaterialTheme.typography.bodySmall,
                    color = getStatusColor(transfer.status),
                    fontWeight = FontWeight.Medium
                )

                transfer.errorMessage?.let { error ->
                    Text(
                        error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    message: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Helper functions
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

private fun formatSpeed(bytesPerSecond: Long): String {
    return "${formatFileSize(bytesPerSecond)}/s"
}

private fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return formatter.format(date)
}

@Composable
private fun getStatusColor(status: TransferStatus): androidx.compose.ui.graphics.Color {
    return when (status) {
        TransferStatus.COMPLETED -> MaterialTheme.colorScheme.primary
        TransferStatus.FAILED -> MaterialTheme.colorScheme.error
        TransferStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
        TransferStatus.TRANSFERRING -> MaterialTheme.colorScheme.primary
        TransferStatus.CONNECTING -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

private fun getStatusText(status: TransferStatus): String {
    return when (status) {
        TransferStatus.PENDING -> "Pendiente"
        TransferStatus.CONNECTING -> "Conectando..."
        TransferStatus.TRANSFERRING -> "Transfiriendo..."
        TransferStatus.COMPLETED -> "Completado"
        TransferStatus.FAILED -> "Error"
        TransferStatus.CANCELLED -> "Cancelado"
        TransferStatus.QUEUED -> "En cola"
    }
}