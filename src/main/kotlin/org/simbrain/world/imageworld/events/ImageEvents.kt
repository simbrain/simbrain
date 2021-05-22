package org.simbrain.world.imageworld.events

import org.simbrain.util.Event
import java.beans.PropertyChangeSupport

/**
 * See [Event].
 */
class ImageEvents(val source : Any) : Event(PropertyChangeSupport(source)) {

    fun onImageUpdate(handler: Runnable) = "ImageUpdate".event(handler)
    fun fireImageUpdate() = "ImageUpdate"()

    fun onResize(handler: Runnable) = "Resize".event(handler)
    fun fireResize() = "Resize"()
}