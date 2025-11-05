package com.example.gestorarchivos.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.gestorarchivos.model.FileType
import java.io.File

object FileUtils {

    fun openFileWithExternalApp(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val mimeType = getMimeType(file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, "Abrir con...")
            context.startActivity(chooser)

        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context,
                "No hay aplicaciones disponibles para abrir este archivo",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Error al abrir el archivo: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun shareFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val mimeType = getMimeType(file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Compartir archivo"))

        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Error al compartir el archivo: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun getMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            // Imágenes
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            "webp" -> "image/webp"

            // Documentos
            "txt" -> "text/plain"
            "pdf" -> "application/pdf"
            "doc", "docx" -> "application/msword"
            "xls", "xlsx" -> "application/vnd.ms-excel"
            "ppt", "pptx" -> "application/vnd.ms-powerpoint"

            // Código
            "json" -> "application/json"
            "xml" -> "text/xml"
            "html", "htm" -> "text/html"
            "css" -> "text/css"
            "js" -> "text/javascript"
            "java" -> "text/x-java-source"
            "kt" -> "text/x-kotlin"
            "md" -> "text/markdown"

            // Video
            "mp4" -> "video/mp4"
            "avi" -> "video/x-msvideo"
            "mkv" -> "video/x-matroska"
            "mov" -> "video/quicktime"

            // Audio
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "ogg" -> "audio/ogg"
            "m4a" -> "audio/mp4"

            // Archivos comprimidos
            "zip" -> "application/zip"
            "rar" -> "application/x-rar-compressed"
            "7z" -> "application/x-7z-compressed"
            "tar" -> "application/x-tar"
            "gz" -> "application/gzip"

            // APK
            "apk" -> "application/vnd.android.package-archive"

            else -> "*/*"
        }
    }

    fun canBeOpenedInternally(fileType: FileType): Boolean {
        return when (fileType) {
            FileType.IMAGE, FileType.TEXT, FileType.JSON, FileType.XML -> true
            else -> false
        }
    }

    fun getFileIcon(fileType: FileType): String {
        return when (fileType) {
            FileType.DIRECTORY -> "📁"
            FileType.IMAGE -> "🖼️"
            FileType.TEXT -> "📄"
            FileType.JSON -> "📋"
            FileType.XML -> "📋"
            FileType.VIDEO -> "🎬"
            FileType.AUDIO -> "🎵"
            FileType.PDF -> "📕"
            FileType.ARCHIVE -> "🗜️"
            FileType.APK -> "📦"
            FileType.UNKNOWN -> "📄"
        }
    }
}