package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import org.simbrain.custom_sims.*
import org.simbrain.network.desktop.NetworkDesktopComponent
import org.simbrain.network.gui.nodes.SubnetworkNode
import org.simbrain.network.subnetworks.SRNNetwork
import org.simbrain.network.trainers.BasicOptimizer
import org.simbrain.network.trainers.TrainingDataset
import org.simbrain.util.*
import org.simbrain.world.textworld.EmbeddingType
import org.simbrain.workspace.gui.SimbrainDesktop
import org.simbrain.world.textworld.TokenEmbeddingBuilder
import java.awt.geom.Rectangle2D


/**
 * Next-word prediction on simple generated sentences in a simple recurrent network, after Elman (1990).
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
        embeddingType = EmbeddingType.OneHot()
    }.build(allWords)

    // Text World for Inputs
    val textWorldInputs = addTextWorld("Text World (inputs)").apply {
        updateOn = false
        world.autoAdvance = true
    }
    val text = makeElmanVector(numInputSentences)
    textWorldInputs.world.tokenEmbedding = tokenEmbedding
    textWorldInputs.world.text = text


    // Text World for Outputs
    val textWorldOut = addTextWorld("Text World (outputs)").apply { updateOn = false }
    textWorldOut.world.showTokenBoundaries = false
    textWorldOut.world.highlightCurrentToken = false
    TokenEmbeddingBuilder().build(text)

    // Network
    val networkComponent = addNetworkComponent("Network").apply { updateOn = false }
    val network = networkComponent.network
    val srn = SRNNetwork(
        textWorldInputs.world.tokenEmbedding.dimension,
        150,
        textWorldInputs.world.tokenEmbedding.dimension,
        point(0,0))
    network.addNetworkModel(srn)

    // Initialize weights with Xavier initialization to prevent saturation
    srn.initWeights()

    val trainingInputsTokens = makeElmanVector(numTrainingSentences)
        .tokenizeWordsFromString()

    val trainingInputs = trainingInputsTokens
        .map {
            tokenEmbedding.get(it)
        }.toTypedArray().toMatrix()

    val trainingTargetTokens = trainingInputsTokens.drop(1)
    val trainingTarget = trainingInputs.shiftUpAndPadEndWithZero()

    srn.trainingSet = TrainingDataset(
        inputs = trainingInputs.toArray().map { it.toMutableList() }.toMutableList(),
        targets = trainingTarget.toArray().map { it.toMutableList() }.toMutableList(),
        inputRowNames = trainingInputsTokens,
        inputColumnNames = textWorldInputs.world.tokenEmbedding.tokens,
        targetRowNames = trainingTargetTokens,
        targetColumnNames = textWorldInputs.world.tokenEmbedding.tokens
    )
    srn.trainerConfig.learningRate = learningRate
    srn.trainerConfig.computeAccuracy = true
    srn.trainerConfig.optimizer = BasicOptimizer(momentum = 0.0)

    // Comment this out to pretrain the network
    // From the original paper: "The training continued in this manner until the network had experienced 6 complete passes
    // through the sequence."
    // repeat(6) {
    //     srn.trainer.trainOnce()
    //     println("${srn.trainer.lossFunction.name}: ${srn.trainer.lossFunction.loss}")
    // }

    withGui {
        place(textWorldInputs, SIM_WINDOW_GAP, SIM_WINDOW_GAP, 531, 929)
        place(textWorldOut, SIM_WINDOW_GAP + 531 + SIM_WINDOW_GAP, SIM_WINDOW_GAP, 500, 204)
        place(networkComponent, SIM_WINDOW_GAP + 531 + SIM_WINDOW_GAP, SIM_WINDOW_GAP + 204 + SIM_WINDOW_GAP, 500, 550)
        val networkPanel = (getDesktopComponent(networkComponent) as NetworkDesktopComponent).networkPanel
        workspace.launch(Dispatchers.Swing) {
            // Wait for the network panel's zoom to fit so the popup points at the box's final position
            delay(500L)
            val box = (networkPanel.getNode(srn) as SubnetworkNode).interactionBox.globalFullBounds
            val boxOnCanvas = networkPanel.canvas.camera.viewToLocal(
                Rectangle2D.Double(box.x, box.y, box.width, box.height)
            ).bounds
            SimbrainDesktop.onboardingManager.showPopup(
                PopupConfig(
                    title = "Train the network",
                    message = "The network is untrained, so it will predict poorly if you use it now " +
                        "(click Step on the main toolbar to see). Double-click this box to open the trainer " +
                        "and press Play until accuracy reaches about 30%. Then step the workspace again to " +
                        "see it predict much better.",
                    targetComponent = networkPanel.canvas,
                    targetBounds = boxOnCanvas,
                    placement = PopupPlacement.AUTO_VERTICAL,
                    suppressionKey = "srn_word_prediction_train_hint",
                    style = PopupStyle.INFO
                )
            )
        }
    }

    workspace.addUpdateAction("Update inputs") {
        textWorldInputs.update()
    }

    workspace.addUpdateAction("Set current word as input activations") {
        val currentVector = textWorldInputs.world.currentVector
        srn.inputLayer.setActivations(currentVector)
    }

    workspace.addUpdateAction("Update network") {
        networkComponent.update()
    }

    workspace.addUpdateAction("Write predicted next word to output") {
        val totalActivations = srn.outputLayer.activations.toDoubleArray().sum()
        val choices = srn.outputLayer.activations.toDoubleArray().mapIndexed { index, d -> index to d }
            .sortedByDescending { (_, d) -> d }
            .take(5)
        val chosenWords = choices.map { (index, _) -> tokenEmbedding.tokens[index] }
        val chosenProbs = choices.map { (_, d) -> d / totalActivations }
        val chosen = chosenWords.zip(chosenProbs).joinToString(" ") { (word, prob) -> "$word (${prob.format(3)})" }
        textWorldOut.world.text = """
            Current Word: ${textWorldInputs.world.currentToken}
            Predicted Next Words: $chosen
        """.trimIndent()
    }

    workspace.launch {
        delay(10L)
        workspace.iterateSuspend(1)
        // This first step finishes after the build does, but it is still not the user's work
        workspace.markClean()
    }

    addSidebarInfo(
        """
        # Next Word Prediction (SRN)

        This simulation is a scaled-down version of Elman's (1990) classic study of temporal pattern learning in Simple Recurrent Networks (SRNs),
        with fewer training sentences and a smaller vocabulary.
        The network learns to predict the next word in grammatically constrained sentences, developing internal representations
        that capture syntactic and semantic relationships.

        # Simulation Details

        The simulation creates sentences using grammatical templates with different word categories:
        - **Noun categories**: Human, animal, inanimate objects, aggressive entities, fragile items, food
        - **Verb categories**: Intransitive, transitive, aggressive-patient, perceptual, destructive, eating

        Example sentence templates:
        - `man eat cookie` (human + eat + food)
        - `dragon destroy glass` (aggressive + destroy + fragile)
        - `cat chase mouse` (animal + transitive + animal)

        The SRN processes these sentences one word at a time, using its recurrent connections to maintain context and predict the next word based on grammatical constraints.

        ## Expected accuracy

        The network starts untrained, so its predictions are close to random until you train it.

        As Elman notes, the prediction task is non-deterministic: after `man`, any verb that fits a human subject can
        come next, so some error is inevitable. Elman therefore compared the network's output not to the actual next
        word but to the probability of each possible next word, computed from training sentences that match up to that
        point. By that measure the network learned to approximate those probabilities.

        The trainer here reports top-1 accuracy: how often the most active output is the actual next word. Estimated
        values for this grammar:

        - Chance: about 5% (21 words)
        - Guessing from the current word only: about 26%
        - Best possible, always choosing the most probable next word: about 31%

        So an accuracy near 30% means the network has learned what the grammar allows. Training accuracy can climb
        higher with long training, but that comes from memorizing this particular training text rather than, in
        Elman's terms, extracting generalizations.

        # Activities

        ## Activity: Train the network to predict next words

        1. Run the simulation to see the network processing the current sentence.

        2. Train the network by right-clicking on the `SRN` network and opening the `Train network` dialog.

        3. Monitor learning progress through the `Loss function` - the network should learn to predict grammatically appropriate next words.
           Aim for an accuracy of about 30%.

        4. Observe predictions in the `Output Text World` to see what words the network thinks should come next.

        ## Explore further

        Experiment with parameters:

        - Adjust the `number of training sentences` by editing the script
        - Modify the `learning rate`
        - Change the `hidden layer size` by editing the script

        # References

        Elman, J. L. (1990). [_Finding structure in time_](https://crl.ucsd.edu/~elman/Papers/fsit.pdf). _Cognitive Science_, _14_(2), 179-211.

        # Credits

        [Jeff Yoshimi](https://jeffyoshimi.net/index.html)

        """.trimIndent()
    )

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

val allWords = listOf(
    noun_hum,
    noun_anim,
    noun_inanim,
    noun_agress,
    noun_frag,
    noun_food,
    verb_intran,
    verb_tran,
    verb_agpat,
    verb_percept,
    verb_destroy,
    verb_eat
).flatten()

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
