package com.example.gestorarchivos.bluetooth

import android.app.Service
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.IBinder
import com.example.gestorarchivos.model.FileTransfer
import com.example.gestorarchivos.model.TransferProgress
import com.example.gestorarchivos.model.TransferStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.*
import java.security.MessageDigest
import java.util.*
import android.util.Log

class BluetoothTransferService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _activeTransfers = MutableStateFlow<List<FileTransfer>>(emptyList())
    val activeTransfers: StateFlow<List<FileTransfer>> = _activeTransfers.asStateFlow()

    private val _transferProgress = MutableStateFlow<Map<String, TransferProgress>>(emptyMap())
    val transferProgress: StateFlow<Map<String, TransferProgress>> = _transferProgress.asStateFlow()

    companion object {
        private const val BUFFER_SIZE = 8192
        private const val HEADER_SIZE = 1024
    }

    override fun onBind(intent: Intent?): IBinder? = null

    fun sendFile(
        filePath: String,
        socket: BluetoothSocket,
        deviceName: String,
        deviceAddress: String
    ): String {
        val transferId = UUID.randomUUID().toString()
        val file = File(filePath)

        if (!file.exists()) {
            Log.e("BluetoothTransferService", "Archivo no encontrado: $filePath")
            return transferId
        }

        Log.d("BluetoothTransferService", "Iniciando envío de archivo: ${file.name} (${file.length()} bytes)")

        val transfer = FileTransfer(
            id = transferId,
            fileName = file.name,
            fileSize = file.length(),
            deviceName = deviceName,
            deviceAddress = deviceAddress,
            status = TransferStatus.PENDING,
            startTime = Date(),
            isIncoming = false,
            filePath = filePath
        )

        updateTransfer(transfer)

        scope.launch {
            try {
                performFileSend(transfer, file, socket)
            } catch (e: Exception) {
                Log.e("BluetoothTransferService", "Error enviando archivo", e)
                updateTransfer(transfer.copy(
                    status = TransferStatus.FAILED,
                    errorMessage = e.message
                ))
            }
        }

        return transferId
    }

    private suspend fun performFileSend(
        transfer: FileTransfer,
        file: File,
        socket: BluetoothSocket
    ) {
        withContext(Dispatchers.IO) {
            Log.d("BluetoothTransferService", "Comenzando envío real del archivo")

            val outputStream = socket.outputStream
            val fileInputStream = FileInputStream(file)

            try {
                updateTransfer(transfer.copy(status = TransferStatus.CONNECTING))

                // Enviar cabecera con información del archivo
                val header = createFileHeader(file)
                outputStream.write(header)
                outputStream.flush()
                Log.d("BluetoothTransferService", "Cabecera enviada")

                updateTransfer(transfer.copy(status = TransferStatus.TRANSFERRING))

                val buffer = ByteArray(BUFFER_SIZE)
                var totalSent = 0L
                var bytesRead: Int
                val startTime = System.currentTimeMillis()

                while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    outputStream.flush() // AGREGAR: flush después de cada escritura
                    totalSent += bytesRead

                    val currentTime = System.currentTimeMillis()
                    val elapsedTime = (currentTime - startTime) / 1000.0
                    val speed = if (elapsedTime > 0) (totalSent / elapsedTime).toLong() else 0L

                    val progress = (totalSent.toFloat() / file.length())

                    Log.d("BluetoothTransferService", "Progreso: ${(progress * 100).toInt()}% - $totalSent/${file.length()} bytes")

                    updateProgress(TransferProgress(
                        transferId = transfer.id,
                        bytesTransferred = totalSent,
                        totalBytes = file.length(),
                        progress = progress,
                        speedBytesPerSecond = speed
                    ))

                    updateTransfer(transfer.copy(
                        progress = progress,
                        transferredBytes = totalSent
                    ))
                }

                // Enviar checksum para verificación
                val checksum = calculateChecksum(file)
                outputStream.write(checksum)
                outputStream.flush()
                Log.d("BluetoothTransferService", "Archivo enviado completamente")

                updateTransfer(transfer.copy(
                    status = TransferStatus.COMPLETED,
                    progress = 1f,
                    endTime = Date()
                ))

            } finally {
                fileInputStream.close()
            }
        }
    }

    fun receiveFile(
        socket: BluetoothSocket,
        deviceName: String,
        deviceAddress: String,
        saveDirectory: String
    ): String {
        val transferId = UUID.randomUUID().toString()

        Log.d("BluetoothTransferService", "Iniciando recepción de archivo en: $saveDirectory")

        scope.launch {
            try {
                performFileReceive(transferId, socket, deviceName, deviceAddress, saveDirectory)
            } catch (e: Exception) {
                Log.e("BluetoothTransferService", "Error recibiendo archivo", e)
            }
        }

        return transferId
    }

    private suspend fun performFileReceive(
        transferId: String,
        socket: BluetoothSocket,
        deviceName: String,
        deviceAddress: String,
        saveDirectory: String
    ) {
        withContext(Dispatchers.IO) {
            Log.d("BluetoothTransferService", "Comenzando recepción real del archivo")

            // Verificar que el directorio existe y es escribible
            val saveDir = File(saveDirectory)
            if (!saveDir.exists()) {
                saveDir.mkdirs()
                Log.d("BluetoothTransferService", "Directorio creado: ${saveDir.absolutePath}")
            }

            Log.d("BluetoothTransferService", "Directorio existe: ${saveDir.exists()}")
            Log.d("BluetoothTransferService", "Directorio escribible: ${saveDir.canWrite()}")

            val inputStream = socket.inputStream

            try {
                // Leer cabecera
                Log.d("BluetoothTransferService", "Leyendo cabecera...")
                val headerBytes = ByteArray(HEADER_SIZE)
                var totalRead = 0
                while (totalRead < HEADER_SIZE) {
                    val bytesRead = inputStream.read(headerBytes, totalRead, HEADER_SIZE - totalRead)
                    if (bytesRead == -1) {
                        Log.e("BluetoothTransferService", "Error leyendo cabecera - conexión cerrada")
                        break
                    }
                    totalRead += bytesRead
                }

                val fileInfo = parseFileHeader(headerBytes)
                Log.d("BluetoothTransferService", "Archivo a recibir: ${fileInfo.fileName} (${fileInfo.fileSize} bytes)")

                val transfer = FileTransfer(
                    id = transferId,
                    fileName = fileInfo.fileName,
                    fileSize = fileInfo.fileSize,
                    deviceName = deviceName,
                    deviceAddress = deviceAddress,
                    status = TransferStatus.TRANSFERRING,
                    startTime = Date(),
                    isIncoming = true,
                    filePath = "$saveDirectory/${fileInfo.fileName}"
                )

                updateTransfer(transfer)

                val outputFile = File(saveDirectory, fileInfo.fileName)
                Log.d("BluetoothTransferService", "Creando archivo: ${outputFile.absolutePath}")

                // Verificar que podemos crear el archivo
                try {
                    val fileOutputStream = FileOutputStream(outputFile)
                    Log.d("BluetoothTransferService", "FileOutputStream creado exitosamente")

                    val buffer = ByteArray(BUFFER_SIZE)
                    var totalReceived = 0L
                    var bytesRead: Int
                    val startTime = System.currentTimeMillis()

                    Log.d("BluetoothTransferService", "Comenzando recepción de datos...")

                    while (totalReceived < fileInfo.fileSize) {
                        val remainingBytes = (fileInfo.fileSize - totalReceived).toInt()
                        val bytesToRead = minOf(BUFFER_SIZE, remainingBytes)

                        bytesRead = inputStream.read(buffer, 0, bytesToRead)
                        if (bytesRead == -1) {
                            Log.e("BluetoothTransferService", "Error: conexión cerrada prematuramente")
                            break
                        }

                        fileOutputStream.write(buffer, 0, bytesRead)
                        fileOutputStream.flush()
                        totalReceived += bytesRead

                        val currentTime = System.currentTimeMillis()
                        val elapsedTime = (currentTime - startTime) / 1000.0
                        val speed = if (elapsedTime > 0) (totalReceived / elapsedTime).toLong() else 0L

                        val progress = totalReceived.toFloat() / fileInfo.fileSize

                        if (totalReceived % (BUFFER_SIZE * 10) == 0L) { // Log cada 10 buffers
                            Log.d("BluetoothTransferService", "Recibiendo: ${(progress * 100).toInt()}% - $totalReceived/${fileInfo.fileSize} bytes")
                        }

                        updateProgress(TransferProgress(
                            transferId = transferId,
                            bytesTransferred = totalReceived,
                            totalBytes = fileInfo.fileSize,
                            progress = progress,
                            speedBytesPerSecond = speed
                        ))

                        updateTransfer(transfer.copy(
                            progress = progress,
                            transferredBytes = totalReceived
                        ))
                    }

                    fileOutputStream.close()
                    Log.d("BluetoothTransferService", "Archivo guardado completamente: ${outputFile.absolutePath}")
                    Log.d("BluetoothTransferService", "Tamaño del archivo guardado: ${outputFile.length()} bytes")
                    Log.d("BluetoothTransferService", "Archivo existe: ${outputFile.exists()}")

                    // Verificar checksum
                    val receivedChecksum = ByteArray(32)
                    val checksumRead = inputStream.read(receivedChecksum)
                    Log.d("BluetoothTransferService", "Checksum leído: $checksumRead bytes")

                    if (verifyChecksum(outputFile, receivedChecksum)) {
                        Log.d("BluetoothTransferService", "✅ Checksum verificado correctamente")
                        updateTransfer(transfer.copy(
                            status = TransferStatus.COMPLETED,
                            progress = 1f,
                            endTime = Date()
                        ))

                        // AGREGAR: Escaneo del archivo para que aparezca en galería/explorador
                       // scanFile(outputFile)

                    } else {
                        Log.e("BluetoothTransferService", "❌ Error de checksum")
                        outputFile.delete()
                        updateTransfer(transfer.copy(
                            status = TransferStatus.FAILED,
                            errorMessage = "Error en la verificación del archivo"
                        ))
                    }

                } catch (e: Exception) {
                    Log.e("BluetoothTransferService", "Error creando archivo: ${e.message}", e)
                    updateTransfer(transfer.copy(
                        status = TransferStatus.FAILED,
                        errorMessage = "Error guardando archivo: ${e.message}"
                    ))
                }

            } catch (e: Exception) {
                Log.e("BluetoothTransferService", "Error en recepción", e)
            }
        }
    }
