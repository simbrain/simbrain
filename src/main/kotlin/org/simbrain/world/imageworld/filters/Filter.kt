package org.simbrain.world.imageworld.filters

import org.simbrain.util.CachedObject
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Producible
import org.simbrain.world.imageworld.ImageSource
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
    private val imageOp: ImageOperation<*>,

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
        val scaleX = computeScalingFactor(source.width, width)
        val scaleY = computeScalingFactor(source.height, height)
        val scaleOp = FilterUtils.createScaleOp(scaleX, scaleY, true)
        var image = source.currentImage
        image = scaleOp.filter(image, null)
        return imageOp.getOp().filter(image, null).also {
            if (it.height != height || it.width != width) {
                throw AssertionError(
                    String.format(
                        "Filtered image size not equal to filter size (filtered image size: %d x %d, filter size: %d x %d)", it.width,
                        it.height, width, height
                    )
                )
            }
        }
    }

    private fun computeScalingFactor(source: Int, target: Int): Float {
        if (source == target) {
            return 1f
        }
        // Subtract 0.1 from width and height to avoid exceeding the specified dimension due to floating point error.
        return (target - 0.1f) / source
    }

}