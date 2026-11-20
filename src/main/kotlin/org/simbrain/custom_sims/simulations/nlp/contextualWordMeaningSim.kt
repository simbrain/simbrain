/**
 * Contextual embeddings: trains a tiny transformer on sentences where "bank" means money or a
 * riverside. A network view shows what goes into the model (each word's fixed vector), what comes out
 * (each word's vector in context) and the predicted word, for whatever the controls feed it: a
 * test item or the sentence currently being trained on. A projection
 * plot shows the test items together, so students can watch the two senses of "bank" separate. There
 * are two models, masked (BERT-style) and next-word (GPT-style), each keeping its own training.
 */
package org.simbrain.custom_sims.simulations.nlp

import kotlinx.coroutines.delay
import org.jfree.chart.LegendItem
import org.jfree.chart.LegendItemCollection
import org.jfree.chart.LegendItemSource
import org.jfree.chart.title.LegendTitle
import org.simbrain.custom_sims.*
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.core.NetworkTextObject
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.plot.applySimbrainChartTheme
import org.simbrain.plot.projection.ProjectionComponent
import org.simbrain.plot.projection.ProjectionDesktopComponent
import org.simbrain.util.*
import org.simbrain.util.projection.AuxDataColoringManager
import org.simbrain.util.projection.CoordinateProjection
import org.simbrain.util.projection.DataPoint
import smile.feature.extraction.PCA
import java.awt.Color
import java.awt.Dimension
import java.awt.geom.Ellipse2D
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JLabel
import kotlin.math.*

