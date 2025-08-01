package org.simbrain.world.imageworld.filters

import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.CopyableObject
import java.awt.RenderingHints.*
import java.awt.image.BufferedImage

/**
 * An image operation that resizes images to specified dimensions.
 */
class ResizeOperation(
    @UserParameter(label = "Width", description = "Target width in pixels", order = 2)
    var width: Int = 100,
    
    @UserParameter(label = "Height", description = "Target height in pixels", order = 3)
    var height: Int = 100
) : ImageOperation() {

    init {
        name = "Resize ${width}x${height}"
    }

    override fun applyOperation(input: BufferedImage): BufferedImage {
        // If dimensions match exactly, no scaling needed
        if (input.width == width && input.height == height) {
            return input
        }
        
        // Scale the image to exact target dimensions
        val scaledImage = BufferedImage(width, height, input.type)
        val graphics = scaledImage.createGraphics()
        
        try {
            // Use high-quality scaling
            graphics.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR)
            graphics.setRenderingHint(KEY_RENDERING, VALUE_RENDER_QUALITY)
            graphics.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON)
            
            // Draw the scaled image
            graphics.drawImage(input, 0, 0, width, height, null)
        } finally {
            graphics.dispose()
        }
        
        return scaledImage
    }

    override fun copy(): ImageOperation {
        val copy = ResizeOperation(width, height)
        copy.enabled = enabled
        copy.name = name
        return copy
    }

    override fun getTypeList(): List<Class<out CopyableObject>>? {
        return listOf(
            ResizeOperation::class.java,
            GrayscaleOperation::class.java,
            ThresholdOperation::class.java,
            EdgeDetectionFilter::class.java,
            GaborFilter::class.java
        )
    }
} 