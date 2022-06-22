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

import org.simbrain.util.UserParameter
import org.simbrain.util.Utils.round
import org.simbrain.util.environment.SmellSource
import org.simbrain.util.point
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.toRadian
import org.simbrain.workspace.AttributeContainer
import org.simbrain.world.odorworld.OdorWorld
import org.simbrain.world.odorworld.effectors.Effector
import org.simbrain.world.odorworld.effectors.StraightMovement
import org.simbrain.world.odorworld.effectors.Turning
import org.simbrain.world.odorworld.events.EntityEvents
import org.simbrain.world.odorworld.events.EntityLocationEvent
import org.simbrain.world.odorworld.intersect
import org.simbrain.world.odorworld.sensors.GridSensor
import org.simbrain.world.odorworld.sensors.ObjectSensor
import org.simbrain.world.odorworld.sensors.Sensor
import java.awt.geom.Point2D
import kotlin.math.cos
import kotlin.math.sin

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
    Rotatable by Rotation(events),
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
    val parentWorld get() = world

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
    val sensors: List<Sensor> get() = _sensors

    /**
     * Effectors.
     */
    private val _effectors: MutableList<Effector> = ArrayList()
    val effectors: List<Effector> get() = _effectors

    /**
     * Smell Source (if any). Initialize to random smell source with 10
     * components.
     */
    var smellSource = SmellSource(10)

    /**
     * Before moving, see if there are any collisions. If there are, change the landing spot of the movement to a
     * point before the collision occurs.
     *
     * Collisions are detected using the AABB algorithm: https://learnopengl.com/In-Practice/2D-Game/Collisions/Collision-detection
     */
    fun applyMovement() {
        if (dtheta != 0.0) {
            heading += dtheta
        }

        val dx = cos(heading.toRadian()) * speed
        val dy = -sin(heading.toRadian()) * speed

        val bounds = world.collidableObjects.filter { it !== this }

        val directionX = if (dx > 0) 1 else -1
        val directionY = if (dy > 0) 1 else -1

        val moveInX = Bound(x + dx, y, width, height)

        val distanceXShortenBy = bounds
            .associateWith { moveInX.intersect(it) }
            .filter { it.value.intersect }
            .minByOrNull { it.value.dx }
            ?.apply { events.fireCollided(key) }?.value?.dx ?: 0.0

        val moveInY = Bound(x + (dx - distanceXShortenBy * directionX), y + dy, width, height)

        val distanceYShortenBy = bounds
            .associateWith { moveInY.intersect(it) }
            .filter { it.value.intersect }
            .minByOrNull { it.value.dy }
            ?.apply { events.fireCollided(key) }?.value?.dy ?: 0.0

        val newX = x + (dx - distanceXShortenBy * directionX)
        val newY = y + (dy - distanceYShortenBy * directionY)

        location = if (world.wrapAround) {
            val maxXLocation = (world.width - width)
            val maxYLocation = (world.height - height)
            point((newX + maxXLocation) % maxXLocation, (newY + maxYLocation) % maxYLocation)
        } else {
            point(newX, newY)
        }

    }

    fun update() {
        applyMovement()
        sensors.forEach { it.update(this) }
        effectors.forEach { it.update(this) }
    }

    override fun toString(): String {
        return "name = $name type = $entityType location = (${round(x, 2)}, ${round(y, 2)})"
    }

    fun addEffector(effector: Effector) {
        _effectors.add(effector)
        if (effector.id == null) {
            effector.setId(world.effectorIDGenerator.andIncrement)
        }
        events.fireEffectorAdded(effector)
    }

    fun removeAllEffectors() {
        _effectors.forEach { events.fireEffectorRemoved(it) }
        _effectors.clear()
    }

    fun removeEffector(effector: Effector) {
        _effectors.remove(effector)
        events.fireEffectorRemoved(effector)
    }

    fun addSensor(sensor: Sensor) {
        _sensors.add(sensor)
        if (sensor.id == null) {
            sensor.setId(world.sensorIDGenerator.andIncrement)
        }
        events.fireSensorAdded(sensor)
    }

    fun addDefaultSensorsEffectors() {
        addDefaultEffectors()
        addSensor(ObjectSensor(EntityType.SWISS, 50.0, 45.0))
        addSensor(ObjectSensor(EntityType.SWISS, 0.0, 0.0))
        addSensor(
            ObjectSensor(EntityType.SWISS, 50.0, -45.0)
        )
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

    fun onCollide(block: (other: Bounded) -> Unit) {
    }

    fun removeAllSensors() {
        _sensors.forEach { events.fireSensorRemoved(it) }
        _sensors.clear()
    }

    fun removeSensor(sensor: Sensor) {
        _sensors.remove(sensor)
        events.fireSensorRemoved(sensor)
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
        return world.entityList
            .filter { it !== this }
            .filter { it.location.distance(location) <= radius }
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
        TODO()
    }

    /**
     * Add left and right sensors of a given type.
     *
     * @param type type of sensor to add
     * @param range the range of the object sensors
     */
    fun addLeftRightSensors(type: EntityType?, range: Double) {
        addObjectSensor(type, 50.0, 45.0, range) // Left sensor
        addObjectSensor(type, 50.0, -45.0, range) // Right sensor
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

    /**
     * Add an object sensor to this entity.
     */
    fun addObjectSensor(type: EntityType?, radius: Double, angle: Double, range: Double): ObjectSensor {
        val sensor = ObjectSensor(type, radius, angle)
        sensor.setRange(range)
        addSensor(sensor)
        return sensor
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

}

interface StaticallyLocatable {
    val x: Double
    val y: Double
    val location: Point2D
}

interface Locatable : StaticallyLocatable {
    override var x: Double
    override var y: Double
    override var location: Point2D
}

/**
 * Top-left x, y position of the entity.
 */
class Location(@Transient private val event: EntityLocationEvent) : Locatable {
    @Transient
    private var dirty = true

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

    @Transient
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

interface Rotatable {
    var heading: Double
}

class Rotation(@Transient private val event: EntityLocationEvent) : Rotatable {
    @UserParameter(label = "heading", description = "heading", order = 2)
    override var heading = 0.0
        set(value) {
            event.fireMoved()
            field = ((value % 360.0) + 360.0) % 360.0
        }
}

/**
 * Common interface for [Size] and [Bounded].
 */
interface WithSize {
    val width: Double
    val height: Double
    val size: Point2D
        get() = point(width, height)
}

class Size(override val width: Double, override val height: Double) : WithSize

interface Bounded : StaticallyLocatable, WithSize

class Bound(
    override val x: Double,
    override val y: Double,
    override val width: Double,
    override val height: Double
) : Bounded {
    override val location: Point2D = point(x, y)
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
