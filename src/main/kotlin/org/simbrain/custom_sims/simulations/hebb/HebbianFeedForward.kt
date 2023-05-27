package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.simbrain.custom_sims.createControlPanel
import org.simbrain.custom_sims.newSim
import org.simbrain.network.NetworkComponent
import org.simbrain.util.Utils.FS
import org.simbrain.workspace.serialization.WorkspaceSerializer
import org.simbrain.world.odorworld.OdorWorldComponent
import java.io.File
import java.io.FileInputStream

/**
 * Demo for studying Hebbian feed-forward pattern association
 */
val hebbianFeedForward = newSim {

    // Basic setup
    workspace.clearWorkspace()
    // TODO: See SimbrainDesktop.save(). Make a util
    withContext(Dispatchers.IO) {
        val theFile = File("simulations" + FS + "workspaces"+ FS + "hebbFF.zip")
        WorkspaceSerializer(workspace).deserialize(FileInputStream(theFile))
        workspace.currentFile = theFile
    }
    val network = (workspace.getComponent("Network 1") as NetworkComponent).network
    val nice = network.getNeuronByLabel("\"nice\"")
    val yuck = network.getNeuronByLabel("\"yuck\"")

    val world = (workspace.getComponent("OdorWorld 1") as OdorWorldComponent).world

    fun unclampOutputs() {
        nice?.isClamped = false
        yuck?.isClamped = false
    }
    withGui {
        createControlPanel("Control Panel", 5, 10) {
            addButton("Training Mode (clamped nodes)") {
                network.clampNeurons(true)
                network.freezeSynapses(false)
                unclampOutputs()
            }.apply {
                toolTipText = "Clamps nodes and unclamps weights"
            }
            addButton("Test Mode (clamped weights)") {
                network.clampNeurons(false)
                network.freezeSynapses(true)
            }.apply {
                toolTipText = "Clamps weights and unclamps nodes"
            }
        }
    }



}