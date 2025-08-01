package org.simbrain.world.imageworld.events

import org.simbrain.util.Events
import org.simbrain.world.imageworld.filters.ImageProcessingPipeline

/**
 * Events for image pipeline collection management.
 * Updated to work with ImageProcessingPipeline objects.
 */
class ImagePipelineCollectionEvents: Events() {

    val pipelineAdded = OneArgEvent<ImageProcessingPipeline>()
    val pipelineRemoved = OneArgEvent<ImageProcessingPipeline>()
    val pipelineChanged = ChangedEvent<ImageProcessingPipeline>()
    val pipelineSelectionChanged = OneArgEvent<ImageProcessingPipeline>()

}