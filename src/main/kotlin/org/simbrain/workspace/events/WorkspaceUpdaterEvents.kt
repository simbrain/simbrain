package org.simbrain.workspace.events

import org.simbrain.util.Events2


class WorkspaceUpdaterEvents() : Events2() {

    val couplingsUpdates = NoArgEvent()
    val workspaceUpdated = NoArgEvent()
    val runStarted = NoArgEvent()
    val runFinished = NoArgEvent()

}