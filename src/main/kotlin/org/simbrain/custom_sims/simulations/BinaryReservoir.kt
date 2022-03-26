package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.network.connections.FixedDegree
import org.simbrain.network.core.Neuron
import org.simbrain.network.groups.NeuronCollection
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.neuron_update_rules.BinaryRule
import org.simbrain.util.*
import org.simbrain.util.Utils.FS
import java.io.File
import javax.swing.JTextField

/**
 * Create a reservoir simulation...
 */
val binaryReservoir = newSim {

    // TODO: Button for variance and implement.
    // U_bar
    // Measure of chaos, etc.

    var k = 2
    var variance = .1

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Reservoir Sim")
    val network = networkComponent.network

    // Add a self-connected neuron array to the network
    val resNeurons = List(100) {
        val rule = BinaryRule()
        rule.threshold = .5
        val neuron = Neuron(network, rule)
        neuron
    }
    network.addNetworkModels(resNeurons)
    val reservoir = NeuronCollection(network, resNeurons)
    network.addNetworkModel(reservoir)
    reservoir.label = "Reservoir"
    reservoir.layout(GridLayout())
    reservoir.location = point(0, 0)

    val conn = FixedDegree(degree = k)
    conn.connectNeurons(network, resNeurons, resNeurons)

    fun perturbAndRunNetwork() {

        // Clear the network
        reservoir.clear()

        // Baseline window
        repeat(10) { network.update() }

        // Perturb 10 nodes
        resNeurons.take(10).forEach { n -> n.activation = 1.0 }

        // Response window
        repeat(100) { network.update() }
    }

    // Each row is an activation vector at a time.
    // Rows correspond to times, and columns to nodes
    val activations = mutableListOf<List<Double>>()
    network.addUpdateAction(updateAction("Record activations") {
        activations.add(resNeurons.map { n -> n.activation })
    })

    withGui {
        place(networkComponent) {
            location = point(249, 10)
            width = 400
            height = 400
        }

        createControlPanel("Control Panel", 5, 10) {
            val tf_stdev: JTextField = addTextField("Weight stdev", "" + variance)
            addComponent(tf_stdev)
            addButton("Apply Variance") {
                // Update variance of weight strengths
                // TODO: Confusing because it is not a flat scaling, but relative
                val new_variance = tf_stdev.text.toDouble()
                network.flatSynapseList.forEach { synapse ->
                    synapse.strength = synapse.strength * (new_variance / variance)
                }
                variance = new_variance
            }
            addButton("Run one trial") {
                perturbAndRunNetwork()
                showSaveDialog("activations.csv", "Save Activations") {
                    writeText(activations.toCsvString())
                }
            }

            addButton("Run one trial per parameter") {
                // Choose directory for files
                val path = showDirectorySelectionDialog()
                if (path != null) {
                    listOf(.1, .2, .3, .4, .5, .6, .7, .8, .9).forEach {
                        variance = it
                        perturbAndRunNetwork()
                        // println("${variance}: ${activations}")
                        File(path + FS + "activations${variance}.csv").writeText(activations.toCsvString())
                    }
                }
            }

        }

    }

    // Location of the projection in the desktop
    val projectionPlot = addProjectionPlot("Activations")
    withGui {
        place(projectionPlot) {
            location = point(630, 10)
            width = 400
            height = 400
        }
    }

    // Couple the neuron array to the projection plot
    with(couplingManager) {
        reservoir couple projectionPlot
    }

}