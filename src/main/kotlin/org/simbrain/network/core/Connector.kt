package org.simbrain.network.core

import org.simbrain.network.NetworkModel
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
     * Whether to render an image of this entity.
     */
    var isEnableRendering: Boolean = true

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
     * Returns the output of this connector
     */
    context(Network)
    abstract val output: Matrix

    private fun initEvents() {
        // When the parents of the matrix are deleted, delete the matrix
        source.events.deleted.on(wait = true) { delete() }
        target.events.deleted.on(wait = true) { delete() }
    }

    override fun delete() {
        source.removeOutgoingConnector(this)
        target.removeIncomingConnector(this)
        events.deleted.fireAndBlock(this)
    }
}
