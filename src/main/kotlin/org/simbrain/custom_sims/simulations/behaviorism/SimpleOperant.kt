package org.simbrain.custom_sims.simulations.behaviorism

import org.simbrain.custom_sims.*
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.addNeuronGroup
import org.simbrain.network.core.getModelByLabel
import org.simbrain.network.layouts.LineLayout
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.util.math.SimbrainMath
import org.simbrain.util.place
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.gui.SimbrainDesktop
import org.simbrain.workspace.updater.updateAction
import kotlin.math.max

/**
 * Simulation to demonstrate simple operant conditioning.
 *
 * @author Tim Meyer
 * @author Jeff Yoshimi
 */
val simpleOperant = newSim("simple operant conditioning") {

    val numNeurons = 3
    workspace.clearWorkspace()

    val nc = addNetworkComponent("Behaviors")
    withGui {
        place(nc, 195, 9, 447, 296)
    }
    val behaviorNet = nc.network.addNeuronGroup(-9.25, 95.93, numNeurons)
    behaviorNet.label = "Behaviors"
    behaviorNet.layout = LineLayout(100.0, LineLayout.LineOrientation.HORIZONTAL)
    behaviorNet.applyLayout(-5, -85)
    behaviorNet.isClamped = true

    // Use aux values to store firing probabilities
    behaviorNet.getNeuron(0).auxValue = .34
    behaviorNet.getNeuron(1).auxValue = .33
    behaviorNet.getNeuron(2).auxValue = .33

    setUpSimpleOperantWorkpace(workspace)

}.registerReopenFunction { workspace -> setUpSimpleOperantWorkpace(workspace) }


suspend fun SimulationScope.setUpSimpleOperantWorkpace(workspace: Workspace) {

    val network = workspace.componentList.filterIsInstance<NetworkComponent>().first().network
    val behaviorNet = network.getModelByLabel<NeuronGroup>("Behaviors")
    val nodeToLabel = HashMap<Neuron, String>()
    nodeToLabel[behaviorNet.getNeuron(0)] = "Yell"
    nodeToLabel[behaviorNet.getNeuron(1)] = "Sit"
    nodeToLabel[behaviorNet.getNeuron(2)] = "Run"

    fun setWinningNode(nodeIndex: Int) {
        for (i in 0 until behaviorNet.size) {
            if (i == nodeIndex) {
                behaviorNet.getNeuron(i).activation = 1.0
            } else {
                behaviorNet.getNeuron(i).activation = 0.0
            }
        }
    }

    // Add custom network update action
    network.updateManager.addAction(updateAction("Custom behaviorism update") {
        // Select "winning" neuron based on its probability
        // TODO: There must be a better, generalizable way to do this
        val random = Math.random()
        if (random < behaviorNet.getNeuron(0).auxValue) {
            setWinningNode(0)
        } else if (random < (behaviorNet.getNeuron(0).auxValue
                    + behaviorNet.getNeuron(1).auxValue)
        ) {
            setWinningNode(1)
        } else {
            setWinningNode(2)
        }
    })

    withGui {

        fun updateNodeLabels() {
            for (n in behaviorNet.neuronList) {
                n.label = "" + nodeToLabel[n] + ": " + SimbrainMath.roundDouble(n.auxValue, 2)
            }
        }
        updateNodeLabels()

        // Initialize labels
        fun normalizeProbabilities() {
            var totalMass = 0.0
            for (n in behaviorNet.neuronList) {
                totalMass += n.auxValue
            }
            for (n in behaviorNet.neuronList) {
                n.auxValue /= totalMass
            }
        }

        createControlPanel("Control Panel", 0, 15) {

            addButton("Reward Agent") {
                for (n in behaviorNet.neuronList) {
                    if (n.activation > 0) {
                        val p = n.auxValue
                        n.auxValue = max(p + .1 * p, 0.0)
                    }
                }
                normalizeProbabilities()
                updateNodeLabels()
                SimbrainDesktop.workspace.simpleIterate()
            }

            addButton("Punish Agent") {
                for (n in behaviorNet.neuronList) {
                    if (n.activation > 0) {
                        val p = n.auxValue
                        n.auxValue = max(p - .1 * p, 0.0)
                    }
                }
                normalizeProbabilities()
                updateNodeLabels()
                SimbrainDesktop.workspace.simpleIterate()
            }

            addButton("Do Nothing") {
                SimbrainDesktop.workspace.simpleIterate()
            }
        }

        addSidebarInfo(
            """
            # Introduction
            Models simple operant conditioning. Nodes here correspond to behaviors. Behaviors are initially emitted spontaneously and randomly.
            Reward the behavior you want to reinforce and punish all others to train the network to produce the desired 
             result. 
            """.trimIndent()
        )
    }
}





