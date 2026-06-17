package org.simbrain.network.events

import java.awt.geom.Point2D

/**
 * @see org.simbrain.util.FlowEvents
 */
class ConnectorEvents: NetworkModelEvents() {
    val locationChanged = ChangedEvent<Point2D>()

    /**
     * Fire if the 'show weights' option changes (i.e. weights are visible in GUI).
     */
    val showWeightsChanged = NoArgEvent()

    val colorPreferencesChanged = NoArgEvent()
}