package org.simbrain.network.core

import org.simbrain.network.core.Network.Randomizers.weightRandomizer
import org.simbrain.network.learningrules.StaticSynapseRule
import org.simbrain.network.learningrules.SynapseUpdateRule
import org.simbrain.network.spikeresponders.NonResponder
import org.simbrain.network.spikeresponders.SpikeResponder
import org.simbrain.network.util.EmptyMatrixData
import org.simbrain.network.util.MatrixDataHolder
import org.simbrain.util.UserParameter
import org.simbrain.util.copyFrom
import org.simbrain.util.flatten
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
    private var spikeResponder: SpikeResponder = NonResponder()

    // TODO: Conditionally enable based on type of source array rule?
    /**
     * Holds data for prototype rule.
     */
    private val dataHolder: MatrixDataHolder = EmptyMatrixData

    /**
     * Holds data for spike responder.
     */
    var spikeResponseData: MatrixDataHolder = EmptyMatrixData

    /**
     * The weight matrix object. Overwriting this causes unexpected behavior in GUI elements so best practice is to
     * create a new matrix and copy its value to this one.
     */
    @get:Producible
    val weightMatrix: Matrix

    /**
     * A matrix with the same size as the weight matrix. Holds values from post synaptic responses.
     * Only used with spike responders.
     */
    val psrMatrix: Matrix

    /**
     * A binary matrix with 1s corresponding to entries of the weight matrix that are greater than 1 and thus
     * excitatory, and 0s otherwise. Used by [.getExcitatoryOutputs]
     */
    @Transient
    private var excitatoryMask: Matrix? = null

    /**
     * A binary matrix with 1s corresponding to entries of the weight matrix that are less than 1 and thus
     * inhibitory, and 0s otherwise. Used by [.getInhibitoryOutputs] }
     */
    @Transient
    private var inhibitoryMask: Matrix? = null

    /**
     * Construct the matrix.
     *
     * @param source source layer
     * @param target target layer
     */
    init {
        source.addOutgoingConnector(this)
        target.addIncomingConnector(this)

        weightMatrix = Matrix(target.inputSize(), source.outputSize())
        diagonalize()

        psrMatrix = Matrix(target.inputSize(), source.outputSize())

        updateMasks()
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
        val diag = Matrix.eye(target.inputSize(), source.outputSize())
        weightMatrix.copyFrom(diag)
        updateMasks()
        events.updated.fire()
    }

    context(Network)
    override fun update() {
        // TODO: Check for clamping and enabling

        if (learningRule !is StaticSynapseRule) {
            learningRule.apply(this, dataHolder)
            updateMasks()
            events.updated.fire()
        }
    }

    /**
     * Returns the product of this matrix its source activations, or psr if source array's rule is spiking.
     *
     * @see Synapse.updateOutput
     */
    context(Network)
    override val output: Matrix
        get() {
            // TODO: Do frozen, clamping, or enabling make sense here

            if (spikeResponder is NonResponder) {
                // For "connectionist" case. PSR Matrix not needed in this case
                return weightMatrix.mm(source.outputs)
            } else {
                // Updates the psrMatrix in the spiking case
                spikeResponder.apply(this, spikeResponseData)
                return Matrix.column(psrMatrix.rowSums())
            }
        }

    /**
     * Update the psr matrix in the connectionist case.
     */
    private fun updateConnectionistPSR() {
        if (spikeResponder is NonResponder) {
            // For "connectionist" case. Unusual to need this, but could happen with excitatory inputs and no spike
            // responder, for example.
            // Populate each row of the psrMatrix with the element-wise product of the pre-synaptic output vector and
            // that row of the matrix
            val output = source.outputs
            for (i in 0 until weightMatrix.nrow()) {
                for (j in 0 until weightMatrix.ncol()) {
                    val newVal = weightMatrix[i, j] * output[j, 0]
                    psrMatrix[i, j] = newVal
                }
            }
        }
    }

    private fun updateExcitatoryMask() {
        excitatoryMask = weightMatrix.clone()
        for (i in 0 until excitatoryMask!!.nrow()) {
            for (j in 0 until excitatoryMask!!.ncol()) {
                val newVal = if ((excitatoryMask!![i, j] > 0)) 1 else 0
                excitatoryMask!![i, j] = newVal.toDouble()
            }
        }
    }

    private fun updateInhibitoryMask() {
        inhibitoryMask = weightMatrix.clone()
        for (i in 0 until inhibitoryMask!!.nrow()) {
            for (j in 0 until inhibitoryMask!!.ncol()) {
                val newVal = if ((inhibitoryMask!![i, j] < 0)) 1 else 0
                inhibitoryMask!![i, j] = newVal.toDouble()
            }
        }
    }

    val excitatoryOutputs: DoubleArray
        /**
         * Returns an array representing the sum of the psr's for all excitatory (> 0) pre-synaptic weights
         */
        get() {
            updateConnectionistPSR()
            if (excitatoryMask == null) {
                updateExcitatoryMask()
            }
            return excitatoryMask!!.clone().mul(psrMatrix).rowSums()
        }

    val inhibitoryOutputs: DoubleArray
        /**
         * Returns an array representing the sum of the psr's for all inhibitory (< 0) pre-synaptic weights
         */
        get() {
            updateConnectionistPSR()
            if (inhibitoryMask == null) {
                updateInhibitoryMask()
            }
            return inhibitoryMask!!.clone().mul(psrMatrix).rowSums()
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

    fun setSpikeResponder(spikeResponder: SpikeResponder) {
        this.spikeResponder = spikeResponder
        spikeResponseData = spikeResponder.createMatrixData(weightMatrix.nrow(), weightMatrix.ncol())
    }

    fun updateMasks() {
        updateExcitatoryMask()
        updateInhibitoryMask()
    }
}
