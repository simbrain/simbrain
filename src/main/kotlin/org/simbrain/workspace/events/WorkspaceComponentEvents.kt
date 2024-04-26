package org.simbrain.workspace.events

import org.simbrain.util.Events
import org.simbrain.workspace.AttributeContainer

/**
 * See [Events].
 */
class WorkspaceComponentEvents: Events() {
    val componentUpdated = NoArgEvent()
    val componentMinimized = OneArgEvent<Boolean>()
    val guiToggled = NoArgEvent()
    val componentOnOffToggled = NoArgEvent()
    val componentClosing = NoArgEvent()
    val attributeContainerAdded = OneArgEvent<AttributeContainer>()
    val attributeContainerRemoved = OneArgEvent<AttributeContainer>()
}