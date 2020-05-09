package org.simbrain.network.events

import org.simbrain.network.gui.NetworkSelectionManager
import org.simbrain.network.gui.nodes.ScreenElement
import org.simbrain.util.Event
import java.beans.PropertyChangeSupport

class NetworkSelectionEvent(selectionManager: NetworkSelectionManager) : Event(PropertyChangeSupport(selectionManager)) {
    fun onSelection(handler: (Set<ScreenElement>, Set<ScreenElement>) -> Unit) = "Selection".itemChangedEvent(handler)
    fun fireSelection(old: Set<ScreenElement>, new: Set<ScreenElement>) = "Selection"(old = old, new = new)
}