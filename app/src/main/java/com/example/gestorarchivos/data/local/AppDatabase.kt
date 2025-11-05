package com.example.gestorarchivos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.gestorarchivos.data.local.dao.FavoriteFileDao
import com.example.gestorarchivos.data.local.dao.RecentFileDao
import com.example.gestorarchivos.data.local.entity.FavoriteFile
import com.example.gestorarchivos.data.local.entity.RecentFile

@Database(
    entities = [RecentFile::class, FavoriteFile::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recentFileDao(): RecentFileDao
    abstract fun favoriteFileDao(): FavoriteFileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gestor_archivos_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}