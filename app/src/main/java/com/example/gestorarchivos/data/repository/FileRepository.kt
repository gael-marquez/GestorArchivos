package com.example.gestorarchivos.data.repository

import android.content.Context
import android.os.Environment
import com.example.gestorarchivos.data.local.AppDatabase
import com.example.gestorarchivos.data.local.entity.FavoriteFile
import com.example.gestorarchivos.data.local.entity.RecentFile
import com.example.gestorarchivos.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class FileRepository(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val recentFileDao = database.recentFileDao()
    private val favoriteFileDao = database.favoriteFileDao()

    // Obtener archivos de un directorio
    suspend fun getFilesInDirectory(directory: File): Result<List<FileItem>> = withContext(Dispatchers.IO) {
        try {
            if (!directory.exists() || !directory.isDirectory || !directory.canRead()) {
                return@withContext Result.failure(SecurityException("No se puede acceder al directorio"))
            }

            val files = directory.listFiles()?.mapNotNull { file ->
                try {
                    FileItem(file)
                } catch (e: Exception) {
                    null // Ignorar archivos inaccesibles
                }
            }?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()

            Result.success(files)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Obtener directorio raíz
    fun getRootDirectory(): File {
        return Environment.getExternalStorageDirectory()
    }

    // Obtener directorios principales
    fun getMainDirectories(): List<File> {
        val directories = mutableListOf<File>()

        // Almacenamiento interno
        val internalStorage = Environment.getExternalStorageDirectory()
        if (internalStorage.exists() && internalStorage.canRead()) {
            directories.add(internalStorage)
        }

        // Directorios comunes
        val commonDirs = listOf(
            Environment.DIRECTORY_DOWNLOADS,
            Environment.DIRECTORY_DCIM,
            Environment.DIRECTORY_PICTURES,
            Environment.DIRECTORY_MOVIES,
            Environment.DIRECTORY_MUSIC,
            Environment.DIRECTORY_DOCUMENTS
        )

        commonDirs.forEach { dirType ->
            val dir = Environment.getExternalStoragePublicDirectory(dirType)
            if (dir.exists() && dir.canRead()) {
                directories.add(dir)
            }
        }

        return directories.distinctBy { it.absolutePath }
    }

    // Buscar archivos
    suspend fun searchFiles(query: String, searchIn: File): List<FileItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<FileItem>()

        fun searchRecursively(directory: File, depth: Int = 0) {
            if (depth > 5) return // Limitar profundidad para evitar búsquedas muy largas

            try {
                directory.listFiles()?.forEach { file ->
                    if (file.name.contains(query, ignoreCase = true)) {
                        results.add(FileItem(file))
                    }
                    if (file.isDirectory && file.canRead()) {
                        searchRecursively(file, depth + 1)
                    }
                }
            } catch (e: Exception) {
                // Ignorar errores de acceso
            }
        }

        searchRecursively(searchIn)
        results
    }

    // Operaciones de archivos
    suspend fun copyFile(source: File, destination: File): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (!source.exists()) {
                return@withContext Result.failure(Exception("El archivo origen no existe"))
            }

            if (destination.exists()) {
                return@withContext Result.failure(Exception("El archivo destino ya existe"))
            }

            source.copyTo(destination, overwrite = false)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun moveFile(source: File, destination: File): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (!source.exists()) {
                return@withContext Result.failure(Exception("El archivo origen no existe"))
            }

            if (destination.exists()) {
                return@withContext Result.failure(Exception("El archivo destino ya existe"))
            }

            val success = source.renameTo(destination)
            Result.success(success)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFile(file: File): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) {
                return@withContext Result.failure(Exception("El archivo no existe"))
            }

            val success = if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }

            Result.success(success)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun renameFile(file: File, newName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) {
                return@withContext Result.failure(Exception("El archivo no existe"))
            }

            val newFile = File(file.parent, newName)
            if (newFile.exists()) {
                return@withContext Result.failure(Exception("Ya existe un archivo con ese nombre"))
            }

            val success = file.renameTo(newFile)
            Result.success(success)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createDirectory(parent: File, name: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val newDir = File(parent, name)
            if (newDir.exists()) {
                return@withContext Result.failure(Exception("Ya existe un directorio con ese nombre"))
            }

            val success = newDir.mkdir()
            if (success) {
                Result.success(newDir)
            } else {
                Result.failure(Exception("No se pudo crear el directorio"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Archivos recientes
    suspend fun addRecentFile(fileItem: FileItem) {
        val recentFile = RecentFile(
            path = fileItem.path,
            name = fileItem.name,
            lastAccessTime = System.currentTimeMillis(),
            fileType = fileItem.getFileType().name
        )
        recentFileDao.insertRecentFile(recentFile)
    }

    fun getRecentFiles(): Flow<List<RecentFile>> = recentFileDao.getRecentFiles()

    suspend fun clearRecentFiles() = recentFileDao.clearAllRecent()

    // Favoritos
    suspend fun addFavorite(fileItem: FileItem) {
        val favorite = FavoriteFile(
            path = fileItem.path,
            name = fileItem.name,
            addedTime = System.currentTimeMillis(),
            fileType = fileItem.getFileType().name
        )
        favoriteFileDao.insertFavorite(favorite)
    }

    suspend fun removeFavorite(path: String) = favoriteFileDao.deleteFavorite(path)

    suspend fun isFavorite(path: String): Boolean = favoriteFileDao.isFavorite(path)

    fun getFavorites(): Flow<List<FavoriteFile>> = favoriteFileDao.getFavoriteFiles()

    suspend fun clearFavorites() = favoriteFileDao.clearAllFavorites()
}