package org.simbrain.network.gui.nodes

import smile.math.matrix.Matrix

interface ActivationSequenceProcessor {
    val activations: Matrix

    fun accumulateBackprop(gradient: Matrix, rawMatrixAccumulator: HashMap<Matrix, Matrix>): Matrix
}