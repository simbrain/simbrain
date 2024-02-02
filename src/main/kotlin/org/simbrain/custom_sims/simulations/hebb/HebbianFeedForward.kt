package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.newSim
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.getModelByLabel
import org.simbrain.util.Utils.FS
import org.simbrain.world.odorworld.OdorWorldComponent
import java.io.File

/**
 * Demo for studying Hebbian feed-forward pattern association
 */
val hebbianFeedForward = newSim {

    // Basic setup
    workspace.openWorkspace(File("simulations" + FS + "workspaces"+ FS + "hebbFF.zip"))
    val network = (workspace.getComponent("Network") as NetworkComponent).network
    val nice: Neuron = network.getModelByLabel("\"nice\"")
    val yuck: Neuron = network.getModelByLabel("\"yuck\"")

    val world = (workspace.getComponent("Stimuli") as OdorWorldComponent).world

    fun unclampOutputs() {
        nice?.clamped = false
        yuck?.clamped = false
    }
    // withGui {
    //     createControlPanel("Control Panel", 5, 10) {
    //         addButton("Training Mode (clamped nodes)") {
    //             network.clampNeurons(true)
    //             network.freezeSynapses(false)
    //             unclampOutputs()
    //         }.apply {
    //             toolTipText = "Clamps nodes and unclamps weights"
    //         }
    //         addButton("Test Mode (clamped weights)") {
    //             network.clampNeurons(false)
    //             network.freezeSynapses(true)
    //         }.apply {
    //             toolTipText = "Clamps weights and unclamps nodes"
    //         }
    //     }
    // }



}