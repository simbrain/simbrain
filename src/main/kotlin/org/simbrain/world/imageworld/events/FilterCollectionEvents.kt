package org.simbrain.world.imageworld.events

import org.simbrain.util.Events2
import org.simbrain.world.imageworld.filters.Filter

/**
 * See [Events2].
 */
class FilterCollectionEvents2: Events2() {

    val filterAdded = AddedEvent<Filter>()
    val filterRemoved = RemovedEvent<Filter>()
    val filterChanged = ChangedEvent<Filter>()

}