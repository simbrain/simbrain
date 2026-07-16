/**
 * A cognitive-map simulation in which a wandering person learns landmark
 * transitions while a frozen autoencoder represents continuous landmark states.
 * The simulation links the environment graph, neural state, projection, and field plot.
 */
package org.simbrain.custom_sims.simulations.cogsci

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.simbrain.custom_sims.*
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.core.addNeuronCollection
import org.simbrain.network.trainers.SupervisedModel
import org.simbrain.network.trainers.SupervisedTrainer
import org.simbrain.network.trainers.TrainingDataset
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.util.SmellSource
import org.simbrain.util.decayfunctions.GaussianDecayFunction
import org.simbrain.util.genericframe.GenericJInternalFrame
import org.simbrain.util.piccolo.TileMap
import org.simbrain.util.place
import org.simbrain.util.projection.AuxDataColoringManager
import org.simbrain.util.projection.DataPoint
import org.simbrain.util.updateAction
import org.simbrain.util.widgets.FieldImagePanel
import org.simbrain.workspace.updater.UpdateComponent
import org.simbrain.world.odorworld.behaviors.Wander
import org.simbrain.world.odorworld.entities.EntityType
import java.awt.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.swing.*
import kotlin.math.exp
import kotlin.math.min

val landmarkCognitiveMap = newSim {
    workspace.clearWorkspace()

    val odorWorldComponent = addOdorWorldComponent("Landmark World")
    val odorWorld = odorWorldComponent.world.apply {
        tileMap = TileMap(20, 20)
        wrapAround = true
        isObjectsBlockMovement = false
    }
    odorWorld.tileMap.layers.first().let { layer ->
        for (x in 0 until odorWorld.tileMap.width) {
            for (y in 0 until odorWorld.tileMap.height) {
                layer[x, y] = 6
            }
        }
    }

    val agent = odorWorld.addEntity(320, 320, EntityType.Amy).apply {
        name = "Person"
        heading = 0.0
        behavior = Wander().apply { maxSpeed = 1.5 }
    }

    val landmarks = listOf(
        LandmarkDefinition("Swiss", 100.0, 100.0, EntityType.Swiss),
        LandmarkDefinition("Flower", 320.0, 90.0, EntityType.Flower),
        LandmarkDefinition("Fish", 540.0, 100.0, EntityType.Fish),
        LandmarkDefinition("Candle", 100.0, 270.0, EntityType.Candle),
        LandmarkDefinition("Pansy", 540.0, 270.0, EntityType.Pansy),
        LandmarkDefinition("Tulip", 100.0, 500.0, EntityType.Tulip),
        LandmarkDefinition("Dandelions", 320.0, 540.0, EntityType.Dandelions),
        LandmarkDefinition("Geraniums", 540.0, 500.0, EntityType.Geraniums),
        LandmarkDefinition("Flax", 210.0, 210.0, EntityType.Flax),
        LandmarkDefinition("Gouda", 430.0, 210.0, EntityType.Gouda),
        LandmarkDefinition("Bell", 210.0, 430.0, EntityType.Bell),
        LandmarkDefinition("Candy", 430.0, 430.0, EntityType.Candy)
    )
    val stateEncoder = LandmarkStateEncoder(
        landmarks = landmarks,
        worldWidth = odorWorld.width,
        worldHeight = odorWorld.height
    )
    landmarks.forEachIndexed { index, landmark ->
        odorWorld.addEntity(landmark.x.toInt(), landmark.y.toInt(), landmark.entityType).apply {
            name = landmark.label
            smellSource = SmellSource(DoubleArray(landmarks.size) { channel -> if (channel == index) 1.0 else 0.0 }).apply {
                decayFunction = GaussianDecayFunction(220.0)
            }
        }
    }

    val networkComponent = addNetworkComponent("Distributed Landmark State")
    val network = networkComponent.network
    val sensoryState = network.addNeuronCollection(stateEncoder.labels.size).apply {
        label = "Sensory State"
        isClamped = true
        betweenNeuronInterval = 60
        setLayoutBasedOnSize()
        applyLayout()
        setLocation(-15.0, 115.0)
    }
    val mainState = network.addNeuronCollection(16).apply {
        label = "Main State"
        neuronList.forEach { it.updateRule = SigmoidalRule() }
        betweenNeuronInterval = 38
        setLayoutBasedOnSize()
        applyLayout()
        setLocation(311.0, 107.0)
    }
    val objectReadouts = network.addNeuronCollection(stateEncoder.labels.size).apply {
        label = "Object Readouts"
        neuronList.forEach { it.updateRule = SigmoidalRule() }
        betweenNeuronInterval = 60
        setLayoutBasedOnSize()
        applyLayout()
        setLocation(625.0, 115.0)
    }
    val sensoryToMain = SynapseGroup(sensoryState, mainState).apply { label = "i→h" }
    val mainToReadouts = SynapseGroup(mainState, objectReadouts).apply { label = "h→o" }
    val autoencoder = SupervisedModel(sensoryState, objectReadouts)
    network.addNetworkModels(sensoryState, mainState, objectReadouts, sensoryToMain, mainToReadouts, autoencoder)
    fun abbreviatedLabel(label: String) = when (label) {
        "Dandelions" -> "Dandel."
        "Geraniums" -> "Geran."
        else -> label
    }
    sensoryState.neuronList.zip(stateEncoder.labels).forEach { (neuron, label) -> neuron.label = abbreviatedLabel(label) }
    objectReadouts.neuronList.zip(stateEncoder.labels).forEach { (neuron, label) -> neuron.label = abbreviatedLabel(label) }
    initializeDeterministicWeights(listOf(sensoryToMain, mainToReadouts), listOf(mainState, objectReadouts))

    val trainingNetwork = Network()
    val trainingSensoryState = trainingNetwork.addNeuronCollection(stateEncoder.labels.size).apply { isClamped = true }
    val trainingMainState = trainingNetwork.addNeuronCollection(16).apply {
        neuronList.forEach { it.updateRule = SigmoidalRule() }
    }
    val trainingObjectReadouts = trainingNetwork.addNeuronCollection(stateEncoder.labels.size).apply {
        neuronList.forEach { it.updateRule = SigmoidalRule() }
    }
    val trainingSensoryToMain = SynapseGroup(trainingSensoryState, trainingMainState)
    val trainingMainToReadouts = SynapseGroup(trainingMainState, trainingObjectReadouts)
    val trainingAutoencoder = SupervisedModel(trainingSensoryState, trainingObjectReadouts)
    trainingNetwork.addNetworkModels(
        trainingSensoryState,
        trainingMainState,
        trainingObjectReadouts,
        trainingSensoryToMain,
        trainingMainToReadouts,
        trainingAutoencoder
    )
    initializeDeterministicWeights(
        listOf(trainingSensoryToMain, trainingMainToReadouts),
        listOf(trainingMainState, trainingObjectReadouts)
    )
    val trainingSamples = stateEncoder.trainingSamples(odorWorld.width, odorWorld.height)
    val trainingComplete = AtomicBoolean(false)
    val fieldVisible = AtomicBoolean(true)

    val graph = LandmarkGraph()
    val graphPanel = EnvironmentGraphPanel(graph, landmarks.map(LandmarkDefinition::label))
    val legendPanel = ProjectionLegendPanel(stateEncoder.labels)
    val fieldImagePanel = FieldImagePanel(
        source = {
            if (trainingComplete.get()) {
                stateEncoder.labels.zip(objectReadouts.activationArray.toList()).map { (label, activation) ->
                    abbreviatedLabel(label) to activation
                }
            } else {
                emptyList()
            }
        }
    ).apply {
        threshold = 0.60
        maxItems = 8
        preferredSize = Dimension(580, 330)
    }
    val projectionPlot = addProjectionPlot("Distributed State Space").apply {
        projector.tolerance = 0.1
        projector.showLabels = false
        projector.connectPoints = false
        projector.baseColor = Color(85, 85, 85)
        projector.coloringManager = AuxDataColoringManager()
    }

    workspace.updater.updateManager.clear()
    workspace.updater.updateManager.addAction(UpdateComponent(odorWorldComponent))
    workspace.updater.updateManager.addAction(updateAction("Encode landmark sensory state") {
        sensoryState.activationArray = stateEncoder.encode(agent.x, agent.y)
    })
    workspace.updater.updateManager.addAction(updateAction("Update distributed state") {
        if (trainingComplete.get()) {
            with(network) { autoencoder.forwardPass() }
            network.events.updated.fire()
        }
    })
    workspace.updater.updateManager.addAction(updateAction("Record landmark state") {
        val label = stateEncoder.nearestLabel(agent.x, agent.y)
        graph.observe(label)
        if (trainingComplete.get()) {
            val readouts = objectReadouts.activationArray.copyOf()
            val composition = formatComposition(stateEncoder.labels, readouts)
            legendPanel.observe(readouts)
            projectionPlot.addPoint(
                DataPoint(
                    upstairsPoint = mainState.activationArray.copyOf(),
                    label = composition,
                    aux = dominantReadoutColor(stateEncoder.labels, readouts)
                )
            )
        }
        graphPanel.repaint()
        if (fieldVisible.get()) fieldImagePanel.repaint()
    })
    workspace.updater.updateManager.addAction(UpdateComponent(projectionPlot))

    lateinit var fieldFrame: GenericJInternalFrame
    withGui {
        place(odorWorldComponent, SIM_WINDOW_GAP, SIM_WINDOW_GAP, 600, 600)
        delay(100)
        odorWorldComponent.scale(0.85)
        place(networkComponent, 620, 10, 481, 352)
        place(projectionPlot, 622, 377, 485, 392)
        val graphFrame = GenericJInternalFrame("Environment Graph", true, true, true, true).apply {
            layout = BorderLayout()
            add(graphPanel, BorderLayout.CENTER)
            add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                add(JButton("Reset map").apply {
                    addActionListener {
                        graph.reset()
                        graphPanel.repaint()
                    }
                })
            }, BorderLayout.SOUTH)
            setBounds(1110, 10, 420, 356)
            isVisible = true
        }
        addInternalFrame(graphFrame)
        val stateControlsFrame = createControlPanel("State Controls", SIM_WINDOW_GAP, SIM_WINDOW_GAP + 600 + SIM_WINDOW_GAP) {
            addCheckBox("Show field plot", true) { visible ->
                fieldVisible.set(visible)
                fieldFrame.isVisible = visible
                if (visible) fieldImagePanel.repaint()
            }
            addFormattedNumericTextField("Projection tolerance", projectionPlot.projector.tolerance) { tolerance ->
                projectionPlot.projector.tolerance = tolerance.coerceAtLeast(0.0)
            }
        }
        stateControlsFrame.setBounds(10, 620, 600, 300)
        val fieldThresholdSlider = JSlider(0, 100, 60).apply {
            majorTickSpacing = 25
            minorTickSpacing = 5
            paintTicks = true
            paintLabels = true
            addChangeListener {
                fieldImagePanel.threshold = value / 100.0
                fieldImagePanel.repaint()
            }
        }
        val fieldControlBar = JPanel(BorderLayout(6, 0)).apply {
            border = BorderFactory.createEmptyBorder(2, 8, 2, 8)
            add(JLabel("Threshold"), BorderLayout.WEST)
            add(fieldThresholdSlider, BorderLayout.CENTER)
        }
        fieldFrame = GenericJInternalFrame("Field Plot", true, true, true, true).apply {
            layout = BorderLayout()
            add(fieldImagePanel, BorderLayout.CENTER)
            add(fieldControlBar, BorderLayout.SOUTH)
            setBounds(1112, 379, 420, 263)
            isVisible = true
        }
        addInternalFrame(fieldFrame)
        val legendFrame = GenericJInternalFrame("Projection Color Legend", true, true, true, true).apply {
            layout = BorderLayout()
            add(legendPanel, BorderLayout.CENTER)
            setBounds(627, 771, 486, 200)
            isVisible = true
        }
        addInternalFrame(legendFrame)
    }

    SwingUtilities.invokeLater {
        workspace.launch(Dispatchers.Default) {
            trainingAutoencoder.pretrain(trainingNetwork, trainingSamples)
            SwingUtilities.invokeLater {
                copyTrainedParameters(
                    listOf(trainingSensoryToMain, trainingMainToReadouts),
                    listOf(sensoryToMain, mainToReadouts),
                    listOf(trainingMainState, trainingObjectReadouts),
                    listOf(mainState, objectReadouts)
                )
                trainingComplete.set(true)
            }
        }
    }

    addSidebarInfo(
        """
        # Landmark Cognitive Map

        A person wanders through a grass-filled OdorWorld containing twelve landmarks. The symbolic graph records directed transitions between landmarks, while the neural network represents the continuously varying sensory state.

        Each landmark contributes a distance-weighted value to its own sensory channel. A thirteenth `Background` channel represents low strongest-landmark evidence. Several objects can therefore be simultaneously active, while background remains a bona fide neural state.

        A frozen autoencoder converts this 13-channel state into a 16-neuron distributed `Main State` and reconstructs continuous object readouts from that state alone. The `Distributed State Space` plot records representative main states. Point colors show the strongest decoded readout, including `Background`; use the color legend or hover over a point to inspect its decoded composition.

        The `Field Plot` visualizes these same object readouts. Its labels, size, and intensity therefore reflect the network's current decoded state rather than landmark location or raw sensor input.

        Use **Reset map** to clear the symbolic transition graph.
        """.trimIndent()
    )
}

