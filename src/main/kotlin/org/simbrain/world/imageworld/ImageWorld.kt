package org.simbrain.world.imageworld

import org.simbrain.world.imageworld.filters.Filter
import org.simbrain.world.imageworld.filters.FilterManager
import org.simbrain.world.imageworld.transformations.TransformationCollection
import java.awt.image.BufferedImage
import java.io.File

/**
 * At each update, apply the current transformation in a [TransformationCollection] to the current image in an [ImageAlbum]
 *
 * Display the result of the current transformation applied to the current image to the screen.
 */
class ImageWorld {

    /**
     * Contains the current image rendered here.
     */
    val imageAlbum = ImageAlbum()

    /**
     * List of transformations.
     */
    val transformationCollection = TransformationCollection(imageAlbum)

    /**
     * Manager for multiple image filters.
     */
    val filterManager = FilterManager()

    /**
     * Clear the image album and set the current image with a blank canvas of the indicated size.
     */
    fun resetImageAlbum(width: Int, height: Int) {
        imageAlbum.reset(width, height)
    }

    /**
     * Load images from an array.
     *
     * @param files array of images to load
     */
    fun loadImages(files: Array<File>) {
        imageAlbum.loadImages(files)
    }

    /**
     * Returns number of frames in the "album" associated with this component.
     */
    val numImages: Int get() = imageAlbum.numFrames

    /**
     * Update the image source to the next image.
     */
    fun nextFrame() {
        imageAlbum.nextFrame()
    }

    /**
     * Update the image source to the previous image.
     */
    fun previousFrame() {
        imageAlbum.previousFrame()
    }

    /**
     * Convenience method to get current transformation.
     */
    val currentTransformation: Filter? get() = transformationCollection.currentTransformation

    /**
     * Convenience method to set current transformation on collection.
     */
    fun setCurrentTransformation(name: String) {
        transformationCollection.transformations
            .find { it.name == name }
            ?.let { transformationCollection.setCurrentTransformation(it) }
    }

    /**
     * Convenience method to get current image.
     */
    val currentImage: BufferedImage get() = imageAlbum.currentImage

    // Legacy methods for backward compatibility
    @Deprecated("Use transformationCollection instead", ReplaceWith("transformationCollection"))
    val filterCollection get() = transformationCollection

    @Deprecated("Use currentTransformation instead", ReplaceWith("currentTransformation"))
    val currentFilter get() = currentTransformation

    @Deprecated("Use setCurrentTransformation instead", ReplaceWith("setCurrentTransformation(name)"))
    fun setCurrentFilter(name: String) = setCurrentTransformation(name)
}