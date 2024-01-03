package org.simbrain.network.groups;

import org.jetbrains.annotations.NotNull;
import org.simbrain.network.LocatableModelKt;
import org.simbrain.network.core.*;
import org.simbrain.network.events.NeuronCollectionEvents;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.layouts.Layout;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.network.util.ActivationInputManager;
import org.simbrain.network.util.ActivationRecorder;
import org.simbrain.network.util.SubsamplingManager;
import org.simbrain.util.RectangleOutlines;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.UserParameter;
import org.simbrain.util.Utils;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.util.propertyeditor.CopyableObject;
import org.simbrain.workspace.Consumable;
import org.simbrain.workspace.Producible;
import smile.math.matrix.Matrix;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.simbrain.network.LocatableModelKt.getCenterLocation;
import static org.simbrain.util.GeomKt.minus;

/**
 * Superclass for neuron collections (which are loose assemblages of neurons) and neuron groups (which enforce consistent
 * neuron update rules and track synapse polarity).
 * <br>
 * Subclasses maintain lists of neurons and can copy their activations to matrices. To communicate with other
 * {@link Layer}s it can create output matrices and accept input matrices, but it wil only create and cache these if
 * relevant methods are called. Matrix based layers should subclass {@link ArrayLayer}
 */
public abstract class AbstractNeuronCollection extends Layer implements CopyableObject {

    /**
     * Reference to the network this group is a part of.
     */
    private final Network parentNetwork;

    /**
     * Set of incoming synapse groups.
     */
    transient private HashSet<SynapseGroup> incomingSgs = new HashSet<>();

    /**
     * Set of outgoing synapse groups.
     */
    transient private HashSet<SynapseGroup> outgoingSgs = new HashSet<>();

    /**
     * Optional information about the current state of the group. For display in
     * GUI.
     */
    private String stateInfo = "";

    /**
     * Support for property change events.
     */
    protected transient NeuronCollectionEvents events = new NeuronCollectionEvents();

    /**
     * Cache of neuron activation values.
     */
    private double[] activations;

    /**
     * Cache of input values.
     */
    private double[] inputs;

    /**
     * Flag to mark whether {@link #activations} is "dirty", that is outdated and in need of updating.
     */
    private boolean cachedActivationsDirty = true;
    private boolean cachedInputsDirty = true;

    /**
     * References to neurons in this collection
     */
    protected final List<Neuron> neuronList = new CopyOnWriteArrayList<>();

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

    @UserParameter(label = "Increment amount", increment = .1, order = 90)
    private double increment = .1;

    /**
     * Space between neurons within a layer.
     */
    private int betweenNeuronInterval = 50;

    /**
     * In method setLayoutBasedOnSize, this is used as the threshold number of neurons in the group, above which to use
     * grid layout instead of line layout.
     */
    private int gridThreshold = 9;

    /**
     * Default layout for neuron groups.
     */
    public static final Layout DEFAULT_LAYOUT = new GridLayout();

    /**
     * The layout for the neurons in this group.
     */
    @UserParameter(label = "Layout", tab = "Layout", order = 150)
    private Layout layout = DEFAULT_LAYOUT;

    /**
     * Default constructor.
     */
    public AbstractNeuronCollection(Network net) {
        parentNetwork = net;
        inputManager = new ActivationInputManager(this);
        subsamplingManager = new SubsamplingManager(this);
        activationRecorder = new ActivationRecorder(this);
    }

    @Override
    public Matrix getOutputs() {
        // TODO: Performance drain? Consider caching this.
        return Matrix.column(getActivations());
    }

    @Override
    public void addInputs(Matrix newInputs) {
        addInputs(newInputs.col(0));
    }

    /**
     * Set input values of neurons using an array of doubles. Assumes the order
     * of the items in the array matches the order of items in the neuronlist.
     * <p>
     * Does not throw an exception if the provided input array and neuron list
     * do not match in size.
     */
    @Consumable
    public void addInputs(double[] inputs) {
        int size = Math.min(inputs.length, neuronList.size());
        for (int i = 0; i < size; i++) {
            neuronList.get(i).addInputValue(inputs[i]);
        }
        invalidateCachedInputs();
    }

