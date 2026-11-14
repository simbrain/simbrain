/**
 * A Leabra network learns Spanish words for eight pictures, like studying flashcards. The minus phase is the
 * network's guess and the plus phase is being shown the answer. Settling, phases, and learning are handled by
 * [LeabraTrainer].
 */
package org.simbrain.custom_sims.simulations.leabra

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.piccolo2d.nodes.PPath
import org.simbrain.custom_sims.*
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.NetworkTextObject
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.gui.nodes.WeightMatrixNode
import org.simbrain.network.trainers.LeabraProjection
import org.simbrain.network.trainers.LeabraTrainer
import org.simbrain.network.trainers.leabraLayer
import org.simbrain.network.trainers.leabraProjection
import org.simbrain.network.updaterules.LeabraRule
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.updateAction
import org.simbrain.workspace.Workspace
import java.awt.BasicStroke
import java.awt.Color
import javax.swing.JComboBox

val leabraVocabulary = newSim("leabra_vocabulary") {
    workspace.clearWorkspace()
    exposeTypes(LeabraRule::class)

    val networkComponent = addNetworkComponent("Leabra network")
    val network = networkComponent.network

    val picture = leabraLayer(PICTURE_SIZE, PICTURE).apply {
        gridMode = true
        gridColumns = PICTURE_COLUMNS
        location = point(0, 300)
    }
    val hidden = leabraLayer(49, HIDDEN).apply {
        gridMode = true
        gridColumns = 7
        location = point(0, 0)
    }
    val word = leabraLayer(flashcards.size, WORD).apply {
        circleMode = true
        circleSpacingX = 70.0
        labelArray = flashcards.map { it.spanish }.toTypedArray()
        location = point(0, -300)
    }
    val projections = listOf(
        leabraProjection(picture, hidden),
        leabraProjection(hidden, word),
        leabraProjection(word, hidden, FEEDBACK_SCALE),
    )
    projections.forEach { it.weights.label = "${it.source.label} to ${it.target.label}" }
    val learningText = NetworkTextObject("").apply {
        fontSize = 20
        isBold = true
        color = LEARNING_TEXT_COLOR
        location = point(250, -150)
    }
    network.addNetworkModels(
        listOf(picture, hidden, word, learningText) + projections.map { it.weights },
        usePlacementManager = false
    )

    addTimeSeriesComponent(ERROR_PLOT, "Percent wrong")

    setUpLeabraVocabulary(workspace)

    addSidebarInfo(
        """
        # Learning words with Leabra

        A [Leabra](https://github.com/emer/leabra) network learns the Spanish words for eight pictures, the way a
        student studies flashcards. It illustrates three principles of Leabra:

        1. Learning from expectation and outcome. The network first guesses the word, then is shown the answer, and
           its weights change so that next time its guess will be closer to the answer.
        2. Bidirectional connections. The answer flows back from the `Word` layer to the `Hidden` layer, so hidden
           units learn too, even though they are never told the answer.
        3. Sparse, distributed representations. In the `Hidden` layer, inhibition lets only a few units be active
           at once, and each picture is represented by its own small set of them. Pictures that look alike share
           some of these units, which lets the network name a picture from part of it.

        # Simulation Details

        The `Picture` layer shows a 7 by 7 drawing. The `Word` layer has one unit per Spanish word. Between them is a
        `Hidden` layer, connected in both directions to `Word`.

        Each card is a trial with two phases. In the minus phase the picture is clamped and the network settles for
        75 cycles; whatever word unit wins is its guess. In the plus phase the correct word is also clamped and the
        network settles for 25 more cycles. Each weight then changes according to how much more or less its two units
        were active together in the plus phase than in the minus phase. This is error-driven learning, but no error
        is computed explicitly: the difference between the two phases plays that role.

        Units are point neurons like the one in the `Point neuron` simulation, and each layer has the inhibition
        shown in the `Layer inhibition` simulation. This is a simplified version of Leabra. It omits running activity averages, Hebbian learning, and weight
        contrast enhancement, among other things.

        ## Control Panel Settings

        - `Picture`: Which flashcard the buttons below use.
        - `Show picture`: Show the picture and let the network guess (the minus phase).
        - `Show part of picture`: Show a random half of the picture's pixels and let the network guess.
        - `Answer`: The word `Show answer` teaches. Choosing a picture sets it to the correct word, but it can be
          changed to teach the network something wrong.
        - `Show answer`: Show the network the answer (the plus phase) and learn. If the network has not guessed
          yet, it guesses first.
        - `Study all cards`: Go through every card once, with its correct word, in shuffled order, and plot the
          percent the network gets wrong.
        - `Study all cards 10 times`: Study all cards 10 times.
        - `Reset weights`: Randomize the weights so the network forgets everything, and clear the plot.

        The buttons run the network themselves, so there is no need to press play. Whenever the network learns,
        `Learning!` appears in the network and its weights flash yellow.

        # Activities

        ## Activity 1: Learn from a guess and an answer
        1. Choose `sun` and click `Show picture`.
            - The most active unit in `Word` is the network's guess. It is arbitrary, since the network has not
              learned anything.
            - Note which units are active in `Hidden`.
        2. Click `Show answer`.
            - The `Word` layer jumps to `sol`.
            - Some `Hidden` units turn on or off. The answer reaches them through the connections from `Word` back
              to `Hidden` (principle 2).
        3. Alternate `Show picture` and `Show answer` a few times.
            - The network soon guesses `sol` on its own (principle 1).

        ## Activity 2: Learn the whole deck
        1. Click `Study all cards 10 times`.
            - The percent wrong falls to 0, usually within 10 passes.
        2. Show a few pictures to check the network's answers.

        ## Activity 3: Recognize part of a picture
        1. After learning the deck, choose a picture and click `Show part of picture` several times.
            - The network usually still names the picture, even with half of it missing (principle 3).
            - Its mistakes tend to be pictures that look alike, such as `tree`, `house`, and `star`.

        ## Activity 4: Teach something wrong
        1. After learning the deck, choose `heart`, set `Answer` to `"pez"`, and click `Show answer`.
        2. Click `Show picture`.
            - One lesson is usually enough for the network to guess `pez`. It learns whatever it is shown and has
              no way to know the answer was wrong.
        3. Show some other pictures.
            - Often some of them are now named wrongly too. All the words share the same hidden units and weights,
              so a change made for one card can disturb others.
        4. Click `Study all cards` once or twice.
            - The network relearns `corazón` and the other words.

        ## Explore further
        - Reset the weights and teach only `tree`. Then show `star` and `house`, which share its top half.
        - Open a layer's properties and change its inhibition gain to see how it affects learning.

        # References

        O'Reilly, R. C., Munakata, Y., Frank, M. J., Hazy, T. E., & Contributors. (2012).
        [_Computational cognitive neuroscience_](https://compcogneuro.org). Wiki Book.
        """.trimIndent(),
        width = 330
    )
}.registerReopenFunction { workspace -> setUpLeabraVocabulary(workspace) }

