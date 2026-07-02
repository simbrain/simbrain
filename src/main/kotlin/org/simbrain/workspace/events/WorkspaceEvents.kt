package org.simbrain.workspace.events

import org.simbrain.util.FlowEvents
import org.simbrain.workspace.WorkspaceComponent

/**
 * See [FlowEvents].
 */
class WorkspaceEvents: FlowEvents() {
    val workspaceCleared = NoArgEvent()
    val workspaceOpened = NoArgEvent()
    val componentAdded = AwaitableEvent<WorkspaceComponent>()
    val componentRemoved = OneArgEvent<WorkspaceComponent>()
}