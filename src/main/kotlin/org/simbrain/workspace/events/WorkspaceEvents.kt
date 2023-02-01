package org.simbrain.workspace.events

import org.simbrain.util.Events2
import org.simbrain.workspace.WorkspaceComponent

/**
 * See [Events2].
 */
class WorkspaceEvents2: Events2() {
    val workspaceCleared = NoArgEvent()
    val workspaceOpened = NoArgEvent()
    val componentAdded = AddedEvent<WorkspaceComponent>()
    val componentRemoved = RemovedEvent<WorkspaceComponent>()
}