package org.simbrain.network.core

import org.jetbrains.kotlinx.dl.api.core.Sequential
import org.simbrain.network.connectors.Layer
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.workspace.AttributeContainer
import smile.math.matrix.Matrix
import java.awt.geom.Rectangle2D

class DeepNet(private val network: Network, val size: Int) : Layer(), AttributeContainer, EditableObject {

    var deepNetLayers: Sequential = Sequential()

    private var inputs = FloatArray(size)

    init {
        label = network.idManager.getProposedId(this.javaClass)
    }

    override fun addInputs(newInputs: Matrix?) {
        for (i in 0 until inputs.size) {
            if (newInputs != null) {
                inputs[i] += newInputs.get(i, 0).toFloat()
            }
        }
    }

    override fun getOutputs(): Matrix {
        val out = Matrix(size, 1)
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

    override fun getInputs(): Matrix? {
        // TODO: If needed, convert float to double matrix
        return null
    }

    override fun getBound(): Rectangle2D {
        return Rectangle2D.Double(x - 150 / 2, y - 50 / 2, 150.0, 50.0)
    }

}

