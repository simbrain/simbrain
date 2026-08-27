package org.simbrain.workspace.couplings

import org.simbrain.util.FlowEvents
import org.simbrain.workspace.AttributeContainer

/**
 * See [FlowEvents]
 */
class CouplingEvents: FlowEvents() {

    val couplingAdded = OneArgEvent<Coupling>()
    val couplingRemoved = OneArgEvent<Coupling>()
    val couplingsRemoved = OneArgEvent<Iterable<Coupling>>()

    /**
     * A coupling was replaced in place with an edited transform chain; carries both instances so
     * holders of the old one, such as update actions, can swap. See [CouplingManager.setTransforms].
     */
    val couplingChanged = OneArgEvent<CouplingChange>()

    /**
     * Workspace-wide relay of [org.simbrain.workspace.events.WorkspaceComponentEvents.attributeContainerChanged],
     * so consumers of a coupling (e.g. plots) can react to changes on the producing side, such as label changes,
     * without subscribing to every component.
     */
    val attributeContainerChanged = OneArgEvent<AttributeContainer>()

    /**
     * A coupling's update threw. The update loop logs the first failure per coupling and continues
     * with the remaining couplings; this event fires on every failure so the GUI can surface them.
     */
    val couplingFailed = OneArgEvent<CouplingFailure>()

}

data class CouplingFailure(val coupling: Coupling, val cause: Throwable)

data class CouplingChange(val old: Coupling, val replacement: Coupling)