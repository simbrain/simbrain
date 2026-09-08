package org.simbrain.world.odorworld.behaviors

import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.util.propertyeditor.CustomTypeName
import org.simbrain.util.rayVsAabb
import org.simbrain.util.shortestAngleDelta
import org.simbrain.util.toRadian
import org.simbrain.util.wrapAroundVectorTo
import org.simbrain.world.odorworld.GridDirection
import org.simbrain.world.odorworld.entities.MovementMode
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Programmatic NPC behavior that drives an entity's movement each tick by writing
 * to its [OdorWorldEntity.movement] (speed and dtheta). Attach to an entity via
 * [OdorWorldEntity.behavior]; it is invoked at the start of [OdorWorldEntity.update].
 *
 * Behaviors built on [Steering] work unchanged in [MovementMode.GRID]: only the four cardinal headings are
 * scored, and the winner becomes a one-cell step through [OdorWorldEntity.pendingGridStep] instead of a
 * speed and turn rate.
 */
abstract class NpcBehavior : CopyableObject {

    abstract fun update(entity: OdorWorldEntity)

    abstract override fun copy(): NpcBehavior

    override fun getTypeList() = npcBehaviorTypes
}

/**
 * Behavior types offered in the entity dialog's dropdown. Mutable so that behaviors defined outside the
 * world package (e.g. simulation-specific ones) can register themselves and remain visible and editable.
 */
val npcBehaviorTypes: MutableList<Class<out CopyableObject>> = mutableListOf(
    NoOpBehavior::class.java,
    Pursue::class.java,
    Evade::class.java,
    Wander::class.java
)

/**
 * Default behavior: no AI control. Manual or coupling-driven movement is unaffected.
 */
@CustomTypeName("None")
class NoOpBehavior : NpcBehavior() {
    override fun update(entity: OdorWorldEntity) {}
    override fun copy(): NoOpBehavior = NoOpBehavior()
    override val name = "None"
}

/**
 * Per-tick snapshot of context-steering scoring, suitable for visualization.
 * The k-th candidate's world-space heading is [headings] [k]; this is the agent's heading
 * plus a fixed offset, so the candidate set rotates with the agent. [obstacleDistances]
 * combine tilemap walls and entity AABBs (whichever the feeler hits first).
 *
 * The mutable fields below are filled in over the course of a tick: [Steering.applyHeading]
 * records what the behavior intended; behaviors can set [behaviorNotes] to explain a decision;
 * `OdorWorldEntity.applyMovement` records what actually moved and whether collision shortened it.
 */
class SteeringDebugInfo(
    val headings: DoubleArray,
    val scores: DoubleArray,
    val obstacleDistances: DoubleArray,
    val chosenHeading: Double,
    val feelerLength: Double
) {
    var intendedSpeed: Double = 0.0
    var intendedDtheta: Double = 0.0
    var actualDx: Double = 0.0
    var actualDy: Double = 0.0
    var collided: Boolean = false
    var behaviorNotes: String = ""
}

/**
 * Shared context-steering primitives. Behaviors sample [numCandidates] headings around
 * the agent, score each (interest minus danger), and pick the best. The agent's heading
 * convention is degrees with 0 = right and screen-space y pointing down, so the
 * unit direction for heading h is (cos h, -sin h) — matching [OdorWorldEntity.velocity].
 */
object Steering {

