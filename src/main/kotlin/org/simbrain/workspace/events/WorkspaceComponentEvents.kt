package org.simbrain.workspace.events

import org.simbrain.util.FlowEvents
import org.simbrain.workspace.AttributeContainer

/**
 * See [FlowEvents].
 */
class WorkspaceComponentEvents: FlowEvents() {
    val componentUpdated = NoArgEvent()
    val componentMinimized = OneArgEvent<Boolean>()
    val guiToggled = NoArgEvent()
    val componentOnOffToggled = NoArgEvent()
    val componentClosing = NoArgEvent()
    val attributeContainerAdded = AwaitableEvent<AttributeContainer>()
    val attributeContainerRemoved = OneArgEvent<AttributeContainer>()
    val attributeContainerChanged = OneArgEvent<AttributeContainer>()
}