package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addTextWorld
import org.simbrain.custom_sims.couplingManager
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

    // Text World for Inputs
    val textWorldInputs = addTextWorld("Text World (Inputs)")
    val text = makeElmanVector(100)
    val tokenEmbedding = TokenEmbeddingBuilder().apply {
        embeddingType = EmbeddingType.ONE_HOT
    }.build(text)

    textWorldInputs.world.text = text
    textWorldInputs.world.tokenEmbedding = tokenEmbedding

    withGui {
        place(textWorldInputs) {
            location = point(0, 0)
            width = 450
            height = 250
        }
    }

    // Network
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network
    val srn = SRNNetwork(
        network,
        textWorldInputs.world.tokenEmbedding.dimension,
        50,
        textWorldInputs.world.tokenEmbedding.dimension,
        point(0,0))
    network.addNetworkModel(srn)

    val trainingInputs = makeElmanVector(100)
        .tokenizeWordsFromString()
        .map {
            tokenEmbedding.get(it)
        }.toTypedArray().toMatrix()

    val trainingTarget = trainingInputs.shiftUpAndPadEndWithZero()

    srn.trainingSet = MatrixDataset(trainingInputs, trainingTarget)
    srn.trainer.learningRate = 0.04
    srn.trainer.lossFunction = IterableTrainer.LossFunction.RootMeanSquaredError()
    repeat(10) {
        srn.trainer.trainOnce()
        println("${srn.trainer.lossFunction.name}: ${srn.trainer.lossFunction.loss}")
    }

    withGui {
        place(networkComponent) {
            location = point(460, 0)
            width = 500
            height = 550
        }
    }

    // // Text World for Outputs
    // val textWorldOut = addTextWorld("Text World (Outputs)")
    // textWorldOut.world.tokenEmbedding = tokenEmbedding
    //
    // withGui {
    //     place(textWorldOut) {
    //         location = point(0, 265)
    //         width = 450
    //         height = 250
    //     }
    // }


    // Couple the text world to neuron collection
    with(couplingManager) {
        createCoupling(
            textWorldInputs.world.getProducer("getCurrentVector"),
            srn.inputLayer.getConsumer("forceSetActivations")
        )
        // createCoupling(
        //     srn.getProducer("getOutputs"),
        //     textWorldOut.world.getConsumer("displayClosestWord")
        // )
    }

    workspace.addUpdateAction("Print Predicted Next Word") {
        val closestWord = tokenEmbedding.getClosestWord(srn.outputLayer.activations.toDoubleArray())
        println("Predicted Next Word: $closestWord")
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
    println(makeElmanVector(10))
}

