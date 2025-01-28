package com.example.projectcm.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "TrashProblems")
data class TrashProblem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val imagePath: String, 
    val status: String, 
    val adminName: String? = null, 
    val reportedAt: LocalDateTime = LocalDateTime.now(),
    val resolvedAt: LocalDateTime? = null, 
    val latitude: Double, 
    val longitude: Double 
)