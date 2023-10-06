package org.simbrain.network.events

import java.awt.geom.Point2D

/**
 * @see Events2
 */
class ConnectorEvents: NetworkModelEvents() {
    val locationChanged = ChangedEvent<Point2D>()
    val showWeightsChanged = NoArgEvent()
    val lineUpdated = NoArgEvent()
}