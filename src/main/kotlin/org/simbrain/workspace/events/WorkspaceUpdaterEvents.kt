package org.simbrain.workspace.events

import org.simbrain.util.FlowEvents


class WorkspaceUpdaterEvents() : FlowEvents() {

    val couplingsUpdates = NoArgEvent()
    val workspaceUpdated = NoArgEvent()
    val runStarted = NoArgAwaitableEvent()
    val runFinished = NoArgAwaitableEvent()

}