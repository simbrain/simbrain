package org.simbrain.custom_sims.simulations.edge_of_chaos;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.custom_sims.helper_classes.NetBuilder;
import org.simbrain.custom_sims.helper_classes.OdorWorldBuilder;
import org.simbrain.custom_sims.helper_classes.PlotBuilder;
import org.simbrain.network.connections.RadialSimple;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.neuron_update_rules.BinaryRule;
import org.simbrain.network.update_actions.ConcurrentBufferedUpdate;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.math.DecayFunctions.StepDecayFunction;
import org.simbrain.util.math.ProbDistributions.NormalDistribution;
import org.simbrain.util.math.ProbabilityDistribution;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.SmellSensor;

import javax.swing.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Demonstration of representational capacities of recurrent networks based on
 * Bertschinger, Nils, and Thomas Natschläger. "Real-time computation at the
 * edge of chaos in recurrent neural networks." Neural computation 16.7 (2004):
 * 1413-1436.
 */
public class EdgeOfChaos extends RegisteredSimulation {

    // TODO: Add more objects

    // Simulation Parameters
    int NUM_NEURONS = 120;
    static int GRID_SPACE = 25;
    // Since mean is 0, lower variance means lower average weight strength
    //  For 120 neurons: .01,.1, and > .4
    private double variance = .1;
    private int K = 4; // in-degree (num connections to each neuron)

    // References
    Network network;
    SynapseGroup sgReservoir, cheeseToRes, flowersToRes;
    NeuronGroup reservoir, sensorNodes;
    OdorWorldBuilder world;
    OdorWorldEntity mouse;

    @Override
    public void run() {

        // Clear workspace
        sim.getWorkspace().clearWorkspace();

        // Build network
        NetBuilder net = sim.addNetwork(5, 0, 443, 620, "Edge of Chaos");
        network = net.getNetwork();
        buildNetwork();

        // Projection plot
        PlotBuilder plot = sim.addProjectionPlot(451, 260, 412, 365, "PCA");
        sim.couple(net.getNetworkComponent(), reservoir, plot.getProjectionPlotComponent());

        // Odor world sim
        buildOdorWorld();

        // Set up control panel
        controlPanel();
    }

