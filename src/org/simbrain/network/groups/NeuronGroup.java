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

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.layouts.Layout;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.network.layouts.LineLayout.LineOrientation;
import org.simbrain.network.neuron_update_rules.interfaces.BiasedUpdateRule;
import org.simbrain.util.Utils;
import org.simbrain.util.math.SimbrainMath;

/**
 * A group of neurons. A primary abstraction for larger network structures.
 * Layers in feed-forward networks are neuron groups. Self-organizing-maps
 * subclass this class. Etc.
 */
public class NeuronGroup extends Group implements CopyableGroup<NeuronGroup> {

    // TODO: If 3.x is developed and neurongroup sticks around:
    //  Add prototype neuron as in synapse group
    //  Add group level polarity
    //  Fix isSpiking

    /**
     * The default for how often {@link #writeActsToFile()} should flush
     * the output stream when writing to a file.
     */
    public static final int FLUSH_FREQUENCY = 1000;

    /**
     * The number of neurons in the group by default.
     */
    public static final int DEFAULT_GROUP_SIZE = 10;

    /** The description of the update rule governing the group. */
    private String updateRule;
 
    /**
     * Mostly here for backwards compatibility {@link #recordAsSpikes} is
     * ultimately the more important variable.
     */
    private boolean isSpikingNeuronGroup = false;

    /** The neurons in this group. */
    private List<Neuron> neuronList = new ArrayList<Neuron>(500);

    /** Default layout for neuron groups. */
    public static final Layout DEFAULT_LAYOUT = new LineLayout(50,
            LineOrientation.HORIZONTAL);

    /** The layout for the neurons in this group. */
    private Layout layout = DEFAULT_LAYOUT;

    /** Set of incoming synapse groups. */
    private final HashSet<SynapseGroup> incomingSgs =
            new HashSet<SynapseGroup>();

    /** Set of outgoing synapse groups. */
    private final HashSet<SynapseGroup> outgoingSgs =
            new HashSet<SynapseGroup>();

    /**
     * In method setLayoutBasedOnSize, this is used as the threshold number of
     * neurons in the group, above which to use grid layout instead of line
     * layout.
     */
    private int gridThreshold = 10;

    /** Space between neurons within a layer. */
    private int betweenNeuronInterval = 50;

    /** Data (input vectors) for testing the network. */
    private double[][] testData;

    /**
     * Whether or not {@link #writeActsToFile()} will write activations as a
     * state matrix or a spike train.
     */
    private boolean recordAsSpikes;

    /** The output stream which writes activation values to a file.*/
    private PrintWriter valueWriter;

    /** Whether or not this group is in a state that allows recording. */
    private boolean recording;

    /**
     * Whether or not this neuron group is in input mode. If the group is in
     * input mode then its update involves either injecting activation or
     * directly setting the activation of the neurons in the group based on
     * the values in test data, ignoring all other inputs.
     */
    private boolean inputMode = false;

    /**
     * The index in test data the neuron group is currently on if
     * {@link #inputMode} is true.
     */
    private int inputIndex = 0;

    /**
     * A counter to keep track of how many times {@link #writeActsToFile()} has
     * been called so as to determine when to flush the output stream.
     */
    private int writeCounter = 0;
    
    /** Indices used with subsampling. */
    private int[] subsamplingIndices;
    
    /**
     * Reset the indices used for subsampling.
     */
    public void resetSubsamplingIndices() {
        if (neuronList != null) {
            subsamplingIndices = SimbrainMath.randPermute(0, neuronList.size());   
        }
    }
    
    /**
     * This used to be how neuron group recordings were labeled. This is now
     * only here for backwards compatibility.
     */
    @Deprecated
    private int fileNum = 0;

    /**
     * Construct a new neuron group from a list of neurons.
     *
     * @param net
     *            the network
     * @param neurons
     *            the neurons
     */
    public NeuronGroup(final Network net, final List<Neuron> neurons) {
        super(net);
        neuronList = new ArrayList<Neuron>(neurons.size());
        for (Neuron neuron : neurons) {
            addNeuron(neuron);
        }
        // Very slow to add to a copy on write array list so do it this way
        neuronList = new CopyOnWriteArrayList<Neuron>(neuronList);
        updateRule = getNeuronType();
        resetSubsamplingIndices();
    }

