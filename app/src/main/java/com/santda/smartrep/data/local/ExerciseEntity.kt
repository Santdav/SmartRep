package com.santda.smartrep.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.santda.smartrep.domain.model.Equipment
import com.santda.smartrep.domain.model.ExerciseType

@Entity(
    tableName = "exercise",
    indices = [Index(value = ["seedKey"], unique = true)],
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val seedKey: String? = null,
    val name: String,
    val equipment: Equipment,
    val exerciseType: ExerciseType,
    val isCustom: Boolean = false,
    val isArchived: Boolean = false,
)