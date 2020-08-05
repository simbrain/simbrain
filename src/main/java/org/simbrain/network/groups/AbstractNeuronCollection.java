package org.simbrain.network.groups;

import org.jetbrains.annotations.NotNull;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.simbrain.network.LocatableModel;
import org.simbrain.network.LocatableModelKt;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.dl4j.ArrayConnectable;
import org.simbrain.network.dl4j.WeightMatrix;
import org.simbrain.network.events.NeuronCollectionEvents;
import org.simbrain.network.util.ActivationInputManager;
import org.simbrain.network.util.ActivationRecorder;
import org.simbrain.network.util.SubsamplingManager;
import org.simbrain.util.RectangleOutlines;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.UserParameter;
import org.simbrain.util.Utils;
import org.simbrain.util.propertyeditor.CopyableObject;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Consumable;
import org.simbrain.workspace.Producible;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.simbrain.network.LocatableModelKt.getCenterLocation;
import static org.simbrain.util.GeomKt.minus;

/**
 * Superclass for neuron collections (which are loose assemblages of neurons) and neuron groups (which enforce consistent
 * neuron update rules and track synapse polarity).
 */
public abstract class AbstractNeuronCollection implements CopyableObject, AttributeContainer, ArrayConnectable, LocatableModel {

    /**
     * Reference to the network this group is a part of.
     */
    private final Network parentNetwork;

    /**
     * Id of this group.
     */
    @UserParameter(label = "ID", description = "Id of this neuron collection", order = -1, editable = false)
    protected String id;

    /**
     * Name of this group. Null strings lead to default labeling conventions.
     */
    @UserParameter(label = "Label", description = "Neuron collection label", useSetter = true,
            order = 10)
    private String label;

    /**
     * Optional information about the current state of the group. For display in
     * GUI.
     */
    private String stateInfo = "";

    /**
     * Support for property change events.
     */
    protected transient NeuronCollectionEvents events = new NeuronCollectionEvents(this);

    /**
     * References to neurons in this collection
     */
    private List<Neuron> neuronList = new CopyOnWriteArrayList<>();

    /**
     * Array to hold activation values for any caller that needs the activation values for this group in array form.
     * Lazy... activations are only written (and this array is only initialized) when {@link #getActivations()} is
     * called.
     */
    private double[] activations;

    /**
     * For buffered update relative to weight matrices.
     */
    private INDArray arrayBuffer;

    /**
     * A single outgoing weight matrix is possible, to a neuron collection, group, or array.
     */
    private WeightMatrix incomingWeightMatrix;

    /**
     * A neuron collection or group may connect to multiple neuron arrays via weight matrices.
     */
    private List<WeightMatrix> outgoingWeightMatrices = new ArrayList<>();

    /**
     * Whether or not this neuron group is in input mode. If the group is in
     * input mode then its update involves either injecting activation or
     * directly setting the activation of the neurons in the group based on
     * the values in test data, ignoring all other inputs.
     */
    @UserParameter(label = "Input mode", order = 40)
    protected boolean inputMode = false;

    /**
     * Maintains a matrix of data that can be used to send inputs to this neuron collection.
     */
    protected ActivationInputManager inputManager;

    /**
     * Allows activations to be downsampled.
     */
    protected SubsamplingManager subsamplingManager;

    /**
     * Manage recording activation histories for a network
     */
    protected ActivationRecorder activationRecorder;

    /**
     * Default constructor.
     */
    public AbstractNeuronCollection(Network net) {
        parentNetwork = net;
        inputManager = new ActivationInputManager(this);
        subsamplingManager = new SubsamplingManager(this);
        activationRecorder = new ActivationRecorder(this);
    }

    /**
     * Get the central x coordinate of this group, based on the positions of the neurons that comprise it.
     *
     * @return the center x coordinate.
     */
    public double getCenterX() {
        return getCenterLocation(neuronList).x;
    }

    /**
     * Get the central y coordinate of this group, based on the positions of the neurons that comprise it.
     *
     * @return the center y coordinate.
     */
    public double getCenterY() {
        return getCenterLocation(neuronList).y;
    }

