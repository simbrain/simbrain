/*
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network.groups;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.layouts.Layout;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.network.layouts.LineLayout.LineOrientation;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.network.neuron_update_rules.UpdateRuleEnum;
import org.simbrain.network.neuron_update_rules.interfaces.BiasedUpdateRule;
import org.simbrain.util.UserParameter;
import org.simbrain.util.Utils;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.workspace.Producible;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * A group of neurons. A primary abstraction for larger network structures.
 * Layers in feed-forward networks are neuron groups. Self-organizing-maps
 * subclass this class. Etc.
 *<br>
 * Updating is done using a collection of neurons, but in a NeuronGroup they must all be the same type. This allows
 * the group to be characterized as spiking vs. non-spiking, for example, and serves other functions.
 * Because this constraint was added after neuron groups were first introduced, it is not yet rigorously enforced
 * in the code.
 *
 */
public class NeuronGroup extends AbstractNeuronCollection {

    /**
     * The default for how often {@link #writeActsToFile()} should flush
     * the output stream when writing to a file.
     */
    public static final int FLUSH_FREQUENCY = 1000;

    /**
     * The number of neurons in the group by default.
     */
    public static final int DEFAULT_GROUP_SIZE = 10;

    /**
     * "Prototype" update rule.
     */
    @UserParameter(label = "Group Update Rule", useSetter = true, order = 20)
    private UpdateRuleEnum groupUpdateRule;


    /**
     * Default layout for neuron groups.
     */
    public static final Layout DEFAULT_LAYOUT = new GridLayout();

    /**
     * The layout for the neurons in this group.
     */
    private Layout.LayoutObject layout = new Layout.LayoutObject();

    /**
     * Set of incoming synapse groups.
     */
    private final HashSet<SynapseGroup> incomingSgs = new HashSet<SynapseGroup>();

    /**
     * Set of outgoing synapse groups.
     */
    private final HashSet<SynapseGroup> outgoingSgs = new HashSet<SynapseGroup>();

    /**
     * In method setLayoutBasedOnSize, this is used as the threshold number of
     * neurons in the group, above which to use grid layout instead of line
     * layout.
     */
    private int gridThreshold = 9;

    /**
     * Space between neurons within a layer.
     */
    private int betweenNeuronInterval = 50;

    /**
     * Whether or not {@link #writeActsToFile()} will write activations as a
     * state matrix or a spike train.
     */
    private boolean recordAsSpikes;

    /**
     * The output stream which writes activation values to a file.
     */
    private PrintWriter valueWriter;

    /**
     * Whether or not this group is in a state that allows recording.
     */
    private boolean recording;

    /**
     * A counter to keep track of how many times {@link #writeActsToFile()} has
     * been called so as to determine when to flush the output stream.
     */
    private int writeCounter = 0;

    /**
     * Number of subsamples to take. This value is also implicitly a threshold.
     * If a neuron group has more than this many neurons, and subsampling is
     * turned on, a vector with this many components is returned by (
     * {@link #getSubsampledActivations()}
     */
    //@UserParameter(label = "Number of subsamples", useSetter = true)
    // TODO
    private int numSubSamples = 100;

    /**
     * Array to hold subsamples to be used when, for example, plotting the
     * state of large network.
     */
    private double[] subSampledValues = {};

    /**
     * Array to hold activation values for any caller that needs the activation values for this group in array form.
     * Lazy... activations are only written (and this array is only initialized) when {@link #getActivations()} is
     * called.
     */
    private double [] activations;

    /**
     * Indices used with subsampling.
     */
    private int[] subsamplingIndices = {};

    /**
     * Construct a new neuron group from a list of neurons.
     *
     * @param net     the network
     * @param neurons the neurons
     */
    public NeuronGroup(final Network net, final List<Neuron> neurons) {
        super(net);
        addNeurons(neurons);
        resetSubsamplingIndices();
    }

    /**
     * Construct a new neuron group with a specified number of neurons.
     *
     * @param net parent network
     * @param numNeurons how many neurons it will have
     */
    public NeuronGroup(final Network net, final int numNeurons) {
        this(net, new Point2D.Double(0, 0), numNeurons);
    }

