package org.simbrain.network.core

import org.simbrain.network.gui.dialogs.NetworkPreferences.weightRandomizer
import org.simbrain.network.gui.nodes.ActivationSequenceProcessor
import org.simbrain.network.learningrules.StaticSynapseRule
import org.simbrain.network.learningrules.SynapseUpdateRule
import org.simbrain.network.spikeresponders.NonResponder
import org.simbrain.network.spikeresponders.SpikeResponder
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.MatrixDataHolder
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.workspace.Consumable
import org.simbrain.workspace.Producible
import smile.math.matrix.Matrix
import kotlin.math.min

/**
 * A dense weight matrix that connects a source and target [Layer] object. A default way of linking arbitrary
 * layers.
 *
 * Stored in a target-source format: The matrix has as many rows as the target layer and as many columns as the
 * source layer.
 * The matrix is multiplied by the source layer column to produce the output activations.
 *
 * Since in Simbrain the source layer is typically shown as a row already, it's easy to visualize
 * the rows of the weight matrix being dotted one at a time with the rows of the source layer, to
 * generate the target.
 *
 *A next good one might be Generic 3 objects.
 * In that one the central network is recurrent network and there is sparse input from the distributed olfactor inputs to the network.
 * The inputs work like agent trails, so you can say that. The simulations shows how networks develop cognitive maps of their environment.
 * Then in the “what to do” section you can tell them to run the sim, drag the mouse around the three objects and observe how a map develops in the map window.
 * When you get right on top of an object it labels the point with the corresponding object.
 * So you get a feel for how an agent develops a sense of an enviornment.
 * If it slows down you can press the eraser button in the map window to start mapping again.
 *
 */
class WeightMatrix(source: Layer, target: Layer) : Connector(source, target) {

    @UserParameter(label = "Increment amount", increment = .1, order = 20)
    var increment = .1

    /**
     * Whether weights can be changed by learning rules or not (they can still be manually updated).
     */
    @UserParameter(
        label = "Clamped",
        description = "If the weight matrix is clamped, local learning rules won't be applied",
        order = 30)
    var clamped = false
        set(value) {
            field = value
            events.clampChanged.fire()
        }

    @UserParameter(label = "Learning Rule", order = 100)
    var learningRule: SynapseUpdateRule<*, *> = StaticSynapseRule()

    /**
     * Only used if source connector's rule is spiking.
     */
    @UserParameter(label = "Spike Responder", showDetails = false, order = 200)
    var spikeResponder: SpikeResponder = NonResponder()
        set(value) {
            field = value
            spikeResponseData = value.createMatrixData(weights.nrow(), weights.ncol())
        }

    /**
     * Holds data for learning rule.
     */
    var learningRuleData: MatrixDataHolder by GuiEditable(
        initValue = EmptyMatrixData,
        order = 210,
        label = "Learning Rule Data",
        tab = "Data"
    )

    /**
     * Holds data for spike responder.
     */
    var spikeResponseData: MatrixDataHolder by GuiEditable(
        initValue = EmptyMatrixData,
        order = 220,
        label = "Spike Responder Data",
        tab = "Data",
        onUpdate = {
            val proposedDataHolder = widgetValue(::spikeResponder).createMatrixData(weights.nrow(), weights.ncol())
            if (widgetValue(::spikeResponseData)::class != proposedDataHolder::class) {
                refreshValue(proposedDataHolder)
            }
        }
    )

    /**
     * The weight matrix object. Overwriting this causes unexpected behavior in GUI elements so best practice is to
     * create a new matrix and copy its value to this one.
     */
    @get:Producible
    val weights: Matrix

    @UserParameter(label = "PSR Matrix", order = 300, tab = "Data")
    override var psrMatrix: Matrix

    /**
     * A binary matrix with 1s corresponding to entries of the weight matrix that are greater than 1 and thus
     * excitatory, and 0s otherwise. Used by [.getExcitatoryOutputs]
     */
    @Transient
    val excitatoryMask: Matrix

    /**
     * A binary matrix with 1s corresponding to entries of the weight matrix that are less than 1 and thus
     * inhibitory, and 0s otherwise. Used by [.getInhibitoryOutputs] }
     */
    @Transient
    val inhibitoryMask: Matrix

    @UserParameter(label = "Transpose Graphics", order = 10)
    var transposeGraphics = false
        set(value) {
            field = value
            events.updated.fire()
        }

    /**
     * Construct the matrix.
     *
     * @param source source layer
     * @param target target layer
     */
    init {
        source.addOutgoingConnector(this)
        target.addIncomingConnector(this)

        weights = Matrix(target.size, source.size)

        excitatoryMask = Matrix(target.size, source.size)
        inhibitoryMask = Matrix(target.size, source.size)

        diagonalize()
        updateMasks()

        psrMatrix = Matrix(target.size, source.size)

    }

