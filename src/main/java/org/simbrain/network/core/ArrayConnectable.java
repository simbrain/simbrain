package org.simbrain.network.core;

import org.nd4j.linalg.api.ndarray.INDArray;

import java.awt.geom.Point2D;

/**
 * Classes that implement this interface can be the source or target of an
 * ND4J weight matrix (or other layer-to-layer connector, if we add them).
 */
public interface ArrayConnectable {

    /**
     * Set input activations.
     */
    void setInputArray(INDArray activations);

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
    WeightMatrix getOutgoingWeightMatrix();

    /**
     * Connection from this ArrayConncetable to another one
     */
    void setOutgoingWeightMatrix(WeightMatrix weightMatrix);

    /**
     * Get the id associated with this source or target.
     */
    String getId();

    /**
     * Set the upper-left location of this object.
     */
    void setLocation(Point2D location);

    /**
     * Get a graphical attachment point for this object, where the line representing a {@link WeightMatrix} will attach.
     */
    Point2D getAttachmentPoint();

    /**
     * Register a callback function to run when the location of this object is updated.
     */
    void onLocationChange(Runnable task);

    Network getNetwork();

    /**
     * Call this when deleting the object.
     */
    default void fireDeleted() {
        getNetwork().removeWeightMatrix(getIncomingWeightMatrix());
        getNetwork().removeWeightMatrix(getOutgoingWeightMatrix());
    };

}
