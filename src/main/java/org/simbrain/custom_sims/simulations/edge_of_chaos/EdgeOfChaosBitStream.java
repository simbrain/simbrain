package org.simbrain.custom_sims.simulations.edge_of_chaos;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.neuron_update_rules.BinaryRule;
import org.simbrain.network.util.SimnetUtils;
import org.simbrain.plot.timeseries.TimeSeriesModel;
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent;
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
    Network net;
    SynapseGroup sgRes1, sgRes2;
    NeuronGroup res1, res2, bitStream1, bitStream2;

    @Override
    public void run() {

        // Clear workspace
        sim.getWorkspace().clearWorkspace();

        // Build network
        NetworkComponent nc = sim.addNetwork(138,10,450,450, "Edge of Chaos");
        net = nc.getNetwork();
        buildNetwork();

        // Set up the time series and a custom action
        setUpTimeSeries();

        // Set up control panel
        controlPanel();
    }

    private void controlPanel() {
        ControlPanel panel = ControlPanel.makePanel(sim, "Controller", 5,10,133,136);
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
            for (double[] row : bitStream1.getInputManager().getData()) {
                if (row[0] != 0) {
                    row[0] = new_ubar;
                }
            }
            for (double[] row : bitStream2.getInputManager().getData()) {
                if (row[0] != 0) {
                    row[0] = new_ubar;
                }
            }
        });
    }

    void buildNetwork() {
        net.setTimeStep(0.5);

        // Make reservoirs
        res1 = EdgeOfChaos.createReservoir(net, 10, 10, NUM_NEURONS);
        res1.setLabel("Reservoir 1");
        res2 = EdgeOfChaos.createReservoir(net, (int) res1.getMaxX() + 100, 10, NUM_NEURONS);
        res2.setLabel("Reservoir 2");

        // Connect reservoirs
        sgRes1 = EdgeOfChaos.connectReservoir(net, res1, variance, 4);
        sgRes2 = SynapseGroup.createSynapseGroup(res2, res2);
        SimnetUtils.copySynapses(sgRes2, sgRes1);
        sgRes2.setLabel("Recurrent Synapses");

        net.addNetworkModel(sgRes2);

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
        NeuronGroup bitStreamInputs = new NeuronGroup(net, 1);
        bitStreamInputs.setLocation(reservoir.getCenterX(), reservoir.getMaxY() + offset);
        BinaryRule b = new BinaryRule(0, u_bar, .5);
        bitStreamInputs.setNeuronType(b);
        bitStreamInputs.setClamped(true);
        bitStreamInputs.getInputManager().setData(new double[][]{{u_bar}, {0.0}, {0.0}, {0.0}, {0.0}, {u_bar}, {0.0}, {u_bar}, {u_bar}, {0.0}, {u_bar}, {u_bar}, {0.0}, {0.0}, {u_bar}});
        bitStreamInputs.setInputMode(true);
        net.addNetworkModel(bitStreamInputs);
        return bitStreamInputs;
    }

    private void setUpTimeSeries() {
        // Set up the plot
        TimeSeriesPlotComponent ts = sim.addTimeSeries(588,10,363,285, "Time Series");
        TimeSeriesModel.ScalarTimeSeries sts1 = ts.getModel().addScalarTimeSeries("Difference");

        sim.getWorkspace().addUpdateAction(new UpdateActionAdapter("Update time series") {
            @Override
            public void invoke() {
                double activationDiff = SimbrainMath.distance(res1.getActivations(), res2.getActivations());
                ts.getModel().addData(0, sim.getWorkspace().getTime(), activationDiff);
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
    public String getSubmenuName() {
        return "Chaos";
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
