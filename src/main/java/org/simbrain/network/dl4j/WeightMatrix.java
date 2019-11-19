package org.simbrain.network.dl4j;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.simbrain.network.LocatableModel;
import org.simbrain.network.NetworkModel;
import org.simbrain.network.core.Network;
import org.simbrain.network.groups.NeuronCollection;
import org.simbrain.util.UserParameter;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Consumable;
import org.simbrain.workspace.Producible;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * An ND4J weight matrix that connects a source and target {@link ArrayConnectable}
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
    @UserParameter(
            label = "Label"
    )
    private String label = "";

    /**
     * Id of this array.
     */
    @UserParameter(label = "ID", description = "Id of this weight matrix", order = -1, editable = false)
    private String id;

    /**
     * The weight matrix object.
     */
    private INDArray weightMatrix;

    /**
     * WeightMatrixNode will render an image of this matrix if set to true
     */
    private boolean enableRendering = true;

    /**
     * Support for property change events.
     */
    private transient PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

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

        // Default for "adapter" cases is 1-1
        if (source instanceof NeuronCollection || target instanceof NeuronCollection) {
            weightMatrix = Nd4j.create(source.outputSize(), target.inputSize());
            INDArray id = Nd4j.eye(Math.min(source.outputSize(), target.inputSize()));
            weightMatrix.get(NDArrayIndex.createCoveringShape(id.shape())).assign(id);
        } else {
            // For now randomize new matrices between arrays
            randomize();
        }


    }

    /**
     * Initialize the id for this array. A default label based
     * on the id is also set.
     */
    public void initializeId() {
        id = parent.getWeightMatrixGenerator().getId();
        label = id.replaceAll("_", " ");
    }
    /**
     * Default update simply matrix multiplies source times matrix and sets
     * result to target.
     */
    public void update() {
        target.setInputArray(source.getOutputArray().mmul(weightMatrix));
    }

    /**
     * Set the label. This prevents the group id being used as the label for
     * new groups.  If null or empty labels are sent in then the group label is used.
     */
    @Consumable(defaultVisibility = false)
    public void setLabel(String label) {
        String oldLabel = this.label;
        this.label = label;
        changeSupport.firePropertyChange("label", oldLabel , label);
    }

    @Override
    public String toString() {
        String ret = new String();
        ret += weightMatrix.rows() + "x" + weightMatrix.columns() + " matrix [" + getId() + "] ";
        ret += "  Connects " + source.getId() + " to " + target.getId() + "\n";
        return ret;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    public ArrayConnectable getSource() {
        return source;
    }

    public ArrayConnectable getTarget() {
        return target;
    }

    public INDArray getWeightMatrix() {
        return weightMatrix;
    }

    @Producible
    public double[] getWeights() {
        return Nd4j.toFlattened(weightMatrix).toDoubleVector();
    }

    public boolean isUseCurve() {
        return useCurve;
    }

    public void setUseCurve(boolean useCurve) {
        this.useCurve = useCurve;
        changeSupport.firePropertyChange("lineUpdated", null , null);
    }

    @Consumable
    public void setWeights(double[] newWeights) {
        weightMatrix.data().setData(newWeights);
        changeSupport.firePropertyChange("updated", null , null);
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
    public void fireDeleted() {
        source.removeOutgoingWeightMatrix(this);
        target.setIncomingWeightMatrix(null);
        target.getOutgoingWeightMatrices().stream()
                .filter(m -> m.getTarget() == source)
                .forEach(m -> { // Even though this is for each but should happen only once.
                    m.setUseCurve(false);
                    setUseCurve(false);
                });
        changeSupport.firePropertyChange("delete", this, null);
    }

    /**
     * Randomize weights in this matrix
     */
    public void randomize() {
        weightMatrix = Nd4j.rand((int) source.outputSize(), (int) target.inputSize()).subi(0.5).mul(2);
        changeSupport.firePropertyChange("updated", null , null);
    }

    //public Layer asLayer() {
    //    return new DenseLayer.Builder().nIn(source.arraySize()).nOut(target.arraySize())
    //            .activation(Activation.SOFTMAX)
    //            // random initialize weights with values between 0 and 1
    //            .weightInit(new UniformDistribution(0, 1))
    //            .build();
    //}
}
