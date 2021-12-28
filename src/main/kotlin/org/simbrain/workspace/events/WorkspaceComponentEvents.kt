package org.simbrain.workspace.events

import org.simbrain.util.Event
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.WorkspaceComponent
import java.beans.PropertyChangeSupport
import java.util.function.Consumer

/**
 * Events relating to workspace components.
 */
class WorkspaceComponentEvents(wc: WorkspaceComponent) : Event(PropertyChangeSupport(wc)) {

    fun onComponentUpdated(handler: Runnable) = "ComponentUpdated".event(handler)
    fun fireComponentUpdated() = "ComponentUpdated"()

    fun onGUIToggled(handler: Runnable) = "GUIToggled".event(handler)
    fun fireGUIToggled() = "GUIToggled"()

    fun onComponentOnOffToggled(handler: Runnable) = "ComponentOnOffToggled".event(handler)
    fun fireComponentOnOffToggled() = "ComponentOnOffToggled"()

    fun onComponentClosing(handler: Runnable) = "ComponentClosing".event(handler)
    fun fireComponentClosing() = "ComponentClosing"()

    fun onAttributeContainerAdded(handler: Consumer<AttributeContainer>) = "AttributeContainerAdded".itemAddedEvent(handler)
    fun fireAttributeContainerAdded(ac: AttributeContainer) = "AttributeContainerAdded"(new = ac)

    fun onAttributeContainerRemoved(handler: Consumer<AttributeContainer>) = "AttributeContainerRemoved".itemRemovedEvent(handler)
    fun fireAttributeContainerRemoved(ac: AttributeContainer) = "AttributeContainerRemoved"(old = ac)
    
}