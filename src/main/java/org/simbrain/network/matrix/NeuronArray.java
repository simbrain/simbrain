package org.simbrain.network.matrix;


import org.simbrain.network.core.ArrayLayer;
import org.simbrain.network.core.Layer;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.network.util.MatrixDataHolder;
import org.simbrain.util.UserParameter;
import org.simbrain.util.Utils;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Producible;
import smile.math.matrix.Matrix;
import smile.stat.distribution.GaussianDistribution;

import java.awt.geom.Rectangle2D;

/**
 * A "neuron array" backed by a Smile Matrix.
 */
public class NeuronArray extends ArrayLayer implements EditableObject, AttributeContainer {

    @UserParameter(label = "Update Rule", useSetter = true, isObjectType = true, order = 100)
    NeuronUpdateRule prototypeRule = new LinearRule();

    /**
     * Holds data for prototype rule.
     */
    private MatrixDataHolder dataHolder;

    /**
     * Array to hold activation values. These are also the outputs that are consumed by
     * other network components via {@link Layer}.
     */
    private Matrix activations;

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
        super(net, size);
        activations = new Matrix(size, 1);
        randomize();
        setLabel(net.getIdManager().getProposedId(this.getClass()));
        setPrototypeRule(prototypeRule);
    }

    /**
     * Make a deep copy of this array.
     *
     * @param newParent the new parent network
     * @param orig      the array to copy
     * @return the deep copy
     */
    public NeuronArray deepCopy(Network newParent, NeuronArray orig) {
        NeuronArray copy = new NeuronArray(newParent, orig.outputSize());
        copy.setLocation(orig.getLocation());
        copy.setActivations(orig.getActivations());
        copy.setPrototypeRule(orig.getPrototypeRule());
        copy.setDataHolder(orig.getDataHolder().copy());
        return copy;
    }

    @Producible
    public double[] getActivationArray() {return activations.col(0);}

    public Matrix getActivations() {
        return activations;
    }

    @Override
    public Matrix getOutputs() {
        return activations;
    }

    @Override
    public void randomize() {
        activations = Matrix.rand(size(),1,
                new GaussianDistribution(0, 1));
        getEvents().fireUpdated();
    }

    @Override
    public Rectangle2D getBound() {
        return new Rectangle2D.Double(getX() - getWidth() / 2, getY() - getHeight() / 2,
                getWidth(), getHeight());
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
        setLocation(getX() + offsetX, getY() + offsetY);
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
    public void update() {
        if (isClamped()) {
            return;
        }
        prototypeRule.apply(this, dataHolder);
        getInputs().mul(0); // clear inputs
        getEvents().fireUpdated();
    }


    public void setActivations(Matrix newActivations) {
        activations = newActivations;
        getEvents().fireUpdated();
    }

    public void fireLocationChange() {
        getEvents().fireLocationChange();
    }

    /**
     * Input and output size are the same for neuron arrays.
     */
    public int size() {
        return  (int) activations.size();
    }

    @Override
    public int inputSize() {
        return size();
    }

    @Override
    public int outputSize() {
        return (int) activations.size();
    }

    @Override
    public String toString() {
        return getId() + " with " + activations.size() + " activations: " +
                Utils.getTruncatedArrayString(getActivations().col(0), 10);
    }

    @Override
    public void clear() {
        activations.mul(0);
        getEvents().fireUpdated();
    }

    @Override
    public void increment() {
        activations.add(getIncrement());
        getEvents().fireUpdated();
    }

    @Override
    public void decrement() {
        activations.sub(getIncrement());
        getEvents().fireUpdated();
    }

    public void setPrototypeRule(NeuronUpdateRule prototypeRule) {
        this.prototypeRule = prototypeRule;
        dataHolder = prototypeRule.createMatrixData(size());
    }

    public NeuronUpdateRule getPrototypeRule() {
        return prototypeRule;
    }

    public MatrixDataHolder getDataHolder() {
        return dataHolder;
    }

    public void setDataHolder(MatrixDataHolder dataHolder) {
        this.dataHolder = dataHolder;
    }
}
