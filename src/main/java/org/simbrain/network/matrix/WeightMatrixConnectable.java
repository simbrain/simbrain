package org.simbrain.network.matrix;

import org.jetbrains.annotations.NotNull;
import org.simbrain.network.LocatableModel;
import org.simbrain.network.core.Network;
import org.simbrain.network.events.LocationEvents;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.workspace.Consumable;
import org.simbrain.workspace.Producible;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Classes that implement this interface can be the source or target of a
 * weight matrix (or other layer-to-layer connector, if we add them). Encompasses
 * both {@link NeuronArray} and {@link org.simbrain.network.groups.AbstractNeuronCollection}.
 */
public abstract class WeightMatrixConnectable extends LocatableModel {

    /**
     * "Fan-in" of incoming weight matrices.
     */
    private final List<WeightMatrix> incomingWeightMatrices = new ArrayList<>();;

    /**
     * "Fan-out" of outgoing weight matrices.
     */
    private final List<WeightMatrix> outgoingWeightMatrices = new ArrayList<>();

    /**
     * Event support.
     */
    private transient LocationEvents events = new LocationEvents(this);

    public void addIncomingWeightMatrix(WeightMatrix weightMatrix) {
        incomingWeightMatrices.add(weightMatrix);
    }

    public void removeIncomingWeightMatrix(WeightMatrix weightMatrix) {
        incomingWeightMatrices.remove(weightMatrix);
    }

    public void addOutgoingWeightMatrix(WeightMatrix weightMatrix) {
        outgoingWeightMatrices.add(weightMatrix);
    }

    public void removeOutgoingWeightMatrix(WeightMatrix weightMatrix) {
        outgoingWeightMatrices.remove(weightMatrix);
    }

    public List<WeightMatrix> getIncomingWeightMatrices() {
        return incomingWeightMatrices;
    }

    public List<WeightMatrix> getOutgoingWeightMatrices() {
        return outgoingWeightMatrices;
    }

    /**
     * Return the current weighted inputs. Should be used for internal updating only.
     */
    protected abstract double[] getInputs();

    // There should NOT be a setInputs function because it will mess up updating.

    @Consumable()
    public abstract void addInputs(double[] newInputs);

    @Producible()
    public abstract double[] getActivations();

    @Consumable()
    public abstract void setActivations(double[] activations);

    /**
     * Iterate through incoming arrayconnetables, multiply by intervening weight matrices, and add the results
     * to an array which is returned.
     */
    public double[] getWeightedInputs() {
        double[] result = new double[getInputs().length];
        for (WeightMatrix wm : incomingWeightMatrices) {
            result = SimbrainMath.addVector(result, wm.weightsTimesSource());
        }
        return result;
    }

    /**
     * Register a callback function to run when the location of this object is updated.
     */
    public abstract void onLocationChange(Runnable task);

    public abstract Network getNetwork();

    public abstract Rectangle2D getBound();

    @Override
    public void postUnmarshallingInit() {
        if (events == null) {
            events = new LocationEvents(this);
        }
    }

    @NotNull
    @Override
    public LocationEvents getEvents() {
        return events;
    }

    /**
     * Add increment to every entry in weight matrix
     */
    public void incrementArray(double amount) {
        double[] newActivations = Arrays
                .stream(getActivations())
                .map(a -> a + amount)
                .toArray();
        setActivations(newActivations);
        events.fireUpdated();
    }

    /**
     * Subtract increment from every entry in the array
     */
    public void decrementArray(double amount) {
        double[] newActivations = Arrays
                .stream(getActivations())
                .map(a -> a - amount)
                .toArray();
        setActivations(newActivations);
        events.fireUpdated();
    }

    /**
     * Clear array values.
     */
    public void clearArray() {
        double[] newActivations = new double[getActivations().length];
        setActivations(newActivations);
        events.fireUpdated();
    }

}
