/**
 * Renders every IAC simulation's network side by side so pool placement and label overlap can be checked without
 * opening each simulation.
 */
package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.simbrain.custom_sims.SimulationScope
import org.simbrain.custom_sims.simulations.iac.iacGames
import org.simbrain.custom_sims.simulations.iac.iacJetsSharks5People
import org.simbrain.custom_sims.simulations.iac.iacJetsSharksFull
import org.simbrain.custom_sims.simulations.iac.iacLanguages
import org.simbrain.custom_sims.simulations.iac.iacMovies
import org.simbrain.custom_sims.simulations.iac.iacNovels
import org.simbrain.custom_sims.simulations.iac.iacSpongeBob
import org.simbrain.network.NetworkComponent
import org.simbrain.network.gui.NetworkPanel
import java.awt.Component
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.BorderFactory
import javax.swing.JDialog
import javax.swing.JPanel
import javax.swing.SwingUtilities

class IacNetworksSnapshot : UiSnapshotDef {
    override val name = "iac_networks"

    private val simulations = listOf(
        "Jets and Sharks" to iacJetsSharksFull,
        "Jets and Sharks (5 people)" to iacJetsSharks5People,
        "Games at Alivia's" to iacGames,
        "Language Classifier" to iacLanguages,
        "Movies" to iacMovies,
        "Novels" to iacNovels,
        "SpongeBob" to iacSpongeBob
    )

    override fun build(): Component {
        val panels = simulations.map { (componentName, sim) ->
            val scope = SimulationScope()
            runBlocking { sim.task.invoke(scope, null) }
            val component = scope.workspace.getComponent(componentName) as NetworkComponent
            NetworkPanel(component).apply {
                preferredSize = Dimension(1000, 650)
                border = BorderFactory.createTitledBorder(componentName)
            }
        }
        val grid = JPanel(GridLayout(0, 2, 8, 8)).apply { panels.forEach { add(it) } }
        SwingUtilities.invokeAndWait {
            JDialog().apply { contentPane = grid; pack() }
            panels.forEach { it.network.events.zoomToFitPage.fire() }
        }
        return grid
    }
}
