package com.example.gestorarchivos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_files")
data class RecentFile(
    @PrimaryKey
    val path: String,
    val name: String,
    val lastAccessTime: Long,
    val fileType: String
)