package com.santda.smartrep.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: WorkoutDao

    @Before
    fun setUp() {
        db = buildInMemoryDb()
        dao = db.workoutDao()
    }

    @After
    fun tearDown() = db.close()

    // ---- Schema integrity ----

    @Test
    fun foreignKeys_areEnforced() = runTest {
        assertFailsWith<SQLiteConstraintException> {
            dao.insertWorkoutExercise(
                WorkoutExerciseEntity(workoutId = 999, exerciseId = 999, position = 0),
            )
        }
    }

    @Test
    fun deletingWorkout_cascadesToExercisesAndSets() = runTest {
        val id = db.createWorkout(exercises = listOf(listOf(true, true), listOf(false)))
        assertEquals(2, db.count("workout_exercise"))
        assertEquals(3, db.count("workout_set"))

        dao.deleteWorkout(id)

        assertEquals(0, db.count("workout"))
        assertEquals(0, db.count("workout_exercise"))
        assertEquals(0, db.count("workout_set"))
        // The exercise library is untouched.
        assertEquals(2, db.count("exercise"))
    }

    @Test
    fun deletingExerciseWithHistory_isRestricted() = runTest {
        db.createWorkout(exercises = listOf(listOf(true)))

        assertFailsWith<SQLiteConstraintException> {
            db.openHelper.writableDatabase.execSQL("DELETE FROM exercise")
        }
        assertEquals(1, db.count("exercise"))
    }

    @Test
    fun duplicateStartedAtAndName_isRejected() = runTest {
        dao.insertWorkout(workout("Push", startedAt = 5_000L))

        assertFailsWith<SQLiteConstraintException> {
            dao.insertWorkout(workout("Push", startedAt = 5_000L))
        }
        assertEquals(1, db.count("workout"))
    }

    @Test
    fun sameNameDifferentStart_isAllowed() = runTest {
        dao.insertWorkout(workout("Push", startedAt = 5_000L))
        dao.insertWorkout(workout("Push", startedAt = 6_000L))

        assertEquals(2, db.count("workout"))
    }

    @Test
    fun insertWorkoutOrIgnore_returnsMinusOneOnDuplicate() = runTest {
        val first = dao.insertWorkoutOrIgnore(workout("Push", startedAt = 5_000L))
        val second = dao.insertWorkoutOrIgnore(workout("Push", startedAt = 5_000L))

        assertNotEquals(-1L, first)
        assertEquals(-1L, second)
        assertEquals(1, db.count("workout"))
    }

    // ---- Active workout ----

    @Test
    fun activeWorkout_isTheOneWithNoEndedAt() = runTest {
        assertEquals(0, dao.countActive())
        assertNull(dao.observeActive().first())

        val id = db.createWorkout(name = "Active", startedAt = 2_000L, exercises = listOf(listOf(false)))
        db.createWorkout(name = "Done", startedAt = 1_000L, endedAt = 1_500L)

        assertEquals(1, dao.countActive())
        val active = dao.observeActive().first()
        assertNotNull(active)
        assertEquals(id, active!!.workout.id)
        assertEquals(1, active.exercises.size)
        assertEquals(1, active.exercises[0].sets.size)
        assertNotNull(active.exercises[0].exercise)
    }

    // ---- History ----

    @Test
    fun history_listsOnlyFinishedWorkouts_newestFirst() = runTest {
        db.createWorkout(name = "Old", startedAt = 1_000L, endedAt = 1_100L)
        db.createWorkout(name = "New", startedAt = 3_000L, endedAt = 3_100L)
        db.createWorkout(name = "Active", startedAt = 4_000L)

        val names = dao.observeHistory().first().map { it.name }

        assertEquals(listOf("New", "Old"), names)
    }

    // ---- Finishing a workout ----

    @Test
    fun countIncompleteSets_countsOnlyThatWorkout_andChangesNothing() = runTest {
        val id = db.createWorkout(
            name = "Mine",
            exercises = listOf(listOf(true, false), listOf(false)),
        )
        db.createWorkout(name = "Other", startedAt = 9_000L, exercises = listOf(listOf(false, false)))

        assertEquals(2, dao.countIncompleteSets(id))
        // "Go back" path: counting must not modify anything.
        assertEquals(5, db.count("workout_set"))
    }

    @Test
    fun finishWorkout_removesIncompleteSetsAndEmptyExercises() = runTest {
        val id = db.createWorkout(
            exercises = listOf(
                listOf(true, false), // keeps 1 set
                listOf(false, false), // loses everything -> exercise removed
                listOf(true), // untouched
            ),
        )

        val result = dao.finishWorkout(id, endedAt = 7_777L)

        assertEquals(FinishResult.SAVED, result)
        assertEquals(2, db.count("workout_exercise"))
        assertEquals(2, db.count("workout_set"))
        assertEquals(0, dao.countIncompleteSets(id))
        val saved = dao.observeById(id).first()!!
        assertEquals(7_777L, saved.workout.endedAt)
        assertEquals(0, dao.countActive())
    }

    @Test
    fun finishWorkout_withAllSetsCompleted_changesOnlyEndedAt() = runTest {
        val id = db.createWorkout(exercises = listOf(listOf(true, true), listOf(true)))

        val result = dao.finishWorkout(id, endedAt = 8_000L)

        assertEquals(FinishResult.SAVED, result)
        assertEquals(2, db.count("workout_exercise"))
        assertEquals(3, db.count("workout_set"))
    }

    @Test
    fun finishWorkout_withNoCompletedSets_deletesTheWorkout() = runTest {
        val id = db.createWorkout(exercises = listOf(listOf(false, false), listOf(false)))

        val result = dao.finishWorkout(id, endedAt = 8_000L)

        assertEquals(FinishResult.DELETED_EMPTY, result)
        assertEquals(0, db.count("workout"))
        assertEquals(0, db.count("workout_exercise"))
        assertEquals(0, db.count("workout_set"))
    }

    @Test
    fun finishWorkout_withNoExercises_deletesTheWorkout() = runTest {
        val id = db.createWorkout()

        assertEquals(FinishResult.DELETED_EMPTY, dao.finishWorkout(id, endedAt = 8_000L))
        assertEquals(0, db.count("workout"))
    }

    @Test
    fun finishWorkout_doesNotTouchOtherWorkouts() = runTest {
        val target = db.createWorkout(name = "Target", exercises = listOf(listOf(true, false)))
        val other = db.createWorkout(name = "Other", startedAt = 9_000L, exercises = listOf(listOf(false)))

        dao.finishWorkout(target, endedAt = 8_000L)

        assertEquals(1, dao.countIncompleteSets(other))
        assertNull(dao.observeById(other).first()!!.workout.endedAt)
    }
}