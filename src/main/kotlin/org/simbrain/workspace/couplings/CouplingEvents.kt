package org.simbrain.workspace.couplings

import org.simbrain.util.Events2

/**
 * See [Events2]
 */
class CouplingEvents2: Events2() {

    val couplingAdded = AddedEvent<Coupling>()
    val couplingRemoved = RemovedEvent<Coupling>()
    val couplingsRemoved = RemovedEvent<Iterable<Coupling>>()

}