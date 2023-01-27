package org.simbrain.workspace.couplings

import org.simbrain.util.Event
import org.simbrain.util.Events2
import java.beans.PropertyChangeSupport
import java.util.function.Consumer

/**
 * Attribute events are handled in [WorkspaceComponentEvents]
 */
class CouplingEvents(couplingManager: CouplingManager) : Event(PropertyChangeSupport(couplingManager)) {

    fun onCouplingAdded(handler: Consumer<Coupling>) = "CouplingAdded".itemAddedEvent(handler)
    fun fireCouplingAdded(coupling: Coupling) = "CouplingAdded"(new = coupling)

    fun onCouplingRemoved(handler: Consumer<Coupling>) = "CouplingRemoved".itemRemovedEvent(handler)
    fun fireCouplingRemoved(coupling: Coupling) = "CouplingRemoved"(old = coupling)

    fun onCouplingsRemoved(handler: Consumer<Iterable<Coupling>>) = "CouplingsRemoved".itemRemovedEvent(handler)
    fun fireCouplingsRemoved(couplings: Iterable<Coupling>) = "CouplingsRemoved"(old = couplings)

}

class CouplingEvents2: Events2() {

    val couplingAdded = AddedEvent<Coupling>()
    val couplingRemoved = RemovedEvent<Coupling>()
    val couplingsRemoved = RemovedEvent<Iterable<Coupling>>()

}