package org.simbrain.network.learningrules

import org.simbrain.network.core.Connector
import org.simbrain.network.core.Network
import org.simbrain.network.core.Synapse
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.EmptyScalarData

/**
 * **ClampedSynapse**.
 */
open class StaticSynapseRule : SynapseUpdateRule<EmptyScalarData, EmptyMatrixData>() {

    var isClipped: Boolean = false

    override fun init(synapse: Synapse) {
        // TODO Auto-generated method stub
    }

    override fun copy(): SynapseUpdateRule<*, *> {
        val cs = StaticSynapseRule()
        return cs
    }

    context(Network)
    override fun apply(synapse: Synapse, data: EmptyScalarData) {
        // if (clipped) {
        // super.setStrength(Synapse(synapse.getStrength()));
        // }
    }

    context(Network)
    override fun apply(connector: Connector, dataHolder: EmptyMatrixData) {
        // Static synapse rule does nothing for both scalar and matrix versions
        // Weights remain unchanged
    }

    override val name: String
        get() = "Static"
}
