package org.simbrain.network.matrix;

import org.simbrain.network.core.*;
import org.simbrain.network.spikeresponders.NonResponder;
import org.simbrain.network.synapse_update_rules.StaticSynapseRule;
import org.simbrain.network.synapse_update_rules.spikeresponders.SpikeResponder;
import org.simbrain.network.util.EmptyMatrixData;
import org.simbrain.network.util.MatrixDataHolder;
import org.simbrain.util.UserParameter;
import org.simbrain.workspace.Consumable;
import org.simbrain.workspace.Producible;
import smile.math.matrix.Matrix;
import smile.stat.distribution.GaussianDistribution;

import static org.simbrain.util.SmileUtilsKt.flatten;

/**
 * A dense weight matrix that connects a source and target {@link Layer} object. A default way of linking arbitrary
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
public class WeightMatrix extends Connector {

    @UserParameter(label = "Increment amount", increment = .1, order = 20)
    private double increment = .1;

    @UserParameter(label = "Learning Rule", useSetter = true, isObjectType = true, order = 100)
    SynapseUpdateRule prototypeRule = new StaticSynapseRule();

    /**
     * Only used if source connector's rule is spiking.
     */
    @UserParameter(label = "Spike Responder", isObjectType = true,
            useSetter = true, showDetails = false, order = 200)
    private SpikeResponder spikeResponder = new NonResponder();
    // TODO: Conditionally enable based on type of source array rule?

    /**
     * Holds data for prototype rule.
     */
    private MatrixDataHolder dataHolder = EmptyMatrixData.INSTANCE;

    /**
     * Holds data for spike responder.
     */
    public MatrixDataHolder spikeResponseData = EmptyMatrixData.INSTANCE;

    /**
     * The weight matrix object.
     */
    private Matrix weightMatrix;

    /**
     * A matrix with the same size as the weight matrix. Holds values from post synaptic responses.
     * Only used with spike responders.
     */
    private Matrix psrMatrix;

    /**
     * A binary matrix with 1s corresponding to entries of the weight matrix that are greater than 1 and thus
     * excitatory, and 0s otherwise. Used by {@link #getExcitatoryOutputs()}
     */
    private transient Matrix excitatoryMask;

    /**
     * A binary matrix with 1s corresponding to entries of the weight matrix that are less than 1 and thus
     * inhibitory, and 0s otherwise. Used by {@link #getInhibitoryOutputs()} }
     */
    private transient Matrix inhibitoryMask;

    /**
     * Construct the matrix.
     *
     * @param net parent network
     * @param source source layer
     * @param target target layer
     */
    public WeightMatrix(Network net, Layer source, Layer target) {
        super(source, target, net);

        source.addOutgoingConnector(this);
        target.addIncomingConnector(this);

        weightMatrix = new Matrix(target.inputSize(), source.outputSize());
        diagonalize();

        psrMatrix = new Matrix(target.inputSize(), source.outputSize());

        getEvents().getUpdated().on(null, true, () -> {
           updateExcitatoryMask();
           updateInhibitoryMask();
        });
        updateExcitatoryMask();
        updateInhibitoryMask();
    }

    @Producible
    public Matrix getWeightMatrix() {
        return weightMatrix;
    }

    @Producible
    public double[] getWeights() {
        return flatten(weightMatrix);
    }

    /**
     * Set the weights using a double array.
     */
    public void setWeights(double[][] newWeights) {
        for (int i = 0; i <  newWeights.length; i++) {
                for (int j = 0; j < newWeights[i].length; j++) {
                weightMatrix.set(i,j,newWeights[i][j]);
            }
        }
    }

    @Consumable
    public void setWeights(double[] newWeights) {
        int len = Math.min((int) weightMatrix.size(), newWeights.length);
        for (int i = 0; i < len; i++) {
            weightMatrix.set(i / weightMatrix.ncol(), i % weightMatrix.ncol(), newWeights[i]);
        }
        getEvents().getUpdated().fireAndForget();
    }

    @Consumable
    public void setWeightMatrix(Matrix weightMatrix) {
        this.weightMatrix = weightMatrix.clone();
        getEvents().getUpdated().fireAndForget();
    }

    /**
     * Diagonalize the matrix.
     */
    public void diagonalize() {
        clear();
        weightMatrix = Matrix.eye(target.inputSize(), source.outputSize());
        getEvents().getUpdated().fireAndForget();
    }

    @Override
    public void update() {

        // TODO: Check for clamping and enabling

        if (!(prototypeRule instanceof StaticSynapseRule)){
            prototypeRule.apply(this, dataHolder);
            getEvents().getUpdated().fireAndForget();
        }
    }

    /**
     * Returns the product of this matrix its source activations, or psr if source array's rule is spiking.
     *
     * @see Synapse#updateOutput()
     */
    @Override
    public Matrix getOutput() {

        // TODO: Do frozen, clamping, or enabling make sense here

        if (spikeResponder instanceof NonResponder) {
            // For "connectionist" case. PSR Matrix not needed in this case
            return weightMatrix.mm(source.getOutputs());
        } else {
            // Updates the psrMatrix in the spiking case
            spikeResponder.apply(this, spikeResponseData);
            return Matrix.column(psrMatrix.rowSums());
        }
    }

    /**
     * Update the psr matrix in the connectionist case.
     */
    public void updateConnectionistPSR() {
        if (spikeResponder instanceof NonResponder) {
            // For "connectionist" case. Unusual to need this, but could happen with excitatory inputs and no spike
            // responder, for example.
            // Populate each row of the psrMatrix with the element-wise product of the pre-synaptic output vector and
            // that row of the matrix
            var output = source.getOutputs();
            for (int i = 0; i <  weightMatrix.nrow(); i++) {
                for (int j = 0; j < weightMatrix.ncol(); j++) {
                    var newVal = weightMatrix.get(i,j) * output.get(j, 0);
                    psrMatrix.set(i,j, newVal);
                }
            }
        }
    }

    private void updateExcitatoryMask() {
        excitatoryMask = weightMatrix.clone();
        for (int i = 0; i <  excitatoryMask.nrow(); i++) {
            for (int j = 0; j < excitatoryMask.ncol(); j++) {
                var newVal = (excitatoryMask.get(i,j) > 0) ? 1 : 0;
                excitatoryMask.set(i,j, newVal);
            }
        }
    }

    private void updateInhibitoryMask() {
        inhibitoryMask = weightMatrix.clone();
        for (int i = 0; i <  inhibitoryMask.nrow(); i++) {
            for (int j = 0; j < inhibitoryMask.ncol(); j++) {
                var newVal = (inhibitoryMask.get(i,j) < 0) ? 1 : 0;
                inhibitoryMask.set(i,j, newVal);
            }
        }
    }

    /**
     * Returns an array representing the sum of the psr's for all excitatory (> 0) pre-synaptic weights
     */
    public double[] getExcitatoryOutputs() {
        updateConnectionistPSR();
        if (excitatoryMask == null) {
            updateExcitatoryMask();
        }
        return excitatoryMask.clone().mul(psrMatrix).rowSums();
    }

    /**
     * Returns an array representing the sum of the psr's for all inhibitory (< 0) pre-synaptic weights
     */
    public double[] getInhibitoryOutputs() {
        updateConnectionistPSR();
        if (inhibitoryMask == null) {
            updateInhibitoryMask();
        }
        return inhibitoryMask.clone().mul(psrMatrix).rowSums();
    }


    public SynapseUpdateRule getPrototypeRule() {
        return prototypeRule;
    }

    public void setPrototypeRule(SynapseUpdateRule prototypeRule) {
        this.prototypeRule = prototypeRule;
    }

    @Override
    public void randomize() {
        weightMatrix = Matrix.rand(getTarget().inputSize(), getSource().outputSize(),
                new GaussianDistribution(0, 1));
        getEvents().getUpdated().fireAndForget();
    }

    @Override
    public void increment() {
        weightMatrix.add(increment);
        getEvents().getUpdated().fireAndForget();
    }

    @Override
    public void decrement() {
        weightMatrix.sub(increment);
        getEvents().getUpdated().fireAndForget();
    }

    /**
     * Set all entries to 0.
     */
    public void hardClear() {
        weightMatrix = new Matrix(weightMatrix.nrow(), weightMatrix.ncol());
        getEvents().getUpdated().fireAndForget();
    }

    @Override
    public String toString() {
        return getId()
                + " (" + weightMatrix.nrow() + "x" + weightMatrix.ncol() + ") "
                + "connecting " + source.getId() + " to " + target.getId();
    }

    public Matrix getPsrMatrix() {
        return psrMatrix;
    }

    public void setSpikeResponder(SpikeResponder spikeResponder) {
        this.spikeResponder = spikeResponder;
        spikeResponseData = spikeResponder.createMatrixData(weightMatrix.nrow(), weightMatrix.ncol());
    }

}
