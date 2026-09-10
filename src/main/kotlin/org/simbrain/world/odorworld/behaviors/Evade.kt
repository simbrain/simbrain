package org.simbrain.world.odorworld.behaviors

import org.simbrain.util.UserParameter
import org.simbrain.util.magnitude
import org.simbrain.util.point
import org.simbrain.util.wrapAroundDistanceTo
import org.simbrain.world.odorworld.GridDirection
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.MovementMode
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.entities.vectorTo
import kotlin.math.max
import kotlin.random.Random

/**
 * Flee visible entities of [threatType], avoiding both threats (predicted ahead by
 * [leadTicks]) and walls. Stops when no threats are within [visionRange].
 *
 * In grid movement mode each tick steps to whichever neighboring cell is furthest from the nearest threat by
 * path length, or stays put when no neighbor is better, so a dead end is only entered when it really is the
 * furthest place to be.
 */
class Evade : NpcBehavior() {

    @UserParameter(label = "Threat Type", description = "Entity type to flee from", order = 1)
    var threatType: EntityType = EntityType.Swiss

    @UserParameter(label = "Max Speed", minimumValue = 0.0, order = 10)
    var maxSpeed: Double = 2.0

    @UserParameter(label = "Vision Range", minimumValue = 0.0, order = 20)
    var visionRange: Double = 300.0

    @UserParameter(
        label = "Lead Ticks",
        description = "How many ticks ahead to predict each threat's position",
        minimumValue = 0.0,
        order = 30
    )
    var leadTicks: Double = 10.0

    @UserParameter(
        label = "Max Turn",
        description = "Maximum heading change per tick (degrees)",
        minimumValue = 0.0,
        order = 40
    )
    var maxTurn: Double = 10.0

    @UserParameter(
        label = "Obstacle Feeler Length",
        description = "Distance ahead to probe for walls and other entities",
        minimumValue = 0.0,
        order = 50
    )
    var feelerLength: Double = 64.0

    @UserParameter(label = "Obstacle Avoidance Weight", minimumValue = 0.0, order = 60)
    var wallWeight: Double = 1.5

    @UserParameter(
        label = "Threat Weight",
        description = "How strongly threats repel relative to walls",
        minimumValue = 0.0,
        order = 70
    )
    var threatWeight: Double = 2.0

    @UserParameter(
        label = "Num Rays",
        description = "Number of candidate headings sampled each tick. More rays find narrower gaps at higher cost.",
        minimumValue = 4.0,
        order = 80
    )
    var numRays: Int = 24

    override fun update(entity: OdorWorldEntity) {
        val world = entity.world
        val w = world.width
        val h = world.height

        val threats = world.entityList.filter { other ->
            other !== entity && other.entityType == threatType &&
                entity.location.wrapAroundDistanceTo(other.location, w, h) <= visionRange
        }

        if (threats.isEmpty()) {
            Steering.stop(entity, "Evade: no $threatType in range ($visionRange)")
            return
        }

        if (entity.movementMode == MovementMode.GRID) {
            updateGrid(entity, threats)
            return
        }

        val predicted = threats.map { t ->
            val v = t.velocity
            point(t.location.x + v.x * leadTicks, t.location.y + v.y * leadTicks)
        }

        val stuck = entity.wasStuckLastTick
        val n = if (stuck) numRays * 2 else numRays
        val offset = if (stuck) Random.nextDouble() * (360.0 / numRays) else 0.0

        val best = Steering.pickBestHeading(
            entity,
            feelerLength,
            numCandidates = n,
            angularOffset = offset,
            isObstacle = { it.entityType != threatType }
        ) { _, dirX, dirY, obstacleDist ->
            var threat = 0.0
            for (i in threats.indices) {
                val v = entity.vectorTo(predicted[i])
                val dist = v.magnitude
                if (dist < 1e-3) continue
                val cos = (v.x * dirX + v.y * dirY) / dist
                val proximity = max(0.0, 1.0 - dist / visionRange)
                threat += max(0.0, cos) * proximity
            }
            val obstacleDanger = 1.0 - obstacleDist / feelerLength
            -threat * threatWeight - obstacleDanger * wallWeight
        }

        Steering.applyHeading(entity, best, maxSpeed, maxTurn)
        if (entity.showSteeringDebug) {
            val stuckSuffix = if (stuck) " — escape mode" else ""
            entity.steeringDebug?.behaviorNotes = "Evade: ${threats.size} ${threatType} in range$stuckSuffix"
        }
    }

    private fun updateGrid(entity: OdorWorldEntity, threats: List<OdorWorldEntity>) {
        val world = entity.world
        val here = entity.cell
        if (!world.isCellOnGrid(here)) {
            commitGridStep(entity, null, maxSpeed, "Evade: off the grid")
            return
        }
        val threatMaps = threats
            .map { world.cellAt(it.location) }
            .filter { world.isCellOnGrid(it) }
            .map { world.gridDistancesFrom(it) }
        fun threatDistance(cell: GridCell): Int {
            val reachable = threatMaps.map { it.distanceAt(cell) }.filter { it != UNREACHABLE }
            return reachable.minOrNull() ?: Int.MAX_VALUE
        }
        val current = threatDistance(here)
        if (current == Int.MAX_VALUE) {
            commitGridStep(entity, null, maxSpeed, "Evade: no $threatType can reach this cell")
            return
        }
        val facing = entity.facingDirection
        val best = world.openGridDirections(here)
            .mapNotNull { direction ->
                world.gridStepTarget(here.first, here.second, direction)?.let { direction to threatDistance(it) }
            }
            .maxWithOrNull(compareBy<Pair<GridDirection, Int>> { it.second }.thenBy { if (it.first == facing) 1 else 0 })
        if (best == null || best.second <= current) {
            commitGridStep(entity, null, maxSpeed, "Evade: holding, $current steps from $threatType")
            return
        }
        commitGridStep(entity, best.first, maxSpeed, "Evade: ${best.second} steps from $threatType")
    }

    override fun copy(): Evade = Evade().also {
        it.threatType = threatType
        it.maxSpeed = maxSpeed
        it.visionRange = visionRange
        it.leadTicks = leadTicks
        it.maxTurn = maxTurn
        it.feelerLength = feelerLength
        it.wallWeight = wallWeight
        it.threatWeight = threatWeight
        it.numRays = numRays
    }

    override val name = "Evade"
}
