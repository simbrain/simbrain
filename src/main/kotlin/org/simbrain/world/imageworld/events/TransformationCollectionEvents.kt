package org.simbrain.world.imageworld.events

import org.simbrain.util.*
import org.simbrain.world.imageworld.filters.Filter

/**
 * Events for transformation collection management.
 * Renamed from FilterCollectionEvents to better distinguish from the new multi-filter system.
 */
class TransformationCollectionEvents: Events() {

    val transformationAdded = OneArgEvent<Filter>()
    val transformationRemoved = OneArgEvent<Filter>()
    val transformationChanged = ChangedEvent<Filter>()
    val transformationSelectionChanged = OneArgEvent<Filter>()

}