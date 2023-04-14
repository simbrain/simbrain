package org.simbrain.network.matrix;


import org.jetbrains.annotations.NotNull;
import org.simbrain.network.core.ArrayLayer;
import org.simbrain.network.core.Layer;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.events.NeuronArrayEvents2;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.network.util.MatrixDataHolder;
import org.simbrain.util.UserParameter;
import org.simbrain.util.Utils;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Producible;
import smile.math.matrix.Matrix;
import smile.stat.distribution.GaussianDistribution;

import java.awt.geom.Rectangle2D;

/**
 * A "neuron array" backed by a Smile Matrix. Stored as a column vector.
 */
public class NeuronArray extends ArrayLayer implements EditableObject, AttributeContainer {
    @UserParameter(label = "Update Rule", useSetter = true, isObjectType = true, order = 100)
    NeuronUpdateRule updateRule = new LinearRule();

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

    @UserParameter(label = "Grid Mode", useSetter = true, description = "If true, show activations as a grid, " +
            "otherwise show them as a line",
            order = 10)
    private boolean gridMode = false;

    private transient NeuronArrayEvents2 events = new NeuronArrayEvents2();

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
        setUpdateRule(updateRule);
    }

    /**
     * Make a deep copy of this array.
     *
     * @param newParent the new parent network
     * @return the deep copy
     */
    public NeuronArray deepCopy(Network newParent) {
        NeuronArray copy = new NeuronArray(newParent, this.outputSize());
        copy.setLocation(this.getLocation());
        copy.setGridMode(this.gridMode);
        copy.setActivations(this.getActivations());
        copy.setUpdateRule(this.getUpdateRule());
        copy.setDataHolder(this.getDataHolder().copy());
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
        getEvents().getUpdated().fireAndForget();
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

    @NotNull
    @Override
    public NeuronArrayEvents2 getEvents() {
        return events;
    }

    @Override
    public void onCommit() {
        getEvents().getLabelChanged().fireAndForget("", getLabel());
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
        getEvents().getUpdated().fireAndForget();
    }

    /**
     * Since Neuron Array is immutable, this object will be used in the creation dialog.
     */
    public static class CreationTemplate implements EditableObject {

        /**
         * Size of the neuron array.
         */
        @UserParameter(label = "Nodes", description = "Number of nodes", order = 1)
        private int numNodes = 100;


        /**
         * Add a neuron array to network created from field values which should be setup by an Annotated Property
         * Editor.
         *
         * @param network the network this neuron array adds to
         * @return the created neuron array
         */
        public NeuronArray create(Network network) {
            return new NeuronArray(network, numNodes);
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
        updateRule.apply(this, dataHolder);
        getInputs().mul(0); // clear inputs
        getEvents().getUpdated().fireAndForget();
    }


    public void setActivations(Matrix newActivations) {
        activations = newActivations;
        getEvents().getUpdated().fireAndForget();
    }

    public void setActivations(double[] newActivations) {
        setActivations(Matrix.column(newActivations));
    }

    public void fireLocationChange() {
        getEvents().getLocationChanged().fireAndForget();
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
        getEvents().getUpdated().fireAndForget();
    }

    @Override
    public void increment() {
        activations.add(getIncrement());
        getEvents().getUpdated().fireAndForget();
    }

    @Override
    public void decrement() {
        activations.sub(getIncrement());
        getEvents().getUpdated().fireAndForget();
    }

    public void setUpdateRule(NeuronUpdateRule updateRule) {
        this.updateRule = updateRule;
        dataHolder = updateRule.createMatrixData(size());
        getEvents().getUpdated().fireAndForget();
    }

    public NeuronUpdateRule getUpdateRule() {
        return updateRule;
    }

    public MatrixDataHolder getDataHolder() {
        return dataHolder;
    }

    public void setDataHolder(MatrixDataHolder dataHolder) {
        this.dataHolder = dataHolder;
    }

    public void setGridMode(boolean gridMode) {
        this.gridMode = gridMode;
        getEvents().getUpdated().fireAndForget();
    }

    public boolean isGridMode() {
        return gridMode;
    }

    /**
     * See {@link org.simbrain.workspace.serialization.WorkspaceComponentDeserializer}
     */
    public Object readResolve() {
        events = new NeuronArrayEvents2();
        return this;
    }

    public double[] getExcitatoryInputs() {
        return getIncomingConnectors().stream()
                .filter(wm -> wm instanceof WeightMatrix)
                .map(wm -> ((WeightMatrix) wm).getExcitatoryOutputs())
                .reduce(SimbrainMath::addVector)
                .orElse(new double[inputSize()]);
    }
    public double[] getInhibitoryInputs() {
        return getIncomingConnectors().stream()
                .filter(wm -> wm instanceof WeightMatrix)
                .map(wm -> ((WeightMatrix) wm).getInhibitoryOutputs())
                .reduce(SimbrainMath::addVector)
                .orElse(new double[inputSize()]);
    }

}
