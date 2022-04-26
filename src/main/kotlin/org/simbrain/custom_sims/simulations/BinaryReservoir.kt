package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.network.connections.FixedDegree
import org.simbrain.network.core.Neuron
import org.simbrain.network.groups.NeuronCollection
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.neuron_update_rules.BinaryRule
import org.simbrain.util.*
import org.simbrain.util.Utils.FS
import org.simbrain.util.stats.distributions.NormalDistribution
import org.simbrain.util.stats.distributions.TwoValued
import java.io.File
import java.lang.Math.sqrt
import javax.swing.JTextField

/**
 * Create a reservoir simulation.
 *
 * Based on Bertschinger, Nils, and Thomas Natschläger.
 * "Real-time computation at the edge of chaos in recurrent neural networks."
 * Neural computation 16.7 (2004): 1413-1436.
 *
 * Also see [EdgeOfChaos] and [EdgeOfChaosBitsream]
 *
 * @author Jeff Yoshimi
 * @author Sergio Ponce de Leon
 */
val binaryReservoir = newSim {

    // TODO: Button for variance and implement.
    // U_bar
    // Measure of chaos, etc.

    val k = 4
    var variance = .1
    val baseIterations = 400
    val responseIterations = 300
    val inputNoise = TwoValued(-1.0,1.0, .5)
    val numNeurons = 250

    var normalDist = NormalDistribution(0.0, 1.0)
    // normalDist.randomSeed = 1 // Not sufficient to randomize

    // Stored activations
    // Each row is an activation vector at a time.
    // Rows correspond to times, and columns to nodes
    val activations = mutableListOf<List<Double>>()

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Reservoir Sim")
    val network = networkComponent.network

    // Add a self-connected neuron array to the network
    val resNeurons = List(numNeurons) {
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

    /**
     * Resets the variance by rescaling synapses. Assumes valid variance to begin with.
     */
    fun setVariance(newVariance: Double) {
        // if (newVariance !in (0.0..1.0)) {
        //     JOptionPane.showMessageDialog(null, "Variance must be between 0 and 1")
        //     return
        // }
        normalDist.standardDeviation = sqrt(newVariance)
        network.flatSynapseList.forEach { synapse ->
            synapse.strength = normalDist.sampleDouble()
        }
        variance = newVariance

        val av = network.flatSynapseList.map { it.strength }.toDoubleArray().variance()
        println("Variance set to ${variance}; Actual variance: ${av}")

    }

    fun perturbAndRunNetwork() {

        println("Variance: ${variance}")

        // Clear the network and activations
        reservoir.randomize()
        activations.clear()

        // Baseline window
        repeat(baseIterations) { network.update() }

        // Perturb 10 nodes
        resNeurons.take(10).forEach { n -> n.activation = 1.0 }

        // Response window
        repeat(responseIterations) { network.update() }
    }

    network.addUpdateAction(updateAction("Record activations") {
        val u = inputNoise.sampleDouble()
        resNeurons.map{n -> n.addInputValue(u)}
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
                val newVariance = tf_stdev.text.toDouble()
                setVariance(newVariance)
            }
            addButton("Run one trial") {
                perturbAndRunNetwork()
                showSaveDialog("", "activations.csv") {
                    writeText(activations.toCsvString())
                }
            }

            addButton("Run one trial per parameter") {
                    val path = showDirectorySelectionDialog()
                    if (path != null) {
                        (5..95 step 5).forEach {
                            setVariance(it/100.0)
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