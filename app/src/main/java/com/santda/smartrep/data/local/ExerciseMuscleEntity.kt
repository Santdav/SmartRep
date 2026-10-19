package com.santda.smartrep.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import com.santda.smartrep.domain.model.Muscle
import com.santda.smartrep.domain.model.MuscleRole

@Entity(
    tableName = "exercise_muscle",
    primaryKeys = ["exerciseId", "muscle"],
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    // exerciseId is already covered by the composite primary key prefix.
)
data class ExerciseMuscleEntity(
    val exerciseId: Long,
    val muscle: Muscle,
    val role: MuscleRole,
)