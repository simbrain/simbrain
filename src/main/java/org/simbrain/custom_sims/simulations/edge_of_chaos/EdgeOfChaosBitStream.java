package org.simbrain.custom_sims.simulations.edge_of_chaos;

import org.simbrain.custom_sims.Simulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.core.SynapseGroup;
import org.simbrain.network.neurongroups.NeuronGroup;
import org.simbrain.network.updaterules.BinaryRule;
import org.simbrain.plot.timeseries.TimeSeriesModel;
import org.simbrain.plot.timeseries.TimeSeriesPlotComponent;
import org.simbrain.util.SmileUtilsKt;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.workspace.updater.UpdateActionKt;

import javax.swing.*;

/**
 * Demonstration of representational capacities of recurrent networks based on
 * Bertschinger, Nils, and Thomas Natschläger. "Real-time computation at the
 * edge of chaos in recurrent neural networks." Neural computation 16.7 (2004):
 * 1413-1436.
 *
 * Video of this in action: https://x.com/JeffYoshimi/status/1529126714948743168
 */
public class EdgeOfChaosBitStream extends Simulation {

    //TODO: Docviewer definitely needed. Note that we have preliminary documentation on a google doc. It just has to be ported over!

    // Simulation Parameters
    int NUM_NEURONS = 120;
    static int GRID_SPACE = 25;

    // For 120 neurons. Adjust weight stdev to study.  .5 ordered.  1.9 or so is the edgeof chaos

    // Since mean is 0, lower variance means lower average weight strength
    private double variance = .5;
    private double u_bar = 1.0;

    // References
    Network net;
    SynapseGroup sgRes1, sgRes2;
    NeuronGroup res1, res2, bitStream1, bitStream2;
    int currentRow = 0;

    private long seed = 42L;


    @Override
    public void run() {

        // Clear workspace
        sim.getWorkspace().clearWorkspace();

        // Build network
        NetworkComponent nc = sim.addNetwork(201,10,480,590, "Edge of Chaos");
        net = nc.getNetwork();
        buildNetwork();

        // Set up the time series and a custom action
        setUpTimeSeries();

        // Set up control panel
        controlPanel();

        sim.addDocViewerWithText(1055, 11, 471, 291, "Edge Of Chaos Bitstream",
                """
                # Introduction
                This simulation is a simulation of two reservoirs, or two recurrent networks, running concurrently where the difference between the two reservoirs are recorded in a time series. From their difference, we can infer the three different states of computation: `ordered`, `edge of chaos`, and `chaos`. The goal of this
                simulation is to understand the differences in the three different states and get a view of each state.
                                
                # Background
                [Reservoir computing](https://en.wikipedia.org/wiki/Reservoir_computing) is a general theory of the computational properties of neural networks that attempts to grasp at the types of computation a neural network in the brain requires (i.e., constantly cycling recurrent activity in response to varying stimuli/inputs).
                                
                From this theory emerges two key concepts to keep in mind: the _fading memory property_ and the _separation property_. The fading memory property builds upon the idea that different "memory" states are stored within recurrent networks as activation patterns and how recurrent networks can remember past inputs, or past memory states. However, the property also states that past inputs should fade over time with the influx of new inputs. 
                The separation property builds upon the idea of the fading memory property, stating that with two different inputs, the network will produce two different activation states. However, we would not want an excess amount of separation if we are provided two similar inputs (i.e., two types of flowers, we would still want to know that it is a flower).
                                
                Tying these properties to the three different states, an ordered state will have a weakened fading memory and separation property where it will lean into an attractor. A chaotic state will have both properties however, with an excess amount of separation. The edge of chaos state is the state that our brains have been theorized to be within, where there is just the right amount of separation and fading memory.
                                
                # Finding The Three Different States
                In this simulation, the only configuration to the simulation is the `weight stdev`. Change the `weight stdev` and press the `update` button to tweak the reservoirs' responses to activation inputs. After, you start the simulation, click on the `magic wand` (the sparkly stick icon next to the `pointer` icon), and increase the activation in one of the reservoirs. This will make the two bitstreams different from one another, which is illustrated in the time series. To `reset` the simulation, press `k`.
                                
                For low weight stdev, the difference in the two reservoirs dies off quickly (i.e., `ordered` state); whereas for high weight stdev, the difference persists for a long period of time (i.e., `chaotic` state). The goal of this simulation is to continue tuning the weight stdev to find the `edge of chaos` (where the difference fluctuates for a short period of time, and then goes away).
                                
                (Include link to Edge of Chaos twitter post)
                                
                """);
    }