    /**
     * Construct a new neuron group with a specified number of neurons.
     *
     * @param net
     *            parent network
     * @param numNeurons
     *            how many neurons it will have
     */
    public NeuronGroup(final Network net, final int numNeurons) {
        this(net, new Point2D.Double(0, 0), numNeurons);
    }

    /**
     * Construct a new neuron group with a specified number of neurons.
     *
     * @param net
     *            parent network
     * @param initialPosition
     *            initial location of the group
     * @param numNeurons
     *            how many neurons it will have
     */
    public NeuronGroup(final Network net, Point2D initialPosition,
            final int numNeurons) {
        super(net);
        neuronList = new ArrayList<Neuron>(numNeurons);
        for (int i = 0; i < numNeurons; i++) {
            addNeuron(new Neuron(net), false);
        }
        // Very slow to add to a copy on write array list so do it this way
        neuronList = new CopyOnWriteArrayList<Neuron>(neuronList);
        layout.setInitialLocation(initialPosition);
        layout.layoutNeurons(this.getNeuronList());
        updateRule = getNeuronType();
        resetSubsamplingIndices();
    }

    /**
     * Create a neuron group without any initial neurons and an initial
     * position.
     *
     * @param network
     *            parent network
     * @param initialPosition
     *            the starting position from which to lay-out the neurons in the
     *            group whenever they are added.
     */
    public NeuronGroup(final Network network, Point2D initialPosition) {
        super(network);
        layout.setInitialLocation(initialPosition);
        resetSubsamplingIndices();
    }

    /**
     * Create a neuron group without any initial neurons.
     *
     * @param network
     *            parent network
     */
    public NeuronGroup(final Network network) {
        super(network);
    }

    /**
     * Copy constructor. pass in network for cases where a group is pasted from
     * one network to another
     *
     * @param network
     *            parent network
     * @param toCopy
     *            the neuron group this will become a (deep) copy of.
     */
    public NeuronGroup(final Network network, final NeuronGroup toCopy) {
        super(network);
        for (Neuron neuron : toCopy.getNeuronList()) {
            this.addNeuron(new Neuron(network, neuron), false);
        }
        this.setLabel(toCopy.getLabel());
        this.updateRule = toCopy.updateRule;
        resetSubsamplingIndices();
    }

    @Override
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
        for (Neuron neuron : neuronList) {
            neuron.setParentGroup(null);
            neuron.getNetwork().removeNeuron(neuron);
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
        neuronList.clear();
        Runtime.getRuntime().gc();
    }

    /**
     * Updates all the neurons in the neuron group according to their 
     * NeuronUpdateRule(s). If the group is in input mode reads in the next
     * set of values from the input table and sets the neuron values
     * accordingly.
     */
    @Override
    public void update() {
        if (inputMode) {
            if (testData == null) {
                throw new NullPointerException("Test data variable is null,"
                        + " but neuron group " + getLabel() + " is in input"
                        + " mode.");
            }
            // Surrounded by checks, so actually safe.
            readNextInputUnsafe();
        } else {
            Network.updateNeurons(neuronList);
        }
        if (isRecording()) {
            writeActsToFile();
        }
    }
    
    /**
     * A forwarding method surrounding {@link #readNextInputUnsafe()} in the
     * appropriate checks to make it safe. This allows outside classes to
     * force the neuron group to read in and set activations according to
     * the value(s) in its input table.
     */
    public void readNextInputs() {
        if (inputMode) {
            if (testData == null) {
                throw new NullPointerException("Test data variable is null,"
                        + " but neuron group " + getLabel() + " is in input"
                        + " mode.");
            }
            // Surrounded by checks, so actually safe.
            readNextInputUnsafe();
        } else {
            throw new IllegalStateException("Neuron Group " + getLabel()
                    + " is not in input mode.");
        }
    }
    
