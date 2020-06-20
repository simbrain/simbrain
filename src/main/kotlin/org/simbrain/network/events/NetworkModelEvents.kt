package org.simbrain.network.events

import org.simbrain.network.NetworkModel
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.SynapseUpdateRule
import org.simbrain.util.Event
import java.beans.PropertyChangeSupport
import java.util.function.BiConsumer
import java.util.function.Consumer

/**
 * @see Event
 */
open class NetworkModelEvents(val model: NetworkModel) : Event(PropertyChangeSupport(model)) {

    fun fireSelected() = "Selected"(new = model)
    fun onSelected(handler: Consumer<NetworkModel>) = "Selected".itemAddedEvent(handler)

    fun fireDeleted() = "Deleted"(new = model)
    fun onDeleted(handler: Consumer<NetworkModel>) = "Deleted".itemAddedEvent(handler)

}