package org.simbrain.network.util

import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.CopyableObject

/**
 * Holders for matrix valued data used in array based update rules. E.g. [NeuronArray]
 */
interface MatrixDataHolder

class EmptyMatrixData : MatrixDataHolder

class BiasedMatrixData(var size: Int) : MatrixDataHolder {
    var biases = DoubleArray(size)
}

class SpikingMatrixData(var size: Int) : MatrixDataHolder {
    var spikes = BooleanArray(size)
}

class NakaMatrixData(size: Int) : MatrixDataHolder {
    var a = DoubleArray(size)
}

/**
 * Holders for scalar data used in scalar update rules, like [NeuronUpdateRule] and [SynapseUpdateRule].
 */
interface ScalarDataHolder : CopyableObject {
    override fun copy(): ScalarDataHolder
}

class EmptyScalarData : ScalarDataHolder {
    override fun copy(): EmptyScalarData {
        return EmptyScalarData()
    }
}

class BiasedScalarData(
    @UserParameter(label = "bias")
    var bias: Double = 0.0
) : ScalarDataHolder {
    override fun copy(): BiasedScalarData {
        return BiasedScalarData(bias)
    }
}

class NakaScalarData(@UserParameter(label = "a") var a: Double = 0.0) : ScalarDataHolder {
    override fun copy(): NakaScalarData {
        return NakaScalarData(a)
    }
}

class IzhikData(
    @UserParameter(label = "a") var a: Double = 0.0,
    @UserParameter(label = "b") var b: Double = 0.0,
    @UserParameter(label = "c") var c: Double = 0.0,
    @UserParameter(label = "d") var d: Double = 0.0
) : ScalarDataHolder {
    override fun copy(): IzhikData {
        return IzhikData(a, b, c, d)
    }
}
