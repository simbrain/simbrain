package org.simbrain.network.core;


import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

/**
 * High performance immutable array backed by ND4J Array.
 */
//TOOD: Name? More like layer?  Or tensor?
public class NeuronArray {

    /**
     * Reference to network this neuron is part of.
     */
    private final Network parent;

    /**
     * ND4J Array backing this object
     */
    private INDArray neuronArray = Nd4j.rand(1000,1000);

    /**
     * x-coordinate of this neuron in 2-space.
     */
    private double x;

    /**
     * y-coordinate of this neuron in 2-space.
     */
    private double y;

    /**
     * z-coordinate of this neuron in 3-space. Currently no GUI implementation,
     * but fully useable for scripting. Like polarity this will get a full
     * implementation in the next development cycle... probably by 4.0.
     */
    private double z;

    public NeuronArray(Network net) {
        parent = net;
    }

    public void update() {};

    public int getRows() {
        return neuronArray.rows();
    }

    public int getCols() {
        return neuronArray.columns();
    }

    public INDArray getNeuronArray() {
        return neuronArray;
    }


    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }


}
