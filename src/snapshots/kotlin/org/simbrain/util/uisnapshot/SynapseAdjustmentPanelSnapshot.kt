package org.simbrain.util.uisnapshot

import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.gui.dialogs.SynapseAdjustmentPanel
import java.awt.Component
import kotlin.random.Random

class SynapseAdjustmentPanelSnapshot : UiSnapshotDef {
    override val name = "synapse_adjustment_panel"

    override fun build(): Component {
        val rng = Random(42)
        val src = Neuron()
        val tgt = Neuron()
        val synapses = (0 until 200).map {
            val strength = if (rng.nextBoolean()) rng.nextDouble(0.0, 1.0) else -rng.nextDouble(0.0, 1.0)
            Synapse(src, tgt, strength)
        }
        return SynapseAdjustmentPanel(synapses)
    }
}
