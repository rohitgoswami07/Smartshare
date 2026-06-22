package com.rohit.smartshare.repository

import com.rohit.smartshare.data.ShareDao
import com.rohit.smartshare.data.ShareEntity
import kotlin.random.Random

class ShareRepository(
    private val shareDao: ShareDao
) {

    suspend fun createShare(bucketId: Int, expiryHours: Int = 24): String {
        val code = generateCode()
        val expiryTime = System.currentTimeMillis() + (expiryHours * 60 * 60 * 1000L)
        shareDao.insertShare(
            ShareEntity(
                bucketId = bucketId,
                shareCode = code,
                expiryTime = expiryTime
            )
        )
        return code
    }

    suspend fun lookupShare(code: String): ShareEntity? {
        val share = shareDao.getShareByCode(code)
        if (share != null && System.currentTimeMillis() > share.expiryTime) {
            return null
        }
        return share
    }

    suspend fun getSharesByBucket(bucketId: Int): List<ShareEntity> {
        return shareDao.getSharesByBucket(bucketId)
    }

    private fun generateCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..8).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }
}
