package org.simbrain.network.matrix;

import org.jetbrains.annotations.NotNull;
import org.simbrain.network.LocatableModel;
import org.simbrain.network.core.Network;
import org.simbrain.network.events.LocationEvents;
import org.simbrain.util.UserParameter;
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
public abstract class ArrayConnectable extends LocatableModel {

    /**
     * Array to hold activation values.
     */
    private double[] activations;

    /**
     * For buffered update.
     */
    private double[] buffer;

    /**
     * Collects inputs from other network models using arrays.
     */
    private double[] inputs;

    /**
     * "Fan-in" of incoming weight matrices.
     */
    private final List<WeightMatrix> incomingWeightMatrices = new ArrayList<>();;

    /**
     * "Fan-out" of outgoing weight matrices.
     */
    private final List<WeightMatrix> outgoingWeightMatrices = new ArrayList<>();

    @UserParameter(label = "Increment amount", increment = .1, order = 20)
    private double increment = .1;

    /**
     * Event support.
     */
    private transient LocationEvents events = new LocationEvents(this);

    public ArrayConnectable(int size) {
        activations = new double[size];
        buffer = new double[size];
        inputs = new double[size];
    }

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

    @Producible()
    public double[] getActivations() {
        return activations;
    }

    @Consumable()
    public void setActivations(double[] activations) {
        this.activations = activations;
    }

    public double[] getWeightedInputs() {
        double[] result = new double[inputs.length];
        for (WeightMatrix wm : incomingWeightMatrices) {
            result = SimbrainMath.addVector(result, wm.getWeightMatrix().mv(wm.getSource().getActivations()));
        }
        return result;
    }

    public void copyBufferToActivation() {
        copyToActivations(buffer);
    }

    public void copyToActivations(double [] newActivations) {
        // TODO: Is this the most performant way to copy an array?
        activations = Arrays.stream(newActivations).toArray();
    }

    public double[] getInputs() {
        return inputs;
    }

    public void copyToInputs(double [] newInputs) {
        inputs = Arrays.stream(newInputs).toArray();
    }

    public void copyToBuffer(double [] newBuffer) {
        buffer = Arrays.stream(newBuffer).toArray();
    }

    @Consumable()
    public void setInputs(double[] inputs) {
        this.inputs = inputs;
    }

    /**
     * Register a callback function to run when the location of this object is updated.
     */
    public abstract void onLocationChange(Runnable task);

    public abstract Network getNetwork();

    public abstract Rectangle2D getBound();

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
    public void increment() {
        for (int i = 0; i < activations.length; i++) {
            activations[i] += increment;
        }
        events.fireUpdated();
    }

    /**
     * Subtract increment from every entry in the array
     */
    public void decrement() {
        for (int i = 0; i < activations.length; i++) {
            activations[i] -= increment;
        }
        events.fireUpdated();
    }

    /**
     * Clear activations;
     */
    public void clear() {
        activations = new double[activations.length];
        events.fireUpdated();
    }

}
