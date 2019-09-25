package org.simbrain.network.core;

import org.nd4j.linalg.api.ndarray.INDArray;

import java.awt.geom.Point2D;

/**
 * Classes that implement this interface can be the source or target of an
 * ND4J weight matrix (or other layer-to-layer connector, if we add them).
 */
public interface ArrayConnectable {

    /**
     * Returns an ND4J array object representing activations.
     *
     * @return the activation array
     */
    INDArray getActivationArray();

    /**
     * Set the activations using an ND4J array.
     *
     * @param activations the activations to pass in.
     */
    void setActivationArray(INDArray activations);

    /**
     * Size of the activation array.
     *
     * @return
     */
    long arraySize();

    /**
     * Get the id associated with this source or target.
     */
    String getId();

    /**
     * Get an location of this object where a {@link WeightMatrix} will connect to.
     *
     * @return a location of this object
     */
    Point2D getLocation();

    /**
     * Register a callback function to run when the location of this object is updated.
     *
     * @param task a callback function
     */
    void onLocationChange(Runnable task);

}
