package org.simbrain.network.matrix;


import org.jetbrains.annotations.NotNull;
import org.simbrain.network.LocatableModel;
import org.simbrain.network.core.Network;
import org.simbrain.network.events.LocationEvents;
import org.simbrain.network.events.NetworkModelEvents;
import org.simbrain.util.UserParameter;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Consumable;
import org.simbrain.workspace.Producible;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * High performance immutable array backed by ND4J Array.
 */
public class NeuronArray extends ArrayConnectable implements EditableObject, AttributeContainer {

    //TODO: Rename ideas: Array, Layer, ND4J Array, Double Array
    //TODO: See if data can be stored as an array. If not maybe used column instead of row.

    /**
     * Reference to network this array is part of.
     */
    private final Network parent;

    /**
     * Number of columns in the under laying ND4J Array.
     */
    @UserParameter(label = "Nodes", description = "Number of nodes", editable = false, order = 1)
    private int numNodes = 100;

    @UserParameter(label = "Increment amount", increment = .1, order = 20)
    private double increment = .1;

    /**
     * ND4J Array backing this object
     */
    private double[] neuronArray;

    /**
     * For buffered update.
     */
    private double[] arrayBuffer;

    /**
     * Center of the neuron array.
     */
    private double x;

    /**
     * Center of the neuron array.
     */
    private double y;

    /**
     * Reference to incoming weight matrix. TODO: Consider making this a list, prior to implementing serialization. But
     * note that there is no support currently for many-to-one connections.
     */
    private WeightMatrix incomingWeightMatrix;

    /**
     * "Fan-out" of outgoing weight matrices.
     */
    private List<WeightMatrix> outgoingWeightMatrices = new ArrayList<>();

    /**
     * Render an image showing each activation when true.
     */
    @UserParameter(label = "Show activations", description = "Whether to show activations as a pixel image", order = 4)
    private boolean renderActivations = true;

    /**
     * Event support.
     */
    private transient LocationEvents events = new LocationEvents(this);

    /**
     * Construct a neuron array.
     *
     * @param net      parent net
     * @param numNodes number of nodes
     */
    public NeuronArray(Network net, int numNodes) {
        parent = net;
        neuronArray = new double[numNodes];
        this.numNodes = numNodes;
        randomize();
    }

    /**
     * Make a deep copy of this array.
     *
     * @param newParent the new parent network
     * @param orig      the array to copy
     * @return the deep copy
     */
    public NeuronArray deepCopy(Network newParent, NeuronArray orig) {
        NeuronArray copy = new NeuronArray(newParent, orig.getNumNodes());
        copy.setLabel(copy.getId());
        copy.x = orig.x;
        copy.y = orig.y;
        copy.setValues(orig.getValues());
        return copy;
    }

    @Consumable()
    public void setValues(double[] values) {
        neuronArray = values;
    }

    @Producible()
    public double[] getValues() {
        return neuronArray;
    }

    /**
     * Simple randomization for now.
     */
    public void randomize() {
        neuronArray = SimbrainMath.randomVector(neuronArray.length, -1, 1);
        events.fireUpdated();
    }

    public int getNumNodes() {
        return neuronArray.length;
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
        fireLocationChange();
    }

    @Override
    public Rectangle2D getBound() {
        return new Rectangle2D.Double(x - 150 / 2, y - 50 / 2, 150, 50);
    }

    public boolean isRenderActivations() {
        return renderActivations;
    }

    public void setRenderActivations(boolean renderActivations) {
        this.renderActivations = renderActivations;
    }

    @Override
    public WeightMatrix getIncomingWeightMatrix() {
        return incomingWeightMatrix;
    }

    public void setIncomingWeightMatrix(WeightMatrix incomingWeightMatrix) {
        this.incomingWeightMatrix = incomingWeightMatrix;
    }

    @Override
    public List<WeightMatrix> getOutgoingWeightMatrices() {
        return outgoingWeightMatrices;
    }

