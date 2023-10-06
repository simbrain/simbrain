package org.simbrain.workspace.events

import org.simbrain.util.Events
import org.simbrain.workspace.AttributeContainer

/**
 * See [Events].
 */
class WorkspaceComponentEvents: Events() {
    val componentUpdated = NoArgEvent()
    val componentMinimized = AddedEvent<Boolean>()
    val guiToggled = NoArgEvent()
    val componentOnOffToggled = NoArgEvent()
    val componentClosing = NoArgEvent()
    val attributeContainerAdded = AddedEvent<AttributeContainer>()
    val attributeContainerRemoved = RemovedEvent<AttributeContainer>()
}