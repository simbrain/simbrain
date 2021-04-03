package org.simbrain.network.matrix;

import org.simbrain.network.LocatableModel;
import org.simbrain.network.core.Network;
import org.simbrain.network.events.LocationEvents;

import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * Classes that implement this interface can be the source or target of an
 * ND4J weight matrix (or other layer-to-layer connector, if we add them).
 */
public abstract class ArrayConnectable extends LocatableModel {

    /**
     * Set input activations.
     */
    public abstract void setInputArray(double[] activations);

    /**
     * Returns "output" activations.
     */
    public abstract double[] getOutputArray();

    /**
     * (Possibly cached) input array size.
     */
    public abstract int inputSize();

    /**
     * (Possibly cached) output array size.
     */
    public abstract int outputSize();

    /**
     * Connection from another ArrayConncetable to this one.
     */
    public abstract WeightMatrix getIncomingWeightMatrix();

    /**
     * Connection from another ArrayConncetable to this one.
     */
    public abstract void setIncomingWeightMatrix(WeightMatrix weightMatrix);

    /**
     * Connection from this ArrayConncetable to another one
     */
    public abstract List<WeightMatrix> getOutgoingWeightMatrices();

    /**
     * Connection from this ArrayConncetable to another one
     */
    public abstract void addOutgoingWeightMatrix(WeightMatrix weightMatrix);

    public abstract void removeOutgoingWeightMatrix(WeightMatrix weightMatrix);

    /**
     * Register a callback function to run when the location of this object is updated.
     */
    public abstract void onLocationChange(Runnable task);

    public abstract Network getNetwork();

    public abstract Rectangle2D getBound();

    public abstract void postUnmarshallingInit();
}
