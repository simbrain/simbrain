/**
 * Renders the flashcard vocabulary network after a guess, so layer placement and label overlap can be checked.
 */
package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.simbrain.custom_sims.SimulationScope
import org.simbrain.custom_sims.simulations.leabra.flashcards
import org.simbrain.custom_sims.simulations.leabra.leabraVocabulary
import org.simbrain.custom_sims.simulations.leabra.vocabularyTrainer
import org.simbrain.network.NetworkComponent
import org.simbrain.network.gui.NetworkPanel
import java.awt.Component
import java.awt.Dimension
import javax.swing.JDialog
import javax.swing.SwingUtilities

class LeabraVocabularySnapshot : UiSnapshotDef {
    override val name = "leabra_vocabulary"

    override fun build(): Component {
        val scope = SimulationScope()
        runBlocking { leabraVocabulary.task.invoke(scope, null) }
        val component = scope.workspace.componentList.filterIsInstance<NetworkComponent>().first()
        val network = component.network
        vocabularyTrainer(network).test(flashcards.first().picture)
        val panel = NetworkPanel(component).apply { preferredSize = Dimension(520, 640) }
        SwingUtilities.invokeAndWait {
            JDialog().apply { contentPane = panel; pack() }
            panel.network.events.zoomToFitPage.fire()
        }
        return panel
    }
}
