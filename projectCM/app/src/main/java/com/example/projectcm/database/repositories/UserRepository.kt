package com.example.projectcm.database.repositories

import com.example.projectcm.database.dao.UserDao
import com.example.projectcm.database.entities.User

class UserRepository(private val userDao: UserDao) {

    suspend fun getUser(username: String, password: String): User? {
        return userDao.getUser(username, password)
    }

    suspend fun registerUser(user: User) {
        userDao.insertUser(user)
    }

    suspend fun isUsernameTaken(username: String): Boolean {
        return userDao.getUserByUsername(username) != null
    }
}