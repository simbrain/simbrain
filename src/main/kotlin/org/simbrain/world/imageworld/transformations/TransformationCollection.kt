package org.simbrain.world.imageworld.transformations

import org.simbrain.world.imageworld.ImageSource
import org.simbrain.world.imageworld.events.TransformationCollectionEvents
import org.simbrain.world.imageworld.filters.Filter
import org.simbrain.world.imageworld.filters.GrayOp
import org.simbrain.world.imageworld.filters.IdentityOp
import org.simbrain.world.imageworld.filters.ThresholdOp

/**
 * Maintains a list of transformations that can be applied to an ImageSource.
 */
class TransformationCollection(val imageSource: ImageSource) {

    /**
     * List of transformations that can be applied to an image.
     */
    private val transformationsList = mutableListOf<Filter>()

    /**
     * Currently selected transformation.
     */
    lateinit var currentTransformation: Filter
        private set

    /**
     * Handle transformation events.
     */
    @Transient
    var events = TransformationCollectionEvents()
        private set

    init {
        initializeDefaultTransformations()
        imageSource.events.imageUpdate.on(null, true) {
            transformationsList.forEach { it.applyFilter() }
        }
    }

    /**
     * See [org.simbrain.workspace.serialization.WorkspaceComponentDeserializer]
     */
    fun readResolve(): Any {
        events = TransformationCollectionEvents()
        imageSource.events.imageUpdate.on {
            transformationsList.forEach { it.applyFilter() }
        }
        return this
    }

    /**
     * Initialize some default transformations on world creation.
     */
    private fun initializeDefaultTransformations() {
        // Load default transformations
        val unfiltered = Filter(
            "Unfiltered",
            imageSource, IdentityOp(), imageSource.width, imageSource.height
        )
        imageSource.events.resize.on(null, true) {
            unfiltered.height = imageSource.currentImage.height
            unfiltered.width = imageSource.currentImage.width
        }
        transformationsList.add(unfiltered)

        val gray100x100 = Filter(
            "Gray 100x100",
            imageSource, GrayOp(), 100, 100
        )
        transformationsList.add(gray100x100)

        val color100x100 = Filter(
            "Color 100x100", imageSource, IdentityOp(), 100, 100
        )
        transformationsList.add(color100x100)

        val threshold10x10 = Filter(
            "Threshold 10x10", imageSource, ThresholdOp(0.5), 10, 10
        )
        transformationsList.add(threshold10x10)

        val threshold250x250 = Filter(
            "Threshold 250x250",
            imageSource, ThresholdOp(0.5), 250, 250
        )
        transformationsList.add(threshold250x250)

        currentTransformation = transformationsList[0]
    }

    /**
     * Add a new transformation to the list.
     */
    fun addTransformation(transformation: Filter) {
        transformationsList.add(transformation)
        events.transformationAdded.fire(transformation)
    }

    /**
     * Remove the indicated transformation.
     */
    fun removeTransformation(transformation: Filter) {
        // Can't remove the "Unfiltered" option
        if (transformation.id.equals("Unfiltered", ignoreCase = true)) {
            return
        }
        transformationsList.remove(transformation)
        events.transformationRemoved.fire(transformation)
    }

    /**
     * Set the current transformation.
     */
    fun setCurrentTransformation(transformation: Filter) {
        val oldTransformation = currentTransformation
        currentTransformation = transformation
        if (oldTransformation != null) {
            events.transformationChanged.fire(transformation, oldTransformation)
        }
    }

    val transformations: List<Filter> get() = transformationsList.toList()

}