package org.simbrain.workspace.events

import org.simbrain.util.Event
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.WorkspaceComponent
import java.beans.PropertyChangeSupport
import java.util.function.Consumer

/**
 * Class for listeners on a workspace.
 *
 * @author Matt Watson
 * @author Yulin Li
 */
class WorkspaceEvents(workspace: Workspace) : Event(PropertyChangeSupport(workspace)) {

    fun onWorkspaceCleared(handler: Runnable) = "WorkspaceCleared".event(handler)
    fun fireWorkspaceCleared() = "WorkspaceCleared"()

    fun onNewWorkspaceOpened(handler: Runnable) = "NewWorkspaceOpened".event(handler)
    fun fireNewWorkspaceOpened() = "NewWorkspaceOpened"()

    fun onComponentAdded(handler: Consumer<WorkspaceComponent>) = "ComponentAdded".itemAddedEvent(handler)
    fun fireComponentAdded(component: WorkspaceComponent) = "ComponentAdded"(new = component)

    fun onComponentRemoved(handler: Consumer<WorkspaceComponent>) = "ComponentRemoved".itemRemovedEvent(handler)
    fun fireComponentRemoved(component: WorkspaceComponent) = "ComponentRemoved"(old = component)
    
}