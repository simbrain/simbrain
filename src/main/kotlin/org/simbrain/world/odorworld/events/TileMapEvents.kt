package org.simbrain.world.odorworld.events

import org.piccolo2d.nodes.PImage
import org.simbrain.util.FlowEvents

/**
 * See [FlowEvents].
 */
class TileMapEvents: FlowEvents() {
    val layersChanged = NoArgEvent()
    val layerImageChanged = AwaitableEvent<Pair<PImage?, PImage?>>()
    val mapSizeChanged = NoArgEvent()
}