package org.simbrain.network.matrix;

import org.simbrain.network.NetworkModel;
import org.simbrain.network.core.Network;
import org.simbrain.network.events.WeightMatrixEvents;
import org.simbrain.network.groups.NeuronCollection;
import org.simbrain.util.UserParameter;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Consumable;
import org.simbrain.workspace.Producible;
import smile.math.matrix.Matrix;
import smile.stat.distribution.GaussianDistribution;

import java.util.Arrays;

/**
 * An weight matrix that connects a source and target {@link ArrayConnectable}
 * object.
 */
public class WeightMatrix implements EditableObject, AttributeContainer, NetworkModel {

    /**
     * The source "layer" / activation vector for this weight matrix.
     */
    private ArrayConnectable source;

    /**
     * The target "layer" for this weight matrix.
     */
    private ArrayConnectable target;

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

    /**
     * A label for this Neuron Array for display purpose.
     */
    @UserParameter(label = "Label", order = 10)
    private String label = "";

    /**
     * Id of this array.
     */
    @UserParameter(label = "ID", description = "Id of this weight matrix", order = -1, editable = false)
    private String id;

    @UserParameter(label = "Increment amount", increment = .1, order = 20)
    private double increment = .1;

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
    public WeightMatrix(Network net, ArrayConnectable source, ArrayConnectable target) {
        this.parent = net;
        this.source = source;
        this.target = target;

        source.addOutgoingWeightMatrix(this);
        target.setIncomingWeightMatrix(this);

        initEvents();

        // Default for "adapter" cases is 1-1
        if (source instanceof NeuronCollection || target instanceof NeuronCollection) {
            weightMatrix = new Matrix(source.outputSize(), target.inputSize());
            diagonalize();
        } else {
            // For now randomize new matrices between arrays
            randomize();
        }

        initializeId();

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

    /**
     * Initialize the id for this array. A default label based
     * on the id is also set.
     */
    public void initializeId() {
        id = parent.getIdManager().getId(WeightMatrix.class);
        label = id.replaceAll("_", " ");
    }

    @Override
    public String getLabel() {
        // Label not used for weight matrix currently
        return id;
    }

    /**
     * Default update simply matrix multiplies source times matrix and sets
     * result to target.
     */
    public void update() {
        // TODO: Check
        double[][] outputArray = new double[][]{source.getOutputArray()};
        target.setInputArray(Arrays.stream(new Matrix(1, source.inputSize(), outputArray)
                .mm(weightMatrix)
                .toArray())
                .flatMapToDouble(Arrays::stream)
                .toArray());
    }

    @Override
    public void setBufferValues() {
        // TODO
        update();
    }

    @Override
    public void applyBufferValues() {
        // No implementation?
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

    @Override
    public String toString() {
        String ret = new String();
        ret += weightMatrix.nrows() + "x" + weightMatrix.ncols() + " matrix [" + getId() + "] ";
        ret += "  Connects " + source.getId() + " to " + target.getId() + "\n";
        ret += "\t\t" + Arrays.deepToString(weightMatrix.toArray()) + "\n";
        return ret;
    }

    public ArrayConnectable getSource() {
        return source;
    }

    public ArrayConnectable getTarget() {
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
        weightMatrix = new Matrix(newWeights); // TODO: Is this ok?
        events.fireUpdated();
    }

    public boolean isEnableRendering() {
        return enableRendering;
    }

    public void setEnableRendering(boolean enableRendering) {
        this.enableRendering = enableRendering;
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * Notify listeners that this object has been deleted.
     */
    public void delete() {
        source.removeOutgoingWeightMatrix(this);
        target.setIncomingWeightMatrix(null);
        target.getOutgoingWeightMatrices().stream()
                .filter(m -> m.getTarget() == source)
                .forEach(m -> { // Even though this is for each but should happen only once.
                    m.setUseCurve(false);
                    setUseCurve(false);
                });
        events.fireDeleted();
        parent.delete(this);
    }

    /**
     * Randomize weights in this matrix
     */
    public void randomize() {
        weightMatrix = Matrix.rand(source.outputSize(), (int) target.inputSize(),
                new GaussianDistribution(0, 1));
        events.fireUpdated();
    }

    public WeightMatrixEvents getEvents() {
        return events;
    }

    /**
     * Add increment to every entry in weight matrix
     */
    public void increment() {
        weightMatrix.add(increment);
        events.fireUpdated();
    }

    /**
     * Subtract increment from every entry in weight matrix
     */
    public void decrement() {
        weightMatrix.sub(increment);
        events.fireUpdated();
    }

    /**
     * Set all entries to 0.
     */
    public void clear() {
        weightMatrix = new Matrix(weightMatrix.nrows(), weightMatrix.ncols());
        events.fireUpdated();
    }

    /**
     * Diagonalize the matrix.
     */
    public void diagonalize() {
        clear();
        weightMatrix = Matrix.eye(Math.min(source.outputSize(), target.inputSize()));
        events.fireUpdated();
    }

    public void postUnmarshallingInit() {
        if (events == null) {
            events = new WeightMatrixEvents(this);
        }
        initEvents();
    }

    //public Layer asLayer() {
    //    return new DenseLayer.Builder().nIn(source.arraySize()).nOut(target.arraySize())
    //            .activation(Activation.SOFTMAX)
    //            // random initialize weights with values between 0 and 1
    //            .weightInit(new UniformDistribution(0, 1))
    //            .build();
    //}
}
