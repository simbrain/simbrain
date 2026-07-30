package org.simbrain.network.events

class SubnetworkEvents: LocationEvents() {
    val customInfoUpdated = NoArgEvent()

    /**
     * Fired when a subnetwork switches between alternate ways of drawing itself, such as a BPTT
     * network toggling its unrolled-over-time picture.
     */
    val displayModeChanged = NoArgEvent()
}