package org.simbrain.network.core

/**
 * Classes that implement this interface describe individual actions that
 * together comprise a network update.
 *
 * @author jyoshimi
 */
interface NetworkUpdateAction {
    /**
     * Invoke this action.
     */
    fun invoke()

    /**
     * Provide a String description of this update method.
     *
     * @return the update description
     */
    val description: String?

    /**
     * Provide a longer description for tooltips, etc.
     *
     * @return the update description
     */
    val longDescription: String?
}
