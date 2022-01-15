package org.simbrain.network.matrix;

import org.simbrain.network.core.Connector;
import org.simbrain.network.core.Layer;
import org.simbrain.network.core.Network;
import org.simbrain.util.UserParameter;
import org.simbrain.workspace.Consumable;
import smile.math.matrix.Matrix;

/**
 * Array based layers (based on Smile matrices) should extend this. Maintains an input vector for summing inputs.
 */
public abstract class ArrayLayer extends Layer {

    @UserParameter(label = "Clamped", description = "Clamping", order = 3)
    private boolean clamped;

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

    @Consumable
    public void addInputs(double[] inputs) {
        addInputs(new Matrix(inputs));
    }

    @Override
    public Network getNetwork() {
        return parent;
    }


    @Override
    public void toggleClamping() {
        setClamped(!isClamped());
    }

    public void setClamped(final boolean clamped) {
        this.clamped = clamped;
        getEvents().fireClampChanged();
    }

    public boolean isClamped() {
        return clamped;
    }

}