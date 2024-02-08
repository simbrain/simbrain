package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.network.connections.Sparse
import org.simbrain.network.core.*
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.layouts.LineLayout
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.projection.PCAProjection
import org.simbrain.util.stats.distributions.NormalDistribution
import org.simbrain.workspace.updater.UpdateComponent
import org.simbrain.workspace.updater.UpdateCoupling
import java.util.*

/**
 * See https://www.sciencedirect.com/science/article/pii/S0006899321004352
 */
val allostaticPatternCompletion = newSim {

    // TODOS:
    // - Encapsulate state machine into a class

    // STATE MACHINE
    class Transition(val from: String, val to: String, val probability: Double)

    fun List<Transition>.sampleFirst(): String {
        val randomIndex = Random().nextInt(size)
        return this[randomIndex].from
    }

    fun List<Transition>.sampleNext(firstState: String): String {
        var cumulativeProb = 0.0
        val randNum = Random().nextDouble()
        filter { it.from == firstState }.forEach {
            cumulativeProb += it.probability
            if (randNum < cumulativeProb) return it.to
        }
        throw IllegalStateException("ERROR: Check that transition probabilities from source state add to 1")
    }

    val nounVerbTransitions = listOf(
        Transition("man", "walks", 0.75),
        Transition("man", "bites", 0.25),
        Transition("dog", "walks", 0.25),
        Transition("dog", "bites", 0.75)
    )

    val verbNounTransitions = listOf(
        Transition("walks", "dog", 0.75),
        Transition("walks", "man", 0.25),
        Transition("bites", "dog", 0.25),
        Transition("bites", "man", 0.75)
    )

    // Create the input sequence
    val inputSequence = mutableListOf<String>()
    repeat(1000) {
        val firstWord = nounVerbTransitions.sampleFirst()
        inputSequence.add("${firstWord}_S")
        val secondWord = nounVerbTransitions.sampleNext(firstWord)
        inputSequence.add("${secondWord}_V")
        val thirdWord = verbNounTransitions.sampleNext(secondWord)
        inputSequence.add("${thirdWord}_O")
        val sentenceBreak = "END"
        inputSequence.add(sentenceBreak)
    }

    // NETWORK

    // Number of reservoir neurons
    val numResNeurons = 100

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Allostatic Pattern Completion")
    val network = networkComponent.network

    // Reservoir
    val reservoir = List(numResNeurons) {
        Neuron(AllostaticUpdateRule())
    }.let {
        network.addNetworkModelsAsync(it)
        NeuronCollection(it)
    }.apply {
        label = "Reservoir"
        location = point(0, 0)
    }
    network.addNetworkModel(reservoir)
    reservoir.layout(GridLayout())
    val sparse = Sparse()
    sparse.connectionDensity = .1
    val reservoirSynapseGroup = SynapseGroup(reservoir, reservoir, sparse)
    val dist = NormalDistribution(0.0, 1.0)
    reservoirSynapseGroup.synapses.forEach { s ->
        s.strength = dist.sampleDouble()
    }
    network.addNetworkModelAsync(reservoirSynapseGroup)

    // Input nodes
    val inputs = network.addNeuronCollectionAsync(5) {
        updateRule = LinearRule()
        network.addNetworkModelAsync(this)
    }.apply {
        label = "Inputs"
        setLabels(listOf("man", "dog", "walks", "bites", "END"))
        setClamped(true)
        layout(LineLayout())
        location = point(-550, 0)
    }

    // Connect input nodes to reservoir
    val inputsToRes = SynapseGroup(inputs, reservoir, sparse)
    inputsToRes.label = "Inputs to Res"
    network.addNetworkModelAsync(inputsToRes)
    inputsToRes.synapses.forEach { s ->
        s.strength = 5.0
    }

    // Location of the network in the desktop
    withGui {
        place(networkComponent) {
            location = point(385, 0)
            width = 800
            height = 600
        }
    }

    // TEXT WORLD
    val textWorld = addTextWorld("Text World")
    textWorld.world.text = inputSequence.joinToString(" ")

    withGui {
        place(textWorld) {
            location = point(0, 0)
            width = 400
            height = 500
        }
    }

    // PCA
    val pca = addProjectionPlot2("Activations")
    pca.projector.tolerance = .2
    pca.projector.projectionMethod = PCAProjection()
    withGui {
        place(pca) {
            location = point(143, 200)
            width = 500
            height = 500
        }
    }

    val textCoupling = with(couplingManager) {
        val currentWord = textWorld.world.getProducer("getCurrentToken")
        val pcaLabel = pca.getConsumer("setLabel")
        couplingManager.createCoupling(currentWord, pcaLabel)
    }

    // Reservoir to plot coupling. Comment / uncomment depending on whether activations or spikes are used.
    val activationPCACoupling = couplingManager.createCoupling(reservoir, pca)
    var spikes: DoubleArray? = null

    // CUSTOM UPDATE

    val inputEncodings = mapOf(
        "man" to doubleArrayOf(1.0, 0.0, 0.0, 0.0, 0.0),
        "dog" to doubleArrayOf(0.0, 1.0, 0.0, 0.0, 0.0),
        "walks" to doubleArrayOf(0.0, 0.0, 1.0, 0.0, 0.0),
        "bites" to doubleArrayOf(0.0, 0.0, 0.0, 1.0, 0.0),
        "END" to doubleArrayOf(0.0, 0.0, 0.0, 0.0, 1.0)
    )
    val zeroInput = doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0)


    network.updateManager.addAction(0, updateAction("Set inputs") {
        var word = textWorld.world.currentItem?.text
        if (!word.equals("END")) {
            word = word?.dropLast(2)
        }
        inputs.forceSetActivations(inputEncodings[word] ?: zeroInput)
        spikes = with(network) {
            reservoir.neuronList.map {
                if (it.isSpike) 1.0 else 0.0
            }.toDoubleArray()
        }
    })

    workspace.updater.updateManager.clear()
    workspace.updater.updateManager.addAction(UpdateComponent(textWorld))
    workspace.updater.updateManager.addAction(UpdateComponent(networkComponent))
    workspace.updater.updateManager.addAction(updateAction("Send spikes to pca") {
        spikes?.let { pca.addPoint(it) }
    })
    // workspace.updater.updateManager.addAction(UpdateCoupling(activationPCACoupling))
    workspace.updater.updateManager.addAction(UpdateCoupling(textCoupling))
    workspace.updater.updateManager.addAction(UpdateComponent(pca))
}

