package org.simbrain.custom_sims.simulations.edge_of_chaos;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextField;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.custom_sims.helper_classes.NetBuilder;
import org.simbrain.custom_sims.helper_classes.OdorWorldBuilder;
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
import org.simbrain.util.randomizer.Randomizer;
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
    static int GRID_SPACE = 25;
    // Since mean is 0, lower variance means lower average weight strength
    private static double variance = .5;
    private static int kIn = 4; // in-degree (num connections to each neuron)

    // References
    Network network;
    SynapseGroup sgReservoir, cheeseToRes, flowersToRes;
    NeuronGroup reservoir, sensorNodes;
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

    private void controlPanel() {
        ControlPanel panel = ControlPanel.makePanel(sim, "Controller", 5, 10);
        JTextField tf_stdev = panel.addTextField("Weight stdev", "" + variance);
        panel.addButton("Update", () -> {

            // Update variance of weight strengths
            double new_variance = Double.parseDouble(tf_stdev.getText());
            for (Synapse synapse : sgReservoir.getAllSynapses()) {
                synapse.setStrength(
                        synapse.getStrength() * (new_variance / variance));
            }
            variance = new_variance;
        });
    }

    void buildNetwork() {
        network.setTimeStep(0.5);

        // Make reservoir
        reservoir = createReservoir(network, 10, 10, NUM_NEURONS);
        reservoir.setLabel("Reservoir");

        // Connect reservoir
        sgReservoir = connectReservoir(network, reservoir);

        // Set up sensor nodes
        buildSensorNodes();

        // Use concurrent buffered update
        network.getUpdateManager().clear();
        network.getUpdateManager().addAction(ConcurrentBufferedUpdate
                .createConcurrentBufferedUpdate(network));
    }

    private void buildSensorNodes() {
        // Offset in pixels of input nodes to right of reservoir
        int offset = 310;

        // Sensor nodes
        sensorNodes = new NeuronGroup(network, 6);
        sensorNodes.setLocation(reservoir.getMaxX() + offset,
                reservoir.getMinY());
        sensorNodes.setLabel("Sensors");
        sensorNodes.setClamped(true);
        network.addGroup(sensorNodes);
        // Make custom connections from sensor nodes to upper-left and
        // lower-right quadrants of the reservoir network to ensure visually
        // distinct patterns.
        cheeseToRes = sensorConnections(sensorNodes, reservoir,
                new int[] { 0, 1, 2 }, .8, 1);
        network.addGroup(cheeseToRes);
        flowersToRes = sensorConnections(sensorNodes, reservoir,
                new int[] { 3, 4, 5 }, .8, 3);
        network.addGroup(flowersToRes);
    }

    static NeuronGroup createReservoir(Network parentNet, int x, int y,
            int numNeurons) {
        GridLayout layout = new GridLayout(GRID_SPACE, GRID_SPACE,
                (int) Math.sqrt(numNeurons));
        List<Neuron> neurons = new ArrayList<Neuron>(numNeurons);
        for (int i = 0; i < numNeurons; i++) {
            Neuron neuron = new Neuron(parentNet);
            BinaryRule thresholdUnit = new BinaryRule();
            thresholdUnit.setInputType(InputType.WEIGHTED);
            neuron.setUpdateRule(thresholdUnit);
            neurons.add(neuron);
        }
        NeuronGroup ng = new NeuronGroup(parentNet, neurons);
        parentNet.addGroup(ng);
        ng.setLayout(layout);
        ng.applyLayout(new Point2D.Double(x, y));
        return ng;
    }

    static SynapseGroup connectReservoir(Network parentNet,
            NeuronGroup res) {

        PolarizedRandomizer exRand = new PolarizedRandomizer(
                Polarity.EXCITATORY, ProbDistribution.NORMAL);
        PolarizedRandomizer inRand = new PolarizedRandomizer(
                Polarity.INHIBITORY, ProbDistribution.NORMAL);
        exRand.setParam1(0);
        exRand.setParam2(Math.sqrt(variance));
        inRand.setParam1(0);
        inRand.setParam2(Math.sqrt(variance));

        RadialSimpleConstrainedKIn con = new RadialSimpleConstrainedKIn(kIn,
                (int) (Math.sqrt(res.getNeuronList().size()) * GRID_SPACE / 2));
        SynapseGroup reservoir = SynapseGroup.createSynapseGroup(res, res, con,
                0.5, exRand, inRand);
        reservoir.setLabel("Recurrent Synapses");
        parentNet.addGroup(reservoir);

        reservoir.setUpperBound(200, Polarity.EXCITATORY);
        reservoir.setLowerBound(0, Polarity.EXCITATORY);
        reservoir.setLowerBound(-200, Polarity.INHIBITORY);
        reservoir.setUpperBound(0, Polarity.INHIBITORY);

        return reservoir;
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
