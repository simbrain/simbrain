/**
 * Locomotion policy for the thermotaxis worm, run as an [NpcBehavior] at the start of each world tick.
 * It owns translation (constant crawl or a scripted empirical turn) and the plate-edge reflection, while
 * continuous steering arrives separately through [org.simbrain.world.odorworld.effectors.Turning]
 * effectors coupled from the motor neurons, which adjust the heading this behavior then crawls along.
 * The entity's own speed stays zero so [OdorWorldEntity.applyMovement] never double-moves it.
 */
package org.simbrain.custom_sims.simulations.neuroscience

import org.simbrain.util.point
import org.simbrain.world.odorworld.behaviors.NpcBehavior
import org.simbrain.world.odorworld.behaviors.npcBehaviorTypes
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.ThermalGradient
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

class ThermotaxisWormBehavior : NpcBehavior() {

    companion object {
        init {
            // Registers the type as soon as the class loads (sim start or workspace deserialization),
            // so the entity dialog's behavior dropdown can display and re-edit it instead of showing
            // "None" and silently replacing it on commit.
            if (ThermotaxisWormBehavior::class.java !in npcBehaviorTypes) {
                npcBehaviorTypes.add(ThermotaxisWormBehavior::class.java)
            }
        }
    }

    var gradient: ThermalGradient = ThermalGradient()

    var useEmpiricalTurns = true

    private val dt = 0.1

    private var turnTime = 0.0

    private var remainingTurnSteps = 0

    private var turnStepX = 0.0

    private var turnStepY = 0.0

    private var turnHeading = 0.0

    internal var turnRandom = Random(Random.nextInt())

    @Volatile
    var activeTurnLabel: String? = null
        private set

    fun reset() {
        turnTime = 0.0
        turnRandom = Random(Random.nextInt())
        cancelTurn()
    }

    fun cancelTurn() {
        remainingTurnSteps = 0
        activeTurnLabel = null
    }

    override fun update(entity: OdorWorldEntity) {
        val world = entity.world
        val temperature = gradient.temperatureAt(entity.location, world)
        var heading = entity.heading * PI / 180.0
        if (useEmpiricalTurns && remainingTurnSteps > 0) {
            // A scripted turn owns the heading for its whole duration; steering the Turning effectors
            // applied at the end of the previous tick must not leak into it.
            heading = turnHeading
        }

        val turn = if (useEmpiricalTurns && remainingTurnSteps == 0) {
            ThermotaxisTurnPolicy.select(temperature, turnTime, heading, turnRandom, gradient.direction)
        } else {
            null
        }
        if (turn != null) {
            heading = turn.heading
            val duration = turn.durationSeconds.coerceAtLeast(dt)
            turnStepX = dt * turn.displacement * cos(heading) / duration * world.width / PLATE_WIDTH
            turnStepY = -dt * turn.displacement * sin(heading) / duration * world.height / PLATE_HEIGHT
            remainingTurnSteps = (duration / dt).roundToInt()
            activeTurnLabel = turn.label
        }

        val isTurning = useEmpiricalTurns && remainingTurnSteps > 0
        val stepDistance: Double
        var nextX: Double
        var nextY: Double
        if (isTurning) {
            nextX = entity.x + turnStepX
            nextY = entity.y + turnStepY
            stepDistance = hypot(turnStepX, turnStepY)
            remainingTurnSteps--
        } else {
            activeTurnLabel = null
            stepDistance = CRAWLING_SPEED * dt * world.width / PLATE_WIDTH
            nextX = entity.x + stepDistance * cos(heading)
            nextY = entity.y - stepDistance * sin(heading)
        }

        val edgeMargin = 12.0
        if (nextX < edgeMargin || nextX > world.width - edgeMargin) {
            heading = PI - heading
            nextX = nextX.coerceIn(edgeMargin, world.width - edgeMargin)
        }
        if (nextY < edgeMargin || nextY > world.height - edgeMargin) {
            heading = -heading
            nextY = nextY.coerceIn(edgeMargin, world.height - edgeMargin)
        }
        if (isTurning) {
            turnHeading = heading
        }
        entity.heading = heading * 180.0 / PI
        entity.location = point(nextX, nextY)
        entity.recordTravelDistance(stepDistance)
        turnTime += dt
    }

    override fun copy(): ThermotaxisWormBehavior = ThermotaxisWormBehavior().also {
        it.gradient = gradient
        it.useEmpiricalTurns = useEmpiricalTurns
    }

    override val name: String
        get() = "Thermotaxis worm"
}
