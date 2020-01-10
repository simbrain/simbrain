package org.simbrain.network.events

import org.simbrain.network.core.Network
import org.simbrain.network.core.NetworkTextObject
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.dl4j.MultiLayerNet
import org.simbrain.network.dl4j.NeuronArray
import org.simbrain.network.dl4j.WeightMatrix
import org.simbrain.network.groups.NeuronCollection
import org.simbrain.network.groups.NeuronGroup
import org.simbrain.network.groups.Subnetwork
import org.simbrain.network.groups.SynapseGroup
import org.simbrain.util.Event
import java.beans.PropertyChangeSupport
import java.util.function.Consumer

/**
 * All Network events are defined here. This is also the main javadoc for the
 * event design.
 *
 * Events are organized into "fire" functions to broadcast events and "onX" events
 * to handle them.  They are placed next to each other so it is easy to track how event broadcasting and event handling
 * are related.
 *
 * Advantages of this design are: externally no need for strings so all references can be autocompleted.  Also,
 * since the fireX and onX methods are (by convention) next to each other, it's easy
 * to get from the code where an event is fired in the code to where it is handled.
 *
 */
class NetworkEvents(network: Network) : Event(PropertyChangeSupport(network)) {

    fun onUpdateCompleted(handler: Consumer<Boolean>) = "UpdateCompleted".itemAddedEvent(handler)
    fun fireUpdateCompleted(completed: Boolean) = "UpdateCompleted"(new = completed)

    fun onUpdateTimeDisplay(handler: Consumer<Boolean>) = "UpdateTimeDisplay".itemAddedEvent(handler)
    fun fireUpdateTimeDisplay(display: Boolean) = "UpdateTimeDisplay"(new = display)

    fun onNeuronAdded(handler: Consumer<Neuron>) = "NeuronAdded".itemAddedEvent(handler)
    fun fireNeuronAdded(neuron: Neuron) = "NeuronAdded"(new = neuron)
    fun onNeuronRemoved(handler: Consumer<Neuron>) = "NeuronRemoved".itemRemovedEvent(handler)
    fun fireNeuronRemoved(neuron: Neuron) = "NeuronRemoved"(old = neuron)

    fun onSynapseAdded(handler: Consumer<Synapse>) = "SynapseAdded".itemAddedEvent(handler)
    fun fireSynapseAdded(synapse: Synapse) = "SynapseAdded"(new = synapse)
    fun onSynapseRemoved(handler: Consumer<Synapse>) = "SynapseRemoved".itemRemovedEvent(handler)
    fun fireSynapseRemoved(synapse: Synapse) = "SynapseRemoved"(old = synapse)

    fun onTextAdded(handler: Consumer<NetworkTextObject>) = "TextAdded".itemAddedEvent(handler)
    fun fireTextAdded(textObject: NetworkTextObject) = "TextAdded"(new = textObject)
    fun onTextRemoved(handler: Consumer<NetworkTextObject>) = "TextRemoved".itemRemovedEvent(handler)
    fun fireTextRemoved(textObject: NetworkTextObject) = "TextRemoved"(old = textObject)

    fun onNeuronArrayAdded(handler: Consumer<NeuronArray>) = "NeuronArrayAdded".itemAddedEvent(handler)
    fun fireNeuronArrayAdded(neuronArray: NeuronArray) = "NeuronArrayAdded"(new = neuronArray)
    fun onNeuronArrayRemoved(handler: Consumer<NeuronArray>) = "NeuronArrayRemoved".itemRemovedEvent(handler)
    fun fireNeuronArrayRemoved(neuronArray: NeuronArray) = "NeuronArrayRemoved"(old = neuronArray)