    /**
     * If this neuron group has an input table reads in the next entry on the
     *  table. If all inputs have been read this method resets the counter and
     *  starts again from the beginning of the table. 
     *  
     *  For spiking neuron update rules, values read in are treated as current
     *  being injected into the cell, for non-spiking neurons activations are
     *  set immediately to the value at that index in the table.
     *  
     *  This method is unsafe because it does not check if the group is in
     *  input mode or if the input table is non-null. 
     */
    private void readNextInputUnsafe() {
        if (inputIndex >= testData.length) {
            inputIndex = 0;
        }
        if (isSpikingNeuronGroup()) {
            setInputValues(testData[inputIndex]);
            for (int i = 0; i < size(); i++) {
                neuronList.get(i).setToBufferVals();
            }
        } else {
            forceSetActivations(testData[inputIndex]);
        }
        inputIndex++;
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
        for (Neuron n : neuronList) {
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
        this.getParentNetwork().fireGroupParametersChanged(this);
        this.getParentNetwork().fireGroupChanged(this, "Recording Started");
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
        this.getParentNetwork().fireGroupParametersChanged(this);
        this.getParentNetwork().fireGroupChanged(this, "Recording Stopped");
    }

    /**
     * Writes the activations of the network to a file. When
     * {@link #startRecording()} is called, the group checks whether or not the
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
                    if (neuronList.get(i).isSpike()) {
                        write = true;
                        start = i;
                        break;
                    }
                }
                if (write) {
                    valueWriter.print(this.getParentNetwork().getTime());
                    valueWriter.print(" ");
                    for (int i = start, n = size(); i < n; i++) {
                        if (neuronList.get(i).isSpike()) {
                            valueWriter.print(i);
                            valueWriter.print(" ");
                        }
                    }
                    valueWriter.println();
                    writeCounter++;
                }
            } else {
                for (int i = 0, n = size() - 1; i < n; i++) {
                    valueWriter.print(neuronList.get(i).getActivation() + ", ");
                }
                valueWriter.print(neuronList.get(size()-1).getActivation());
                valueWriter.println();
                writeCounter++;
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return the name of the neuron update rule used by all the neurons in
     *         this group (or mixed if more than one update rule governs the
     *         neurons).
     */
    public String getNeuronType() {
        String nType = "Mixed";
        if (size() == 0) {
            return nType;
        }
        Iterator<Neuron> nIter = neuronList.iterator();
        NeuronUpdateRule nur = nIter.next().getUpdateRule();
        boolean conflict = false;
        while (nIter.hasNext() && !conflict) {
            conflict = !(nur.getClass().equals(nIter.next().getUpdateRule()
                    .getClass()));
        }
        if (conflict) {
            return nType;
        } else {
            return nur.getName();
        }
    }

    /**
     * Return the neuron at the specified index of the internal list storing neurons.
     *
     * @param neuronIndex index of the neuron
     * @return the neuron at that index
     */
    public Neuron getNeuron(int neuronIndex) {
    	return neuronList.get(neuronIndex);
    }

    /**
     * @return the neurons in this group.
     */
    public List<Neuron> getNeuronList() {
        return Collections.unmodifiableList(neuronList);
    }

    /**
     * 
     * Returns a modifiable list of neuron list. Modifications made to this list
     * change the group because this is the underlying list of the group.
     * 
     * @return the neurons in this group.
     */
    public List<Neuron> getNeuronListUnsafe() {
        return neuronList;
    }

    /**
     * Set the update rule for the neurons in this group.
     *
     * @param base
     *            the neuron update rule to set.
     */
    public void setNeuronType(NeuronUpdateRule base) {
        isSpikingNeuronGroup = base.isSpikingNeuron();
        for (Neuron neuron : neuronList) {
            neuron.setUpdateRule(base.deepCopy());
        }
    }

    /**
     * Set the string update rule for the neurons in this group.
     *
     * @param rule
     *            the neuron update rule to set.
     */
    public void setNeuronType(String rule) {
        try {
            NeuronUpdateRule newRule = (NeuronUpdateRule) Class.forName(
                    "org.simbrain.network.neuron_update_rules." + rule)
                    .newInstance();
            isSpikingNeuronGroup = newRule.isSpikingNeuron();
        } catch (InstantiationException | IllegalAccessException
                | ClassNotFoundException e)
        {
            e.printStackTrace();
        }
        for (Neuron neuron : neuronList) {
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
     * @param synapse
     *            the synapse to check
     * @return true if it's attached to a neuron in this group
     */
    public boolean inFanInOfSomeNode(final Synapse synapse) {
        boolean ret = false;
        for (Neuron neuron : neuronList) {
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
     * @param lower
     *            lower bound for randomization.
     * @param upper
     *            upper bound for randomization.
     */
    public void randomizeBiases(double lower, double upper) {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.randomizeBias(lower, upper);
        }
    }

    /**
     * Add a neuron to group.
     *
     * @param neuron
     *            neuron to add
     * @param fireEvent
     *            whether to fire a neuron added event
     */
    public void addNeuron(Neuron neuron, boolean fireEvent) {
        neuronList.add(neuron);
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

    /**
     * Add neuron to group.
     *
     * @param neuron
     *            neuron to add
     */
    public void addNeuron(Neuron neuron) {
        addNeuron(neuron, true);
    }

    /**
     * Delete the provided neuron.
     *
     * @param toDelete
     *            the neuron to delete
     */
    public void removeNeuron(Neuron toDelete) {
        neuronList.remove(toDelete);
        if (isEmpty()) {
            delete();
        }
        resetSubsamplingIndices();
    }

    /**
     * Removes all neurons with no incoming or outgoing synapses from the group.
     */
    public void prune() {
        Iterator<Neuron> reaper = neuronList.iterator();
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
        ret += ("Neuron Group [" + getLabel() + "]. Neuron group with "
                + this.getNeuronList().size() + " neuron(s)" + ". Located at ("
                + Utils.round(this.getPosition().x, 2) + ","
                + Utils.round(this.getPosition().y, 2) + ").\n");
        ret += layout.toString();
        return ret;
    }

    @Override
    public boolean isEmpty() {
        return neuronList.isEmpty();
    }

    /**
     * Set input values of neurons using an array of doubles. Assumes the order
     * of the items in the array matches the order of items in the neuronlist.
     *
     * Does not throw an exception if the provided input array and neuron list
     * do not match in size.
     *
     * @param inputs
     *            the input vector as a double array.
     */
    public void setInputValues(double[] inputs) {
        for (int i = 0, n = size(); i < n; i++) {
            if (i >= inputs.length) {
                break;
            }
            neuronList.get(i).setInputValue(inputs[i]);
        }
    }
    
    /**
     * Set activations of neurons using an array of doubles. Assumes the order
     * of the items in the array matches the order of items in the neuronlist.
     *
     * Does not throw an exception if the provided input array and neuron list
     * do not match in size.
     *
     * @param inputs
     *            the input vector as a double array.
     */
    public void setActivations(double[] inputs) {
        for (int i = 0, n = size(); i < n; i++) {
            if (i >= inputs.length) {
                break;
            }
            neuronList.get(i).setActivation(inputs[i]);
        }
    }

    /**
     * Force set activations of neurons using an array of doubles. Assumes the
     * order of the items in the array should match the order of items in the
     * neuronlist.
     *
     * Does not throw an exception if the provided input array and neuron list
     * do not match in size.
     *
     * @param inputs
     *            the input vector as a double array.
     */
    public void forceSetActivations(double[] inputs) {
        for (int i = 0, n = size(); i < n; i++) {
            if (i >= inputs.length) {
                break;
            }
            neuronList.get(i).forceSetActivation(inputs[i]);
        }
    }

    /**
     * Return activations as a double array.
     *
     * @return the activation array
     */
    public double[] getActivations() {
        double[] retArray = new double[neuronList.size()];
        int i = 0;
        for (Neuron neuron : neuronList) {
            retArray[i++] = neuron.getActivation();
        }
        return retArray;
    }
    
    /**
     * Returns an array of spike indices used in couplings, (e.g. to a raster
     * plot). For example, if a neuron group has 9 neurons, and neurons 1 and 4
     * just spiked, the producer will send the list (1,0,0,4,0,0,0,0,0).
     * 
     * @return the spike index array
     */
    public double[] getSpikeIndexes() {
        List<Double> inds = new ArrayList<Double>(size());
        int i = 0;
        for (Neuron n : neuronList) {
            if (n.isSpike()) {
                inds.add((double) i);
            }
            i++;
        }
        double [] vals = new double[inds.size()];
        int j = 0;
        for (Double d : inds) {
            vals[j++] = d.doubleValue();
        }
        return vals;
    }

    // public byte [] getSpikes() {
    // if (!isSpikingNeuronGroup) {
    // return null;
    // }
    // int numBytes = size() % 8 == 0 ? size()/8 : size()/8 + 1;
    // byte [] actArr = new byte[numBytes];
    // int actInt = 0;
    // for (int i = 0; i < size(); i++) {
    // if (((SpikingNeuronUpdateRule) neuronList.get(i).getUpdateRule())
    // .hasSpiked()) {
    // actInt = actInt | (1 << (i % 32));
    // }
    // }
    //
    // }

    /**
     * Return biases as a double array.
     *
     * @return the bias array
     */
    public double[] getBiases() {
        double[] retArray = new double[neuronList.size()];
        int i = 0;
        for (Neuron neuron : neuronList) {
            if (neuron.getUpdateRule() instanceof BiasedUpdateRule) {
                retArray[i++] = ((BiasedUpdateRule) neuron.getUpdateRule())
                        .getBias();
            }
        }
        return retArray;
    }

    /**
     * True if the group contains the specified neuron.
     *
     * @param n
     *            neuron to check for.
     * @return true if the group contains this neuron, false otherwise
     */
    public boolean containsNeuron(final Neuron n) {
        return neuronList.contains(n);
    }

    /**
     * @return the number of neurons in the group
     */
    @Override
    public int size() {
        return neuronList.size();
    }

    /**
     * Returns all the neurons in this group within a certain radius of the
     * given neuron. This method will never return the given neuron as part
     * of the list of neurons within the given radius, nor will it return
     * neurons with the exact same position as the given neuron as a part 
     * of the returned list.
     * 
     * @param n the neurons
     * @param radius the radius to search within.
     * @return neurons in the group within a certain radius
     */
    public List<Neuron> getNeuronsInRadius(Neuron n, int radius) {
        ArrayList<Neuron> ret = new ArrayList<Neuron>((int)(size()/0.75f));
        for (Neuron potN : neuronList) {
            double dist = Network.getEuclideanDist(n, potN); 
            if (dist <= radius && dist != 0) {
                ret.add(potN);
            }
         }
        return ret;
    }
    
    // TODO: Below don't take account of the actual width of neurons themselves.
    // Treats them as points.
    
    /**
     * Get the central x coordinate of this group, based on the positions of the
     * neurons that comprise it.
     *
     * @return the center x coordinate.
     */
    public double getCenterX() {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (Neuron neuron : neuronList) {
            if (neuron.getX() < min) {
                min = neuron.getX();
            }
            if (neuron.getX() > max) {
                max = neuron.getX();
            }
        }
        return min + (max - min) / 2;
    }

    /**
     * Get the central y coordinate of this group, based on the positions of the
     * neurons that comprise it.
     *
     * @return the center y coordinate.
     */
    public double getCenterY() {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (Neuron neuron : neuronList) {
            if (neuron.getY() < min) {
                min = neuron.getY();
            }
            if (neuron.getY() > max) {
                max = neuron.getY();
            }
        }
        return min + (max - min) / 2;
    }

    /**
     * Returns the maximum X position of this group based on the neurons that
     * comprise it.
     *
     * @return the x position of the farthest right neuron in the group.
     */
    public double getMaxX() {
        double max = Double.NEGATIVE_INFINITY;
        for (Neuron neuron : neuronList) {
            if (neuron.getX() > max) {
                max = neuron.getX();
            }
        }
        return max;
    }

    /**
     * Returns the minimum X position of this group based on the neurons that
     * comprise it.
     *
     * @return the x position of the farthest left neuron in the group.
     */
    public double getMinX() {
        double min = Double.POSITIVE_INFINITY;
        for (Neuron neuron : neuronList) {
            if (neuron.getX() < min) {
                min = neuron.getX();
            }
        }
        return min;
    }

    /**
     * Returns the maximum Y position of this group based on the neurons that
     * comprise it.
     *
     * @return the y position of the farthest north neuron in the group.
     */
    public double getMaxY() {
        double max = Double.NEGATIVE_INFINITY;
        for (Neuron neuron : neuronList) {
            if (neuron.getY() > max) {
                max = neuron.getY();
            }
        }
        return max;
    }

    /**
     * Returns the minimum Y position of this group based on the neurons that
     * comprise it.
     *
     * @return the y position of the farthest south neuron in the group.
     */
    public double getMinY() {
        double min = Double.POSITIVE_INFINITY;
        for (Neuron neuron : neuronList) {
            if (neuron.getY() < min) {
                min = neuron.getY();
            }
        }
        return min;
    }

    /**
     * Return the width of this group, based on the positions of the neurons
     * that comprise it.
     *
     * @return the width of the group
     */
    public double getWidth() {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (Neuron neuron : neuronList) {
            if (neuron.getX() < min) {
                min = neuron.getX();
            }
            if (neuron.getX() > max) {
                max = neuron.getX();
            }
        }
        return max - min;
    }

    /**
     * Return the height of this group, based on the positions of the neurons
     * that comprise it.
     *
     * @return the height of the group
     */
    public double getHeight() {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (Neuron neuron : neuronList) {
            if (neuron.getY() < min) {
                min = neuron.getY();
            }
            if (neuron.getY() > max) {
                max = neuron.getY();
            }
        }
        return max - min;
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
     * @return the spatial positions of the corners of the neuron group
     *  (X and Y only)
     */
    public Point2D[] getFourCorners() {
        double centerX = getCenterX();
        double centerY = getCenterY();

        Point2D[] corners = new Point2D[4];

        corners[0] = new Point2D.Double(getMaxX() - centerX, getMaxY()
                - centerY);
        corners[3] = new Point2D.Double(getMaxX() - centerX, getMinY()
                - centerY);
        corners[2] = new Point2D.Double(getMinX() - centerX, getMinY()
                - centerY);
        corners[1] = new Point2D.Double(getMinX() - centerX, getMaxY()
                - centerY);

        return corners;
    }

    /**
     *
     * @param x
     *            x coordinate for neuron group
     * @param y
     *            y coordinate for neuron group
     */
    public void setLocation(final double x, final double y) {
        offset(-this.getMinX(), -this.getMinY());
        offset(x, y);
    }

    /**
     * Translate all neurons (the only objects with position information).
     *
     * @param offsetX
     *            x offset for translation.
     * @param offsetY
     *            y offset for translation.
     */
    public void offset(final double offsetX, final double offsetY) {
        for (Neuron neuron : neuronList) {
            neuron.setX(neuron.getX() + offsetX);
            neuron.setY(neuron.getY() + offsetY);
        }
    }

    /**
     * Set all activations to 0.
     */
    public void clearActivations() {
        for (Neuron n : this.getNeuronList()) {
            n.clear();
        }
    }

    /**
     * Set clamping on all neurons in this group.
     *
     * @param clamp
     *            true to clamp them, false otherwise
     */
    public void setClamped(final boolean clamp) {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.setClamped(clamp);
        }
    }

    /**
     * Set all activations to a specified value.
     *
     * @param value
     *            the value to set the neurons to
     */
    public void setActivationLevels(final double value) {
        for (Neuron n : getNeuronList()) {
            n.setActivation(value);
        }
    }

    /**
     * Force set all activations to a specified value.
     *
     * @param value
     *            the value to set the neurons to
     */
    public void forceSetActivationLevels(final double value) {
        for (Neuron n : getNeuronList()) {
            n.forceSetActivation(value);
        }
    }

    /**
     * Copy activations from one neuron group to this one.
     *
     * @param toCopy
     *            the group to copy activations from.
     */
    public void copyActivations(NeuronGroup toCopy) {
        int i = 0;
        for (Neuron neuron : toCopy.getNeuronList()) {
            if (i < neuronList.size()) {
                neuronList.get(i).setActivation(
                        neuron.getInputValue() + neuron.getActivation());
                neuronList.get(i++).setSpike(neuron.isSpike());

            }
        }
    }

    /**
     * Print activations as a vector.
     */
    public void printActivations() {
        System.out.println(Utils.doubleArrayToString(Network
                .getActivationVector(neuronList)));
    }

    @Override
    public String getUpdateMethodDesecription() {
        return "Update neurons";
    }

    /**
     * Apply any input values to the activations of the neurons in this group.
     */
    public void applyInputs() {
        for (Neuron neuron : getNeuronList()) {
            neuron.setActivation(neuron.getActivation()
                    + neuron.getInputValue());
        }
    }

    /**
     * @return the layout
     */
    public Layout getLayout() {
        return layout;
    }

    /**
     * Set the layout. Does not apply it. Call apply layout for that.
     *
     * @param layout
     *            the layout to set
     */
    public void setLayout(Layout layout) {
        this.layout = layout;
    }

    /**
     * Return current position (upper left corner of neuron in the farthest
     * north-west position.
     *
     * @return position upper left position of group
     */
    public Point2D.Double getPosition() {
        return new Point2D.Double(getMinX(), getMinY());
    }

    public void setXYZCoordinatesFromFile(String filename) {
        try(Scanner rowSc = new Scanner(new File(filename));) {
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
                            neuronList.get(j++).setX(coordinate);
                        } else if (i == 1) {
                            neuronList.get(j++).setY(coordinate);
                        } else if (i == 2) {
                            neuronList.get(j++).setZ(coordinate);
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
        layout.setInitialLocation(getPosition());
        layout.layoutNeurons(getNeuronList());
    }

    /**
     * Apply this group's layout to its neurons based on a specified initial
     * position.
     *
     * @param initialPosition
     *            the position from which to begin the layout.
     */
    public void applyLayout(Point2D initialPosition) {
        layout.setInitialLocation(initialPosition);
        layout.layoutNeurons(getNeuronList());
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
     * @param lb
     *            the lower bound to set.
     */
    public void setLowerBound(double lb) {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.setLowerBound(lb);
        }
    }

    /**
     * Set the upper bound on all neurons in this group.
     *
     * @param ub
     *            the upper bound to set.
     */
    public void setUpperBound(double ub) {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.setUpperBound(ub);
        }
    }

    /**
     * Set the increment on all neurons in this group.
     *
     * @param increment
     *            the increment to set.
     */
    public void setIncrement(double increment) {
        for (Neuron neuron : this.getNeuronList()) {
            neuron.setIncrement(increment);
        }
    }

    /**
     * @return the testData
     */
    public double[][] getTestData() {
        return testData;
    }

    /**
     * @param testData
     *            the testData to set
     * @exception IllegalArgumentException
     */
    public void setTestData(double[][] testData)
            throws IllegalArgumentException {
        for (int i = 0; i < testData.length; i++) {
            if (testData[i].length != size()) {
                if (i == 0) {
                    throw new IllegalArgumentException("Data Inconsistency:"
                            + " Test data does not have a column number equal"
                            + " to the number of neurons in the group.");
                } else {
                    throw new IllegalArgumentException("Data Inconsistency:"
                            + " Test data does not have equal column lengths.");
                }
            }
        }
        testAndSetIfSpiking();
        this.testData = testData;
    }

    /**
     * Tests if this neuron group can be considered a spiking neuron group
     * and sets that value to true/false acordingly.
     */
    public void testAndSetIfSpiking() {
        boolean spiking = true;
        for (Neuron n : neuronList) {
            if (!n.getUpdateRule().isSpikingNeuron()) {
                spiking = false;
                break;
            }
        }
        setSpikingNeuronGroup(spiking);
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
     * @param initialPosition
     *            the initial Position for the layout
     */
    public void setLayoutBasedOnSize(Point2D initialPosition) {
        if (initialPosition == null) {
            initialPosition = new Point2D.Double(0, 0);
        }
        LineLayout lineLayout = new LineLayout(betweenNeuronInterval,
                LineOrientation.HORIZONTAL);
        GridLayout gridLayout = new GridLayout(betweenNeuronInterval,
                betweenNeuronInterval);
        if (neuronList.size() < gridThreshold) {
            lineLayout.setInitialLocation(initialPosition);
            setLayout(lineLayout);
        } else {
            gridLayout.setInitialLocation(initialPosition);
            setLayout(gridLayout);
        }
        // Used rather than apply layout to make sure initial position is used.
        getLayout().layoutNeurons(neuronList);
    }

    /**
     * @return the betweenNeuronInterval
     */
    public int getBetweenNeuronInterval() {
        return betweenNeuronInterval;
    }

    /**
     * @param betweenNeuronInterval
     *            the betweenNeuronInterval to set
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
     * @param gridThreshold
     *            the gridThreshold to set
     */
    public void setGridThreshold(int gridThreshold) {
        this.gridThreshold = gridThreshold;
    }

    /**
     * Clear the neuron list.
     */
    public void clearNeuronList() {
        neuronList.clear();
    }

    /**
     * Utility to method (used in couplings) to get a string showing the labels
     * of all "active" neurons (neurons with activation above a threshold).
     *
     * @param threshold
     *            threshold above which to consider a neuron "active"
     * @return the "active labels"
     */
    public String getLabelsOfActiveNeurons(double threshold) {
        StringBuilder strBuilder = new StringBuilder("");
        for (Neuron neuron : neuronList) {
            if ((neuron.getActivation() > threshold)
                    && (!neuron.getLabel().isEmpty())) {
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
            if ((neuron.getActivation() > min)
                    && (!neuron.getLabel().isEmpty())) {
                result = neuron.getLabel();
                min = neuron.getActivation();
            }
        }
        return result + " ";
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

    /**
     * Sets whether or not this neuron group is in input mode. When in input
     * mode the neuron group will draw activations from its {@link #testData}
     *  field instead of from any impinging synapses or its own neuron update
     *  functions. This function removes the neurons from the neuron set in
     *  ConcurrentBufferedUpdate, preventing it from updating the neurons in
     *  this group, and re-adds those neurons when input mode is turned off.
     *  Thus the update action associated with this neuron group MUST be added
     *  to the network update sequence even if ParallelBufferedUpdate is
     *  selected in order for input values to update the group properly.
     * @param inputMode whether or not this group will run in input mode during
     * network and workspace updates.
     * @throws IllegalArgumentException if input mode is set to true, but the
     * {@link #testData} field is set to null.
     */
    public void setInputMode(boolean inputMode)
            throws IllegalArgumentException {
        if (testData == null && inputMode) {
            throw new IllegalArgumentException("Cannot set input mode to true"
                    + " if there is no input data stored in NeuronGroup field:"
                    + " testData");
        }
        this.inputMode = inputMode;
        this.getParentNetwork().fireGroupChanged(this,
                getLabel() + " input mode " + inputMode);
    }

    public boolean isSpikingNeuronGroup() {
        return isSpikingNeuronGroup;
    }

    public void setSpikingNeuronGroup(boolean isSpikingNeuronGroup) {
        this.isSpikingNeuronGroup = isSpikingNeuronGroup;
    }

    /**
     * Whether to use subsampling (for large neuron groups, only using a sample
     * of activations for external components).
     */
    private static boolean useSubSampling = true;

    /**
     * Number of subsamples to take. This value is also implicitly a threshold.
     * If a neuron group has more than this many neurons, and subsampling is
     * turned on, a vector with this many components is returned by (
     * {@link getExternalActivations}
     */
    private static int numSubSamples = 100;

    /**
     * Returns a vector of activations to be used by some object external to the
     * neuron group. If subsampling is turned on only some sample of these
     * activations will be returned. Thus if plotting activations of a thousand
     * node network, a sample of 100 activations might be returned.
     *
     * @return the vector of external activations.
     */
    public double[] getExternalActivations() {
        if (!useSubSampling) {
            return getActivations();
        }
        if (neuronList.size() < numSubSamples) {
            return getActivations();
        } else {
            double[] retArray = new double[numSubSamples];
            for (int i = 0; i < numSubSamples; i++) {
                retArray[i] = neuronList.get(i).getActivation();
            }
            return retArray;            
        }
    }

    /**
     * @return the useSubSampling
     */
    public static boolean isUseSubSampling() {
        return useSubSampling;
    }

    /**
     * @param useSubSampling the useSubSampling to set
     */
    public static void setUseSubSampling(boolean useSubSampling) {
        NeuronGroup.useSubSampling = useSubSampling;
    }

    /**
     * @return the numSubSamples
     */
    public static int getNumSubSamples() {
        return numSubSamples;
    }

    /**
     * @param numSubSamples the numSubSamples to set
     */
    public static void setNumSubSamples(int numSubSamples) {
        NeuronGroup.numSubSamples = numSubSamples;
    }
    
    /**
     * Get the neuron with the specified label, or null if none found.
     *
     * @param label
     *            label to search for
     * @return the associated neuron
     */
    public Neuron getNeuronByLabel(String label) {
        // TODO: Share code with Network level method with same name.
        for (Neuron neuron : this.getNeuronList()) {
            if (neuron.getLabel().equalsIgnoreCase(label)) {
                return neuron;
            }
        }
        return null;
    }
    
    
    /**
     * A hack to guess that the neuron group is spiking and thus that spike
     * indices menu should be shown.   The isSpikingNeuronGroup field is 
     * not currently being maintained, though we may bring it back.
     *
     * @return true if the first node in the group is spiking, false otherwise.
     */
    public boolean isSpiking2() {
        if (neuronList.size() > 0) {
            if(neuronList.get(0).getUpdateRule().isSpikingNeuron()) {
                return true;
            }
        }
        return false;
    }
    
}
