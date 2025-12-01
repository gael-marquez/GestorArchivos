package com.example.gestorarchivos.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.example.gestorarchivos.model.BluetoothDeviceInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.lang.reflect.Method
import java.util.*

@SuppressLint("MissingPermission")
class BluetoothManager(private val context: Context) {

    companion object {
        private val MY_UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")
        private const val SERVICE_NAME = "GestorArchivosTransfer"
        private const val TAG = "BluetoothManager"
    }

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDeviceInfo>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null
    private var currentDevice: BluetoothDevice? = null

    // CAMBIAR: usar una variable privada en lugar de propiedad pública
    private var bluetoothTransferService: BluetoothTransferService? = null

    // Callback para manejar transferencias
    var onFileReceived: ((String, String, String) -> Unit)? = null // fileName, deviceName, filePath

    enum class ConnectionStatus {
        Disconnected, Connecting, Connected, Error
    }

    private val deviceDiscoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        val deviceInfo = BluetoothDeviceInfo(
                            name = it.name ?: "Dispositivo Desconocido",
                            address = it.address,
                            isPaired = it.bondState == BluetoothDevice.BOND_BONDED
                        )
                        addDiscoveredDevice(deviceInfo)
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isScanning.value = false
                }
            }
        }
    }

    init {
        registerReceiver()
    }

    private fun registerReceiver() {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(deviceDiscoveryReceiver, filter)
    }

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled ?: false

    fun startDiscovery() {
        if (!isBluetoothEnabled()) return

        _discoveredDevices.value = emptyList()
        _isScanning.value = true
        _errorMessage.value = null

        // Añadir dispositivos ya pareados
        bluetoothAdapter?.bondedDevices?.forEach { device ->
            val deviceInfo = BluetoothDeviceInfo(
                name = device.name ?: "Dispositivo Desconocido",
                address = device.address,
                isPaired = true
            )
            addDiscoveredDevice(deviceInfo)
        }

        bluetoothAdapter?.startDiscovery()
    }

    fun stopDiscovery() {
        bluetoothAdapter?.cancelDiscovery()
        _isScanning.value = false
    }

    private fun addDiscoveredDevice(device: BluetoothDeviceInfo) {
        val currentDevices = _discoveredDevices.value.toMutableList()
        if (currentDevices.none { it.address == device.address }) {
            currentDevices.add(device)
            _discoveredDevices.value = currentDevices
        }
    }

    fun connectToDevice(deviceAddress: String): Job {
        return scope.launch {
            try {
                Log.d(TAG, "Intentando conectar a dispositivo: $deviceAddress")
                _connectionStatus.value = ConnectionStatus.Connecting
                _errorMessage.value = null

                val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
                currentDevice = device

                if (device == null) {
                    throw IOException("Dispositivo no encontrado")
                }

                // Cancelar descubrimiento para mejorar la conexión
                bluetoothAdapter?.cancelDiscovery()

                // Intentar múltiples métodos de conexión
                var connected = false
                val connectionMethods = listOf(
                    { createStandardSocket(device) },
                    { createInsecureSocket(device) },
                    { createReflectionSocket(device, 1) },
                    { createReflectionSocket(device, 2) },
                    { createReflectionSocket(device, 3) }
                )

                for ((index, createSocket) in connectionMethods.withIndex()) {
                    if (connected) break

                    try {
                        Log.d(TAG, "Intentando método de conexión ${index + 1}")
                        clientSocket = createSocket()

                        if (clientSocket == null) {
                            Log.w(TAG, "Método ${index + 1}: Socket nulo")
                            continue
                        }

                        // Intentar conectar con timeout reducido
                        withTimeout(10000) { // 10 segundos por intento
                            clientSocket?.connect()
                        }

                        Log.d(TAG, "Método ${index + 1}: Conexión exitosa")
                        connected = true

                    } catch (e: Exception) {
                        Log.w(TAG, "Método ${index + 1} falló: ${e.message}")
                        clientSocket?.close()
                        clientSocket = null

                        // Si es el último método, lanzar la excepción
                        if (index == connectionMethods.size - 1) {
                            throw e
                        }
                    }
                }

                if (!connected) {
                    throw IOException("No se pudo conectar con ningún método")
                }

                Log.d(TAG, "Conexión exitosa con ${device.name}")
                _connectionStatus.value = ConnectionStatus.Connected

            } catch (e: Exception) {
                Log.e(TAG, "Error al conectar: ${e.message}", e)
                _connectionStatus.value = ConnectionStatus.Error
                _errorMessage.value = when {
                    e.message?.contains("timeout", true) == true -> "Tiempo de conexión agotado"
                    e.message?.contains("read failed") == true -> "El dispositivo no está disponible para recibir conexiones"
                    e.message?.contains("Service discovery failed") == true -> "Servicio no disponible en el dispositivo"
                    e.message?.contains("Connection refused") == true -> "Conexión rechazada por el dispositivo"
                    e.message?.contains("Device not found") == true -> "Dispositivo no encontrado"
                    e.message?.contains("broken pipe") == true -> "Conexión perdida"
                    e.message?.contains("Host is down") == true -> "El dispositivo no responde"
                    else -> "Error de conexión: ${e.message}"
                }
                clientSocket?.close()
                clientSocket = null
            }
        }
    }

    // Método 1: Socket estándar
    private fun createStandardSocket(device: BluetoothDevice): BluetoothSocket? {
        return try {
            Log.d(TAG, "Creando socket estándar...")
            device.createRfcommSocketToServiceRecord(MY_UUID)
        } catch (e: Exception) {
            Log.w(TAG, "Error creando socket estándar: ${e.message}")
            null
        }
    }

    // Método 2: Socket inseguro
    private fun createInsecureSocket(device: BluetoothDevice): BluetoothSocket? {
        return try {
            Log.d(TAG, "Creando socket inseguro...")
            val method: Method = device.javaClass.getMethod("createInsecureRfcommSocketToServiceRecord", UUID::class.java)
            method.invoke(device, MY_UUID) as BluetoothSocket
        } catch (e: Exception) {
            Log.w(TAG, "Error creando socket inseguro: ${e.message}")
            null
        }
    }

    // Método 3: Socket por reflexión con canal específico
    private fun createReflectionSocket(device: BluetoothDevice, channel: Int): BluetoothSocket? {
        return try {
            Log.d(TAG, "Creando socket por reflexión, canal $channel...")
            val method: Method = device.javaClass.getMethod("createRfcommSocket", Int::class.java)
            method.invoke(device, channel) as BluetoothSocket
        } catch (e: Exception) {
            Log.w(TAG, "Error creando socket reflexión canal $channel: ${e.message}")
            null
        }
    }

    fun startServer(): Job {
        return scope.launch {
            try {
                Log.d(TAG, "Iniciando servidor Bluetooth...")
                _errorMessage.value = null

                // Intentar múltiples métodos de servidor
                serverSocket = try {
                    bluetoothAdapter?.listenUsingRfcommWithServiceRecord(SERVICE_NAME, MY_UUID)
                } catch (e: Exception) {
                    Log.w(TAG, "Socket seguro falló, intentando inseguro...")
                    bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord(SERVICE_NAME, MY_UUID)
                }

                Log.d(TAG, "Servidor iniciado, esperando conexiones...")

                while (true) {
                    try {
                        val socket = serverSocket?.accept()
                        socket?.let {
                            Log.d(TAG, "Conexión entrante aceptada")
                            _connectionStatus.value = ConnectionStatus.Connected
                            handleIncomingConnection(it)
                        }
                    } catch (e: IOException) {
                        Log.d(TAG, "Servidor detenido")
                        break
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error en servidor: ${e.message}", e)
                _connectionStatus.value = ConnectionStatus.Error
                _errorMessage.value = "Error al iniciar servidor: ${e.message}"
            }
        }
    }

    private fun handleIncomingConnection(socket: BluetoothSocket) {
        scope.launch {
            clientSocket = socket
            val remoteDevice = socket.remoteDevice

            Log.d(TAG, "Conexión entrante de: ${remoteDevice?.name}")

            // Iniciar recepción de archivos automáticamente
            bluetoothTransferService?.let { service ->
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                val saveDirectory = "${downloadsDir.absolutePath}/BluetoothReceived"

                Log.d(TAG, "Guardando archivos en: $saveDirectory")


                // Crear directorio si no existe
                val dir = java.io.File(saveDirectory)
                if (!dir.exists()) {
                    val created = dir.mkdirs()
                    Log.d(TAG, "Directorio creado: $created - ${dir.absolutePath}")
                }

                service.receiveFile(
                    socket = socket,
                    deviceName = remoteDevice?.name ?: "Dispositivo Desconocido",
                    deviceAddress = remoteDevice?.address ?: "00:00:00:00:00:00",
                    saveDirectory = saveDirectory
                )
            }
        }
    }

    // CAMBIAR: Función para enviar archivo
    fun sendFile(filePath: String, deviceName: String, deviceAddress: String): String? {
        val socket = getCurrentSocket()
        return if (socket != null && bluetoothTransferService != null) {
            Log.d(TAG, "Enviando archivo: $filePath")
            bluetoothTransferService!!.sendFile(filePath, socket, deviceName, deviceAddress)
        } else {
            Log.e(TAG, "No hay conexión activa o servicio no disponible")
            null
        }
    }

    // CAMBIAR: Función para configurar el servicio de transferencia
    fun initializeTransferService(service: BluetoothTransferService) {
        this.bluetoothTransferService = service
    }

    fun disconnect() {
        try {
            Log.d(TAG, "Desconectando...")
            clientSocket?.close()
            serverSocket?.close()
            _connectionStatus.value = ConnectionStatus.Disconnected
            _errorMessage.value = null
            currentDevice = null
        } catch (e: IOException) {
            Log.w(TAG, "Error al desconectar: ${e.message}")
        }
    }

    fun getCurrentSocket(): BluetoothSocket? = clientSocket

    fun clearError() {
        _errorMessage.value = null
    }

    fun cleanup() {
        disconnect()
        try {
            context.unregisterReceiver(deviceDiscoveryReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Error al desregistrar receiver: ${e.message}")
        }
        scope.cancel()
    }
}