    @Override
    public Point2D getLocation() {
        return getCenterLocation(neuronList);
    }

    @Override
    public void setLocation(@NotNull Point2D location) {
        Point2D delta = minus(location, getLocation());
        neuronList.forEach(n -> {
            n.offset(delta.getX(), delta.getY());
        });
        events.fireLocationChange();
    }

    public Rectangle2D getBound() { return LocatableModelKt.getBound(neuronList); }

    public RectangleOutlines getOutlines() { return LocatableModelKt.getOutlines(neuronList); }

    /**
     * Return the width of this group, based on the positions of the neurons that comprise it.
     *
     * @return the width of the group
     */
    public double getWidth() {
        return getBound().getWidth();
    }

    /**
     * Return the height of this group, based on the positions of the neurons that comprise it.
     *
     * @return the height of the group
     */
    public double getHeight() {
        return getBound().getHeight();
    }

    /**
     * @return the longest dimensions upon which neurons are laid out.
     */
    public double getMaxDim() {
        if (getWidth() > getHeight()) {
            return getWidth();
        } else {
            return getHeight();
        }
    }

    /**
     * Translate all neurons (the only objects with position information).
     *
     * @param offsetX x offset for translation.
     * @param offsetY y offset for translation.
     */
    public void offset(final double offsetX, final double offsetY) {
        for (Neuron neuron : neuronList) {
            neuron.offset(offsetX, offsetY, false);
        }
        events.fireLocationChange();
    }

    /**
     * Returns an neuron using a provided index
     *
     * @param i index of the neuron in the neuron list
     */
    public Neuron getNeuron(int i) {
        return neuronList.get(i);
    }

    /**
     * Add a neuron to the collection.
     */
    public void addNeuron(Neuron neuron) {
        neuronList.add(neuron);
        addListener(neuron);
    }

    /**
     * Add a collection of neurons.
     */
    public void addNeurons(Collection<Neuron> neurons) {
        neuronList.addAll(neurons);
        neurons.forEach(this::addListener);
    }

    /**
     * Add listener to indicated neuron.
     */
    private void addListener(Neuron n) {
        n.getEvents().onLocationChange(() -> events.fireLocationChange());
    }

    /**
     * Remove a neuron
     *
     * @param neuron the neuron to remove
     */
    public void removeNeuron(Neuron neuron) {
        neuronList.remove(neuron);
    }

    /**
     * Remove all neurons.
     */
    public void removeAllNeurons() {
        neuronList.clear();
    }

    /**
     * Force set activations of neurons using an array of doubles. Assumes the order of the items in the array should
     * match the order of items in the neuronlist.
     * <p>
     * Does not throw an exception if the provided input array and neuron list do not match in size.
     *
     * @param inputs the input vector as a double array.
     */
    @Consumable()
    public void forceSetActivations(double[] inputs) {
        for (int i = 0, n = size(); i < n; i++) {
            if (i >= inputs.length) {
                break;
            }
            neuronList.get(i).forceSetActivation(inputs[i]);
        }
    }

    /**
     * True if the group contains the specified neuron.
     *
     * @param n neuron to check for.
     * @return true if the group contains this neuron, false otherwise
     */
    public boolean containsNeuron(final Neuron n) {
        return neuronList.contains(n);
    }

