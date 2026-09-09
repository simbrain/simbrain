/**
 * An object in an [OdorWorld]: position, heading and sprite type, the sensors and effectors attached to it, and its
 * movement. Continuous movement uses speed and turn rate with a swept box collision against tiles, maze walls,
 * other entities and the map edge; grid movement travels cell to cell with new steps accepted only at cell
 * centers. Movement requests arrive through four channels with fixed priority: manual keys, a running grid plan
 * (see [GridStepScope]), an [NpcBehavior], then the coupled [movement] speed and turn rate. The world owns cell
 * geometry and blocking rules.
 */
package org.simbrain.world.odorworld.entities

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import org.simbrain.util.*
import org.simbrain.util.decayfunctions.DecayFunction
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.stats.distributions.UniformRealDistribution
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Producible
import org.simbrain.world.odorworld.GridDirection
import org.simbrain.world.odorworld.OdorWorld
import org.simbrain.world.odorworld.behaviors.NoOpBehavior
import org.simbrain.world.odorworld.behaviors.NpcBehavior
import org.simbrain.world.odorworld.behaviors.SteeringDebugInfo
import org.simbrain.world.odorworld.effectors.Effector
import org.simbrain.world.odorworld.effectors.StraightMovement
import org.simbrain.world.odorworld.effectors.Turning
import org.simbrain.world.odorworld.events.EntityEvents
import org.simbrain.world.odorworld.intersect
import org.simbrain.world.odorworld.sensors.*
import java.awt.geom.Point2D
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.startCoroutine
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin

/**
 * How an entity moves through the world. Continuous movement uses speed and heading with pixel-level collision.
 * Grid movement travels one [OdorWorld.gridCellSizeInTiles] cell at a time at [OdorWorldEntity.gridSpeed] pixels
 * per update, turns in right angles, and honors maze walls; the coupled speed only matters as a sign, new steps
 * are only accepted at cell centers, and the entity always comes to rest on one.
 */
enum class MovementMode { CONTINUOUS, GRID }

