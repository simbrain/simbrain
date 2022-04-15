package org.simbrain.workspace.updater

import org.simbrain.workspace.WorkspaceComponent

/**
 * Update a specific workspace component.
 *
 * @author jyoshimi
 */
class UpdateComponent(
    val component: WorkspaceComponent
) : UpdateAction("Update ${component.name}") {
    override suspend fun run() {
        component.update()
    }
}