package com.santda.smartrep.data.seed

import android.content.Context
import com.santda.smartrep.data.local.ExerciseDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Inserts bundled exercises. Safe to call on every app start:
 * only seedKeys missing from the database are inserted; existing rows are never
 * updated, un-archived, or duplicated (even if the user archived or edited them).
 */
@Singleton
class SeedLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exerciseDao: ExerciseDao,
) {
    suspend fun loadIfNeeded(): Int = withContext(Dispatchers.IO) {
        val text = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        seed(SeedParser.parse(text))
    }

    internal suspend fun seed(exercises: List<SeedExercise>): Int {
        val existing = exerciseDao.getAllSeedKeys().toHashSet()
        var inserted = 0
        for (seed in exercises) {
            if (seed.seedKey in existing) continue
            if (exerciseDao.insertWithMuscles(seed.toEntity(), seed.toMuscles()) != -1L) inserted++
        }
        return inserted
    }

    companion object { const val ASSET_NAME = "seed_exercises.json" }
}