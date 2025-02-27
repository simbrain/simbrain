package org.simbrain.custom_sims.simulations
import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.createControlPanel
import org.simbrain.custom_sims.newSim
import org.simbrain.network.subnetworks.RestrictedBoltzmannMachine
import org.simbrain.util.place
import org.simbrain.util.runWithProgressWindow
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

    rbm.visibleLayer.labelArray = arrayOf(
        "ceiling", "large", "telephone", "books", "sofa", "drapes",
        "cupboard", "toilet", "walls", "medium", "bed", "desk-chair",
        "easy-chair", "stove", "sink", "scale", "door", "small",
        "typewriter", "clock", "coffee-cup", "coffeepot", "dresser", "oven",
        "windows", "very-small", "bookshelf", "picture", "ashtray", "refrigerator",
        "television", "computer", "very-large", "desk", "carpet", "floor-lamp",
        "fireplace", "toaster", "bathtub", "clothes-hanger"
    )

    fun activateNode(label: String) = rbm.visibleLayer.labelArray
        .indexOf(label)
        .let { index ->
            if (index < 0) null else index
        }?.let { index ->
            rbm.visibleLayer.activations[index, 0] = 1.0
        }

    fun activateNodes(labels: List<String>) {
        rbm.visibleLayer.clear()
        labels.forEach { activateNode(it) }
        rbm.visibleLayer.events.updated.fire()
    }

    fun flipBitWithChance(bit: Int, chance: Double): Int {
        return if (Random.nextDouble() < chance) 1 - bit else bit
    }

    withGui {
        place(networkComponent, 236, 10, 800, 600)
        createControlPanel("Control Panel", 5, 10) {
            addButton("Kitchen") {
                activateNodes(listOf(
                    "oven", "coffee-pot", "cupboard", "toaster", "refrigerator", "sink", "stove", "drapes",
                    "coffee-cup", "clock", "telephone", "small", "window", "walls", "ceiling"
                ))
            }
            addButton("Office") {
                activateNodes(listOf(
                    "computer", "ash-tray", "coffee-cup", "picture", "desk-chair", "books", "carpet",
                    "bookshelf", "typewriter", "telephone", "desk", "large", "door", "walls", "ceiling"
                ))
            }
            addButton("Bathroom") {
                activateNodes(listOf("scale", "toilet", "bathtub", "cupboard", "sink", "very-small", "door", "walls", "ceiling"))
            }
            addButton("Living Room") {
                activateNodes(listOf(
                    "television", "drapes", "fire-place", "easy-chair", "sofa", "floor-lamp", "picture",
                    "clock", "books", "carpet", "bookshelf", "telephone", "very-large", "window", "door",
                    "walls", "ceiling"
                ))
            }
            addButton("Bedroom") {
                activateNodes(listOf("coat-hanger", "television", "dresser", "drapes", "picture", "clock", "books",
                    "carpet", "bookshelf", "bed", "medium", "window", "door", "walls", "ceiling"
                ))
            }
            addSeparator()
            addButton("Train on Current Pattern") {
                with(network) {
                    runWithProgressWindow(20, batchSize = 10) {
                        rbm.trainOnCurrentPattern()
                    }
                }
            }
            addButton("Recall Current Pattern") {
                workspace.iterateSuspend(10)
            }
            addButton("Permute Current Pattern") {
                rbm.visibleLayer.activationArray = rbm.visibleLayer
                    .activationArray
                    .map { flipBitWithChance(it.toInt(), 0.1).toDouble() }
                    .toDoubleArray()
            }
        }
    }

}