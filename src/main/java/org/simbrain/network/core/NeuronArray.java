package org.simbrain.network.core;


import org.deeplearning4j.nn.conf.distribution.UniformDistribution;
import org.deeplearning4j.nn.conf.layers.Layer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.simbrain.network.groups.Subnetwork;
import org.simbrain.util.UserParameter;
import org.simbrain.util.Utils;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Consumable;
import org.simbrain.workspace.Producible;

import java.awt.geom.Point2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * High performance immutable array backed by ND4J Array.
 */
public class NeuronArray implements EditableObject, AttributeContainer, ArrayConnectable {

    //TODO: Rename ideas: Array, Layer, ND4J Array, Double Array
    //TODO: See if data can be stored as an array. If not maybe used column instead of row.

    /**
     * Reference to network this array is part of.
     */
    private final Network parent;

    /**
     * A label for this Neuron Array for display purpose.
     */
    @UserParameter(
            label = "Label"
    )
    private String label = "";

    /**
     * Id of this array.
     */
    @UserParameter(label = "ID", description = "Id of this array", order = -1, editable = false)
    private String id;

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
     * If true, when the array is added to the network its id will not be used as its label.
     */
    private boolean useCustomLabel = false;

    private WeightMatrix incomingWeightMatrix;

    private WeightMatrix outgoingWeightMatrix;

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
    public NeuronArray(Network net, int numNodes) {
        parent = net;
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
        if (label == null  || label.isEmpty()) {
            useCustomLabel = false;
        } else {
            useCustomLabel = true;
        }
        String oldLabel = this.label;
        this.label = label;
        changeSupport.firePropertyChange("label", oldLabel , label);
    }

    /**
     * Simple randomization for now.
     */
    public void randomize() {
        neuronArray = Nd4j.rand(1, numNodes).subi(0.5).mul(2);
        changeSupport.firePropertyChange("updated", null , null);
    }

    /**
     * Initialize the id for this array. A default label based
     * on the id is also set.
     */
    public void initializeId() {
        id = parent.getArrayIdGenerator().getId();
        if (!useCustomLabel) {
            label = id.replaceAll("_", " ");
        }
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
        fireLocationChange();
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
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
    public WeightMatrix getOutgoingWeightMatrix() {
        return outgoingWeightMatrix;
    }

    public void setOutgoingWeightMatrix(WeightMatrix outgoingWeightMatrix) {
        this.outgoingWeightMatrix = outgoingWeightMatrix;
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
        ArrayConnectable.super.fireDeleted();
        changeSupport.firePropertyChange("delete", this, null);
    }

    public boolean isUseCustomLabel() {
        return useCustomLabel;
    }

    public void setUseCustomLabel(boolean useCustomLabel) {
        this.useCustomLabel = useCustomLabel;
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
        changeSupport.firePropertyChange("updated", null , null);
    }

    /**
     * Clear activations;
     */
    public void clear() {
        neuronArray.assign(0);
        changeSupport.firePropertyChange("updated", null , null);
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
                label = "Label",
                description = "If left blank, a default label will be created."
        )
        private String label = "";

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
            NeuronArray na = new NeuronArray(network, numNodes);
            na.setLabel(label);
            return na;
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

    @Override
    public void setLocation(Point2D location) {
        this.x = location.getX();
        this.y = location.getY();
    }

    @Override
    public Point2D getAttachmentPoint() {
        return new Point2D.Double(x + 150 / 2.0, y + 50 / 2.0);
    }

    public void fireLocationChange() {
        changeSupport.firePropertyChange("moved", null, null);
    }

    @Override
    public void onLocationChange(Runnable task) {
        addPropertyChangeListener(evt -> {
            if ("moved".equals(evt.getPropertyName())) {
                task.run();
            }
        });
    }

    @Override
    public Network getNetwork() {
        return parent;
    }

    //public Layer asLayer() {
    //    return new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
    //            .nOut(inputSize())
    //            .activation(Activation.SOFTMAX)
    //            .weightInit(new UniformDistribution(0, 1))
    //            .build();
    //}

    @Override
    public String toString() {
        return "Array [" + getId() + "] with " + inputSize() + " components\n";
    }
}