//    private fun scanFile(file: File) {
//        try {
//            // Usar MediaScannerConnection en lugar de context
//            val connection = android.media.MediaScannerConnection.scanFile(
//                null, // context no necesario aquí
//                arrayOf(file.absolutePath),
//                null
//            ) { path, uri ->
//                Log.d("BluetoothTransferService", "Archivo escaneado: $path")
//            }
//            Log.d("BluetoothTransferService", "Archivo enviado al media scanner")
//        } catch (e: Exception) {
//            Log.w("BluetoothTransferService", "Error escaneando archivo: ${e.message}")
//        }
//    }

    private fun createFileHeader(file: File): ByteArray {
        val header = ByteArray(HEADER_SIZE)
        val fileName = file.name.toByteArray()
        val fileSize = file.length()

        // Escribir nombre del archivo (primeros 256 bytes)
        System.arraycopy(fileName, 0, header, 0, minOf(fileName.size, 256))

        // Escribir tamaño del archivo (siguientes 8 bytes)
        val sizeBytes = fileSize.toString().toByteArray()
        System.arraycopy(sizeBytes, 0, header, 256, minOf(sizeBytes.size, 8))

        return header
    }

    private fun parseFileHeader(headerBytes: ByteArray): FileInfo {
        val fileNameBytes = headerBytes.sliceArray(0..255)
        val fileName = String(fileNameBytes).trim('\u0000')

        val sizeBytes = headerBytes.sliceArray(256..263)
        val fileSize = String(sizeBytes).trim('\u0000').toLongOrNull() ?: 0L

        return FileInfo(fileName, fileSize)
    }

    private fun calculateChecksum(file: File): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        val inputStream = FileInputStream(file)
        val buffer = ByteArray(BUFFER_SIZE)
        var bytesRead: Int

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            md.update(buffer, 0, bytesRead)
        }

        inputStream.close()
        return md.digest()
    }

    private fun verifyChecksum(file: File, expectedChecksum: ByteArray): Boolean {
        val actualChecksum = calculateChecksum(file)
        return actualChecksum.contentEquals(expectedChecksum)
    }

    private fun updateTransfer(transfer: FileTransfer) {
        val currentTransfers = _activeTransfers.value.toMutableList()
        val index = currentTransfers.indexOfFirst { it.id == transfer.id }

        if (index >= 0) {
            currentTransfers[index] = transfer
        } else {
            currentTransfers.add(transfer)
        }

        _activeTransfers.value = currentTransfers
    }

    private fun updateProgress(progress: TransferProgress) {
        val currentProgress = _transferProgress.value.toMutableMap()
        currentProgress[progress.transferId] = progress
        _transferProgress.value = currentProgress
    }

    fun cancelTransfer(transferId: String) {
        val transfers = _activeTransfers.value.toMutableList()
        val transfer = transfers.find { it.id == transferId }

        transfer?.let {
            updateTransfer(it.copy(status = TransferStatus.CANCELLED))
        }
    }

    data class FileInfo(val fileName: String, val fileSize: Long)

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}