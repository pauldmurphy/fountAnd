package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scripts")
data class ScriptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val linkedUri: String? = null,
    val linkedFileName: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
