package org.simbrain.workspace.couplings

import org.simbrain.util.FlowEvents

/**
 * See [FlowEvents]
 */
class CouplingEvents: FlowEvents() {

    val couplingAdded = OneArgEvent<Coupling>()
    val couplingRemoved = OneArgEvent<Coupling>()
    val couplingsRemoved = OneArgEvent<Iterable<Coupling>>()

}