    @Override
    public void addOutgoingWeightMatrix(WeightMatrix outgoingWeightMatrix) {
        this.outgoingWeightMatrices.add(outgoingWeightMatrix);
    }

    @Override
    public void removeOutgoingWeightMatrix(WeightMatrix weightMatrix) {
        this.outgoingWeightMatrices.remove(weightMatrix);
    }

    @Override
    public void onCommit() {
        events.fireLabelChange("", getLabel());
    }

    @Override
    public String getName() {
        return "Neuron Array";
    }

    /**
     * Offset this neuron array
     *
     * @param offsetX x offset for translation.
     * @param offsetY y offset for translation.
     */
    public void offset(final double offsetX, final double offsetY) {
        x += offsetX;
        y += offsetY;
        events.fireUpdated();
    }

    /**
     * Add increment to every entry in weight matrix
     */
    public void increment() {
        for (int i = 0; i < neuronArray.length; i++) {
            neuronArray[i] += increment;
        }
        events.fireUpdated();
    }

    /**
     * Subtract increment from every entry in the array
     */
    public void decrement() {
        for (int i = 0; i < neuronArray.length; i++) {
            neuronArray[i] -= increment;
        }
        events.fireUpdated();
    }

    /**
     * Clear activations;
     */
    public void clear() {
        neuronArray = new double[neuronArray.length];
        events.fireUpdated();
    }

    /**
     * Since Neuron Array is immutable, this object will be used in the creation dialog.
     */
    public static class CreationTemplate implements EditableObject {

        /**
         * Number of columns in the under laying ND4J Array.
         */
        @UserParameter(label = "Nodes", description = "Number of nodes", order = 1)
        private int numNodes = 100;

        /**
         * A label for this Neuron Array for display purpose.
         */
        @UserParameter(label = "Label", description = "If left blank, a default label will be created.", initialValueMethod = "getLabel")
        private String label;

        /**
         * Create the template with a proposed label
         */
        public CreationTemplate(String proposedLabel) {
            this.label = proposedLabel;
        }

        /**
         * Add a neuron array to network created from field values which should be setup by an Annotated Property
         * Editor.
         *
         * @param network the network this neuron array adds to
         * @return the created neuron array
         */
        public NeuronArray create(Network network) {
            NeuronArray na = new NeuronArray(network, numNodes);
            na.setLabel(label);
            return na;
        }

        /**
         * Getter called by reflection by {@link UserParameter#initialValueMethod}
         */
        public String getLabel() {
            return label;
        }

        @Override
        public String getName() {
            return "Neuron Array";
        }
    }

    @Override
    public double[] getOutputArray() {
        return neuronArray;
    }

    @Override
    public int inputSize() {
        return neuronArray.length;
    }

    @Override
    public int outputSize() {
        return neuronArray.length;
    }

    @Override
    public void setInputArray(double[] activations) {
        neuronArray = activations;
        numNodes = activations.length;
    }

    @Override
    public void updateBuffer() {
        arrayBuffer = Arrays.stream(neuronArray).toArray();
    }

    @Override
    public void updateStateFromBuffer() {
        if (arrayBuffer != null) {
            setInputArray(Arrays.stream(arrayBuffer).toArray());
        }
    }
    
    public void fireLocationChange() {
        events.fireLocationChange();
    }

    @Override
    public void onLocationChange(Runnable task) {
        events.onLocationChange(task);
    }

    @Override
    public Network getNetwork() {
        return parent;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(" with " + inputSize() + " components\n");
        // TODO: For larger numbers could present as a matrix
        int maxToDisplay = 10;
        if (neuronArray.length < maxToDisplay) {
            sb.append(Arrays.toString(neuronArray));
        }
        return sb.toString();
    }

    public LocationEvents getEvents() {
        return events;
    }

    @Override
    public void postUnmarshallingInit() {
        if (events == null) {
            events = new LocationEvents(this);
        }
    }

}