class OdorWorldEntity @JvmOverloads constructor(
    val world: OdorWorld,
    entityType: EntityType = EntityType.Swiss,
    @Transient
    var events: EntityEvents = EntityEvents(),
) : EditableObject, AttributeContainer, Locatable, Rotatable, Movable, WithSize, Bounded, WithDispersion {

    override var id: String? = null

    @UserParameter(label = "Name", order = 1)
    override var name: String = "null"

    @UserParameter(label = "Type", order = 2)
    var entityType: EntityType = entityType
        set(value) {
            events.typeChanged.fire(field, value)
            field = value
        }

    @Transient
    private var locationPointDirty = true

    @UserParameter(label = "X", description = "X Position", order = 3)
    override var x = 0.0
        set(value) {
            // can't access width and height during deserialization.  our check for deserialization is that tilemap is null.
            field = if (world.tileMap == null) {
                value
            } else {
                value.coerceIn(0.0, world.width)
            }
            events.moved.fire()
            locationPointDirty = true
        }

    @UserParameter(label = "Y", description = "Y Position", order = 3)
    override var y = 0.0
        set(value) {
            // can't access width and height during deserialization.  our check for deserialization is that tilemap is null.
            field = if (world.tileMap == null) {
                value
            } else {
                value.coerceIn(0.0, world.height)
            }
            events.moved.fire()
            locationPointDirty = true
        }

    @Transient
    override var location: Point2D = point(x, y)
        get() {
            if (locationPointDirty) {
                field = point(x, y)
            }
            locationPointDirty = false
            return field
        }
        set(value) {
            field = value
            x = value.x
            y = value.y
        }

    @UserParameter(label = "heading", description = "heading", order = 2)
    override var heading = 0.0
        set(value) {
            field = ((value % 360.0) + 360.0) % 360.0
            events.moved.fire()
        }

    override val width: Double
        get() = entityType.width.toDouble()

    override val height: Double
        get() = entityType.height.toDouble()

    var movementMode by GuiEditable(
        initValue = MovementMode.CONTINUOUS,
        label = "Movement mode",
        description = "Continuous: speed and heading with pixel collision. Grid: one cell per step, right-angle " +
                "turns, maze walls block steps.",
        order = 5,
        setter = {
            field = it
            // during deserialization the world has no tile map yet, and the saved location must be kept as is
            if (it == MovementMode.GRID) {
                if (world.tileMap != null) snapToCellCenter()
            } else {
                cancelTransit()
            }
        }
    )

    /**
     * Pixels moved per update while travelling between cells in [MovementMode.GRID]. At or above the cell size a
     * step completes in one update. NPC behaviors set this from their max speed, as they set linear speed in
     * continuous mode.
     */
    var gridSpeed by GuiEditable(
        initValue = 8.0,
        label = "Grid speed",
        description = "Pixels per update while moving between cells in grid mode. At or above the cell size a " +
                "step takes a single update. An NPC behavior uses its own max speed instead.",
        min = 0.01,
        order = 6,
        onUpdate = {
            showWidget(widgetValue(::movementMode) == MovementMode.GRID && widgetValue(::behavior) is NoOpBehavior)
        }
    )

    /**
     * Center of the cell being travelled to while a grid step is in progress, or null at rest.
     */
    @Transient
    var transitTarget: Point2D? = null
        private set

    val isInTransit: Boolean
        get() = transitTarget != null

    /**
     * One-shot cardinal step requested by an [NpcBehavior] for the next update in [MovementMode.GRID]. Takes
     * priority over [speed] and [dtheta] and is cleared once consumed.
     */
    @Transient
    var pendingGridStep: GridDirection? = null

    /**
     * Direction held on the manual driving keys, consumed at every cell center until cleared. Takes priority over
     * behaviors and couplings, as manual movement does in continuous mode.
     */
    @Transient
    var manualGridDirection: GridDirection? = null

    @Transient
    private var gridPlan: GridPlanRun? = null

    val isFollowingGridPlan: Boolean
        get() = gridPlan != null

    /**
     * Replace any running plan with [block], a script whose step calls (see [GridStepScope]) each suspend until
     * that step arrives or is blocked. Steps are taken one per cell center, after manual keys and before the
     * behavior. The result completes with true when the block finishes, with false if the plan is replaced or
     * cleared first, and with the block's exception if it throws. Arrival is produced by world updates, so only
     * await the result from code that is not itself a workspace update action.
     */
    fun queueGridSteps(block: suspend GridStepScope.() -> Unit): Deferred<Boolean> {
        clearGridSteps()
        val run = GridPlanRun()
        gridPlan = run
        block.startCoroutine(GridStepScope(this, run), object : Continuation<Unit> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<Unit>) {
                if (gridPlan === run) gridPlan = null
                result.fold(
                    onSuccess = { run.completion.complete(true) },
                    onFailure = { e ->
                        if (e is CancellationException) run.completion.complete(false) else run.completion.completeExceptionally(e)
                    }
                )
            }
        })
        return run.completion
    }

    /**
     * [queueGridSteps] followed by waiting for the plan to finish. Never call this from a workspace update action,
     * since the world updates that carry the steps would be waiting on this call.
     */
    suspend fun walkGridSteps(block: suspend GridStepScope.() -> Unit): Boolean = queueGridSteps(block).await()

    /**
     * Stop any running plan; its block unwinds with a cancellation and its result completes with false.
     */
    fun clearGridSteps() {
        val run = gridPlan ?: return
        gridPlan = null
        val waiting = run.pending?.continuation ?: run.awaitingArrival
        run.pending = null
        run.awaitingArrival = null
        waiting?.resumeWithException(CancellationException("Grid plan cleared"))
        if (!run.completion.isCompleted) run.completion.complete(false)
    }

    @UserParameter(label = "Enable Sensors", order = 6)
    var isSensorsEnabled: Boolean = true

    @UserParameter(label = "Enable Effectors", order = 6)
    var isEffectorsEnabled = true

    @UserParameter(
        label = "Show Sensors / Effectors",
        description = "Show Attributes (Sensors and Effectors)",
        order = 30
    )
    var isShowSensorsAndEffectors = true

    @UserParameter(
        label = "Show Trail",
        description = "If true, a path is shown indicating where the entity travels",
        order = 40
    )
    var isShowTrail = false
        set(value) {
            val oldValue = field
            field = value
            events.trailVisibilityChanged.fire(value, oldValue)
        }

    var distancePerAnimationFrame by GuiEditable(
        initValue = 10.0,
        label = "Distance per animation frame",
        description = "Number of pixels the sprite must move before the next frame in the sprite animation is used",
        min = 0.1,
        increment = 1.0,
        order = 50,
        setter = {
            field = it.coerceAtLeast(0.1)
        },
        onUpdate = {
            showWidget(widgetValue(OdorWorldEntity::entityType).hasMultipleAnimationFrames())
        }
    )

    var drawTrailWithoutRunningWorkspace by GuiEditable(
        initValue = false,
        description = "Draw trials even when the workspace is not running",
        order = 60,
        conditionallyEnabledBy = OdorWorldEntity::isShowTrail
    )

    /**
     * Smell Source (if any). Initialize to random smell source with 10
     * components.
     */
    var smellSource = SmellSource(10)

    val sensors = CopyOnWriteArrayList<Sensor>()

    val effectors = CopyOnWriteArrayList<Effector>()

    val isRotating get() = entityType.rotating

    val velocity get() = point(cos(heading.toRadian()) * speed, -sin(heading.toRadian()) * speed)

    /**
     * Manages programatic movement (based on couplings to neurons, etc.)
     */
    val movement = Movement()

    /**
     * Manages movement of the entity using the control keys.
     */
    val manualMovement = ManualMovement()

    /**
     * NPC behavior. Runs each tick before [applyMovement] and writes to [movement]
     * (speed and dtheta). Defaults to None so manual or coupling-driven movement is unaffected.
     */
    @UserParameter(
        label = "Behavior",
        description = "Programmatic NPC behavior (Pursue, Evade, Wander, ...) that drives movement each tick",
        tab = "Behavior",
        order = 10
    )
    var behavior: NpcBehavior = NoOpBehavior()

    var showSteeringDebug by GuiEditable(
        initValue = false,
        label = "Show Steering Debug",
        description = "Draw the per-candidate scores and obstacle feeler hits used by the NPC behavior",
        tab = "Behavior",
        order = 20
    )

    @Transient
    var steeringDebug: SteeringDebugInfo? = null

    /**
     * True if the previous [applyMovement] tried to move (speed > 0) but progressed less than
     * 10% of intended due to collision. Used by NPC behaviors to escalate ray density / jitter.
     */
    @Transient
    var wasStuckLastTick: Boolean = false

    @Deprecated("Use world", ReplaceWith("world"))
    val parentWorld
        get() = world

    /**
     * The grid cell this entity is in, as (column, row).
     */
    val cell: Pair<Int, Int>
        get() = world.cellAt(location)

    val facingDirection: GridDirection
        get() = GridDirection.fromHeading(heading)

    fun snapToCellCenter() {
        val (column, row) = cell
        location = world.cellCenter(
            column.coerceIn(0, (world.gridColumns - 1).coerceAtLeast(0)),
            row.coerceIn(0, (world.gridRows - 1).coerceAtLeast(0))
        )
        heading = facingDirection.heading
    }

    /**
     * Rotate by [delta] degrees. In grid mode any nonzero turn is a quarter turn in that direction.
     */
    fun turn(delta: Double) {
        heading = if (movementMode == MovementMode.GRID) {
            if (delta == 0.0) heading else facingDirection.heading + 90.0 * sign(delta)
        } else {
            heading + delta
        }
    }

    /**
     * Move one grid cell in [direction] within this call. Returns false, leaving the location unchanged and
     * firing [EntityEvents.collided], when a wall, blocking tile, map edge, or blocking entity is in the way, or
     * when a step is already in transit.
     */
    fun stepOneCell(direction: GridDirection, face: Boolean = true): Boolean =
        requestGridStep(direction, face, instant = true)

    /**
     * Begin a grid step in [direction]. When [instant], the entity lands on the target cell now; otherwise it enters
     * transit and later calls to [advanceTransit] carry it there. Returns false without moving when the step is
     * blocked, which also fires [EntityEvents.collided], or when one is already in transit; check [isInTransit]
     * first to tell the two apart. Never waits, so it is safe to call from a workspace update action.
     */
    fun requestGridStep(
        direction: GridDirection,
        face: Boolean = true,
        instant: Boolean = gridSpeed >= world.gridCellPixelSize
    ): Boolean {
        if (isInTransit) return false
        val target = beginGridStep(direction, face) ?: return false
        if (instant) {
            arriveAt(target)
        } else {
            transitTarget = target
        }
        return true
    }

    /**
     * Carry an in-progress grid step [distance] pixels closer to its target, arriving when that is enough.
     */
    fun advanceTransit(distance: Double) {
        val target = transitTarget ?: return
        val remaining = location.distance(target)
        if (distance >= remaining) {
            arriveAt(target)
            return
        }
        val t = distance / remaining
        recordTravelDistance(distance)
        location = point(x + (target.x - x) * t, y + (target.y - y) * t)
    }

    private fun cancelTransit() {
        transitTarget = null
    }

    private fun arriveAt(target: Point2D) {
        recordTravelDistance(location.distance(target))
        location = target
        cancelTransit()
        gridPlan?.let { run ->
            val continuation = run.awaitingArrival ?: return
            run.awaitingArrival = null
            continuation.resume(true)
        }
    }

    /**
     * Shared start of a grid step: faces [direction] when [face] is set, then returns the target cell center or
     * null (after firing [EntityEvents.collided]) when the step is blocked.
     */
    private fun beginGridStep(direction: GridDirection, face: Boolean): Point2D? {
        if (face) heading = direction.heading
        val (column, row) = cell
        world.gridStepBlocker(column, row, direction, this)?.let {
            events.collided.fire(it)
            return null
        }
        val (targetColumn, targetRow) = world.gridStepTarget(column, row, direction) ?: return null
        return world.cellCenter(targetColumn, targetRow)
    }

    private fun takePendingGridStep(): GridDirection? = pendingGridStep.also { pendingGridStep = null }

    /**
     * At a cell center, begin the next grid step from the command channels in priority order: manual keys, a
     * queued plan, a behavior's request, then the coupled movement channels. Requests lower in the order are
     * discarded, as manual movement overrides the rest in continuous mode.
     */
    private fun beginNextGridStep(instant: Boolean = gridSpeed >= world.gridCellPixelSize) {
        val behaviorStep = takePendingGridStep()
        manualGridDirection?.let {
            wasStuckLastTick = !requestGridStep(it, face = true, instant = instant)
            return
        }
        gridPlan?.takeIf { it.pending != null }?.let {
            beginPlanStep(it, instant)
            return
        }
        behaviorStep?.let {
            wasStuckLastTick = !requestGridStep(it, face = true, instant = instant)
            return
        }
        consumeGridCommand()?.let { wasStuckLastTick = !requestGridStep(it, face = false, instant = instant) }
    }

    /**
     * Begin the step the plan's block is waiting on. A blocked step resumes the block with false right away, and
     * if the block then asks for another step it is tried in the same call, so a plan never loses an update to a
     * wall. An accepted step ends the call: the block resumes with true on arrival, which for an instant step has
     * already happened inside the request.
     */
    private fun beginPlanStep(run: GridPlanRun, instant: Boolean) {
        while (gridPlan === run) {
            val pending = run.pending ?: return
            run.pending = null
            run.awaitingArrival = pending.continuation
            if (requestGridStep(pending.direction, face = true, instant = instant)) {
                wasStuckLastTick = false
                return
            }
            run.awaitingArrival = null
            wasStuckLastTick = true
            pending.continuation.resume(false)
        }
    }

    /**
     * Grid-mode reading of the movement state: a nonzero [dtheta] is a quarter turn, and a nonzero [speed] asks
     * for one cell forward or backward. Returns the direction to step, or null when standing still.
     */
    private fun consumeGridCommand(): GridDirection? {
        if (dtheta != 0.0) turn(dtheta)
        val currentSpeed = speed
        if (currentSpeed == 0.0) {
            wasStuckLastTick = false
            return null
        }
        return if (currentSpeed > 0) facingDirection else facingDirection.opposite
    }

    /**
     * Before moving, see if there are any collisions. If there are, change the landing spot of the movement to a
     * point before the collision occurs.
     *
     * Collisions are detected using the AABB algorithm: https://learnopengl.com/In-Practice/2D-Game/Collisions/Collision-detection
     *
     * In [MovementMode.GRID] this instead begins or continues a grid step at the manual movement increment per
     * call, which is what the panel's key-driving timer needs.
     */
    fun applyMovement() {
        if (movementMode == MovementMode.GRID) {
            if (!isInTransit) beginNextGridStep(instant = false)
            advanceTransit(manualMovement.manualStraightMovementIncrement)
            return
        }
        if (dtheta != 0.0) {
            heading += dtheta
        }

        if (speed == 0.0) {
            wasStuckLastTick = false
            steeringDebug?.let {
                it.actualDx = 0.0
                it.actualDy = 0.0
                it.collided = false
            }
            return
        }

        recordTravelDistance(kotlin.math.abs(speed))

        val (dx, dy) = velocity

        val bounds = world.collidableObjects.filter { it !== this }

        val (effectiveDx, hitInX) = sweep(bounds, dx, 0.0)
        val (effectiveDy, hitInY) = sweep(bounds, 0.0, dy, xOffset = effectiveDx)
        hitInX?.let { events.collided.fire(it) }
        hitInY?.let { events.collided.fire(it) }
        val newX = x + effectiveDx
        val newY = y + effectiveDy

        location = if (world.wrapAround) {
            val maxXLocation = world.width
            val maxYLocation = world.height
            point((newX + maxXLocation) % maxXLocation, (newY + maxYLocation) % maxYLocation)
        } else {
            point(newX, newY)
        }

        val effectiveSpeed = kotlin.math.sqrt(effectiveDx * effectiveDx + effectiveDy * effectiveDy)
        wasStuckLastTick = effectiveSpeed < kotlin.math.abs(speed) * 0.1
        steeringDebug?.let {
            it.actualDx = effectiveDx
            it.actualDy = effectiveDy
            it.collided = hitInX != null || hitInY != null
        }
    }

    /**
     * Sweeps this entity's box along one axis of travel (exactly one of [dx], [dy] is nonzero) from its current
     * location shifted by [xOffset], and returns the travel allowed before the nearest obstacle face together with
     * that obstacle, or the full travel and null. Only faces ahead of the box count, so an obstacle already
     * overlapping the box never blocks, and a box flush against a face can still slide along it. This is what
     * keeps zero-thickness maze walls and fast movers from tunneling. The world edge is the exception: the box is
     * clamped to stay inside it.
     */
    private fun sweep(bounds: List<Bounded>, dx: Double, dy: Double, xOffset: Double = 0.0): Pair<Double, Bounded?> {
        if (dx == 0.0 && dy == 0.0) return 0.0 to null
        val left = x + xOffset - width / 2
        val right = x + xOffset + width / 2
        val top = y - height / 2
        val bottom = y + height / 2
        var allowed = if (dx != 0.0) dx else dy
        var hit: Bounded? = null
        val epsilon = 1e-9
        for (obstacle in bounds) {
            if (obstacle is OdorWorld) {
                // the world contains rather than obstructs: clamp so the box ends inside it, which also nudges a
                // box that already pokes past the edge back in
                val contained = when {
                    dx > 0 -> minOf(allowed, obstacle.width - right)
                    dx < 0 -> maxOf(allowed, -left)
                    dy > 0 -> minOf(allowed, obstacle.height - bottom)
                    else -> maxOf(allowed, -top)
                }
                if (contained != allowed) {
                    allowed = contained
                    hit = obstacle
                }
                continue
            }
            val obstacleTopLeft = obstacle.topLeftLocation
            val oLeft = obstacleTopLeft.x
            val oRight = oLeft + obstacle.width
            val oTop = obstacleTopLeft.y
            val oBottom = oTop + obstacle.height
            val faceDistance: Double
            if (dx != 0.0) {
                if (oTop >= bottom || oBottom <= top) continue
                faceDistance = if (dx > 0) oLeft - right else left - oRight
            } else {
                if (oLeft >= right || oRight <= left) continue
                faceDistance = if (dy > 0) oTop - bottom else top - oBottom
            }
            if (faceDistance < -epsilon) continue
            val travel = if (dx > 0 || dy > 0) faceDistance else -faceDistance
            if (kotlin.math.abs(travel) < kotlin.math.abs(allowed)) {
                allowed = travel
                hit = obstacle
            }
        }
        return allowed to hit
    }

    /**
     * One world iteration. In [MovementMode.GRID] the behavior only runs at cell centers, where a new step may
     * begin; the step then advances by [gridSpeed] each iteration, so sensors report the real position on the way.
     */
    suspend fun update() {
        if (movementMode == MovementMode.GRID) {
            if (!isInTransit) {
                behavior.update(this)
                beginNextGridStep()
            }
            advanceTransit(gridSpeed)
        } else {
            behavior.update(this)
            applyMovement()
        }
        if (isSensorsEnabled) {
            sensors.forEach { it.update(this) }
        }
        if (isEffectorsEnabled) {
            effectors.forEach { it.update(this) }
        }
    }

    override var showDispersion: Boolean = false

    override val decayFunction: DecayFunction get() = smellSource.decayFunction

    override fun toString(): String {
        return """
            [$name] <$entityType>
            ${location.format(2)} ${if (isRotating) "$heading°" else ""}
        """.trimIndent()
    }

    fun addEffector(effector: Effector) {
        effectors.add(effector)
        if (effector.id == null) {
            effector.id = world.effectorIDGenerator.getAndIncrement()
        }
        events.effectorAdded.fire(effector)
    }

    fun removeAllEffectors() {
        effectors.forEach { events.effectorRemoved.fire(it) }
        effectors.clear()
    }

    fun removeEffector(effector: Effector) {
        effectors.remove(effector)
        events.effectorRemoved.fire(effector)
    }

    fun addSensor(sensor: Sensor) {
        sensors.add(sensor)
        if (sensor.id == null) {
            sensor.id = world.sensorIDGenerator.getAndIncrement()
        }
        events.sensorAdded.fire(sensor)
    }

    fun addDefaultSensorsEffectors() {
        addDefaultEffectors()
        addSensor(ObjectSensor(EntityType.Swiss, 50.0, 45.0))
        addSensor(ObjectSensor(EntityType.Swiss, 0.0, 0.0))
        addSensor(
            ObjectSensor(EntityType.Swiss, 50.0, -45.0)
        )
        if (sensors.none { it is View3DSensor }) {
            addSensor(View3DSensor())
        }
        // TODO: Add more defaults
    }

    /**
     * Add straight, left, and right effectors, in that order.
     */
    fun addDefaultEffectors() {
        addEffector(StraightMovement())
        addEffector(Turning(Turning.LEFT))
        addEffector(Turning(Turning.RIGHT))
    }

    fun removeAllSensors() {
        sensors.forEach { events.sensorRemoved.fire(it) }
        sensors.clear()
    }

    fun removeSensor(sensor: Sensor) {
        sensors.remove(sensor)
        events.sensorRemoved.fire(sensor)
    }

    fun delete() {
        events.deleted.fire(this)
    }

    fun getEffector(label: String) = effectors.first { it.label == label }
    fun getSensor(label: String) = sensors.first { it.label == label }

    fun setLocation(x: Int, y: Int) {
        location = point(x, y)
    }

    fun setLocation(x: Double, y: Double) {
        location = point(x, y)
    }

    fun getEntitiesInRadius(radius: Double): List<OdorWorldEntity> {
        return world.entityList
            .filter { it !== this }
            .filter { it.location.distance(location) <= radius }
    }

    fun speakToEntity(phrase: String) {
        sensors.filterIsInstance<Hearing>().forEach { it.hear(phrase) }
    }

    /**
     * Add a grid of tile sensors.
     *
     * @param numTilesX number of rows in grid
     * @param numTilesY number of columns in grid
     */
    @JvmOverloads
    fun addTileSensors(numTilesX: Int, numTilesY: Int, offset: Int = 1) {
        val tileWidth = world.width / numTilesX
        val tileHeight = world.height / numTilesY
        for (i in 0 until numTilesX) {
            for (j in 0 until numTilesY) {
                addSensor(
                    GridSensor(
                        (i * tileWidth + offset).toInt(),
                        (j * tileHeight + offset).toInt(),
                        tileWidth.toInt(),
                        tileHeight.toInt()
                    )
                )
            }
        }
    }

    fun setLocationRelativeToCenter(x: Int, y: Int) {
        val (nx, ny) = point(x, y) + world.location
        setLocation(nx, ny)
    }

    /**
     * Add left and right sensors of a given type.
     *
     * @param type type of sensor to add
     * @param range the range of the object sensors
     */
    fun addLeftRightSensors(type: EntityType, range: Double) {
        addObjectSensor(type, 50.0, 45.0, range) // Left sensor
        addObjectSensor(type, 50.0, -45.0, range) // Right sensor
    }

    fun randomizeLocationAndHeading() {
        location = point(
            UniformRealDistribution(0.0, world.width).sampleDouble(),
            UniformRealDistribution(0.0, world.height).sampleDouble()
        )
        heading = UniformRealDistribution(0.0, 360.0).sampleDouble()
    }

    /**
     * Add an object sensor to this entity.
     */
    fun addObjectSensor(type: EntityType, radius: Double, angle: Double, range: Double): ObjectSensor {
        val sensor = ObjectSensor(type, radius, angle)
        sensor.decayFunction.dispersion = range
        addSensor(sensor)
        return sensor
    }

    @get:UserParameter(label = "Linear Speed", order = 9)
    override var speed: Double
        get() = if (manualMovement.speed != 0.0 || manualMovement.dtheta != 0.0) manualMovement.speed else movement.speed
        set(value) {
            movement.speed = value
        }

    @get:UserParameter(label = "Angular speed", order = 10)
    override var dtheta: Double
        get() = if (manualMovement.speed != 0.0 || manualMovement.dtheta != 0.0) manualMovement.dtheta else movement.dtheta
        set(value) {
            movement.dtheta = value
        }

    /**
     * Current animation frame index. Used for entities with multiple animation frames.
     * This is transient state that syncs between model and view.
     */
    @Transient
    var animationFrame: Int = 0
        private set

    /**
     * Distance (in pixels) accumulated by [applyMovement] but not yet consumed by frame advancement.
     */
    @Transient
    private var accumulatedAnimationDistance: Double = 0.0

    /**
     * Records movement performed outside [applyMovement] so distance-based animation can advance normally.
     */
    fun recordTravelDistance(distance: Double) {
        accumulatedAnimationDistance += kotlin.math.abs(distance)
    }

    /**
     * Consume accumulated movement distance and advance the animation frame accordingly.
     * One frame is advanced per [distancePerAnimationFrame] pixels travelled.
     *
     * @return the number of frames advanced
     */
    fun advanceAnimation(): Int {
        val numFrames = entityType.imageBasePaths.firstOrNull()?.size ?: 1
        if (numFrames <= 1) return 0

        var framesAdvanced = 0
        while (accumulatedAnimationDistance >= distancePerAnimationFrame) {
            animationFrame = (animationFrame + 1) % numFrames
            accumulatedAnimationDistance -= distancePerAnimationFrame
            framesAdvanced++
        }
        return framesAdvanced
    }

    /**
     * Reset animation to the first frame (static pose).
     */
    fun resetAnimation() {
        animationFrame = 0
        accumulatedAnimationDistance = 0.0
    }

    /**
     * Returns the name of the first object encountered in the provided radius, or an empty string if none is found.
     */
    @Producible
    @JvmOverloads
    fun getNearbyObjectName(radius: Int = 10): String? {
        return (world.entityList - this).firstOrNull {
            this.location.distance(it.location) < radius
        }?.entityType?.description
    }

    fun clearTrail() {
        events.trailCleared.fire()
    }

    suspend fun select() {
        events.selected.fire(this)
    }

    override val childrenContainers: List<AttributeContainer>
        get() = sensors + effectors

    @get:Producible
    val locationArray
        get() = doubleArrayOf(x, y)
}

/**
 * Vector from this entity's location to [other], using wrap-around when the world has it on.
 */
fun OdorWorldEntity.vectorTo(other: Point2D): Point2D {
    return if (world.wrapAround) {
        location.wrapAroundVectorTo(other, world.width, world.height)
    } else {
        other - location
    }
}

private fun EntityType.hasMultipleAnimationFrames(): Boolean {
    return (imageBasePaths.firstOrNull()?.size ?: 1) > 1
}