    /**
     * Set clamping on all neurons in this group.
     *
     * @param clamp true to clamp them, false otherwise
     */
    public void setClamped(final boolean clamp) {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.setClamped(clamp);
        }
    }

    /**
     * Set all activations to a specified value.
     *
     * @param value the value to set the neurons to
     */
    public void setActivationLevels(final double value) {
        for (Neuron n : getNeuronList()) {
            n.setActivation(value);
        }
    }

    /**
     * Force set all activations to a specified value.
     *
     * @param value the value to set the neurons to
     */
    public void forceSetActivationLevels(final double value) {
        for (Neuron n : getNeuronList()) {
            n.forceSetActivation(value);
        }
    }

    /**
     * Copy activations from one neuron group to this one.
     *
     * @param toCopy the group to copy activations from.
     */
    public void copyActivations(AbstractNeuronCollection toCopy) {
        int i = 0;
        for (Neuron neuron : toCopy.getNeuronList()) {
            if (i < neuronList.size()) {
                neuronList.get(i).setActivation(neuron.getInputValue() + neuron.getActivation());
                neuronList.get(i++).setSpike(neuron.isSpike());

            }
        }
    }

    /**
     * Print activations as a vector.
     */
    public void printActivations() {
        System.out.println(Utils.doubleArrayToString(Network.getActivationVector(neuronList)));
    }

    /**
     * Randomize all neurons in group.
     */
    public void randomize() {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.randomize();
        }
    }

    /**
     * Randomize bias for all neurons in group.
     *
     * @param lower lower bound for randomization.
     * @param upper upper bound for randomization.
     */
    public void randomizeBiases(double lower, double upper) {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.randomizeBias(lower, upper);
        }
    }

    /**
     * Return flat list of fanins for all neurons in group.
     *
     * @return incoming weights
     */
    public List<Synapse> getIncomingWeights() {
        List<Synapse> retList = new ArrayList<Synapse>();
        for (Neuron neuron : this.getNeuronList()) {
            retList.addAll(neuron.getFanIn());
        }
        return retList;
    }

    /**
     * Return flat list of fanouts for all neurons in group.
     *
     * @return outgoing weights
     */
    public List<Synapse> getOutgoingWeights() {
        List<Synapse> retList = new ArrayList<Synapse>();
        for (Neuron neuron : this.getNeuronList()) {
            retList.addAll(neuron.getFanOut().values());
        }
        return retList;
    }

    /**
     * Randomize fan-in for all neurons in group.
     */
    public void randomizeIncomingWeights() {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.randomizeFanIn();
        }
//        getParentNetwork().fireSynapsesUpdated(getIncomingWeights()); // TODO: [event] let synapse handle this
    }

    /**
     * Randomize fan-out for all neurons in group.
     */
    public void randomizeOutgoingWeights() {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.randomizeFanOut();
        }
