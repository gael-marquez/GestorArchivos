package com.example.gestorarchivos.data.local.dao

import androidx.room.*
import com.example.gestorarchivos.data.local.entity.RecentFile
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentFileDao {
    @Query("SELECT * FROM recent_files ORDER BY lastAccessTime DESC LIMIT 20")
    fun getRecentFiles(): Flow<List<RecentFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentFile(file: RecentFile)

    @Query("DELETE FROM recent_files WHERE path = :path")
    suspend fun deleteRecentFile(path: String)

    @Query("DELETE FROM recent_files")
    suspend fun clearAllRecent()
}