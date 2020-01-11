package org.simbrain.network.events

interface LocationEvents {

    fun onLocationChange(handler: Runnable)
    fun fireLocationChange()

}