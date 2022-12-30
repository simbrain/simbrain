package org.simbrain.world.odorworld.events

import org.piccolo2d.nodes.PImage
import org.simbrain.util.Event
import org.simbrain.util.piccolo.TileMap
import java.beans.PropertyChangeSupport
import java.util.function.BiConsumer

/**
 * See [Event].
 */
class TileMapEvents(val tileMap: TileMap):Event(PropertyChangeSupport(tileMap)) {

    fun onLayerAdded(handler: Runnable) = "LayerAdded".event(handler)
    fun fireLayerAdded() = "LayerAdded"()

    fun onLayerImageChanged(handler: BiConsumer<PImage?, PImage?>) = "LayerImageChanged".itemChangedEvent(handler)
    fun fireLayerImageChanged(old: PImage?, new: PImage?) = "LayerImageChanged"(old = old, new = new)

}