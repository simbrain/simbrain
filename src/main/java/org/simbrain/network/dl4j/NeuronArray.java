package org.simbrain.network.dl4j;


import org.jetbrains.annotations.NotNull;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.simbrain.network.LocatableModel;
import org.simbrain.network.core.Network;
import org.simbrain.network.events.NeuronArrayEvents;
import org.simbrain.util.UserParameter;
import org.simbrain.util.Utils;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Consumable;
import org.simbrain.workspace.Producible;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * High performance immutable array backed by ND4J Array.
 */
public class NeuronArray implements EditableObject, AttributeContainer, ArrayConnectable, LocatableModel {

    //TODO: Rename ideas: Array, Layer, ND4J Array, Double Array
    //TODO: See if data can be stored as an array. If not maybe used column instead of row.

    /**
     * Reference to network this array is part of.
     */
    private final Network parent;

    /**
     * A label for this Neuron Array for display purpose.
     */
    @UserParameter(label = "Label")
    private String label = "";

    /**
     * Id of this array.
     */
    @UserParameter(label = "ID", description = "Id of this array", order = -1, editable = false)
    private final String id;

    /**
     * Number of columns in the under laying ND4J Array.
     */
    @UserParameter(
            label = "Nodes",
            description = "Number of nodes",
            editable = false,
            order = 1
    )
    private int numNodes = 100;

    /**
     * ND4J Array backing this object
     */
    private INDArray neuronArray;

    /**
     * x-coordinate of this neuron in 2-space.
     */
    private double x;

    /**
     * y-coordinate of this neuron in 2-space.
     */
    private double y;

    /**
     * Reference to incoming weight matrix.
     * TODO: Consider making this a list, prior to implementing serialization. But note that
     * there is no support currently for many-to-one connections.
     */
    private WeightMatrix incomingWeightMatrix;

    /**
     * "Fan-out" of outgoing weight matrices.
     */
    private List<WeightMatrix> outgoingWeightMatrices = new ArrayList<>();

    /**
     * Render an image showing each activation when true.
     */
    @UserParameter(
            label = "Show activations",
            description = "Whether to show activations as a pixel image",
            order = 4
    )
    private boolean renderActivations = true;

    /**
     * Event support.
     */
    private transient NeuronArrayEvents events = new NeuronArrayEvents(this);

    /**
     * Construct a neuron array.
     *
     * @param net parent net
     * @param numNodes number of nodes
     */
    public NeuronArray(Network net, int numNodes) {
        parent = net;
        this.id = net.getIdManager().getId(NeuronArray.class);
        this.numNodes = numNodes;
        randomize();
    }

    /**
     * Make a deep copy of this array.
     *
     * @param newParent the new parent network
     * @param orig the array to copy
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
        float[] floatValues = Utils.castToFloat(values);
        neuronArray = Nd4j.create(floatValues).reshape(neuronArray.rows(), neuronArray.columns());
    }

    @Producible()
    public double[] getValues() {
        return Nd4j.toFlattened(neuronArray).toDoubleVector();
    }

    /**
     * Set the label. This prevents the group id being used as the label for
     * new groups.  If null or empty labels are sent in then the group label is used.
     */
    @Consumable(defaultVisibility = false)
    public void setLabel(String label) {
        String oldLabel = this.label;
        this.label = label;
        events.fireLabelChange(oldLabel , label);
    }

    /**
     * Simple randomization for now.
     */
    public void randomize() {
        neuronArray = Nd4j.rand(1, numNodes).subi(0.5).mul(2);
        events.fireUpdated();
    }

    public void update() {

        // TODO: This is just a place holder. Do something useful.
        // neuronArray = Nd4j.rand(10,10).subi(0.5).mul(2);

        events.fireUpdated();
    }

    public int getNumNodes() {
        return (int) neuronArray.length();
    }

    public INDArray getNeuronArray() {
        return neuronArray;
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

    public void setNeuronArray(INDArray data) {
        neuronArray = data;
        numNodes = data.columns();
    }

    @Override
    public void onCommit() {
        events.fireLabelChange(null, label);
    }

    public String getLabel() {
        return label;
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
     * Clear activations;
     */
    public void clear() {
        neuronArray.assign(0);
        events.fireUpdated();
    }

    /**
     * Since Neuron Array is immutable, this object will be used in the creation dialog.
     */
    public static class CreationTemplate implements EditableObject {

        /**
         * Number of columns in the under laying ND4J Array.
         */
        @UserParameter(
                label = "Nodes",
                description = "Number of nodes",
                order = 1
        )
        private int numNodes = 100;

        /**
         * A label for this Neuron Array for display purpose.
         */
        @UserParameter(
                label = "Label",
                description = "If left blank, a default label will be created.",
                initialValueMethod = "getLabel"
        )
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
         *
         * @return the created neuron array
         */
        public NeuronArray create(Network network) {
            NeuronArray na = new NeuronArray(network, numNodes);
            na.label = label;
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
    public INDArray getOutputArray() {
        return neuronArray;
    }

    @Override
    public long inputSize() {
        return neuronArray.length();
    }

    @Override
    public long outputSize() {
        return neuronArray.length();
    }

    @Override
    public void setInputArray(INDArray activations) {
        setNeuronArray(activations);
        //update();
    }

    @Override
    public String getId() {
        return id;
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
        return "Array [" + getId() + "] with " + inputSize() + " components\n";
    }

    public NeuronArrayEvents getEvents() {
        return events;
    }
}
