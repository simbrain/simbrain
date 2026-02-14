package org.simbrain.network.core

import org.simbrain.network.events.TensorConnectorEvents
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.workspace.AttributeContainer

/**
 * Base class for connectors between [Tensor] nodes. Analogous to [Connector] but for the
 * tensor/CNN hierarchy. Subclasses implement [propagate] to accumulate their output into
 * [target]'s inputs array.
 */
abstract class TensorConnector(val source: Tensor, val target: Tensor) :
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
