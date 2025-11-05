package com.example.gestorarchivos.data.local.dao

import androidx.room.*
import com.example.gestorarchivos.data.local.entity.FavoriteFile
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteFileDao {
    @Query("SELECT * FROM favorite_files ORDER BY addedTime DESC")
    fun getFavoriteFiles(): Flow<List<FavoriteFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(file: FavoriteFile)

    @Query("DELETE FROM favorite_files WHERE path = :path")
    suspend fun deleteFavorite(path: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_files WHERE path = :path)")
    suspend fun isFavorite(path: String): Boolean

    @Query("DELETE FROM favorite_files")
    suspend fun clearAllFavorites()
}