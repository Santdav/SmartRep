package com.santda.smartrep.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("routineId"),
        Index("startedAt"),
        Index(value = ["startedAt", "name"], unique = true),
    ],
)
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long? = null,
    val name: String,
    val notes: String = "",
    val startedAt: Long,
    val endedAt: Long? = null, // null => active workout (at most one, enforced in the repository)
)