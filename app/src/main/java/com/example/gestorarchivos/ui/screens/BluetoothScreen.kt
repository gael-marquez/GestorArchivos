package com.example.gestorarchivos.ui.screens

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gestorarchivos.bluetooth.BluetoothManager
import com.example.gestorarchivos.model.BluetoothDeviceInfo
import com.example.gestorarchivos.viewmodel.BluetoothViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothScreen(
    viewModel: BluetoothViewModel = viewModel(),
    onNavigateToFileManager: () -> Unit = {},
    onSendFile: () -> Unit = {} // AGREGAR ESTE PARÁMETRO
) {
    val context = LocalContext.current

    // Estados del ViewModel
    val hasPermissions by viewModel.hasPermissions.collectAsState()
    val missingPermissions by viewModel.missingPermissions.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val showTransferManager by viewModel.showTransferManager.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()


    // Launcher para permisos
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.updatePermissions()
    }

    // Launcher para habilitar Bluetooth
    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.startDeviceDiscovery()
        }
    }

    // Verificar y solicitar permisos al iniciar
    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            permissionLauncher.launch(viewModel.getRequiredPermissions())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Barra superior - CORREGIDA
        CenterAlignedTopAppBar(
            title = {
                Text(
                    "Bluetooth Transfer",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                IconButton(onClick = { viewModel.showTransferManager() }) {
                    Icon(Icons.Default.History, contentDescription = "Historial")
                }
                IconButton(onClick = onNavigateToFileManager) {
                    Icon(Icons.Default.Folder, contentDescription = "Archivos")
                }
                IconButton(
                    onClick = {
                        openDownloadsFolder(context)
                    }
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Archivos Recibidos")
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
        errorMessage?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        error,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )

                    TextButton(
                        onClick = { viewModel.clearError() }
                    ) {
                        Text("Cerrar")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Estado de permisos y Bluetooth
        if (!hasPermissions) {
            PermissionRequiredCard(
                missingPermissions = missingPermissions,
                onRequestPermissions = {
                    permissionLauncher.launch(viewModel.getRequiredPermissions())
                }
            )
        } else if (!viewModel.isBluetoothEnabled()) {
            BluetoothDisabledCard(
                onEnableBluetooth = {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    enableBluetoothLauncher.launch(enableBtIntent)
                }
            )
        } else {
            // Interfaz principal de Bluetooth
            BluetoothMainInterface(
                discoveredDevices = discoveredDevices,
                isScanning = isScanning,
                connectionStatus = connectionStatus,
                selectedDevice = selectedDevice,
                onStartScan = { viewModel.startDeviceDiscovery() },
                onStopScan = { viewModel.stopDeviceDiscovery() },
                onDeviceSelected = { viewModel.selectDevice(it) },
                onConnect = { viewModel.connectToDevice(it) },
                onStartServer = { viewModel.startServer() },
                onDisconnect = { viewModel.disconnect() },
                onSendFile = onSendFile
            )
        }
    }

    // Dialog del gestor de transferencias
    if (showTransferManager) {
        TransferManagerDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.hideTransferManager() }
        )
    }
}

@Composable
fun PermissionRequiredCard(
    missingPermissions: List<String>,
    onRequestPermissions: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Permisos Requeridos",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Se necesitan permisos de Bluetooth para continuar",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onRequestPermissions,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Otorgar Permisos")
            }
        }
    }
}

@Composable
fun BluetoothDisabledCard(
    onEnableBluetooth: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.BluetoothDisabled,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Bluetooth Desactivado",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Active el Bluetooth para continuar",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onEnableBluetooth,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Icon(Icons.Default.Bluetooth, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Activar Bluetooth")
            }
        }
    }
}


@Composable
fun BluetoothMainInterface(
    discoveredDevices: List<BluetoothDeviceInfo>,
    isScanning: Boolean,
    connectionStatus: BluetoothManager.ConnectionStatus,
    selectedDevice: BluetoothDeviceInfo?,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onDeviceSelected: (BluetoothDeviceInfo) -> Unit,
    onConnect: (BluetoothDeviceInfo) -> Unit,
    onStartServer: () -> Unit,
    onDisconnect: () -> Unit,
    onSendFile: () -> Unit = {} // AGREGAR ESTE PARÁMETRO
) {
    Column {
        // Panel de control
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = if (isScanning) onStopScan else onStartScan,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    if (isScanning) Icons.Default.Stop else Icons.Default.Search,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isScanning) "Detener" else "Buscar")
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(
                onClick = onStartServer,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Wifi, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Servidor")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Estado de conexión
        ConnectionStatusCard(
            connectionStatus = connectionStatus,
            selectedDevice = selectedDevice,
            onDisconnect = onDisconnect
        )

        // NUEVA SECCIÓN: Acciones cuando está conectado
        if (connectionStatus == BluetoothManager.ConnectionStatus.Connected && selectedDevice != null) {
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "Dispositivo Conectado",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        "Conectado a ${selectedDevice.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = onSendFile, // USAR EL PARÁMETRO AQUÍ
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enviar Archivo")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedButton(
                            onClick = onDisconnect,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Desconectar")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lista de dispositivos
        Text(
            "Dispositivos Encontrados",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (isScanning) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Buscando dispositivos...")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        LazyColumn {
            items(discoveredDevices) { device ->
                DeviceCard(
                    device = device,
                    isSelected = selectedDevice?.address == device.address,
                    onSelect = { onDeviceSelected(device) },
                    onConnect = { onConnect(device) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceCard(
    device: BluetoothDeviceInfo,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (device.isPaired) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    device.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (device.isPaired) {
                    Text(
                        "Pareado",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Button(
                onClick = onConnect,
                enabled = isSelected
            ) {
                Text("Conectar")
            }
        }
    }
}
@Composable
fun ConnectionStatusCard(
    connectionStatus: BluetoothManager.ConnectionStatus,
    selectedDevice: BluetoothDeviceInfo?,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (connectionStatus) {
                BluetoothManager.ConnectionStatus.Connected -> MaterialTheme.colorScheme.primaryContainer
                BluetoothManager.ConnectionStatus.Connecting -> MaterialTheme.colorScheme.secondaryContainer
                BluetoothManager.ConnectionStatus.Error -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                when (connectionStatus) {
                    BluetoothManager.ConnectionStatus.Connected -> Icons.Default.BluetoothConnected
                    BluetoothManager.ConnectionStatus.Connecting -> Icons.Default.Bluetooth
                    BluetoothManager.ConnectionStatus.Error -> Icons.Default.ErrorOutline
                    else -> Icons.Default.BluetoothDisabled
                },
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    when (connectionStatus) {
                        BluetoothManager.ConnectionStatus.Connected -> "Conectado"
                        BluetoothManager.ConnectionStatus.Connecting -> "Conectando..."
                        BluetoothManager.ConnectionStatus.Error -> "Error de Conexión"
                        else -> "Desconectado"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )

                selectedDevice?.let {
                    Text(
                        it.name,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (connectionStatus == BluetoothManager.ConnectionStatus.Connected) {
                TextButton(onClick = onDisconnect) {
                    Text("Desconectar")
                }
            }
        }
    }
}
private fun openDownloadsFolder(context: android.content.Context) {
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
        intent.setDataAndType(
            android.net.Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload%2FBluetoothReceived"),
            "resource/folder"
        )
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        // Si falla, abrir Downloads general
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
            intent.setDataAndType(
                android.net.Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload"),
                "resource/folder"
            )
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e2: Exception) {
            android.util.Log.e("BluetoothScreen", "Error abriendo carpeta: ${e2.message}")
        }
    }
}