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
        source.addOutgoingFlattenConnector(this)
        target.addIncomingFlattenConnector(this)
    }

    /**
     * Copy source activations into target inputs. Called during the target's
     * accumulateInputs pass.
     */
    fun propagate() {
        val src = source.activations
        val n = minOf(src.size, target.size)
        val col = DoubleArray(target.size)
        src.copyInto(col, endIndex = n)
        target.addInputs(col)
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