/** A named object placed at a fixed position in the OdorWorld. */
data class LandmarkDefinition(
    val label: String,
    val x: Double,
    val y: Double,
    val entityType: EntityType
)

/**
 * Converts an agent location into a continuous landmark state.
 *
 * Every landmark gets a Gaussian distance-weighted activation. The final
 * Background channel represents the absence of a strongly active landmark.
 */
class LandmarkStateEncoder(
    val landmarks: List<LandmarkDefinition>,
    private val distanceScale: Double = 110.0,
    private val recognitionRadius: Double = 48.0,
    private val worldWidth: Double? = null,
    private val worldHeight: Double? = null
) {
    /** Input and readout labels, including the explicit Background state. */
    val labels = landmarks.map(LandmarkDefinition::label) + "Background"

    /** Encode a world location as a multi-landmark neural input vector. */
    fun encode(x: Double, y: Double): DoubleArray {
        val landmarkActivations = DoubleArray(landmarks.size) { index ->
            val landmark = landmarks[index]
            val distance = distanceTo(landmark, x, y)
            exp(-0.5 * (distance / distanceScale) * (distance / distanceScale))
        }
        val background = (1.0 - (landmarkActivations.maxOrNull() ?: 0.0)).coerceIn(0.0, 1.0)
        return landmarkActivations + background
    }

    /** Return the nearby landmark used for the discrete environment graph. */
    fun nearestLabel(x: Double, y: Double): String? = landmarks
        .minByOrNull { landmark -> distanceTo(landmark, x, y) }
        ?.takeIf { landmark -> distanceTo(landmark, x, y) <= recognitionRadius }
        ?.label

    /** Sample the full world to produce deterministic autoencoder training states. */
    fun trainingSamples(width: Double, height: Double, stepsPerDimension: Int = 16): List<DoubleArray> = buildList {
        for (row in 0 until stepsPerDimension) {
            for (column in 0 until stepsPerDimension) {
                val x = width * (column + 0.5) / stepsPerDimension
                val y = height * (row + 0.5) / stepsPerDimension
                add(encode(x, y))
            }
        }
    }

    private fun distanceTo(landmark: LandmarkDefinition, x: Double, y: Double): Double {
        val dx = wrappedDelta(x - landmark.x, worldWidth)
        val dy = wrappedDelta(y - landmark.y, worldHeight)
        return kotlin.math.hypot(dx, dy)
    }

    private fun wrappedDelta(delta: Double, dimension: Double?): Double {
        if (dimension == null) return delta
        val absoluteDelta = kotlin.math.abs(delta) % dimension
        return min(absoluteDelta, dimension - absoluteDelta)
    }
}

