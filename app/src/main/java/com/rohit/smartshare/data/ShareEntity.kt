package com.rohit.smartshare.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shares")
data class ShareEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val bucketId: Int,

    val shareCode: String,

    val expiryTime: Long
)
