package org.simbrain.world.odorworld.behaviors

import org.simbrain.util.UserParameter
import org.simbrain.util.magnitude
import org.simbrain.util.point
import org.simbrain.util.wrapAroundDistanceTo
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import kotlin.math.max
import kotlin.random.Random

/**
 * Flee visible entities of [threatType], avoiding both threats (predicted ahead by
 * [leadTicks]) and walls. Stops when no threats are within [visionRange].
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
