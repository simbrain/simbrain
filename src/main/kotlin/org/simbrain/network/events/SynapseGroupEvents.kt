package org.simbrain.network.events

import org.simbrain.network.core.Synapse
import org.simbrain.network.groups.SynapseGroup
import org.simbrain.util.Event
import java.awt.geom.Point2D
import java.beans.PropertyChangeSupport
import java.util.function.BiConsumer
import java.util.function.Consumer

/**
 * @see Event
 */
class SynapseGroupEvents(val sg: SynapseGroup) : NetworkModelEvents(sg) {

    fun onDelete(handler: Consumer<SynapseGroup>) = "Delete".itemRemovedEvent(handler)
    fun fireDelete() = "Delete"(old = sg)

    fun onLocationChange(handler: BiConsumer<Point2D, Point2D>) = "LocationChange".itemChangedEvent(handler)
    fun fireLocationChange(old: Point2D, new: Point2D) = "LocationChange"(old = old, new = new)

    fun onLabelChange(handler: BiConsumer<String, String>) = "LabelChange".itemChangedEvent(handler)
    fun fireLabelChange(old: String?, new: String) = "LabelChange"(old = old, new = new)

    fun onSynapseAdded(handler: Consumer<Synapse>) = "SynapseAdded".itemAddedEvent(handler)
    fun fireSynapseAdded(syn: Synapse) = "SynapseAdded"(new = syn)

    fun onSynapseRemoved(handler: Consumer<Synapse>) = "SynapseRemoved".itemRemovedEvent(handler)
    fun fireSynapseRemoved(syn: Synapse) = "SynapseRemoved"(old = syn)

    fun onVisibilityChange(handler: Runnable) = "VisibilityChange".event(handler)
    fun fireVisibilityChange() = "VisibilityChange"()


}