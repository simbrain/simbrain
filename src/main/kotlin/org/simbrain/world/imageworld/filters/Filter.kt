package org.simbrain.world.imageworld.filters

import org.simbrain.util.CachedObject
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Producible
import org.simbrain.world.imageworld.ImageSource
import java.awt.RenderingHints.*
import java.awt.image.BufferedImage

/**
 * Wraps an [ImageOperation] in a structure that allows for coupling, event handling etc.
 *
 * Arrays tracking int rgb colors and doubles for
 * brightness, red, green, and blue separately are maintained and can serve
 * as producers for couplings.
 * <br></br>
 * The actual filtering happens in the [org.simbrain.world.imageworld.filters]
 * package. Filters do the work of allowing the filtered images to
 * couple to something else. This makes sense biologically: retinal patterns
 * are what neurons "sense".
 *
 * @param source An ImageSource from which to extract filter values.  For "image world" this will be a [java.awt.image.FilteredImageSource], which applies the relevant downscaling, thresholding, and other operations.
 *
 * @author Yulin Li
 * @author Tim Shea
 * @author Jeff Yoshimi
 */
class Filter(
    @UserParameter(label = "Name") override var id: String,
    val source: ImageSource,

    @UserParameter(label = "Filter", order = 3)
    var imageOp: ImageOperation<*>,

    width: Int,

    height: Int,
) : AttributeContainer, EditableObject {

    @UserParameter(label = "Width", order = 1)
    var width: Int = width
        set(value) {
            field = value
            filteredImageCache.invalidate()
        }

    @UserParameter(label = "Height", order = 2)
    var height: Int = height
        set(value) {
            field = value
            filteredImageCache.invalidate()
        }

    /**
     * The filtered image that can be displayed in the desktop.
     */
    @Transient
    private var filteredImageCache: CachedObject<BufferedImage> = CachedObject {
        createFilteredImage()
    }

    var filteredImage by filteredImageCache::value

    /**
     * Array of ints representing rgb colors. See [BufferedImage.getRGB]
     */
    @get:Producible
    val rGBColor: IntArray get() = filteredImage.getRGB(0, 0, width, height, null, 0, width)

    init {
        applyFilter()
    }

    /**
     * See [org.simbrain.workspace.serialization.WorkspaceComponentDeserializer]
     */
    fun readResolve(): Any {
        // Reinitialize the transient cache after deserialization
        filteredImageCache = CachedObject { createFilteredImage() }
        applyFilter()
        return this
    }

    @get:Producible
    val brightness: DoubleArray
        get() = filteredImage.getRGB(0, 0, width, height, null, 0, width).map { color ->
            val red = ((color ushr 16) and 0xFF) / 255.0
            val green = ((color ushr 8) and 0xFF) / 255.0
            val blue = (color and 0xFF) / 255.0
            // Cf. https://en.wikipedia.org/wiki/Luma_(video)
            red * 0.2126 + green * 0.7152 + blue * 0.0722
        }.toDoubleArray()

    @get:Producible(defaultVisibility = false)
    val red: DoubleArray
        get() = filteredImage.getRGB(0, 0, width, height, null, 0, width).map { color ->
            ((color ushr 16) and 0xFF) / 255.0
        }.toDoubleArray()

    @get:Producible(defaultVisibility = false)
    val green: DoubleArray
        get() = filteredImage.getRGB(0, 0, width, height, null, 0, width).map { color ->
            ((color ushr 8) and 0xFF) / 255.0
        }.toDoubleArray()

    @get:Producible(defaultVisibility = false)
    val blue: DoubleArray
        get() = filteredImage.getRGB(0, 0, width, height, null, 0, width).map { color ->
            (color and 0xFF) / 255.0
        }.toDoubleArray()

    override fun toString() = this.id

    fun applyFilter() {
        filteredImage = createFilteredImage()
    }

    private fun createFilteredImage(): BufferedImage {
        val sourceImage = source.currentImage

        // to guarantee the values won't change throughout the function call
        val targetWidth = width
        val targetHeight = height
        
        // If dimensions match exactly, no scaling needed
        if (sourceImage.width == targetWidth && sourceImage.height == targetHeight) {
            return imageOp.getOp().filter(sourceImage, null)
        }
        
        // Scale the image to exact target dimensions
        val scaledImage = BufferedImage(targetWidth, targetHeight, sourceImage.type)
        val graphics = scaledImage.createGraphics()
        
        try {
            // Use high-quality scaling
            graphics.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR)
            graphics.setRenderingHint(KEY_RENDERING, VALUE_RENDER_QUALITY)
            graphics.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON)
            
            // Draw the scaled image
            graphics.drawImage(sourceImage, 0, 0, targetWidth, targetHeight, null)
        } finally {
            graphics.dispose()
        }
        
        // Verify the scaled image has the exact dimensions we want
        if (scaledImage.width != targetWidth || scaledImage.height != targetHeight) {
            throw AssertionError(
                "Scaled image dimensions (${scaledImage.width} x ${scaledImage.height}) " +
                "do not match target dimensions ($targetWidth x $targetHeight)"
            )
        }
        
        // Apply the filter operation to the SCALED image, not the original
        return imageOp.getOp().filter(scaledImage, null)
    }

    override val name get() = id
}