    /**
     * Iterate candidate headings, raycast a feeler in each against tilemap walls AND other
     * entities' AABBs, and pick the heading with the highest [score]. [score] receives the
     * heading angle, the unit direction, and the distance to the closest obstacle in that
     * direction (clamped to [feelerLength]). [isObstacle] decides which world entities count as
     * obstacles — defaults to "any other entity"; behaviors typically exclude their own
     * targets/threats. [numCandidates] controls ray density (more rays find narrower gaps);
     * [angularOffset] rotates the entire candidate set by a fixed amount, useful for jittering
     * the sample positions between ticks (e.g. while stuck) so directions missed last tick get
     * a chance. When [OdorWorldEntity.showSteeringDebug] is on, per-candidate scores and feeler
     * hits are captured into [OdorWorldEntity.steeringDebug] for the GUI overlay.
     *
     * In [MovementMode.GRID] the candidate set is always the four cardinal headings, since those are the only
     * moves available, and [numCandidates] and [angularOffset] are ignored.
     */
    fun pickBestHeading(
        entity: OdorWorldEntity,
        feelerLength: Double,
        numCandidates: Int,
        angularOffset: Double = 0.0,
        isObstacle: (OdorWorldEntity) -> Boolean = { it !== entity },
        score: (heading: Double, dirX: Double, dirY: Double, obstacleDistance: Double) -> Double
    ): Double {
        val grid = entity.movementMode == MovementMode.GRID
        val candidateCount = if (grid) GridDirection.entries.size else numCandidates
        val offset = if (grid) 0.0 else angularOffset
        val capture = entity.showSteeringDebug
        val headings = if (capture) DoubleArray(candidateCount) else null
        val scores = if (capture) DoubleArray(candidateCount) else null
        val obstacles = if (capture) DoubleArray(candidateCount) else null
        val world = entity.world
        val tileMap = world.tileMap
        val wrap = world.wrapAround
        val ww = world.width
        val wh = world.height
        val agentHalfW = entity.width / 2
        val agentHalfH = entity.height / 2

        // Pre-compute AABBs of nearby obstacle entities, Minkowski-expanded by the agent's
        // half-extents so a hit from the agent's center == agent edge contacting obstacle edge.
        // Positions are shifted to the closest wrapped image of the agent.
        val obsBoxes = ArrayList<DoubleArray>()
        if (world.isObjectsBlockMovement) {
            for (other in world.entityList) {
                if (other === entity || !isObstacle(other)) continue
                val cx: Double
                val cy: Double
                if (wrap) {
                    val v = entity.location.wrapAroundVectorTo(other.location, ww, wh)
                    cx = entity.x + v.x
                    cy = entity.y + v.y
                } else {
                    cx = other.x
                    cy = other.y
                }
                val maxReach = feelerLength + max(other.width, other.height) / 2 + max(entity.width, entity.height) / 2
                val dx = cx - entity.x
                val dy = cy - entity.y
                if (dx * dx + dy * dy > maxReach * maxReach) continue
                obsBoxes.add(
                    doubleArrayOf(
                        cx - other.width / 2 - agentHalfW,
                        cy - other.height / 2 - agentHalfH,
                        other.width + entity.width,
                        other.height + entity.height
                    )
                )
            }
        }

        world.maze?.let { maze ->
            val reach = feelerLength + max(entity.width, entity.height)
            for (wall in maze.wallSegments(world.gridCellPixelSize)) {
                val nearX = entity.x.coerceIn(wall.x1, wall.x2)
                val nearY = entity.y.coerceIn(wall.y1, wall.y2)
                val dx = nearX - entity.x
                val dy = nearY - entity.y
                if (dx * dx + dy * dy > reach * reach) continue
                obsBoxes.add(
                    doubleArrayOf(
                        wall.x1 - agentHalfW,
                        wall.y1 - agentHalfH,
                        (wall.x2 - wall.x1) + entity.width,
                        (wall.y2 - wall.y1) + entity.height
                    )
                )
            }
        }

        val baseHeading = if (grid) entity.facingDirection.heading else entity.heading
        var bestScore = Double.NEGATIVE_INFINITY
        var bestHeading = baseHeading
        for (k in 0 until candidateCount) {
            val heading = ((baseHeading + offset + k * (360.0 / candidateCount)) % 360.0 + 360.0) % 360.0
            val rad = heading.toRadian()
            val dirX = cos(rad)
            val dirY = -sin(rad)
            var dist = tileMap?.raycastBlocked(
                entity.location, dirX, dirY, feelerLength, wrap, agentHalfW, agentHalfH
            ) ?: feelerLength
            for (b in obsBoxes) {
                val d = rayVsAabb(entity.x, entity.y, dirX, dirY, b[0], b[1], b[2], b[3], dist)
                if (d < dist) dist = d
            }
            val s = score(heading, dirX, dirY, dist)
            headings?.set(k, heading)
            scores?.set(k, s)
            obstacles?.set(k, dist)
            if (s > bestScore) {
                bestScore = s
                bestHeading = heading
            }
        }
        if (capture && headings != null && scores != null && obstacles != null) {
            entity.steeringDebug = SteeringDebugInfo(headings, scores, obstacles, bestHeading, feelerLength)
        }
        return bestHeading
    }

    /**
     * Steers toward [targetHeading], turning at most [maxTurn] degrees this tick at [speed]. In
     * [MovementMode.GRID] this instead queues a one-cell step in the cardinal direction nearest
     * [targetHeading] travelling at [speed] pixels per update when [speed] is positive; [maxTurn] does not apply
     * since grid entities face each step.
     */
    fun applyHeading(entity: OdorWorldEntity, targetHeading: Double, speed: Double, maxTurn: Double) {
        if (entity.movementMode == MovementMode.GRID) {
            entity.pendingGridStep = if (speed > 0) GridDirection.fromHeading(targetHeading) else null
            if (speed > 0) entity.gridSpeed = speed
            entity.movement.dtheta = 0.0
            entity.movement.speed = 0.0
            entity.steeringDebug?.let {
                it.intendedSpeed = speed
                it.intendedDtheta = shortestAngleDelta(entity.heading, targetHeading)
            }
            return
        }
        val delta = shortestAngleDelta(entity.heading, targetHeading)
        val dtheta = delta.coerceIn(-maxTurn, maxTurn)
        entity.movement.dtheta = dtheta
        entity.movement.speed = speed
        entity.steeringDebug?.let {
            it.intendedSpeed = speed
            it.intendedDtheta = dtheta
        }
    }

    /**
     * Helper for behaviors that decide to stop. Writes zero speed/dtheta and records [reason]
     * into the debug snapshot for the overlay text. When debug is on, replaces any prior rays
     * with an empty snapshot so the overlay shows just the status text.
     */
    fun stop(entity: OdorWorldEntity, reason: String) {
        entity.pendingGridStep = null
        entity.movement.speed = 0.0
        entity.movement.dtheta = 0.0
        if (entity.showSteeringDebug) {
            entity.steeringDebug = SteeringDebugInfo(
                DoubleArray(0), DoubleArray(0), DoubleArray(0), entity.heading, 0.0
            ).also { it.behaviorNotes = reason }
        }
    }
}
