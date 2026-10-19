package com.santda.smartrep.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// Table only in v1: no DAO, repository, UI, or calculation touches it.
@Entity(tableName = "body_weight")
data class BodyWeightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordedAt: Long,
    val weightKg: Double,
)