    private void controlPanel() {
        ControlPanel panel = ControlPanel.makePanel(sim, "Controller", 847, 10);
        JTextField tf_stdev = panel.addTextField("Weight stdev", "" + variance);
        panel.addButton("Update", () -> {

            // Update variance of weight strengths
            double new_variance = Double.parseDouble(tf_stdev.getText());
            for (Synapse synapse : sgReservoir.getAllSynapses()) {
                synapse.setStrength(synapse.getStrength() * (new_variance / variance));
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
        sgReservoir = connectReservoir(network, reservoir, variance, K);

        // Set up sensor nodes
        buildSensorNodes();

        // Use concurrent buffered update
        network.getUpdateManager().clear();
        network.getUpdateManager().addAction(ConcurrentBufferedUpdate.createConcurrentBufferedUpdate(network));
    }

    private void buildSensorNodes() {

        // Offset in pixels of input nodes to right of reservoir
        int offset = 310;

        // Sensor nodes
        sensorNodes = new NeuronGroup(network, 6);
        sensorNodes.setLocation(229, 561);
        sensorNodes.setLabel("Sensors");
        sensorNodes.setClamped(true);
        network.addGroup(sensorNodes);
        // Make custom connections from sensor nodes to upper-left and
        // lower-right quadrants of the reservoir network to ensure visually
        // distinct patterns.
        cheeseToRes = sensorConnections(sensorNodes, reservoir, new int[] {0, 1, 2}, .8, 1);
        network.addGroup(cheeseToRes);
        flowersToRes = sensorConnections(sensorNodes, reservoir, new int[] {3, 4, 5}, .8, 3);
        network.addGroup(flowersToRes);
    }

    static NeuronGroup createReservoir(Network parentNet, int x, int y, int numNeurons) {
        GridLayout layout = new GridLayout(GRID_SPACE, GRID_SPACE, (int) Math.sqrt(numNeurons));
        List<Neuron> neurons = new ArrayList<Neuron>(numNeurons);
        for (int i = 0; i < numNeurons; i++) {
            Neuron neuron = new Neuron(parentNet);
            BinaryRule thresholdUnit = new BinaryRule();
            neuron.setUpdateRule(thresholdUnit);
            neurons.add(neuron);
        }
        NeuronGroup ng = new NeuronGroup(parentNet, neurons);
        parentNet.addGroup(ng);
        ng.setLayout(layout);
        ng.applyLayout(new Point2D.Double(x, y));
        return ng;
    }

    public static SynapseGroup connectReservoir(Network parentNet, NeuronGroup res, double variance, int k) {

        ProbabilityDistribution exRand =
            NormalDistribution.builder()
                .polarity(Polarity.EXCITATORY)
                .mean(0)
                .standardDeviation(Math.sqrt(variance))
                .build();

        ProbabilityDistribution inRand =
            NormalDistribution.builder()
                .polarity(Polarity.INHIBITORY)
                .mean(0)
                .standardDeviation(Math.sqrt(variance))
                .build();

        RadialSimple con = new RadialSimple(parentNet, res.getNeuronList());
        con.setExcCons(k/2);
        con.setExcitatoryRadius((int) (Math.sqrt(res.getNeuronList().size()) * GRID_SPACE / 2));
        con.setInhCons(k/2);
        con.setInhibitoryRadius((int) (Math.sqrt(res.getNeuronList().size()) * GRID_SPACE / 2));
        con.setSelectMethod(RadialSimple.SelectionStyle.IN);

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
     * @param src            inputs
     * @param tar            reservoir
     * @param srcNodeIndices which input nodes to connect
     * @param sparsity       sparsity
     * @param quadrantNumber 1,2,3 or 4 from upper-left clockwise
     * @return the resulting synapse group
     */
    private SynapseGroup sensorConnections(NeuronGroup src, NeuronGroup tar, int[] srcNodeIndices, double sparsity, int quadrantNumber) {

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
                            Synapse syn = new Synapse(src.getNeuronListUnsafe().get(srcNodeIndices[j]), tarNList.get(ii));
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
        world = sim.addOdorWorld(440, 9, 413, 248, "Two Objects");
        world.getWorld().setObjectsBlockMovement(false);
        mouse = world.addAgent(165, 110, "Mouse");
        mouse.setHeading(90);

        // Set up world
        double dispersion = 65;
        OdorWorldEntity cheese1 = world.addEntity(40, 40, "Swiss.gif", new double[] {1, 0, 0, 0, 0, 0});
        OdorWorldEntity cheese2 = world.addEntity(60, 40, "Gouda.gif", new double[] {0, 1, 0, 0, 0, 0});
        OdorWorldEntity cheese3 = world.addEntity(80, 40, "Bluecheese.gif", new double[] {1, 0, 1, 0, 0, 0});
        OdorWorldEntity flower1 = world.addEntity(290, 40, "Pansy.gif", new double[] {0, 0, 0, 0, 0, 1});
        OdorWorldEntity flower2 = world.addEntity(310, 40, "Flax.gif", new double[] {0, 0, 0, 0, 0, 1});
        OdorWorldEntity flower3 = world.addEntity(330, 40, "Tulip.gif", new double[] {0, 0, 0, 0, 0, 1});
        cheese1.getSmellSource().setDispersion(dispersion);
        cheese2.getSmellSource().setDispersion(dispersion);
        cheese3.getSmellSource().setDispersion(dispersion);
        flower1.getSmellSource().setDispersion(dispersion);
        flower2.getSmellSource().setDispersion(dispersion);
        flower3.getSmellSource().setDispersion(dispersion);
        cheese1.getSmellSource().setDecayFunction(StepDecayFunction.create());
        cheese2.getSmellSource().setDecayFunction(StepDecayFunction.create());
        cheese3.getSmellSource().setDecayFunction(StepDecayFunction.create());
        flower1.getSmellSource().setDecayFunction(StepDecayFunction.create());
        flower2.getSmellSource().setDecayFunction(StepDecayFunction.create());
        flower3.getSmellSource().setDecayFunction(StepDecayFunction.create());

        // Couple agent to cheese and flower nodes
        sim.couple((SmellSensor) mouse.getSensor("Smell-Center"), sensorNodes);
    }

    public EdgeOfChaos(SimbrainDesktop desktop) {
        super(desktop);
    }

    public EdgeOfChaos() {
        super();
    }

    ;

    @Override
    public String getName() {
        return "Edge of Chaos (Embodied)";
    }

    @Override
    public EdgeOfChaos instantiate(SimbrainDesktop desktop) {
        return new EdgeOfChaos(desktop);
    }

}
