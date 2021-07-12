package org.simbrain.network.core;

import org.jetbrains.annotations.NotNull;
import org.simbrain.network.LocatableModel;
import org.simbrain.network.events.LocationEvents;
import smile.math.matrix.Matrix;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Superclass for network models involved in, roughly speaking, array based computations.Simbrain layers are connected
 * to each other by {@link Connector}s. Subclassses include neuron arrays, collections of neurons, and deep networks,
 * backed by different data structures, including java arrays, Smile Matrices, and Tensor Flow tensors.
 * <br>
 * This class maintains connectors, id, and events, etc.
 * <br>
 * Input and output functions must be provided in Matrix format to support communication between different types
 * of layer. However, specific subclasses can communicate in other ways to support fast or custom communication. For
 * example, tensor-based layers and connectors could communicate using tensor flow operations, or specialized layers
 * with multiple arrays could be joined by special connectors making use of those arrays.
 *
 * @author Jeff Yoshimi
 * @author Yulin Li
 */
public abstract class Layer extends LocatableModel {

    /**
     * "Fan-in" of incoming connectors.
     */
    private final List<Connector> incomingConnectors = new ArrayList<>();

    /**
     * "Fan-out" of outgoing connectors.
     */
    private final List<Connector> outgoingConnectors = new ArrayList<>();

    /**
     * Return the current inputs as a size x 1 input vector.
     */
    public abstract Matrix getInputs();

    /**
     * Add inputs to input vector. Performed in first pass of {@link org.simbrain.network.update_actions.BufferedUpdate}
     * Asynchronous buffered update assumes that inputs are aggregated in one pass then updated in a second pass.
     * Thus setting inputs directly is a dangerous operation and so is not allowed.
     */
    public abstract void addInputs(Matrix inputs);

    /**
     * For "single-layer" layers this is activations. For multi-layer cases it is the output layer.
     */
    public abstract Matrix getOutputs();

    /**
     * Default x coordinate of center of layer.
     */
    private double x;

    /**
     * Default y coordinate of center of layer.
     */
    private double y;

    /**
     * Event support.
     */
    private transient LocationEvents events = new LocationEvents(this);

    /**
     * Returns the size of whatever is used as output for this layer, e.g. an activation vector or output layer
     * activation vector.
     */
    public abstract int size();

    /**
     * Register a callback function to run when the location of this object is updated.
     */
    public void onLocationChange(Runnable task) {
        events.onLocationChange(task);
    }

    public abstract Network getNetwork();

    public abstract Rectangle2D getBound();

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

    @Override
    public void delete() {
        getEvents().fireDeleted();
    }

    public Object readResolve() {
        events = new LocationEvents(this);
        return this;
    }

    @NotNull
    @Override
    public Point2D getLocation() {
        return new Point2D.Double(x, y);
    }

    @Override
    public void setLocation(Point2D location) {
        this.x = location.getX();
        this.y = location.getY();
        events.fireLocationChange();
    }

    public void setLocation(double x, double y) {
        setLocation(new Point2D.Double(x,y));
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}