val contextualWordMeaningSim = newSim {

    workspace.clearWorkspace()

    val sentences = readSimulationFileContents("texts" / "bank_senses.txt").lines().filter { it.isNotBlank() }

    val models = ModelKind.entries.associateWith { ContextualWordModel(sentences, it.masked) }.toMutableMap()
    val lossHistory = ModelKind.entries.associateWith { mutableListOf<Pair<Double, Double>>() }
    var kind = ModelKind.MASKED
    var model = models.getValue(kind)
    var view = BankView.TARGET_MASKED
    var itemIndex = 0
    var currentItem = view.items.first()
    val aligner = PlotAligner()
    val training = AtomicBoolean(false)
    var status: JLabel? = null

    val networkComponent = addNetworkComponent("Network")
    val display = ModelDisplay(model.contextSize, model.model.config.embedDim)
    networkComponent.network.addNetworkModels(display.models, usePlacementManager = false)

    val projection = addProjectionPlot("Test item vectors").apply {
        projector.projectionMethod = CoordinateProjection()
        projector.tolerance = 0.0
        projector.showLabels = true
        projector.connectPoints = false
        projector.useHotPoint = false
        projector.coloringManager = AuxDataColoringManager()
    }
    val lossPlot = addTimeSeriesComponent("Training error", "Error")

    /** Shows [snapshot] in the network, with [region] (where its plotted vector falls) above the plotted column. */
    fun show(snapshot: ContextualWordModel.Snapshot, region: String? = null) {
        display.show(snapshot, region)
        val text = if (model.epochs == 0.0) "untrained" else "%.1f passes through the text".format(model.epochs)
        status?.let { label -> swingInvokeLater { label.text = text } }
    }

    fun recordLoss(trainee: ContextualWordModel, error: Double) {
        lossHistory.getValue(if (trainee.masked) ModelKind.MASKED else ModelKind.NEXT_WORD).add(trainee.epochs to error)
        lossPlot.model.addData(0, trainee.epochs, error)
    }

    /**
     * Which meaning's region [vector] falls in: the meaning whose test items' average output vector is nearer, in
     * all the model's dimensions rather than the plot's two. The test item at [skip] is left out of the averages,
     * so an item is judged by the others. When the two distances are nearly equal it is in neither region.
     */
    fun region(vector: DoubleArray, skip: Int): String {
        val items = view.items
        val vectors = items.map { it.run(model).let { snapshot -> snapshot.outputVectors[snapshot.focus] } }
        val distances = Sense.entries.associateWith { sense ->
            val group = items.indices.filter { it != skip && items[it].sense == sense }.map { vectors[it] }
            val center = DoubleArray(vector.size) { d -> group.sumOf { it[d] } / group.size }
            sqrt(vector.indices.sumOf { (vector[it] - center[it]).pow(2) })
        }
        val nearest = distances.minBy { it.value }
        val farthest = distances.maxOf { it.value }
        return if (farthest - nearest.value <= REGION_MARGIN * farthest) "in neither region"
        else "in ${nearest.key.title.lowercase()} region"
    }

    fun showCurrentItem() {
        val snapshot = currentItem.run(model)
        show(snapshot, region(snapshot.outputVectors[snapshot.focus], skip = itemIndex))
    }

    fun refreshPlot(ease: Boolean = false) {
        val items = view.items
        val vectors = items.map { it.run(model).let { snapshot -> snapshot.outputVectors[snapshot.focus] } }
        val coordinates = aligner.project(vectors, ease)
        val points = items.zip(coordinates).map { (item, xy) -> DataPoint(xy, label = item.label, aux = item.sense?.color) }
        projection.replacePoints(points, highlighted = points.getOrNull(itemIndex))
    }

    /** Shows the [kind] model, with its own training and error history. */
    fun switchTo(newKind: ModelKind) {
        training.set(false)
        kind = newKind
        model = models.getValue(kind)
        aligner.reset()
        lossPlot.model.clearData()
        lossHistory.getValue(kind).forEach { (epochs, error) -> lossPlot.model.addData(0, epochs, error) }
        refreshPlot()
        showCurrentItem()
    }

    /** Replaces the shown model with a new, untrained one. The other model is untouched. */
    fun reset() {
        models[kind] = ContextualWordModel(sentences, kind.masked)
        lossHistory.getValue(kind).clear()
        switchTo(kind)
    }

    fun nextItem() {
        val items = view.items
        itemIndex = (itemIndex + 1) % items.size
        val item = items[itemIndex]
        currentItem = item
        showCurrentItem()
        refreshPlot()
    }

    /**
     * Feeds a typed sentence to the network. The plot keeps showing the test items, with none highlighted.
     * The network reads `[mask]` if there is one, else "bank", else the last word.
     */
    fun runSentence(text: String) {
        val words = ContextualWordModel.words(text)
        if (words.isEmpty()) return
        val unknown = words.filterNot(model::knows).distinct()
        if (unknown.isNotEmpty()) {
            showMessage(
                "Not in the model's vocabulary: ${unknown.joinToString(", ")}. Show training text lists the words it knows.",
                MessageKind.WARNING,
            )
            return
        }
        if (words.size > model.contextSize) {
            showMessage("The model reads at most ${model.contextSize} words.", MessageKind.WARNING)
            return
        }
        val focus = words.indexOf(ContextualWordModel.MASK).takeIf { it >= 0 }
            ?: words.indexOf(TARGET).takeIf { it >= 0 }
            ?: words.lastIndex
        currentItem = TestItem(words, focus, words[focus])
        itemIndex = -1
        showCurrentItem()
        refreshPlot()
    }

    workspace.addUpdateAction("Show next test item") { nextItem() }

    refreshPlot()
    showCurrentItem()

    withGui {
        val controlPanel = createControlPanel("Controls", SIM_WINDOW_GAP, SIM_WINDOW_GAP) {
            addComboBox(
                "Model", ModelKind.entries, ModelKind.MASKED,
                toolTip = "Masked (BERT-style): learns by guessing hidden words from both sides. Next word " +
                    "(GPT-style): learns by guessing each word from the words before it. These are two separate " +
                    "networks, and each keeps its own training when you switch."
            ) { switchTo(it) }
            addSeparator()
            status = addLabelledText("Trained", " ", toolTip = "How much the shown model has been trained.")
                .apply { preferredSize = Dimension(200, preferredSize.height) }
            addButton("Train one sentence") {
                if (training.get()) return@addButton
                val error = model.trainSentences(1)
                recordLoss(model, error)
                model.lastTrained?.let { show(it) }
                refreshPlot()
            }.apply { toolTipText = "Train on one sentence and show it in the network, as the model saw it." }
            addButton("Train $TRAIN_BATCH sentences") {
                if (!training.compareAndSet(false, true)) return@addButton
                val trainee = model
                var trained = 0
                while (trained < TRAIN_BATCH && training.get() && model === trainee) {
                    val error = trainee.trainSentences(SENTENCES_PER_FRAME)
                    trained += SENTENCES_PER_FRAME
                    if (model !== trainee) break
                    recordLoss(trainee, error)
                    trainee.lastTrained?.let { show(it) }
                    refreshPlot(ease = true)
                    delay(FRAME_DELAY_MS)
                }
                training.set(false)
                if (model === trainee) showCurrentItem()
                repeat(SETTLE_FRAMES) {
                    if (model !== trainee || training.get()) return@addButton
                    refreshPlot(ease = true)
                    delay(FRAME_DELAY_MS)
                }
            }.apply { toolTipText = "Train on $TRAIN_BATCH sentences, showing them as it goes. The training text has about 260." }
            addButton("Show training text") {
                showText(sentences.joinToString("\n"), "Training text")
            }.apply { toolTipText = "Open the sentences the model is trained on." }
            addSeparator()
            addComboBox(
                "Test items", BankView.entries, view,
                toolTip = "What to run through the model and plot. Target masked: sentences with the target " +
                    "word, \"bank\", replaced by [mask], for the model to guess. Clue before and Clue after: \"bank\" in short sentences where the clue " +
                    "to its meaning comes before or after it. Training is the same either way."
            ) { selected ->
                view = selected
                itemIndex = 0
                currentItem = view.items.first()
                aligner.reset()
                showCurrentItem()
                refreshPlot()
            }
            addButton("Next test item") {
                nextItem()
            }.apply { toolTipText = "Feed the next test item to the network and highlight its point in the plot." }
            addButton("Show test items") {
                showText(view.description(), "Test items: $view")
            }.apply { toolTipText = "List the sentences currently plotted, with each point's label." }
            addSeparator()
            var typed = TYPED_EXAMPLE
            addTextField(
                "Sentence", typed,
                toolTip = "Your own sentence, using only words from the training text. Write [mask] to hide a word for the model to guess.",
            ) { typed = it }.apply {
                columns = 22
                addActionListener { runSentence(typed) }
            }
            addButton("Run sentence") {
                runSentence(typed)
            }.apply { toolTipText = "Feed the sentence to the network. Pressing Enter in the field does the same." }
            addSeparator()
            // TODO: a "Show circuit" checkbox that draws the layers between the input and output rows, by handing
            //  the shown model and sentence to a TinyLanguageModel (its `model` is a swappable TinyLmModel). Check
            //  its next-word assumptions, its own forward pass per update, and rebuilds on config change.
            addButton("Reset") {
                reset()
            }.apply { toolTipText = "Start the shown model over, untrained. The other model keeps its training." }
        }.awaitLayout()
        showCurrentItem()
        val networkX = controlPanel.rightEdgeWithGap()
        place(networkComponent, networkX, SIM_WINDOW_GAP, 760, 700)
        val plotX = networkX + 760 + SIM_WINDOW_GAP
        place(projection, plotX, SIM_WINDOW_GAP, 460, 440)
        place(lossPlot, plotX, SIM_WINDOW_GAP + 440 + SIM_WINDOW_GAP, 460, 250)
        (getDesktopComponent(projection) as ProjectionDesktopComponent).chart.apply {
            addLegend(LegendTitle(LegendItemSource {
                LegendItemCollection().apply {
                    Sense.entries.forEach {
                        add(LegendItem("${it.title} context", "", "", "", Ellipse2D.Double(-4.0, -4.0, 8.0, 8.0), it.color))
                    }
                }
            }))
            applySimbrainChartTheme()
        }
    }

    addSidebarInfo(SIDEBAR)
}

