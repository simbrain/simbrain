package org.simbrain.workspace.updater

import org.simbrain.util.UpdateAction
import org.simbrain.workspace.WorkspaceComponent

/**
 * Update a specific workspace component.
 */
class UpdateComponent(
    val component: WorkspaceComponent
) : UpdateAction(
    "Update ${component.name}",
    "An action that updates ${component.name}"
) {
    override suspend fun run() {
        component.update()
    }
}