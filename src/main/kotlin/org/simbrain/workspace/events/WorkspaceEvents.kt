package org.simbrain.workspace.events

import org.simbrain.util.Events
import org.simbrain.workspace.WorkspaceComponent

/**
 * See [Events].
 */
class WorkspaceEvents: Events() {
    val workspaceCleared = NoArgEvent()
    val workspaceOpened = NoArgEvent()
    val componentAdded = OneArgEvent<WorkspaceComponent>()
    val componentRemoved = OneArgEvent<WorkspaceComponent>()
}