private const val SENTENCES_PER_FRAME = 4
private const val TRAIN_BATCH = 100
private const val FRAME_DELAY_MS = 25L
private const val SETTLE_FRAMES = 20

/** A vector is in neither meaning's region when its distances to the two differ by at most this fraction. */
private const val REGION_MARGIN = 0.1

/** Fraction of the way each displayed point moves toward its new position per frame. */
private const val EASING = 0.3

private enum class ModelKind(private val label: String, val masked: Boolean) {
    MASKED("Masked (BERT-like)", true),
    NEXT_WORD("Next word (GPT-like)", false);

    override fun toString() = label
}

private enum class Sense(val color: Color, val title: String, val colorName: String) {
    MONEY(Color(214, 120, 30), "Money", "orange"),
    RIVER(Color(40, 110, 200), "River", "blue"),
}

/**
 * One input to run through the model and plot: its words, the position to read, and how to label it. Words
 * containing `[mask]` are run with [ContextualWordModel.fillIn], so each kind of model sees what it would in training.
 */
private class TestItem(val words: List<String>, val focus: Int, val label: String, val sense: Sense? = null) {
    fun run(model: ContextualWordModel) =
        if (ContextualWordModel.MASK in words) model.fillIn(words) else model.run(words, focus)
}

private enum class BankView(private val label: String) {
    TARGET_MASKED("Target masked") {
        override val items
            get() = sentenceItems(CLUE_BEFORE_MONEY + CLUE_AFTER_MONEY, CLUE_BEFORE_RIVER + CLUE_AFTER_RIVER, hide = true)
    },
    CLUE_BEFORE("Clue before") {
        override val items get() = sentenceItems(CLUE_BEFORE_MONEY, CLUE_BEFORE_RIVER)
    },
    CLUE_AFTER("Clue after") {
        override val items get() = sentenceItems(CLUE_AFTER_MONEY, CLUE_AFTER_RIVER)
    };

