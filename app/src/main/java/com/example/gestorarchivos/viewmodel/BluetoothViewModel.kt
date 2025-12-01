package com.example.gestorarchivos.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestorarchivos.bluetooth.BluetoothManager
import com.example.gestorarchivos.bluetooth.BluetoothPermissionManager
import com.example.gestorarchivos.bluetooth.BluetoothTransferService
import com.example.gestorarchivos.model.BluetoothDeviceInfo
import com.example.gestorarchivos.model.FileTransfer
import com.example.gestorarchivos.model.TransferProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BluetoothViewModel(application: Application) : AndroidViewModel(application) {

    private val bluetoothManager = BluetoothManager(application)
    private val permissionManager = BluetoothPermissionManager(application)
    private val transferService = BluetoothTransferService()

    // Estados para permisos
    private val _hasPermissions = MutableStateFlow(false)
    val hasPermissions: StateFlow<Boolean> = _hasPermissions.asStateFlow()

    private val _missingPermissions = MutableStateFlow<List<String>>(emptyList())
    val missingPermissions: StateFlow<List<String>> = _missingPermissions.asStateFlow()

    // Estados del Bluetooth
    val discoveredDevices = bluetoothManager.discoveredDevices
    val isScanning = bluetoothManager.isScanning
    val connectionStatus = bluetoothManager.connectionStatus

    // Estados de transferencia
    val activeTransfers = transferService.activeTransfers
    val transferProgress = transferService.transferProgress

    private val _transferHistory = MutableStateFlow<List<FileTransfer>>(emptyList())
    val transferHistory: StateFlow<List<FileTransfer>> = _transferHistory.asStateFlow()

    // Estado de la UI
    private val _selectedDevice = MutableStateFlow<BluetoothDeviceInfo?>(null)
    val selectedDevice: StateFlow<BluetoothDeviceInfo?> = _selectedDevice.asStateFlow()

    private val _showTransferManager = MutableStateFlow(false)
    val showTransferManager: StateFlow<Boolean> = _showTransferManager.asStateFlow()
    val errorMessage = bluetoothManager.errorMessage

    init {
        checkPermissions()
        observeTransfers()
        bluetoothManager.initializeTransferService(transferService)

        // AGREGAR: Callback para archivos recibidos
        bluetoothManager.onFileReceived = { fileName, deviceName, filePath ->
            // Notificar que se recibió un archivo
            android.util.Log.d("BluetoothViewModel", "Archivo recibido: $fileName de $deviceName en $filePath")
        }
    }

    private fun checkPermissions() {
        _hasPermissions.value = permissionManager.hasAllPermissions()
        _missingPermissions.value = permissionManager.getMissingPermissions()
    }

    private fun observeTransfers() {
        viewModelScope.launch {
            transferService.activeTransfers.collect { transfers ->
                val completedTransfers = transfers.filter {
                    it.status.name in listOf("COMPLETED", "FAILED", "CANCELLED")
                }
                _transferHistory.value = completedTransfers
            }
        }
    }

    fun updatePermissions() {
        checkPermissions()
    }

    fun getRequiredPermissions(): Array<String> {
        return permissionManager.getRequiredPermissions()
    }

    fun isBluetoothEnabled(): Boolean {
        return bluetoothManager.isBluetoothEnabled()
    }

    fun startDeviceDiscovery() {
        if (_hasPermissions.value) {
            bluetoothManager.startDiscovery()
        }
    }

    fun stopDeviceDiscovery() {
        bluetoothManager.stopDiscovery()
    }

    fun selectDevice(device: BluetoothDeviceInfo) {
        _selectedDevice.value = device
    }

    fun connectToDevice(device: BluetoothDeviceInfo) {
        viewModelScope.launch {
            bluetoothManager.connectToDevice(device.address)
        }
    }

    fun startServer() {
        viewModelScope.launch {
            bluetoothManager.startServer()
        }
    }

    fun sendFile(filePath: String, deviceAddress: String, deviceName: String): String? {
        return if (_hasPermissions.value && connectionStatus.value == BluetoothManager.ConnectionStatus.Connected) {
            // Aquí necesitarías obtener el socket actual del bluetoothManager
            // Por simplicidad, retornamos un ID simulado
            "transfer_${System.currentTimeMillis()}"
        } else null
    }

    fun cancelTransfer(transferId: String) {
        transferService.cancelTransfer(transferId)
    }

    fun showTransferManager() {
        _showTransferManager.value = true
    }

    fun hideTransferManager() {
        _showTransferManager.value = false
    }

    fun disconnect() {
        bluetoothManager.disconnect()
        _selectedDevice.value = null
    }
    fun startFileTransfer(filePath: String) {
        val currentDevice = _selectedDevice.value

        if (currentDevice != null && _hasPermissions.value && canTransferFiles()) {
            val transferId = bluetoothManager.sendFile(
                filePath = filePath,
                deviceName = currentDevice.name,
                deviceAddress = currentDevice.address
            )

            if (transferId != null) {
                android.util.Log.d("BluetoothViewModel", "Transferencia iniciada con ID: $transferId")
            } else {
                android.util.Log.e("BluetoothViewModel", "Error al iniciar transferencia")
            }
        } else {
            android.util.Log.e("BluetoothViewModel", "No se puede transferir: sin dispositivo, permisos o conexión")
        }
    }

    // Función auxiliar para obtener el socket actual (simplificada)
    private fun getCurrentBluetoothSocket(): android.bluetooth.BluetoothSocket? {
        return bluetoothManager.getCurrentSocket()
    }

    // Función para verificar si puede transferir archivos
    fun canTransferFiles(): Boolean {
        return _hasPermissions.value &&
                connectionStatus.value == BluetoothManager.ConnectionStatus.Connected &&
                _selectedDevice.value != null
    }
    fun clearError() {
        bluetoothManager.clearError()
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothManager.cleanup()
    }
}