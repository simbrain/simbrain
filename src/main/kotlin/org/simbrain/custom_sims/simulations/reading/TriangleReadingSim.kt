/** A small distributed spelling-to-sound model for demonstrating reading frequency effects. */
package org.simbrain.custom_sims.simulations.reading

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.plot.PlotOrientation
import org.jfree.data.category.DefaultCategoryDataset
import org.simbrain.custom_sims.*
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.trainers.AdamOptimizer
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.plot.applySimbrainChartTheme
import org.simbrain.plot.timeseries.TimeSeriesModel
import org.simbrain.plot.timeseries.TimeSeriesPlotPanel
import org.simbrain.util.ControlPanelKt
import org.simbrain.util.place
import org.simbrain.util.point
import java.awt.*
import javax.swing.*
import javax.swing.table.DefaultTableModel

private const val HIDDEN_UNITS = 40
private const val DEFAULT_LEARNING_RATE = 0.01
private const val TRAINING_BLOCK_SIZE = 2_000
private const val EMPTY_TEST_FEEDBACK = "Input:          —\nTarget output:  —\nDecoded output: —\nError:          —"

val triangleReadingSim = newSim("triangle_reading") { optionString ->
    workspace.clearWorkspace()
    val dataset = ReadingDataset.loadDevelopmentDataset()
    val orthography = OrthographyEncoder()
    val phonology = PhonologyEncoder()
    val networkComponent = addNetworkComponent("Triangle reading network")
    val model = BackpropNetwork(intArrayOf(orthography.dimension, HIDDEN_UNITS, phonology.dimension), point(0, 0)).apply {
        trainerConfig.optimizer = AdamOptimizer()
        trainerConfig.learningRate = DEFAULT_LEARNING_RATE
        trainerConfig.testConfiguration.enabled = false
        inputLayer.label = "Orthography"
        hiddenLayers().first().label = "Hidden"
        outputLayer.label = "Phonology"
        (inputLayer.updateRule as LinearRule).apply {
            lowerBound = 0.0
            upperBound = 1.0
        }
        inputLayer.labelArray = orthography.labels.toTypedArray()
        outputLayer.labelArray = phonology.labels.toTypedArray()
        layers.filterIsInstance<NeuronArray>().forEach {
            it.circleMode = true
            it.gridMode = true
        }
        // One row per letter or phoneme slot
        inputLayer.gridColumns = orthography.symbolsPerSlot
        outputLayer.gridColumns = phonology.featureNames.size
        outputLayer.circleSpacingX = 75.0
        inputLayer.location = point(0, 550)
        hiddenLayers().first().location = point(0, 0)
        outputLayer.location = point(0, -520)
        initWeights()
        initBiases()
    }
    networkComponent.network.addNetworkModels(model)
    val reader = TriangleReader(networkComponent.network, model, dataset, orthography, phonology)
    if (optionString?.startsWith("train:") == true) {
        val samples = optionString.removePrefix("train:").toIntOrNull()?.coerceAtLeast(1) ?: TRAINING_BLOCK_SIZE
        reader.trainSampled(samples, SamplingMode.FREQUENCY_WEIGHTED)
        printFrequencyExperiment(reader)
    }
    installReaderUi(networkComponent, reader)
}.registerReopenFunction { workspace ->
    val networkComponent = workspace.componentList.filterIsInstance<NetworkComponent>().firstOrNull() ?: return@registerReopenFunction
    val model = networkComponent.network.getModels(BackpropNetwork::class.java).firstOrNull() ?: return@registerReopenFunction
    val dataset = ReadingDataset.loadDevelopmentDataset()
    val reader = TriangleReader(networkComponent.network, model, dataset)
    installReaderUi(networkComponent, reader)
}

private fun printFrequencyExperiment(reader: TriangleReader) {
    val results = reader.evaluateAll()
    val means = results.groupBy { it.word.frequencyBand }.mapValues { (_, values) -> values.map(reader::relativeError).average() }
    println("Triangle reading frequency experiment")
    println("word,frequency,regularity,error,decoded")
    results.forEach { result ->
        println("${result.word.word},${result.word.frequency},${result.word.regularity.name.lowercase()},${result.error},${result.decodedPronunciation.joinToString(" ")}")
    }
    println("high_frequency_mean_relative_error=${means.getValue("High")}")
    println("low_frequency_mean_relative_error=${means.getValue("Low")}")
}

