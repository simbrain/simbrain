package org.simbrain.util

/**
 * Executable actions and a description for those actions. Used by [org.simbrain.workspace.updater.WorkspaceUpdater] and by [org.simbrain.network.core.NetworkUpdateManager]
 */
abstract class UpdateAction(

    open val description: String?,

    /**
     * A longer description for tooltips, etc.
     */
    open val longDescription: String? = description

) {
    abstract suspend fun run()
}

inline fun updateAction(description: String? = null, longDescription: String? = description, crossinline action: suspend () -> Unit): UpdateAction {
    return object : UpdateAction(description, longDescription) {
        override suspend fun run() {
            action()
        }
    }
}

@JvmOverloads
fun create(description: String? = null, longDescription: String? = description, action: Runnable): UpdateAction {
    return object : UpdateAction(description, longDescription) {
        override suspend fun run() {
            action.run()
        }
    }
}

suspend operator fun UpdateAction.invoke() {
    run()
}