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
package org.simbrain.world.odorworld

import org.simbrain.util.SimpleIdManager.SimpleId
import org.simbrain.util.SmellSource
import org.simbrain.util.UserParameter
import org.simbrain.util.math.SimbrainMath
import org.simbrain.util.piccolo.TileMap
import org.simbrain.util.piccolo.TileMapLayer
import org.simbrain.util.piccolo.loadTileMap
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.world.odorworld.effectors.Effector
import org.simbrain.world.odorworld.entities.Bounded
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.events.OdorWorldEvents
import org.simbrain.world.odorworld.sensors.Sensor
import java.awt.geom.Point2D
import java.util.*
import java.util.function.Consumer

/**
 * A 2d environment. Contains a list of [OdorWorldEntity]s, which can either be agents or static objects.
 *
 * Agents have sensors for detecting objects, and effectors for moving.
 *
 * Contains a [TileMap] which is a grid of tiles. This tilemap determines the size of the world.
 * The world's size cannot be set directly.
 */
class OdorWorld : EditableObject, Bounded {

    /**
     * List of odor world entities.
     */
    val entityList: MutableList<OdorWorldEntity> = ArrayList()

    /**
     * Basic tilemap that determines the size and basic features of the world.
     */
    var tileMap = loadTileMap("empty.tmx")
        set(value) {
            field = value
            this.selectedLayer = value.layers[0]
            events.tileMapChanged.fire()
        }

    /**
     * Sum of lengths of smell vectors for all smelly objects in the world.
     */
    @Transient
    var maxVectorNorm: Double = 0.0
        private set

    /**
     * Whether or not sprites wrap around or are halted at the borders
     */
    @UserParameter(
        label = "Map boundary wraps around",
        description = "Whether or not entities wrap around or are halted at the borders of the map",
        order = 6
    )
    var wrapAround: Boolean = true

    /**
     * If true, then objects block movements; otherwise agents can walk through
     * objects.
     */
    @UserParameter(
        label = "Objects block movement",
        description = "If true, then objects block movements; otherwise agents can walk through objects",
        order = 10
    )
    var isObjectsBlockMovement: Boolean = true

    @UserParameter(
        label = "Use camera centering", description = "For large worlds centers the camera on the current " +
                "agent. Turn off in particular when not using tilemaps.", order = 20
    )
    var isUseCameraCentering: Boolean = true

    /**
     * Entity Id generator.
     */
    private val entityIDGenerator = SimpleId("Entity", 1)

    /**
     * Sensor Id generator.
     */
    val sensorIDGenerator: SimpleId = SimpleId("Sensor", 1)

    /**
     * Effector Id generator.
     */
    val effectorIDGenerator: SimpleId = SimpleId("Effector", 1)

    /**
     * Agent Name generator.
     */
    private val agentIdGenerator = SimpleId("Agent", 1)

    /**
     * Event support
     */
    @Transient
    var events: OdorWorldEvents = OdorWorldEvents()
        protected set

    /**
     * Last clicked position.
     */
    @JvmField
    var lastClickedPosition: Point2D = Point2D.Double(50.0, 50.0)

    var selectedLayer: TileMapLayer = tileMap.layers[0]

    /**
     * Update world.
     */
    suspend fun update() {
        entityList.forEach(Consumer { obj: OdorWorldEntity -> obj.update() })
        events.updated.fire().await()
    }

    /**
     * Stop animation.
     */
    fun stopAnimation() {
        events.animationStopped.fire()
        events.worldStopped.fire()
    }

    /**
     * Add an Odor World Entity.
     *
     * @param entity the entity to add
     */
    fun addEntity(entity: OdorWorldEntity) {
        if (entityList.contains(entity)) {
            return
        }

        // Set the entity's id
        entity.id = entityIDGenerator.getAndIncrement()
        entity.name = entity.id!!

        // Add entity to the map
        entityList.add(entity)

        events.entityAdded.fire(entity)

        // Recompute max stimulus length
        recomputeMaxVectorNorm()
    }

    /**
     * Add new entity at last clicked position with default properties.
     */
    fun addEntity(): OdorWorldEntity {
        val entity = OdorWorldEntity(this)
        entity.location = Point2D.Double(lastClickedPosition.x, lastClickedPosition.y)
        addEntity(entity)
        return entity
    }

    /**
     * Add entity of a specified type.
     */
    fun addEntity(type: EntityType?): OdorWorldEntity {
        val entity = OdorWorldEntity(this, type!!)
        addEntity(entity)
        return entity
    }

    /**
     * Add entity with a stimulus value.
     */
    fun addEntity(x: Int, y: Int, type: EntityType?, stimulus: DoubleArray?): OdorWorldEntity {
        val entity = addEntity(x, y, type)
        entity.smellSource = SmellSource(stimulus)
        addEntity(entity)
        return entity
    }

    fun addEntity(x: Int, y: Int, type: EntityType?): OdorWorldEntity {
        val entity = OdorWorldEntity(this, type!!)
        entity.setLocation(x, y)
        entity.smellSource = SmellSource(6)
        entity.entityType = type
        addEntity(entity)
        return entity
    }

    fun addEntity(x: Double, y: Double, type: EntityType?): OdorWorldEntity {
        return addEntity(x.toInt(), y.toInt(), type)
    }

