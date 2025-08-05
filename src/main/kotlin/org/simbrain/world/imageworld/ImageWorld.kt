package org.simbrain.world.imageworld

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.simbrain.util.UserParameter
import org.simbrain.util.math.SimbrainMath
import org.simbrain.workspace.WorkspaceComponent
import org.simbrain.world.imageworld.filters.ImageProcessingPipeline
import org.simbrain.world.imageworld.transformations.ImagePipelineCollection
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.OutputStream

/**
 * Image World. Displays an image and (hopefully) allows it to provide a kind
 * of "retina" for neural networks. Images are loaded and filtered (currently
 * down-sampled and thresholded). The filtered image then provides arrays that
 * can be coupled to a neural network.
 *
 * @author Jeff Yoshimi
 * @author Tim Shea
 */
class ImageWorld : WorkspaceComponent("Image World"), CoroutineScope {

    var penColor: Color = Color.white

    var penSize: Int = 1

    var useSmoothing: Boolean = false

    /**
     * The quality level of smoothing (LOW, MEDIUM, HIGH)
     */
    enum class SmoothingQuality {
        LOW, MEDIUM, HIGH;

        override fun toString(): String {
            return name.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    /**
     * The available brush shapes
     */
    enum class BrushShape {
        CIRCLE, SQUARE, SOFT;

        override fun toString(): String {
            return name.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    var smoothingQuality: SmoothingQuality = SmoothingQuality.HIGH
    var brushShape: BrushShape = BrushShape.CIRCLE


    @Transient
    private val job = SupervisorJob()

    @Transient
    override val coroutineContext = Dispatchers.Default + job

    /**
     * The current image.
     */
    @UserParameter(label = "Image URL")
    var imageSourceName: String = ""
        set(value) {
            field = value
            if (value.isNotEmpty()) {
                launch { imageAlbum.loadImage(value) }
            }
        }

    /**
     * The image album that manages multiple images.
     */
    val imageAlbum = ImageAlbum()

    val imagePipelineCollection = ImagePipelineCollection(imageAlbum)

    init {
        if (imageSourceName.isNotEmpty()) {
            launch { imageAlbum.loadImage(imageSourceName) }
        }
    }

    val currentPipeline: ImageProcessingPipeline get() = imagePipelineCollection.currentPipeline

    suspend fun setCurrentPipeline(name: String) {
        imagePipelineCollection.pipelines
            .find { it.name == name }
            ?.let { imagePipelineCollection.setCurrentPipeline(it) }
    }

    val currentBufferedImage: BufferedImage get() = imagePipelineCollection.currentPipeline.processedImage

    /**
     * Rounds the values before sending them out as a string.
     */
    override fun toString(): String {
        return SimbrainMath.roundDouble(imagePipelineCollection.currentPipeline.brightness.first(), 2).toString()
    }

    override fun save(output: OutputStream, format: String?) {
        // TODO: Implement save functionality
    }

    suspend fun resetImageAlbum(width: Int, height: Int) {
        imageAlbum.reset(width, height)
    }

    val numImages: Int get() = imageAlbum.numFrames

    suspend fun nextFrame() = imageAlbum.nextFrame()

    suspend fun previousFrame() = imageAlbum.previousFrame()

    val currentImage: BufferedImage get() = currentBufferedImage

    suspend fun loadImages(files: Array<java.io.File>) {
        imageAlbum.loadImages(files)
    }
}