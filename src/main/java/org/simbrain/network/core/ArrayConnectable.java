package org.simbrain.network.core;

import org.nd4j.linalg.api.ndarray.INDArray;

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

}
