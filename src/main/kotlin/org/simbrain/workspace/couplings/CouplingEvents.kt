package org.simbrain.workspace.couplings

import org.simbrain.util.Events

/**
 * See [Events]
 */
class CouplingEvents: Events() {

    val couplingAdded = AddedEvent<Coupling>()
    val couplingRemoved = RemovedEvent<Coupling>()
    val couplingsRemoved = RemovedEvent<Iterable<Coupling>>()

}