package org.simbrain.network.util

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
interface ScalarDataHolder

class EmptyScalarData : ScalarDataHolder

class BiasedScalarData(var bias:  Double = 0.0) : ScalarDataHolder

class NakaScalarData(var a:  Double = 0.0) : ScalarDataHolder
