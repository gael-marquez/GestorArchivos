package com.example.gestorarchivos.model

import java.util.Date

data class FileTransfer(
    val id: String,
    val fileName: String,
    val fileSize: Long,
    val deviceName: String,
    val deviceAddress: String,
    val status: TransferStatus,
    val progress: Float = 0f,
    val transferredBytes: Long = 0L,
    val startTime: Date,
    val endTime: Date? = null,
    val isIncoming: Boolean,
    val filePath: String? = null,
    val errorMessage: String? = null
)

enum class TransferStatus {
    PENDING,
    CONNECTING,
    TRANSFERRING,
    COMPLETED,
    FAILED,
    CANCELLED,
    QUEUED
}

data class TransferProgress(
    val transferId: String,
    val bytesTransferred: Long,
    val totalBytes: Long,
    val progress: Float,
    val speedBytesPerSecond: Long
)