package org.simbrain.network.util;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.AbstractNeuronCollection;
import org.simbrain.network.groups.NeuronGroup;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Manages recording activations or spike histories.
 */
public class ActivationRecorder {

    /**
     * The default for how often {@link #writeActsToFile()} should flush
     * the output stream when writing to a file.
     */
    public static final int FLUSH_FREQUENCY = 1000;

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
     * The collection to be subsampled from.
     */
    private final AbstractNeuronCollection nc;

    /**
     * Construct activaiton recorder.
     */
    public ActivationRecorder(AbstractNeuronCollection parentCollection) {
        this.nc = parentCollection;
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
        for (Neuron n : nc.getNeuronList()) {
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

        nc.fireRecordingStarted();
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
        nc.fireRecordingStopped();
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
                for (int i = 0, n = nc.size(); i < n; i++) {
                    if (nc.getNeuron(i).isSpike()) {
                        write = true;
                        start = i;
                        break;
                    }
                }
                if (write) {
                    valueWriter.print(nc.getParentNetwork().getTime());
                    valueWriter.print(" ");
                    for (int i = start, n = nc.size(); i < n; i++) {
                        if (nc.getNeuron(i).isSpike()) {
                            valueWriter.print(i);
                            valueWriter.print(" ");
                        }
                    }
                    valueWriter.println();
                    writeCounter++;
                }
            } else {
                for (int i = 0, n = nc.size() - 1; i < n; i++) {
                    valueWriter.print(nc.getNeuron(i).getActivation() + ", ");
                }
                valueWriter.print(nc.getNeuron(nc.size() - 1).getActivation());
                valueWriter.println();
                writeCounter++;
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public boolean isRecording() {
        return recording;
    }

    public void setRecording(boolean recording) {
        this.recording = recording;
    }

    public boolean isRecordAsSpikes() {
        return recordAsSpikes;
    }

    public void setRecordAsSpikes(boolean recordAsSpikes) {
        this.recordAsSpikes = recordAsSpikes;
    }
}
