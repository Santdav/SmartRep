package com.santda.smartrep.data.seed


import com.santda.smartrep.domain.model.ExerciseType
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure JVM checks on the bundled JSON (Gradle runs unit tests with the module dir as cwd). */
class SeedDataTest {

    private val exercises: List<SeedExercise> by lazy {
        SeedParser.parse(File("src/main/assets/${SeedLoader.ASSET_NAME}").readText())
    }

    @Test
    fun file_parses_and_has_a_useful_size() {
        assertTrue("size=${exercises.size}", exercises.size in 100..400)
    }

    @Test
    fun seedKeys_and_names_are_unique() {
        val keys = exercises.map { it.seedKey }
        assertEquals("duplicate seedKeys", keys.size, keys.toSet().size)
        val names = exercises.map { it.name.lowercase() }
        assertEquals("duplicate names", names.size, names.toSet().size)
    }


    @Test
    fun seedKeys_are_well_formed() {
        val bad = exercises.filterNot { Regex("^[a-z0-9_]+$").matches(it.seedKey) }
        assertTrue("bad seedKeys: ${bad.map { it.seedKey }}", bad.isEmpty())
    }

    @Test
    fun every_exercise_has_a_primary_muscle_and_no_muscle_appears_twice() {
        for (e in exercises) {
            assertTrue("${e.seedKey}: no primary muscle", e.primary.isNotEmpty())
            val all = e.primary + e.secondary
            assertEquals("${e.seedKey}: muscle repeated (composite PK would overwrite it)", all.size, all.toSet().size)
        }
    }

    @Test
    fun every_exercise_type_is_represented() {
        val used = exercises.map { it.exerciseType }.toSet()
        val missing = ExerciseType.entries.filter { it !in used }.map { it.name }
        assertTrue("types without seed exercises: $missing", missing.isEmpty())
    }
}