/** Initialize the visible and off-screen autoencoders identically for repeatable runs. */
fun initializeDeterministicWeights(synapseGroups: List<SynapseGroup>, trainableLayers: List<NeuronCollection>) {
    synapseGroups.forEachIndexed { groupIndex, group ->
        group.synapses.forEachIndexed { synapseIndex, synapse ->
            synapse.strength = ((groupIndex * 97 + synapseIndex * 17) % 201 - 100) / 500.0
        }
    }
    trainableLayers.forEachIndexed { layerIndex, layer ->
        layer.neuronList.forEachIndexed { neuronIndex, neuron ->
            neuron.bias = ((layerIndex * 23 + neuronIndex * 11) % 41 - 20) / 200.0
        }
    }
}

/** Copy a finished off-screen training network into the network displayed in the workspace. */
fun copyTrainedParameters(
    sourceSynapseGroups: List<SynapseGroup>,
    targetSynapseGroups: List<SynapseGroup>,
    sourceLayers: List<NeuronCollection>,
    targetLayers: List<NeuronCollection>
) {
    sourceSynapseGroups.zip(targetSynapseGroups).forEach { (source, target) ->
        source.synapses.zip(target.synapses).forEach { (sourceSynapse, targetSynapse) ->
            targetSynapse.strength = sourceSynapse.strength
        }
    }
    sourceLayers.zip(targetLayers).forEach { (source, target) ->
        source.neuronList.zip(target.neuronList).forEach { (sourceNeuron, targetNeuron) ->
            targetNeuron.bias = sourceNeuron.bias
        }
    }
}