class Flashcard(val english: String, val spanish: String, drawing: String) {
    val picture = drawing.trimIndent().filterNot { it.isWhitespace() }.map { if (it == '#') 1.0 else 0.0 }.toDoubleArray()
}

val flashcards = listOf(
    Flashcard("sun", "sol", """
        .......
        .#.#.#.
        ..###..
        .#####.
        ..###..
        .#.#.#.
        .......
    """),
    Flashcard("moon", "luna", """
        ..###..
        .##....
        .#.....
        .#.....
        .#.....
        .##....
        ..###..
    """),
    Flashcard("house", "casa", """
        ...#...
        ..###..
        .#####.
        #######
        .#...#.
        .#.#.#.
        .#.#.#.
    """),
    Flashcard("tree", "árbol", """
        ...#...
        ..###..
        .#####.
        #######
        ...#...
        ...#...
        ..###..
    """),
    Flashcard("heart", "corazón", """
        .......
        .##.##.
        #######
        #######
        .#####.
        ..###..
        ...#...
    """),
    Flashcard("star", "estrella", """
        ...#...
        ...#...
        #######
        .#####.
        ..###..
        .##.##.
        .#...#.
    """),
    Flashcard("fish", "pez", """
        .......
        ..##..#
        .####.#
        #.#####
        .####.#
        ..##..#
        .......
    """),
    Flashcard("cup", "taza", """
        .......
        #####..
        #####.#
        #####.#
        #####..
        .###...
        .......
    """),
)

