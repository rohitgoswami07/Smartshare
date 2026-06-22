package com.rohit.smartshare.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val username: String,

    val email: String,

    val password: String
)
