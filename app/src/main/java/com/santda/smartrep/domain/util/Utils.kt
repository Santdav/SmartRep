package com.santda.smartrep.domain.util
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Storage is always canonical: weight in kilograms, distance in meters.
 * Convert only at the UI edge (display and input) and in the importer.
 * Values are never rounded when converting; use [roundTo] for display only.
 */
enum class WeightUnit(val kgPerUnit: Double) {
    KG(1.0),
    LB(0.45359237); // exact international definition

    /** Converts a value expressed in this unit to kilograms. */
    fun toKg(value: Double): Double = value * kgPerUnit

    /** Converts kilograms to a value expressed in this unit. */
    fun fromKg(kg: Double): Double = kg / kgPerUnit
}

enum class DistanceUnit(val metersPerUnit: Double) {
    METERS(1.0),
    KM(1000.0),
    MI(1609.344); // exact international definition

    /** Converts a value expressed in this unit to meters. */
    fun toMeters(value: Double): Double = value * metersPerUnit

    /** Converts meters to a value expressed in this unit. */
    fun fromMeters(meters: Double): Double = meters / metersPerUnit
}

/**
 * Rounds to [decimals] decimal places (half away from zero for positive values).
 * Intended for display only. Never round before storing.
 */
fun Double.roundTo(decimals: Int): Double {
    require(decimals >= 0) { "decimals must be >= 0" }
    val factor = 10.0.pow(decimals)
    return (this * factor).roundToLong() / factor
}