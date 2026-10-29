/**
 * The batch form of a layer update, which [org.simbrain.network.trainers.BatchedBackprop] uses to train a whole batch
 * of examples as matrix products.
 */
package org.simbrain.network.updaterules.interfaces

import smile.math.matrix.Matrix

interface BatchActivation {

    /**
     * Whether [activateColumns] reproduces this rule's layer update exactly. A subclass that changes the update
     * without overriding [activateColumns] reports false and trains one example at a time.
     */
    val activatesBatches: Boolean

    /** The activations for each column of [netInputs], as the rule's layer update gives for that column alone. */
    fun activateColumns(netInputs: Matrix): Matrix
}
