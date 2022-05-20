package org.simbrain.custom_sims.simulations.edge_of_chaos;

import org.simbrain.custom_sims.Simulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.connections.Direction;
import org.simbrain.network.connections.FixedDegree;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.core.SynapseGroup2;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.neuron_update_rules.BinaryRule;
import org.simbrain.plot.projection.ProjectionComponent;
import org.simbrain.util.decayfunctions.StepDecayFunction;
import org.simbrain.util.stats.ProbabilityDistribution;
import org.simbrain.util.stats.distributions.NormalDistribution;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.OdorWorldComponent;
import org.simbrain.world.odorworld.entities.EntityType;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.SmellSensor;

import javax.swing.*;
import java.awt.geom.Point2D;
import java.util.List;

/**
 * Demonstration of representational capacities of recurrent networks based on
 * Bertschinger, Nils, and Thomas Natschläger. "Real-time computation at the
 * edge of chaos in recurrent neural networks." Neural computation 16.7 (2004):
 * 1413-1436.
 */
public class EdgeOfChaos extends Simulation {

    // TODO: Add more objects

    // Simulation Parameters
    int NUM_NEURONS = 120;
    static int GRID_SPACE = 25;
    // Since mean is 0, lower variance means lower average weight strength
    //  For 120 neurons: .01,.1, and > .4
    private double variance = .1;
    private int K = 4; // in-degree (num connections to each neuron)

    // References
    Network net;
    SynapseGroup2 sgReservoir, cheeseToRes, flowersToRes;
    NeuronGroup reservoir, sensorNodes;
    OdorWorldComponent oc;
    OdorWorldEntity mouse;

    @Override
    public void run() {

        // Clear workspace
        sim.getWorkspace().clearWorkspace();

        // Build network
        NetworkComponent nc = sim.addNetwork(5, 0, 443, 620, "Edge of Chaos");
        net = nc.getNetwork();
        buildNetwork();

        // Projection plot
        ProjectionComponent pc = sim.addProjectionPlot(447,248,412,372, "PCA");
        sim.couple(reservoir, pc);

        // Odor world sim
        buildOdorWorld();

        // Set up control panel
        controlPanel();
    }

    private void controlPanel() {
        ControlPanel panel = ControlPanel.makePanel(sim, "Controller", 844,2,215,133);
        JTextField tf_stdev = panel.addTextField("Weight stdev", "" + variance);
        panel.addButton("Update", () -> {

            // Update variance of weight strengths
            double new_variance = Double.parseDouble(tf_stdev.getText());
            for (Synapse synapse : sgReservoir.getSynapses()) {
                synapse.setStrength(synapse.getStrength() * (new_variance / variance));
            }
            variance = new_variance;
        });
    }

    void buildNetwork() {
        net.setTimeStep(0.5);

        // Make reservoir
        reservoir = createReservoir(net, 10, 10, NUM_NEURONS);
        reservoir.setLabel("Reservoir");

        // Connect reservoir
        sgReservoir = connectReservoir(net, reservoir, variance, K);

        // Set up sensor nodes
        buildSensorNodes();

        // // Use concurrent buffered update
        // net.getUpdateManager().clear();
        // net.getUpdateManager().addAction(ConcurrentBufferedUpdate.createConcurrentBufferedUpdate(net));
    }

    private void buildSensorNodes() {

        // Offset in pixels of input nodes to right of reservoir
        int offset = 310;

        // Sensor nodes
        sensorNodes = new NeuronGroup(net, 6);
        sensorNodes.setLabel("Sensors");
        // sensorNodes.setClamped(true);
        net.addNetworkModel(sensorNodes);
        sensorNodes.setLocation(229, 561);
        // Make custom connections from sensor nodes to upper-left and
        // lower-right quadrants of the reservoir network to ensure visually
        // distinct patterns.
        cheeseToRes = sensorConnections(sensorNodes, reservoir, new int[] {0, 1, 2}, .8, 1);
        net.addNetworkModel(cheeseToRes);
        flowersToRes = sensorConnections(sensorNodes, reservoir, new int[] {3, 4, 5}, .8, 3);
        net.addNetworkModel(flowersToRes);
    }

    static NeuronGroup createReservoir(Network parentNet, int x, int y, int numNeurons) {
        GridLayout layout = new GridLayout(GRID_SPACE, GRID_SPACE, (int) Math.sqrt(numNeurons));
        NeuronGroup ng = new NeuronGroup(parentNet, numNeurons);
        BinaryRule thresholdUnit = new BinaryRule();
        ng.setPrototypeRule(thresholdUnit);
        parentNet.addNetworkModel(ng);

        ng.setLayout(layout);
        ng.applyLayout(new Point2D.Double(x, y));
        return ng;
    }