    /**
     * Return inputs as a double array. Either create the array or return a cache of it.
     */
    @Producible
    public double[] getInputActivations() {
        if (cachedInputsDirty) {
            inputs = neuronList.stream()
                    .map(Neuron::getInput)
                    .mapToDouble(Double::doubleValue)
                    .toArray();
        }
        return inputs;
    }

    @NotNull
    @Override
    public Matrix getInputs() {
        return Matrix.column(getInputActivations());
    }

    /**
     * Input and output size are the same for collections of neurons.
     */
    public int size() {
        return getActivations().length;
    }

    @Override
    public int outputSize() {
        return size();
    }

    @Override
    public int inputSize() {
        return size();
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
        events.getLocationChanged().fireAndForget();
    }

    @Override
    @NotNull
    public Rectangle2D getBound() { return LocatableModelKt.getBound(neuronList); }

    public RectangleOutlines getOutlines() { return LocatableModelKt.getOutlines(neuronList); }

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
        events.getLocationChanged().fireAndForget();
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
        neuron.setId(getParentNetwork().getIdManager().getAndIncrementId(Neuron.class));
        addListener(neuron);
    }

    /**
     * Add a collection of neurons.
     */
    protected void addNeurons(Collection<Neuron> neurons) {
        neurons.forEach(this::addNeuron);
        subsamplingManager.resetIndices();
    }

    /**
     * Add listener to indicated neuron.
     */
    protected void addListener(Neuron n) {
        n.getEvents().getLocationChanged().on(() -> events.getLocationChanged().fireAndForget());
        // n.getEvents().onLocationChange(fireLocationChange); // TODO Reimplement when debounce is working
        n.getEvents().getDeleted().on(neuronList::remove);
        n.getEvents().getActivationChanged().on((aold,anew) -> {
            invalidateCachedActivations();
        });
        n.getEvents().getDeleted().on(neuron-> {
            neuronList.remove(neuron);
            if (isEmpty()) {
                delete();
            }
        });
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
    @Consumable
    public void forceSetActivationLevels(final double value) {
        for (Neuron n : getNeuronList()) {
            n.forceSetActivation(value);
        }
        cachedActivationsDirty = true;
    }

    @Override
    public void randomize() {
        neuronList.forEach(Neuron::randomize);
        invalidateCachedActivations();
    }

    /**
     * Randomize bias for all neurons in group.
     */
    public void randomizeBiases() {
        for (Neuron neuron : this.getNeuronList()) {
            NetworkUtilsKt.randomizeBias(neuron);
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
    }

    /**
     * Randomize fan-out for all neurons in group.
     */
    public void randomizeOutgoingWeights() {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.randomizeFanOut();
        }
    }

    public HashSet<SynapseGroup> getIncomingSgs() {
        return incomingSgs;
    }

    public HashSet<SynapseGroup> getOutgoingSg() {
        return outgoingSgs;
    }

    public boolean removeIncomingSg(SynapseGroup sg) {
        return incomingSgs.remove(sg);
    }

    public boolean removeOutgoingSg(SynapseGroup sg) {
        return outgoingSgs.remove(sg);
    }

    @Override
    public void delete() {
        super.delete();
        outgoingSgs.forEach(SynapseGroup::delete);
        incomingSgs.forEach(SynapseGroup::delete);
    }

    @Override
    public void updateInputs() {
        // if (inputManager.getData() == null) {
        //     throw new NullPointerException("Test data variable is null," + " but neuron group " + getLabel() + " is in input" + " mode.");
        // }
        // inputManager.applyCurrentRow(); // TODO

        double[] wtdInputs = new double[size()];
        for (Connector c : getIncomingConnectors()) {
            wtdInputs = SimbrainMath.addVector(wtdInputs, c.getOutput().col(0));
        }
        addInputs(wtdInputs);
    }

    @Override
    public void update() {
        if (activationRecorder.isRecording()) {
            activationRecorder.writeActsToFile();
        }
        invalidateCachedActivations();
    }

    @Producible(arrayDescriptionMethod = "getLabelArray")
    public double[] getActivations() {
        if (cachedActivationsDirty) {
            activations = neuronList.stream()
                    .map(Neuron::getActivation)
                    .mapToDouble(Double::doubleValue)
                    .toArray();
            cachedActivationsDirty = false;
        }
        return activations;
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

    protected void invalidateCachedInputs() {
        cachedInputsDirty = true;
    }

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
    public void postOpenInit() {
        super.postOpenInit();
        if (events == null) {
            events = new NeuronCollectionEvents();
        }

        incomingSgs = new HashSet<>();
        outgoingSgs = new HashSet<>();

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

    public void clearInputs() {
        neuronList.forEach(n -> n.clearInput());
    }

    @Override
    public void clear() {
        clearArray();
    }

    @Override
    public void increment() {
        incrementArray(increment);
    }

    @Override
    public void decrement() {
        decrementArray(increment);
    }

    /**
     * Add increment to every entry in weight matrix
     */
    public void incrementArray(double amount) {
        double[] newActivations = Arrays
                .stream(getActivations())
                .map(a -> a + amount)
                .toArray();
        setActivations(newActivations);
        events.getUpdated().fireAndForget();
    }

    /**
     * Subtract increment from every entry in the array
     */
    public void decrementArray(double amount) {
        double[] newActivations = Arrays
                .stream(getActivations())
                .map(a -> a - amount)
                .toArray();
        setActivations(newActivations);
        events.getUpdated().fireAndForget();
    }

    /**
     * Clear array values.
     */
    public void clearArray() {
        double[] newActivations = new double[getActivations().length];
        setActivations(newActivations);
        events.getUpdated().fireAndForget();
    }

    @Override
    public void toggleClamping() {
        neuronList.forEach(Neuron::toggleClamping);
    }

    /**
     * If more than gridThreshold neurons use a grid layout, else a horizontal line layout.
     */
    public void setLayoutBasedOnSize() {
        setLayoutBasedOnSize(new Point2D.Double(0, 0));
    }

    /**
     * If more than gridThreshold neurons use a grid layout, else a horizontal line layout.
     *
     * @param initialPosition the initial Position for the layout
     */
    public void setLayoutBasedOnSize(Point2D initialPosition) {
        if (initialPosition == null) {
            initialPosition = new Point2D.Double(0, 0);
        }
        LineLayout lineLayout = new LineLayout(betweenNeuronInterval, LineLayout.LineOrientation.HORIZONTAL);
        GridLayout gridLayout = new GridLayout(betweenNeuronInterval, betweenNeuronInterval);
        if (getNeuronList().size() < gridThreshold) {
            lineLayout.setInitialLocation(initialPosition);
            setLayout(lineLayout);
        } else {
            gridLayout.setInitialLocation(initialPosition);
            setLayout(gridLayout);
        }
        // Used rather than apply layout to make sure initial position is used.
        getLayout().layoutNeurons(getNeuronList());
    }

    public Point2D.Double getTopLeftLocation() {
        return LocatableModelKt.getTopLeftLocation(neuronList);
    }

    /**
     * Apply this group's layout to its neurons.
     */
    public void applyLayout() {
        layout.setInitialLocation(getTopLeftLocation());
        layout.layoutNeurons(getNeuronList());
    }

    /**
     * Apply this group's layout to its neurons based on a specified top-left initial position.
     *
     * @param initialPosition the position from which to begin the layout.
     */
    public void applyLayout(Point2D initialPosition) {
        layout.setInitialLocation(initialPosition);
        layout.layoutNeurons(getNeuronList());
    }

    /**
     * Forwards to {@link #applyLayout(Point2D)}
     */
    public void applyLayout(int x, int y) {
        applyLayout(new Point2D.Double(x,y));
    }

    /**
     * Sets a new layout and applies it, using the groups' current location.
     */
    public void applyLayout(Layout newLayout) {
        layout = newLayout;
        applyLayout(getLocation());
    }

    public int getBetweenNeuronInterval() {
        return betweenNeuronInterval;
    }

    public void setBetweenNeuronInterval(int betweenNeuronInterval) {
        this.betweenNeuronInterval = betweenNeuronInterval;
    }

    public int getGridThreshold() {
        return gridThreshold;
    }

    public void setGridThreshold(int gridThreshold) {
        this.gridThreshold = gridThreshold;
    }

    public Layout getLayout() {
        return layout;
    }

    public void setLayout(Layout layout) {
        this.layout = layout;
    }
}
