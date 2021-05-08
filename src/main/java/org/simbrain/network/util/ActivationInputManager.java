package org.simbrain.network.util;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.AbstractNeuronCollection;
import org.simbrain.network.groups.NeuronGroup;

/**
 * Manages sending inputs to an {@link AbstractNeuronCollection}. Stores a matrix values that can be
 * edited using a {@link org.simbrain.util.table.NumericTable}.
 */
public class ActivationInputManager {

    /**
     * Data (input vectors) for testing the network.
     */
    private double[][] data;

    /**
     * The current row of the test data
     */
    private int inputIndex = 0;

    /**
     * Used when reading inputs from an input file, so it knows how to parse
     * the file. Normally we have a csv whose rows are activation vectors. Here
     * we have a set of spike times and neuron indices as tuples.
     */
    // TODO: For now not used for collections; Discuss with Zoe.  Also ASDF?
    private boolean inputSpikes = false;

    /**
     * The collection whose inputs should be set.
     */
    private final AbstractNeuronCollection pc;

    /**
     * Construct the input manager
     */
    public ActivationInputManager(AbstractNeuronCollection pc) {
        this.pc = pc;
    }

    /**
     * Apply current row of values to {@link #pc} using {@link AbstractNeuronCollection#forceSetActivations(double[])}}},
     * and iterate current row.
     */
    public void applyCurrentRow() {
        if (inputIndex >= data.length) {
            inputIndex = 0;
        }
        if (inputSpikes) {
            pc.addInputs(data[inputIndex]);
            for (int i = 0; i < pc.getNeuronList().size(); i++) {
                pc.getNeuron(i).update();
            }
        } else {
            pc.forceSetActivations(data[inputIndex]);
        }
        inputIndex++;
    }

    public void setData(double[][] data) {
        for (int i = 0; i < data.length; i++) {
            if (data[i].length != pc.size()) {
                if (i == 0) {
                    throw new IllegalArgumentException("Data Inconsistency:" + " Test data does not have a column number equal" + " to the number of neurons in the group.");
                } else {
                    throw new IllegalArgumentException("Data Inconsistency:" + " Test data does not have equal column lengths.");
                }
            }
        }
        if (pc instanceof NeuronGroup) {
            testAndSetIfSpiking();
        }
        this.data = data;
    }

    /**
     * Tests if this neuron group can be considered a spiking neuron group
     * and sets that value to true/false acordingly.
     */
    public void testAndSetIfSpiking() {
        boolean spiking = true;
        for (Neuron n : pc.getNeuronList()) {
            if (!n.getUpdateRule().isSpikingNeuron()) {
                spiking = false;
                break;
            }
        }
        inputSpikes = spiking;
    }

    public double[][] getData() {
        return data;
    }

    public void setInputSpikes(boolean inputSpikes) {
        this.inputSpikes = inputSpikes;
    }

    public boolean isInputSpikes() {
        return inputSpikes;
    }
}
