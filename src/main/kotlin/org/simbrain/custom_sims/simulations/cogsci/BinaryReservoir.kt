package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.awaitAll
import org.simbrain.custom_sims.*
import org.simbrain.network.connections.FixedDegree
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.addToNetwork
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.updaterules.BinaryRule
import org.simbrain.util.*
import org.simbrain.util.Utils.FS
import org.simbrain.util.stats.distributions.NormalDistribution
import org.simbrain.util.stats.distributions.TwoValued
import org.simbrain.workspace.updater.updateAction
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

    // Neurons in the reservoir
    val numNeurons = 250

    // Fan-in to each neuron
    val k = 4

    // Weight variance
    var variance = .1

    // Times to iterate before perturbation
    val baseIterations = 400

    // What percent of neurons to perturb
    val percentToPerturb = .1
    val numNodesToPerturb = (percentToPerturb * numNeurons).toInt()

    // Times to iterate after perturbation
    val responseIterations = 300

    // Noise used to generate u-bar
    val inputNoise = TwoValued(-1.0, 1.0, .5)

    // Distribution used to generate synapses
    var normalDist = NormalDistribution(0.0, 1.0)

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
        Neuron(rule)
    }
    network.addNetworkModels(resNeurons).awaitAll()
    val reservoir = NeuronCollection(resNeurons)
    network.addNetworkModel(reservoir)?.await()
    reservoir.label = "Reservoir"
    reservoir.layout(GridLayout())
    reservoir.location = point(0, 0)

    val conn = FixedDegree(degree = k)
    conn.connectNeurons(resNeurons, resNeurons).addToNetwork(network)

    /**
     * Resets the variance by rescaling synapses. Assumes valid variance to begin with.
     */
    fun setVariance(newVariance: Double) {
        normalDist.standardDeviation = sqrt(newVariance)
        network.flatSynapseList.forEach { synapse ->
            synapse.strength = normalDist.sampleDouble()
        }
        variance = newVariance

        val av = network.flatSynapseList.map { it.strength }.toDoubleArray().variance
        println("Variance set to ${variance}; Actual variance: ${av}")

    }
    setVariance(variance)

    fun perturbAndRunNetwork(network: Network) {

        println("Variance: ${variance}")

        // Clear the network and activations
        reservoir.randomize()
        activations.clear()

        // Baseline window
        repeat(baseIterations) { network.update() }

        // Perturb nodes
        resNeurons.take(numNodesToPerturb).forEach { n -> n.addInputValue(1.0) }

        // Response window
        repeat(responseIterations) { network.update() }
    }

    network.addUpdateAction(updateAction("Record activations") {
        val u = inputNoise.sampleDouble()
        resNeurons.map { n -> n.addInputValue(u) }
        activations.add(resNeurons.map { n -> n.activation })
        // println("" + activations.last() + ",")
    })

    withGui {
        place(networkComponent) {
            location = point(0, 0)
            width = 567
            height = 617
        }

        createControlPanel("Control Panel", 567, 0) {
            val tf_stdev: JTextField = addTextField("Weight stdev", "" + variance)
            addComponent(tf_stdev)
            addButton("Apply Variance") {
                val newVariance = tf_stdev.text.toDouble()
                setVariance(newVariance)
            }
            addButton("Run one trial") {
                perturbAndRunNetwork(network)
                showSaveDialog("", "activations.csv") {
                    writeText(activations.toCsvString())
                }
            }


            addButton("Run one trial per parameter") {
                val path = showDirectorySelectionDialog()
                if (path != null) {
                    createGeometricProgression(.1, 1.25)
                        .takeWhile { it < 10 }
                        .forEach {
                            setVariance(it)
                            perturbAndRunNetwork(network)
                            // println("${variance}: ${activations}")
                            File(path + FS + "activations${it}.csv").writeText(activations.toCsvString())
                        }
                }

            }

        }

    }

    // Location of the projection in the desktop
    val projectionPlot = addProjectionPlot("Activations")
    withGui {
        place(projectionPlot) {
            location = point(567, 197)
            width = 452
            height = 420
        }
    }

    addSidebarInfo(
        """
        # Binary Reservoir
        
        This simulation shows how research can be done in Simbrain and in coordination with other programming environments. Though this simulation can be 
        used directly in any way as desired, pressing the buttons can configure parameters, run simulations, and save data that is outputted to a `.csv` file 
        which can then be analyzed in other programming environments like Python. For more information on the `Binary Reservoir` simulation and its usage, 
        feel free to contact jponcedeleon@ucmerced.edu.

        ## Simulation Details
        
        In this simulation, each trial resets the node connection parameters, and after `400` baseline iterations, it models a perturbation to the network by adding 
        an additional input signal to `10%` of the nodes, and continues to update activations for an additional `300` iterations to show the reservoir's response to new input
        (these and other parameters can be configured in the [simulation code](https://docs.simbrain.net/docs/simulations/)). This perturbation allows for the calculation of a 
        measure called the **perturbational complexity index (PCI)**, which is associated with consciousness in humans and other animals [[1](https://pubmed.ncbi.nlm.nih.gov/31133480/)]. 
        In this simulation, the relationship between PCI and edge-of-chaos activation dynamics is studied in a binary reservoir network.

        The `run one trial` button produces one file for the specified `weight stdev`. You can also see the set variance and the actual variance in your programming environment
        after you apply the `weight stdev`. Whereas, the `run one trial per parameter` button will produce a set of files, one for each mean weight variance of a geometric 
        progression (from `0.1` to `10`). This makes it possible to study how PCI varies with `weight variance`, which is known to be a control parameter for putting networks 
        into different dynamical regimes, from `ordered` to `chaotic` and in between `edge of chaos`. For an in-depth description and illustration of the different dynamical 
        regimes, refer to the `Edge of Chaos bitstream` simulation.
        
        # What to Do
        
        In this simulation, similarly to the other reservoir network simulations, the only configuration to the simulation is the `weight stdev`. Below are two different ways
        that you can utilize this simulation.
        
        ## Observing The Edge of Chaos
        
        1) Change the `weight stdev` value and press the `Apply Variance` button to change the reservoir's weight distribution.
                        
        2) Start the simulation by clicking on the `run` button.
        
        3) Now, observe the changes in the PCA plot and in its activation patterns to determine its current dynamical state.   
        
        5) To reset the simulation, stop the simulation by clicking the `run` button again, and click on the reservoir, then press `k`. Afterwards, press the `clear` button in the 
        PCA plot to clear the points.
        
        6) Afterwards, click back on the `cursor` button, and left-click outside of the reservoir to unselect all neurons.

        ## Conducting Research on the Relationship between PCI and Dynamical Regimes
        
        1) Change the `weight stdev` value and press the `Apply Variance` button to change the reservoir's weight distribution.
        
        2) Click the `run one trial` button or, the `run one trial per parameter` button.
        
        3) Save it to your device after the simulation finishes.  
        
        ### References
        
        1) Comolatti, R., Pigorini, A., Casarotto, S., Fecchio, M., Faria, G., Sarasso, S., Rosanova, M., Gosseries, O., Boly, M., Bodart, O., Ledoux, D., Brichant, J. F., Nobili, L., Laureys, S., Tononi, G., Massimini, M., & Casali, A. G. (2019). [A fast and general method to empirically estimate the complexity of brain responses to transcranial and intracranial stimulations](https://doi.org/10.1016/j.brs.2019.05.013). _Brain stimulation_, _12_(5), 1280–1289. 
        
        ### Credits
        
        [Jeff Yoshimi](https://jeffyoshimi.net/index.html), Kanly Thao, and Sergio Ponce de Leon.
        
        """.trimIndent()
    )

    // Couple the neuron array to the projection plot
    with(couplingManager) {
        reservoir couple projectionPlot
    }

}