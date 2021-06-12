package org.simbrain.network.matrix;

import org.simbrain.network.NetworkModel;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.SynapseUpdateRule;
import org.simbrain.network.events.WeightMatrixEvents;
import org.simbrain.network.groups.AbstractNeuronCollection;
import org.simbrain.network.synapse_update_rules.StaticSynapseRule;
import org.simbrain.util.DataHolder;
import org.simbrain.util.UserParameter;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Consumable;
import org.simbrain.workspace.Producible;
import smile.math.matrix.Matrix;
import smile.stat.distribution.GaussianDistribution;

import java.util.Arrays;

/**
 * An weight matrix that connects a source and target {@link WeightMatrixConnectable}
 * object.
 */
public class WeightMatrix extends NetworkModel implements EditableObject, AttributeContainer  {

    /**
     * The source "layer" / activation vector for this weight matrix.
     */
    private WeightMatrixConnectable source;

    /**
     * The target "layer" for this weight matrix.
     */
    private WeightMatrixConnectable target;

    /**
     * Reference to network this neuron is part of.
     */
    private final Network parent;

    /**
     * When true, the WeightMatrixNode will draw a curve instead of a straight line.
     * Set to true when there are weight matrices going in both directions between
     * neuron arrays so that they would block each other with a straight line.
     */
    private boolean useCurve = false;

    @UserParameter(label = "Increment amount", increment = .1, order = 20)
    private double increment = .1;

    @UserParameter(label = "Learning Rule", useSetter = true, isObjectType = true, order = 100)
    SynapseUpdateRule prototypeRule = new StaticSynapseRule();

    /**
     * Holds data for prototype rule.
     */
    private DataHolder dataHolder = new DataHolder.EmptyDataHolder();

    /**
     * Holds data for spike responder.
     */
    private DataHolder spikeResponseHolder = new DataHolder.EmptyDataHolder();

    /**
     * The weight matrix object.
     */
    private Matrix weightMatrix;

    /**
     * WeightMatrixNode will render an image of this matrix if set to true
     */
    private boolean enableRendering = true;

    /**
     * Event support.
     */
    private transient WeightMatrixEvents events = new WeightMatrixEvents(this);

    /**
     * Construct the matrix.
     *
     * @param net parent network
     * @param source source layer
     * @param target target layer
     */
    public WeightMatrix(Network net, WeightMatrixConnectable source, WeightMatrixConnectable target) {
        this.parent = net;
        this.source = source;
        this.target = target;

        source.addOutgoingWeightMatrix(this);
        target.addIncomingWeightMatrix(this);

        initEvents();

        weightMatrix = new Matrix(source.getActivations().length,
                target.getActivations().length);

        // Hack to initialize backend array so there are no delays later at first computation
        weightMatrix.aat();

        // Default for "adapter" cases is 1-1
        if (source instanceof AbstractNeuronCollection) {
            diagonalize();
        } else {
            // For now randomize new matrices between arrays
            randomize();
        }
    }

    private void initEvents() {

        // When the parents of the matrix are deleted, delete the matrix
        source.getEvents().onDeleted(m -> {
            delete();
        });
        target.getEvents().onDeleted(m -> {
            delete();
        });
    }

    @Override
    public String toString() {
        return getId()
                + " (" + weightMatrix.nrows() + "x" + weightMatrix.ncols() + ") "
                + "connecting " + source.getId() + " to " + target.getId();
    }

    public WeightMatrixConnectable getSource() {
        return source;
    }

    public WeightMatrixConnectable getTarget() {
        return target;
    }

    public Matrix getWeightMatrix() {
        return weightMatrix;
    }

    @Producible
    public double[] getWeights() {
        return Arrays.stream(weightMatrix.toArray())
                .flatMapToDouble(Arrays::stream)
                .toArray();
    }

    public boolean isUseCurve() {
        return useCurve;
    }

    public void setUseCurve(boolean useCurve) {
        this.useCurve = useCurve;
        events.fireLineUpdated();
    }

    @Consumable
    public void setWeights(double[] newWeights) {
        int len = Math.min((int) weightMatrix.size(), newWeights.length);
        for (int i = 0; i < len; i++) {
            weightMatrix.set(i / weightMatrix.ncols(), i % weightMatrix.ncols(), newWeights[i]);
        }
        events.fireUpdated();
    }

    public boolean isEnableRendering() {
        return enableRendering;
    }

    public void setEnableRendering(boolean enableRendering) {
        this.enableRendering = enableRendering;
    }

    @Override
    public void delete() {
        source.removeOutgoingWeightMatrix(this);
        target.removeIncomingWeightMatrix(this);
        events.fireDeleted();
    }

    @Override
    public void
    randomize() {
        weightMatrix = Matrix.rand(source.getActivations().length,  target.getActivations().length,
                new GaussianDistribution(0, 1));
        events.fireUpdated();
    }

    public WeightMatrixEvents getEvents() {
        return events;
    }

    @Override
    public void increment() {
        weightMatrix.add(increment);
        events.fireUpdated();
    }

    @Override
    public void decrement() {
        weightMatrix.sub(increment);
        events.fireUpdated();
    }

    /**
     * Set all entries to 0.
     */
    public void hardClear() {
        weightMatrix = new Matrix(weightMatrix.nrows(), weightMatrix.ncols());
        events.fireUpdated();
    }

    /**
     * Diagonalize the matrix.
     */
    public void diagonalize() {
        clear();
        weightMatrix = Matrix.eye(Math.min(source.getActivations().length, target.getActivations().length));
        events.fireUpdated();
    }

    @Override
    public void postUnmarshallingInit() {
        if (events == null) {
            events = new WeightMatrixEvents(this);
        }
        initEvents();
    }

    /**
     * Returns the product of the this matrix its source activations
     */
    public double[] weightsTimesSource() {
        return weightMatrix.mv(source.getActivations());
    }

    @Override
    public void update() {
        // TODO: Check for clamping
        if (! (prototypeRule instanceof StaticSynapseRule)){
            weightMatrix = prototypeRule.apply(source.getMatrix(),
                    target.getMatrix(), weightMatrix, dataHolder);
            events.fireUpdated();
        }
    }

    public SynapseUpdateRule getPrototypeRule() {
        return prototypeRule;
    }

    public void setPrototypeRule(SynapseUpdateRule prototypeRule) {
        this.prototypeRule = prototypeRule;
    }
}
