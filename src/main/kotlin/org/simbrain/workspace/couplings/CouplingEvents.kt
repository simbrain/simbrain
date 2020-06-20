package org.simbrain.workspace.couplings

import org.simbrain.util.Event
import org.simbrain.workspace.couplings.Coupling
import org.simbrain.workspace.couplings.CouplingManager
import java.beans.PropertyChangeSupport
import java.util.function.Consumer

class CouplingEvents(couplingManager: CouplingManager) : Event(PropertyChangeSupport(couplingManager)) {

    fun onCouplingAdded(handler: Consumer<Coupling>) = "CouplingAdded".itemAddedEvent(handler)
    fun fireCouplingAdded(coupling: Coupling) = "CouplingAdded"(new = coupling)

    fun onCouplingRemoved(handler: Consumer<Coupling>) = "CouplingRemoved".itemRemovedEvent(handler)
    fun fireCouplingRemoved(coupling: Coupling) = "CouplingRemoved"(old = coupling)

    fun onCouplingsRemoved(handler: Consumer<List<Coupling>>) = "CouplingsRemoved".itemRemovedEvent(handler)
    fun fireCouplingsRemoved(couplings: List<Coupling>) = "CouplingsRemoved"(old = couplings)

}