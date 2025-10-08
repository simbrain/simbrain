package org.simbrain.network.core

/**
 * Classes that implement this interface describe individual actions that
 * together comprise a network update.
 */
interface NetworkUpdateAction {

    fun invoke()

    val description: String?

    /**
     * Longer description for tooltips, etc.
     */
    val longDescription: String?
}
