package org.simbrain.network.events

import org.simbrain.network.dl4j.MultiLayerNet
import org.simbrain.util.Event
import java.beans.PropertyChangeSupport

class MultiLayerNetEvents(net: MultiLayerNet) : Event(PropertyChangeSupport(net)), LocationEvents {

    override fun onLocationChange(handler: Runnable) = "LocationChange".event(handler)
    override fun fireLocationChange() = "LocationChange"()

}