package org.simbrain.world.imageworld

import org.simbrain.world.imageworld.events.ImageEvents
import java.awt.image.BufferedImage

/**
 * Produces BufferedImages periodically and notifies listeners when the image changes or is resized.
 * <br></br>
 * Image sources can be enabled or disabled. E.g. if a webcam is available it can enable its image source, and then
 * when it is turned off the image source can be disabled (however this is not currently used and has not been tested).
 *
 * Whenever the current image is updated, the adapter compares it to previous image and decides which events need to
 * be called.
 *
 * @author Tim Shea
 */
abstract class ImageSource {
    /**
     * Whether the source will update the image when updateImage
     * is invoked.
     */
    var isEnabled: Boolean = true

    /**
     * Image backing the source.
     */
    var currentImage: BufferedImage
        protected set

    /**
     * Handle Image source Events.
     */
    @Transient
    var events: ImageEvents = ImageEvents()
        private set

    /**
     * Construct a new ImageSourceAdapter with the specified currentImage.
     *
     * @param currentImage the image to provide from the source
     */
    constructor(currentImage: BufferedImage = BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB)) {
        events = ImageEvents()
        this.currentImage = currentImage
        events.imageUpdate.fireAsync()
    }

    /**
     * See [org.simbrain.workspace.serialization.WorkspaceComponentDeserializer]
     */
    fun readResolve(): Any {
        events = ImageEvents()
        return this
    }

    /**
     * Notify ImageSourceListeners that a new image is available.
     */
    suspend fun fireImageUpdate() {
        if (this.isEnabled) {
            events.imageUpdate.fire()
        }
    }

    /**
     * Set the current image on the source, and optionally fire an updaet event.
     */
    suspend fun setCurrentImage(image: BufferedImage, fireEvents: Boolean = true) {
        val resized = image.width != currentImage.width || image.height != currentImage.height
        currentImage = image
        if (fireEvents) {
            if (resized && this.isEnabled) {
                events.resize.fire()
            }
            fireImageUpdate()
        }
    }

    suspend fun clearCurrentImage() {
        setCurrentImage(BufferedImage(currentImage.width, currentImage.height, BufferedImage.TYPE_INT_RGB))
    }

    val width: Int
        get() = currentImage.width

    val height: Int
        get() = currentImage.height
}
