package org.simbrain.world.imageworld.filters

import java.awt.image.BufferedImage

/**
 * Base class for image processing filters that can be applied to BufferedImages.
 * Filters perform pixel-level processing operations that can be chained together.
 */
abstract class ImageFilter : ImageOperation() {

    /**
     * Implement the actual filtering logic in subclasses.
     * This method should create a new BufferedImage with the filter applied.
     */
    protected abstract fun applyFilter(input: BufferedImage): BufferedImage

    /**
     * Implementation of the ImageOperation interface - delegates to applyFilter
     */
    final override fun applyOperation(input: BufferedImage): BufferedImage {
        return applyFilter(input)
    }

    /**
     * Create a copy of this filter with the same settings.
     */
    abstract override fun copy(): ImageFilter
}