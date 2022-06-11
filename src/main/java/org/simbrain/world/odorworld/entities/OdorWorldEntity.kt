/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.world.odorworld.entities

import com.thoughtworks.xstream.converters.UnmarshallingContext
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider
import com.thoughtworks.xstream.io.HierarchicalStreamReader
import com.thoughtworks.xstream.mapper.Mapper
import org.simbrain.util.*
import org.simbrain.util.environment.SmellSource
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.workspace.AttributeContainer
import org.simbrain.world.odorworld.OdorWorld
import org.simbrain.world.odorworld.effectors.Effector
import org.simbrain.world.odorworld.effectors.StraightMovement
import org.simbrain.world.odorworld.effectors.Turning
import org.simbrain.world.odorworld.events.EntityEvents
import org.simbrain.world.odorworld.events.EntityLocationEvent
import org.simbrain.world.odorworld.sensors.GridSensor
import org.simbrain.world.odorworld.sensors.ObjectSensor
import org.simbrain.world.odorworld.sensors.Sensor
import java.awt.geom.Point2D
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.javaType

interface Locatable {
    var x: Double
    var y: Double
    var heading: Double
    var location: Point2D
}

class Location(@Transient private val event: EntityLocationEvent) : Locatable {
    private var dirty = false

    @UserParameter(label = "X", description = "X Position", useSetter = true, order = 3)
    override var x = 0.0
        set(value) {
            field = value
            event.fireMoved()
            dirty = true
        }

    @UserParameter(label = "Y", description = "Y Position", useSetter = true, order = 3)
    override var y = 0.0
        set(value) {
            field = value
            event.fireMoved()
            dirty = true
        }

    @UserParameter(label = "heading", description = "heading", order = 2)
    override var heading = 0.0
        set(value) {
            event.fireMoved()
            field = ((value % 360.0) + 360.0) % 360.0
        }


    override var location: Point2D = point(x, y)
        get() {
            if (dirty) {
                field = point(x, y)
            }
            dirty = false
            return field
        }
        set(value) {
            field = value
            x = value.x
            y = value.y
        }

}

interface WithSize {
    var width: Double
    var height: Double
    var size: Point2D
        get() = point(width, height)
        set(value) {
            val (w, h) = value
            width = w
            height = h
        }
}

class Size(override var width: Double, override var height: Double) : WithSize

interface Bounded: WithSize {
    val x: Double
    val y: Double
    val location: Point2D
    val worldBound: Boolean get() = false
}

class Bound(
    override val x: Double,
    override val y: Double,
    override var width: Double,
    override var height: Double,
    override val worldBound: Boolean = false
) : Bounded {
    override val location: Point2D
        get() = point(x, y)
}

fun Bounded.intersect(other: Bounded): BoundIntersection {
    val a = this
    val b = other

    return if (b.worldBound) {
        val left = a.x - b.x
        val right = (b.x + b.width) - (a.x + a.width)
        val top = a.y - b.y
        val bottom = (b.y + b.height) - (a.y + a.height)
        val xCollision = -min(left, right)
        val yCollision = -min(top, bottom)
        BoundIntersection(xCollision > 0 || yCollision > 0, xCollision, yCollision)
    } else {
        val xCollision = min((a.x + a.width) - b.x, (b.x + b.width) - a.x)
        val yCollision = min((a.y + a.height) - b.y, (b.y + b.height) - a.y)

        BoundIntersection(xCollision > 0 && yCollision > 0, xCollision, yCollision)
    }

}

data class BoundIntersection(val intersect: Boolean, val dx: Double, val dy: Double)

interface Movable {
    var speed: Double
    var dtheta: Double
}

class Movement : Movable {
    override var speed: Double = 0.0

    override var dtheta: Double = 0.0

    override fun toString(): String {
        return "Movement(speed=$speed, dtheta=$dtheta)"
    }

}

interface ManuallyMovable : Movable {
    /**
     * Amount to manually move forward or in cardinal directions.
     */
    @get:UserParameter(label = "Straight movement", order = 10, useSetter = true)
    var manualStraightMovementIncrement: Double

    /**
     * Amount to manually rotate.
     */
    @get:UserParameter(label = "Turn amount", order = 10)
    var manualMotionTurnIncrement: Double

    fun increaseSpeed()
    fun decreaseSpeed()
    fun turnLeft()
    fun turnRight()
    fun stopTurning()
}

class ManualMovement(
    override var manualStraightMovementIncrement: Double = 1.0,
    override var manualMotionTurnIncrement: Double = 1.0
) : ManuallyMovable, Movable by Movement() {

    override fun increaseSpeed() {
        speed += manualStraightMovementIncrement
    }

    override fun decreaseSpeed() {
        speed -= manualStraightMovementIncrement
    }

    override fun turnLeft() {
        dtheta = manualMotionTurnIncrement
    }

    override fun turnRight() {
        dtheta = -manualMotionTurnIncrement
    }

    override fun stopTurning() {
        dtheta = 0.0
    }

    override fun toString(): String {
        return "Movement(speed=$speed, dtheta=$dtheta)"
    }
}

