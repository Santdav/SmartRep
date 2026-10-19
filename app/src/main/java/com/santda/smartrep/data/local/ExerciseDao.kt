package com.santda.smartrep.data.local


import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class ExerciseWithMuscles(
    @Embedded val exercise: ExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "exerciseId")
    val muscles: List<ExerciseMuscleEntity>,
)

@Dao
abstract class ExerciseDao {

    // ---- Writes ----

    /** Returns the new row id, or -1 if ignored (e.g. duplicate seedKey). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(exercise: ExerciseEntity): Long

    @Update
    abstract suspend fun update(exercise: ExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertMuscles(muscles: List<ExerciseMuscleEntity>)

    @Query("DELETE FROM exercise_muscle WHERE exerciseId = :exerciseId")
    abstract suspend fun deleteMuscles(exerciseId: Long)

    /** Soft delete / restore. Exercises are never hard-deleted. */
    @Query("UPDATE exercise SET isArchived = :archived WHERE id = :exerciseId")
    abstract suspend fun setArchived(exerciseId: Long, archived: Boolean)

    /** Used by the seed loader and by custom exercise creation. Returns -1 if ignored. */
    @Transaction
    open suspend fun insertWithMuscles(
        exercise: ExerciseEntity,
        muscles: List<ExerciseMuscleEntity>,
    ): Long {
        val id = insert(exercise)
        if (id == -1L) return -1L
        insertMuscles(muscles.map { it.copy(exerciseId = id) })
        return id
    }

    /** Replaces the exercise row and its full muscle set atomically. */
    @Transaction
    open suspend fun updateWithMuscles(
        exercise: ExerciseEntity,
        muscles: List<ExerciseMuscleEntity>,
    ) {
        update(exercise)
        deleteMuscles(exercise.id)
        insertMuscles(muscles.map { it.copy(exerciseId = exercise.id) })
    }

    // ---- Reads ----

    /** Non-archived exercises, filtered by name (empty query returns all), sorted A-Z. */
    @Transaction
    @Query(
        """
        SELECT * FROM exercise
        WHERE isArchived = 0 AND name LIKE '%' || :query || '%'
        ORDER BY name COLLATE NOCASE
        """
    )
    abstract fun observeActive(query: String = ""): Flow<List<ExerciseWithMuscles>>

    @Transaction
    @Query("SELECT * FROM exercise WHERE id = :exerciseId")
    abstract fun observeById(exerciseId: Long): Flow<ExerciseWithMuscles?>

    @Query("SELECT * FROM exercise WHERE id = :exerciseId")
    abstract suspend fun getById(exerciseId: Long): ExerciseEntity?

    /** Seed loader: add only seedKeys that are not present yet. */
    @Query("SELECT seedKey FROM exercise WHERE seedKey IS NOT NULL")
    abstract suspend fun getAllSeedKeys(): List<String>

    /** Hevy import (M4): case-insensitive match, archived exercises included. */
    @Query("SELECT * FROM exercise WHERE name = :name COLLATE NOCASE LIMIT 1")
    abstract suspend fun findByName(name: String): ExerciseEntity?
}