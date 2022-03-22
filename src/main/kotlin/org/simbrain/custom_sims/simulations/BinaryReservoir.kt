package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.network.connections.FixedDegree
import org.simbrain.network.core.Neuron
import org.simbrain.network.groups.NeuronCollection
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.neuron_update_rules.BinaryRule
import org.simbrain.util.place
import org.simbrain.util.point
import javax.swing.JTextField

/**
 * Create a reservoir simulation...
 */
val binaryReservoir = newSim {

    // TODO: Button for variance and implement.
    // U_bar
    // Measure of chaos, etc.

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Reservoir Sim")
    val network = networkComponent.network

    // Add a self-connected neuron array to the network
    val resNeurons = (0..100).map {
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

    var k = 2
    var variance = .1

    val conn = FixedDegree(degree = k)
    conn.connectNeurons(network, resNeurons, resNeurons)

    withGui {
        place(networkComponent) {
            location = point(179,10)
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
                network.flatSynapseList.forEach{ synapse ->
                    synapse.strength = synapse.strength * (new_variance / variance)
                }
                variance = new_variance
            }
        }

    }

    // Location of the projection in the desktop
    val projectionPlot = addProjectionPlot("Activations")
    withGui {
        place(projectionPlot) {
            location = point(570, 10)
            width = 400
            height = 400
        }
    }

    // Couple the neuron array to the projection plot
    with(couplingManager) {
        reservoir couple projectionPlot
    }

}