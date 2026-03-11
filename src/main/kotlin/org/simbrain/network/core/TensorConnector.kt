package org.simbrain.network.core

import org.simbrain.network.events.TensorConnectorEvents
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.workspace.AttributeContainer

/**
 * Base class for connectors between [TensorLayer] nodes. Analogous to [Connector] but for the
 * tensor/CNN hierarchy. Subclasses implement [propagate] to accumulate their output into
 * [target]'s inputs array.
 *
 * **Gradient accumulation contract:** During backpropagation, backward methods accumulate
 * into [TensorLayer.gradients] and connector gradient arrays (e.g. [ConvolutionConnector.kernelGrads]).
 * Callers must clear these arrays before a new backward pass or batch:
 * - Call [TensorLayer.clearGradients] on each tensor before backpropagation.
 * - Call [ConvolutionConnector.clearGrads] on conv connectors before accumulating a new batch.
 */
abstract class TensorConnector(val source: TensorLayer, val target: TensorLayer) :
    NetworkModel(), EditableObject, AttributeContainer {

    @Transient
    override var events: TensorConnectorEvents = TensorConnectorEvents()

    init {
        source.addOutgoingConnector(this)
        target.addIncomingConnector(this)
    }

    /**
     * Compute output from [source] activations and accumulate into [target] inputs.
     * Called during the accumulate-inputs pass.
     */
    abstract fun propagate()

    override suspend fun delete(): List<NetworkModel> {
        source.removeOutgoingConnector(this)
        target.removeIncomingConnector(this)
        events.deleted.fire(this).await()
        return listOf(this)
    }

    override suspend fun afterRestore(context: Any?) {
        source.addOutgoingConnector(this)
        target.addIncomingConnector(this)
    }
}
