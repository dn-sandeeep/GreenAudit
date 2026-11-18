package com.sandeep.greenaudit.data.local.seed

import com.sandeep.greenaudit.data.local.dao.ParkDao
import com.sandeep.greenaudit.data.local.entity.ParkEntity
import com.sandeep.greenaudit.data.local.entity.ParkStatus
import com.sandeep.greenaudit.data.local.entity.ParkType
import com.sandeep.greenaudit.data.local.entity.SyncState
import com.sandeep.greenaudit.domain.gis.GeoMath
import com.sandeep.greenaudit.domain.gis.GeoPoint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * First-run demo data: 22 real Jaipur parks with generated (but
 * geodesically-consistent) boundary polygons, so the parks list/map screens
 * have something real to show without requiring a manual "add park" pass.
 * Boundaries are synthesized around each park's real-world center - exact
 * corner coordinates aren't public data, but the center, name and park type
 * are real - and [ParkEntity.areaSqMeters]/[perimeterMeters] are computed
 * from that boundary via the same [GeoMath] used everywhere else, so the
 * numbers shown in the UI are internally consistent.
 */
object SeedDataProvider {

    private const val METERS_PER_DEGREE_LAT = 111_320.0

    private data class Seed(
        val name: String,
        val type: ParkType,
        val status: ParkStatus,
        val center: GeoPoint,
        val radiusMeters: Double,
        val vertexCount: Int,
        val surveyedDaysAgo: Int? = null,
    )

    private val seeds = listOf(
        Seed("Ram Niwas Garden", ParkType.GARDEN, ParkStatus.OPEN, GeoPoint(26.9158, 75.8178), 65.0, 6, surveyedDaysAgo = 28),
        Seed("Jawahar Circle Garden", ParkType.GARDEN, ParkStatus.NEEDS_REPAIR, GeoPoint(26.8417, 75.8056), 55.0, 5, surveyedDaysAgo = 31),
        Seed("Smriti Van", ParkType.FOREST_PARK, ParkStatus.CLOSED, GeoPoint(26.8512, 75.7944), 40.0, 5, surveyedDaysAgo = 47),
        Seed("Central Park Jaipur", ParkType.GARDEN, ParkStatus.OPEN, GeoPoint(26.8949, 75.8064), 90.0, 7),
        Seed("Vidhyadhar Garden", ParkType.GARDEN, ParkStatus.OPEN, GeoPoint(26.9521, 75.8192), 50.0, 5),
        Seed("Talkatora Garden", ParkType.GARDEN, ParkStatus.OPEN, GeoPoint(26.9231, 75.8225), 45.0, 4),
        Seed("Kanak Vrindavan Garden", ParkType.GARDEN, ParkStatus.OPEN, GeoPoint(26.9646, 75.8187), 60.0, 6),
        Seed("Jal Mahal Lakefront", ParkType.LAKE_FRONT, ParkStatus.OPEN, GeoPoint(26.9537, 75.8511), 150.0, 8),
        Seed("Sisodia Rani Garden", ParkType.GARDEN, ParkStatus.NEEDS_REPAIR, GeoPoint(26.8777, 75.8864), 55.0, 6),
        Seed("Nehru Garden", ParkType.PLAYGROUND, ParkStatus.OPEN, GeoPoint(26.9064, 75.8126), 35.0, 4),
        Seed("Panch Batti Park", ParkType.GARDEN, ParkStatus.OPEN, GeoPoint(26.9076, 75.8072), 30.0, 4),
        Seed("Statue Circle Garden", ParkType.GARDEN, ParkStatus.OPEN, GeoPoint(26.9075, 75.8064), 38.0, 5),
        Seed("Subhash Udyan", ParkType.GARDEN, ParkStatus.OPEN, GeoPoint(26.9203, 75.7935), 42.0, 5),
        Seed("Vaishali Nagar Central Park", ParkType.PLAYGROUND, ParkStatus.OPEN, GeoPoint(26.9142, 75.7382), 48.0, 4),
        Seed("Vivekanand Park", ParkType.PLAYGROUND, ParkStatus.NEEDS_REPAIR, GeoPoint(26.9068, 75.7441), 36.0, 4),
        Seed("Amagarh Forest Park", ParkType.FOREST_PARK, ParkStatus.OPEN, GeoPoint(26.9427, 75.8732), 120.0, 7),
        Seed("Masala Chowk Park", ParkType.GARDEN, ParkStatus.CLOSED, GeoPoint(26.9231, 75.8252), 25.0, 4),
        Seed("Galtaji Lakefront", ParkType.LAKE_FRONT, ParkStatus.OPEN, GeoPoint(26.9075, 75.8469), 100.0, 7),
        Seed("Chomu House Garden", ParkType.GARDEN, ParkStatus.OPEN, GeoPoint(26.9187, 75.8143), 33.0, 4),
        Seed("Ramniwas Zoo Garden", ParkType.GARDEN, ParkStatus.NEEDS_REPAIR, GeoPoint(26.9139, 75.8198), 58.0, 6),
        Seed("Panna Meena Park", ParkType.GARDEN, ParkStatus.OPEN, GeoPoint(26.9847, 75.8509), 40.0, 5),
        Seed("Man Sagar Green Belt", ParkType.LAKE_FRONT, ParkStatus.OPEN, GeoPoint(26.9553, 75.8465), 110.0, 7),
    )

    /** Builds the seed [ParkEntity] list. Pure function, safe to unit test. */
    fun buildParks(nowMillis: Long): List<ParkEntity> = seeds.mapIndexed { index, seed ->
        val boundary = polygonAround(seed.center, seed.radiusMeters, seed.vertexCount, seed = index)
        ParkEntity(
            id = "seed-park-%02d".format(index + 1),
            name = seed.name,
            type = seed.type,
            boundary = boundary,
            areaSqMeters = GeoMath.computeAreaSqMeters(boundary),
            perimeterMeters = GeoMath.computePerimeterMeters(boundary),
            status = seed.status,
            lastSurveyDateMillis = seed.surveyedDaysAgo?.let { nowMillis - it * DAY_MILLIS },
            syncState = SyncState.SYNCED,
            updatedAtMillis = nowMillis,
        )
    }

    /** Inserts the seed parks only if the table is currently empty (first launch). */
    suspend fun seedIfEmpty(parkDao: ParkDao, nowMillis: Long = System.currentTimeMillis()) {
        if (parkDao.count() == 0) {
            parkDao.upsertAll(buildParks(nowMillis))
        }
    }

    /**
     * A closed ring of [vertexCount] points around [center], each placed at a
     * slightly jittered radius so the shape isn't a perfect regular polygon.
     * Vertices are emitted in strictly increasing angle order, so the ring
     * never self-intersects.
     */
    private fun polygonAround(
        center: GeoPoint,
        radiusMeters: Double,
        vertexCount: Int,
        seed: Int,
    ): List<GeoPoint> {
        val random = Random(seed)
        val metersPerDegreeLon = METERS_PER_DEGREE_LAT * cos(Math.toRadians(center.latitude))
        return (0 until vertexCount).map { i ->
            val angle = 2 * PI * i / vertexCount
            val jitteredRadius = radiusMeters * (0.75 + random.nextDouble() * 0.5)
            val dLat = (jitteredRadius * cos(angle)) / METERS_PER_DEGREE_LAT
            val dLon = (jitteredRadius * sin(angle)) / metersPerDegreeLon
            GeoPoint(center.latitude + dLat, center.longitude + dLon)
        }
    }

    private const val DAY_MILLIS = 86_400_000L
}
