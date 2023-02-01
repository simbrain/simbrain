package org.simbrain.network.events

import java.awt.geom.Point2D

/**
 * @see Events2
 */
class ConnectorEvents2: NetworkModelEvents2() {
    val locationChanged = ChangedEvent<Point2D>()
    val lineUpdated = NoArgEvent()
}