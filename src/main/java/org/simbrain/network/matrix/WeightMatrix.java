package org.simbrain.network.matrix;

import org.simbrain.network.core.Connector;
import org.simbrain.network.core.Layer;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.SynapseUpdateRule;
import org.simbrain.network.synapse_update_rules.StaticSynapseRule;
import org.simbrain.network.util.EmptyMatrixData;
import org.simbrain.network.util.MatrixDataHolder;
import org.simbrain.util.UserParameter;
import org.simbrain.workspace.Consumable;
import org.simbrain.workspace.Producible;
import smile.math.matrix.Matrix;
import smile.stat.distribution.GaussianDistribution;

import java.util.Arrays;

/**
 * An dense weight matrix that connects a source and target {@link Layer} object. A default way of linking arbitrary
 * layers.
 *
 * The matrix has as many rows as the target layer and as many columns as the source layer.
 * The matrix is multiplied by the transpose of the source layer column to produce the output activations.
 *
 * Since in Simbrain the source layer is typically shown as a row already, it's easy to visualize
 * the rows of the weight matrix being dotted with the source layer.
 *
 */
public class WeightMatrix extends Connector {

    @UserParameter(label = "Increment amount", increment = .1, order = 20)
    private double increment = .1;

    @UserParameter(label = "Learning Rule", useSetter = true, isObjectType = true, order = 100)
    SynapseUpdateRule prototypeRule = new StaticSynapseRule();

    /**
     * Holds data for prototype rule.
     */
    private MatrixDataHolder dataHolder = new EmptyMatrixData();

    /**
     * Holds data for spike responder.
     */
    private MatrixDataHolder spikeResponseHolder = new EmptyMatrixData();

    /**
     * The weight matrix object.
     */
    private Matrix weightMatrix;

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
    }

    public Matrix getWeightMatrix() {
        return weightMatrix;
    }

    @Producible
    public double[] getWeights() {
        return Arrays.stream(weightMatrix.toArray())
                .flatMapToDouble(Arrays::stream)
                .toArray();
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
            weightMatrix.set(i / weightMatrix.ncols(), i % weightMatrix.ncols(), newWeights[i]);
        }
        getEvents().fireUpdated();
    }

    /**
     * Diagonalize the matrix.
     */
    public void diagonalize() {
        clear();
        weightMatrix = Matrix.eye(target.inputSize(), source.outputSize());
        getEvents().fireUpdated();
    }

    /**
     * Returns the product of the this matrix its source activations
     */
    @Override
    public Matrix getOutput() {
        return weightMatrix.mm(source.getOutputs());
    }

    @Override
    public void update() {
        // TODO: Check for clamping or "freezing"
        if (!(prototypeRule instanceof StaticSynapseRule)){
            prototypeRule.apply(this, dataHolder);
            getEvents().fireUpdated();
        }
    }

    public SynapseUpdateRule getPrototypeRule() {
        return prototypeRule;
    }

    public void setPrototypeRule(SynapseUpdateRule prototypeRule) {
        this.prototypeRule = prototypeRule;
    }

    @Override
    public void
    randomize() {
        weightMatrix = Matrix.rand(getTarget().inputSize(), getSource().outputSize(),
                new GaussianDistribution(0, 1));
        getEvents().fireUpdated();
    }

    @Override
    public void increment() {
        weightMatrix.add(increment);
        getEvents().fireUpdated();
    }

    @Override
    public void decrement() {
        weightMatrix.sub(increment);
        getEvents().fireUpdated();
    }

    /**
     * Set all entries to 0.
     */
    public void hardClear() {
        weightMatrix = new Matrix(weightMatrix.nrows(), weightMatrix.ncols());
        getEvents().fireUpdated();
    }

    @Override
    public String toString() {
        return getId()
                + " (" + weightMatrix.nrows() + "x" + weightMatrix.ncols() + ") "
                + "connecting " + source.getId() + " to " + target.getId();
    }
}
