package com.rohit.smartshare.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "files")
data class FileEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val fileName: String,

    val shareLink: String,

    val uploadedAt: Long
)