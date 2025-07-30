package org.simbrain.world.imageworld.filters

import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.EditableObject
import java.awt.image.BufferedImage

/**
 * Base class for image processing filters that can be applied to BufferedImages.
 * Unlike transformations which change the size/format of images, filters perform
 * pixel-level processing operations that can be chained together.
 */
abstract class ImageFilter : EditableObject {

    @UserParameter(label = "Enabled", description = "Whether this filter is active", order = 0)
    var enabled: Boolean = true

    @UserParameter(label = "Name", description = "Name of this filter", order = 1)
    override var name: String = this::class.simpleName ?: "Unknown Filter"

    /**
     * Apply this filter to the input image and return the filtered result.
     * If the filter is disabled, returns the input image unchanged.
     */
    fun apply(input: BufferedImage): BufferedImage {
        return if (enabled) {
            applyFilter(input)
        } else {
            input
        }
    }

    /**
     * Implement the actual filtering logic in subclasses.
     * This method should create a new BufferedImage with the filter applied.
     */
    protected abstract fun applyFilter(input: BufferedImage): BufferedImage

    /**
     * Create a copy of this filter with the same settings.
     */
    abstract fun copy(): ImageFilter

    override fun toString(): String = name
}