package org.simbrain.world.imageworld.events

import org.simbrain.util.Events
import org.simbrain.world.imageworld.filters.Filter

/**
 * See [Events].
 */
class FilterCollectionEvents: Events() {

    val filterAdded = OneArgEvent<Filter>()
    val filterRemoved = OneArgEvent<Filter>()
    val filterChanged = ChangedEvent<Filter>()

}