private suspend fun SimulationScope.installReaderUi(
    networkComponent: NetworkComponent,
    reader: TriangleReader
) {
    withGui desktop@{
        desktopPane.allFrames.filter { it.title == "Triangle reading controls" }.forEach { it.dispose() }
        Frame.getFrames().filter {
            it.title == "Triangle Reading Training" || it.title == "Word-frequency results" || it.title == "Word-frequency effect"
        }.forEach { it.dispose() }
        var selectedWord = reader.dataset.words.first()
        var samplingMode = SamplingMode.FREQUENCY_WEIGHTED
        var sampleCount = TRAINING_BLOCK_SIZE
        var resultsWindow: JFrame? = null
        var trainingFrame: JFrame? = null
        var chartWindow: JFrame? = null
        lateinit var controls: ControlPanelKt
        lateinit var feedback: JTextArea
        suspend fun showBarChart(title: String, yAxisLabel: String, means: Map<String, Double>) {
            withContext(Dispatchers.Swing) {
                val dataset = DefaultCategoryDataset().apply {
                    addValue(means.getValue("High"), yAxisLabel, "High frequency")
                    addValue(means.getValue("Low"), yAxisLabel, "Low frequency")
                }
                chartWindow?.dispose()
                chartWindow = JFrame(title).apply {
                    defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
                    add(ChartPanel(ChartFactory.createBarChart(
                        title,
                        "Frequency",
                        yAxisLabel,
                        dataset,
                        PlotOrientation.VERTICAL,
                        false,
                        true,
                        false
                    ).apply { applySimbrainChartTheme() }))
                    setSize(850, 500)
                    setLocationRelativeTo(this@desktop.frame)
                    isVisible = true
                }
            }
        }
        suspend fun showFrequencyChart() {
            val means = reader.evaluateAll().groupBy { it.word.frequencyBand }
                .mapValues { (_, values) -> values.map(reader::relativeError).average() }
            showBarChart("Word-frequency effect", "Relative phonological error", means)
        }
        suspend fun showFrequencyResults() {
            val evaluations = reader.evaluateAll()
            val means = evaluations.groupBy { it.word.frequencyBand }.mapValues { (_, values) -> values.map(reader::relativeError).average() }
            val state = if (reader.trainingPresentations == 0) "Untrained baseline" else "After ${reader.trainingPresentations} presentations"
            val rows: Array<Array<Any>> = evaluations.map { result -> arrayOf<Any>(
                    result.word.word,
                    result.word.frequency,
                    result.word.frequencyBand,
                    result.word.regularity.name.lowercase(),
                    result.word.pronunciation.joinToString(" "),
                    result.decodedPronunciation.joinToString(" "),
                    "%.5f".format(result.error),
                    "%.5f".format(reader.relativeError(result))
                ) }.toTypedArray()
            val columns = arrayOf<Any>("Word", "Frequency", "Band", "Regularity", "Target", "Decoded", "MSE", "Relative MSE")
            val table = JTable(DefaultTableModel(rows, columns)).apply { isEnabled = false }
            withContext(Dispatchers.Swing) {
                resultsWindow?.dispose()
                val summary = JPanel(GridLayout(0, 1, 8, 8)).apply {
                    border = BorderFactory.createEmptyBorder(12, 12, 12, 12)
                    add(JLabel(state))
                    add(JLabel("High-frequency mean error: ${"%.5f".format(means.getValue("High"))}"))
                    add(JLabel("Low-frequency mean error: ${"%.5f".format(means.getValue("Low"))}"))
                }
                resultsWindow = JFrame("Word-frequency results").apply {
                    defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
                    layout = BorderLayout()
                    add(summary, BorderLayout.NORTH)
                    add(JScrollPane(table), BorderLayout.CENTER)
                    setSize(1250, 850)
                    setLocationRelativeTo(this@desktop.frame)
                    isVisible = true
                }
            }
        }
        fun showTrainingFrame() {
            trainingFrame?.takeIf { it.isDisplayable }?.let {
                it.toFront()
                return
            }
            val samples = JSpinner(SpinnerNumberModel(sampleCount, 1, 1_000_000, 100))
            val sampling = JComboBox(SamplingMode.entries.toTypedArray()).apply { selectedItem = samplingMode }
            val learningRate = JSpinner(SpinnerNumberModel(reader.model.trainerConfig.learningRate, 0.0001, 1.0, 0.001))
            val errorText = JLabel("Sampled error: not yet trained")
            val status = JLabel("Choose an exposure schedule and train.")
            val trainButton = JButton("Train")
            val playButton = JButton("Play")
            val resetButton = JButton("Reset weights")
            val errorModel = TimeSeriesModel().apply { addTimeSeries("Sampled error") }
            val errorPanel = TimeSeriesPlotPanel(errorModel).apply {
                preferredSize = Dimension(460, 260)
                seriesRemovalEnabled = false
                chartPanel.chart.xyPlot.domainAxis.label = "Training block"
                chartPanel.chart.xyPlot.rangeAxis.label = "Sampled error"
            }
            val trainingControls = JPanel(GridLayout(0, 2, 8, 8)).apply {
                border = BorderFactory.createEmptyBorder(12, 12, 12, 12)
                add(JLabel("Presentations per block"))
                add(samples)
                add(JLabel("Sampling"))
                add(sampling)
                add(JLabel("Learning rate"))
                add(learningRate)
                add(errorText)
                add(status)
                add(trainButton)
                add(playButton)
                add(resetButton)
                add(JLabel())
            }
            val frame = JFrame("Triangle Reading Training").apply {
                defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
                layout = BorderLayout()
                add(trainingControls, BorderLayout.NORTH)
                add(errorPanel, BorderLayout.CENTER)
                pack()
                setLocationRelativeTo(null)
            }
            trainingFrame = frame
            var playingWorker: SwingWorker<Unit, Double>? = null
            fun startTraining(untilStopped: Boolean) {
                sampleCount = samples.value as Int
                samplingMode = sampling.selectedItem as SamplingMode
                reader.model.trainerConfig.learningRate = learningRate.value as Double
                val requestedSamples = sampleCount
                val requestedMode = samplingMode
                trainButton.isEnabled = false
                resetButton.isEnabled = false
                samples.isEnabled = false
                sampling.isEnabled = false
                learningRate.isEnabled = false
                playButton.text = if (untilStopped) "Stop" else "Play"
                status.text = if (untilStopped) "Training until stopped..." else "Training $requestedSamples sampled presentations..."
                val worker = object : SwingWorker<Unit, Double>() {
                    override fun doInBackground() {
                        do {
                            publish(reader.trainSampled(requestedSamples, requestedMode))
                        } while (untilStopped && !isCancelled)
                    }

                    override fun process(errors: MutableList<Double>) {
                        errors.forEach { error ->
                            errorModel.timeSeriesList[0].series.add(errorModel.timeSeriesList[0].series.itemCount.toDouble(), error)
                            errorText.text = "Sampled error: ${"%.5f".format(error)}"
                        }
                    }

                    override fun done() {
                        status.text = if (isCancelled) "Training stopped" else "Training complete"
                        trainButton.isEnabled = true
                        resetButton.isEnabled = true
                        samples.isEnabled = true
                        sampling.isEnabled = true
                        learningRate.isEnabled = true
                        playButton.text = "Play"
                        playButton.isEnabled = true
                        playingWorker = null
                    }
                }
                if (untilStopped) playingWorker = worker
                worker.execute()
            }
            trainButton.addActionListener {
                startTraining(untilStopped = false)
            }
            playButton.addActionListener {
                val worker = playingWorker
                if (worker == null) {
                    startTraining(untilStopped = true)
                } else {
                    status.text = "Stopping after this training block..."
                    playButton.isEnabled = false
                    worker.cancel(false)
                }
            }
            resetButton.addActionListener {
                reader.reset()
                errorModel.clearData()
                errorText.text = "Sampled error: not yet trained"
                status.text = "Weights reset"
                feedback.text = EMPTY_TEST_FEEDBACK
            }
            frame.isVisible = true
        }
        controls = createControlPanel("Triangle reading controls", SIM_WINDOW_GAP, SIM_WINDOW_GAP) {
            addButton("Training...", context = Dispatchers.Swing) {
                feedback.text = EMPTY_TEST_FEEDBACK
                showTrainingFrame()
            }
            addSeparator()
            addComboBox("Word", reader.dataset.words, selectedWord) { selectedWord = it }
            addButton("Test word") {
                val result = reader.evaluate(selectedWord)
                feedback.text = if (reader.trainingPresentations == 0) {
                    "Input:          ${selectedWord.word}\nTarget output:  ${selectedWord.pronunciation.joinToString(" ")}\nDecoded output: (untrained)\nError:          —"
                } else {
                    "Input:          ${selectedWord.word}\nTarget output:  ${selectedWord.pronunciation.joinToString(" ")}\nDecoded output: ${result.decodedPronunciation.joinToString(" ")}\nError:          ${"%.5f".format(result.error)}"
                }
            }
            addButton("Show word-frequency chart") {
                feedback.text = EMPTY_TEST_FEEDBACK
                showFrequencyChart()
            }
            addButton("Show results table") {
                feedback.text = EMPTY_TEST_FEEDBACK
                showFrequencyResults()
            }
            addSeparator()
            addButton("Reset") {
                feedback.text = EMPTY_TEST_FEEDBACK
                reader.reset()
            }
            addSeparator()
            feedback = JTextArea(EMPTY_TEST_FEEDBACK).apply {
                isEditable = false
                isOpaque = false
                lineWrap = true
                wrapStyleWord = true
                font = Font(Font.MONOSPACED, Font.PLAIN, 12)
                preferredSize = Dimension(260, 42)
            }
            addComponent(feedback)
        }.awaitLayout()
        place(networkComponent, controls.rightEdgeWithGap(), SIM_WINDOW_GAP, 1000, 680)
    }
    addSidebarInfo(
        """
        # Triangle Reading

        This compact pedagogical model learns a distributed mapping from five letter positions to four phoneme positions. It is inspired by connectionist triangle models of reading, but is not an exact replication of a published architecture.

        # Simulation Details

        ## Network and representations

        Orthography is a five-position letter code with 27 units per position: `a`–`z` plus blank. Phonology uses four phoneme positions, each represented by 18 articulatory features. A 40-unit hidden layer maps between them.

        ## Training

        Special training samples words from the development lexicon. In the frequency-weighted condition, a word's frequency changes how often it is experienced, not its input representation. The training window shows sampled error by training block and provides uniform sampling as a control condition.

        ## Control Panel Settings

        **Training...** opens the sampled-training window. Its **Train** button runs one block, **Play** repeats blocks until stopped, and **Reset weights** starts over. Its settings choose presentations per block, sampling schedule, and learning rate.

        **Word** selects the item used by **Test word**, which clamps its spelling and displays the network's phonological activation pattern.

        **Show word-frequency chart** evaluates the current network and opens the frequency-condition chart. It does not train or reset the model. Its baseline-relative error measure makes the high- and low-frequency bars both `1.0` immediately after reset, then shows their relative improvement after training.

        **Show results table** opens the enlarged per-word table.

        **Reset** reinitializes the network.

        # What to Do

        1. Select a word and press **Test word** to inspect its target phonemes, decoded output, and error.
        2. Press **Training...** to choose a presentation count, learning rate, and either uniform or frequency-weighted sampling.
        3. Press **Show word-frequency chart** to inspect the current network. It evaluates only; it does not change the weights.

        # Experiments

        ## Word-frequency effect

        Reset the model, then press **Show word-frequency chart**: both bars begin at `1.0`. The result window lists raw and baseline-relative error for every word. Condition summaries and the chart use relative error, which controls for differences in phonological target-vector complexity. Train with frequency-weighted experience, then show the chart again; the expected qualitative pattern is lower relative error for the high-frequency group. Switch to uniform sampling as a control: removing differential experience should reduce that advantage.

        # Planned extensions

        Future experiments will add recurrent phonological settling, semantic pathways, frequency × regularity analyses, pseudoword tests, and lesions. The present model is deliberately feed-forward.

        # Credits

        Inspired by Seidenberg and McClelland (1989), Plaut et al. (1996), and Harm and Seidenberg (1999, 2004). Built for Simbrain.

        ## Links

        - [Seidenberg & McClelland (1989), *A distributed, developmental model of word recognition and naming*](https://pubmed.ncbi.nlm.nih.gov/2798649/)
        - [Plaut et al. (1996), *Understanding normal and impaired word reading*](https://pubmed.ncbi.nlm.nih.gov/8650300/)
        - [Harm & Seidenberg (2004), *Computing the meanings of words in reading*](https://doi.org/10.1037/0033-295X.111.3.662)
        - [Simbrain documentation](https://docs.simbrain.net/docs/)
        """.trimIndent()
    )
}
