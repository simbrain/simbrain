package org.simbrain.world.imageworld.events

import org.simbrain.util.FlowEvents
import org.simbrain.world.imageworld.filters.ImageProcessingPipeline

/**
 * Events for image pipeline collection management.
 * Updated to work with ImageProcessingPipeline objects.
 */
class ImagePipelineCollectionEvents: FlowEvents() {

    val pipelineAdded = AwaitableEvent<ImageProcessingPipeline>()
    val pipelineRemoved = AwaitableEvent<ImageProcessingPipeline>()
    val pipelineChanged = AwaitableEvent<Pair<ImageProcessingPipeline, ImageProcessingPipeline>>()
    val pipelineSelectionChanged = AwaitableEvent<ImageProcessingPipeline>()

}