    /**
     * Construct a new neuron group with a specified number of neurons.
     *
     * @param net             parent network
     * @param initialPosition initial location of the group
     * @param numNeurons      how many neurons it will have
     */
    public NeuronGroup(final Network net, Point2D initialPosition, final int numNeurons) {
        super(net);
        List<Neuron> newNeurons = new ArrayList<>();
        for (int i = 0; i < numNeurons; i++) {
            Neuron newNeuron = new Neuron(net);
            newNeurons.add(newNeuron);
        }
        // Very slow to add to a copy on write array list so do it this way
        addNeurons(newNeurons);
        layout.getLayout().setInitialLocation(initialPosition);
        layout.getLayout().layoutNeurons(this.getNeuronList());
        resetSubsamplingIndices();
    }

    /**
     * Create a neuron group without any initial neurons and an initial
     * position.
     *
     * @param network         parent network
     * @param initialPosition the starting position from which to lay-out the neurons in the
     *                        group whenever they are added.
     */
    public NeuronGroup(final Network network, Point2D initialPosition) {
        super(network);
        layout.getLayout().setInitialLocation(initialPosition);
        resetSubsamplingIndices();
    }

    /**
     * Create a neuron group without any initial neurons.
     *
     * @param network parent network
     */
    public NeuronGroup(final Network network) {
        super(network);
    }

    /**
     * Copy constructor. pass in network for cases where a group is pasted from
     * one network to another
     *
     * @param network parent network
     * @param toCopy  the neuron group this will become a (deep) copy of.
     */
    public NeuronGroup(final Network network, final NeuronGroup toCopy) {
        super(network);
        List<Neuron> newNeurons = new ArrayList<>();
        for (Neuron neuron : toCopy.getNeuronList()) {
            newNeurons.add(new Neuron(network, neuron));
        }
        addNeurons(newNeurons);
        this.setLayout(toCopy.getLayout());
        this.setGroupUpdateRule(toCopy.groupUpdateRule);
        resetSubsamplingIndices();
    }

    public NeuronGroup(Network network, int numNeurons, String id) {
        super(network);
        for (int i = 0; i < numNeurons ; i++) {
            addNeuron(new Neuron(network, new LinearRule()));
        }
        super.id = id;
    }

    /**
     * Returns a deep copy of the neuron group with a new network parent
     *
     * @param newParent the new parent network for this group, potentially different from the original (used when
     *                  copying and pasting from one network to another)
     */
    public NeuronGroup deepCopy(Network newParent) {
        return new NeuronGroup(newParent, this);
    }

    @Override
    public void delete() {
        if (isMarkedForDeletion()) {
            return;
        } else {
            setMarkedForDeletion(true);
        }
        for (Neuron neuron : getNeuronList()) {
            neuron.getNetwork().removeNeuron(neuron, true);
        }
        if (hasParentGroup()) {
            if (getParentGroup() instanceof Subnetwork) {
                ((Subnetwork) getParentGroup()).removeNeuronGroup(this);
            }
            if (getParentGroup().isEmpty()) {
                getParentNetwork().removeGroup(getParentGroup());
            }
        }

        stopRecording();
        removeAllNeurons();
        changeSupport.firePropertyChange("delete", this, null);
    }

    /**
     * Updates all the neurons in the neuron group according to their
     * NeuronUpdateRule(s). If the group is in input mode reads in the next
     * set of values from the input table and sets the neuron values
     * accordingly.
     */
    @Override
    public void update() {
        super.update();
        if (!inputMode) {
            Network.updateNeurons(getNeuronList());
        }
        if (isRecording()) {
            writeActsToFile();
        }
        fireLabelUpdated();
    }


    /**
     * Creates a file which activations will be written to and activates the
     * necessary output streams. Uses the name of the the group for the name of
     * the file, adding an incremented number to the name each time this method
     * is called. Recording happens only if the appropriate neuron group
     * recording action is a part of the network update. Also checks if this
     * neuron group is populated entirely by spiking neurons and if so, sets
     * {@link #recordAsSpikes} to true, since {@link #writeActsToFile()} writes
     * activations differently if the neuron group contains only spiking
     * neurons.
     *
     * @param outputFile the file to write the activations to
     */
    public void startRecording(final File outputFile) {
        boolean spikeRecord = true;
        for (Neuron n : getNeuronList()) {
            if (!n.getUpdateRule().isSpikingNeuron()) {
                spikeRecord = false;
                break;
            }
        }
        recordAsSpikes = spikeRecord;
        recording = true;
        try {
            if (valueWriter != null) {
                valueWriter.close();
            }
            FileWriter fw = new FileWriter(outputFile);
            valueWriter = new PrintWriter(fw);
        } catch (IOException e) {
            e.printStackTrace();
        }
        changeSupport.firePropertyChange("recordingStarted", null, null);
    }

