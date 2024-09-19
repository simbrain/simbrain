package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addTextWorld
import org.simbrain.custom_sims.newSim
import org.simbrain.network.subnetworks.SRNNetwork
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

    // Word embedding
    val allWords = listOf(noun_hum, noun_anim, noun_inanim, noun_agress, noun_frag, noun_food, verb_intran, verb_tran, verb_agpat, verb_percept, verb_destroy, verb_eat)
        .flatten().distinct().joinToString(" ")
    val tokenEmbedding = TokenEmbeddingBuilder().apply {
        embeddingType = EmbeddingType.ONE_HOT
    }.build(allWords)

    // Text World for Inputs
    val textWorldInputs = addTextWorld("Text World (Inputs)").apply { updateOn = false }
    val text = makeElmanVector(numInputSentences)
    textWorldInputs.world.text = text
    textWorldInputs.world.tokenEmbedding = tokenEmbedding

    // Text World for Outputs
    val textWorldOut = addTextWorld("Text World (Outputs)").apply { updateOn = false }
    TokenEmbeddingBuilder().build(text)

    // Network
    val networkComponent = addNetworkComponent("Network").apply { updateOn = false }
    val network = networkComponent.network
    val srn = SRNNetwork(
        textWorldInputs.world.tokenEmbedding.dimension,
        150,
        textWorldInputs.world.tokenEmbedding.dimension,
        point(0,0))
    network.addNetworkModel(srn)?.await()

    val trainingInputsTokens = makeElmanVector(numTrainingSentences)
        .tokenizeWordsFromString()

    val trainingInputs = trainingInputsTokens
        .map {
            tokenEmbedding.get(it)
        }.toTypedArray().toMatrix()

    val trainingTargetTokens = trainingInputsTokens.drop(1)
    val trainingTarget = trainingInputs.shiftUpAndPadEndWithZero()

    srn.trainingSet = MatrixDataset(
        trainingInputs,
        trainingTarget,
        inputRowNames = trainingInputsTokens,
        inputColumnNames = textWorldInputs.world.tokenEmbedding.tokens,
        targetRowNames = trainingTargetTokens,
        targetColumnNames = textWorldInputs.world.tokenEmbedding.tokens
    )
    srn.trainer.learningRate = learningRate

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

    workspace.addUpdateAction("Update Inputs") {
        textWorldInputs.update()
    }

    workspace.addUpdateAction("Set Current Word as Input Activations") {
        val currentVector = textWorldInputs.world.currentVector
        srn.inputLayer.setActivations(currentVector)
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

    workspace.launch {
        delay(10L)
        workspace.iterateSuspend(1)
    }

}

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

fun makeElmanVector(numSentences: Int): String {

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

