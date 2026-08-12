package org.simbrain.network.events

class SubnetworkEvents: LocationEvents() {
    val customInfoUpdated = NoArgEvent()

    /**
     * Fired when a subnetwork switches between alternate ways of drawing itself, such as a BPTT
     * network toggling its unrolled-over-time picture.
     */
    val displayModeChanged = NoArgEvent()

    /**
     * Fired when a subnetwork has fresh data for an alternate view to draw, such as the per-timestep
     * activations of a BPTT network's unrolled columns. Throttled because it can fire once per training
     * window, far faster than the canvas needs repainting.
     */
    val displayDataUpdated = NoArgEvent(interval = 50, timingMode = TimingMode.Throttle)
}