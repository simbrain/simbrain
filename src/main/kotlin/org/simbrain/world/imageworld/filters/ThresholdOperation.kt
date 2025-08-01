package org.simbrain.world.imageworld.filters

import org.simbrain.util.UserParameter
import java.awt.image.BufferedImage

/**
 * An image operation that applies thresholding to convert images to binary (black/white).
 * Pixels above the threshold become white, pixels below become black.
 */
class ThresholdOperation(
    @UserParameter(label = "Threshold", description = "Threshold value (0.0 - 1.0)", order = 2)
    var threshold: Double = 0.5
) : ImageOperation() {

    init {
        name = "Threshold"
        // Clamp threshold to valid range
        this.threshold = maxOf(0.0, minOf(1.0, threshold))
    }

    override fun applyOperation(input: BufferedImage): BufferedImage {
        val width = input.width
        val height = input.height
        val output = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        
        // Convert threshold from 0.0-1.0 to 0-255
        val thresholdValue = (threshold * 255).toInt()

        for (y in 0 until height) {
            for (x in 0 until width) {
                val rgb = input.getRGB(x, y)
                
                // Extract RGB components
                val red = (rgb shr 16) and 0xFF
                val green = (rgb shr 8) and 0xFF  
                val blue = rgb and 0xFF
                
                // Calculate grayscale value using standard luminance formula
                val grayscale = (red * 0.299 + green * 0.587 + blue * 0.114).toInt()
                
                // Apply threshold: above threshold = white (255), below = black (0)
                val thresholdedValue = if (grayscale > thresholdValue) 255 else 0
                
                // Set all RGB components to the same value for grayscale output
                val outputRgb = (thresholdedValue shl 16) or (thresholdedValue shl 8) or thresholdedValue
                output.setRGB(x, y, outputRgb)
            }
        }

        return output
    }

    override fun copy(): ImageOperation {
        val copy = ThresholdOperation(threshold)
        copy.enabled = enabled
        copy.name = name
        return copy
    }
} 