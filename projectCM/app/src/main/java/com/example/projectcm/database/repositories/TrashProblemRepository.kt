package com.example.projectcm.database.repositories

import com.example.projectcm.database.dao.TrashProblemDao
import com.example.projectcm.database.entities.TrashProblem

class TrashProblemRepository(private val dao: TrashProblemDao) {
    suspend fun addTrashProblem(problem: TrashProblem) = dao.insertTrashProblem(problem)
    suspend fun updateTrashProblem(problem: TrashProblem) = dao.updateTrashProblem(problem)
    fun getAllTrashProblems() = dao.getAllTrashProblems()
}