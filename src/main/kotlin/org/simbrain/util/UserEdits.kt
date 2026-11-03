/**
 * Lets generic editors and trainers, which know nothing about workspaces, report that the user changed something
 * worth saving. The desktop connects this to its workspace; without a desktop a report does nothing.
 */
package org.simbrain.util

object UserEdits {

    @Volatile
    var onEdit: () -> Unit = {}

    @JvmStatic
    fun report() = onEdit()
}
