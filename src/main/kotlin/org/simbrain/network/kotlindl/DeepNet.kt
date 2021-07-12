package org.simbrain.network.kotlindl

import org.jetbrains.kotlinx.dl.api.core.Sequential
import org.simbrain.network.core.Layer
import org.simbrain.network.core.Network
import org.simbrain.network.util.lenet5Classic
import org.simbrain.util.UserParameter
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.workspace.AttributeContainer
import smile.math.matrix.Matrix
import java.awt.geom.Rectangle2D

class DeepNet(private val network: Network
    , val inputSize: Int
    , val outputSize: Int) : Layer(), AttributeContainer, EditableObject {

    var deepNetLayers: Sequential = Sequential()

    private var inputs = FloatArray(outputSize)

    init {
        label = network.idManager.getProposedId(this.javaClass)
        deepNetLayers = lenet5Classic // TODO: Temp
    }

    override fun addInputs(newInputs: Matrix?) {
        for (i in 0 until inputs.size) {
            if (newInputs != null) {
                inputs[i] += newInputs.get(i, 0).toFloat()
            }
        }
    }

    override fun getOutputs(): Matrix {
        val out = Matrix(outputSize, 1)
        out[deepNetLayers.predict(inputs), 0] = 1.0
        return out
    }

    override fun size(): Int {
        return deepNetLayers.layers.last().outputShape[1].toInt();
    }

    override fun getNetwork(): Network {
        return network
    }

    override fun getId(): String {
        return super<Layer>.getId()
    }

    override fun toString(): String {
        return id + ":\n" + deepNetLayers.layers.joinToString("\n") { it.name }
    }

    override fun update() {
        events.fireUpdated()
    }

    override fun getInputs(): Matrix? {
        return null
    }

    override fun getBound(): Rectangle2D {
        return Rectangle2D.Double(x - 150 / 2, y - 50 / 2, 150.0, 50.0)
    }

    /**
     * Helper class for creating new deep networks.
     */
    class DeepNetCreator : EditableObject {

        @UserParameter(label = "Number of inputs", order = 10)
        var nin = 10

        // @UserParameter(label = "Input layer", isObjectType = true, order = 20)
        // var TFLayer = TFDenseLayer()

        @UserParameter(label = "Number of outputs", order = 20)
        var nout = 5

        override fun getName(): String {
            return "Deep Network"
        }

        fun create(net : Network): DeepNet {
            return DeepNet(net, nin, nout)
        }

    }

}

