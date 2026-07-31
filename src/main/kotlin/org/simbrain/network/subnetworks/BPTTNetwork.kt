/**
 * A recurrent network trained by backpropagation through time.
 *
 * Structurally this is a three layer feed-forward network plus a single weight matrix from the hidden
 * layer back to itself. It is deliberately a sibling of [SRNNetwork] rather than an option on it: an
 * SRN approximates the recurrent gradient by treating the previous hidden state as a fixed input,
 * while this network unrolls over time and lets the gradient flow back through several steps. Having
 * both lets the approximation be compared against the thing it approximates.
 *
 * The unrolling itself lives in [org.simbrain.network.trainers.accumulateBPTT]; this class owns the
 * structure, the recurrent matrix, and the memory that carries across timesteps.
 */
package org.simbrain.network.subnetworks

import org.simbrain.network.core.*
import org.simbrain.network.trainers.*
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.network.util.Alignment
import org.simbrain.network.util.Direction
import org.simbrain.network.util.alignNetworkModels
import org.simbrain.network.util.offsetNetworkModel
import org.simbrain.util.UserParameter
import org.simbrain.util.copy
import org.simbrain.util.point
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.workspace.Consumable
import org.simbrain.workspace.Producible
import smile.math.matrix.Matrix
import java.awt.geom.Point2D

class BPTTNetwork : FeedForward, SupervisedNetwork {

    lateinit var hiddenLayer: NeuronArray

    /**
     * The recurrent weights. During training this matrix's copies across the unrolled timesteps are
     * tied, so every step's gradient is summed into a single update.
     */
    lateinit var hiddenToHidden: WeightMatrix

    override lateinit var trainingSet: TrainingDataset

    override lateinit var testingSet: TrainingDataset

    @delegate:Transient
    override val layers: LinkedHashSet<Layer> by lazy {
        computeOrderedUpdatePath(setOf(inputLayer), outputLayer)
    }

    constructor(
        numInputNodes: Int = 10,
        numHiddenNodes: Int = 10,
        numOutputNodes: Int = 10,
        initialPosition: Point2D = point(0, 0)
    ) : super(
        intArrayOf(numInputNodes, numHiddenNodes, numOutputNodes),
        initialPosition
    ) {

        inputLayer.label = "Input"
        inputLayer.isClamped = true

        hiddenLayer = layerList[1].also {
            it.updateRule = SigmoidalRule()
            it.label = "Hidden"
        }

        outputLayer.label = "Output"
        outputLayer.updateRule = SigmoidalRule()

        hiddenToHidden = WeightMatrix(hiddenLayer, hiddenLayer)
        hiddenToHidden.randomize()
        addModels(hiddenToHidden)

        // The recurrent arrow is a fixed 200px circle drawn to the left of the hidden layer. At
        // FeedForward's default spacing the neighbouring weight matrices' labels sit inside its
        // vertical span, so the layers are spread out to clear it.
        betweenLayerInterval = RECURRENT_LAYER_INTERVAL
        listOf(inputLayer to hiddenLayer, hiddenLayer to outputLayer).forEach { (lower, upper) ->
            alignNetworkModels(lower, upper, Alignment.VERTICAL)
            offsetNetworkModel(
                lower, upper, Direction.NORTH,
                (betweenLayerInterval / 2).toDouble(), 100.0, 200.0
            )
        }

        trainingSet = createDiagonalDataset(numInputNodes, numOutputNodes, shiftAmount = 1)
        testingSet = TrainingDataset(mutableListOf(), mutableListOf(), numInputNodes, numOutputNodes)

        customInfo = InfoText(stateInfoText)

        setLocation(initialPosition.x, initialPosition.y)
    }

    @XStreamConstructor
    protected constructor() : super()

    override var trainerConfig = BPTTTrainerConfig(lossFunctionProvider = ::possibleLossFunctions)

    /**
     * Nullable rather than lateinit because [FeedForward]'s constructor sets a location, and
     * [Subnetwork]'s location setter reads this before any of this class's initializers have run.
     */
    override var customInfo: InfoText? = null
        private set

    /**
     * How many timesteps the network is unrolled over. Shown on the canvas because it is the one
     * training setting that changes what the network is able to learn rather than just how fast.
     */
    val stateInfoText: String
        get() = "Unrolled over ${trainerConfig.truncationDepth} steps"

    fun updateStateInfoText() {
        customInfo?.text = stateInfoText
        events.customInfoUpdated.fire()
    }

    /**
     * Whether to draw the network unrolled over time alongside its rolled-up form. Purely a display
     * choice: unrolling during training is virtual, so this changes nothing about the model.
     */
    var unrolledView: Boolean = false
        set(value) {
            field = value
            events.displayModeChanged.fire()
        }

    /**
     * The last few timesteps' activations by layer, oldest first, for the unrolled view to draw. The
     * final entry is always the step the layers currently hold, so the view can line the list up against
     * its columns by treating the rolled network as the newest step.
     *
     * Filled two ways. Training publishes a whole unrolled window at once; ordinary iteration appends one
     * step per update. Both leave the newest step in the live layers, so the two agree. Empty until
     * something runs with the view showing, since neither path collects anything otherwise.
     */
    var unrolledActivations: List<Map<Layer, Matrix>> = emptyList()
        private set