    private void controlPanel() {
        ControlPanel panel = ControlPanel.makePanel(sim, "Controller", 5,10,205,180);
        JTextField tf_stdev = panel.addTextField("Weight stdev", "" + variance);
        panel.addButton("Update", () -> {

            // Update variance of weight strengths
            double new_variance = Double.parseDouble(tf_stdev.getText());
            for (Synapse synapse : sgRes1.getSynapses()) {
                synapse.setStrength(synapse.getStrength() * (new_variance / variance));
            }
            for (Synapse synapse : sgRes2.getSynapses()) {
                synapse.setStrength(synapse.getStrength() * (new_variance / variance));
            }
            variance = new_variance;

            // Update strength of bitstream signals
            // // TODO: Complain if input strength set to 0.0
            //  double new_ubar = Double.parseDouble(input_tf.getText());
            //  for (double[] row : bitStream1.getInputData().toArray()) {
            //      if (row[0] != 0) {
            //          row[0] = new_ubar;
            //      }
            //  }
            //  for (double[] row : bitStream2.getInputData().toArray()) {
            //      if (row[0] != 0) {
            //          row[0] = new_ubar;
            //      }
            //  }
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
        sgRes1 = EdgeOfChaos.connectReservoir(net, res1, variance, 4, seed);
        sgRes2 = EdgeOfChaos.connectReservoir(net, res2, variance, 4, seed);
        sgRes2.setLabel("Recurrent Synapses");
        net.addNetworkModel(sgRes2);

        // Set up "bit-stream" inputs
        bitStream1 = buildBitStream(res1);
        bitStream1.setLabel("Bit stream 1");
        bitStream2 = buildBitStream(res2);
        bitStream2.setLabel("Bit stream 2");
        AllToAll connector1 = new AllToAll(false, seed);
        AllToAll connector2 = new AllToAll(false, seed);
        var thing1 = connector1.connectNeurons(bitStream1.getNeuronList(), res1.getNeuronList());
        var thing2 = connector2.connectNeurons(bitStream2.getNeuronList(), res2.getNeuronList());
        net.addNetworkModels(thing1);
        net.addNetworkModels(thing2);
    }

    NeuronGroup bitStreamInputs;

    private NeuronGroup buildBitStream(NeuronGroup reservoir) {
        // Offset in pixels of input nodes to right of reservoir
        int offset = 200;
        bitStreamInputs = new NeuronGroup(1);
        BinaryRule b = new BinaryRule(0, u_bar, .49);
        bitStreamInputs.setUpdateRule(b);
        var bitStream = new double[][]{{u_bar}, {0.0}, {0.0}, {0.0}, {0.0}, {u_bar}, {0.0}, {u_bar}, {u_bar}, {0.0}, {u_bar}, {u_bar}, {0.0}, {0.0}, {u_bar}};
        bitStreamInputs.setInputData(SmileUtilsKt.toMatrix(bitStream));
        net.addNetworkModel(bitStreamInputs);
        bitStreamInputs.setLocation(reservoir.getCenterX(), reservoir.getMaxY() + offset);
        return bitStreamInputs;
    }

    private void setUpTimeSeries() {
        // Set up the plot
        TimeSeriesPlotComponent ts = sim.addTimeSeries(681,15,363,285, "Time Series");
        TimeSeriesModel.TimeSeries sts1 = ts.getModel().addTimeSeries("Difference");

        sim.getWorkspace().getUpdater().getUpdateManager().addAction(UpdateActionKt.create("Update inputs", () -> {
            bitStream1.addInputs(bitStream1.getInputData().row(currentRow));
            bitStream2.addInputs(bitStream2.getInputData().row(currentRow));
            currentRow = (currentRow + 1) % bitStream1.getInputData().nrow();
        }), 0);

        sim.getWorkspace().addUpdateAction(UpdateActionKt.create("Update time series", () -> {
            int activationDiff = SimbrainMath.hamming(res1.getActivationArray(), res2.getActivationArray());
            ts.getModel().addData(0, sim.getWorkspace().getTime(), activationDiff);
        }));
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
