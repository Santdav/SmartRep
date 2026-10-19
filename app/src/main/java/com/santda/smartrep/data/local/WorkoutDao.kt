package com.santda.smartrep.data.local



import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// Room does not guarantee the order of @Relation lists: sort by `position` in the repository.
data class WorkoutExerciseWithSets(
    @Embedded val workoutExercise: WorkoutExerciseEntity,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: ExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "workoutExerciseId")
    val sets: List<WorkoutSetEntity>,
)

data class WorkoutWithExercises(
    @Embedded val workout: WorkoutEntity,
    @Relation(
        entity = WorkoutExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "workoutId",
    )
    val exercises: List<WorkoutExerciseWithSets>,
)

enum class FinishResult {
    /** Incomplete sets were removed and endedAt was set. */
    SAVED,

    /** No completed sets remained, so the workout was deleted instead of saved empty. */
    DELETED_EMPTY,
}

@Dao
abstract class WorkoutDao {

    // ---- workout ----

    /** ABORT on conflict: a collision with the (startedAt, name) unique index is a real bug here. */
    @Insert
    abstract suspend fun insertWorkout(workout: WorkoutEntity): Long

    /** For the Hevy import (M4): returns -1 if the workout already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertWorkoutOrIgnore(workout: WorkoutEntity): Long

    @Update
    abstract suspend fun updateWorkout(workout: WorkoutEntity)

    /** Cascades to workout_exercise and workout_set. */
    @Query("DELETE FROM workout WHERE id = :workoutId")
    abstract suspend fun deleteWorkout(workoutId: Long)

    // ---- workout_exercise ----

    @Insert
    abstract suspend fun insertWorkoutExercise(workoutExercise: WorkoutExerciseEntity): Long

    @Update
    abstract suspend fun updateWorkoutExercise(workoutExercise: WorkoutExerciseEntity)

    @Delete
    abstract suspend fun deleteWorkoutExercise(workoutExercise: WorkoutExerciseEntity)

    // ---- workout_set ----

    @Insert
    abstract suspend fun insertSet(set: WorkoutSetEntity): Long

    @Insert
    abstract suspend fun insertSets(sets: List<WorkoutSetEntity>): List<Long>

    @Update
    abstract suspend fun updateSet(set: WorkoutSetEntity)

    @Delete
    abstract suspend fun deleteSet(set: WorkoutSetEntity)

    // ---- Active workout ----

    /** The repository uses this to enforce "at most one active workout". */
    @Query("SELECT COUNT(*) FROM workout WHERE endedAt IS NULL")
    abstract suspend fun countActive(): Int

    @Transaction
    @Query("SELECT * FROM workout WHERE endedAt IS NULL LIMIT 1")
    abstract fun observeActive(): Flow<WorkoutWithExercises?>

    // ---- History ----

    @Transaction
    @Query("SELECT * FROM workout WHERE id = :workoutId")
    abstract fun observeById(workoutId: Long): Flow<WorkoutWithExercises?>

    @Query("SELECT * FROM workout WHERE endedAt IS NOT NULL ORDER BY startedAt DESC")
    abstract fun observeHistory(): Flow<List<WorkoutEntity>>

    // ---- Finishing a workout ----

    @Query(
        """
        SELECT COUNT(*) FROM workout_set
        WHERE completed = 0
          AND workoutExerciseId IN (SELECT id FROM workout_exercise WHERE workoutId = :workoutId)
        """
    )
    abstract suspend fun countIncompleteSets(workoutId: Long): Int

    @Query(
        """
        DELETE FROM workout_set
        WHERE completed = 0
          AND workoutExerciseId IN (SELECT id FROM workout_exercise WHERE workoutId = :workoutId)
        """
    )
    abstract suspend fun deleteIncompleteSets(workoutId: Long)

    @Query(
        """
        DELETE FROM workout_exercise
        WHERE workoutId = :workoutId
          AND id NOT IN (SELECT workoutExerciseId FROM workout_set)
        """
    )
    abstract suspend fun deleteExercisesWithoutSets(workoutId: Long)

    @Query(
        """
        SELECT COUNT(*) FROM workout_set
        WHERE workoutExerciseId IN (SELECT id FROM workout_exercise WHERE workoutId = :workoutId)
        """
    )
    abstract suspend fun countSets(workoutId: Long): Int

    @Query("UPDATE workout SET endedAt = :endedAt WHERE id = :workoutId")
    abstract suspend fun setEndedAt(workoutId: Long, endedAt: Long)

    /**
     * "Finish anyway": deletes incomplete sets, removes exercises left without sets,
     * then sets endedAt, all in one transaction. If nothing completed remains,
     * the workout is deleted instead of saved empty.
     *
     * Showing the warning dialog (countIncompleteSets) is the caller's job.
     */
    @Transaction
    open suspend fun finishWorkout(workoutId: Long, endedAt: Long): FinishResult {
        deleteIncompleteSets(workoutId)
        deleteExercisesWithoutSets(workoutId)
        if (countSets(workoutId) == 0) {
            deleteWorkout(workoutId)
            return FinishResult.DELETED_EMPTY
        }
        setEndedAt(workoutId, endedAt)
        return FinishResult.SAVED
    }
}