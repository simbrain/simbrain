package org.simbrain.network.matrix;

import org.simbrain.network.core.Connector;
import org.simbrain.network.core.Layer;
import org.simbrain.network.core.Network;
import smile.math.matrix.Matrix;

/**
 * Array based layers (based on Smile matrices) should extend this. Maintains an input vector for summing inputs.
 */
public abstract class ArrayLayer extends Layer {

    /**
     * Reference to network this array is part of.
     */
    private final Network parent;

    /**
     * Collects inputs from other network models using arrays.
     */
    private Matrix inputs;

    public ArrayLayer(Network net, int inputSize) {
        parent = net;
        inputs = new Matrix(inputSize, 1);
    }

    public Matrix getInputs() {
        return inputs;
    }

    @Override
    public int inputSize() {
        return (int) inputs.size();
    }

    @Override
    public void updateInputs() {
        Matrix wtdInputs = new Matrix(inputSize(), 1);
        for (Connector c : getIncomingConnectors()) {
            wtdInputs.add(c.getOutput());
        }
        addInputs(wtdInputs);
    }

    @Override
    public void addInputs(Matrix newInputs) {
        inputs.add(newInputs);
    }

    @Override
    public Network getNetwork() {
        return parent;
    }

}