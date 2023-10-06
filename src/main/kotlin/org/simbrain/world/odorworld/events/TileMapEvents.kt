package org.simbrain.world.odorworld.events

import org.piccolo2d.nodes.PImage
import org.simbrain.util.Events

/**
 * See [Events].
 */
class TileMapEvents: Events() {
    val layerAdded = NoArgEvent()
    val layerImageChanged = ChangedEvent<PImage?>()
    val mapSizeChanged = NoArgEvent()
}