    /**
     * Halts recording of activations. Closes all involved output streams.
     */
    public void stopRecording() {
        if (valueWriter != null) {
            valueWriter.close();
            valueWriter = null;
        }
        recording = false;
        changeSupport.firePropertyChange("recordingStopped", null, null);
    }

    /**
     * Writes the activations of the network to a file. When
     * startRecording is called, the group checks whether or not the
     * group is entirely populated by spiking neurons. If it is then this
     * methods writes the activations to a file as spike trains in [neuron
     * id][spk time] couplets. Otherwise it writes the neurons' activation
     * values as a state matrix to the file. Flushes the output stream every
     * {@link #FLUSH_FREQUENCY} invocations.
     */
    public void writeActsToFile() {
        try {
            if (writeCounter >= FLUSH_FREQUENCY) {
                valueWriter.flush();
                writeCounter = 0;
            }
            boolean write = false;
            if (recordAsSpikes) {
                int start = 0;
                for (int i = 0, n = size(); i < n; i++) {
                    if (getNeuron(i).isSpike()) {
                        write = true;
                        start = i;
                        break;
                    }
                }
                if (write) {
                    valueWriter.print(this.getParentNetwork().getTime());
                    valueWriter.print(" ");
                    for (int i = start, n = size(); i < n; i++) {
                        if (getNeuron(i).isSpike()) {
                            valueWriter.print(i);
                            valueWriter.print(" ");
                        }
                    }
                    valueWriter.println();
                    writeCounter++;
                }
            } else {
                for (int i = 0, n = size() - 1; i < n; i++) {
                    valueWriter.print(getNeuron(i).getActivation() + ", ");
                }
                valueWriter.print(getNeuron(size() - 1).getActivation());
                valueWriter.println();
                writeCounter++;
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return the name of the neuron update rule used by all the neurons in
     * this group (or mixed if more than one update rule governs the
     * neurons).
     */
    public String getNeuronType() {
        String nType = "Mixed";
        if (size() == 0) {
            return nType;
        }
        Iterator<Neuron> nIter = getNeuronList().iterator();
        NeuronUpdateRule nur = nIter.next().getUpdateRule();
        boolean conflict = false;
        while (nIter.hasNext() && !conflict) {
            conflict = !(nur.getClass().equals(nIter.next().getUpdateRule().getClass()));
        }
        if (conflict) {
            return nType;
        } else {
            return nur.getName();
        }
    }

    /**
     * Set the update rule for the neurons in this group.
     *
     * @param base the neuron update rule to set.
     */
    public void setNeuronType(NeuronUpdateRule base) {
        inputManager.setInputSpikes(base.isSpikingNeuron());
        for (Neuron neuron : getNeuronList()) {
            neuron.setUpdateRule(base.deepCopy());
        }
    }

    /**
     * Set the update rule using a class.
     */
    public void setGroupUpdateRule(UpdateRuleEnum rule) {
        groupUpdateRule = rule;
        try {
            setNeuronType(rule.getRule().getConstructor().newInstance());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }


    /**
     * Set the string update rule for the neurons in this group.
     *
     * @param rule the neuron update rule to set.
     */
    public void setNeuronType(String rule) {
        try {
            NeuronUpdateRule newRule = (NeuronUpdateRule) Class.forName("org.simbrain.network.neuron_update_rules." + rule).newInstance();
            inputManager.setInputSpikes(newRule.isSpikingNeuron());
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        for (Neuron neuron : getNeuronList()) {
            neuron.setUpdateRule(rule);
        }
    }

    /**
     * Return a human-readable name for this type of neuron group. Subclasses
     * should override this. Used in the Gui for various purposes.
     *
     * @return the name of this type of neuron group.
     */
    public String getTypeDescription() {
        return "Neuron Group";
    }

    /**
     * Returns true if the provided synapse is in the fan-in weight vector of
     * some node in this neuron group.
     *
     * @param synapse the synapse to check
     * @return true if it's attached to a neuron in this group
     */
    public boolean inFanInOfSomeNode(final Synapse synapse) {
        boolean ret = false;
        for (Neuron neuron : getNeuronList()) {
            if (neuron.getFanIn().contains(synapse)) {
                ret = true;
            }
        }
        return ret;
    }

    /**
     * Randomize fan-in for all neurons in group.
     */
    public void randomizeIncomingWeights() {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.randomizeFanIn();
        }
        getParentNetwork().fireSynapsesUpdated(getIncomingWeights());
    }

    /**
     * Randomize fan-out for all neurons in group.
     */
    public void randomizeOutgoingWeights() {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.randomizeFanOut();
        }
        getParentNetwork().fireSynapsesUpdated(getOutgoingWeights());
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
     * Add a neuron to group.
     *
     * @param neuron    neuron to add
     * @param fireEvent whether to fire a neuron added event
     */
    public void addNeuron(Neuron neuron, boolean fireEvent) {
        super.addNeuron(neuron);
        neuron.setParentGroup(this);
        if (getParentNetwork() != null) {
            neuron.setId(getParentNetwork().getNeuronIdGenerator().getId());
            if (fireEvent) {
                getParentNetwork().fireNeuronAdded(neuron);
            }
        }
        if (fireEvent) {
            resetSubsamplingIndices();
        }
    }

    @Override
    public void addNeurons(Collection<Neuron> neurons) {
        groupUpdateRule = UpdateRuleEnum.get(neurons.iterator().next().getUpdateRule());
        // TODO: Throw exception if not same type
        super.addNeurons(neurons);
        neurons.forEach(n -> n.setParentGroup(this));
    }



    /**
     * Add neuron to group.
     *
     * @param neuron neuron to add
     */
    public void addNeuron(Neuron neuron) {
        addNeuron(neuron, true);
    }

    /**
     * Removes all neurons with no incoming or outgoing synapses from the group.
     */
    public void prune() {
        Iterator<Neuron> reaper = getNeuronList().iterator();
        while (reaper.hasNext()) {
            Neuron n = reaper.next();
            if (n.getFanIn().size() == 0 && n.getFanOut().size() == 0) {
                reaper.remove();
            }
        }
    }

    @Override
    public String toString() {
        String ret = new String();
        ret += ("Neuron Group [" + getLabel() + "]. Neuron group with " + this.getNeuronList().size() + " neuron(s)" + ". Located at (" + Utils.round(this.getPosition().x, 2) + "," + Utils.round(this.getPosition().y, 2) + ").\n");
        //ret += layout.toString();
        return ret;
    }



    /**
     * Returns an array of spike indices used in couplings, (e.g. to a raster
     * plot). For example, if a neuron group has 9 neurons, and neurons 1 and 4
     * just spiked, the producer will send the list (1,0,0,4,0,0,0,0,0).
     *
     * @return the spike index array
     */
    @Producible()
    public double[] getSpikeIndexes() {
        List<Double> inds = new ArrayList<Double>(size());
        int i = 0;
        for (Neuron n : getNeuronList()) {
            if (n.isSpike()) {
                inds.add((double) i);
            }
            i++;
        }
        double[] vals = new double[inds.size()];
        int j = 0;
        for (Double d : inds) {
            vals[j++] = d;
        }
        return vals;
    }

    /**
     * Return biases as a double array.
     *
     * @return the bias array
     */
    public double[] getBiases() {
        double[] retArray = new double[getNeuronList().size()];
        int i = 0;
        for (Neuron neuron : getNeuronList()) {
            if (neuron.getUpdateRule() instanceof BiasedUpdateRule) {
                retArray[i++] = ((BiasedUpdateRule) neuron.getUpdateRule()).getBias();
            }
        }
        return retArray;
    }

    /**
     * @return the spatial positions of the corners of the neuron group
     * (X and Y only)
     */
    public Point2D[] getFourCorners() {
        double centerX = getCenterX();
        double centerY = getCenterY();

        Point2D[] corners = new Point2D[4];

        corners[0] = new Point2D.Double(getMaxX() - centerX, getMaxY() - centerY);
        corners[3] = new Point2D.Double(getMaxX() - centerX, getMinY() - centerY);
        corners[2] = new Point2D.Double(getMinX() - centerX, getMinY() - centerY);
        corners[1] = new Point2D.Double(getMinX() - centerX, getMaxY() - centerY);

        return corners;
    }

    /**
     * @param x x coordinate for neuron group
     * @param y y coordinate for neuron group
     */
    public void setLocation(final double x, final double y) {
        offset(-this.getMinX(), -this.getMinY());
        offset(x, y);
    }

    @Override
    public Point2D getLocation() {
        return new Point2D.Double(getMinX(), getMinY());
    }

    public void setLocation(Point2D point) {
        setLocation(point.getX(), point.getY());
    }

    @Override
    public String getUpdateMethodDescription() {
        return "Update neurons";
    }

    /**
     * Apply any input values to the activations of the neurons in this group.
     */
    public void applyInputs() {
        for (Neuron neuron : getNeuronList()) {
            neuron.setActivation(neuron.getActivation() + neuron.getInputValue());
        }
    }

    public Layout getLayout() {
        return layout.getLayout();
    }

    public Layout.LayoutObject getLayoutObject() {
        return layout;
    }

    public void setLayoutObject(Layout.LayoutObject layoutObject) {
        this.layout = layoutObject;
    }

    public void setLayout(Layout layout) {
        this.layout.setLayout(layout);
    }

    public void setXYZCoordinatesFromFile(String filename) {
        try (Scanner rowSc = new Scanner(new File(filename));) {
            Scanner colSc = null;
            int i = 0;
            int j;
            try {
                while (rowSc.hasNextLine()) {
                    colSc = new Scanner(rowSc.nextLine());
                    colSc.useDelimiter(", *");
                    j = 0;
                    while (colSc.hasNext()) {
                        double coordinate = colSc.nextDouble();
                        if (i == 0) {
                            getNeuron(j++).setX(coordinate);
                        } else if (i == 1) {
                            getNeuron(j++).setY(coordinate);
                        } else if (i == 2) {
                            getNeuron(j++).setZ(coordinate);
                        } else {
                            return;
                        }
                    }
                    i++;
                    colSc.close();
                }
            } finally {
                if (colSc != null) {
                    colSc.close();
                }
            }
        } catch (IOException ie) {
            ie.printStackTrace();
            return;
        }
    }

    /**
     * Apply this group's layout to its neurons.
     */
    public void applyLayout() {
        layout.getLayout().layoutNeurons(getNeuronList());
        firePositionChanged();
    }

    /**
     * Apply this group's layout to its neurons based on a specified initial
     * position.
     *
     * @param initialPosition the position from which to begin the layout.
     */
    public void applyLayout(Point2D initialPosition) {
        layout.getLayout().setInitialLocation(initialPosition);
        layout.getLayout().layoutNeurons(getNeuronList());
        firePositionChanged();
    }

    public HashSet<SynapseGroup> getIncomingSgs() {
        return new HashSet<SynapseGroup>(incomingSgs);
    }

    public HashSet<SynapseGroup> getOutgoingSg() {
        return new HashSet<SynapseGroup>(outgoingSgs);
    }

    public boolean containsAsIncoming(SynapseGroup sg) {
        return incomingSgs.contains(sg);
    }

    public boolean containsAsOutgoing(SynapseGroup sg) {
        return outgoingSgs.contains(sg);
    }

    public void addIncomingSg(SynapseGroup sg) {
        incomingSgs.add(sg);
    }

    public void addOutgoingSg(SynapseGroup sg) {
        outgoingSgs.add(sg);
    }

    public boolean removeIncomingSg(SynapseGroup sg) {
        return incomingSgs.remove(sg);
    }

    public boolean removeOutgoingSg(SynapseGroup sg) {
        return outgoingSgs.remove(sg);
    }

    //TODO: Replace these with get/set to input manager
    public double[][] getTestData() {
        return inputManager.getData();
    }
    public void setTestData(double[][] testData) throws IllegalArgumentException {
        inputManager.setData(testData);
    }

    /**
     * If more than gridThreshold neurons use a grid layout, else a horizontal
     * line layout.
     */
    public void setLayoutBasedOnSize() {
        setLayoutBasedOnSize(new Point2D.Double(0, 0));
    }

    /**
     * If more than gridThreshold neurons use a grid layout, else a horizontal
     * line layout.
     *
     * @param initialPosition the initial Position for the layout
     */
    public void setLayoutBasedOnSize(Point2D initialPosition) {
        if (initialPosition == null) {
            initialPosition = new Point2D.Double(0, 0);
        }
        LineLayout lineLayout = new LineLayout(betweenNeuronInterval, LineOrientation.HORIZONTAL);
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

    /**
     * @return the betweenNeuronInterval
     */
    public int getBetweenNeuronInterval() {
        return betweenNeuronInterval;
    }

    /**
     * @param betweenNeuronInterval the betweenNeuronInterval to set
     */
    public void setBetweenNeuronInterval(int betweenNeuronInterval) {
        this.betweenNeuronInterval = betweenNeuronInterval;
    }

    /**
     * @return the gridThreshold
     */
    public int getGridThreshold() {
        return gridThreshold;
    }

    /**
     * @param gridThreshold the gridThreshold to set
     */
    public void setGridThreshold(int gridThreshold) {
        this.gridThreshold = gridThreshold;
    }

    public boolean isRecording() {
        return recording;
    }

    public void setRecordAsSpikes(boolean recordAsSpikes) {
        this.recordAsSpikes = recordAsSpikes;
    }

    public boolean isRecordAsSpikes() {
        return recordAsSpikes;
    }

    public boolean isInputMode() {
        return inputMode;
    }


    // TODO
    public boolean isSpikingNeuronGroup() {
        return inputManager.isInputSpikes();
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
        if (numSubSamples >= getNeuronList().size()) {
            return getActivations();
        }

        if (subSampledValues == null || subSampledValues.length != numSubSamples) {
            subSampledValues = new double[numSubSamples];
        }
        for (int ii = 0; ii < numSubSamples; ii++) {
            subSampledValues[ii] = getNeuron(subsamplingIndices[ii]).getActivation();
        }
        return subSampledValues;
    }

    public int getNumSubSamples() {
        return numSubSamples;
    }

    public void setNumSubSamples(int _numSubSamples) {
       double [] newSubSamples = new double[_numSubSamples];
       int len = _numSubSamples > subsamplingIndices.length ? subsamplingIndices.length : _numSubSamples;
       System.arraycopy(subSampledValues, 0, newSubSamples, 0, len);
       subSampledValues = newSubSamples;
       this.numSubSamples = _numSubSamples;
    }

    @Override
    public EditableObject copy() {
        return this.deepCopy(this.getParentNetwork());
    }


    /**
     * Reset the indices used for subsampling.
     */
    public void resetSubsamplingIndices() {
        if (getNeuronList() != null) {
            subsamplingIndices = SimbrainMath.randPermute(0, getNeuronList().size());
        }
    }
    /**
     * Helper class for creating new neuron groups using {@link org.simbrain.util.propertyeditor.AnnotatedPropertyEditor}.
     */
    public static class NeuronGroupCreator implements EditableObject {

        @UserParameter(label = "Number of neurons", description = "How many neurons this neuron group should have", order = -1)
        int numNeurons = 20;

        /**
         * A label for this Neuron Group for display purposes.
         */
        @UserParameter(
                label = "Label",
                initialValueMethod = "getLabel"
        )
        private String label;

        /**
         * Initial update rule
         */
        @UserParameter(label = "Update Rule")
        private UpdateRuleEnum updateRule = UpdateRuleEnum.LINEAR;

        /**
         * Create the template with a proposed label
         */
        public NeuronGroupCreator(String proposedLabel) {
            this.label = proposedLabel;
        }

        /**
         * Add a neuron array to network created from field values which should be setup by an Annotated Property
         * Editor.
         *
         * @param network the network this neuron array adds to
         *
         * @return the created neuron array
         */
        public NeuronGroup create(Network network) {
            NeuronGroup ng = new NeuronGroup(network, numNeurons,
                    network.getArrayIdGenerator().getId());
            ng.setLabel(label);
            ng.setGroupUpdateRule(updateRule);
            ng.applyLayout();
            return ng;
        }

        /**
         * Getter called by reflection by {@link UserParameter#initialValueMethod}
         */
        public String getLabel() {
            return label;
        }

        @Override
        public String getName() {
            return "Neuron Group";
        }
    }

}