class OdorWorldEntity @JvmOverloads constructor(
    val world: OdorWorld,
    @UserParameter(label = "Type", order = 2)
    var entityType: EntityType = EntityType.SWISS,
    @Transient
    var events: EntityEvents = EntityEvents(),
) :
    EditableObject,
    AttributeContainer,
    Locatable by Location(events),
    Movable,
    WithSize by Size(entityType.imageWidth, entityType.imageHeight), Bounded {

    @Deprecated("Use location")
    val centerLocation: Point2D
        get() = location

    @get:JvmName("isSensorsEnabled")
    var sensorsEnabled: Boolean = true
    val currentlyHeardPhrases: MutableList<String> = arrayListOf()

    @UserParameter(label = "Name", order = 1)
    override var name: String = "null"

    override var id: String? = null

    @Deprecated("Use world", ReplaceWith("world"))
    val parentWorld = world

    val isRotating get() = entityType.isRotating

    val movement = Movement()
    val manualMovement = ManualMovement()

    /**
     * Enable effectors. If not the agent is "paralyzed.
     */
    @UserParameter(label = "Enable Effectors", order = 6)
    var isEffectorsEnabled = false

    /**
     * If true, show peripheral attributes.
     */
    @UserParameter(
        label = "Show Sensors / Effectors",
        description = "Show Attributes (Sensors and Effectors)",
        order = 30
    )
    var isShowSensors = true

    /**
     * Sensors.
     */
    private val _sensors: MutableList<Sensor> = ArrayList()
    val sensors: List<Sensor> by this::_sensors

    /**
     * Effectors.
     */
    private val _effectors: MutableList<Effector> = ArrayList()
    val effectors: List<Effector> by this::_effectors

    /**
     * Smell Source (if any). Initialize to random smell source with 10
     * components.
     */
    var smellSource = SmellSource(10)

    fun applyMovement() {
        if (dtheta != 0.0) {
            heading += dtheta
        }

        val dx = cos(heading.toRadian()) * speed
        val dy = -sin(heading.toRadian()) * speed

        val worldBound = Bound(0.0, 0.0, world.width.toDouble(), world.height.toDouble(), worldBound = true)

        val bounds = (world.entityList + worldBound).filter { it !== this }

        val moveInX = Bound(x + dx, y, width, height)

        val directionX = if (dx > 0) 1 else -1
        val directionY = if (dy > 0) 1 else -1

        val distanceXShortenBy = bounds
            .map { moveInX.intersect(it) }
            .filter { it.intersect }
            .minOfOrNull { it.dx } ?: 0.0

        val moveInY = Bound(x + (dx - distanceXShortenBy * directionX), y + dy, width, height)

        val distanceYShortenBy = bounds
            .map { moveInY.intersect(it) }
            .filter { it.intersect }
            .minOfOrNull { it.dy } ?: 0.0

        location = point(x + (dx - distanceXShortenBy * directionX), y + (dy - distanceYShortenBy * directionY))

    }

    fun update() {
        applyMovement()
        sensors.forEach { it.update() }
        effectors.forEach { it.update() }
    }

    fun addEffector(effector: Effector) {
        _effectors.add(effector.apply {
            setId(world.effectorIDGenerator.andIncrement)
        })
    }

    fun removeEffector(effector: Effector) {
        _effectors.remove(effector)
    }

    fun addSensor(sensor: Sensor) {
        _sensors.add(sensor.also {
            it.parent = this
            if (it.id == null) {
                it.setId(world.sensorIDGenerator.andIncrement)
            }
        })
    }

    /**
     * Add some default sensors and effectors.
     */
    fun addDefaultSensorsEffectors() {
        addDefaultEffectors()

        // Add default sensors
        addSensor(
            ObjectSensor(
                this, EntityType.SWISS, Math.PI / 8,
                50.0
            )
        )
        addSensor(ObjectSensor(this, EntityType.SWISS, 0.0, 0.0))
        addSensor(
            ObjectSensor(
                this, EntityType.SWISS, -Math.PI / 8,
                50.0
            )
        )

        //TODO: Add more defaults
    }

    /**
     * Add straight, left, and right effectors, in that order.
     */
    fun addDefaultEffectors() {
        addEffector(StraightMovement(this))
        addEffector(Turning(this, Turning.LEFT))
        addEffector(Turning(this, Turning.RIGHT))
    }

    fun onCollide(block: (other: Bounded) -> Unit) {

    }

    fun removeSensor(sensor: Sensor) {
        _sensors.remove(sensor)
    }

    fun delete() {
        world.deleteEntity(this)
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
        return world.entityList.filter { it.location.distance(location) <= radius }
    }

    fun speakToEntity(phrase: String) {
        currentlyHeardPhrases.add(phrase)
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
                addSensor(GridSensor(this, i * tileWidth + offset, j * tileHeight + offset, tileWidth, tileHeight))
            }
        }
    }

    fun setLocationRelativeToCenter(x: Int, y: Int) {
        TODO()
    }

    fun addLeftRightSensors(entityType: EntityType, angle: Int) {
        TODO()
    }

    @Deprecated("Use location=")
    fun setCenterLocation(x: Int, y: Int) {
        location = point(x, y)
    }

    @Deprecated("Use location=")
    fun setCenterLocation(x: Float, y: Float) {
        location = point(x.toDouble(), y.toDouble())
    }

    fun randomizeLocation() {
        TODO()
    }

    fun addObjectSensor(entityType: EntityType, radius: Double, angle: Double, range: Double): ObjectSensor {
        TODO("Not yet implemented")
    }

    @get:UserParameter(label = "Linear Speed", order = 9, useSetter = true)
    override var speed: Double
        get() = if (manualMovement.speed != 0.0 || manualMovement.dtheta != 0.0) manualMovement.speed else movement.speed
        set(value) {
            movement.speed = value
        }

    @get:UserParameter(label = "Angular speed", order = 10, useSetter = true)
    override var dtheta: Double
        get() = if (manualMovement.speed != 0.0 || manualMovement.dtheta != 0.0) manualMovement.dtheta else movement.dtheta
        set(value) {
            movement.dtheta = value
        }

    /**
     * Perform initialization of objects after de-serializing.
     */
    fun readResolve(): Any {
        events = EntityEvents()

        // motionEventListeners = ArrayList()
        // collisionEventHandlers = ArrayList()
        // currentlyHeardPhrases = ArrayList()
        sensors.forEach { it.postSerializationInit() }
        effectors.forEach { it.postSerializationInit() }
        return this
    }

}

