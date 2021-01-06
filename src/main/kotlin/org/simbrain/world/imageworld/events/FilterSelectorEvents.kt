package org.simbrain.world.imageworld.events

import org.simbrain.util.Event
import org.simbrain.world.imageworld.filters.FilterSelector
import org.simbrain.world.imageworld.filters.Filter
import java.beans.PropertyChangeSupport
import java.util.function.Consumer

/**
 * See [Event].
 */
class FilterSelectorEvents(val source : FilterSelector) : Event(PropertyChangeSupport(source)) {

    fun onFilterAdded(handler: Consumer<Filter>) = "FilterAdded".itemAddedEvent(handler)
    fun fireFilterAdded(filter: Filter) = "FilterAdded"(new = filter)

    fun onFilterRemoved(handler: Consumer<Filter>) =
        "FilterRemoved".itemRemovedEvent(handler)
    fun onFilterAdded(filter: Filter) = "FilterRemoved"(old = filter)

}