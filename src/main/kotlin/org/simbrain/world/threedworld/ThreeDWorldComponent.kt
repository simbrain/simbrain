package org.simbrain.world.threedworld

import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.io.xml.DomDriver
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.WorkspaceComponent
import org.simbrain.world.threedworld.ThreeDWorldComponent
import org.simbrain.world.threedworld.engine.ThreeDEngine
import org.simbrain.world.threedworld.engine.ThreeDEngineConverter
import org.simbrain.world.threedworld.entities.Agent
import org.simbrain.world.threedworld.entities.BoxEntityXmlConverter
import org.simbrain.world.threedworld.entities.ModelEntityXmlConverter
import java.io.InputStream
import java.io.OutputStream

/**
 * ThreeDWorldComponent is a workspace component to extract some serialization and attribute
 * management from the ThreeDWorld.
 */
class ThreeDWorldComponent : WorkspaceComponent {
    /**
     * @return The ThreeDWorld for this workspace component.
     */
    var world: ThreeDWorld
        private set

    /**
     * Construct a new ThreeDWorldComponent.
     *
     * @param name The name of the new component.
     */
    constructor(name: String?) : super(name!!) {
        world = ThreeDWorld()
        world.events.closed.on { this.close() }
        world.events.agentAdded.on(handler = this::fireAttributeContainerAdded)
        world.events.agentAdded.on { agent: Agent ->
            fireAttributeContainerAdded(agent)
            setChangedSinceLastSave(true)
            agent.events.sensorAdded.on(handler = this::fireAttributeContainerAdded)
            agent.events.effectorAdded.on(handler = this::fireAttributeContainerAdded)
            agent.events.sensorDeleted.on(handler = this::fireAttributeContainerRemoved)
            agent.events.effectorDeleted.on(handler = this::fireAttributeContainerRemoved)
            setChangedSinceLastSave(true)
        }
        // TODO: Removed (see odorworldcomponent)
    }

    /**
     * Construct a ThreeDWorldComponent with an existing ThreeDWorld.
     *
     * @param name  The name of the new component.
     * @param world The world.
     */
    private constructor(name: String, world: ThreeDWorld) : super(name) {
        this.world = world
    }

    override val attributeContainers: List<AttributeContainer>
        get() {
            val models: MutableList<AttributeContainer> = ArrayList()
            // models.add(world); No couplings at world level currently
            for (entity in world.entities) {
                models.add(entity)
                if (entity is Agent) {
                    val agent = entity
                    models.addAll(agent.sensors)
                    models.addAll(agent.effectors)
                }
            }
            return models
        }

    override fun save(output: OutputStream, format: String?) {
        val previousState = world.engine.state
        world.engine.queueState(ThreeDEngine.State.SystemPause, true)
        xStream.toXML(world, output)
        world.engine.queueState(previousState, false)
    }

    override fun close() {
        super.close()
        world.engine.stop(false)
    }

    override suspend fun update() {
        world.engine.updateSync()
    }

    companion object {
        @JvmStatic
        val xStream: XStream
            /**
             * @return A newly constructed xstream for serializing a ThreeDWorld.
             */
            get() {
                val stream = XStream(DomDriver())
                stream.registerConverter(ThreeDEngineConverter())
                stream.registerConverter(BoxEntityXmlConverter())
                stream.registerConverter(ModelEntityXmlConverter())
                return stream
            }

        /**
         * Open a saved ThreeDWorldComponent from an XML input stream.
         *
         * @param input  The input stream to read.
         * @param name   The name of the new world component.
         * @param format The format of the input stream. Should be xml.
         * @return A deserialized ThreeDWorldComponent with a valid ThreeDWorld.
         */
        fun open(input: InputStream?, name: String, format: String?): ThreeDWorldComponent {
            val world = xStream.fromXML(input) as ThreeDWorld
            world.engine.queueState(ThreeDEngine.State.RenderOnly, false)
            return ThreeDWorldComponent(name, world)
        }

        @JvmStatic
        fun create(workspace: Workspace, name: String?): ThreeDWorldComponent {
            if (workspace.getComponentList(ThreeDWorldComponent::class.java).isEmpty()) {
                return ThreeDWorldComponent(name)
            } else {
                throw RuntimeException("Only one 3D World component is supported.")
            }
        }
    }
}
