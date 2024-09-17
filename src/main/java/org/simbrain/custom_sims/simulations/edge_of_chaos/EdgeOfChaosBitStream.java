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
        NetworkComponent nc = sim.addNetwork(494, 10, 480, 590, "Edge of Chaos");
        net = nc.getNetwork();
        buildNetwork();

        // Set up the time series and a custom action
        setUpTimeSeries();

        // Set up control panel
        controlPanel();

        sim.addDocViewerWithText(15, 11, 471, 590, "Edge Of Chaos Information",
"""
        # Introduction
                        
        This simulation is a simulation of two reservoirs, or two recurrent networks, running concurrently where the difference between the two reservoirs are recorded in a time 
        series. From their difference, we can infer the three different states of computation: `ordered`, `edge of chaos`, and `chaos`. The **goal** of this simulation is to 
        **continuously change the simulation's configuration** until you have reached the **edge of chaos**, and get an understanding of each state.
        
        # Background
                        
        [Reservoir computing](https://en.wikipedia.org/wiki/Reservoir_computing) is a general theory of the computational properties that exists in neural networks and attempts 
        to grasp at the types of computation that a neural network in the brain requires to function properly (i.e., the constant cycling of recurrent activity in response to 
        varying stimuli/inputs). From this theory emerges two key concepts to keep in mind: the **fading memory property** and the **separation property**.
        
        1) The fading memory property states that **recurrent networks** can **store different memory states** as activation patterns and **recall** past memory states.
        However, the property also states that past activation patterns should **fade over time** with the **influx of new activation inputs**. However, we would not want an
        excess amount, or lack thereof, of the fading of memory, we want just the right amount. Too little, and the network cannot store new information; too much, and 
        the network cannot collect and store new information.
                        
        2) The separation property builds upon the concept of the fading memory property, stating that with **the influx of new activation inputs**, a network will **produce 
        different memory states**. However, we would not want an excess amount of separation. Like for instance, if a neural network receives the activation patterns of two 
        types of flowers, the network should still know that it is a flower with a minor distinction in activation but, not too large of a distinction where it differentiates
        the two flowers (an example of this can be tested in the `Embodied EdgeOfChaos` simulation).
        
        Tying these properties to the three different states, an `ordered state` will have a **weakened fading and weakened separation of memory states** where its activation 
        patterns will be pulled into a cyclic cycle of activation patterns. In this state, its difference will quickly die off. A `chaotic state` will have both properties however, 
        with an **excess amount of separation** where its difference will continuously fluctuate. The `edge of chaos state` is the state that our brains have been theorized to 
        be within, where there is just the **right balance of the separation and fading of memory**. In this state, the difference will fluctuate for a period of time, and 
        then die off.
        
        # What To Do
        
        To test these concepts, this simulation will utilize two reservoirs as memory states to illustrate the three types of computational states that the neural networks can
        be in. They will begin with the same activation patterns, and through the addition of new activation inputs, they will produce a difference in activation. This difference 
        will tell us which computational state we are in.
                        
        ## Finding The Three Different States
        
        In this simulation, the only configuration to the simulation is the `weight stdev`. To find each state, follow the steps below.
        
        1) Change the `weight stdev` value and press the `update` button to change the reservoirs' responses to new activation inputs, which will be shown in the time series.
                        
        2) Start the simulation by clicking on the `play` button in the top-left corner.
        
        3) Then, click on the `magic wand` (the icon next to the `pointer` icon), and increase the activation in one of the reservoirs by holding left-click 
        and moving around in either or both reservoirs.
        
        4) Now, observe the changes in the time series and determine its current computational state.   
        
        5) To `reset` the simulation, stop the simulation by clicking the `play` button again and press `k`.
        
        6) Afterwards, click back on the `pointer` icon, and left-click outside of the reservoirs to unselect all neurons.
        
        ### Experimentation with The Ordered State
        
        An experiment to better understand the **ordered state** is to set `weight stdev` to a very low value, like `0.01`, and look at the two reservoirs' activation patterns. 
        Afterwards, experiment with adding in new activation with the `magic wand` and see how the differences between the two reservoirs are changing.
        
        ### Find The Edge Of Chaos
        
        To find the **edge of chaos** state, find the state where there is just the right amount of orderliness and chaos, where the difference remains a short period of time,
        and then disappears. To start, try a `weight stdev` of 3.0, and slowly move down, repeating the steps above until the `edge of chaos`.
                        
        ### Foot-note
        
        If you want a quick look at how the different types of states are exhibited in the time series, click on this 
        [link](https://x.com/JeffYoshimi/status/1529126714948743168).

                """);
    }

    private void controlPanel() {
        ControlPanel panel = ControlPanel.makePanel(sim, "Controller", 984, 10, 205, 180);
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
        TimeSeriesPlotComponent ts = sim.addTimeSeries(984, 200, 425, 399, "Time Series");
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
