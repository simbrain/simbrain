package org.simbrain.world.imageworld.events

import org.simbrain.util.Event
import org.simbrain.util.Events2
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

class ImageEvents2: Events2() {
    val imageUpdate = NoArgEvent()
    val resize = NoArgEvent()
}