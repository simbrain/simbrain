package org.simbrain.workspace.updater

/**
 * Classes that implement this interface describe individual actions that
 * together comprise a workspace update.
 *
 * @author jyoshimi
 */
abstract class UpdateAction(
    /**
     * Provide a String description of this update method.
     *
     * @return the update description
     */
    open val description: String?,

    /**
     * Provide a longer description for tooltips, etc.
     *
     * @return the update description
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