    /** Replace the history with a whole unrolled window, whose last step the layers are left holding. */
    fun publishUnrolledActivations(trace: List<Map<Layer, Matrix>>) {
        unrolledActivations = trace
        events.displayDataUpdated.fire()
    }

    /**
     * Append the step the layers currently hold, dropping anything that has fallen out of the window.
     * Called after each ordinary forward pass, which is the only way the unrolled view can be filled
     * outside training: one iteration computes one step, so the columns can only show what has already
     * happened.
     */
    private fun recordTimestep() {
        val step = listOf<Layer>(inputLayer, hiddenLayer, outputLayer).associateWith { it.activations.clone() }
        val depth = trainerConfig.truncationDepth.coerceAtLeast(1)
        unrolledActivations = (unrolledActivations + step).takeLast(depth)
        events.displayDataUpdated.fire()
    }

    override fun onTrainerConfigChanged() = updateStateInfoText()

    override val name: String
        get() = "BPTT"

    override fun createTrainer(network: Network) = BPTTTrainer(network, this)

    /**
     * Clear the memory the recurrent connection carries between timesteps. Called at the start of a
     * pass over the training sequence so that a pass does not inherit state from the previous one.
     */
    fun resetRecurrentState() {
        hiddenLayer.activations = Matrix(hiddenLayer.size, 1)
        hiddenLayer.clearInputs()
        // The history describes steps that led to a state the network no longer has.
        unrolledActivations = emptyList()
    }

    context(Network)
    override fun accumulateInputs() {
        inputLayer.accumulateInputs()
    }

    context(Network)
    override fun update() {
        forwardPass()
    }

    /**
     * One timestep. The recurrent matrix contributes the hidden layer's previous activations because
     * the ordered update path reaches the hidden layer before overwriting it.
     *
     * This is the single point every non-training advance passes through, whether from a workspace tick
     * or from applying a row in the training dialog, which is why the history is recorded here. Training
     * does not come this way: [org.simbrain.network.trainers.accumulateBPTT] drives the layers directly
     * and publishes its window in one go.
     */
    context(Network)
    override fun forwardPass() {
        layers.forwardPass(listOf(inputLayer.activations), listOf(inputLayer))
        if (unrolledView) recordTimestep()
    }

    // Forwarded from output layer
    @Producible
    fun getOutputs(): DoubleArray {
        return outputLayer.activationArray
    }

    // Forwards to input layer
    @Consumable
    fun addInputs(inputs: DoubleArray) {
        inputLayer.addInputs(inputs)
    }

    override fun randomize(randomizer: ProbabilityDistribution?) {
        super.randomize(randomizer)
        hiddenToHidden.randomize(randomizer)
    }

    override fun initWeights() {
        (wmList + hiddenToHidden).forEach { wm -> trainerConfig.weightInitializationStrategy.initializeWeights(wm) }
    }

    override fun initBiases() {
        (layerList - inputLayer).forEach {
            it.clear()
            it.randomizeBiases()
        }
    }

    override fun toString(): String {
        return """
            Name: $displayName
            Type: BPTT Network
            Input Layer: ${inputLayer.size} neurons
            Hidden Layer: ${hiddenLayer.size} neurons
            Output Layer: ${outputLayer.size} neurons
            Truncation Depth: ${trainerConfig.truncationDepth}
        """.trimIndent()
    }

    override fun copy(): BPTTNetwork {
        val copy = BPTTNetwork(inputLayer.size, hiddenLayer.size, outputLayer.size)

        copy.layerList.zip(layerList).forEach { (copyLayer, originalLayer) ->
            copyLayer.copyFrom(originalLayer)
        }
        copy.wmList.zip(wmList).forEach { (copyWeightMatrix, originalWeightMatrix) ->
            copyWeightMatrix.copyFrom(originalWeightMatrix)
        }
        copy.hiddenToHidden.copyFrom(hiddenToHidden)

        copy.trainingSet = trainingSet.copy()
        copy.testingSet = testingSet.copy()
        copy.trainerConfig = trainerConfig.copy()
        copy.customInfo = InfoText(copy.stateInfoText)

        return copy
    }

    /**
     * Helper class for creating BPTT networks.
     */
    companion object {
        /**
         * Vertical spacing between layers, wide enough that the hidden layer's recurrent arrow does
         * not collide with the weight matrices above and below it.
         */
        const val RECURRENT_LAYER_INTERVAL = 500
    }

    class BPTTCreator(val initialPosition: Point2D) : EditableObject {

        @UserParameter(label = "Number of inputs", order = 10)
        var nin = 5

        @UserParameter(label = "Number of hidden", order = 20)
        var nhidden = 5

        @UserParameter(label = "Number of outputs", order = 30)
        var nout = 5

        override val name = "BPTT Network"

        fun create(): BPTTNetwork {
            return BPTTNetwork(nin, nhidden, nout, initialPosition)
        }
    }
}
