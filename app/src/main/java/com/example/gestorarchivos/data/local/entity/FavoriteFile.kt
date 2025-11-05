package com.example.gestorarchivos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_files")
data class FavoriteFile(
    @PrimaryKey
    val path: String,
    val name: String,
    val addedTime: Long,
    val fileType: String
)