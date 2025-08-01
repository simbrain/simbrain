package org.simbrain.world.imageworld.transformations

import org.simbrain.workspace.AttributeContainer
import org.simbrain.world.imageworld.ImageSource
import org.simbrain.world.imageworld.events.ImagePipelineCollectionEvents
import org.simbrain.world.imageworld.filters.GrayscaleOperation
import org.simbrain.world.imageworld.filters.ImageProcessingPipeline
import org.simbrain.world.imageworld.filters.ResizeOperation
import org.simbrain.world.imageworld.filters.ThresholdOperation

/**
 * Maintains a list of image processing pipelines that can be applied to an ImageSource.
 */
class ImagePipelineCollection(val imageSource: ImageSource): AttributeContainer {

    /**
     * List of pipelines that can be applied to an image.
     */
    private val pipelinesList = mutableListOf<ImageProcessingPipeline>()

    /**
     * Currently selected pipeline.
     */
    lateinit var currentPipeline: ImageProcessingPipeline
        private set

    /**
     * Reference to the default unfiltered pipeline that cannot be deleted
     */
    private lateinit var defaultUnfilteredPipeline: ImageProcessingPipeline

    /**
     * Handle pipeline events.
     */
    @Transient
    var events = ImagePipelineCollectionEvents()
        private set

    init {
        initializeDefaultPipelines()
        imageSource.events.imageUpdate.on(null, true) {
            pipelinesList.forEach { it.applyPipeline() }
        }
    }

    /**
     * See [org.simbrain.workspace.serialization.WorkspaceComponentDeserializer]
     */
    fun readResolve(): Any {
        events = ImagePipelineCollectionEvents()
        imageSource.events.imageUpdate.on {
            pipelinesList.forEach { it.applyPipeline() }
        }
        // Reinitialize the default unfiltered pipeline reference
        if (pipelinesList.isNotEmpty()) {
            defaultUnfilteredPipeline = pipelinesList[0]
        }
        return this
    }

    /**
     * Initialize some default pipelines on world creation.
     */
    private fun initializeDefaultPipelines() {
        // Load default pipelines
        val unfiltered = ImageProcessingPipeline("Unfiltered", imageSource)
        defaultUnfilteredPipeline = unfiltered
        
        // Add resize operation to match original image size
        imageSource.events.resize.on(null, true) {
            // Update any resize operations to match image size if needed
        }
        pipelinesList.add(unfiltered)

        val gray100x100 = ImageProcessingPipeline("Gray 100x100", imageSource)
        gray100x100.addOperation(ResizeOperation(100, 100))
        gray100x100.addOperation(GrayscaleOperation())
        pipelinesList.add(gray100x100)

        val color100x100 = ImageProcessingPipeline("Color 100x100", imageSource)
        color100x100.addOperation(ResizeOperation(100, 100))
        pipelinesList.add(color100x100)

        val threshold10x10 = ImageProcessingPipeline("Threshold 10x10", imageSource)
        threshold10x10.addOperation(ResizeOperation(10, 10))
        threshold10x10.addOperation(ThresholdOperation(0.5))
        pipelinesList.add(threshold10x10)

        val threshold250x250 = ImageProcessingPipeline("Threshold 250x250", imageSource)
        threshold250x250.addOperation(ResizeOperation(250, 250))
        threshold250x250.addOperation(ThresholdOperation(0.5))
        pipelinesList.add(threshold250x250)

        currentPipeline = pipelinesList[0]
    }

    /**
     * Add a new pipeline to the list.
     */
    fun addPipeline(pipeline: ImageProcessingPipeline) {
        pipelinesList.add(pipeline)
        events.pipelineAdded.fire(pipeline)
    }

    fun addPipeline(name: String, config: ImageProcessingPipeline.() -> Unit = {}) {
        val pipeline = ImageProcessingPipeline(name, imageSource).apply(config)
        pipelinesList.add(pipeline)
    }

    /**
     * Remove the indicated pipeline.
     */
    fun removePipeline(pipeline: ImageProcessingPipeline) {
        // Cannot remove the default unfiltered pipeline
        if (pipeline === defaultUnfilteredPipeline) {
            return
        }
        pipelinesList.remove(pipeline)
        events.pipelineRemoved.fire(pipeline)
    }

    /**
     * Check if a pipeline is the default unfiltered pipeline that cannot be modified
     */
    fun isDefaultUnfilteredPipeline(pipeline: ImageProcessingPipeline): Boolean {
        return pipeline === defaultUnfilteredPipeline
    }

    /**
     * Set the current pipeline.
     */
    fun setCurrentPipeline(pipeline: ImageProcessingPipeline) {
        val oldPipeline = currentPipeline
        currentPipeline = pipeline
        events.pipelineChanged.fire(pipeline, oldPipeline)
    }

    val pipelines: List<ImageProcessingPipeline> get() = pipelinesList.toList()

    override val id: String = "ImagePipelineCollection"

    override val childrenContainers get() = pipelinesList.toList()
}