package org.simbrain.network.events

import org.simbrain.network.gui.NetworkSelectionManager
import org.simbrain.network.gui.nodes.ScreenElement
import org.simbrain.util.Event
import java.beans.PropertyChangeSupport

class NetworkSelectionEvent(selectionManager: NetworkSelectionManager) : Event(PropertyChangeSupport(selectionManager)) {
    fun onSelection(handler: (old: Set<ScreenElement>, new: Set<ScreenElement>) -> Unit) =
            "Selection".itemChangedEvent(handler)
    fun fireSelection(old: Set<ScreenElement>, new: Set<ScreenElement>) = "Selection"(old = old, new = new)

    fun onSourceSelection(handler: (old: Set<ScreenElement>, new: Set<ScreenElement>) -> Unit) =
            "SourceSelection".itemChangedEvent(handler)
    fun fireSourceSelection(old: Set<ScreenElement>, new: Set<ScreenElement>) = "SourceSelection"(old = old, new = new)
}