    public static SynapseGroup2 connectReservoir(Network parentNet, NeuronGroup res, double variance, int k) {

        ProbabilityDistribution exRand = new NormalDistribution(0.0, Math.sqrt(variance));
        ProbabilityDistribution inRand =  new NormalDistribution(0.0, Math.sqrt(variance));

        FixedDegree con = new FixedDegree();
        con.setDegree(k);
        con.setDirection(Direction.IN);

        SynapseGroup2 reservoir = new SynapseGroup2(res, res, con);
        // TODO:  0.5, exRand, inRand);
        reservoir.setLabel("Recurrent Synapses");
        parentNet.addNetworkModel(reservoir);

        // TODO
        // reservoir.setUpperBound(200, Polarity.EXCITATORY);
        // reservoir.setLowerBound(0, Polarity.EXCITATORY);
        // reservoir.setLowerBound(-200, Polarity.INHIBITORY);
        // reservoir.setUpperBound(0, Polarity.INHIBITORY);

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
    private SynapseGroup2 sensorConnections(NeuronGroup src, NeuronGroup tar, int[] srcNodeIndices, double sparsity,
                                            int quadrantNumber) {

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

        SynapseGroup2 src2Res = new SynapseGroup2(src, tar);
        List<Neuron> tarNList = tar.getNeuronList();
        for (int ii = 0; ii < tar.size(); ++ii) {
            double x = tarNList.get(ii).getX();
            double y = tarNList.get(ii).getY();
            if ((y >= yStart) && (y < yEnd)) {
                if ((x >= xStart) && (x < xEnd)) {
                    for (int j = 0; j < srcNodeIndices.length; j++) {
                        if (Math.random() < sparsity) {
                            Synapse syn = new Synapse(src.getNeuronList().get(srcNodeIndices[j]), tarNList.get(ii));
                            syn.setStrength(1);
                            src2Res.addSynapse(syn);
                        }
                    }
                }
            }
        }
        return src2Res;
    }

    private void buildOdorWorld() {

        // Create the odor world
        oc = sim.addOdorWorld(447,1,413,248, "Two objects");
        oc.getWorld().setObjectsBlockMovement(false);
        oc.getWorld().setUseCameraCentering(false);
        mouse = oc.getWorld().addEntity(165, 110, EntityType.MOUSE);
        mouse.setHeading(90);

        // Set up world
        double dispersion = 65;
        OdorWorldEntity cheese1 = oc.getWorld().addEntity(40, 40,EntityType.SWISS, new double[] {1, 0, 0, 0, 0, 0});
        OdorWorldEntity cheese2 = oc.getWorld().addEntity(60, 40,EntityType.GOUDA, new double[] {0, 1, 0, 0, 0, 0});
        OdorWorldEntity cheese3 = oc.getWorld().addEntity(80, 40, EntityType.BLUECHEESE, new double[] {1, 0, 1, 0, 0,
                0});
        OdorWorldEntity flower1 = oc.getWorld().addEntity(290, 40,EntityType.PANSY, new double[] {0, 0, 0, 0, 0, 1});
        OdorWorldEntity flower2 = oc.getWorld().addEntity(310, 40,EntityType.FLAX, new double[] {0, 0, 0, 0, 0, 1});
        OdorWorldEntity flower3 = oc.getWorld().addEntity(330, 40,EntityType.TULIP, new double[] {0, 0, 0, 0, 0, 1});
        cheese1.getSmellSource().setDispersion(dispersion);
        cheese2.getSmellSource().setDispersion(dispersion);
        cheese3.getSmellSource().setDispersion(dispersion);
        flower1.getSmellSource().setDispersion(dispersion);
        flower2.getSmellSource().setDispersion(dispersion);
        flower3.getSmellSource().setDispersion(dispersion);
        cheese1.getSmellSource().setDecayFunction(new StepDecayFunction());
        cheese2.getSmellSource().setDecayFunction(new StepDecayFunction());
        cheese3.getSmellSource().setDecayFunction(new StepDecayFunction());
        flower1.getSmellSource().setDecayFunction(new StepDecayFunction());
        flower2.getSmellSource().setDecayFunction(new StepDecayFunction());
        flower3.getSmellSource().setDecayFunction(new StepDecayFunction());

        // Couple agent to cheese and flower nodes
        SmellSensor smellSensor = new SmellSensor(mouse);
        mouse.addSensor(smellSensor);
        sim.couple(smellSensor, sensorNodes);
    }

    public EdgeOfChaos(SimbrainDesktop desktop) {
        super(desktop);
    }

    public EdgeOfChaos() {
        super();
    }

    private String getSubmenuName() {
        return "Chaos";
    }

    @Override
    public String getName() {
        return "Edge of Chaos (Embodied)";
    }

    @Override
    public EdgeOfChaos instantiate(SimbrainDesktop desktop) {
        return new EdgeOfChaos(desktop);
    }

}
