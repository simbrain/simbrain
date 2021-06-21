package org.simbrain.network.connectors;

import org.jetbrains.annotations.NotNull;
import org.simbrain.network.LocatableModel;
import org.simbrain.network.core.Network;
import org.simbrain.network.events.LocationEvents;
import org.simbrain.workspace.Consumable;
import org.simbrain.workspace.Producible;
import smile.math.matrix.Matrix;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Classes that implement this interface can be the source or target of a
 * {@link Connector}, e.g. a weight matrix. Associated with an activation vector and a separate
 * input vector that stores summed output from incoming connectors.
 */
public abstract class Connectable extends LocatableModel {

    /**
     * "Fan-in" of incoming connectors.
     */
    private final List<Connector> incomingConnectors = new ArrayList<>();;

    /**
     * "Fan-out" of outgoing connectors.
     */
    private final List<Connector> outgoingConnectors = new ArrayList<>();

    /**
     * Event support.
     */
    private transient LocationEvents events = new LocationEvents(this);

    public void addIncomingConnector(Connector connector) {
        incomingConnectors.add(connector);
    }

    public void removeIncomingConnector(Connector connector) {
        incomingConnectors.remove(connector);
    }

    public void addOutgoingConnector(Connector connector) {
        outgoingConnectors.add(connector);
    }

    public void removeOutgoingConnector(Connector connector) {
        outgoingConnectors.remove(connector);
    }

    public List<Connector> getIncomingConnectors() {
        return incomingConnectors;
    }

    public List<Connector> getOutgoingConnectors() {
        return outgoingConnectors;
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

    // TODO: Temp until we convert to matrices
    public Matrix getActivationsAsMatrix() {
        return new Matrix(getActivations());
    }

    @Consumable()
    public abstract void setActivations(double[] activations);

    /**
     * Iterate through incoming connectors and return their summed outputs.
     */
    public Matrix getSummedOutputs() {
        Matrix  result = new Matrix( getInputs().length,1);
        for (Connector c : incomingConnectors) {
                result.add(c.getOutput());
            }
        return result;
    }

    /**
     * Size of activation vector.
     */
    public int size() {
        return getActivations().length;
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

    @Override
    public void delete() {
        getEvents().fireDeleted();
    }

    public Object readResolve() {
        events = new LocationEvents(this);
        return this;
    }

}