    @get:Producible
    val weightArray: DoubleArray
        get() = weights.flatten()

    /**
     * Set the weights using a double array.
     */
    fun setWeights(newWeights: Array<DoubleArray>) {
        for (i in newWeights.indices) {
            for (j in newWeights[i].indices) {
                weights[i, j] = newWeights[i][j]
            }
        }
    }

    @Consumable
    fun setWeights(newWeights: DoubleArray) {
        val len = min(weights.size().toInt().toDouble(), newWeights.size.toDouble()).toInt()
        for (i in 0 until len) {
            weights[i / weights.ncol(), i % weights.ncol()] = newWeights[i]
        }
        updateMasks()
        events.updated.fire()
    }

    @Consumable
    fun setMatrixValues(otherWeightMatrix: Matrix?) {
        weights.copyFrom(otherWeightMatrix!!)
        updateMasks()
        events.updated.fire()
    }

    /**
     * Diagonalize the matrix.
     */
    fun diagonalize() {
        clear()
        val diag = Matrix.eye(target.size, source.size)
        weights.copyFrom(diag)
        updateMasks()
        events.updated.fire()
    }

    context(Network)
    override fun update() {
        if (clamped) {
            return
        }
        if (learningRule !is StaticSynapseRule) {
            learningRule.apply(this, learningRuleData)
            updateMasks()
            events.updated.fire()
        }
    }


    override fun toggleClamping() {
        clamped = !clamped
    }

    /**
     * Prepare the PSR Matrix for consumption by target layers.
     *
     * Most targets use the updated PSRMatrix by calling [getSummedPSRs].
     * Some targets like [ActivationSequence] bypass the PSRMatrix and compute a matrix product directly.
     */
    context(Network)
    override fun updatePSR() {
        if (spikeResponder is NonResponder) {

            // Connectionist case

            // For activation sequence case, just use last activation vector
            val sourceActivations = if (source is ActivationSequenceProcessor) {
                source.activations.row(- 1).toColumnVector()
            } else {
                source.activations
            }.let { activations ->
                // Some update rules apply a rule to source activations before they are multiplied by weights
                // If the target is a neuron collection we assume all neurons in the collection have the same update rule
                (target as? AbstractNeuronCollection)?.neuronList?.first()
                    ?.updateRule?.synapticInputModifier(activations)
                ?: activations
            }


            // One "half" of a matrix product. Source activations are element-wise multiplied by rows of matrix
            psrMatrix.copyFrom(weights.scaleColumns(sourceActivations))

        } else {
            // Spiking case
            spikeResponder.apply(this, spikeResponseData)
        }
    }

    private fun updateExcitatoryMask() {
        for (i in 0 until weights.nrow()) {
            for (j in 0 until weights.ncol()) {
                val newVal = if ((weights[i, j] > 0)) 1 else 0
                excitatoryMask[i, j] = newVal.toDouble()
            }
        }
    }

    private fun updateInhibitoryMask() {
        for (i in 0 until weights.nrow()) {
            for (j in 0 until weights.ncol()) {
                val newVal = if ((weights[i, j] < 0)) 1 else 0
                inhibitoryMask[i, j] = newVal.toDouble()
            }
        }
    }

    override fun randomize(randomizer: ProbabilityDistribution?) {
        for (i in 0 until weights.nrow()) {
            for (j in 0 until weights.ncol()) {
                weights[i, j] = (randomizer ?: weightRandomizer).sampleDouble()
            }
        }
        updateMasks()
        events.updated.fire()
    }

    override fun increment() {
        weights.add(increment)
        updateMasks()
        events.updated.fire()
    }

    override fun decrement() {
        weights.sub(increment)
        updateMasks()
        events.updated.fire()
    }

    /**
     * Set all entries to 0.
     */
    fun hardClear() {
        weights.copyFrom(Matrix(weights.nrow(), weights.ncol()))
        events.updated.fire()
    }

    override fun toString(): String {
        return (id
                + " (" + weights.nrow() + "x" + weights.ncol() + ") "
                + "connecting " + source.id + " to " + target.id)
    }

    fun updateMasks() {
        updateExcitatoryMask()
        updateInhibitoryMask()
    }

    fun copyFrom(other: WeightMatrix) {
        this.weights.copyFrom(other.weights)
        this.excitatoryMask.copyFrom(other.excitatoryMask)
        this.inhibitoryMask.copyFrom(other.inhibitoryMask)
        this.psrMatrix.copyFrom(other.psrMatrix)
        this.learningRuleData = other.learningRuleData.copy()
        this.spikeResponseData = other.spikeResponseData.copy()
    }
}
