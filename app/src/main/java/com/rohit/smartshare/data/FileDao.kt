package com.rohit.smartshare.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface FileDao {

    @Insert
    suspend fun insertFile(
        file: FileEntity
    )

    @Query(
        "SELECT * FROM files ORDER BY uploadedAt DESC"
    )
    suspend fun getAllFiles():
            List<FileEntity>
}
