package com.rohit.smartshare.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ShareDao {

    @Insert
    suspend fun insertShare(share: ShareEntity)

    @Query("SELECT * FROM shares WHERE shareCode = :code LIMIT 1")
    suspend fun getShareByCode(code: String): ShareEntity?

    @Query("SELECT * FROM shares WHERE bucketId = :bucketId")
    suspend fun getSharesByBucket(bucketId: Int): List<ShareEntity>
}
