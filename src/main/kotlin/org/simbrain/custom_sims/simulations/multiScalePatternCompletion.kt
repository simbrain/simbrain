package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.newSim
import org.simbrain.custom_sims.updateAction
import org.simbrain.network.connections.Sparse
import org.simbrain.network.core.SynapseGroup2
import org.simbrain.network.core.addNeuronCollection
import org.simbrain.network.groups.NeuronCollection
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.layouts.LineLayout
import org.simbrain.network.neuron_update_rules.LinearRule
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.stats.distributions.NormalDistribution
import java.util.*

/**
 * See https://www.sciencedirect.com/science/article/pii/S0006899321004352
 */
val multiScalePatternCompletion = newSim {

    // TODOS:
    // - Encapsulate state machine into a class
    // - Put inputs in text world with couplings

    class Transition(val from: String, val to: String, val probability: Double)

    fun List<Transition>.sampleFirst(): String {
        val randomIndex = Random().nextInt(size)
        return this[randomIndex].from
    }

    fun List<Transition>.sampleNext(firstState: String): String {
        var cumulativeProb = 0.0
        val randNum = Random().nextDouble()
        filter { it.from == firstState}.forEach {
            cumulativeProb += it.probability
            if (randNum < cumulativeProb ) return it.to
        }
        throw IllegalStateException("ERROR: Check that transition probabilities from source state add to 1")
    }

    val nounVerbTransitions = listOf(
        Transition("man", "walks", 0.75),
        Transition("man", "bites", 0.25),
        Transition("dog", "walks", 0.25),
        Transition("dog", "bites", 0.75))

    val verbNounTransitions = listOf(
        Transition("walks", "dog", 0.75),
        Transition("walks", "man", 0.25),
        Transition("bites", "dog", 0.25),
        Transition("bites", "man", 0.75))

    // Create the input sequence
    val inputSequence = mutableListOf<String>()
    repeat(10) {
        val firstWord = nounVerbTransitions.sampleFirst()
        inputSequence.add(firstWord)
        val secondWord = nounVerbTransitions.sampleNext(firstWord)
        inputSequence.add(secondWord)
        val thirdWord = verbNounTransitions.sampleNext(secondWord)
        inputSequence.add(thirdWord)
        // println("$firstWord $secondWord $thirdWord")
    }

    // NETWORK

    // Number of reservoir neurons
    val numResNeurons = 100

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Multi Scale Pattern Completion")
    val network = networkComponent.network

    // Reservoir
    val reservoir = List(numResNeurons) {
        AllostaticNeuron(network, AllostaticUpdateRule())
    }.let {
        network.addNetworkModels(it)
        NeuronCollection(network, it)
    }.apply {
        label = "Reservoir"
        layout(GridLayout())
        location = point(0, 0)
    }
    network.addNetworkModel(reservoir)
    val sparse = Sparse()
    sparse.connectionDensity = .1
    val reservoirSynapseGroup = SynapseGroup2(reservoir, reservoir, sparse)
    val dist = NormalDistribution(1.0, .1)
    reservoirSynapseGroup.synapses.forEach { s ->
        s.strength = dist.sampleDouble()
    }
    network.addNetworkModel(reservoirSynapseGroup)?.join()

    // Input nodes
    val inputs = network.addNeuronCollection(5) {
        updateRule = LinearRule()
        network.addNetworkModel(this)
    }.apply {
        label = "Inputs"
        setClamped(true)
        layout(LineLayout())
        location = point(-550, 0)
    }

    // Connect input nodes to reservoir
    val inputsToRes = SynapseGroup2(inputs, reservoir, sparse)
    inputsToRes.label = "Inputs to Res"
    network.addNetworkModel(inputsToRes)
    inputsToRes.synapses.forEach { s ->
        s.strength = 0.75
    }

    // Location of the network in the desktop
    withGui {
        place(networkComponent) {
            location = point(0, 0)
            width = 800
            height = 600
        }
    }

    val inputEncodings = mapOf(
        "man"   to doubleArrayOf(1.0, 0.0, 0.0, 0.0),
        "dog"   to doubleArrayOf(0.0, 1.0, 0.0, 0.0),
        "walks" to doubleArrayOf(0.0, 0.0, 1.0, 0.0),
        "bites" to doubleArrayOf(0.0, 0.0, 0.0, 1.0)
    )

    var wordIndex = 0
    network.updateManager.addAction(0, updateAction("Set inputs") {
        val word = inputSequence[wordIndex++ % inputSequence.size]
        println(word)
        inputs.forceSetActivations(inputEncodings[word])
    })
}

