package org.simbrain.network.kotlindl

import org.jetbrains.kotlinx.dl.api.core.activation.Activations
import org.simbrain.util.UserParameter

/**
 * Wrapper for tensor flow dense layer
 */
class TFDenseLayer : TFLayer() {

    @UserParameter(label = "Number of outputs", order = 10)
    var nout = 5

    @UserParameter(label = "Actiation function", order = 20)
    var activations = Activations.Relu

    @UserParameter(label = "Kernel initializer", order = 30)
    var kernelInitializer = ""

    @UserParameter(label = "Bias initializer", order = 40)
    var biasInitializer = ""

}