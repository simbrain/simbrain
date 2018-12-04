package org.simbrain.custom_sims.simulations.edge_of_chaos;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.custom_sims.helper_classes.NetBuilder;
import org.simbrain.custom_sims.helper_classes.PlotBuilder;
import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.neuron_update_rules.BinaryRule;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.workspace.updater.UpdateActionAdapter;

import javax.swing.*;

/**
 * Demonstration of representational capacities of recurrent networks based on
 * Bertschinger, Nils, and Thomas Natschläger. "Real-time computation at the
 * edge of chaos in recurrent neural networks." Neural computation 16.7 (2004):
 * 1413-1436.
 */
public class EdgeOfChaosBitStream extends RegisteredSimulation {

    // Simulation Parameters
    int NUM_NEURONS = 120;
    static int GRID_SPACE = 25;

    // For 120 neurons.   .5 ordered.  1.9 or so edge.

    // Since mean is 0, lower variance means lower average weight strength
    private static double variance = .5;
    private double u_bar = .5; // I want to try defaulting to 1 so "bits" are obvious

    // References
    Network network;
    SynapseGroup sgRes1, sgRes2;
    NeuronGroup res1, res2, bitStream1, bitStream2;

    @Override
    public void run() {

        // Clear workspace
        sim.getWorkspace().clearWorkspace();

        // Build network
        NetBuilder net = sim.addNetwork(170, 10, 450, 450, "Edge of Chaos");
        network = net.getNetwork();
        buildNetwork();

        // Set up the time series and a custom action
        setUpTimeSeries();

        // Set up control panel
        controlPanel();
    }

    private void controlPanel() {
        ControlPanel panel = ControlPanel.makePanel(sim, "Controller", 5, 10);
        JTextField input_tf = panel.addTextField("Input strength", "" + u_bar);
        JTextField tf_stdev = panel.addTextField("Weight stdev", "" + variance);
        panel.addButton("Update", () -> {

            // Update variance of weight strengths
            double new_variance = Double.parseDouble(tf_stdev.getText());
            for (Synapse synapse : sgRes1.getAllSynapses()) {
                synapse.setStrength(synapse.getStrength() * (new_variance / variance));
            }
            for (Synapse synapse : sgRes2.getAllSynapses()) {
                synapse.setStrength(synapse.getStrength() * (new_variance / variance));
            }
            variance = new_variance;

            // Update strength of bitstream signals
            // TODO: Complain if input strength set to 0.
            double new_ubar = Double.parseDouble(input_tf.getText());
            for (double[] row : bitStream1.getTestData()) {
                if (row[0] != 0) {
                    row[0] = new_ubar;
                }
            }
            for (double[] row : bitStream2.getTestData()) {
                if (row[0] != 0) {
                    row[0] = new_ubar;
                }
            }
        });
    }

    void buildNetwork() {
        network.setTimeStep(0.5);

        // Make reservoirs
        res1 = EdgeOfChaos.createReservoir(network, 10, 10, NUM_NEURONS);
        res1.setLabel("Reservoir 1");
        res2 = EdgeOfChaos.createReservoir(network, (int) res1.getMaxX() + 100, 10, NUM_NEURONS);
        res2.setLabel("Reservoir 2");

        // Connect reservoirs
        sgRes1 = EdgeOfChaos.connectReservoir(network, res1, variance, 4);
        sgRes2 = new SynapseGroup(res2, res2);
        sgRes2.copySynapses(sgRes1);
        sgRes2.setLabel("Recurrent Synapses");
        network.addGroup(sgRes2);

        // Set up "bit-stream" inputs
        bitStream1 = buildBitStream(res1);
        bitStream1.setLabel("Bit stream 1");
        bitStream2 = buildBitStream(res2);
        bitStream2.setLabel("Bit stream 2");
        AllToAll connector = new AllToAll();
        connector.connectAllToAll(bitStream1.getNeuronList(), res1.getNeuronList());
        connector.connectAllToAll(bitStream2.getNeuronList(), res2.getNeuronList());

        // Use concurrent buffered update
        //        network.getUpdateManager().clear();
        //        network.getUpdateManager().addAction(ConcurrentBufferedUpdate
        //                .createConcurrentBufferedUpdate(network));
    }

    private NeuronGroup buildBitStream(NeuronGroup reservoir) {
        // Offset in pixels of input nodes to right of reservoir
        int offset = 200;
        NeuronGroup bitStreamInputs = new NeuronGroup(network, 1);
        bitStreamInputs.setLocation(reservoir.getCenterX(), reservoir.getMaxY() + offset);
        BinaryRule b = new BinaryRule(0, u_bar, .5);
        bitStreamInputs.setNeuronType(b);
        bitStreamInputs.setClamped(true);
        bitStreamInputs.setTestData(new double[][]{{u_bar}, {0.0}, {0.0}, {0.0}, {0.0}, {u_bar}, {0.0}, {u_bar}, {u_bar}, {0.0}, {u_bar}, {u_bar}, {0.0}, {0.0}, {u_bar}});
        bitStreamInputs.setInputMode(true);
        network.addGroup(bitStreamInputs);
        return bitStreamInputs;
    }

    private void setUpTimeSeries() {
        // Set up the plot
        PlotBuilder ts = sim.addTimeSeriesPlot(609, 11, 363, 285, "Difference");
        sim.getWorkspace().addUpdateAction(new UpdateActionAdapter("Update time series") {
            @Override
            public void invoke() {
                double activationDiff = SimbrainMath.distance(res1.getActivations(), res2.getActivations());
                ts.getTimeSeriesModel().addData(0, sim.getWorkspace().getTime(), activationDiff);
            }
        });
    }

    public EdgeOfChaosBitStream(SimbrainDesktop desktop) {
        super(desktop);
    }

    public EdgeOfChaosBitStream() {
        super();
    }

    @Override
    public String getName() {
        return "Edge of Chaos (Bit Stream)";
    }

    @Override
    public EdgeOfChaosBitStream instantiate(SimbrainDesktop desktop) {
        return new EdgeOfChaosBitStream(desktop);
    }

}
