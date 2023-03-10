package org.simbrain.world.odorworld.entities

import org.simbrain.util.*
import org.simbrain.util.decayfunctions.DecayFunction
import org.simbrain.util.environment.SmellSource
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.stats.distributions.UniformRealDistribution
import org.simbrain.workspace.AttributeContainer
import org.simbrain.world.odorworld.OdorWorld
import org.simbrain.world.odorworld.effectors.Effector
import org.simbrain.world.odorworld.effectors.StraightMovement
import org.simbrain.world.odorworld.effectors.Turning
import org.simbrain.world.odorworld.events.EntityEvents2
import org.simbrain.world.odorworld.intersect
import org.simbrain.world.odorworld.sensors.GridSensor
import org.simbrain.world.odorworld.sensors.ObjectSensor
import org.simbrain.world.odorworld.sensors.Sensor
import org.simbrain.world.odorworld.sensors.WithDispersion
import kotlin.math.cos
import kotlin.math.sin

class OdorWorldEntity @JvmOverloads constructor(
    val world: OdorWorld,
    @UserParameter(label = "Type", order = 2)
    var entityType: EntityType = EntityType.SWISS,
    @Transient
    var events: EntityEvents2 = EntityEvents2(),
) :
    EditableObject,
    AttributeContainer,
    Locatable by Location(events),
    Rotatable by Rotation(events),
    Movable,
    WithSize by Size(entityType.imageWidth, entityType.imageHeight), Bounded, WithDispersion {

    override var id: String? = null

    @UserParameter(label = "Name", order = 1)
    override var name: String = "null"

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

    /**
     * Smell Source (if any). Initialize to random smell source with 10
     * components.
     */
    var smellSource = SmellSource(10)

    private val _sensors: MutableList<Sensor> = ArrayList()
    val sensors: List<Sensor> get() = _sensors

    private val _effectors: MutableList<Effector> = ArrayList()
    val effectors: List<Effector> get() = _effectors

    /**
     * Whatever phrases the entity can currently "hear".
     */
    val currentlyHeardPhrases: MutableList<String> = arrayListOf()

    val isRotating get() = entityType.isRotating

    /**
     * Manages programatic movement (based on couplings to neurons, etc.)
     */
    val movement = Movement()

    /**
     * Manages movement of the entity using the control keys.
     */
    val manualMovement = ManualMovement()

    @Deprecated("Use world", ReplaceWith("world"))
    val parentWorld
        get() = world

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
            ?.apply { events.collided.fireAndForget(key) }?.value?.dx ?: 0.0

        val moveInY = Bound(x + (dx - distanceXShortenBy * directionX), y + dy, width, height)

        val distanceYShortenBy = bounds
            .associateWith { moveInY.intersect(it) }
            .filter { it.value.intersect }
            .minByOrNull { it.value.dy }
            ?.apply { events.collided.fireAndForget(key) }?.value?.dy ?: 0.0

        val newX = x + (dx - distanceXShortenBy * directionX)
        val newY = y + (dy - distanceYShortenBy * directionY)

        location = if (world.wrapAround) {
            val maxXLocation = world.width
            val maxYLocation = world.height
            point((newX + maxXLocation) % maxXLocation, (newY + maxYLocation) % maxYLocation)
        } else {
            point(newX, newY)
        }

    }

    fun update() {
        applyMovement()
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
        _effectors.add(effector)
        if (effector.id == null) {
            effector.setId(world.effectorIDGenerator.andIncrement)
        }
        events.effectorAdded.fireAndForget(effector)
    }

    fun removeAllEffectors() {
        _effectors.forEach { events.effectorRemoved.fireAndForget(it) }
        _effectors.clear()
    }

    fun removeEffector(effector: Effector) {
        _effectors.remove(effector)
        events.effectorRemoved.fireAndForget(effector)
    }

    fun addSensor(sensor: Sensor) {
        _sensors.add(sensor)
        if (sensor.id == null) {
            sensor.setId(world.sensorIDGenerator.andIncrement)
        }
        events.sensorAdded.fireAndForget(sensor)
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

    fun removeAllSensors() {
        _sensors.forEach { events.sensorRemoved.fireAndForget(it) }
        _sensors.clear()
    }

    fun removeSensor(sensor: Sensor) {
        _sensors.remove(sensor)
        events.sensorRemoved.fireAndForget(sensor)
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