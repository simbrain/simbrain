/**
 * Lets generic editors and dialogs, which know nothing about workspaces, mark that the workspace has unsaved changes,
 * so that closing or quitting offers to save. Code that holds its component should call
 * [org.simbrain.workspace.WorkspaceComponent.markChanged] instead, so that closing that component's window also offers
 * to save. The desktop connects this to its workspace; without a desktop, marking does nothing.
 */
package org.simbrain.util

object UnsavedChanges {

    @Volatile
    var onMark: () -> Unit = {}

    @JvmStatic
    fun mark() = onMark()
}