    /**
     * Add new "agent" (rotating with some default) sensors and effectors at last clicked position.
     */
    fun addAgent(): OdorWorldEntity {
        val entity = OdorWorldEntity(this, EntityType.MOUSE)
        addEntity(entity)
        entity.entityType = EntityType.MOUSE
        var x = lastClickedPosition.x
        if (x > tileMap.mapWidth - EntityType.MOUSE.imageWidth) {
            x = tileMap.mapWidth - EntityType.MOUSE.imageWidth
        }
        var y = lastClickedPosition.y
        if (y > tileMap.mapHeight - EntityType.MOUSE.imageHeight) {
            y = tileMap.mapHeight - EntityType.MOUSE.imageHeight
        }
        entity.setLocation(x, y)
        entity.addDefaultSensorsEffectors()
        return entity
    }

    fun addTile() {
        tileMap.setTile(
            lastClickedPosition.x.toInt() / tileMap.tileWidth,
            lastClickedPosition.y.toInt() / tileMap.tileHeight,
            61,
            selectedLayer
        )
    }

    /**
     * Returns the entity with the given id, or, failing that, a given name. If
     * no entity is found return null.
     *
     * @param id name of entity
     * @return matching entity, if any
     */
    fun getEntity(id: String?): OdorWorldEntity? {
        // Search by id
        for (entity in entityList) {
            if (entity.id.equals(id, ignoreCase = true)) {
                return entity
            }
        }
        // Search for label if no matching id found
        for (entity in entityList) {
            if (entity.name.equals(id, ignoreCase = true)) {
                return entity
            }
        }
        // Matching entity not found
        return null
    }

    /**
     * Return sensor with matching id or null if none found.
     */
    fun getSensor(id: String?): Sensor? {
        for (entity in entityList) {
            for (sensor in entity.sensors) {
                if (sensor.id.equals(id, ignoreCase = true)) {
                    return sensor
                }
            }
        }
        return null
    }

    /**
     * Return effector with matching id or null if none found.
     */
    fun getEffector(id: String?): Effector? {
        for (entity in entityList) {
            for (effector in entity.effectors) {
                if (effector.id.equals(id, ignoreCase = true)) {
                    return effector
                }
            }
        }
        return null
    }

    /**
     * Returns the sensor with the given id, or null if none is found.
     *
     * @param entityId entity id
     * @param sensorId sensor id
     * @return sensor if found
     */
    fun getSensor(entityId: String?, sensorId: String?): Sensor? {
        val entity = getEntity(entityId) ?: return null
        for (sensor in entity.sensors) {
            if (sensor.id.equals(sensorId, ignoreCase = true)) {
                return sensor
            }
        }
        return null
    }

    /**
     * Returns the effector with the given id, or null if none is found.
     *
     * @param entityId   entity id
     * @param effectorId sensor id
     * @return effector if found
     */
    fun getEffector(
        entityId: String?,
        effectorId: String?
    ): Effector? {
        val entity = getEntity(entityId) ?: return null
        for (effector in entity.effectors) {
            if (effector.id.equals(effectorId, ignoreCase = true)) {
                return effector
            }
        }
        return null
    }

    /**
     * Delete entity.
     *
     * @param entity the entity to delete
     */
    fun deleteEntity(entity: OdorWorldEntity) {
        // map.removeSprite(entity);
        if (entityList.contains(entity)) {
            entityList.remove(entity)
            entity.delete()
            for (sensor in entity.sensors) {
                entity.events.sensorRemoved.fire(sensor)
            }
            for (effector in entity.effectors) {
                entity.events.effectorRemoved.fire(effector)
            }
            recomputeMaxVectorNorm()
            events.entityRemoved.fire(entity)
        }
    }

    /**
     * Caches the maximum vector norm, which is used for scaling the color smell sensors.
     */
    private fun recomputeMaxVectorNorm() {
        maxVectorNorm = entityList.stream()
            .filter { obj: OdorWorldEntity? -> Objects.nonNull(obj) }
            .map { e: OdorWorldEntity -> SimbrainMath.getVectorNorm(e.smellSource.stimulusVector) }
            .max { obj: Double, anotherDouble: Double? -> obj.compareTo(anotherDouble!!) }
            .orElse(0.0)
    }

    /**
     * See [org.simbrain.workspace.serialization.WorkspaceComponentDeserializer]
     */
    private fun readResolve(): Any {
        events = OdorWorldEvents()

        for (entity in entityList) {
            //            entity.postSerializationInit();
        }
        recomputeMaxVectorNorm()
        return this
    }

    @get:UserParameter(label = "Width", displayOnly = true)
    override val width: Double
        get() = tileMap.mapWidth.toDouble()

    @get:UserParameter(label = "Height", displayOnly = true)
    override val height: Double
        get() = tileMap.mapHeight.toDouble()

    val collidableObjects: List<Bounded>
        get() {
            val bounds = ArrayList<Bounded>()

            bounds.addAll(tileMap.collisionBounds)

            if (isObjectsBlockMovement) {
                bounds.addAll(entityList)
            }

            if (!wrapAround) {
                bounds.add(this)
            }

            return bounds
        }

    fun start() {
        events.worldStarted.fire()
    }

    override val name: String
        get() = "Odor World"

    override val x: Double
        get() = width / 2


    override val y: Double
        get() = height / 2

    override val location: Point2D
        /**
         * Center location by default.
         */
        get() = Point2D.Double(x, y)

    val centerLocation: Point2D
        /**
         * Forwards to [getLocation] but makes clear that location is centerlocation.
         */
        get() = location
}
