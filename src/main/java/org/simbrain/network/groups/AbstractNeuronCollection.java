package org.simbrain.network.groups;

import org.jetbrains.annotations.NotNull;
import org.simbrain.network.LocatableModelKt;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.events.NeuronCollectionEvents;
import org.simbrain.network.matrix.WeightMatrixConnectable;
import org.simbrain.network.util.ActivationInputManager;
import org.simbrain.network.util.ActivationRecorder;
import org.simbrain.network.util.SubsamplingManager;
import org.simbrain.util.RectangleOutlines;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.Utils;
import org.simbrain.util.propertyeditor.CopyableObject;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Consumable;
import org.simbrain.workspace.Producible;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.simbrain.network.LocatableModelKt.getCenterLocation;
import static org.simbrain.util.GeomKt.minus;

/**
 * Superclass for neuron collections (which are loose assemblages of neurons) and neuron groups (which enforce consistent
 * neuron update rules and track synapse polarity).
 */
public abstract class AbstractNeuronCollection extends WeightMatrixConnectable implements CopyableObject, AttributeContainer {

    /**
     * Reference to the network this group is a part of.
     */
    private final Network parentNetwork;

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
     * Cache of neuron activation values.
     */
    private double[] activations;

    /**
     * Flag to mark whether {@link #activations} is "dirty", that is outdated and in need of updating.
     */
    private boolean cachedActivationsDirty = true;

    /**
     * References to neurons in this collection
     */
    private final List<Neuron> neuronList = new CopyOnWriteArrayList<>();

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

    @NotNull
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
     * Add a neuron to the collection. Exposed in {@link NeuronCollection} but not in {@link NeuronGroup}
     */
    protected void addNeuron(Neuron neuron) {
        neuronList.add(neuron);
        addListener(neuron);
    }

    /**
     * Add a collection of neurons.
     */
    protected void addNeurons(Collection<Neuron> neurons) {
        neurons.forEach(this::addNeuron);
    }

    /**
     * Add listener to indicated neuron.
     */
    protected void addListener(Neuron n) {
        n.getEvents().onLocationChange(fireLocationChange);
        n.getEvents().onActivationChange((aold,anew) -> {
            invalidateCachedActivations();
        });
    }

    private Runnable createFireLocationChange() {
        AtomicReference<Timer> timer = new AtomicReference<>();
        timer.set(null); // debouncing
        return () -> {
            var thisTimer = timer.get();
            if (thisTimer != null) {
                thisTimer.cancel();
            }
            thisTimer = new Timer();
            thisTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    events.fireLocationChange();
                }
            }, 5);
            timer.set(thisTimer);
        };
    }

    private Runnable fireLocationChange = createFireLocationChange();

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
     * Force set all activations to a specified value.
     *
     * @param value the value to set the neurons to
     */
    public void forceSetActivationLevels(final double value) {
        for (Neuron n : getNeuronList()) {
            n.forceSetActivation(value);
        }
        cachedActivationsDirty = true;
    }

    @Override
    public void randomize() {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.randomize();
        }
        invalidateCachedActivations();
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
        invalidateCachedActivations();
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

    @Override
    public void updateInputs() {
        // if (inputManager.getData() == null) {
        //     throw new NullPointerException("Test data variable is null," + " but neuron group " + getLabel() + " is in input" + " mode.");
        // }
        // inputManager.applyCurrentRow(); // TODO

        // Add weighted inputs to inputs
        addInputs(getWeightedInputs());
    }

    @Override
    public void update() {
        if (activationRecorder.isRecording()) {
            activationRecorder.writeActsToFile();
        }
        invalidateCachedActivations();
    }

    @Producible(arrayDescriptionMethod = "getLabelArray")
    @Override
    public double[] getActivations() {
        if (cachedActivationsDirty) {
            activations = neuronList.stream()
                    .map(Neuron::getActivation)
                    .mapToDouble(Double::doubleValue)
                    .toArray();
            cachedActivationsDirty = false;
            return activations;
        } else {
            return activations;
        }
    }

    /**
     * Set input values of neurons using an array of doubles. Assumes the order
     * of the items in the array matches the order of items in the neuronlist.
     * <p>
     * Does not throw an exception if the provided input array and neuron list
     * do not match in size.
     */
    @Override
    @Consumable
    public void addInputs(double[] inputs) {
        int size = Math.min(inputs.length, neuronList.size());
        for (int i = 0; i < size; i++) {
            neuronList.get(i).addInputValue(inputs[i]);
        }
    }

    @Override
    protected double[] getInputs() {
        return neuronList.stream()
                .map(Neuron::getInput)
                .mapToDouble(Double::doubleValue)
                .toArray();
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
     * Set activations of neurons using an array of doubles. Assumes the order
     * of the items in the array matches the order of items in the neuronlist.
     * <p>
     * Does not throw an exception if the provided input array and neuron list
     * do not match in size.
     *
     * @param activations the input vector as a double array.
     */
    @Consumable()
    public void setActivations(double[] activations) {
        int size = Math.min(activations.length, neuronList.size());
        for (int i = 0; i < size; i++) {
            neuronList.get(i).setActivation(activations[i]);
        }
        cachedActivationsDirty = true;
    }

    protected void invalidateCachedActivations() {
        cachedActivationsDirty = true;
    }

    //TODO
    @Consumable()
    public void forceSetActivations(double[] activations) {
        int size = Math.min(activations.length, neuronList.size());
        for (int i = 0; i < size; i++) {
            neuronList.get(i).forceSetActivation(activations[i]);
        }
        cachedActivationsDirty = true;
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
        // this.inputMode = inputMode;
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

    public Network getParentNetwork() {
        return parentNetwork;
    }

    @Override
    public AbstractNeuronCollection copy() {
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

    public String getStateInfo() {
        return stateInfo;
    }

    public void setStateInfo(String stateInfo) {
        this.stateInfo = stateInfo;
    }

    @Override
    public void postUnmarshallingInit() {
        if (events == null) {
            events = new NeuronCollectionEvents(this);
        }

        if (fireLocationChange == null) {
            fireLocationChange = createFireLocationChange();
        }

        // TODO: Resave and remove
        if (activationRecorder == null) {
            activationRecorder = new ActivationRecorder(this);
        }
    }

    public NeuronCollectionEvents getEvents() {
        return events;
    }

    @Override
    public String toString() {
        return getId() + " with " + getActivations().length + " activations: " +
                Utils.getTruncatedArrayString(getActivations(), 10);
    }
}
