package org.simbrain.network.core

import org.simbrain.network.core.Network.Randomizers.weightRandomizer
import org.simbrain.network.learningrules.StaticSynapseRule
import org.simbrain.network.learningrules.SynapseUpdateRule
import org.simbrain.network.spikeresponders.NonResponder
import org.simbrain.network.spikeresponders.SpikeResponder
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.MatrixDataHolder
import org.simbrain.util.UserParameter
import org.simbrain.util.broadcastMultiply
import org.simbrain.util.copyFrom
import org.simbrain.util.flatten
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
 */
class WeightMatrix(source: Layer, target: Layer) : Connector(source, target) {
    @UserParameter(label = "Increment amount", increment = .1, order = 20)
    var increment = .1

    @UserParameter(label = "Learning Rule", order = 100)
    var learningRule: SynapseUpdateRule<*, *> = StaticSynapseRule()

    /**
     * Only used if source connector's rule is spiking.
     */
    @UserParameter(label = "Spike Responder", showDetails = false, order = 200)
    var spikeResponder: SpikeResponder = NonResponder()
        set(value) {
            field = value
            spikeResponseData = value.createMatrixData(weightMatrix.nrow(), weightMatrix.ncol())
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
            val proposedDataHolder = widgetValue(::spikeResponder).createMatrixData(weightMatrix.nrow(), weightMatrix.ncol())
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
    val weightMatrix: Matrix

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

    /**
     * Construct the matrix.
     *
     * @param source source layer
     * @param target target layer
     */
    init {
        source.addOutgoingConnector(this)
        target.addIncomingConnector(this)

        weightMatrix = Matrix(target.size, source.size)

        excitatoryMask = Matrix(target.size, source.size)
        inhibitoryMask = Matrix(target.size, source.size)

        diagonalize()
        updateMasks()

        psrMatrix = Matrix(target.size, source.size)

    }

    @get:Producible
    val weights: DoubleArray
        get() = weightMatrix.flatten()

    /**
     * Set the weights using a double array.
     */
    fun setWeights(newWeights: Array<DoubleArray>) {
        for (i in newWeights.indices) {
            for (j in newWeights[i].indices) {
                weightMatrix[i, j] = newWeights[i][j]
            }
        }
    }

    @Consumable
    fun setWeights(newWeights: DoubleArray) {
        val len = min(weightMatrix.size().toInt().toDouble(), newWeights.size.toDouble()).toInt()
        for (i in 0 until len) {
            weightMatrix[i / weightMatrix.ncol(), i % weightMatrix.ncol()] = newWeights[i]
        }
        updateMasks()
        events.updated.fire()
    }

    @Consumable
    fun setMatrixValues(otherWeightMatrix: Matrix?) {
        weightMatrix.copyFrom(otherWeightMatrix!!)
        updateMasks()
        events.updated.fire()
    }

    /**
     * Diagonalize the matrix.
     */
    fun diagonalize() {
        clear()
        val diag = Matrix.eye(target.size, source.size)
        weightMatrix.copyFrom(diag)
        updateMasks()
        events.updated.fire()
    }

    context(Network)
    override fun update() {
        // TODO: Check for clamping and enabling
        if (learningRule !is StaticSynapseRule) {
            learningRule.apply(this, learningRuleData)
            updateMasks()
            events.updated.fire()
        }
    }

    /**
     * Update the psr matrix in the connectionist case.
     */
    context(Network)
    override fun updatePSR() {
        if (spikeResponder is NonResponder) {
            // For "connectionist" case. Unusual to need this, but could happen with excitatory inputs and no spike
            // responder, for example.
            // Populate each row of the psrMatrix with the element-wise product of the pre-synaptic output vector and
            // that row of the matrix
            psrMatrix.copyFrom(weightMatrix.broadcastMultiply(source.activations))
        } else {
            spikeResponder.apply(this, spikeResponseData)
        }
    }

    private fun updateExcitatoryMask() {
        for (i in 0 until weightMatrix.nrow()) {
            for (j in 0 until weightMatrix.ncol()) {
                val newVal = if ((weightMatrix[i, j] > 0)) 1 else 0
                excitatoryMask[i, j] = newVal.toDouble()
            }
        }
    }

    private fun updateInhibitoryMask() {
        for (i in 0 until weightMatrix.nrow()) {
            for (j in 0 until weightMatrix.ncol()) {
                val newVal = if ((weightMatrix[i, j] < 0)) 1 else 0
                inhibitoryMask[i, j] = newVal.toDouble()
            }
        }
    }

    override fun randomize(randomizer: ProbabilityDistribution?) {
        for (i in 0 until weightMatrix.nrow()) {
            for (j in 0 until weightMatrix.ncol()) {
                weightMatrix[i, j] = (randomizer ?: weightRandomizer).sampleDouble()
            }
        }
        updateMasks()
        events.updated.fire()
    }

    override fun increment() {
        weightMatrix.add(increment)
        updateMasks()
        events.updated.fire()
    }

    override fun decrement() {
        weightMatrix.sub(increment)
        updateMasks()
        events.updated.fire()
    }

    /**
     * Set all entries to 0.
     */
    fun hardClear() {
        weightMatrix.copyFrom(Matrix(weightMatrix.nrow(), weightMatrix.ncol()))
        events.updated.fire()
    }

    override fun toString(): String {
        return (id
                + " (" + weightMatrix.nrow() + "x" + weightMatrix.ncol() + ") "
                + "connecting " + source.id + " to " + target.id)
    }

    fun updateMasks() {
        updateExcitatoryMask()
        updateInhibitoryMask()
    }
}
