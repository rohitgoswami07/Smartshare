package com.rohit.smartshare.repository

import com.rohit.smartshare.data.FileDao
import com.rohit.smartshare.data.FileEntity

class FileRepository(
    private val fileDao: FileDao
) {

    suspend fun insertFile(
        fileName: String,
        shareLink: String
    ) {
        fileDao.insertFile(
            FileEntity(
                fileName = fileName,
                shareLink = shareLink,
                uploadedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getAllFiles(): List<FileEntity> {
        return fileDao.getAllFiles()
    }
}