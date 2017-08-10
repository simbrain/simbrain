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

    // TODO: Add PCA by default

    // Simulation Parameters
    int NUM_NEURONS = 120;
    int GRID_SPACE = 25;
    // Since mean is 0, lower variance means lower average weight strength
    private double variance = .5;
    private double u_bar = .5;
    private int kIn = 4; // in-degree (num connections to each neuron)

    // References
    Network network;
    SynapseGroup reservoir, bitsToRes, cheeseToRes, flowersToRes; // rename
                                                                  // reservoir
    NeuronGroup reservoirNg, bitStreamInputs, sensorNodes;
    OdorWorldBuilder world;
    RotatingEntity mouse;
    OdorWorldEntity cheese, flower, fish;

    @Override
    public void run() {

        // Clear workspace
        sim.getWorkspace().clearWorkspace();

        // Build network
        NetBuilder net = sim.addNetwork(284, 10, 450, 450, "Edge of Chaos");
        network = net.getNetwork();
        buildNetwork();

        // Time series of inputs
        // PlotBuilder ts = sim.addTimeSeriesPlot(689, 10, 363, 285, "Input");
        // sim.couple(net.getNetworkComponent(),
        // inputNodes.getNeuronList().get(0), ts.getTimeSeriesComponent(), 0);

        // Odor world sim
        buildOdorWorld();

        // Set up control panel
        controlPanel();
    }

    private void buildOdorWorld() {

        // Create the odor world
        world = sim.addOdorWorld(725, 12, 416, 378, "Two Objects");
        world.getWorld().setObjectsBlockMovement(false);
        mouse = world.addAgent(165, 245, "Mouse");
        mouse.setHeading(90);

        // Set up world
        cheese = world.addEntity(40, 40, "Swiss.gif",
                new double[] { 1, 0, 0, 0, 0, 0 });
        cheese.getSmellSource().setDispersion(65);
        flower = world.addEntity(290, 40, "Pansy.gif",
                new double[] { 0, 0, 0, 0, 0, 1 });
        flower.getSmellSource().setDispersion(65);

        // Couple agent to cheese and flower nodes
        sim.couple((SmellSensor) mouse.getSensor("Smell-Center"), sensorNodes);
    }

    private void controlPanel() {
        ControlPanel panel = ControlPanel.makePanel(sim, "Controller", 5, 10);
        JTextField input_tf = panel.addTextField("Input strength", "" + u_bar);
        JTextField tf_stdev = panel.addTextField("Weight stdev", "" + variance);
        panel.addButton("Bit-stream mode", "Apply", () -> {
            bitsToRes.setEnabled(true);
            cheeseToRes.setEnabled(false);
            flowersToRes.setEnabled(false);
        });
        panel.addButton("Sensor mode", "Apply", () -> {
            bitsToRes.setEnabled(false);
            cheeseToRes.setEnabled(true);
            flowersToRes.setEnabled(true);
        });
        panel.addButton("Update", () -> {

            // Update variance of weight strengths
            double new_variance = Double.parseDouble(tf_stdev.getText());
            for (Synapse synapse : reservoir.getAllSynapses()) {
                synapse.setStrength(
                        synapse.getStrength() * (new_variance / variance));
            }
            variance = new_variance;

            // Update strength of bitstream signals
            // TODO: Complain if input strength set to 0.
            double new_ubar = Double.parseDouble(input_tf.getText());
            for (double[] row : bitStreamInputs.getTestData()) {
                if (row[0] != 0) {
                    row[0] = new_ubar;
                }
            }
        });
//        panel.addButton("Similarity Study", "Run", () -> {
//            // TODO: Don't allow this to be run twice?
//            cloneReservoir();
//        });
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
        reservoirNg = new NeuronGroup(network, neurons);
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
        reservoir = SynapseGroup.createSynapseGroup(reservoirNg, reservoirNg,
                con, 0.5, exRand, inRand);
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
        bitStreamInputs = new NeuronGroup(network, 1);
        bitStreamInputs.setLocation(reservoirNg.getMaxX() + offset,
                reservoirNg.getMaxY());
        BinaryRule b = new BinaryRule(0, u_bar, .5);
        bitStreamInputs.setNeuronType(b);
        bitStreamInputs.setClamped(true);
        bitStreamInputs.setLabel("Bit-stream");
        bitStreamInputs.setTestData(
                new double[][] { { u_bar }, { 0.0 }, { 0.0 }, { 0.0 }, { 0.0 },
                        { u_bar }, { 0.0 }, { u_bar }, { u_bar }, { 0.0 },
                        { u_bar }, { u_bar }, { 0.0 }, { 0.0 }, { u_bar } });
        bitStreamInputs.setInputMode(true);
        network.addGroup(bitStreamInputs);
        bitsToRes = SynapseGroup.createSynapseGroup(bitStreamInputs,
                reservoirNg, new AllToAll());
        bitsToRes.setStrength(1.0, Polarity.BOTH);
        network.addGroup(bitsToRes);

        // Sensor nodes
        sensorNodes = new NeuronGroup(network, 6);
        sensorNodes.setLocation(reservoirNg.getMaxX() + offset,
                reservoirNg.getMinY());
        sensorNodes.setLabel("Sensors");
        sensorNodes.setClamped(true);
        network.addGroup(sensorNodes);
        // Make custom connections from sensor nodes to upper-left and
        // lower-right quadrants of the reservoir network to ensure visually
        // distinct patterns.
        cheeseToRes = sensorConnections(sensorNodes, reservoirNg,
                new int[] { 0, 1, 2 }, .8, 1);
        network.addGroup(cheeseToRes);
        flowersToRes = sensorConnections(sensorNodes, reservoirNg,
                new int[] { 3, 4, 5 }, .8, 3);
        network.addGroup(flowersToRes);

        // Use concurrent buffered update
        network.getUpdateManager().clear();
        network.getUpdateManager().addAction(ConcurrentBufferedUpdate
                .createConcurrentBufferedUpdate(network));
    }

    // Possibly export this to a utility class
    /**
     * Connect a set of source neurons to a quadrant of the reservoir.
     *
     * @param src inputs
     * @param tar reservoir
     * @param srcNodeIndices which input nodes to connect
     * @param sparsity sparsity
     * @param quadrantNumber 1,2,3 or 4 from upper-left clockwise
     * @return the resulting synapse group
     */
    private SynapseGroup sensorConnections(NeuronGroup src, NeuronGroup tar,
            int[] srcNodeIndices, double sparsity, int quadrantNumber) {

        double xStart, yStart, xEnd, yEnd;

        if (quadrantNumber < 3) {
            yStart = tar.getMinY();
            yEnd = tar.getCenterY();
        } else {
            yStart = tar.getCenterY();
            yEnd = tar.getMaxY();
        }

        if ((quadrantNumber == 1) || (quadrantNumber == 4)) {
            xStart = tar.getMinX();
            xEnd = tar.getCenterX();
        } else {
            xStart = tar.getCenterX();
            xEnd = tar.getMaxX();
        }

        SynapseGroup src2Res = new SynapseGroup(src, tar);
        List<Neuron> tarNList = tar.getNeuronList();
        for (int ii = 0; ii < tar.size(); ++ii) {
            double x = tarNList.get(ii).getX();
            double y = tarNList.get(ii).getY();
            if ((y >= yStart) && (y < yEnd)) {
                if ((x >= xStart) && (x < xEnd)) {
                    for (int j = 0; j < srcNodeIndices.length; j++) {
                        if (Math.random() < sparsity) {
                            Synapse syn = new Synapse(
                                    src.getNeuronListUnsafe()
                                            .get(srcNodeIndices[j]),
                                    tarNList.get(ii));
                            syn.setStrength(1);
                            src2Res.addSynapseUnsafe(syn);
                        }
                    }
                }
            }
        }
        return src2Res;
    }

    // NOT WORKING
    private void cloneReservoir() {

//        network.removeGroup(sensorNodes);
//        sim.getWorkspace()
//                .removeWorkspaceComponent(world.getOdorWorldComponent());

        NeuronGroup clonedRes = new NeuronGroup(network, reservoirNg);
//        clonedRes.setLocation(
//                reservoirNg.getMinX() - reservoirNg.getWidth() - 200,
//                reservoirNg.getMinY());

        network.addGroup(clonedRes);

        // // TODO: Need a copy constructor and/or copy properties function for
        // // synapse groups
        //
        // NeuronGroup clonedInputs = new NeuronGroup(network, bitStreamInputs);
        // network.addGroup(clonedInputs);
        //
        // bitStreamInputs.setLocation(reservoirNg.getCenterX(),
        // reservoirNg.getMaxY() + 100);
        // clonedInputs.setLocation(clonedRes.getCenterX(),
        // clonedRes.getMaxY() + 100);
        //
        // SynapseGroup clonedBitsToRes = SynapseGroup
        // .createSynapseGroup(clonedInputs, clonedRes, new AllToAll());
        // clonedBitsToRes.setStrength(1.0, Polarity.BOTH);
        // network.addGroup(clonedBitsToRes);

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
