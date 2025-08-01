package org.simbrain.world.imageworld

import org.simbrain.util.UserParameter
import org.simbrain.util.math.SimbrainMath
import org.simbrain.workspace.WorkspaceComponent
import org.simbrain.world.imageworld.filters.ImageProcessingPipeline
import org.simbrain.world.imageworld.transformations.ImagePipelineCollection
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
class ImageWorld : WorkspaceComponent("Image World") {

    /**
     * The current image.
     */
    @UserParameter(label = "Image URL")
    var imageSourceName: String = ""
        set(value) {
            field = value
            if (value.isNotEmpty()) {
                imageAlbum.loadImage(value)
            }
        }

    /**
     * The image album that manages multiple images.
     */
    val imageAlbum = ImageAlbum()

    val imagePipelineCollection = ImagePipelineCollection(imageAlbum)

    init {
        if (imageSourceName.isNotEmpty()) {
            imageAlbum.loadImage(imageSourceName)
        }
    }

    /**
     * Initialize the source images.
     *
     * @param simImages images to read and display
     */
    fun initializeImages(vararg simImages: String) {
        for (image in simImages) {
            imageAlbum.loadImage(image)
        }
    }

    /**
     * Initialize the source images from a provided list.
     *
     * @param images images to read and display
     */
    fun initializeImages(images: List<String>) {
        for (image in images) {
            imageAlbum.loadImage(image)
        }
    }

    /**
     * Convenience method to get current pipeline.
     */
    val currentPipeline: ImageProcessingPipeline get() = imagePipelineCollection.currentPipeline

    /**
     * Convenience method to set current pipeline on collection.
     */
    fun setCurrentPipeline(name: String) {
        imagePipelineCollection.pipelines
            .find { it.name == name }
            ?.let { imagePipelineCollection.setCurrentPipeline(it) }
    }

    /**
     * Convenience method to get current image.
     */
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

    // Legacy methods for backward compatibility
    fun resetImageAlbum(width: Int, height: Int) {
        imageAlbum.reset(width, height)
    }

    val numImages: Int get() = imageAlbum.numFrames

    fun nextFrame() = imageAlbum.nextFrame()

    fun previousFrame() = imageAlbum.previousFrame()

    val currentImage: BufferedImage get() = currentBufferedImage

    fun loadImages(files: Array<java.io.File>) {
        imageAlbum.loadImages(files)
    }
}