///**
// * Parent class for all Odor World objects.
// */
//open class OdorWorldEntity1(world: OdorWorld) : EditableObject, AttributeContainer, CopyableObject,
//    Locatable by Location() {
//    @UserParameter(label = "Type", order = 2)
//    private var entityType = EntityType.SWISS
//
//    /**
//     * Name of this entity.
//     */
//    @UserParameter(label = "Name", order = 1)
//    private var name: String? = null
//
//    /**
//     * Id of this entity.
//     */
//    private var id: String? = null
//
//    private fun notifyMoved() {
//        updateCollisionBound()
//        updateSensors()
//        updateEffectors()
//        events.fireMoved()
//    }
//
//    /**
//     * Actual collision bound.
//     */
//    @Transient
//    var collisionBound: RectangleCollisionBound? = null
//        private set
//
//    /**
//     * X Velocity. Used internally for [.simpleMotion].
//     */
//    @UserParameter(label = "Velocity X", description = "X Velocity", useSetter = true, order = 10)
//    @set:Consumable(defaultVisibility = false)
//    var velocityX = 0.0
//        set(value) {
//            field = value
//            updateCollisionBound()
//        }
//
//    /**
//     * Y Velocity. Used internally for [.simpleMotion].
//     */
//    @UserParameter(label = "Velocity Y", description = "Y Velocity", useSetter = true, order = 11)
//    @set:Consumable(defaultVisibility = false)
//    var velocityY = 0.0
//        set(value) {
//            field = value
//            updateCollisionBound()
//        }
//
//    /**
//     * Amount to manually move forward or in cardinal directions.
//     */
//    @UserParameter(label = "Straight movement", order = 10)
//    var manualStraightMovementIncrement = 1.0
//
//    /**
//     * The velocity vector used to update the entity's position when it is
//     * manually moved using the keyboard commands.  This should not be set by
//     * the user.  It is computed from the entity's [ ][.manualStraightMovementIncrement] and [.heading].
//     */
//    val manualMovementVelocity = Point2D.Double()
//
//    /**
//     * Set to true when keyboard is being used for movement (instead of couplings, etc.).
//     */
//    var isManualMode = false
//
//    /**
//     * Current heading / orientation.
//     */
//    @UserParameter(label = "heading", description = "heading", order = 2)
//    var heading = DEFAULT_HEADING
//
//    /**
//     * Change in current heading.
//     */
//    private var dtheta = 0.0
//
//    /**
//     * Amount to manually rotate.
//     */
//    @UserParameter(label = "Turn amount", order = 10)
//    var manualMotionTurnIncrement = 1.0
//
//    /**
//     * Back reference to parent parentWorld.
//     */
//    var parentWorld: OdorWorld
//
//    /**
//     * Sensors.
//     */
//    var sensors: MutableList<Sensor> = ArrayList()
//        private set
//
//    /**
//     * Effectors.
//     */
//    var effectors: MutableList<Effector> = ArrayList()
//        private set
//
//    /**
//     * Smell Source (if any). Initialize to random smell source with 10
//     * components.
//     */
//    var smellSource = SmellSource(10)
//
//    /**
//     * Enable sensors. If not the agent is "blind."
//     */
//    @UserParameter(label = "Enable Sensors", order = 5)
//    var isSensorsEnabled = false
//    /**
//     * @return the effectorsEnabled
//     */
//    /**
//     * @param effectorsEnabled the effectorsEnabled to set
//     */
//    /**
//     * Enable effectors. If not the agent is "paralyzed.
//     */
//    @UserParameter(label = "Enable Effectors", order = 6)
//    var isEffectorsEnabled = false
//    /**
//     * @return the showSensors
//     */
//    /**
//     * If true, show peripheral attributes.
//     */
//    @UserParameter(
//        label = "Show Sensors / Effectors",
//        description = "Show Attributes (Sensors and Effectors)",
//        order = 30
//    )
//    var isShowSensors = true
//
//    /**
//     * Things currently being said by talking entities.
//     */
//    private var currentlyHeardPhrases: MutableList<String>? = ArrayList()
//
//    /**
//     * If true, the agent's heading is always updated based on its velocity.
//     */
//    @UserParameter(
//        label = "Heading based on velocity",
//        description = "If true, the agent's heading is updated at each iteration based on its velocity.",
//        order = 100
//    )
//    var isUpdateHeadingBasedOnVelocity = false
//
//    /**
//     * Handle collision events. Used externally by simulations.
//     */
//    @Transient
//    private var collisionEventHandlers: MutableList<Consumer<OdorWorldEntity?>?> = ArrayList()
//
//    /**
//     * Handle "energy consuming" events. Used externally by simulations.
//     */
//    @Transient
//    private var motionEventListeners: MutableList<MotionEvent?> = ArrayList()
//
//    /**
//     * Whether this entity can be "eaten".
//     */
//    @UserParameter(label = "Edible", description = "If true, colliding with this entity makes it respawn", order = 20)
//    var isEdible = false
//    //TODO: Obviously more could be done with "edibility" than simply respawning.
//    // Eg. specify range and timing of respawn, or whether there is a respawn.
//    /**
//     * Current energy. TODO: Do this properly with calories, an energy model, etc.
//     */
//    var energyLevel = 10000.0
//
//    /**
//     * How much energy is supplied by eating this entity.
//     */
//    var calories = 1000.0
//
////    /**
////     * Collision boxes of the tile map
////     */
////    private val tileCollision = TileCollision()
//
//    /**
//     * Event support.
//     */
//    @Transient
//    var events = EntityEvents(this)
//        protected set
//
//    /**
//     * Construct an entity.
//     *
//     * @param world parent world of entity
//     */
//    constructor(world: OdorWorld) {
//        parentWorld = world
//        initCollisionBounds()
//    }
//
//    private fun initCollisionBounds() {
//        collisionBound = RectangleCollisionBound(
//            Rectangle2D.Double(
//                0.0,
//                0.0,
//                getEntityType().imageWidth,
//                getEntityType().imageHeight
//            )
//        )
//        updateCollisionBound()
//    }
//
//    /**
//     * Construct a basic entity with a single image location.
//     *
//     * @param type  image location
//     * @param world parent world
//     */
//    constructor(world: OdorWorld, type: EntityType) {
//        parentWorld = world
//        setEntityType(type)
//        isSensorsEnabled = type.isUseSensors
//        isEffectorsEnabled = type.isUseEffectors
//    }
//
//    /**
//     * Updates this OdorWorldEntity's Animation and its position based on the
//     * velocity.
//     */
//    fun update() {
//        simpleMotion()
//        updateCollisionBound()
//        updateSensors()
//        updateEffectors()
//
//        // For Backwards compatibility
//        if (currentlyHeardPhrases != null) {
//            currentlyHeardPhrases!!.clear()
//        }
//        if (isUpdateHeadingBasedOnVelocity) {
//            updateHeadingBasedOnVelocity()
//        }
//        events.fireUpdated()
//    }
//
//    fun manualMovementUpdate() {
//        if (isManualMode) {
//            updateCollisionBound()
//            simpleMotion()
//            events.fireUpdated()
//        }
//    }
//
//    fun resetManualVelocity() {
//        manualMovementVelocity.setLocation(0.0, 0.0)
//    }
//
//    /**
//     * Simple motion control.
//     */
//    private fun simpleMotion() {
//        val tryVelocity = if (isManualMode) {
//            manualMovementVelocity
//        } else {
//            point(velocityX, velocityY)
//        }
//        collisionBound!!.velocity = tryVelocity
//        val collisionResults =
//            entitiesInCollisionRadius.map { collisionBound!!.veolcityWithoutCollidingWith(it.collisionBound!!) }
//
//        val velocityWithoutColliding =
//            collisionResults.map { it.maxVelocity }.reduceOrNull { maxPossibleVelocity, velocity ->
//                val (mu, mv) = maxPossibleVelocity
//                val (u, v) = velocity
//                point(min(u, mu), min(v, mv))
//            } ?: tryVelocity
//
//        val (dx, dy) = velocityWithoutColliding
//
//        x += dx
//        y += dy
//
//    }
//
//    /**
//     * Get the entity's name.
//     *
//     * @return entity's name.
//     */
//    override fun getName(): String? {
//        return name ?: id
//    }
//
//    /**
//     * Set the entity's name.
//     *
//     * @param name string name for entity.
//     */
//    fun setName(name: String?) {
//        this.name = name
//    }
//
//    /**
//     * @return the id
//     */
//    override fun getId(): String? {
//        return id
//    }
//
//    /**
//     * @param id the id to set
//     */
//    fun setId(id: String?) {
//        this.id = id
//        if (name == null) {
//            name = id
//        }
//    }
//
//    /**
//     * Add an effector.
//     *
//     * @param effector effector to add
//     */
//    fun addEffector(effector: Effector) {
//        // if (effector.getApplicableTypes().contains(this.getClass()))...
//        effectors.add(effector)
//        effector.id = parentWorld.effectorIDGenerator.andIncrement
//        events.fireEffectorAdded(effector)
//    }
//
//    /**
//     * Removes an effector.
//     *
//     * @param effector effector to remove
//     */
//    fun removeEffector(effector: Effector) {
//        effectors.remove(effector)
//        events.fireEffectorRemoved(effector)
//    }
//
//    /**
//     * Add a sensor.
//     *
//     * @param sensor sensor to add
//     */
//    fun addSensor(sensor: Sensor) {
//        // if (sensor.getApplicableTypes().contains(this.getClass()))...
//        sensors.add(sensor)
//        sensor.parent = this
//
//        // Assign an id unless it already has one
//        if (sensor.id == null) {
//            sensor.id = parentWorld.sensorIDGenerator.andIncrement
//        }
//        events.fireSensorAdded(sensor)
//    }
//
//    /**
//     * Get the sensor with the specified label, or null if none found.
//     *
//     *
//     * Some common choices: "Smell-Left", "Smell-Center", and "Smell-Right"
//     *
//     * @param label label to search for
//     * @return the associated sensor
//     */
//    fun getSensor(label: String?) = sensors.firstOrNull { it.label == label }
//
//    /**
//     * Get the effector with the specified label, or null if none found.
//     *
//     * @param label label to search for
//     * @return the associated sensor
//     */
//    fun getEffector(label: String?) = effectors.firstOrNull { it.label == label }
//
//    fun removeSensor(sensor: Sensor) {
//        sensors.remove(sensor)
//        events.fireSensorRemoved(sensor)
//    }
//
//    fun updateEffectors() {
//        if (isEffectorsEnabled) {
//            effectors.forEach(Consumer { obj: Effector -> obj.update() })
//        }
//    }
//
//    fun updateSensors() {
//        if (isSensorsEnabled) {
//            sensors.forEach(Consumer { obj: Sensor -> obj.update() })
//        }
//    }
//
//    fun updateCollisionBound() {
//        if (isManualMode) {
//            collisionBound!!.setVelocity(manualMovementVelocity.getX(), manualMovementVelocity.getY())
//        } else {
//            collisionBound!!.setVelocity(velocityX, velocityY)
//        }
//        collisionBound!!.setLocation(x, y)
//        collisionBound!!.setSize(entityType.imageWidth, entityType.imageHeight) // TODO: optimize
//    }
//    /**
//     * Add a grid of tile sensors, offset by some fraction of a tile's length.
//     *
//     * @param numTilesX number of rows in grid
//     * @param numTilesY number of columns in grid
//     * @param offset    offset amount in pixels
//     */
//    /**
//     * Add a grid of tile sensors.
//     *
//     * @param numTilesX number of rows in grid
//     * @param numTilesY number of columns in grid
//     */
//    @JvmOverloads
//    fun addTileSensors(numTilesX: Int, numTilesY: Int, offset: Int = 1) {
//        val tileWidth = parentWorld.width / numTilesX
//        val tileHeight = parentWorld.height / numTilesY
//        for (i in 0 until numTilesX) {
//            for (j in 0 until numTilesY) {
//                addSensor(GridSensor(this, i * tileWidth + offset, j * tileHeight + offset, tileWidth, tileHeight))
//            }
//        }
//    }
//
//
//    /**
//     * Perform initialization of objects after de-serializing.
//     */
//    fun postSerializationInit() {
//        events = EntityEvents(this)
//        motionEventListeners = ArrayList()
//        collisionEventHandlers = ArrayList()
//        currentlyHeardPhrases = ArrayList()
//        sensors.forEach(Consumer { obj: Sensor -> obj.postSerializationInit() })
//        effectors.forEach(Consumer { obj: Effector -> obj.postSerializationInit() })
//    }
//
//    fun speakToEntity(phrase: String) {
//        currentlyHeardPhrases!!.add(phrase)
//    }
//
//    fun getCurrentlyHeardPhrases(): List<String> {
//        // Return a copy to avoid concurrent modification errors.
//        return ArrayList(currentlyHeardPhrases)
//    }
//
//    /**
//     * Update the entity type of this entity.
//     *
//     * @param entityType the entity type
//     */
//    fun setEntityType(entityType: EntityType) {
//        this.entityType = entityType
//        collisionBound = RectangleCollisionBound(
//            Rectangle2D.Double(
//                0.0,
//                0.0,
//                getEntityType().imageWidth,
//                getEntityType().imageHeight
//            )
//        )
//        updateCollisionBound()
//    }
//
//    fun getEntityType(): EntityType {
//        return entityType
//    }
//
//    /**
//     * Remove this entity. Assumes it's been removed from parent world already.
//     */
//    fun delete() {
//        parentWorld.deleteEntity(this)
//        events.fireDeleted()
//    }
//
//    /**
//     * Returns the heading in radians.
//     *
//     * @return orientation in degrees
//     */
//    val headingRadians: Double
//        get() = heading * Math.PI / 180
//
////    /**
////     * Set the orientation of the creature.
////     *
////     * @param d the orientation, in degrees
////     */
////    fun setHeading(d: Double) {
////
////        // TOOD: Exception if isRotating is false
////        var newHeading = d
////        if (newHeading >= 360) {
////            newHeading -= 360.0
////        }
////        if (newHeading < 0) {
////            newHeading += 360.0
////        }
////        heading = newHeading
////        events.fireMoved()
////    }
//
////    /**
////     * Returns the current heading, in degrees.
////     *
////     * @return current heading.
////     */
////    fun getHeading(): Double {
////        return heading
////    }
//
//    /**
//     * Rotate left by the specified amount.
//     *
//     * @param amount amount to turn left. Assumes a positive number.
//     */
//    //@Consumible(customDescriptionMethod="getId")
//    fun turnLeft(amount: Double) {
//        turn(amount)
//    }
//
//    /**
//     * Turn by the specified amount, positive or negative.
//     *
//     * @param amount
//     */
//    //@Consumible(customDescriptionMethod="getId")
//    fun turn(amount: Double) {
//        if (amount == 0.0) {
//            return
//        }
//        heading += amount
//        events.fireMoved()
//    }
//
//    /**
//     * Rotate right by the specified amount.
//     *
//     * @param amount amount to turn right. Assumes a positive number.
//     */
//    //@Consumible(customDescriptionMethod="getId")
//    fun turnRight(amount: Double) {
//        turn(-amount)
//    }
//
//    /**
//     * Move the entity in a straight line relative to its current heading.
//     *
//     * @param amount
//     */
//    //@Consumible(customDescriptionMethod="getId")
//    fun goStraight(amount: Double) {
//        val radians = headingRadians
//        velocityX = amount * Math.cos(radians)
//        velocityY = -amount * Math.sin(radians)
//        events.fireMoved()
//    }
//
//    fun goStraight() {
//        val radians = headingRadians
//        val dx = manualStraightMovementIncrement * Math.cos(radians)
//        val dy = -manualStraightMovementIncrement * Math.sin(radians)
//        if (isManualMode) {
//            manualMovementVelocity.setLocation(dx, dy)
//        } else {
//            velocityX = dx
//            velocityY = dy
//        }
//    }
//
//    fun goBackwards() {
//        val radians = headingRadians
//        val dx = -manualStraightMovementIncrement * Math.cos(radians)
//        val dy = manualStraightMovementIncrement * Math.sin(radians)
//        if (isManualMode) {
//            manualMovementVelocity.setLocation(dx, dy)
//        } else {
//            velocityX = dx
//            velocityY = dy
//        }
//    }
//
//    fun turnLeft() {
//        dtheta = manualMotionTurnIncrement
//    }
//
//    fun turnRight() {
//        dtheta = -manualMotionTurnIncrement
//    }
//
//    fun stopTurning() {
//        dtheta = 0.0
//    }
//
//    val isRotating: Boolean
//        get() = entityType.isRotating
//
//    /**
//     * Set the heading to be in the direction of current velocity.
//     */
//    fun updateHeadingBasedOnVelocity() {
//        val velocityIsNonZero = velocityX == 0.0 && velocityY != 0.0
//        if (velocityIsNonZero) {
//            heading = (Math.toDegrees(atan2(velocityX, velocityY)) - 90)
//        }
//    }
//
//    /**
//     * Add some default sensors and effectors.
//     */
//    fun addDefaultSensorsEffectors() {
//        addDefaultEffectors()
//
//        // Add default sensors
//        addSensor(
//            ObjectSensor(
//                this, EntityType.SWISS, Math.PI / 8,
//                50.0
//            )
//        )
//        addSensor(ObjectSensor(this, EntityType.SWISS, 0.0, 0.0))
//        addSensor(
//            ObjectSensor(
//                this, EntityType.SWISS, -Math.PI / 8,
//                50.0
//            )
//        )
//
//        //TODO: Add more defaults
//    }
//
//    /**
//     * Add straight, left, and right effectors, in that order.
//     */
//    fun addDefaultEffectors() {
//        addEffector(StraightMovement(this))
//        addEffector(Turning(this, Turning.LEFT))
//        addEffector(Turning(this, Turning.RIGHT))
//    }
//
//    /**
//     * Add an object sensor to this entity.
//     *
//     * @param type the type of object to sense
//     * @param radius the radius of sensor
//     * @param angle the angle of sensor relative to the front of the entity
//     * @param range how far away sensor can detect objects\
//     * @return the sensor
//     */
//    fun addObjectSensor(type: EntityType?, radius: Double, angle: Double, range: Double): ObjectSensor {
//        val sensor = ObjectSensor(this, type, angle, radius)
//        sensor.setRange(range)
//        addSensor(sensor)
//        return sensor
//    }
//
//    /**
//     * Add left and right sensors of a given type.
//     *
//     * @param type type of sensor to add
//     * @param range the range of the object sensors
//     */
//    fun addLeftRightSensors(type: EntityType?, range: Double) {
//        addObjectSensor(type, 50.0, Math.PI / 8, range) // Left sensor
//        addObjectSensor(type, 50.0, -Math.PI / 8, range) // Right sensor
//    }
//
//
//    /**
//     * Get all entities that are in the collision bound of this entity.
//     *
//     * @return a list of entities in the collision bound.
//     */
//    val entitiesInCollisionRadius: List<OdorWorldEntity>
//        get() = parentWorld.entityList.filter {
//            it.isInRadius(
//                it,
//                it.collisionBound!!.collisionRadius + this.collisionBound!!.collisionRadius
//            )
//        }
//
//    /**
//     * Get all entities that are in the given radius.
//     *
//     * @param radius the radius bound
//     * @return a list of entities in the given radius
//     */
//    fun getEntitiesInRadius(radius: Double): List<OdorWorldEntity> {
//        return parentWorld.entityList.stream()
//            .filter { i: OdorWorldEntity -> isInRadius(i, radius) }
//            .collect(Collectors.toList())
//    }
//
//    /**
//     * Check if a given entity is in a radius of this entity.
//     *
//     * @param other  the entity to check
//     * @param radius the radius
//     * @return true if the given entity is in radius
//     */
//    fun isInRadius(other: OdorWorldEntity, radius: Double): Boolean {
//        if (other === this) {
//            return false
//        }
//        return radius * radius > point(x, y).distanceSqTo(point(other.x, other.y))
//    }
//
//    fun getRadiusTo(other: OdorWorldEntity): Double {
//        return point(x, y).distance(point(other.x, other.y))
//    }
//
//    /**
//     * Returns the name of the closest nearby object, if any, in a fixed radius.
//     *
//     * @return the name of the nearby object or an empty string if there is none
//     */
//    @get:Producible
//    val nearbyObjects: String
//        get() {
//            val entities = getEntitiesInRadius(7.0)
//            //TODO: Need them ordered by distance
//            return if (entities.isEmpty()) "" else entities[0].getEntityType().description
//        }
//
//    /**
//     * Can be used to handle collision events by external simulations.  Not serialized.
//     *
//     * @param handler handles the other entity this entity has collided with
//     */
//    fun onCollide(handler: Consumer<OdorWorldEntity?>?) {
//        collisionEventHandlers.add(handler)
//    }
//
//    /**
//     * Can be used to handle motion events by external simulations.  Not serialized.
//     *
//     * @param handler handles events where this entity moves or turns
//     */
//    fun onMotion(handler: MotionEvent?) {
//        motionEventListeners.add(handler)
//    }
//
//    /**
//     * Wrapper for changes in entity location or heading
//     */
//    interface MotionEvent {
//        fun apply(dx: Double, dy: Double, dtheta: Double)
//    }
//
////    /**
////     * A class representing the tile map collision boxes.
////     */
////    inner class TileCollision {
////        private var x = 0
////        private var y = 0
////        private var eightNeighborCollisionBounds: MutableList<RectangleCollisionBound>? = null
////        fun getBounds(x: Int, y: Int): List<RectangleCollisionBound> {
////            if (this.x == x && this.y == y && eightNeighborCollisionBounds != null) {
////                return eightNeighborCollisionBounds
////            }
////            this.x = x
////            this.y = y
////            eightNeighborCollisionBounds = ArrayList()
////            for (i in 0..8) {
////                val tileX = x + i % 3 - 1
////                val tileY = y + i / 3 - 1
////                if (parentWorld.tileMap.collidingAt(tileX, tileY)) {
////                    eightNeighborCollisionBounds.add(
////                        RectangleCollisionBound(
////                            parentWorld.tileMap.getTileBound(
////                                tileX,
////                                tileY
////                            )
////                        )
////                    )
////                }
////            }
////            return eightNeighborCollisionBounds
////        }
////    }
//
//    override fun copy(): OdorWorldEntity {
//        val copy = OdorWorldEntity(parentWorld, entityType)
//        copy.sensors = sensors.stream()
//            .map { obj: Sensor -> obj.copy() }
//            .peek { s: Sensor -> s.parent = copy }
//            .collect(Collectors.toList())
//        copy.effectors = effectors.stream()
//            .map { obj: Effector -> obj.copy() }
//            .peek { s: Effector -> s.parent = copy }
//            .collect(Collectors.toList())
//        copy.name = name
//        copy.id = id
//        copy.x = x
//        copy.y = y
//        copy.isEffectorsEnabled = isEffectorsEnabled
//        copy.isSensorsEnabled = isSensorsEnabled
//        return copy
//    }
//
//    /**
//     * Randomize the location of this entity within the parent world.
//     */
//    fun randomizeLocation() {
//        x = SimbrainRandomizer.rand.nextDouble(0.0, parentWorld.width - entityType.imageWidth)
//        y = SimbrainRandomizer.rand.nextDouble(0.0, parentWorld.height - entityType.imageHeight)
//    }
//
////    fun randomizeLocationInRange(range: Double) {
////        val x = location[0] + SimbrainRandomizer.rand.nextDouble(-range, range)
////        val y = location[1] + SimbrainRandomizer.rand.nextDouble(-range, range)
////        setLocation(x, y)
////    }
////
////    fun randomizeNewLocation(minimumDistance: Double) {
////        val x = location[0]
////        val y = location[1]
////        var newX: Double
////        var newY: Double
////        for (i in 0..19) {
////            newX = SimbrainRandomizer.rand.nextDouble(0.0, parentWorld.width - entityType.imageWidth)
////            newY = SimbrainRandomizer.rand.nextDouble(0.0, parentWorld.height - entityType.imageHeight)
////            val deltaX = newX - x
////            val deltaY = newY - y
////            if (deltaX * deltaX + deltaY + deltaY > minimumDistance * minimumDistance) {
////                setLocation(newX, newY)
////                return
////            }
////        }
////    }
//
//    /**
//     * Set the location relative to the center of the world.
//     *
//     * @param x x offset from center
//     * @param y y offset from center
//     */
//    fun setLocationRelativeToCenter(x: Double, y: Double) {
//        var midX = (parentWorld.width / 2).toDouble()
//        midX -= entityType.imageWidth / 2
//        var midY = (parentWorld.height / 2).toDouble()
//        midY -= entityType.imageHeight / 2
//        this.x = midX + x
//        this.y = midY + y
//    }
//
//    /**
//     * Remove all sensors.
//     */
//    fun clearSensors() {
//        for (sensor in sensors) {
//            events.fireSensorRemoved(sensor)
//        }
//        sensors.clear()
//    }
//
//    /**
//     * Remove all effectors.
//     */
//    fun clearEffectors() {
//        for (effector in effectors) {
//            events.fireEffectorRemoved(effector)
//        }
//        effectors.clear()
//    }
//
//    /**
//     * See [org.simbrain.workspace.serialization.WorkspaceComponentDeserializer]
//     */
//    private fun readResolve(): Any {
//        events = EntityEvents(this)
//        initCollisionBounds()
//        return this
//    }
//
//    override fun toString(): String {
//        return id + ":" + getEntityType() + " (" + Utils.doubleArrayToString(doubleArrayOf(x, y), 2) + ")"
//    }
//
//    fun setLocation(x: Double, y: Double) {
//        this.x = x
//        this.y = y
//    }
//
//    companion object {
//        /**
//         * Initial heading of agent.
//         */
//        private const val DEFAULT_HEADING = 0.0
//
//        /**
//         * Default location for sensors relative to agent.
//         */
//        private const val WHISKER_ANGLE = Math.PI / 4
//    }
//}


