package org.simbrain.network.events

import org.simbrain.network.NetworkModel
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

    fun onLabelChange(handler: BiConsumer<String, String>) = "LabelChange".itemChangedEvent(handler)
    fun fireLabelChange(old: String, new: String) = "LabelChange"(old = old, new = new)

    fun onClampChanged(handler: Runnable) = "ClampChanged".event(handler)
    fun fireClampChanged() = "ClampChanged"()

    fun onUpdated(handler: Runnable) = "Updated".event(handler)
    fun fireUpdated() = "Updated"()

}