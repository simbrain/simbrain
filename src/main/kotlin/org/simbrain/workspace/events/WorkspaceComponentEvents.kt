package org.simbrain.workspace.events

import org.simbrain.util.Events2
import org.simbrain.workspace.AttributeContainer

/**
 * See [Events2].
 */
class WorkspaceComponentEvents2: Events2() {
    val componentUpdated = NoArgEvent()
    val guiToggled = NoArgEvent()
    val componentOnOffToggled = NoArgEvent()
    val componentClosing = NoArgEvent()
    val attributeContainerAdded = AddedEvent<AttributeContainer>()
    val attributeContainerRemoved = RemovedEvent<AttributeContainer>()
}