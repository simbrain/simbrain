package org.simbrain.world.imageworld.filters

import org.simbrain.util.CachedObject
import org.simbrain.util.FlowEvents
import org.simbrain.util.UserParameter
import org.simbrain.util.getBrightnessArray
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Producible
import org.simbrain.world.imageworld.ImageSource
import java.awt.image.BufferedImage

/**
 * A pipeline of image processing operations that can be applied in sequence.
 * Contains an ordered list of ImageOperation objects (transformations and filters)
 * that are applied to an input image in order.
 */
class ImageProcessingPipeline(
    @UserParameter(label = "Name") override var name: String,
    val source: ImageSource
) : AttributeContainer, EditableObject {

    override var id: String
        get() = name
        set(value) { name = value }

    @UserParameter(label = "Operations", displayOnly = true)
    private val operations = mutableListOf<ImageOperation>()

    /**
     * The processed image cache that can be displayed in the desktop.
     */
    @Transient
    private var processedImageCache: CachedObject<BufferedImage> = CachedObject {
        createProcessedImage()
    }

    var processedImage by processedImageCache::value

    /**
     * Events fired when the pipeline changes
     */
    @Transient
    val events = PipelineEvents()

    class PipelineEvents : FlowEvents() {
        val operationAdded = OneArgEvent<ImageOperation>()
        val operationRemoved = OneArgEvent<ImageOperation>()
        val operationOrderChanged = NoArgEvent()
    }

    init {
        applyPipeline()
    }

    /**
     * See [org.simbrain.workspace.serialization.WorkspaceComponentDeserializer]
     */
    fun readResolve(): Any {
        // Reinitialize the transient cache after deserialization
        processedImageCache = CachedObject { createProcessedImage() }
        applyPipeline()
        return this
    }

    /**
     * Get a read-only view of the current operations
     */
    fun getOperations(): List<ImageOperation> = operations.toList()

    /**
     * Add an operation to the end of the pipeline
     */
    fun addOperation(operation: ImageOperation) {
        operations.add(operation)
        processedImageCache.invalidate()
        events.operationAdded.fire(operation)
    }

    /**
     * Insert an operation at a specific index
     */
    fun insertOperation(index: Int, operation: ImageOperation) {
        operations.add(index, operation)
        processedImageCache.invalidate()
        events.operationOrderChanged.fire()
    }

    /**
     * Remove an operation from the pipeline
     */
    fun removeOperation(operation: ImageOperation) {
        if (operations.remove(operation)) {
            processedImageCache.invalidate()
            events.operationRemoved.fire(operation)
        }
    }

    /**
     * Move an operation up in the pipeline (towards the beginning)
     */
    fun moveOperationUp(operation: ImageOperation) {
        val index = operations.indexOf(operation)
        if (index > 0) {
            operations.removeAt(index)
            operations.add(index - 1, operation)
            processedImageCache.invalidate()
            events.operationOrderChanged.fire()
        }
    }

    /**
     * Move an operation down in the pipeline (towards the end)
     */
    fun moveOperationDown(operation: ImageOperation) {
        val index = operations.indexOf(operation)
        if (index >= 0 && index < operations.size - 1) {
            operations.removeAt(index)
            operations.add(index + 1, operation)
            processedImageCache.invalidate()
            events.operationOrderChanged.fire()
        }
    }

    /**
     * Clear all operations from the pipeline
     */
    fun clearOperations() {
        val removedOps = operations.toList()
        operations.clear()
        processedImageCache.invalidate()
        removedOps.forEach { events.operationRemoved.fire(it) }
    }

    /**
     * Apply the pipeline and refresh the processed image
     */
    fun applyPipeline() {
        processedImage = createProcessedImage()
    }

    /**
     * Create the processed image by applying all operations in sequence
     */
    private fun createProcessedImage(): BufferedImage {
        var currentImage = source.currentImage
        
        // Apply each operation in sequence
        for (operation in operations) {
            currentImage = operation.apply(currentImage)
        }
        
        return currentImage
    }

    // Producer methods for coupling to other components

    /**
     * Array of ints representing rgb colors. See [BufferedImage.getRGB]
     */
    @get:Producible(defaultVisibility = false)
    val rGBColor: IntArray get() = processedImage.getRGB(0, 0, processedImage.width, processedImage.height, null, 0, processedImage.width)

    @get:Producible(defaultVisibility = false)
    val brightness: DoubleArray get() = processedImage.getBrightnessArray()

    @get:Producible(defaultVisibility = false)
    val red: DoubleArray
        get() = processedImage.getRGB(0, 0, processedImage.width, processedImage.height, null, 0, processedImage.width).map { color ->
            ((color ushr 16) and 0xFF) / 255.0
        }.toDoubleArray()

    @get:Producible(defaultVisibility = false)
    val green: DoubleArray
        get() = processedImage.getRGB(0, 0, processedImage.width, processedImage.height, null, 0, processedImage.width).map { color ->
            ((color ushr 8) and 0xFF) / 255.0
        }.toDoubleArray()

    @get:Producible(defaultVisibility = false)
    val blue: DoubleArray
        get() = processedImage.getRGB(0, 0, processedImage.width, processedImage.height, null, 0, processedImage.width).map { color ->
            (color and 0xFF) / 255.0
        }.toDoubleArray()

    /**
     * Interleaved RGB activations in HWC order: [r₀₀, g₀₀, b₀₀, r₀₁, g₀₁, b₀₁, ...].
     * Pre-allocated backing field avoids per-iteration allocation.
     */
    @Transient
    private var _rgbActivations: DoubleArray? = null

    @get:Producible(defaultVisibility = false)
    val rgbActivations: DoubleArray
        get() {
            val img = processedImage
            val size = img.width * img.height
            val buf = _rgbActivations?.takeIf { it.size == size * 3 }
                ?: DoubleArray(size * 3).also { _rgbActivations = it }
            val pixels = img.getRGB(0, 0, img.width, img.height, null, 0, img.width)
            for (i in pixels.indices) {
                val p = pixels[i]
                buf[i * 3]     = ((p ushr 16) and 0xFF) / 255.0
                buf[i * 3 + 1] = ((p ushr 8) and 0xFF) / 255.0
                buf[i * 3 + 2] = (p and 0xFF) / 255.0
            }
            return buf
        }

    override fun toString() = name
} 