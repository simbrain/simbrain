package org.simbrain.world.imageworld.events

import org.simbrain.util.Event
import org.simbrain.world.imageworld.ImageSource
import java.beans.PropertyChangeSupport
import java.util.function.Consumer

/**
 * See [Event].
 */
class ImageSourceEvents(val source : ImageSource) : Event(PropertyChangeSupport(source)) {

    fun onImageUpdate(handler: Consumer<ImageSource>) = "ImageUpdate".itemAddedEvent(handler)
    fun fireImageUpdate(source: ImageSource) = "ImageUpdate"(new =source)

    fun onImageResize(handler: Consumer<ImageSource>) = "ImageResize".itemAddedEvent(handler)
    fun fireImageResize(source: ImageSource) = "ImageResize"(new =source)
}