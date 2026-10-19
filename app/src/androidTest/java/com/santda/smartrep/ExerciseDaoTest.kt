package com.santda.smartrep.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.santda.smartrep.domain.model.Muscle
import com.santda.smartrep.domain.model.MuscleRole
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
class ExerciseDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ExerciseDao

    @Before
    fun setUp() {
        db = buildInMemoryDb()
        dao = db.exerciseDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun duplicateSeedKey_isIgnored() = runTest {
        val first = dao.insert(exercise("Bench Press", seedKey = "bench_press"))
        val second = dao.insert(exercise("Bench Press (copy)", seedKey = "bench_press"))

        assertNotEquals(-1L, first)
        assertEquals(-1L, second)
        assertEquals(1, db.count("exercise"))
    }

    @Test
    fun nullSeedKeys_doNotCollide() = runTest {
        dao.insert(exercise("Custom A"))
        dao.insert(exercise("Custom B"))

        assertEquals(2, db.count("exercise"))
    }

    @Test
    fun insertWithMuscles_storesExerciseAndMuscles() = runTest {
        val id = dao.insertWithMuscles(
            exercise("Bench Press"),
            listOf(
                ExerciseMuscleEntity(0, Muscle.CHEST, MuscleRole.PRIMARY),
                ExerciseMuscleEntity(0, Muscle.TRICEPS, MuscleRole.SECONDARY),
            ),
        )

        val result = dao.observeById(id).first()
        assertNotNull(result)
        assertEquals(2, result!!.muscles.size)
        assertEquals(setOf(id), result.muscles.map { it.exerciseId }.toSet())
    }

    @Test
    fun insertWithMuscles_duplicateSeedKey_insertsNoMuscles() = runTest {
        dao.insertWithMuscles(
            exercise("Bench Press", seedKey = "bench_press"),
            listOf(ExerciseMuscleEntity(0, Muscle.CHEST, MuscleRole.PRIMARY)),
        )
        val result = dao.insertWithMuscles(
            exercise("Bench Press", seedKey = "bench_press"),
            listOf(ExerciseMuscleEntity(0, Muscle.TRICEPS, MuscleRole.PRIMARY)),
        )

        assertEquals(-1L, result)
        assertEquals(1, db.count("exercise_muscle"))
    }

    @Test
    fun updateWithMuscles_replacesMuscleSet() = runTest {
        val id = dao.insertWithMuscles(
            exercise("Bench Press"),
            listOf(ExerciseMuscleEntity(0, Muscle.CHEST, MuscleRole.PRIMARY)),
        )
        val current = dao.getById(id)!!

        dao.updateWithMuscles(
            current.copy(name = "Flat Bench Press"),
            listOf(ExerciseMuscleEntity(id, Muscle.TRICEPS, MuscleRole.PRIMARY)),
        )

        val result = dao.observeById(id).first()!!
        assertEquals("Flat Bench Press", result.exercise.name)
        assertEquals(listOf(Muscle.TRICEPS), result.muscles.map { it.muscle })
    }

    @Test
    fun deletingExercise_cascadesToMuscles() = runTest {
        val id = dao.insertWithMuscles(
            exercise("Bench Press"),
            listOf(ExerciseMuscleEntity(0, Muscle.CHEST, MuscleRole.PRIMARY)),
        )

        db.openHelper.writableDatabase.execSQL("DELETE FROM exercise WHERE id = $id")

        assertEquals(0, db.count("exercise_muscle"))
    }

    @Test
    fun archivedExercises_areHiddenFromObserveActive_butStillReadable() = runTest {
        val id = dao.insert(exercise("Old Exercise"))
        dao.insert(exercise("Kept Exercise"))

        dao.setArchived(id, true)

        assertEquals(listOf("Kept Exercise"), dao.observeActive().first().map { it.exercise.name })
        assertNotNull(dao.getById(id))

        dao.setArchived(id, false)
        assertEquals(2, dao.observeActive().first().size)
    }

    @Test
    fun observeActive_filtersByNameCaseInsensitive_andSortsAlphabetically() = runTest {
        dao.insert(exercise("Squat"))
        dao.insert(exercise("bench press"))
        dao.insert(exercise("Incline Bench Press"))

        val all = dao.observeActive().first().map { it.exercise.name }
        assertEquals(listOf("bench press", "Incline Bench Press", "Squat"), all)

        val filtered = dao.observeActive("BENCH").first().map { it.exercise.name }
        assertEquals(listOf("bench press", "Incline Bench Press"), filtered)
    }

    @Test
    fun getAllSeedKeys_skipsCustomExercises() = runTest {
        dao.insert(exercise("Bench Press", seedKey = "bench_press"))
        dao.insert(exercise("Squat", seedKey = "squat"))
        dao.insert(exercise("My Custom Lift"))

        assertEquals(setOf("bench_press", "squat"), dao.getAllSeedKeys().toSet())
    }

    @Test
    fun findByName_isCaseInsensitive_andIncludesArchived() = runTest {
        val id = dao.insert(exercise("Bench Press"))
        dao.setArchived(id, true)

        assertEquals(id, dao.findByName("BENCH press")?.id)
        assertNull(dao.findByName("Deadlift"))
    }
}