    abstract val items: List<TestItem>

    override fun toString() = label
}

/**
 * Items reading "bank" in each sentence, or with [hide], the `[mask]` that replaces it. Each is labeled by its
 * clue (the last content word other than the target), the same way in every set: "bank (cash)".
 */
private fun sentenceItems(money: List<String>, river: List<String>, hide: Boolean = false): List<TestItem> {
    fun item(sentence: String, sense: Sense): TestItem {
        val words = ContextualWordModel.words(sentence).map { if (hide && it == "bank") ContextualWordModel.MASK else it }
        val target = if (hide) ContextualWordModel.MASK else "bank"
        val clue = words.last { it != target && it !in FUNCTION_WORDS }
        return TestItem(words, words.indexOf(target), "bank ($clue)", sense)
    }
    return money.map { item(it, Sense.MONEY) } + river.map { item(it, Sense.RIVER) }
}

private val FUNCTION_WORDS = setOf("she", "he", "they", "we", "at", "the", "in")

/** The word whose two meanings the simulation is about; a typed sentence is read here if it has no `[mask]`. */
private const val TARGET = "bank"

private const val TYPED_EXAMPLE = "she caught fish at the [mask] and deposited cash"

private val CLUE_BEFORE_MONEY = listOf(
    "she deposited cash at the bank", "she borrowed money at the bank", "she counted coins at the bank",
    "she withdrew savings at the bank", "she cashed checks at the bank",
)
private val CLUE_BEFORE_RIVER = listOf(
    "she caught fish at the bank", "she fed ducks at the bank", "she tied boats at the bank",
    "she picked reeds at the bank", "she spotted frogs at the bank",
)
private val CLUE_AFTER_MONEY = listOf(
    "the bank lent money", "the bank charged interest", "the bank held savings",
    "the bank approved loans", "the bank printed checks",
)
private val CLUE_AFTER_RIVER = listOf(
    "the bank flooded water", "the bank sheltered ducks", "the bank grew reeds",
    "the bank hid frogs", "the bank trapped mud",
)

private fun BankView.description() = buildString {
    Sense.entries.forEach { sense ->
        val group = items.filter { it.sense == sense }
        if (group.isEmpty()) return@forEach
        appendLine("${sense.title} (${sense.colorName})")
        group.forEach { item ->
            val text = item.words.joinToString(" ")
            appendLine(if (text == item.label) "  $text" else "  $text  →  ${item.label}")
        }
        appendLine()
    }
}

/**
 * Replaces every point in the plot at once, so each refresh shows only the current vectors, and marks
 * [highlighted] (the item last fed to the network) as the hot point. The points are already projected to
 * 2D and start out on a coordinate projection. A method picked in the plot's toolbar (e.g. PCA) is kept and
 * re-projects them, except when they all coincide, which such methods cannot fit: then they are placed as given.
 */
private fun ProjectionComponent.replacePoints(points: List<DataPoint>, highlighted: DataPoint?) = with(projector) {
    val coincide = points.all { it.upstairsPoint.contentEquals(points.first().upstairsPoint) }
    synchronized(dataset) {
        dataset.clear()
        points.forEach {
            if (coincide) {
                dataset.insert(it)
                it.setDownstairs(it.upstairsPoint)
            } else {
                addDataPoint(it)
            }
        }
        dataset.currentPoint = highlighted
    }
    useHotPoint = highlighted != null
    events.datasetChanged.fire()
}

/**
 * The network view of the model: one column per position showing the input vector (the word's fixed
 * vector) and, above it, the output vector at that position, with the model's top guess printed above
 * the output columns: every column for a next-word model, only hidden words for a masked model. Guesses
 * for hidden words are bold and give their probability. All arrays are
 * clamped displays; the model itself runs headless and [show] copies a pass into them.
 */
private class ModelDisplay(positions: Int, private val width: Int) {

    private val inputs = List(positions) { i -> column(i, INPUT_Y, INPUT_BOUND) }

    private val outputs = List(positions) { i -> column(i, OUTPUT_Y, 2.0) }

