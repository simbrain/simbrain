/**
 * Renders the error-driven learning network after a guess, so layer placement and label overlap can be checked.
 */
package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.simbrain.custom_sims.SimulationScope
import org.simbrain.custom_sims.simulations.leabra.errorDrivenLearning
import org.simbrain.custom_sims.simulations.leabra.flashcardTrainer
import org.simbrain.custom_sims.simulations.leabra.flashcards
import org.simbrain.custom_sims.simulations.leabra.showResult
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.NetworkTextObject
import org.simbrain.network.gui.NetworkPanel
import java.awt.Component
import java.awt.Dimension
import javax.swing.JDialog
import javax.swing.SwingUtilities

class ErrorDrivenLearningSnapshot : UiSnapshotDef {
    override val name = "leabra_error_driven_learning"

    override fun build(): Component {
        val scope = SimulationScope()
        runBlocking { errorDrivenLearning.task.invoke(scope, null) }
        val component = scope.workspace.componentList.filterIsInstance<NetworkComponent>().first()
        val network = component.network
        flashcardTrainer(network).test(flashcards.first().picture)
        val texts = network.getModels<NetworkTextObject>().associateBy { it.label }
        texts.getValue("Learning message").text = "Learning!"
        texts.getValue("Result").showResult(true)
        val panel = NetworkPanel(component).apply { preferredSize = Dimension(520, 640) }
        SwingUtilities.invokeAndWait {
            JDialog().apply { contentPane = panel; pack() }
            panel.network.events.zoomToFitPage.fire()
        }
        return panel
    }
}
