/**
 * Renders the mirror neuron simulation's network so population placement and label overlap can be checked.
 */
package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.simbrain.custom_sims.SimulationScope
import org.simbrain.custom_sims.simulations.psychology.mirrorLearning
import org.simbrain.network.NetworkComponent
import org.simbrain.network.gui.NetworkPanel
import java.awt.Component
import java.awt.Dimension
import javax.swing.JDialog
import javax.swing.SwingUtilities

class MirrorNetworkSnapshot : UiSnapshotDef {
    override val name = "mirror_network"

    override fun build(): Component {
        val scope = SimulationScope()
        runBlocking { mirrorLearning.task.invoke(scope, null) }
        val component = scope.workspace.componentList.filterIsInstance<NetworkComponent>().first()
        val panel = NetworkPanel(component).apply { preferredSize = Dimension(600, 760) }
        SwingUtilities.invokeAndWait {
            JDialog().apply { contentPane = panel; pack() }
            panel.network.events.zoomToFitPage.fire()
        }
        return panel
    }
}
