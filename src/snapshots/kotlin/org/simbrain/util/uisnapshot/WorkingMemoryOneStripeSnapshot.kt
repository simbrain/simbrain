/**
 * Renders the one-stripe working memory network after remembering A, so layer placement and label overlap can be
 * checked.
 */
package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.simbrain.custom_sims.SimulationScope
import org.simbrain.custom_sims.simulations.leabra.WmStep
import org.simbrain.custom_sims.simulations.leabra.WmTrial
import org.simbrain.custom_sims.simulations.leabra.WorkingMemoryModel
import org.simbrain.custom_sims.simulations.leabra.workingMemoryOneStripe
import org.simbrain.network.NetworkComponent
import org.simbrain.custom_sims.simulations.leabra.showResult
import org.simbrain.network.core.NetworkTextObject
import org.simbrain.network.gui.NetworkPanel
import java.awt.Component
import java.awt.Dimension
import javax.swing.JDialog
import javax.swing.SwingUtilities

class WorkingMemoryOneStripeSnapshot : UiSnapshotDef {
    override val name = "leabra_working_memory_1_stripe"

    override fun build(): Component {
        val scope = SimulationScope()
        runBlocking { workingMemoryOneStripe.task.invoke(scope, null) }
        val component = scope.workspace.componentList.filterIsInstance<NetworkComponent>().first()
        val network = component.network
        WorkingMemoryModel(network).test(WmTrial(WmStep.REMEMBER, 0), fireEvents = true)
        val texts = network.getModels<NetworkTextObject>().associateBy { it.label }
        texts.getValue("Gate").text = "Gate open: storing A"
        texts.getValue("Result").showResult(false)
        val panel = NetworkPanel(component).apply { preferredSize = Dimension(640, 640) }
        SwingUtilities.invokeAndWait {
            JDialog().apply { contentPane = panel; pack() }
            panel.network.events.zoomToFitPage.fire()
        }
        return panel
    }
}
