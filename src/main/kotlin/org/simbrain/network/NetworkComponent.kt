package org.simbrain.network

import kotlinx.coroutines.Dispatchers
import org.simbrain.network.core.Network
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.getNetworkXStream
import org.simbrain.network.subnetworks.Subnetwork
import org.simbrain.util.getSimbrainXStream
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.WorkspaceComponent
import java.io.InputStream
import java.io.OutputStream

/**
 * Network component.
 */
class NetworkComponent : WorkspaceComponent {
    /**
     * Reference to root network, the main model network.
     */
    var network: Network = Network()
        private set

    /**
     * Create a new network component.
     *
     * @param name name
     */
    constructor(name: String?) : super(name!!) {
        init()
    }

    /**
     * Create a new network component.
     *
     * @param name    name of network
     * @param network the network being created
     */
    constructor(name: String?, network: Network) : super(name!!) {
        this.network = network
        init()
    }

    /**
     * Initialize attribute types and listeners.
     */
    private fun init() {
        val event = network.events

        event.modelAdded.on(Dispatchers.Default) { m ->
            setChangedSinceLastSave(true)
            if (m is AttributeContainer) {
                fireAttributeContainerAdded(m)
            }
            if (m is NeuronCollection) {
                m.neuronList.map { addedContainer ->
                    this.fireAttributeContainerAdded(
                        addedContainer
                    )
                }
            }
        }

        event.modelRemoved.on(Dispatchers.Default) { m ->
            setChangedSinceLastSave(true)
            if (m is AttributeContainer) {
                fireAttributeContainerRemoved(m)
            }
            if (m is NeuronCollection) {
                m.neuronList.forEach { removedContainer ->
                    this.fireAttributeContainerRemoved(
                        removedContainer
                    )
                }
            }
        }


        //        event.onNeuronsUpdated(l -> setChangedSinceLastSave(true));
        //
        //        event.onTextAdded(t -> setChangedSinceLastSave(true));
        //
        //        event.onTextRemoved(t -> setChangedSinceLastSave(true));
    }

    override val attributeContainers: List<AttributeContainer>
        get() {
            val result = mutableListOf<AttributeContainer>()
            fun collect(models: Iterable<NetworkModel>) {
                for (model in models) {
                    if (model is AttributeContainer) result.add(model)
                    if (model is Subnetwork) collect(model.modelList.deepAll)
                }
            }
            collect(network.allModelsDeep)
            return result
        }

    override fun save(output: OutputStream, format: String?) {
        getNetworkXStream().toXML(network, output)
    }

    /**
     * Returns a copy of this NetworkComponent.
     *
     * @return the new network component
     */
    fun copy(): NetworkComponent {
        val ret = NetworkComponent("Copy of $name", network.copy())
        return ret
    }

    override suspend fun update() {
        network.updateSuspend(name)
    }

    override val xml: String
        get() = getSimbrainXStream().toXML(network)

    companion object {
        @JvmStatic
        fun open(input: InputStream?, name: String?, format: String?): NetworkComponent {
            val newNetwork = getNetworkXStream().fromXML(input) as Network
            return NetworkComponent(name, newNetwork)
        }
    }
}
