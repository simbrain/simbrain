package org.simbrain.custom_sims.simulations
import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.createControlPanel
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.addNeuronCollection
import org.simbrain.network.core.labels
import org.simbrain.network.subnetworks.RestrictedBoltzmannMachine
import org.simbrain.network.util.Alignment
import org.simbrain.network.util.Direction
import org.simbrain.network.util.alignNetworkModels
import org.simbrain.network.util.offsetNetworkModel
import org.simbrain.util.place
import org.simbrain.util.runWithProgressWindow
import org.simbrain.util.toDoubleArray
import kotlin.random.Random

/**
 * Demo for studying Room Schema From PDP Chapter 14.
 */

val roomSchemaSim = newSim {

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Competitive network
    val rbm = RestrictedBoltzmannMachine(42, 64)
    network.addNetworkModel(rbm)?.await()
    rbm.visibleLayer.circleMode = true
    rbm.visibleLayer.offset(-100.0, 0.0)

    // Neuron Collection and Its Configurations
    val nc = network.addNeuronCollection(42).apply {
        setUpperBound(1.0)
        setLowerBound(0.0)
        isAllClamped = true
        applyLayout(5, 8)
    }
    nc.offset(-400.0, -580.0)

    nc.neuronList.labels = listOf(
        "ceiling", "large", "telephone", "books", "sofa", "drapes",
        "cupboard", "toilet", "walls", "medium", "bed", "desk-chair",
        "easy-chair", "stove", "sink", "scale", "door", "small",
        "typewriter", "clock", "coffee-cup", "coffeepot", "dresser", "oven",
        "windows", "very-small", "bookshelf", "picture", "ashtray", "refrigerator",
        "television", "computer", "very-large", "desk", "carpet", "floor-lamp",
        "fireplace", "toaster", "bathtub", "clothes-hanger"
    )

    fun syncNeuronCollectionToRBM() {
        rbm.visibleLayer.activations = nc.activations
    }

    fun syncRBMToNeuronCollection() {
        nc.activationArray = rbm.visibleLayer.activations.toDoubleArray()
    }

    workspace.addUpdateAction("Sync RBM to Neuron Collection") {
        syncRBMToNeuronCollection()
    }


    fun flipBitWithChance(bit: Int, chance: Double): Int {
        return if (Random.nextDouble() < chance) 1 - bit else bit
    }

    withGui {
        place(networkComponent, 236, 10, 600, 800)
        createControlPanel("Control Panel", 5, 10) {
            addButton("Kitchen") {
                nc.clear()
                rbm.visibleLayer.clear()
                listOf(
                    "oven", "coffee-pot", "cupboard", "toaster", "refrigerator", "sink", "stove", "drapes",
                    "coffee-cup", "clock", "telephone", "small", "window", "walls", "ceiling"
                ).mapNotNull { nc.getNeuronByLabel(it) }.forEach { it.activation = 1.0 }
                syncNeuronCollectionToRBM()
            }
            addButton("Office") {
                nc.clear()
                rbm.visibleLayer.clear()
                listOf(
                    "computer", "ash-tray", "coffee-cup", "picture", "desk-chair", "books", "carpet",
                    "bookshelf", "typewriter", "telephone", "desk", "large", "door", "walls", "ceiling"
                ).forEach {
                    nc.getNeuronByLabel(it)?.activation = 1.0
                }
                syncNeuronCollectionToRBM()
            }
            addButton("Bathroom") {
                nc.clear()
                rbm.visibleLayer.clear()
                listOf("scale", "toilet", "bathtub", "cupboard", "sink", "very-small", "door", "walls", "ceiling").forEach {
                    nc.getNeuronByLabel(it)?.activation = 1.0
                }
                syncNeuronCollectionToRBM()
            }
            addButton("Living Room") {
                nc.clear()
                rbm.visibleLayer.clear()
                listOf(
                    "television", "drapes", "fire-place", "easy-chair", "sofa", "floor-lamp", "picture",
                    "clock", "books", "carpet", "bookshelf", "telephone", "very-large", "window", "door",
                    "walls", "ceiling"
                ).forEach {
                    nc.getNeuronByLabel(it)?.activation = 1.0
                }
                syncNeuronCollectionToRBM()
            }
            addButton("Bedroom") {
                nc.clear()
                rbm.visibleLayer.clear()
                listOf("coat-hanger", "television", "dresser", "drapes", "picture", "clock", "books",
                    "carpet", "bookshelf", "bed", "medium", "window", "door", "walls", "ceiling"
                ).forEach {
                    nc.getNeuronByLabel(it)?.activation = 1.0
                }
                syncNeuronCollectionToRBM()
            }
            addSeparator()
            addButton("Train on Current Pattern") {
                syncNeuronCollectionToRBM()
                with(network) {
                    runWithProgressWindow(20, batchSize = 10) {
                        rbm.trainOnCurrentPattern()
                    }
                }
            }
            addButton("Recall Current Pattern") {
                syncNeuronCollectionToRBM()
                workspace.iterateSuspend(10)
                syncRBMToNeuronCollection()
            }
            addButton("Permute Current Pattern") {
                nc.neuronList.forEach{ it.activation = flipBitWithChance(it.activation.toInt(), .1).toDouble()  }
            }
        }
    }

}