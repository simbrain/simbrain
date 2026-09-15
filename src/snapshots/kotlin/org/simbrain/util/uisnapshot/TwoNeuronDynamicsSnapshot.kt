/** Renders the actual two-neuron simulation layout with both series and the second recurrence tab. */
package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.simbrain.custom_sims.simulations.dynamical_systems.twoNeuronDynamics
import org.simbrain.workspace.gui.SimbrainDesktop
import java.awt.Component
import java.awt.Dimension
import javax.swing.SwingUtilities

class TwoNeuronDynamicsSnapshot : UiSnapshotDef {
    override val name = "two_neuron_dynamics"

    override fun build(): Component {
        lateinit var desktop: SimbrainDesktop
        SwingUtilities.invokeAndWait { desktop = SimbrainDesktop }
        runBlocking {
            twoNeuronDynamics.run(desktop)
            desktop.workspace.iterateSuspend(350)
        }
        SwingUtilities.invokeAndWait {
            desktop.desktopPane.preferredSize = Dimension(1200, 820)
            desktop.desktopPane.selectTab("Neuron 2")
        }
        return desktop.desktopPane
    }
}
