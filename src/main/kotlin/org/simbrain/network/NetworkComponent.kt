package org.simbrain.network

import kotlinx.coroutines.Dispatchers
import org.simbrain.network.core.Network
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.getNetworkXStream
import org.simbrain.network.llm.LanguageModel
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
     * Models whose label events are already relayed as [fireAttributeContainerChanged], so re-adding a model
     * (e.g. through undo) does not stack duplicate subscriptions.
     */
    private val labelRelayedModels = HashSet<NetworkModel>()

    /**
     * Initialize attribute types and listeners.
     */
    private fun init() {
        val event = network.events

        network.allModelsDeep.forEach(::relayLabelChanges)

        event.modelAdded.on(Dispatchers.Default) { m ->
            setChangedSinceLastSave(true)
            relayLabelChanges(m)
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

    /**
     * Forward label changes on couplable models to [fireAttributeContainerChanged] so coupled consumers
     * (e.g. plots showing per-neuron labels) can refresh.
     */
    private fun relayLabelChanges(model: NetworkModel) {
        if (!labelRelayedModels.add(model)) return
        if (model is AttributeContainer) {
            // A model's own label names it in coupling descriptions, so plots naming series from a scalar
            // coupling need to hear about it, not just the array-label changes below.
            model.events.labelChanged.on(Dispatchers.Default) { _, _ ->
                fireAttributeContainerChanged(model)
            }
        }
        when (model) {
            is NeuronCollection -> model.events.labelArrayChanged.on(Dispatchers.Default) {
                fireAttributeContainerChanged(model)
            }
            is NeuronArray -> model.events.labelArrayChanged.on(Dispatchers.Default) {
                fireAttributeContainerChanged(model)
            }
            is Subnetwork -> model.modelList.deepAll.forEach(::relayLabelChanges)
            else -> {}
        }
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
        pauseIfGenerationEnded()
    }

    private var generationPausePending = false

    private var generationPauseFired = false

    /**
     * Pauses a running workspace one iteration after every pause-requesting language model has
     * halted — end of text, full window, or spent budget. The grace iteration lets the couplings
     * deliver the final window, end marker included, to any coupled document before the pause
     * releases it for editing. Fires once per halt episode and re-arms only when some model can
     * advance again; iterate-style runs, which ignore stop(), are left alone.
     */
    private fun pauseIfGenerationEnded() {
        val requesting = network.getModels<LanguageModel>().filter { it.pauseWorkspaceAtEnd && it.isLoaded }
        val ended = requesting.isNotEmpty() && requesting.none { it.canAdvance }
        if (!ended) {
            generationPausePending = false
            generationPauseFired = false
        } else if (!generationPauseFired) {
            if (generationPausePending) {
                generationPausePending = false
                generationPauseFired = true
                if (workspace.updater.isRunning) workspace.stop()
            } else {
                generationPausePending = true
            }
        }
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
