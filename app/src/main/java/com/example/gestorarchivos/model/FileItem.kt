package com.example.gestorarchivos.model

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FileItem(
    val file: File,
    val name: String = file.name,
    val path: String = file.absolutePath,
    val isDirectory: Boolean = file.isDirectory,
    val size: Long = if (file.isDirectory) 0 else file.length(),
    val lastModified: Long = file.lastModified(),
    val extension: String = file.extension.lowercase(),
    val canRead: Boolean = file.canRead(),
    val canWrite: Boolean = file.canWrite()
) {
    fun getFormattedSize(): String {
        if (isDirectory) return "Carpeta"

        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024

        return when {
            size >= gb -> String.format(Locale.getDefault(), "%.2f GB", size / gb)
            size >= mb -> String.format(Locale.getDefault(), "%.2f MB", size / mb)
            size >= kb -> String.format(Locale.getDefault(), "%.2f KB", size / kb)
            else -> "$size B"
        }
    }

    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(lastModified))
    }

    fun getFileType(): FileType {
        return when {
            isDirectory -> FileType.DIRECTORY
            extension in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp") -> FileType.IMAGE
            extension in listOf("txt", "md", "log") -> FileType.TEXT
            extension == "json" -> FileType.JSON
            extension == "xml" -> FileType.XML
            extension in listOf("mp4", "avi", "mkv", "mov") -> FileType.VIDEO
            extension in listOf("mp3", "wav", "ogg", "m4a") -> FileType.AUDIO
            extension in listOf("pdf") -> FileType.PDF
            extension in listOf("zip", "rar", "7z", "tar", "gz") -> FileType.ARCHIVE
            extension in listOf("apk") -> FileType.APK
            else -> FileType.UNKNOWN
        }
    }
}

enum class FileType {
    DIRECTORY,
    IMAGE,
    TEXT,
    JSON,
    XML,
    VIDEO,
    AUDIO,
    PDF,
    ARCHIVE,
    APK,
    UNKNOWN
}