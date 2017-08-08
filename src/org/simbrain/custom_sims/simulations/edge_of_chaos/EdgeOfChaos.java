package org.simbrain.custom_sims.simulations.edge_of_chaos;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextField;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.custom_sims.helper_classes.NetBuilder;
import org.simbrain.custom_sims.helper_classes.OdorWorldBuilder;
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
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.entities.RotatingEntity;
import org.simbrain.world.odorworld.sensors.SmellSensor;

/**
 * Demonstration of representational capacities of recurrent networks based on
 * Bertschinger, Nils, and Thomas Natschläger. "Real-time computation at the
 * edge of chaos in recurrent neural networks." Neural computation 16.7 (2004):
 * 1413-1436.
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
    SynapseGroup reservoir;
    NeuronGroup inputNodes, cheeseNodes, flowerNodes;
    OdorWorldBuilder world;
    RotatingEntity mouse;
    OdorWorldEntity cheese, flower, fish;

    @Override
    public void run() {

        // Clear workspace
        sim.getWorkspace().clearWorkspace();

        // Build network
        NetBuilder net = sim.addNetwork(237, 10, 450, 450, "Edge of Chaos");
        network = net.getNetwork();
        buildNetwork();

        // Time series of inputs
        //PlotBuilder ts = sim.addTimeSeriesPlot(689, 10, 363, 285, "Input");
        //sim.couple(net.getNetworkComponent(),
        //       inputNodes.getNeuronList().get(0), ts.getTimeSeriesComponent(), 0);
        
        // Odor world sim
        buildOdorWorld();
        
        // Set up control panel
        controlPanel();
    }

    private void buildOdorWorld() {
        
        // Create the odor world
        world = sim.addOdorWorld(676,12,416,378, "Two Objects");
        world.getWorld().setObjectsBlockMovement(false);
        mouse = world.addAgent(165, 245, "Mouse");
        mouse.setHeading(90);
        
        // Set up world
        cheese = world.addEntity(40, 40, "Swiss.gif",
                new double[] { 1, 0, 0 });
        cheese.getSmellSource().setDispersion(65);
        flower = world.addEntity(290, 40, "Pansy.gif",
                new double[] { 0, 1, 0 });
        cheese.getSmellSource().setDispersion(65);

        // Couple agent to cheese and flower nodes
        sim.couple((SmellSensor) mouse.getSensor("Smell-Center"), cheeseNodes);
        sim.couple((SmellSensor) mouse.getSensor("Smell-Center"), flowerNodes);
    }

    private void controlPanel() {
        ControlPanel panel = ControlPanel.makePanel(sim, "Test", 5, 10);
        JTextField input_tf = panel.addTextField("Input strength", "" + u_bar);
        JTextField tf_stdev = panel.addTextField("Weight stdev", "" + variance);
        panel.addButton("Update", () -> {
            
            //TODO: Complain if input strength set to 0.
            
            // Update variance of weight strengths
            double new_variance = Double.parseDouble(tf_stdev.getText());
            for (Synapse synapse : reservoir.getAllSynapses()) {
                synapse.setStrength(
                        synapse.getStrength() * (new_variance / variance));
            }
            variance = new_variance;
            
            // Update strength of input signals
            double new_ubar = Double.parseDouble(input_tf.getText());
            for (double[] row : inputNodes.getTestData()) {
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
        NeuronGroup reservoirNg = new NeuronGroup(network, neurons);
        reservoirNg.setLabel("Reservoir");
        network.addGroup(reservoirNg);
        reservoirNg.setLayout(layout);
        reservoirNg.applyLayout(new Point2D.Double(0.0, 0.0));

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
        reservoir = SynapseGroup.createSynapseGroup(reservoirNg, reservoirNg, con, 0.5, exRand,
                inRand);
        reservoir.setLabel("Recurrent Synapses");
        network.addGroup(reservoir);

        reservoir.setUpperBound(200, Polarity.EXCITATORY);
        reservoir.setLowerBound(0, Polarity.EXCITATORY);
        reservoir.setLowerBound(-200, Polarity.INHIBITORY);
        reservoir.setUpperBound(0, Polarity.INHIBITORY);
        
        // Offset in pixels of input nodes to right of reservoir
        // TODO offset to left does not look good. Zach?
        int offset = 310;

        // Set up "bit-stream" input nodes
        inputNodes = new NeuronGroup(network, 1);
        inputNodes.setLocation(reservoirNg.getMaxX() + offset, reservoirNg.getMaxY());
        BinaryRule b = new BinaryRule(0, u_bar, .5);
        inputNodes.setNeuronType(b);
        inputNodes.setClamped(true);
        inputNodes.setLabel("Bit-stream");
        inputNodes.setTestData(new double[][] { { u_bar }, { 0.0 }, { 0.0 }, { 0.0 },
                { 0.0 }, { u_bar }, { 0.0 }, { u_bar }, { u_bar }, { 0.0 },
                { u_bar }, { u_bar }, { 0.0 }, { 0.0 }, { u_bar } });
        inputNodes.setInputMode(true);
        network.addGroup(inputNodes);
        SynapseGroup bitsToRes = SynapseGroup.createSynapseGroup(inputNodes, reservoirNg,
                new AllToAll());
        bitsToRes.setStrength(1.0, Polarity.BOTH);
        network.addGroup(bitsToRes);
        
        // Cheesy nodes
        cheeseNodes = new NeuronGroup(network, 3);
        cheeseNodes.setLocation(reservoirNg.getMaxX() + offset, reservoirNg.getCenterY());
        cheeseNodes.setLabel("Cheeses");
        cheeseNodes.setClamped(true);
        network.addGroup(cheeseNodes);
        SynapseGroup cheeseToRes = SynapseGroup.createSynapseGroup(cheeseNodes, reservoirNg,
                new AllToAll());
        cheeseToRes.setStrength(1.0, Polarity.BOTH);
        network.addGroup(cheeseToRes);
        
        // Flowery nodes
        flowerNodes = new NeuronGroup(network, 3);
        flowerNodes.setLocation(reservoirNg.getMaxX() + offset, reservoirNg.getMinY());
        flowerNodes.setLabel("Flowers");
        flowerNodes.setClamped(true);
        network.addGroup(flowerNodes);
        SynapseGroup flowerToRes = SynapseGroup.createSynapseGroup(flowerNodes, reservoirNg,
                new AllToAll());
        flowerToRes.setStrength(1.0, Polarity.BOTH);
        network.addGroup(flowerToRes);
        
        // Use concurrent buffered update
        network.getUpdateManager().clear();
        network.getUpdateManager().addAction(ConcurrentBufferedUpdate
                .createConcurrentBufferedUpdate(network));
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
