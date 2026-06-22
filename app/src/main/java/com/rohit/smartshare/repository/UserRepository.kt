package com.rohit.smartshare.repository

import com.rohit.smartshare.data.UserDao
import com.rohit.smartshare.data.UserEntity
import com.rohit.smartshare.utils.PasswordUtils

class UserRepository(
    private val userDao: UserDao
) {

    suspend fun registerUser(
        username: String,
        email: String,
        password: String
    ) {
        userDao.insertUser(
            UserEntity(
                username = username,
                email = email,
                password = PasswordUtils.hash(password)
            )
        )
    }

    suspend fun loginUser(
        email: String,
        password: String
    ): UserEntity? {
        return userDao.login(email, PasswordUtils.hash(password))
    }

    suspend fun getUserByEmail(email: String): UserEntity? {
        return userDao.getUserByEmail(email)
    }
}