private const val PICTURE = "Picture"
private const val HIDDEN = "Hidden"
private const val WORD = "Word"
private const val ERROR_PLOT = "Leabra flashcard errors"
private const val PICTURE_SIZE = 49
private const val PICTURE_COLUMNS = 7
private const val FEEDBACK_SCALE = 0.3
private const val FLASH_MARGIN = 6.0
private const val LEARNING_FLASH_MILLIS = 500L
private const val FADE_MILLIS = 400L
private val LEARNING_FLASH_COLOR = Color(255, 200, 0, 130)
private val LEARNING_TEXT_COLOR = Color(215, 140, 0)
private const val LEARNING_TEXT = "Learning!"

private fun quoted(word: String) = "\"$word\""

private fun answer(index: Int) = DoubleArray(flashcards.size) { if (it == index) 1.0 else 0.0 }

/**
 * A random half of [picture]'s lit pixels.
 */
private fun partOf(picture: DoubleArray): DoubleArray {
    val lit = picture.indices.filter { picture[it] > 0 }
    val kept = lit.shuffled().take(lit.size / 2).toSet()
    return DoubleArray(picture.size) { if (it in kept) 1.0 else 0.0 }
}

/**
 * A trainer for the picture, hidden, and word layers of a network built by [leabraVocabulary].
 */
fun vocabularyTrainer(network: Network): LeabraTrainer {
    val arrays = network.getModels<NeuronArray>()
    val layers = listOf(PICTURE, HIDDEN, WORD).map { name -> arrays.first { it.label == name } }
    val (picture, _, word) = layers
    val projections = network.getModels<WeightMatrix>().map {
        val feedback = layers.indexOf(it.source as NeuronArray) > layers.indexOf(it.target as NeuronArray)
        LeabraProjection(it, if (feedback) FEEDBACK_SCALE else 1.0)
    }
    return LeabraTrainer(network, layers, projections, picture, word).apply {
        expectedActivity[picture] = flashcards.map { it.picture.average() }.average()
        expectedActivity[word] = 1.0 / flashcards.size
    }
}

