package org.simbrain.network.core

import org.simbrain.network.events.TensorConnectorEvents
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.workspace.AttributeContainer

/**
 * Bridges the CNN [Tensor] hierarchy to the Layer/[NeuronArray] hierarchy by flattening
 * a Tensor's HWC activations into a 1D input vector for a NeuronArray.
 *
 * This is neither a [TensorConnector] (target is NeuronArray, not Tensor) nor a [Connector]
 * (source is Tensor, not Layer). It is a standalone [NetworkModel] that manages its own
 * registration on both sides.
 *
 * During [propagate], the source Tensor's activations are copied into the target NeuronArray's
 * inputs via [ArrayLayer.addInputs].
 */
class FlattenConnector(val source: Tensor, val target: NeuronArray) :
    NetworkModel(), EditableObject, AttributeContainer {

    @Transient
    override var events: TensorConnectorEvents = TensorConnectorEvents()

    init {
        require(source.shape.size == target.size) {
            "FlattenConnector requires matching sizes, but source has ${source.shape.size} elements and target has size ${target.size}"
        }
        source.addOutgoingFlattenConnector(this)
        target.addIncomingFlattenConnector(this)
    }

    /**
     * Copy source activations into target inputs. Called during the target's
     * accumulateInputs pass. The array is copied internally by [ArrayLayer.addInputs].
     */
    fun propagate() {
        target.addInputs(source.activations)
    }

    /**
     * Backward pass: copies dense-layer gradient back into the source tensor's gradients.
     */
    fun backward(denseGrad: DoubleArray) {
        require(denseGrad.size == source.shape.size) {
            "Flatten backward gradient size ${denseGrad.size} must match source tensor size ${source.shape.size}"
        }
        source.clearGradients()
        System.arraycopy(denseGrad, 0, source.gradients, 0, source.shape.size)
    }

    override suspend fun delete(): List<NetworkModel> {
        source.removeOutgoingFlattenConnector(this)
        target.removeIncomingFlattenConnector(this)
        events.deleted.fire(this).await()
        return listOf(this)
    }

    override suspend fun afterRestore(context: Any?) {
        source.addOutgoingFlattenConnector(this)
        target.addIncomingFlattenConnector(this)
    }

    override val name: String get() = "Flatten"

    override fun toString(): String =
        "$displayName (Flatten ${source.shape.size} -> ${target.size})"
}
