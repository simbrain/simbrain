package org.simbrain.network.core;


import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.buffer.DoubleBuffer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.simbrain.util.UserParameter;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Consumable;
import org.simbrain.workspace.Producible;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * High performance immutable array backed by ND4J Array.
 */
public class NeuronArray implements EditableObject, AttributeContainer {

    //TODO: Rename ideas: Array, Layer, ND4J Array, Double Array
    //TODO: See if data can be stored as an array. If not maybe used column instead of row.

    /**
     * Reference to network this neuron is part of.
     */
    private final Network parent;

    /**
     * A label for this Neuron Array for display purpose.
     */
    @UserParameter(
            label = "Label"
    )
    private String label = "Neuron Array";

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
     * z-coordinate of this neuron in 3-space. Currently no GUI implementation,
     * but fully useable for scripting. Like polarity this will get a full
     * implementation in the next development cycle... probably by 4.0.
     */
    private double z;

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
     * Support for property change events.
     */
    private transient PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    /**
     * Create a neuron array.
     *
     * @param net parent net
     * @param numNodes number of nodes
     */
    private NeuronArray(Network net, int numNodes) {
        parent = net;
        this.numNodes = numNodes;
        neuronArray = Nd4j.rand(1, numNodes).subi(0.5).mul(2);
    }

    @Consumable()
    public void setValues(double[] values) {
        neuronArray = Nd4j.create(values).reshape(neuronArray.rows(), neuronArray.columns());
    }

    @Producible()
    public double[] getValues() {
        return Nd4j.toFlattened(neuronArray).toDoubleVector();
    }

    public void update() {

        // TODO: This is just a place holder. Do something useful.
        // neuronArray = Nd4j.rand(10,10).subi(0.5).mul(2);

        changeSupport.firePropertyChange("updated", null, null);
    }

    public int getNumNodes() {
        return (int) neuronArray.length();
    }

    public INDArray getNeuronArray() {
        return neuronArray;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public boolean isRenderActivations() {
        return renderActivations;
    }

    public void setRenderActivations(boolean renderActivations) {
        this.renderActivations = renderActivations;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    public void setNeuronArray(INDArray data) {
        neuronArray = data;
        numNodes = data.columns();
    }

    @Override
    public void onCommit() {
        changeSupport.firePropertyChange("labelChanged", null, label);
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String getName() {
        return "Neuron Array";
    }

    /**
     * Notify listeners that this object has been deleted.
     */
    public void fireDeleted() {
        changeSupport.firePropertyChange("delete", this, null);
    }

    /**
     * Since Neuron Array is immutable, this object will be used in the creation dialog.
     */
    public static class CreationTemplate implements EditableObject {

        // See NeuronGroup.NeuronGroupCreator. Possibly reuse.

        /**
         * A label for this Neuron Array for display purpose.
         */
        @UserParameter(
                label = "Label"
        )
        private String label = "Neuron Array";

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
         * Add a neuron array to network created from field values which should be setup by an Annotated Property
         * Editor.
         *
         * @param network the network this neuron array adds to
         *
         * @return the created neuron array
         */
        public NeuronArray create(Network network) {
            NeuronArray neuronArray = new NeuronArray(network, numNodes);
            neuronArray.label = label;
            return neuronArray;
        }

        @Override
        public String getName() {
            return "Neuron Array";
        }
    }
}
