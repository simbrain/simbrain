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

import com.thoughtworks.xstream.XStream
import org.simbrain.util.createConstructorCallingConverter
import org.simbrain.util.getSimbrainXStream
import org.simbrain.util.piccolo.TileMap
import org.simbrain.util.piccolo.TiledDataConverter
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.WorkspaceComponent
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import java.io.InputStream
import java.io.OutputStream

/**
 * **WorldPanel** is the container for the world component. Handles toolbar
 * buttons, and serializing of world data. The main environment codes is in [OdorWorldPanel].
 */
class OdorWorldComponent : WorkspaceComponent {
    /**
     * Reference to model world.
     */
    var world: OdorWorld
        private set

    /**
     * Default constructor.
     *
     * @param name
     */
    constructor(name: String?) : super(name!!) {
        world = OdorWorld()
        init()
    }

    /**
     * Constructor used in deserializing.
     *
     * @param name  name of world
     * @param world model world
     */
    constructor(name: String?, world: OdorWorld) : super(name!!) {
        this.world = world
        init()
    }

    private fun init() {
        world.events.entityAdded.on { entity: OdorWorldEntity ->
            fireAttributeContainerAdded(entity)
            setChangedSinceLastSave(true)
            entity.events.sensorAdded.on(handler = ::fireAttributeContainerAdded)
            entity.events.effectorAdded.on(handler = ::fireAttributeContainerAdded)
            entity.events.sensorRemoved.on(handler = ::fireAttributeContainerRemoved)
            entity.events.effectorRemoved.on(handler = ::fireAttributeContainerRemoved)
            setChangedSinceLastSave(true)
        }

        world.events.entityRemoved.on { e: OdorWorldEntity ->
            fireAttributeContainerRemoved(e)
            e.sensors.forEach(this::fireAttributeContainerRemoved)
            e.effectors.forEach(this::fireAttributeContainerRemoved)
            setChangedSinceLastSave(true)
        }
    }

    override val xml: String
        get() = odorWorldXStream.toXML(world)

    override fun save(output: OutputStream, format: String?) {
        odorWorldXStream.toXML(world, output)
    }

    override suspend fun update() {
        world.update()
    }

    override val attributeContainers: List<AttributeContainer>
        get() {
            val models: MutableList<AttributeContainer> = ArrayList()
            for (entity in world.entityList) {
                models.add(entity)
                models.addAll(entity.sensors)
                models.addAll(entity.effectors)
            }
            return models
        }

    override fun start() {
        world.start()
    }

    override fun stop() {
        world.stopAnimation()
    }

    companion object {
        val odorWorldXStream: XStream
            get() {
                val xstream = getSimbrainXStream()
                xstream.processAnnotations(TileMap::class.java)
                xstream.registerConverter(TiledDataConverter(xstream.mapper, xstream.reflectionProvider))
                xstream.registerConverter(
                    createConstructorCallingConverter(
                        listOf(
                            OdorWorldEntity::class.java
                        ), xstream.mapper, xstream.reflectionProvider
                    )
                )
                return xstream
            }

        /**
         * Recreates an instance of this class from a saved component.
         */
        fun open(input: InputStream?, name: String?, format: String?): OdorWorldComponent {
            val xstream = odorWorldXStream
            val newWorld = xstream.fromXML(input) as OdorWorld
            return OdorWorldComponent(name, newWorld)
        }
    }
}
