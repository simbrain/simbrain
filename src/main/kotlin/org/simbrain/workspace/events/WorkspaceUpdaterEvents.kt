package org.simbrain.workspace.events

import org.simbrain.util.Events


class WorkspaceUpdaterEvents() : Events() {

    val couplingsUpdates = NoArgEvent()
    val workspaceUpdated = NoArgEvent()
    val runStarted = NoArgEvent()
    val runFinished = NoArgEvent()

}