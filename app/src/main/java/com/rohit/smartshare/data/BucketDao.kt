package com.rohit.smartshare.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface BucketDao {

    @Insert
    suspend fun insertBucket(bucket: BucketEntity)

    @Query("SELECT * FROM buckets WHERE ownerId = :ownerId ORDER BY createdAt DESC")
    suspend fun getBucketsByOwner(ownerId: Int): List<BucketEntity>

    @Query("DELETE FROM buckets WHERE id = :bucketId")
    suspend fun deleteBucket(bucketId: Int)
}