private suspend fun SimulationScope.setUpLeabraVocabulary(workspace: Workspace) {
    val networkComponent = workspace.componentList.filterIsInstance<NetworkComponent>().first()
    val network = networkComponent.network
    val trainer = vocabularyTrainer(network)
    val word = trainer.outputLayer
    val pictures = flashcards.map { it.picture }
    val answers = flashcards.indices.map(::answer)

    val plot = workspace.componentList.filterIsInstance<TimeSeriesPlotComponent>().first { it.name == ERROR_PLOT }
    var pass = plot.model.timeSeriesList.firstOrNull()?.series?.itemCount ?: 0
    var cardIndex = 0

    /**
     * Index of the word that `Show answer` teaches. Follows the picture unless changed.
     */
    var taughtIndex = 0
    val learningText = network.getModels<NetworkTextObject>().first()

    network.updateManager.clear()
    network.updateManager.addAction(updateAction("Leabra cycle") { trainer.cycle() })

    /**
     * Activity at the end of the last minus phase, until the answer is shown.
     */
    var minus: Map<NeuronArray, DoubleArray>? = null

    suspend fun guessFrom(input: DoubleArray) {
        trainer.startTrial(input)
        workspace.iterateSuspend(trainer.minusCycles)
        minus = trainer.snapshot()
    }

    /**
     * Runs a block that changes weights. With a GUI, the weights flash while it runs.
     */
    var whileLearning: suspend (suspend () -> Unit) -> Unit = { it() }

    suspend fun showAnswer() {
        if (minus == null) guessFrom(pictures[cardIndex])
        val minusActivity = minus!!
        trainer.clamp(word, answers[taughtIndex])
        workspace.iterateSuspend(trainer.plusCycles)
        whileLearning { trainer.learn(minusActivity, trainer.snapshot()) }
        minus = null
    }

    suspend fun study(times: Int) = whileLearning {
        workspace.stop()
        repeat(times) {
            trainer.trainEpoch(pictures, answers)
            pass++
            plot.model.addData(0, pass.toDouble(), 100 * trainer.errorRate(pictures, answers))
        }
    }

    withGui {
        val networkPanel = getNetworkPanel(networkComponent)
        val weightNodes = trainer.projections.map { networkPanel.modelNodeMap.get<WeightMatrixNode>(it.weights) }
        whileLearning = { block ->
            val flashes = withContext(Dispatchers.Swing) {
                weightNodes.map { node ->
                    val bounds = node.imageBox.globalFullBounds
                    PPath.createRoundRectangle(
                        bounds.x - FLASH_MARGIN, bounds.y - FLASH_MARGIN,
                        bounds.width + 2 * FLASH_MARGIN, bounds.height + 2 * FLASH_MARGIN, 8.0, 8.0
                    ).apply {
                        paint = LEARNING_FLASH_COLOR
                        stroke = BasicStroke(3f)
                        strokePaint = LEARNING_TEXT_COLOR
                        pickable = false
                        networkPanel.canvas.layer.addChild(this)
                    }
                }
            }
            learningText.text = LEARNING_TEXT
            try {
                block()
            } finally {
                delay(LEARNING_FLASH_MILLIS)
                learningText.text = ""
                withContext(Dispatchers.Swing) { flashes.forEach { it.animateToTransparency(0f, FADE_MILLIS) } }
                delay(FADE_MILLIS)
                withContext(Dispatchers.Swing) { flashes.forEach { it.removeFromParent() } }
            }
        }
        lateinit var answerBox: JComboBox<String>
        val controlPanel = createControlPanel("Flashcards", SIM_WINDOW_GAP, SIM_WINDOW_GAP) {
            addComboBox(
                "Picture", flashcards.map { it.english }, flashcards.first().english,
                toolTip = "Flashcard used by the buttons below."
            ) { english ->
                cardIndex = flashcards.indexOfFirst { it.english == english }
                minus = null
                answerBox.selectedIndex = cardIndex
            }
            addButton("Show picture") { guessFrom(pictures[cardIndex]) }.apply {
                toolTipText = "Show the picture and let the network guess."
            }
            addButton("Show part of picture") { guessFrom(partOf(pictures[cardIndex])) }.apply {
                toolTipText = "Show a random half of the picture and let the network guess."
            }
            answerBox = addComboBox(
                "Answer", flashcards.map { quoted(it.spanish) }, quoted(flashcards.first().spanish),
                toolTip = "Word that Show answer teaches. Choose a wrong word to teach the network something wrong."
            ) { choice ->
                taughtIndex = flashcards.indexOfFirst { quoted(it.spanish) == choice }
            }
            addButton("Show answer") { showAnswer() }.apply {
                toolTipText = "Show the network the answer and learn."
            }
            addSeparator()
            addButton("Study all cards") {
                minus = null
                study(1)
            }.apply {
                toolTipText = "Teach every card once and plot the percent wrong."
            }
            addButton("Study all cards 10 times") {
                minus = null
                study(10)
            }.apply { toolTipText = "Study all cards 10 times." }
            addSeparator()
            addButton("Reset weights") {
                trainer.randomizeWeights()
                minus = null
                pass = 0
                plot.model.clearData()
            }.apply { toolTipText = "Randomize the weights so the network forgets everything, and clear the plot." }
        }.awaitLayout()
        val networkX = controlPanel.rightEdgeWithGap()
        place(networkComponent, networkX, SIM_WINDOW_GAP, 520, 640)
        place(plot, networkX + 520 + SIM_WINDOW_GAP, SIM_WINDOW_GAP, 400, 300)
    }
}