    fun onMultiLayerNetworkAdded(handler: Consumer<MultiLayerNet>) = "MultiLayerNetworkAdded".itemAddedEvent(handler)
    fun fireMultiLayerNetworkAdded(multiLayerNet: MultiLayerNet) = "MultiLayerNetworkAdded"(new = multiLayerNet)
    fun onMultiLayerNetworkRemoved(handler: Consumer<MultiLayerNet>) = "MultiLayerNetworkRemoved".itemRemovedEvent(handler)
    fun fireMultiLayerNetworkRemoved(multiLayerNet: MultiLayerNet) = "MultiLayerNetworkRemoved"(old = multiLayerNet)

    fun onNeuronCollectionAdded(handler: Consumer<NeuronCollection>) = "NeuronCollectionAdded".itemAddedEvent(handler)
    fun fireNeuronCollectionAdded(neuronCollection: NeuronCollection) = "NeuronCollectionAdded"(new = neuronCollection)
    fun onNeuronCollectionRemoved(handler: Consumer<NeuronCollection>) = "NeuronCollectionRemoved".itemRemovedEvent(handler)
    fun fireNeuronCollectionRemoved(neuronCollection: NeuronCollection) = "NeuronCollectionRemoved"(old = neuronCollection)

    fun onNeuronGroupAdded(handler: Consumer<NeuronGroup>) = "NeuronGroupAdded".itemAddedEvent(handler)
    fun fireNeuronGroupAdded(neuronGroup: NeuronGroup) = "NeuronGroupAdded"(new = neuronGroup)
    fun onNeuronGroupRemoved(handler: Consumer<NeuronGroup>) = "NeuronGroupRemoved".itemRemovedEvent(handler)
    fun fireNeuronGroupRemoved(neuronGroup: NeuronGroup) = "NeuronGroupRemoved"(old = neuronGroup)

    fun onSynapseGroupAdded(handler: Consumer<SynapseGroup>) = "SynapseGroupAdded".itemAddedEvent(handler)
    fun fireSynapseGroupAdded(synapseGroup: SynapseGroup) = "SynapseGroupAdded"(new = synapseGroup)
    fun onSynapseGroupRemoved(handler: Consumer<SynapseGroup>) = "SynapseGroupRemoved".itemRemovedEvent(handler)
    fun fireSynapseGroupRemoved(synapseGroup: SynapseGroup) = "SynapseGroupRemoved"(old = synapseGroup)

    fun onSubnetworkAdded(handler: Consumer<Subnetwork>) = "SubnetworkAdded".itemAddedEvent(handler)
    fun fireSubnetworkAdded(subnetwork: Subnetwork) = "SubnetworkAdded"(new = subnetwork)
    fun onSubnetworkRemoved(handler: Consumer<Subnetwork>) = "SubnetworkRemoved".itemRemovedEvent(handler)
    fun fireSubnetworkRemoved(subnetwork: Subnetwork) = "SubnetworkRemoved"(old = subnetwork)

    fun onWeightMatrixAdded(handler: Consumer<WeightMatrix>) = "WeightMatrixAdded".itemAddedEvent(handler)
    fun fireWeightMatrixAdded(weightMatrix: WeightMatrix) = "WeightMatrixAdded"(new = weightMatrix)
    fun onWeightMatrixRemoved(handler: Consumer<WeightMatrix>) = "WeightMatrixRemoved".itemRemovedEvent(handler)
    fun fireWeightMatrixRemoved(weightMatrix: WeightMatrix) = "WeightMatrixRemoved"(old = weightMatrix)

    // Batch events only fire when a "batch" is completed, e.g. when deleting a group of
    // neurons no event should be fired until they are all deleted.

    fun onBatchDeletionCompleted(handler: Runnable) = "BatchDeletionCompleted".event(handler)
    fun fireBatchDeletionCompleted() = "BatchDeletionCompleted"()

    fun onBatchLocationUpdateCompleted(handler: Runnable) = "BatchLocationUpdateCompleted".event(handler)
    fun fireBatchLocationUpdateCompleted() = "BatchLocationUpdateCompleted"()


}