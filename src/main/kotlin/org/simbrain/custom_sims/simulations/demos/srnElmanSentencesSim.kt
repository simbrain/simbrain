package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addTextWorld
import org.simbrain.custom_sims.newSim
import org.simbrain.network.subnetworks.SRNNetwork
import org.simbrain.network.trainers.IterableTrainer
import org.simbrain.network.trainers.MatrixDataset
import org.simbrain.util.*
import org.simbrain.world.textworld.EmbeddingType
import org.simbrain.world.textworld.TokenEmbeddingBuilder


/**
 * Simulate temporal xor in a simple recurrent network as described by Elman (1990).
 */
val srnElmanSentences = newSim {

    workspace.clearWorkspace()

    val numInputSentences = 100
    val numTrainingSentences = 1000 // 10,000 in Elman's paper
    val learningRate = .04

    // Text World for Inputs
    val textWorldInputs = addTextWorld("Text World (Inputs)")
    val text = makeElmanVector(numInputSentences)
    val tokenEmbedding = TokenEmbeddingBuilder().apply {
        embeddingType = EmbeddingType.ONE_HOT
    }.build(text)

    textWorldInputs.world.text = text
    textWorldInputs.world.tokenEmbedding = tokenEmbedding

    // Text World for Outputs
    val textWorldOut = addTextWorld("Text World (Outputs)")
    TokenEmbeddingBuilder().build(text)

    // Network
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network
    val srn = SRNNetwork(
        network,
        textWorldInputs.world.tokenEmbedding.dimension,
        150,
        textWorldInputs.world.tokenEmbedding.dimension,
        point(0,0))
    network.addNetworkModel(srn)

    val trainingInputs = makeElmanVector(numTrainingSentences)
        .tokenizeWordsFromString()
        .map {
            tokenEmbedding.get(it)
        }.toTypedArray().toMatrix()

    val trainingTarget = trainingInputs.shiftUpAndPadEndWithZero()

    srn.trainingSet = MatrixDataset(trainingInputs, trainingTarget)
    srn.trainer.learningRate = learningRate
    srn.trainer.lossFunction = IterableTrainer.LossFunction.RootMeanSquaredError()

    // Comment this out to pretrain the network
    // From the original paper: "The training continued in this manner until the network had experienced 6 complete passes
    // through the sequence."
    // repeat(6) {
    //     srn.trainer.trainOnce()
    //     println("${srn.trainer.lossFunction.name}: ${srn.trainer.lossFunction.loss}")
    // }

    withGui {
        place(textWorldInputs, 0, 0, 450, 250)
        place(textWorldOut, 0, 265, 450, 350)
        place(networkComponent, 460, 0, 500, 550)
    }

    workspace.updater.updateManager.clear()

    workspace.addUpdateAction("Update Inputs") {
        textWorldInputs.update()
    }

    workspace.addUpdateAction("Set Current Word as Input Activations") {
        val currentVector = textWorldInputs.world.currentVector
        srn.inputLayer.forceSetActivations(currentVector)
    }

    workspace.addUpdateAction("Update Network") {
        networkComponent.update()
    }

    workspace.addUpdateAction("Write Predicted Next Word to Output") {
        val totalActivations = srn.outputLayer.activations.toDoubleArray().sum()
        val choices = srn.outputLayer.activations.toDoubleArray().mapIndexed { index, d -> index to d }
            .sortedByDescending { (_, d) -> d }
            .take(5)
        val chosenWords = choices.map { (index, _) -> tokenEmbedding.tokens[index] }
        val chosenProbs = choices.map { (_, d) -> d / totalActivations }
        val chosen = chosenWords.zip(chosenProbs).joinToString(" ") { (word, prob) -> "$word (${prob.format(3)})" }
        textWorldOut.world.addTextAtEnd(
            """
                |Current Word: ${textWorldInputs.world.currentToken}
                |Predicted Next Words: $chosen
                |
                |
            """.trimMargin("|"),
            ""
        )
    }

}

fun makeElmanVector(numSentences: Int): String {
    val noun_hum = listOf("man", "woman")
    val noun_anim = listOf("cat", "mouse")
    val noun_inanim = listOf("book", "rock")
    val noun_agress = listOf("dragon", "monster")
    val noun_frag = listOf("glass", "plate")
    val noun_food = listOf("cookie", "bread")
    val verb_intran = listOf("think", "sleep")
    val verb_tran = listOf("see", "chase")
    val verb_agpat = listOf("move", "break")
    val verb_percept = listOf("see", "smell")
    val verb_destroy = listOf("break", "smash")
    val verb_eat = listOf("eat")

    val templates = listOf(
        listOf(noun_hum, verb_eat, noun_food),
        listOf(noun_hum, verb_percept, noun_inanim),
        listOf(noun_hum, verb_destroy, noun_frag),
        listOf(noun_hum, verb_intran),
        listOf(noun_hum, verb_tran, noun_hum),
        listOf(noun_hum, verb_agpat, noun_inanim),
        listOf(noun_hum, verb_agpat),
        listOf(noun_anim, verb_eat, noun_food),
        listOf(noun_anim, verb_tran, noun_anim),
        listOf(noun_anim, verb_agpat, noun_inanim),
        listOf(noun_anim, verb_agpat),
        listOf(noun_inanim, verb_agpat),
        listOf(noun_agress, verb_destroy, noun_frag),
        listOf(noun_agress, verb_eat, noun_hum),
        listOf(noun_agress, verb_eat, noun_anim),
        listOf(noun_agress, verb_eat, noun_food)
    )

    return templates.sampleWithReplacement()
        .take(numSentences)
        .map { template -> template.joinToString(" ") { wordType -> wordType.sampleOne() } }
        .joinToString("\n")

}
fun main() {
    // For testing makeElmanVector
    println(makeElmanVector(10))
}

