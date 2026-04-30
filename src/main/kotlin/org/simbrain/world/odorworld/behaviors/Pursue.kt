package org.simbrain.world.odorworld.behaviors

import org.simbrain.util.UserParameter
import org.simbrain.util.magnitude
import org.simbrain.util.point
import org.simbrain.util.wrapAroundDistanceTo
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.entities.vectorTo
import kotlin.math.max
import kotlin.random.Random

/**
 * Chase the nearest visible entities of [targetType], aiming at where they will be
 * (current position + velocity * [leadTicks]). Multi-target attraction is summed and
 * weighted by proximity, so the geometry decides which one wins. Wall-aware via a
 * forward feeler ray.
 */
class Pursue : NpcBehavior() {

    @UserParameter(label = "Target Type", description = "Entity type to chase", order = 1)
    var targetType: EntityType = EntityType.Swiss

    @UserParameter(label = "Max Speed", minimumValue = 0.0, order = 10)
    var maxSpeed: Double = 2.0

    @UserParameter(label = "Vision Range", minimumValue = 0.0, order = 20)
    var visionRange: Double = 300.0

    @UserParameter(
        label = "Lead Ticks",
        description = "How many ticks ahead to predict the target's position when aiming",
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

    @UserParameter(
        label = "Obstacle Avoidance Weight",
        description = "How strongly walls and non-target entities repel relative to target attraction",
        minimumValue = 0.0,
        order = 60
    )
    var wallWeight: Double = 1.5

    @UserParameter(
        label = "Num Rays",
        description = "Number of candidate headings sampled each tick. More rays find narrower gaps at higher cost.",
        minimumValue = 4.0,
        order = 70
    )
    var numRays: Int = 24

    override fun update(entity: OdorWorldEntity) {
        val world = entity.world
        val w = world.width
        val h = world.height

        val targets = world.entityList.filter { other ->
            other !== entity && other.entityType == targetType &&
                entity.location.wrapAroundDistanceTo(other.location, w, h) <= visionRange
        }

        if (targets.isEmpty()) {
            Steering.stop(entity, "Pursue: no $targetType in range ($visionRange)")
            return
        }

        val predicted = targets.map { t ->
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
            isObstacle = { it.entityType != targetType }
        ) { _, dirX, dirY, obstacleDist ->
            var interest = 0.0
            for (i in targets.indices) {
                val v = entity.vectorTo(predicted[i])
                val dist = v.magnitude
                if (dist < 1e-3) continue
                val cos = (v.x * dirX + v.y * dirY) / dist
                val proximity = max(0.0, 1.0 - dist / visionRange)
                interest += max(0.0, cos) * proximity
            }
            val danger = (1.0 - obstacleDist / feelerLength) * wallWeight
            interest - danger
        }

        Steering.applyHeading(entity, best, maxSpeed, maxTurn)
        if (entity.showSteeringDebug) {
            val stuckSuffix = if (stuck) " — escape mode" else ""
            entity.steeringDebug?.behaviorNotes = "Pursue: ${targets.size} ${targetType} in range$stuckSuffix"
        }
    }

    override fun copy(): Pursue = Pursue().also {
        it.targetType = targetType
        it.maxSpeed = maxSpeed
        it.visionRange = visionRange
        it.leadTicks = leadTicks
        it.maxTurn = maxTurn
        it.feelerLength = feelerLength
        it.wallWeight = wallWeight
        it.numRays = numRays
    }

    override val name = "Pursue"
}
