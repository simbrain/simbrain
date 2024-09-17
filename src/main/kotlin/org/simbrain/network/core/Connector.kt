package org.simbrain.network.core

import org.simbrain.network.events.ConnectorEvents
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.workspace.AttributeContainer
import smile.math.matrix.Matrix

/**
 * Superclass for classes that inherit from [Layer], i.e. that either produce or consume vectors of values.
 * Dense connectors, i.e. weight matrices, are the most obvious example. More information is in the layer javadocs.
 */
abstract class Connector(var source: Layer, var target: Layer) : NetworkModel(), EditableObject, AttributeContainer {

    @Transient
    override val events: ConnectorEvents = ConnectorEvents()

    /**
     * If true render an image showing all weight strengths as pixels
     */
    var isShowWeights: Boolean = true
        set(showWeights) {
            field = showWeights
            events.showWeightsChanged.fire()
        }

    /**
     * Construct a connector and initialize events.
     */
    init {
        initEvents()
    }

    /**
     * A matrix of PSRs (pre-synaptic responses) for each connection.
     */
    abstract val psrMatrix: Matrix

    /**
     * Returns the row sums of the psr matrix, which correspond in the connectionist case to the standard produce of an
     * input vector and a weight matrix, and in the spiking case corresponds to the sum of post-synaptic responses along
     * the dendrite of each output neuron.
     */
    fun getSummedPSRs(): DoubleArray {
        return psrMatrix.rowSums()
    }

    context(Network)
    abstract fun updatePSR()

    private fun initEvents() {
        // When the parents of the matrix are deleted, delete the matrix
        source.events.deleted.on(wait = true) { delete() }
        target.events.deleted.on(wait = true) { delete() }
    }

    override suspend fun delete() {
        source.removeOutgoingConnector(this)
        target.removeIncomingConnector(this)
        events.deleted.fire(this).await()
    }
}
