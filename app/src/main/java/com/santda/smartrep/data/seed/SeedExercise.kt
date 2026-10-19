package com.santda.smartrep.data.seed

import com.santda.smartrep.data.local.ExerciseEntity
import com.santda.smartrep.data.local.ExerciseMuscleEntity
import com.santda.smartrep.domain.model.Equipment
import com.santda.smartrep.domain.model.ExerciseType
import com.santda.smartrep.domain.model.Muscle
import com.santda.smartrep.domain.model.MuscleRole
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Shape of assets/seed_exercises.json. Enums are decoded by name, so a typo fails loudly. */
@Serializable
data class SeedFile(val version: Int, val exercises: List<SeedExercise>)

@Serializable
data class SeedExercise(
    val seedKey: String,
    val name: String,
    val equipment: Equipment,
    val exerciseType: ExerciseType,
    val primary: List<Muscle>,
    val secondary: List<Muscle> = emptyList(),
) {
    fun toEntity() = ExerciseEntity(seedKey = seedKey, name = name, equipment = equipment,
        exerciseType = exerciseType, isCustom = false, isArchived = false)

    fun toMuscles(): List<ExerciseMuscleEntity> =
        primary.map { ExerciseMuscleEntity(0, it, MuscleRole.PRIMARY) } +
                secondary.map { ExerciseMuscleEntity(0, it, MuscleRole.SECONDARY) }
}

object SeedParser {
    private val json = Json { ignoreUnknownKeys = true }
    fun parse(text: String): List<SeedExercise> = json.decodeFromString<SeedFile>(text).exercises
}