/** Summary reported by the bounded autoencoder pretraining loop. */
data class PretrainingResult(val epochs: Int, val reconstructionError: Double)

/** Train this autoencoder to reconstruct the supplied landmark-state samples. */
suspend fun SupervisedModel.pretrain(
    network: Network,
    samples: List<DoubleArray>,
    onProgress: (PretrainingResult) -> Unit = {}
): PretrainingResult {
    trainingSet = TrainingDataset(
        inputs = samples.map { it.toMutableList() }.toMutableList(),
        targets = samples.map { it.toMutableList() }.toMutableList()
    )
    testingSet = TrainingDataset(
        inputs = mutableListOf(),
        targets = mutableListOf(),
        inputSize = samples.first().size,
        targetSize = samples.first().size
    )
    trainerConfig.updateType = SupervisedTrainer.UpdateMethod.Epoch()
    trainerConfig.learningRate = 0.05
    val trainer = SupervisedTrainer(network, this)
    var error = reconstructionError(network, samples)
    var epoch = 0
    onProgress(PretrainingResult(epoch, error))
    while (epoch < 500 && error > 0.002) {
        trainer.trainOnce()
        epoch++
        error = reconstructionError(network, samples)
        if (epoch % 5 == 0 || epoch == 500 || error <= 0.002) {
            onProgress(PretrainingResult(epoch, error))
        }
    }
    return PretrainingResult(epoch, error)
}

