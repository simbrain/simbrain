package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.createControlPanel
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.addNeuronCollection
import org.simbrain.network.core.connect
import org.simbrain.network.subnetworks.RestrictedBoltzmannMachine
import org.simbrain.util.place
import org.simbrain.util.plus
import org.simbrain.util.stats.distributions.NormalDistribution
import org.simbrain.util.toMatrix
import smile.math.matrix.Matrix

/**
 * Demo for studying Room Schema From PDP Chapter 14.
 */
val roomSchemaSim = newSim {

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Competitive network
    val rbm = RestrictedBoltzmannMachine(64, 25)
    network.addNetworkModel(rbm)

    // Neuron Collection
    val nc = network.addNeuronCollection(40)

    //Connecting Neuron Collection to Competitive Input Layer
    connect(nc, rbm.visibleLayer)

    // Labeling the neuron collection
    nc.neuronList[0].label = "ceiling"
    nc.neuronList[1].label = "large"
    nc.neuronList[2].label = "telephone"
    nc.neuronList[3].label = "books"
    nc.neuronList[4].label = "sofa"
    nc.neuronList[5].label = "drapes"
    nc.neuronList[6].label = "cupboard"
    nc.neuronList[7].label = "toilet"
    nc.neuronList[8].label = "walls"
    nc.neuronList[9].label = "medium"
    nc.neuronList[10].label = "bed"
    nc.neuronList[11].label = "desk-chair"
    nc.neuronList[12].label = "easy-chair"
    nc.neuronList[13].label = "stove"
    nc.neuronList[14].label = "sink"
    nc.neuronList[15].label = "scale"
    nc.neuronList[16].label = "door"
    nc.neuronList[17].label = "small"
    nc.neuronList[18].label = "typewriter"
    nc.neuronList[19].label = "clock"
    nc.neuronList[21].label = "coffee-cup"
    nc.neuronList[22].label = "coffeepot"
    nc.neuronList[20].label = "dresser"
    nc.neuronList[23].label = "oven"
    nc.neuronList[24].label = "windows"
    nc.neuronList[25].label = "very-small"
    nc.neuronList[26].label = "bookshelf"
    nc.neuronList[27].label = "picture"
    nc.neuronList[28].label = "ashtray"
    nc.neuronList[29].label = "refrigerator"
    nc.neuronList[30].label = "television"
    nc.neuronList[31].label = "computer"
    nc.neuronList[32].label = "very-large"
    nc.neuronList[33].label = "desk"
    nc.neuronList[34].label = "carpet"
    nc.neuronList[35].label = "floor-lamp"
    nc.neuronList[36].label = "fireplace"
    nc.neuronList[37].label = "toaster"
    nc.neuronList[38].label = "bathtub"
    nc.neuronList[39].label = "clothes-hanger"

    // Layout
    nc.applyLayout(8, 5)

    // Moving nc below or to the side of the RBM sim
    nc.locationX = -150.0
    nc.locationY = 400.0



    // Inputs
    val input1 = doubleArrayOf(1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0 )
    val input2 = doubleArrayOf(1.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 1.0 )
    val input3 = doubleArrayOf(1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0 )
    val input4 = doubleArrayOf(0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0 )
    val input5 = doubleArrayOf(1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0 )
    val input6 = doubleArrayOf(0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0)

    // Open with one set of activations (Pattern 1)
    // Open with one set of activations (Pattern 1)
    rbm.visibleLayer.setActivations(input1)

    // Set training set of rbm to these inputs
    rbm.inputData = Matrix.of(arrayOf(input1, input2, input3, input4, input5, input6))

    withGui {
        place(networkComponent, 139, 10, 1600, 900)
        createControlPanel("Control Panel", 5, 10) {
            addButton("Pattern 1") {
                rbm.visibleLayer.activations = input1.toMatrix()
            }
            addButton("Pattern 2") {
                rbm.visibleLayer.activations = input2.toMatrix()
            }
            addButton("Pattern 3") {
                rbm.visibleLayer.activations = input3.toMatrix()
            }
            addButton("Pattern 4") {
                rbm.visibleLayer.activations = input4.toMatrix()
            }
            addButton("Pattern 5") {
                rbm.visibleLayer.activations = input5.toMatrix()
            }
            addButton("Pattern 6") {
                rbm.visibleLayer.activations = input6.toMatrix()
            }
            addButton("Add noise") {
                rbm.visibleLayer.activations += NormalDistribution(standardDeviation = .1)
                    .sampleDouble(rbm.visibleLayer.size())
                    .toMatrix()
            }
        }

    }
}