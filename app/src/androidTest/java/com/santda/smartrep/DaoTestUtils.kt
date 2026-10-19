package com.santda.smartrep.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.santda.smartrep.domain.model.Equipment
import com.santda.smartrep.domain.model.ExerciseType
import org.junit.Assert.fail

fun buildInMemoryDb(): AppDatabase =
    Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext<Context>(),
        AppDatabase::class.java,
    ).build()

fun exercise(name: String = "Bench Press", seedKey: String? = null) = ExerciseEntity(
    seedKey = seedKey,
    name = name,
    equipment = Equipment.BARBELL,
    exerciseType = ExerciseType.WEIGHT_REPS,
)

fun workout(name: String = "Push", startedAt: Long = 1_000L, endedAt: Long? = null) =
    WorkoutEntity(name = name, startedAt = startedAt, endedAt = endedAt)

/**
 * Creates a workout with one distinct exercise per entry in [exercises].
 * Each entry lists the `completed` flag of every set of that exercise.
 * Returns the workout id.
 */
suspend fun AppDatabase.createWorkout(
    name: String = "Push",
    startedAt: Long = 1_000L,
    endedAt: Long? = null,
    exercises: List<List<Boolean>> = emptyList(),
): Long {
    val workoutId = workoutDao().insertWorkout(workout(name, startedAt, endedAt))
    exercises.forEachIndexed { i, sets ->
        val exerciseId = exerciseDao().insert(exercise(name = "$name exercise $i"))
        val workoutExerciseId = workoutDao().insertWorkoutExercise(
            WorkoutExerciseEntity(workoutId = workoutId, exerciseId = exerciseId, position = i),
        )
        sets.forEachIndexed { j, done ->
            workoutDao().insertSet(
                WorkoutSetEntity(workoutExerciseId = workoutExerciseId, position = j, completed = done),
            )
        }
    }
    return workoutId
}

/** Row count of a table, bypassing the DAOs (used to verify cascades and deletes). */
fun AppDatabase.count(table: String): Int =
    openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use {
        it.moveToFirst()
        it.getInt(0)
    }

/** Fails the test unless [block] throws a [T]. Works with suspend calls. */
suspend inline fun <reified T : Throwable> assertFailsWith(block: suspend () -> Unit) {
    try {
        block()
    } catch (e: Throwable) {
        if (e is T) return
        throw e
    }
    fail("Expected ${T::class.java.simpleName} but nothing was thrown")
}