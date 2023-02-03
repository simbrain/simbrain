package org.simbrain.world.odorworld.events

import org.piccolo2d.nodes.PImage
import org.simbrain.util.Events2

/**
 * See [Events2].
 */
class TileMapEvents2: Events2() {
    val layerAdded = NoArgEvent()
    val layerImageChanged = ChangedEvent<PImage?>()
}