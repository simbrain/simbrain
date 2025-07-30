package org.simbrain.world.imageworld.events

import org.simbrain.util.Events
import org.simbrain.world.imageworld.filters.ImageTransformation

/**
 * See [Events].
 */
class FilterCollectionEvents: Events() {

    val imageTransformationAdded = OneArgEvent<ImageTransformation>()
    val imageTransformationRemoved = OneArgEvent<ImageTransformation>()
    val imageTransformationChanged = ChangedEvent<ImageTransformation>()
    val imageTransformationSelectionChanged = OneArgEvent<ImageTransformation>()

}