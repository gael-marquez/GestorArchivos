package com.example.gestorarchivos.ui.screens

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gestorarchivos.viewmodel.BluetoothViewModel

enum class BluetoothNavigationState {
    DEVICE_SELECTION,
    FILE_SELECTION,
    TRANSFER_PROGRESS
}

@Composable
fun BluetoothNavigationScreen(
    viewModel: BluetoothViewModel = viewModel(),
    onNavigateToFileManager: () -> Unit = {}
) {
    var navigationState by remember { mutableStateOf(BluetoothNavigationState.DEVICE_SELECTION) }
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()

    when (navigationState) {
        BluetoothNavigationState.DEVICE_SELECTION -> {
            BluetoothScreen(
                viewModel = viewModel,
                onNavigateToFileManager = onNavigateToFileManager,
                onSendFile = {
                    if (connectionStatus == com.example.gestorarchivos.bluetooth.BluetoothManager.ConnectionStatus.Connected) {
                        navigationState = BluetoothNavigationState.FILE_SELECTION
                    }
                }
            )
        }

        BluetoothNavigationState.FILE_SELECTION -> {
            selectedDevice?.let { device ->
                FileSelectionScreen(
                    connectedDevice = device,
                    onFileSelected = { filePath ->
                        viewModel.startFileTransfer(filePath)
                        navigationState = BluetoothNavigationState.DEVICE_SELECTION
                    },
                    onBack = {
                        navigationState = BluetoothNavigationState.DEVICE_SELECTION
                    }
                )
            }
        }

        BluetoothNavigationState.TRANSFER_PROGRESS -> {
            // Aquí podrías agregar una pantalla de progreso de transferencia
            // Por ahora volvemos a la selección de dispositivos
            navigationState = BluetoothNavigationState.DEVICE_SELECTION
        }
    }
}