    /** The model's top guess above each output column. */
    private val guesses = List(positions) { i ->
        NetworkTextObject("").apply {
            fontSize = GUESS_FONT_SIZE
            location = point(i * COLUMN_SPACING, GUESS_Y)
        }
    }

    /** Which meaning's region the plotted vector falls in, above its column. */
    private val label = NetworkTextObject("").apply {
        fontSize = LABEL_FONT_SIZE
        isItalic = true
    }

    /**
     * Says what the guess row holds, to its left, whenever it holds anything. Plain and gray, so it reads as a row
     * header: bold already marks guesses for hidden words, and italics the region reading.
     */
    private val guessRowLabel = NetworkTextObject("").apply {
        fontSize = LABEL_FONT_SIZE
        color = Color(128, 128, 128)
        location = point(-COLUMN_SPACING, GUESS_Y)
    }

    val models: List<NetworkModel> = inputs + outputs + guesses + label + guessRowLabel

    private fun column(position: Int, y: Int, bound: Double) = NeuronArray(width).apply {
        verticalLayout = true
        isClamped = true
        (updateRule as LinearRule).apply {
            lowerBound = -bound
            upperBound = bound
        }
        location = point(position * COLUMN_SPACING, y)
    }

    fun show(snapshot: ContextualWordModel.Snapshot, region: String?) {
        snapshot.words.forEachIndexed { i, word ->
            if (inputs[i].label != word) inputs[i].label = word
            if (outputs[i].label != word) outputs[i].label = word
            inputs[i].setActivations(snapshot.inputVectors[i])
            outputs[i].setActivations(snapshot.outputVectors[i])
        }
        var anyGuess = false
        guesses.forEachIndexed { i, note ->
            val bold = i in snapshot.hidden
            // A masked model is only scored on hidden words, so its guesses elsewhere mean nothing.
            val shown = snapshot.guesses[i].takeIf { bold || !snapshot.masked }
            val text = shown?.let { (word, p) -> if (bold) "\"$word\" ${"%.2f".format(p)}" else "\"$word\"" }.orEmpty()
            if (text.isNotEmpty()) anyGuess = true
            if (note.isBold != bold || note.text != text) {
                note.isBold = bold
                note.text = text // Setting the text redraws it, picking up the bold change too.
            }
        }
        val rowText = when {
            !anyGuess -> ""
            snapshot.masked -> "filled-in [mask]"
            else -> "predicted next word"
        }
        if (guessRowLabel.text != rowText) guessRowLabel.text = rowText
        val text = region.orEmpty()
        if (label.text != text) label.text = text
        val where = point(snapshot.focus * COLUMN_SPACING, LABEL_Y)
        if (label.location != where) label.location = where
    }

    companion object {
        const val COLUMN_SPACING = 130

        /** Display range of the input vectors. Embedding weights start near ±0.15, so a wider range looks washed out. */
        const val INPUT_BOUND = 0.25
        const val INPUT_Y = 450
        const val OUTPUT_Y = 150
        /** Centered on the output column, just above its label. */
        const val GUESS_Y = OUTPUT_Y - 135
        const val GUESS_FONT_SIZE = 16
        const val LABEL_Y = GUESS_Y - 30
        const val LABEL_FONT_SIZE = 13
    }
}

/**
 * Projects vectors to 2D with PCA, then rescales and rotates (or reflects) each frame to best match
 * the previous one, so points drift smoothly during training instead of jumping whenever PCA picks
 * new axes or the spread changes.
 */
private class PlotAligner {

    private var previous: List<DoubleArray>? = null

    fun reset() {
        previous = null
    }

    /** With [ease], points move only part of the way from their last shown positions toward the new ones. */
    fun project(vectors: List<DoubleArray>, ease: Boolean = false): List<DoubleArray> {
        val center = DoubleArray(vectors.first().size) { d -> vectors.sumOf { it[d] } / vectors.size }
        val spread = vectors.sumOf { v -> v.indices.sumOf { (v[it] - center[it]).let { x -> x * x } } }
        val size = vectors.sumOf { v -> v.sumOf { it * it } }
        if (spread <= 1e-9 * size) return vectors.map { doubleArrayOf(0.0, 0.0) }.also { previous = it }
        val raw = PCA.fit(vectors.toTypedArray()).getProjection(2).apply(vectors.toTypedArray()).toList()
        val mean = doubleArrayOf(raw.sumOf { it[0] } / raw.size, raw.sumOf { it[1] } / raw.size)
        val radius = sqrt(raw.sumOf { (it[0] - mean[0]).pow(2) + (it[1] - mean[1]).pow(2) } / raw.size)
        val scaled = raw.map { doubleArrayOf((it[0] - mean[0]) / radius, (it[1] - mean[1]) / radius) }
        val last = previous
        val aligned = if (last == null || last.size != scaled.size) scaled else {
            val reflected = scaled.map { doubleArrayOf(it[0], -it[1]) }
            val best = listOf(scaled, reflected).map { rotateToward(it, last) }.maxBy { it.second }.first
            if (!ease) best else best.indices.map { i ->
                doubleArrayOf(
                    last[i][0] + EASING * (best[i][0] - last[i][0]),
                    last[i][1] + EASING * (best[i][1] - last[i][1]),
                )
            }
        }
        previous = aligned
        return aligned
    }

