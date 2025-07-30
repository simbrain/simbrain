package org.simbrain.world.imageworld.events

import org.simbrain.util.Events
import org.simbrain.world.imageworld.filters.ImageTransformation

/**
 * Events for transformation collection management.
 * Renamed from FilterCollectionEvents to better distinguish from the new multi-filter system.
 */
class TransformationCollectionEvents: Events() {

    val transformationAdded = OneArgEvent<ImageTransformation>()
    val transformationRemoved = OneArgEvent<ImageTransformation>()
    val transformationChanged = ChangedEvent<ImageTransformation>()
    val transformationSelectionChanged = OneArgEvent<ImageTransformation>()

}