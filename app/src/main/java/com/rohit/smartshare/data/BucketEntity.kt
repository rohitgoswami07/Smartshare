package com.rohit.smartshare.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "buckets")
data class BucketEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,

    val ownerId: Int,

    val createdAt: Long = System.currentTimeMillis()
)