//        getParentNetwork().fireSynapsesUpdated(getOutgoingWeights()); // TODO: [event] let synapse handle this
    }

    public abstract void setNeuronType(String rule);

    /**
     * Set all activations to 0.
     */
    public void clearActivations() {
        for (Neuron n : this.getNeuronList()) {
            n.clear();
        }
    }

    @Override
    public INDArray getOutputArray() {
        float[] floatActivation = new float[getActivations().length];
        // Potential performance cost, but no clear way around this
        for (int i = 0; i < getActivations().length; i++) {
            floatActivation[i] = (float) getActivations()[i];
        }
        return Nd4j.create(new int[]{floatActivation.length}, floatActivation);
    }

    @Override
    public long inputSize() {
        return neuronList.size();
    }

    @Override
    public long outputSize() {
        return neuronList.size();
    }

    @Override
    public void setInputArray(INDArray activations) {
        setInputValues(activations.toDoubleVector());
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
    public void onLocationChange(Runnable task) {
        events.onLocationChange(task);
    }

    /**
     * Returns true if all the neurons in this group are clamped.
     *
     * @return true if all neurons are clamped, false otherwise
     */
    public boolean isAllClamped() {
        boolean ret = true;
        for (Neuron n : getNeuronList()) {
            if (!n.isClamped()) {
                ret = false;
            }
        }
        return ret;
    }

    /**
     * Returns true if all the neurons in this group are unclamped.
     *
     * @return true if all neurons are unclamped, false otherwise
     */
    public boolean isAllUnclamped() {
        boolean ret = true;
        for (Neuron n : getNeuronList()) {
            if (n.isClamped()) {
                ret = false;
            }
        }
        return ret;
    }

    /**
     * Set the lower bound on all neurons in this group.
     *
     * @param lb the lower bound to set.
     */
    public void setLowerBound(double lb) {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.setLowerBound(lb);
        }
    }

    /**
     * Set the upper bound on all neurons in this group.
     *
     * @param ub the upper bound to set.
     */
    public void setUpperBound(double ub) {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.setUpperBound(ub);
        }
    }

    /**
     * Set the increment on all neurons in this group.
     *
     * @param increment the increment to set.
     */
    public void setIncrement(double increment) {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.setIncrement(increment);
        }
    }

    /**
     * Utility to method (used in couplings) to get a string showing the labels of all "active" neurons (neurons with
     * activation above a threshold).
     *
     * @param threshold threshold above which to consider a neuron "active"
     * @return the "active labels"
     */
    public String getLabelsOfActiveNeurons(double threshold) {
        StringBuilder strBuilder = new StringBuilder("");
        for (Neuron neuron : neuronList) {
            if ((neuron.getActivation() > threshold) && (!neuron.getLabel().isEmpty())) {
                strBuilder.append(neuron.getLabel() + " ");
            }
        }
        return strBuilder.toString();
    }

    /**
     * Returns the label of the most active neuron.
     *
     * @return the label of the most active neuron
     */
    public String getMostActiveNeuron() {
        double min = Double.MIN_VALUE;
        String result = "";
        for (Neuron neuron : neuronList) {
            if ((neuron.getActivation() > min) && (!neuron.getLabel().isEmpty())) {
                result = neuron.getLabel();
                min = neuron.getActivation();
            }
        }
        return result + " ";
    }

    /**
     * Sets the polarities of every neuron in the group.
     */
    public void setPolarity(SimbrainConstants.Polarity p) {
        for (Neuron n : neuronList) {
            n.setPolarity(p);
        }
    }

    /**
     * Get the neuron with the specified label, or null if none found.
     *
     * @param label label to search for
     * @return the associated neuron
     */
    public Neuron getNeuronByLabel(String label) {
        return neuronList.stream()
                .filter(n -> n.getLabel().equalsIgnoreCase(label))
                .findFirst()
                .orElse(null);
    }


    /**
     * Set input values of neurons using an array of doubles. Assumes the order
     * of the items in the array matches the order of items in the neuronlist.
     * <p>
     * Does not throw an exception if the provided input array and neuron list
     * do not match in size.
     *
     * @param inputs the input vector as a double array.
     */
    @Consumable()
    public void setInputValues(double[] inputs) {
        for (int i = 0, n = size(); i < n; i++) {
            if (i >= inputs.length) {
                break;
            }
            neuronList.get(i).setInputValue(inputs[i]);
        }
    }

    /**
     * Adds input values.  Useful when doing a many to one coupling.
     */
    @Consumable()
    public void addInputValues(double[] inputs) {
        for (int i = 0, n = size(); i < n; i++) {
            if (i >= inputs.length) {
                break;
            }
            neuronList.get(i).addInputValue(inputs[i]);
        }
    }

    /**
     * Set activations of neurons using an array of doubles. Assumes the order
     * of the items in the array matches the order of items in the neuronlist.
     * <p>
     * Does not throw an exception if the provided input array and neuron list
     * do not match in size.
     *
     * @param inputs the input vector as a double array.
     */
    @Consumable()
    public void setActivations(double[] inputs) {
        for (int i = 0, n = size(); i < n; i++) {
            if (i >= inputs.length) {
                break;
            }
            neuronList.get(i).setActivation(inputs[i]);
        }
    }

    @Producible(arrayDescriptionMethod = "getLabelArray")
    public double[] getActivations() {
        if (activations == null) {
            activations = new double[size()];
        }
        for (int ii=0; ii<size(); ++ii) {
            activations[ii] = neuronList.get(ii).getActivation();
        }
        return activations;
    }

    /**
     * Returns an array of labels, one for each neuron this group.
     * Called by reflection for some coupling related events.
     *
     * @return the label array
     */
    public String[] getLabelArray() {
        String[] retArray = new String[getNeuronList().size()];
        int i = 0;
        for(Neuron neuron : getNeuronList()) {
            if (neuron.getLabel().isEmpty()) {
                retArray[i++] = neuron.getId();
            } else {
                retArray[i++] = neuron.getLabel();
            }
        }
        return retArray;
    }

    @Override
    public Network getNetwork() {
        return getParentNetwork();
    }

    public List<Neuron> getNeuronList() {
        return Collections.unmodifiableList(neuronList);
    }

    public boolean isEmpty() {
        return neuronList.isEmpty();
    }

    public int size() {
        return neuronList.size();
    }

    /**
     * Sets whether or not this neuron group is in input mode. When in input
     * mode the neuron group will draw activations from its {@link ActivationInputManager}
     * instead of from any impinging synapses or its own neuron update
     * functions. This function removes the neurons from the neuron set in
     * ConcurrentBufferedUpdate, preventing it from updating the neurons in
     * this group, and re-adds those neurons when input mode is turned off.
     * Thus the update action associated with this neuron group MUST be added
     * to the network update sequence even if ParallelBufferedUpdate is
     * selected in order for input values to update the group properly.
     *
     * @param inputMode whether or not this group will run in input mode during
     *                  network and workspace updates.
     * @throws IllegalArgumentException if input mode is set to true, but there is no data
     */
    public void setInputMode(boolean inputMode) throws IllegalArgumentException {
        if (inputManager.getData() == null && inputMode) {
            throw new IllegalArgumentException("Cannot set input mode to true" + " if there is no input data stored in NeuronGroup field:" + " testData");
        }
        this.inputMode = inputMode;
        //fireLabelUpdated();
    }
    public double getMinX() {
        return LocatableModelKt.getMinX(neuronList);
    }

    public double getMaxX() {
        return LocatableModelKt.getMaxX(neuronList);
    }

    public double getMinY() {
        return LocatableModelKt.getMinY(neuronList);
    }

    public double getMaxY() {
        return LocatableModelKt.getMaxY(neuronList);
    }

    /**
     * Generic update operations that can be "doubled" if a neuron is part of multiple collections.
     */
    public void update() {
        if (inputMode) {
            updateInputs();
        }
        if (activationRecorder.isRecording()) {
            activationRecorder.writeActsToFile();
        }
    }

    /**
     * Applies input data in {@link ActivationInputManager} to neurons in this group or collection.
     */
    public void updateInputs() {
        if (inputManager.getData() == null) {
            throw new NullPointerException("Test data variable is null," + " but neuron group " + getLabel() + " is in input" + " mode.");
        }
        inputManager.applyCurrentRow();
    };

    public ActivationInputManager getInputManager() {
        return inputManager;
    }

    public SubsamplingManager getSubsamplingManager() {
        return subsamplingManager;
    }

    public ActivationRecorder getActivationRecorder() {
        return activationRecorder;
    }

    /**
     * Returns a vector of subsampled activations to be used by some object external to the
     * neuron group. If plotting activations of a thousand
     * node network, a sample of 100 activations might be returned.
     *
     * @return the vector of external activations.
     */
    @Producible()
    public double[] getSubsampledActivations() {
        return subsamplingManager.getActivations();
    }

    @Consumable(defaultVisibility = false)
    public void setLabel(String label) {
        String oldLabel = this.label;
        this.label = label;
        events.fireLabelChange(oldLabel, label);
    }

    @Producible(defaultVisibility = false)
    public String getLabel() {
        return label;
    }

    public Network getParentNetwork() {
        return parentNetwork;
    }

    @Override
    public AbstractNeuronCollection copy() {
        //TODO
        return null;
    }

    @Override
    public String getName() {
        return null; //TODO
    }

    @Override
    public void onCommit() {
        //todo
    }

    @Override
    public String getId() {
        return id;
    }

    public String getStateInfo() {
        return stateInfo;
    }

    public void setStateInfo(String stateInfo) {
        this.stateInfo = stateInfo;
    }

    public void postUnmarshallingInit() {
        if (events == null) {
            events = new NeuronCollectionEvents(this);
        }

        neuronList.forEach(this::addListener);

        // TODO: Resave and remove
        if (activationRecorder == null) {
            activationRecorder = new ActivationRecorder(this);
        }
    }

    public NeuronCollectionEvents getEvents() {
        return events;
    }

    @Override
    public void setBufferValues() {
        update(); // For loose neurons. Weight matrix buffered update handled by weight matrix
    }

    @Override
    public void applyBufferValues() {
        if (arrayBuffer != null) {
            setInputArray(arrayBuffer.dup());
        }
    }

    @Override
    public void setInputBuffer(INDArray activations) {
        arrayBuffer = activations;
    }
}
