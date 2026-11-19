/**
 * Renders the learned gating network after remembering A and recalling, so layer placement and label overlap can be
 * checked.
 */
package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.simbrain.custom_sims.SimulationScope
import org.simbrain.custom_sims.simulations.leabra.LearnedGatingModel
import org.simbrain.custom_sims.simulations.leabra.WmStep
import org.simbrain.custom_sims.simulations.leabra.WmTrial
import org.simbrain.custom_sims.simulations.leabra.learnedGating
import org.simbrain.custom_sims.simulations.leabra.showResult
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.NetworkTextObject
import org.simbrain.network.gui.NetworkPanel
import java.awt.Component
import java.awt.Dimension
import javax.swing.JDialog
import javax.swing.SwingUtilities

class LearnedGatingSnapshot : UiSnapshotDef {
    override val name = "leabra_learned_gating"

    override fun build(): Component {
        val scope = SimulationScope()
        runBlocking { learnedGating.task.invoke(scope, null) }
        val component = scope.workspace.componentList.filterIsInstance<NetworkComponent>().first()
        val network = component.network
        val model = LearnedGatingModel(network)
        model.test(WmTrial(WmStep.REMEMBER, 0), fireEvents = true)
        model.reward(learn = false)
        val texts = network.getModels<NetworkTextObject>().associateBy { it.label }
        texts.getValue("Gate").text = "Go wins: gate open, storing A"
        texts.getValue("Result").showResult(true)
        texts.getValue("Dopamine message").text = "Reward 1 (output says A), expected 0.00\nDopamine +1.00: better than expected"
        val panel = NetworkPanel(component).apply { preferredSize = Dimension(700, 800) }
        SwingUtilities.invokeAndWait {
            JDialog().apply { contentPane = panel; pack() }
            panel.network.events.zoomToFitPage.fire()
        }
        return panel
    }
}
