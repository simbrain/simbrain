package org.simbrain.network.core;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.simbrain.network.groups.NeuronCollection;
import org.simbrain.util.Utils;

/**
 * An ND4J weight matrix that connects a source and target {@link ArrayConnectable}
 * object.
 */
public class WeightMatrix {

    //TODO Id, Label

    /**
     * The source "layer" / activation vector for this weight matrix.
     */
    private ArrayConnectable source;

    /**
     * The target "layer" for this weight matrix.
     */
    private ArrayConnectable target;

    /**
     * The weight matrix object.
     */
    private INDArray weightMatrix;

    /**
     * WeightMatrixNode will render an image of this matrix if set to true
     */
    private boolean enableRendering = true;

    /**
     * Construct the matrix.
     *
     * @param source source layer
     * @param target target layer
     */
    public WeightMatrix(ArrayConnectable source, ArrayConnectable target) {
        this.source = source;
        this.target = target;

        // Default for "adapter" cases is 1-1
        if (source instanceof NeuronCollection || target instanceof NeuronCollection) {
            weightMatrix = Nd4j.create(source.arraySize(), target.arraySize());
            INDArray id = Nd4j.eye(Math.min(source.arraySize(), target.arraySize()));
            weightMatrix.get(NDArrayIndex.createCoveringShape(id.shape())).assign(id);
        } else {
            // For now randomize new matrices between arrays
            weightMatrix = Nd4j.rand((int) source.arraySize(), (int) target.arraySize()).subi(0.5).mul(2);
        }

    }

    /**
     * Default update simply matrix multiplies source times matrix and sets
     * result to target.
     */
    public void update() {
        target.setActivationArray(source.getActivationArray().mmul(weightMatrix));
    }

    @Override
    public String toString() {
        String ret = new String();
        ret += "Weight Matrix [TODO]:";
        ret += "  Connects " + source.getId() + " to " + target.getId();
        return ret;
    }

    public ArrayConnectable getSource() {
        return source;
    }

    public ArrayConnectable getTarget() {
        return target;
    }

    public INDArray getWeightMatrix() {
        return weightMatrix;
    }

    public boolean isEnableRendering() {
        return enableRendering;
    }

    public void setEnableRendering(boolean enableRendering) {
        this.enableRendering = enableRendering;
    }
}