/** Mean squared reconstruction error across a collection of landmark states. */
fun SupervisedModel.reconstructionError(network: Network, samples: List<DoubleArray>): Double {
    return samples.sumOf { sample ->
        inputLayer.setActivations(sample)
        with(network) { forwardPass() }
        outputLayer.outputArray.zip(sample).sumOf { (actual, target) ->
            val error = actual - target
            error * error
        }
    } / (samples.size * samples.first().size)
}

/** Format the strongest decoded readouts for projection-point tooltips. */
fun formatComposition(labels: List<String>, activations: DoubleArray, threshold: Double = 0.15): String {
    return labels.indices
        .map { index -> labels[index] to activations[index] }
        .filter { (_, activation) -> activation >= threshold }
        .sortedByDescending { (_, activation) -> activation }
        .take(2)
        .joinToString(" · ") { (label, activation) -> "$label (${String.format(Locale.US, "%.2f", activation)})" }
        .ifEmpty { "Background" }
}

/** Give each decoded landmark a stable projection color; Background is gray. */
fun dominantReadoutColor(labels: List<String>, activations: DoubleArray): Color {
    val index = activations.indices.maxByOrNull { activations[it] } ?: return Color.GRAY
    return if (labels[index] == "Background") {
        Color(120, 120, 120)
    } else {
        Color.getHSBColor(index.toFloat() / (labels.size - 1), 0.65f, 0.80f)
    }
}

/** Shows only decoded categories that have occurred, highlighting the current one. */
private class ProjectionLegendPanel(private val labels: List<String>) : JPanel() {

    private val observedLabels = ConcurrentHashMap.newKeySet<String>()
    private val activeLabel = AtomicReference<String?>(null)

    init {
        preferredSize = Dimension(460, 165)
        background = Color.WHITE
    }

    fun observe(activations: DoubleArray) {
        val index = activations.indices.maxByOrNull { activations[it] } ?: return
        val label = labels[index]
        observedLabels += label
        activeLabel.set(label)
        repaint()
    }

    override fun paintComponent(graphics: Graphics) {
        super.paintComponent(graphics)
        val g = graphics.create() as Graphics2D
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val columnWidth = width / 2
        labels.filter(observedLabels::contains).forEachIndexed { displayIndex, label ->
            val index = labels.indexOf(label)
            val column = displayIndex % 2
            val row = displayIndex / 2
            val x = 14 + column * columnWidth
            val y = 18 + row * 21
            if (label == activeLabel.get()) {
                g.color = Color(226, 239, 255)
                g.fillRoundRect(x - 6, y - 16, columnWidth - 10, 20, 6, 6)
            }
            g.color = dominantReadoutColor(labels, DoubleArray(labels.size) { item -> if (item == index) 1.0 else 0.0 })
            g.fillRoundRect(x, y - 12, 14, 14, 4, 4)
            g.color = Color.DARK_GRAY
            g.drawString(label, x + 21, y)
        }
        g.dispose()
    }
}
