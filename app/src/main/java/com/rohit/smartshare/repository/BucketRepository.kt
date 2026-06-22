package com.rohit.smartshare.repository

import com.rohit.smartshare.data.BucketDao
import com.rohit.smartshare.data.BucketEntity

class BucketRepository(
    private val bucketDao: BucketDao
) {

    suspend fun createBucket(name: String, ownerId: Int) {
        bucketDao.insertBucket(
            BucketEntity(
                name = name,
                ownerId = ownerId
            )
        )
    }

    suspend fun getBuckets(ownerId: Int): List<BucketEntity> {
        return bucketDao.getBucketsByOwner(ownerId)
    }

    suspend fun deleteBucket(bucketId: Int) {
        bucketDao.deleteBucket(bucketId)
    }
}
