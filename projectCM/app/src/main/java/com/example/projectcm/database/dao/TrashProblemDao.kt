package com.example.projectcm.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.projectcm.database.entities.TrashProblem
import kotlinx.coroutines.flow.Flow

@Dao
interface TrashProblemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrashProblem(problem: TrashProblem)

    @Update
    suspend fun updateTrashProblem(problem: TrashProblem)

    @Query("SELECT * FROM TrashProblems")
    fun getAllTrashProblems(): Flow<List<TrashProblem>>
}