    /** The rotation of [points] that best matches [target], with how well it matches. */
    private fun rotateToward(points: List<DoubleArray>, target: List<DoubleArray>): Pair<List<DoubleArray>, Double> {
        var a = 0.0
        var b = 0.0
        points.indices.forEach { i ->
            val (x, y) = points[i][0] to points[i][1]
            val (px, py) = target[i][0] to target[i][1]
            a += px * x + py * y
            b += py * x - px * y
        }
        val angle = atan2(b, a)
        val (c, s) = cos(angle) to sin(angle)
        return points.map { doubleArrayOf(it[0] * c - it[1] * s, it[0] * s + it[1] * c) } to sqrt(a * a + b * b)
    }
}

private val SIDEBAR = """
    # Contextual Embeddings

    The word "bank" can mean a place for money or the side of a river. Models like BERT read a whole sentence at once and give each word a vector that depends on the words around it, called a contextual embedding. This simulation trains a tiny BERT-style model on simple sentences about money and rivers. It shows what goes into such a model and what comes out, and lets you watch the two meanings of "bank" pull apart as it learns. It also compares masked models (BERT) with next-word models (GPT). The Significance section at the bottom places all this in the early history of transformers.

    # Simulation Details

    The training text is about 260 short sentences such as `she deposited cash at the bank` and `the bank sheltered ducks`.

    ## How it works

    There are two networks with the same layout: a masked one (BERT-style) and a next-word one (GPT-style). `Model` picks which one is shown, and each keeps its own training. Everything else in the control panel either trains the shown network or feeds it an input to look at.

    - Input row (bottom): each word's fixed vector, looked up from a table with one row per vocabulary word. "bank" gets the same input vector in every sentence. The table is learned during training, so words used in similar sentences drift together.
    - Two transformer layers (not drawn): attention mixes information between word positions. This is the only place context enters.
    - Output row (middle): each word's vector after the two layers, now shaped by its sentence. This is the vector BERT-style models are used for.
    - Guesses (top): each output vector goes through one more learned weight matrix, the same at every position, giving a score for every vocabulary word, and a softmax turns the scores into probabilities. The most probable word is printed above the column, and the row is labeled at its left: `filled-in [mask]` for the masked model, `predicted next word` for the next-word model. Guesses for hidden words are bold and give their probability.

    A masked model is only scored on hidden words, so its guesses appear only there. A next-word model is scored at every word on the word that follows, so a guess appears above every word and the guesses run one step ahead of the input. `[pad]` fills empty positions and gets no guess.

    Training always uses the same training text. A masked model picks about 15% of the words in each sentence. As in BERT, most of these become `[mask]`, some become a random word and some stay as they are, and the model must recover the original word in every case. The guessing is a training exercise. Afterward, the output vectors are what get used.

    The plot always shows the whole current test set, one point per item, and changing `Test items` replaces it. Each point is the output vector at `[mask]` or "bank", reduced to two dimensions with PCA before it reaches the plot, which is why the plot's own method starts on `Coordinate`; picking another method there re-projects the points.A point shows how the model represents a word, not what it predicts. Orange marks a money context and blue a river context, as the legend says. The colors are the answer key: they come from which list the test sentence was written for, and the model never sees them. Each point is labeled with "bank" and its sentence's clue word, such as `bank (cash)`: "bank" in the context of "cash". The label is the same in every test set, even when "bank" is hidden. Only the arrangement matters, not the axes. A typed sentence is shown in the network but not plotted.

    The colors and labels are the answer key. The model's own reading is where its vector lands, shown in italics above the plotted column in the network: `in money region` when the vector is nearer the average of the money items than of the river items, `in river region` for the reverse, and `in neither region` when the two are about equally near. Distances are measured on all 16 numbers, not the plot's two, and each item is compared with the others, not itself. A mistake is a sentence that lands in the other meaning's region.

    ## Architecture

    Both networks run on the same transformer engine as `Language > Tiny language model`, which draws out the layers this simulation hides. The next-word network is that model at different sizes. The differences:

    - Size: 16 numbers per word, two layers with two attention heads each, a vocabulary of 53 words plus `[pad]` and `[mask]`, and about 6,000 weights. BERT-base has about 110 million.
    - Attention in the masked network looks both ways. In the Tiny language model it only looks back.
    - Training feeds one sentence at a time, padded with `[pad]`, rather than windows sliding over running text.

    Compared with BERT itself: there are no special start and end tokens, words are not split into pieces, and padding is not hidden from attention.

    ## Control Panel Settings

    - `Model` shows the `Masked (BERT-like)` or the `Next word (GPT-like)` network. Switching does not erase either one's training.
    - `Trained` shows how many passes through the training text the shown network has had.
    - `Train one sentence` trains on one sentence and shows it in the network, as the model saw it.
    - `Train 100 sentences` trains on 100 sentences, showing them as it goes. The training text has about 260.
    - `Show training text` opens the training sentences.
    - `Test items` chooses what is fed to the network and plotted. Training is the same whichever you pick.
        - `Target masked`: sentences with the target word, "bank", replaced by `[mask]`, such as `she deposited cash at the [mask]`. A next-word model sees only the words before `[mask]`.
        - `Clue before`: "bank" in sentences like `she deposited cash at the bank`, where the clue to its meaning comes first.
        - `Clue after`: "bank" in sentences like `the bank lent money`, where the clue comes after.
    - `Next test item` feeds the next test item to the network and highlights its point in the plot. The main toolbar's `Step` does the same.
    - `Show test items` lists the items, with each point's label.
    - `Sentence` and `Run sentence` feed your own sentence to the network (Enter also works). Use only words from the training text, and write `[mask]` to hide a word for the model to guess.
    - `Reset` starts the shown network over, untrained. The other network keeps its training.

    # Activities

    Activities 1 to 5 use the masked model, the kind BERT is. Activity 6 compares it with a next-word model.

    ## Activity 1: What the model sees

    1. With `Test items` on `Target masked`, press `Next test item` a few times.
        - The input row shows a sentence with `[mask]` where "bank" was. The model's guess for it appears above, in bold with its probability. The model is untrained, so the guess is random.

    ## Activity 2: Training one sentence at a time

    1. Press `Train one sentence` several times.
        - Each press shows a training sentence with a word or two chosen for guessing, in different places each time, their guesses in bold. Most chosen words become `[mask]`. Sometimes one is swapped for a random word or left as it is, and the model must still recover the original.

    ## Activity 3: Training the model

    1. Press `Train 100 sentences` repeatedly and watch the network and the error curve, until the error levels off.
    2. Press `Next test item` a few times.
        - The guess is now "bank", with a high probability.

    ## Activity 4: One word, two meanings

    1. Switch `Test items` to `Clue before` and press `Next test item` a few times.
        - Each press feeds one sentence ending in "bank". The input column for "bank" is the same every time. The output column above it changes with the sentence. The matching point lights up in the plot. Nothing is hidden, so there is no guess.
    2. Look at the plot: orange and blue points sit in separate groups. The same word gets different vectors depending on its meaning. Above the "bank" column, the network says which region each sentence's vector lands in. Look for mistakes, such as `bank (fish)` in the money region.
    3. Switch `Test items` to `Clue after`. The meanings still separate, because the model reads the words after "bank".
    4. Press `Reset` and then `Train 100 sentences` repeatedly, and watch the groups form in the plot as the model learns.

    ## Activity 5: One vector per word, many in context

    1. Type `she caught fish at the bank` in `Sentence` and press `Run sentence`. Then try `she deposited cash at the bank`.
        - The input column for "bank" is identical both times: the input table has one vector per word. The output column above it differs: the layers give the word a vector for each context. The note above it says which region each one lands in.
    2. Type `bank` alone. The input column is the same again, with no context to shape the output.

    ## Activity 6: Masked vs next word

    A next-word model like GPT learns by guessing each word from the words before it. Its attention can only look backward.

    1. Switch `Model` to `Next word (GPT-like)`. This is a separate, untrained network. The masked one keeps its training.
    2. Switch `Test items` to `Target masked` and press `Next test item`.
        - The input stops at the word before `[mask]`: a next-word model never sees what comes after. Above each word is its guess for the next word. The bold one, above the last word, is its guess for the hidden word.
    3. Press `Train one sentence` a few times.
        - Nothing is hidden. Above each word is the model's guess for the word after it. After training, compare the guesses with the input: they run one step ahead.
    4. Switch `Test items` to `Clue before` and press `Train 100 sentences` repeatedly.
        - The meanings separate, as with the masked model. A next-word model's vector at "bank" is trained to guess the word after it, and in the training text that word often depends on the meaning, as in `at the bank downtown` or `at the bank downstream`.
    5. Switch `Test items` to `Clue after`.
        - All the points sit in one spot, in neither region. In `the bank lent money`, a next-word model has seen only `the bank` when it reaches "bank".
    6. Switch `Model` back to `Masked (BERT-like)`. Its points for `Clue after` are still separate.

    ## Explore further

    - Type a sentence with both meanings, such as `she caught fish at the [mask] and deposited cash`. Which clue wins, and does the next-word model agree?
    - Recreate any test item by hand, then change one word and watch the guesses.
    - Edit `simulations/texts/bank_senses.txt` to add new sentences or a third meaning.

    # Significance

    This simulation reproduces, at small scale, the first years of [transformers](https://en.wikipedia.org/wiki/Transformer_(deep_learning_architecture)). The transformer was introduced in 2017 for translation. In 2018 two ways of training it on plain text appeared within months of each other: OpenAI's GPT, which guesses the next word, and Google's BERT, which guesses hidden words. These are the two networks here.

    Before them, the standard word vectors (word2vec, 2013) gave each word a single vector, like this simulation's input table. Transformers give each word a vector for each use, like its output row. That is the change the "bank" plot shows.

    ## BERT and its relatives

    [BERT](https://en.wikipedia.org/wiki/BERT_(language_model)) has the same design and training task as the masked network here, at a much larger scale. The base version has 12 layers, 768 numbers per word and a vocabulary of about 30,000 word pieces, and was trained on about 3 billion words of books and Wikipedia. It also had a second training task, guessing whether two sentences were consecutive, which this simulation leaves out. After training, BERT is used the way the plot uses this model: run a text through it and take the output vectors, often with a little extra training for a task such as classifying sentiment.

    - RoBERTa trains the same design longer, on more text, and drops the second task.
    - DistilBERT and ALBERT are smaller versions that keep most of the accuracy.
    - Sentence-BERT (SBERT) turns BERT's output vectors into one vector per sentence (see the next section).
    - Domain versions such as FinBERT (finance), SciBERT (science) and BioBERT (biomedicine) continue training on specialized text.

    ## Word and sentence embeddings

    The output vectors here are better word embeddings: they are aware of context. Older [word embeddings](https://en.wikipedia.org/wiki/Word_embedding) such as word2vec give "bank" one vector, a blend of its meanings. A contextual embedding gives it a vector for each use.

    Many applications need one vector for a whole sentence or document instead: a [sentence embedding](https://en.wikipedia.org/wiki/Sentence_embedding). The usual recipe starts from contextual embeddings like these. Run the text through a BERT-style model, average the output vectors, and train further so that sentences with similar meanings land close together. [Sentence-BERT](https://www.sbert.net) is the standard example, and BERTopic clusters its sentence vectors to find topics. Today sentence embeddings are the more common product, used for search, clustering and topic models. Contextual word embeddings are the step underneath, and still the tool when the question is about a word in context, as here.

    ## BERTology

    BERT became an object of study in its own right, a field nicknamed BERTology (see Rogers, Kovaleva and Rumshisky, "A Primer in BERTology", 2020). Researchers asked what its vectors and attention encode:

    - Word senses: output vectors for a word like "bank" or "die" fall into clusters by meaning, the effect this simulation's plot shows (Coenen and colleagues, "Visualizing and Measuring the Geometry of BERT", 2019).
    - Grammar: small classifiers trained on the vectors, called probes, recover parts of speech and sentence structure, and some attention heads track relations such as a verb and its subject.
    - Layers: lower layers carry more about the word itself, higher layers more about its role and meaning in context.

    ## Early GPT

    The first GPT (2018) trained a next-word model like the one here, then adapted it to tasks such as classification, much as BERT was used. BERT usually did better on those tasks, because it reads in both directions. GPT-2 (2019) showed that a next-word model trained on enough text could write fluent passages, and that line of work led to today's chatbots. The `Clue after` test items show the cost of reading in one direction, and why BERT-style models remained the usual choice for turning text into vectors.
""".trimIndent()
