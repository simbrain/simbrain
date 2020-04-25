package org.simbrain.network.dl4j;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.simbrain.network.LocatableModel;
import org.simbrain.network.core.Network;

import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * Classes that implement this interface can be the source or target of an
 * ND4J weight matrix (or other layer-to-layer connector, if we add them).
 */
public interface ArrayConnectable extends LocatableModel {

    /**
     * Set input activations.
     */
    void setInputArray(INDArray activations);

    /**
     * Set input buffer to support buffered update.
     */
    void setInputBuffer(INDArray activations);

    /**
     * Returns "output" activations.
     */
    INDArray getOutputArray();

    /**
     * (Possibly cached) input array size.
     */
    long inputSize();

    /**
     * (Possibly cached) output array size.
     */
    long outputSize();

    /**
     * Connection from another ArrayConncetable to this one.
     */
    WeightMatrix getIncomingWeightMatrix();

    /**
     * Connection from another ArrayConncetable to this one.
     */
    void setIncomingWeightMatrix(WeightMatrix weightMatrix);

    /**
     * Connection from this ArrayConncetable to another one
     */
    List<WeightMatrix> getOutgoingWeightMatrices();

    /**
     * Connection from this ArrayConncetable to another one
     */
    void addOutgoingWeightMatrix(WeightMatrix weightMatrix);

    void removeOutgoingWeightMatrix(WeightMatrix weightMatrix);

    /**
     * Get the id associated with this source or target.
     */
    String getId();

    /**
     * Register a callback function to run when the location of this object is updated.
     */
    void onLocationChange(Runnable task);

    Network getNetwork();

    Rectangle2D getBound();

}
