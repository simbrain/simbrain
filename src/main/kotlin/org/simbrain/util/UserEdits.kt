/**
 * Lets generic editors and dialogs, which know nothing about workspaces, report that the user changed something worth
 * saving. Code that holds its component should call [org.simbrain.workspace.WorkspaceComponent.markChanged] instead, so
 * that closing that component's window also offers to save. The desktop connects this to its workspace; without a
 * desktop a report does nothing.
 */
package org.simbrain.util

object UserEdits {

    @Volatile
    var onEdit: () -> Unit = {}

    @JvmStatic
    fun report() = onEdit()
}
