package org.simbrain.network.util

import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.EditableObject

/**
 * Holders for matrix valued data used in array based update rules. E.g. [NeuronArray]
 */
interface MatrixDataHolder

class EmptyMatrixData : MatrixDataHolder

class BiasedMatrixData(var size : Int) : MatrixDataHolder {
    var biases = DoubleArray(size)
}

class SpikingMatrixData(var size: Int) : MatrixDataHolder {
    var spikes = BooleanArray(size)
}

class NakaMatrixData(size: Int) : MatrixDataHolder {
    var a =  DoubleArray(size)
}

/**
 * Holders for scalar data used in scalar update rules, like [NeuronUpdateRule] and [SynapseUpdateRule].
 */
interface ScalarDataHolder : EditableObject

class EmptyScalarData : ScalarDataHolder

class BiasedScalarData(@UserParameter(label="bias")
                       var bias:  Double = 0.0) : ScalarDataHolder

class NakaScalarData(@UserParameter(label="a") var a:  Double = 0.0) : ScalarDataHolder

class IzhikData(
    @UserParameter(label="a") var a:  Double = 0.0,
    @UserParameter(label="b") var b:  Double = 0.0,
    @UserParameter(label="c") var c:  Double = 0.0,
    @UserParameter(label="d") var d:  Double = 0.0
): ScalarDataHolder
