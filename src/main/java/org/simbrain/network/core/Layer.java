package org.simbrain.network.core;

import org.jetbrains.annotations.NotNull;
import org.simbrain.network.LocatableModel;
import org.simbrain.network.events.LocationEvents2;
import org.simbrain.workspace.AttributeContainer;
import smile.math.matrix.Matrix;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Superclass for network models involved in, roughly speaking, array based computations. Simbrain layers are connected
 * to each other by {@link Connector}s. Subclassses include neuron arrays, collections of neurons, and deep networks,
 * backed by different data structures, including java arrays, Smile Matrices, and Tensor Flow tensors.
 * <br>
 * This class maintains connectors, id, and events, etc.
 * <br>
 * Input and output functions must be provided in Smile matrix format to support communication between different types
 * of layer. Smile matrices are the "lingua franca" of layers.
 * <br>
 * However, specific subclasses can communicate in other ways to support fast or custom communication. For example,
 * tensor-based layers and connectors could communicate using tensor flow operations, or specialized layers with
 * multiple arrays could be joined by special connectors making use of those arrays.
 *
 * @author Jeff Yoshimi
 * @author Yulin Li
 */
public abstract class Layer extends LocatableModel implements AttributeContainer {

    // TODO: Currently Smile Matrices are the "lingua Franca" for different layers.
    //  Keep an eye on Kotlin's Multik as a possible alternative

    /**
     * "Fan-in" of incoming connectors.
     */
    private final List<Connector> incomingConnectors = new ArrayList<>();

    /**
     * "Fan-out" of outgoing connectors.
     */
    private final List<Connector> outgoingConnectors = new ArrayList<>();

    /**
     * Add inputs to input vector. Performed in first pass of {@link org.simbrain.network.update_actions.BufferedUpdate}
     * Asynchronous buffered update assumes that inputs are aggregated in one pass then updated in a second pass.
     * Thus setting inputs directly is a dangerous operation and so is not allowed.
     */
    public abstract void addInputs(Matrix inputs);

    /**
     * A column vector of output values. For "single-layer" layers this is activations. For multi-layer cases it is the
     * output layer.
     */
    public abstract Matrix getOutputs();

    /**
     * x coordinate of center of layer.
     */
    private double x;

    /**
     * y coordinate of center of layer.
     */
    private double y;

    /**
     * Width of layer. Mainly used by graphica arrows drawn to represent {@link Connector}s.
     */
    private double width;

    /**
     * Height of layer
     */
    private double height;

    /**
     * Event support.
     */
    private transient LocationEvents2 events = new LocationEvents2();

    /**
     * Returns the output size for this layer.
     */
    public abstract int outputSize();

    /**
     * Returns the input size for this layer.
     */
    public abstract int inputSize();

    /**
     * Register a callback function to run when the location of this object is updated.
     */
    public void onLocationChange(Runnable task) {
        getEvents().getLocationChanged().on(task);
    }

    public abstract Network getNetwork();

    /**
     * Needed so arrow can be set correctly
     */
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
    public void postOpenInit() {
        if (events == null) {
            events = new LocationEvents2();
        }
    }

    @NotNull
    @Override
    public LocationEvents2 getEvents() {
        return events;
    }

    @Override
    public void delete() {
        getEvents().getDeleted().fireAndForget(this);
    }

    /**
     * See {@link org.simbrain.workspace.serialization.WorkspaceComponentDeserializer}
     */
    public Object readResolve() {
        events = new LocationEvents2();
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
        getEvents().getLocationChanged().fireAndForget();
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
        getEvents().getLocationChanged().fireAndForget();
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
        getEvents().getLocationChanged().fireAndForget();
    }

}
