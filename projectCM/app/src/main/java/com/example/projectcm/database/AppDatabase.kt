package com.example.projectcm.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.projectcm.database.dao.TrashProblemDao
import com.example.projectcm.database.dao.UserDao
import com.example.projectcm.database.entities.TrashProblem
import com.example.projectcm.database.entities.User

@Database(entities = [User::class, TrashProblem::class], version = 2, exportSchema = false)
@TypeConverters(LocalDateTimeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun trashProblemDao(): TrashProblemDao

    companion object {
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            if (instance == null) {
                instance = Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "app_database"
                ).fallbackToDestructiveMigration()
                    .build()
            }
            return instance!!
        }
    }
}