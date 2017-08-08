package org.simbrain.custom_sims.simulations.edge_of_chaos;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextField;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.custom_sims.helper_classes.NetBuilder;
import org.simbrain.custom_sims.helper_classes.PlotBuilder;
import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.connections.RadialSimpleConstrainedKIn;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule.InputType;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.neuron_update_rules.BinaryRule;
import org.simbrain.network.update_actions.ConcurrentBufferedUpdate;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.math.ProbDistribution;
import org.simbrain.util.randomizer.PolarizedRandomizer;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.gui.SimbrainDesktop;

/**
 * Playground for testing new features. A lot of stuff is commented out but
 * should work.
 */
public class EdgeOfChaos extends RegisteredSimulation {

    // Simulation Parameters
    int NUM_NEURONS = 130;
    int GRID_SPACE = 25;
    private double variance = .5;
    private double u_bar = .5;
    private int kIn = 4;

    // References
    Network network;
    SynapseGroup sg;
    NeuronGroup input; // TODO: Name

    /**
     * Run the simulation!
     */
    public void run() {

        sim.getWorkspace().clearWorkspace();

        NetBuilder net = sim.addNetwork(237, 10, 450, 450, "Edge of Chaos");
        network = net.getNetwork();

        buildNetwork();
        
        PlotBuilder ts = sim.addTimeSeriesPlot(689, 10, 363, 285, "Input");
        Coupling rewardCoupling = sim.couple(net.getNetworkComponent(),
                input.getNeuronList().get(0), ts.getTimeSeriesComponent(), 0);
        
        ControlPanel panel = ControlPanel.makePanel(sim, "Test", 5, 10);
        JTextField input_tf = panel.addTextField("Input strength", "" + u_bar);
        JTextField tf_stdev = panel.addTextField("Weight stdev", "" + variance);
        panel.addButton("Update", () -> {
            
            //TODO: Complain if input strength set to 0.
            
            // Update variance of weight strengths
            double new_variance = Double.parseDouble(tf_stdev.getText());
            for (Synapse synapse : sg.getAllSynapses()) {
                synapse.setStrength(
                        synapse.getStrength() * (new_variance / variance));
            }
            variance = new_variance;
            
            // Update strength of input signals
            double new_ubar = Double.parseDouble(input_tf.getText());
            for (double[] row : input.getTestData()) {
                if (row[0] != 0) {
                    row[0] = new_ubar;
                }
            }
        });
    }

    void buildNetwork() {
        network.setTimeStep(0.5);
        GridLayout layout = new GridLayout(GRID_SPACE, GRID_SPACE,
                (int) Math.sqrt(NUM_NEURONS));
        layout.setInitialLocation(new Point(10, 10));
        List<Neuron> neurons = new ArrayList<Neuron>(NUM_NEURONS);
        for (int i = 0; i < NUM_NEURONS; i++) {
            Neuron neuron = new Neuron(network);
            BinaryRule str = new BinaryRule();
            str.setInputType(InputType.WEIGHTED);
            neuron.setUpdateRule(str);
            neurons.add(neuron);
        }
        // TODO: Rename to reservoir
        NeuronGroup ng1 = new NeuronGroup(network, neurons);
        ng1.setLabel("Reservoir");
        network.addGroup(ng1);
        ng1.setLayout(layout);
        ng1.applyLayout(new Point2D.Double(0.0, 0.0));

        PolarizedRandomizer exRand = new PolarizedRandomizer(
                Polarity.EXCITATORY, ProbDistribution.NORMAL);
        PolarizedRandomizer inRand = new PolarizedRandomizer(
                Polarity.INHIBITORY, ProbDistribution.NORMAL);
        exRand.setParam1(0);
        exRand.setParam2(Math.sqrt(variance));
        inRand.setParam1(0);
        inRand.setParam2(Math.sqrt(variance));

        RadialSimpleConstrainedKIn con = new RadialSimpleConstrainedKIn(kIn,
                (int) (Math.sqrt(NUM_NEURONS) * GRID_SPACE / 2));
        sg = SynapseGroup.createSynapseGroup(ng1, ng1, con, 0.5, exRand,
                inRand);
        sg.setLabel("Recurrent Synapses");
        network.addGroup(sg);

        // print(sg.size());

        sg.setUpperBound(200, Polarity.EXCITATORY);
        sg.setLowerBound(0, Polarity.EXCITATORY);
        sg.setLowerBound(-200, Polarity.INHIBITORY);
        sg.setUpperBound(0, Polarity.INHIBITORY);

        input = new NeuronGroup(network, 1);
        input.setLocation(ng1.getMinX() - 100, ng1.getMinY());
        BinaryRule b = new BinaryRule(0, u_bar, .5);
        input.setNeuronType(b);
        input.setClamped(true);
        input.setTestData(new double[][] { { u_bar }, { 0.0 }, { 0.0 }, { 0.0 },
                { 0.0 }, { u_bar }, { 0.0 }, { u_bar }, { u_bar }, { 0.0 },
                { u_bar }, { u_bar }, { 0.0 }, { 0.0 }, { u_bar } });
        input.setInputMode(true);
        network.addGroup(input);
        AllToAll inp2ResCon = new AllToAll();
        SynapseGroup inp2resSG = SynapseGroup.createSynapseGroup(input, ng1,
                inp2ResCon);
        inp2resSG.setStrength(1.0, Polarity.BOTH);
        network.addGroup(inp2resSG);

        network.getUpdateManager().clear();
        network.getUpdateManager().addAction(ConcurrentBufferedUpdate
                .createConcurrentBufferedUpdate(network));
    }

    void addTimeSeries() {

        PlotBuilder plot = sim.addTimeSeriesPlot(9, 800, 800, 285, "Inputs");
        //
        // sim.couple(net.getNetworkComponent(),
        // dopamine, plot.getTimeSeriesComponent(), 0);
        // sim.couple(net.getNetworkComponent(), output,
        // plot.getTimeSeriesComponent(), 1);

    }

    public EdgeOfChaos(SimbrainDesktop desktop) {
        super(desktop);
    }

    public EdgeOfChaos() {
        super();
    };

    @Override
    public String getName() {
        return "Edge of Chaos";
    }

    @Override
    public EdgeOfChaos instantiate(SimbrainDesktop desktop) {
        return new EdgeOfChaos(desktop);
    }

}
