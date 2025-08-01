package org.simbrain.world.imageworld.filters

import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.CopyableObject
import java.awt.image.BufferedImage

/**
 * Base class for all image processing operations that can be applied in a pipeline.
 * This includes both transformations (resize, format changes) and filters (blur, edge detection, etc.).
 */
abstract class ImageOperation : CopyableObject {

    @UserParameter(label = "Enabled", description = "Whether this operation is active", order = 0)
    var enabled: Boolean = true

    @UserParameter(label = "Name", description = "Name of this operation", order = 1)
    override var name: String = this::class.simpleName ?: "Unknown Operation"

    /**
     * Apply this operation to the input image and return the result.
     * If the operation is disabled, returns the input image unchanged.
     */
    fun apply(input: BufferedImage): BufferedImage {
        return if (enabled) {
            applyOperation(input)
        } else {
            input
        }
    }

    /**
     * Implement the actual operation logic in subclasses.
     * This method should create a new BufferedImage with the operation applied.
     */
    protected abstract fun applyOperation(input: BufferedImage): BufferedImage

    /**
     * Create a copy of this operation with the same settings.
     */
    abstract override fun copy(): ImageOperation

    override fun toString(): String = name
} 