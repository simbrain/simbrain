package org.simbrain.network.matrix;


import org.jetbrains.annotations.NotNull;
import org.simbrain.network.core.Network;
import org.simbrain.util.UserParameter;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.workspace.AttributeContainer;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

/**
 * A "neuron array" backed by a double array.
 */
public class NeuronArray extends WeightMatrixConnectable implements EditableObject, AttributeContainer {

    //TODO: Rename ideas: Array, Layer,Double Array

    /**
     * Reference to network this array is part of.
     */
    private final Network parent;

    /**
     * Center of the neuron array.
     */
    private double x;

    /**
     * Center of the neuron array.
     */
    private double y;

    /**
     * Array to hold activation values. These are also the outputs that are consumed by
     * other network components via {@link WeightMatrixConnectable}.
     */
    private double[] activations;

    /**
     * Collects inputs from other network models using arrays.
     */
    private double[] inputs;

    /**
     * Render an image showing each activation when true.
     */
    @UserParameter(label = "Show activations", description = "Whether to show activations as a pixel image", order = 4)
    private boolean renderActivations = true;

    /**
     * Construct a neuron array.
     *
     * @param net  parent net
     * @param size number of components in the array
     */
    public NeuronArray(Network net, int size) {
        parent = net;
        activations = new double[size];
        inputs = new double[size];
        randomize();
        setLabel(net.getIdManager().getProposedId(this.getClass()));
    }

    /**
     * Make a deep copy of this array.
     *
     * @param newParent the new parent network
     * @param orig      the array to copy
     * @return the deep copy
     */
    public NeuronArray deepCopy(Network newParent, NeuronArray orig) {
        NeuronArray copy = new NeuronArray(newParent, orig.getActivations().length);
        copy.x = orig.x;
        copy.y = orig.y;
        copy.setActivations(orig.getActivations());
        return copy;
    }

    /**
     * Simple randomization for now.
     */
    public void randomize() {
        setActivations(SimbrainMath.randomVector(getActivations().length, -1, 1));
        getEvents().fireUpdated();
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
    public void onCommit() {
        getEvents().fireLabelChange("", getLabel());
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
        getEvents().fireUpdated();
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
    public void updateInputs() {
        addInputs(getWeightedInputs());
    }

    @Override
    public void update() {
        setActivations(getInputs());
        inputs = new double[inputs.length]; // clear inputs
        getEvents().fireUpdated();
    }

    @Override
    protected double[] getInputs() {
        return inputs;
    }

    @Override
    public void addInputs(double[] newInputs) {
        inputs = SimbrainMath.addVector(inputs, newInputs);
    }

    @Override
    public double[] getActivations() {
        return activations;
    }

    /**
     * Set the activations to a one-hot encoding (all 0s and one 1) at provided index.
     * @see {<a href="https://en.wikipedia.org/wiki/One-hot"></a>}.
     */
    public void setOneHot(int index) {
        clear();
        activations[index] = 1.0;
        getEvents().fireUpdated();
    }

    @Override
    public void setActivations(double[] newActivations) {
        activations = Arrays.stream(newActivations).toArray();
        getEvents().fireUpdated();
    }

    public void fireLocationChange() {
        getEvents().fireLocationChange();
    }

    @Override
    public void onLocationChange(Runnable task) {
        getEvents().onLocationChange(task);
    }

    @Override
    public Network getNetwork() {
        return parent;
    }

}