// TODO: Temporary initial work on a converter
class OdorWorldEntityConverter(mapper: Mapper, reflectionProvider: ReflectionProvider) :
    ReflectionConverter(mapper, reflectionProvider, OdorWorldEntity::class.java) {

    @OptIn(ExperimentalStdlibApi::class)
    override fun unmarshal(reader: HierarchicalStreamReader, context: UnmarshallingContext): Any {
        //TODO: Guarantee order
        reader.moveDown()
        val world = context.convertAnother(reader, OdorWorld::class.java) as OdorWorld
        reader.moveUp()
        reader.moveDown()
        val type = context.convertAnother(reader, EntityType::class.java) as EntityType
        reader.moveUp()
        val entity = OdorWorldEntity(world, type, EntityEvents())
        // Set the rest of the fields by reflection

        // TODO: XStreamUtils
        val fieldMap = entity::class.declaredMemberProperties.associateBy { it.name }
        while (reader.hasMoreChildren()) {
            reader.moveDown()
            if (reader.nodeName.startsWith("_-_-delegate")) {
                while (reader.hasMoreChildren()) {
                    reader.moveDown()
                    val currentField = if (fieldMap[reader.nodeName] == null) {
                        reader.moveUp()
                        continue
                    } else {
                        fieldMap[reader.nodeName]
                    }
                    val fieldValue = context.convertAnother(reader.value, null)
                    // currentField?.set(entity, fieldValue)
                    reader.moveUp()
                }
                continue
            }
            val currentField = if (fieldMap[reader.nodeName] == null) {
                reader.moveUp()
                continue
            } else {
                fieldMap[reader.nodeName]
            }
            val fieldValue = context.convertAnother(reader.value, currentField?.returnType?.javaType as Class<*>?)
            // currentField?.(entity, fieldValue)
            reader.moveUp()
        }
        return entity
    }
}