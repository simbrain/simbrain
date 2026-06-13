package org.simbrain.world.odorworld

import com.thoughtworks.xstream.XStream
import kotlinx.coroutines.Dispatchers
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

    var world: OdorWorld
        private set


    constructor(name: String) : super(name) {
        world = OdorWorld()
        init()
    }

    /**
     * Constructor used in deserializing.
     */
    constructor(name: String, world: OdorWorld) : super(name) {
        this.world = world
        init()
    }

    private fun init() {
        world.events.entityAdded.on(wait = true) { entity: OdorWorldEntity ->
            fireAttributeContainerAdded(entity).await()
            setChangedSinceLastSave(true)
            entity.events.sensorAdded.on(Dispatchers.Default, handler = ::fireAttributeContainerAdded)
            entity.events.effectorAdded.on(Dispatchers.Default, handler = ::fireAttributeContainerAdded)
            entity.events.sensorRemoved.on(Dispatchers.Default, handler = ::fireAttributeContainerRemoved)
            entity.events.effectorRemoved.on(Dispatchers.Default, handler = ::fireAttributeContainerRemoved)
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
                models.addAll(entity.sensors.onEach { it.containerName = entity.name })
                models.addAll(entity.effectors.onEach { it.containerName = entity.name })
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
        fun open(input: InputStream?, name: String, format: String?): OdorWorldComponent {
            val xstream = odorWorldXStream
            val newWorld = xstream.fromXML(input) as OdorWorld
            return OdorWorldComponent(name, newWorld)
        }
    }
}
