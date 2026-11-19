/**
 * Renders the 1-2-AX network after showing 1, A, and X, with example overlays: a five-step strip, stripe 2's gate
 * line open, and a dopamine burst badge, so layer placement and overlap can be checked.
 */
package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.simbrain.custom_sims.SimulationScope
import org.simbrain.custom_sims.simulations.leabra.*
import org.simbrain.network.NetworkComponent
import org.simbrain.network.gui.NetworkPanel
import java.awt.Component
import java.awt.Dimension
import javax.swing.JDialog
import javax.swing.SwingUtilities

class OneTwoAxSnapshot : UiSnapshotDef {
    override val name = "leabra_one_two_ax"

    override fun build(): Component {
        val scope = SimulationScope()
        runBlocking { oneTwoAx.task.invoke(scope, null) }
        val component = scope.workspace.componentList.filterIsInstance<NetworkComponent>().first()
        val network = component.network
        val model = OneTwoAxModel(network)
        listOf("1", "A", "X").forEach { model.test(axIndex(it), fireEvents = true) }
        val panel = NetworkPanel(component).apply { preferredSize = Dimension(1000, 900) }
        val strip = runBlocking { addSequenceStrip(panel, model) }
        val inputs = listOf("1", "A", "X", "B", "Y")
        val responses = listOf(AX_L, AX_L, AX_R, AX_L, AX_R)
        val steps = inputs.indices.map { i ->
            val target = axTrial(inputs.take(i + 1).map { axIndex(it) }).target
            AxStepView(axIndex(inputs[i]), target, responses[i])
        }
        val gateLines = runBlocking { addGateLines(panel, model) }
        val badge = runBlocking { addDopamineBadge(panel, model) }
        SwingUtilities.invokeAndWait {
            strip.steps = steps
            gateLines[1].open = true
            badge.change = DopamineChange.BURST
            JDialog().apply { contentPane = panel; pack() }
            panel.network.events.zoomToFitPage.fire()
        }
        return panel
    }
}
