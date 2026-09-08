package org.simbrain.world.odorworld.behaviors

import org.simbrain.util.UserParameter
import org.simbrain.util.shortestAngleDelta
import org.simbrain.world.odorworld.entities.MovementMode
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import kotlin.math.abs
import kotlin.random.Random

/**
 * Coherent random motion: an internal "desired heading" drifts by a small random
 * amount each tick, and the agent steers toward it while avoiding walls.
 *
 * In grid movement mode this becomes a corridor walk: keep going straight, turn into another open direction
 * with probability [gridTurnChance] when one exists, and turn back only at a dead end.
 */
class Wander : NpcBehavior() {

    @UserParameter(label = "Max Speed", minimumValue = 0.0, order = 10)
    var maxSpeed: Double = 1.5

    @UserParameter(
        label = "Max Turn",
        description = "Maximum heading change per tick (degrees)",
        minimumValue = 0.0,
        order = 20
    )
    var maxTurn: Double = 5.0

    @UserParameter(
        label = "Obstacle Feeler Length",
        description = "Distance ahead to probe for walls and other entities",
        minimumValue = 0.0,
        order = 30
    )
    var feelerLength: Double = 64.0

    @UserParameter(label = "Obstacle Avoidance Weight", minimumValue = 0.0, order = 40)
    var wallWeight: Double = 1.0

    @UserParameter(
        label = "Heading Drift",
        description = "Max random change of the desired heading per tick (degrees)",
        minimumValue = 0.0,
        order = 50
    )
    var driftDegreesPerTick: Double = 5.0

    @UserParameter(
        label = "Num Rays",
        description = "Number of candidate headings sampled each tick. More rays find narrower gaps at higher cost.",
        minimumValue = 4.0,
        order = 60
    )
    var numRays: Int = 24

    @UserParameter(
        label = "Grid Turn Chance",
        description = "In grid movement mode, the chance per cell of turning into another open direction instead of continuing straight",
        minimumValue = 0.0,
        maximumValue = 1.0,
        order = 70
    )
    var gridTurnChance: Double = 0.3

    @Transient
    private var desiredHeading: Double = Double.NaN

    override fun update(entity: OdorWorldEntity) {
        if (entity.movementMode == MovementMode.GRID) {
            updateGrid(entity)
            return
        }
        if (desiredHeading.isNaN()) desiredHeading = entity.heading
        desiredHeading += (Random.nextDouble() - 0.5) * 2.0 * driftDegreesPerTick

        val stuck = entity.wasStuckLastTick
        val n = if (stuck) numRays * 2 else numRays
        val offset = if (stuck) Random.nextDouble() * (360.0 / numRays) else 0.0

        val best = Steering.pickBestHeading(
            entity, feelerLength, numCandidates = n, angularOffset = offset
        ) { heading, _, _, obstacleDist ->
            val delta = shortestAngleDelta(heading, desiredHeading)
            val interest = 1.0 - abs(delta) / 180.0
            val danger = (1.0 - obstacleDist / feelerLength) * wallWeight
            interest - danger
        }

        Steering.applyHeading(entity, best, maxSpeed, maxTurn)
        if (entity.showSteeringDebug) {
            val stuckSuffix = if (stuck) " — escape mode" else ""
            entity.steeringDebug?.behaviorNotes = "Wander$stuckSuffix"
        }
    }

    private fun updateGrid(entity: OdorWorldEntity) {
        val world = entity.world
        val open = world.openGridDirections(entity.cell)
        if (open.isEmpty()) {
            commitGridStep(entity, null, "Wander: boxed in")
            return
        }
        val facing = entity.facingDirection
        val sideways = open.filter { it != facing && it != facing.opposite }
        val step = when {
            facing in open && (sideways.isEmpty() || Random.nextDouble() >= gridTurnChance) -> facing
            sideways.isNotEmpty() -> sideways.random()
            else -> open.random()
        }
        commitGridStep(entity, step, if (step == facing) "Wander: straight" else "Wander: turning $step")
    }

    override fun copy(): Wander = Wander().also {
        it.maxSpeed = maxSpeed
        it.maxTurn = maxTurn
        it.feelerLength = feelerLength
        it.wallWeight = wallWeight
        it.driftDegreesPerTick = driftDegreesPerTick
        it.numRays = numRays
        it.gridTurnChance = gridTurnChance
    }

    override val name = "Wander"
}
