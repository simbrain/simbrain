/**
 * The supervised trainer's run controls and statistics with every optional row turned on: testing error,
 * accuracy, effective step size, and early stopping. A default trainer shows only the iteration count and
 * training error, so this is the layout most likely to crowd the run buttons.
 */
package org.simbrain.util.uisnapshot

import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.TrainerControls
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.trainers.SupervisedTrainer
import java.awt.Component
import javax.swing.SwingUtilities

class SupervisedTrainerStatsSnapshot : UiSnapshotDef {
    override val name = "supervised_trainer_stats"

    override fun build(): Component {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("snapshot", network))
        val bp = BackpropNetwork(intArrayOf(4, 3, 2), null)
        with(bp.trainerConfig) {
            testConfiguration.enabled = true
            computeAccuracy = true
            showEffectiveStepSize = true
            stoppingCondition.useEarlyStopping = true
        }
        val trainer = SupervisedTrainer(network, bp)

        lateinit var controls: Component
        SwingUtilities.invokeAndWait {
            controls = TrainerControls(trainer, bp, panel)
        }
        return controls
    }
}
