package org.simbrain.workspace.couplings

import org.simbrain.util.Events

/**
 * See [Events]
 */
class CouplingEvents: Events() {

    val couplingAdded = OneArgEvent<Coupling>()
    val couplingRemoved = OneArgEvent<Coupling>()
    val couplingsRemoved = OneArgEvent<Iterable<Coupling>>()

}