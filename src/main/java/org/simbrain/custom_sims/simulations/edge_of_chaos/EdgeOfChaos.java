package org.simbrain.custom_sims.simulations.edge_of_chaos;

import org.simbrain.custom_sims.Simulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.connections.Direction;
import org.simbrain.network.connections.FixedDegree;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.core.SynapseGroup;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.neurongroups.NeuronGroup;
import org.simbrain.network.updaterules.BinaryRule;
import org.simbrain.plot.projection.ProjectionComponent;
import org.simbrain.util.decayfunctions.StepDecayFunction;
import org.simbrain.util.projection.DecayColoringManager;
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

    // Simulation Parameters
    int NUM_NEURONS = 120;
    static int GRID_SPACE = 25;
    //  For 120 neurons: .01,.1, and > .4
    private double variance = .1;
    private final int K = 4; // in-degree (num connections to each neuron)

    private final long seed = 42L;

    Network net;
    SynapseGroup sgReservoir, cheeseToRes, flowersToRes;
    NeuronGroup reservoir, sensorNodes;
    OdorWorldComponent oc;
    OdorWorldEntity mouse;

    @Override
    public void run() {

        // Clear workspace
        sim.getWorkspace().clearWorkspace();

        // Build network
        NetworkComponent nc = sim.addNetwork(0, 0, 593, 620, "Edge of Chaos");
        net = nc.getNetwork();
        buildNetwork();

        // Projection plot
        ProjectionComponent pc = sim.addProjectionPlot(592, 248, 413, 372, "PCA");
        pc.getProjector().setColoringManager(new DecayColoringManager());
        sim.couple(reservoir, pc);

        sim.addSidebarInfo(
"""
        # Introduction
        
        This is simulation is an experimental study of how representations work when a network is in different dynamic regimes. See the `Edge Of Chaos
        bitstream` simulation to learn more about the different three different types of dynamical regimes: `chaos`, `edge of chaos`, `ordered`. The main goal of
        this simulation, similar to the other `Edge Of Chaos` simulation, is to find the edge of chaos and see the effects of the reservoir network on an agent's
        representation of an object. Please note, we have not finished studying this network so if you find any patterns or structure, let us know!
        
        ## Simulation Details
        
        In this simulation, a reservoir network is connected to an agent that exists in the 2D environment. In the 2D environment, there is two different
        groups of objects, flowers and cheeses. There are three different types of each object that will be utilized to interact with the agent. In principle,
        the three types have similar representations due to similarities in their structure. The object groups (i.e., cheese or flower) would project to different
        parts of the network. The objects would be moved to the agent and the object would be represented in a PCA plot as points where we can infer similarities 
        or differences between objects. Similarities and differences can be inferred from the position of the object's representation in the PCA plot.
         
        # What To Do
        
        In this simulation, the only configuration to the simulation is the `weight stdev`. To find each state, follow the steps below.
        
        1) Change the `weight stdev` value and press the `update` button to change the reservoir's responses to the object, which will be shown in the PCA plot.
                        
        2) Start the simulation by clicking on the `play` button in the top-left corner.
        
        3) Click on the `cursor` icon, drag one of the six objects to the agent.
        
        4) Observe changes in the reservoir's representation in the PCA plot.   
        
        5) To `reset` the simulation, stop the simulation by clicking the `play` button again and press `k`.
        
        6) Afterwards, click back on the `cursor` icon, and left-click outside of the reservoirs to unselect all neurons.
 
        ## Observing The Representations Of The Objects
        
        To observe the object's representation in the network, delete the recurrent connections (the recurrent synapses) by
        clicking on it and backspace. Then run the simulation and repeat steps 2 to 6.

        """, true);

        // Odor world sim
        buildOdorWorld();

        // Set up control panel
        controlPanel();
    }

    private void controlPanel() {
        ControlPanel panel = ControlPanel.makePanel(sim, "Controller", 1005, 0, 215, 133);
        JTextField tf_stdev = panel.addTextField("Weight stdev", "" + variance);
        panel.addButton("Update", () -> {

            // Update variance of weight strengths
            variance = Double.parseDouble(tf_stdev.getText());
            var normalDist = new NormalDistribution(0.0, variance);
            sgReservoir.randomize(normalDist);
        });
    }

    void buildNetwork() {
        net.setTimeStep(0.5);

        // Make reservoir
        reservoir = createReservoir(net, 10, 10, NUM_NEURONS);
        reservoir.setLabel("Reservoir");

        // Connect reservoir
        sgReservoir = connectReservoir(net, reservoir, variance, K, seed);

        // Set up sensor nodes
        buildSensorNodes();
    }

    private void buildSensorNodes() {

        // Sensor nodes
        sensorNodes = new NeuronGroup(6);
        sensorNodes.setLabel("Sensors");
        // sensorNodes.setClamped(true);
        net.addNetworkModel(sensorNodes);
        sensorNodes.setLocation(229, 561);
        // Make custom connections from sensor nodes to upper-left and
        // lower-right quadrants of the reservoir network to ensure visually
        // distinct patterns.
        cheeseToRes = createSensorConnections(sensorNodes, reservoir, new int[] {0, 1, 2}, .6, 1);
        net.addNetworkModel(cheeseToRes);
        flowersToRes = createSensorConnections(sensorNodes, reservoir, new int[] {3, 4, 5}, .6, 3);
        net.addNetworkModel(flowersToRes);
        sensorNodes.applyLayout();
    }

    public static NeuronGroup createReservoir(Network parentNet, int x, int y, int numNeurons) {
        GridLayout layout = new GridLayout(GRID_SPACE, GRID_SPACE, (int) Math.sqrt(numNeurons));
        NeuronGroup ng = new NeuronGroup(numNeurons);
        BinaryRule thresholdUnit = new BinaryRule();
        ng.setUpdateRule(thresholdUnit);
        parentNet.addNetworkModel(ng);

        ng.setLayout(layout);
        ng.applyLayout(new Point2D.Double(x, y));
        return ng;
    }

    public static SynapseGroup connectReservoir(Network parentNet, NeuronGroup res, double variance, int k, long seed) {

        FixedDegree con = new FixedDegree(k, Direction.IN, false, 20.0, false, seed);

        SynapseGroup reservoir = new SynapseGroup(res, res, con);
        reservoir.setLabel("Recurrent Synapses");
        parentNet.addNetworkModel(reservoir);

        return reservoir;
    }

    // Possibly export this to a utility class

    /**
     * Creates connections between a specified set of source neurons and a designated quadrant
     * of the target neuron group (reservoir). The connections are created with a specified
     * sparsity level, and only neurons within the given quadrant are considered as targets.
     *
     * @param sourceGroup the group of source neurons to connect from
     * @param targetGroup the group of target neurons (reservoir) to connect to
     * @param sourceNodeIndices an array of indices specifying which neurons in the source group to connect
     * @param sparsity the probability of creating a connection between a source neuron and a target neuron (0.0 to 1.0)
     * @param quadrant the target quadrant of the reservoir (1: upper-left, 2: upper-right, 3: lower-right, 4: lower-left)
     * @return a {@link SynapseGroup} representing the newly created connections
     */

    private SynapseGroup createSensorConnections(
            NeuronGroup sourceGroup, NeuronGroup targetGroup, int[] sourceNodeIndices,
            double sparsity, int quadrant) {

        // Define quadrant boundaries
        double xStart, xEnd, yStart, yEnd;

        if (quadrant < 3) { // Top quadrants
            yStart = targetGroup.getMinY();
            yEnd = targetGroup.getCenterY();
        } else { // Bottom quadrants
            yStart = targetGroup.getCenterY();
            yEnd = targetGroup.getMaxY();
        }

        if (quadrant == 1 || quadrant == 4) { // Left quadrants
            xStart = targetGroup.getMinX();
            xEnd = targetGroup.getCenterX();
        } else { // Right quadrants
            xStart = targetGroup.getCenterX();
            xEnd = targetGroup.getMaxX();
        }

        // Create a new SynapseGroup
        SynapseGroup synapseGroup = new SynapseGroup(sourceGroup, targetGroup);

        synapseGroup.removeAllSyapsesBlocking();

        // Iterate over neurons in the target group
        List<Neuron> targetNeurons = targetGroup.getNeuronList();
        for (Neuron targetNeuron : targetNeurons) {
            double x = targetNeuron.getX();
            double y = targetNeuron.getY();

            // Check if the neuron lies within the specified quadrant
            if (x >= xStart && x < xEnd && y >= yStart && y < yEnd) {
                // Connect the source neurons to this target neuron
                for (int sourceIndex : sourceNodeIndices) {
                    if (Math.random() < sparsity) {
                        Neuron sourceNeuron = sourceGroup.getNeuronList().get(sourceIndex);
                        Synapse synapse = new Synapse(sourceNeuron, targetNeuron);
                        synapse.setStrength(1.0); // Default strength
                        synapseGroup.addSynapse(synapse);
                    }
                }
            }
        }

        return synapseGroup;
    }
    private void buildOdorWorld() {

        // Create the odor world
        oc = sim.addOdorWorld(592, 0, 413, 248, "Two objects");
        oc.getWorld().setObjectsBlockMovement(false);
        oc.getWorld().setUseCameraCentering(false);
        mouse = oc.getWorld().addEntity(165, 110, EntityType.Mouse.INSTANCE);
        mouse.setHeading(90);

        // Set up world
        double dispersion = 65;
        OdorWorldEntity cheese1 = oc.getWorld().addEntity(40, 40,EntityType.Swiss.INSTANCE, new double[] {1, 0, 0, 0, 0, 0});
        OdorWorldEntity cheese2 = oc.getWorld().addEntity(60, 40,EntityType.Gouda.INSTANCE, new double[] {0, 1, 0, 0, 0, 0});
        OdorWorldEntity cheese3 = oc.getWorld().addEntity(80, 40, EntityType.BlueCheese.INSTANCE, new double[] {0, 0, 1, 0, 0, 0});
        OdorWorldEntity flower1 = oc.getWorld().addEntity(290, 40,EntityType.Pansy.INSTANCE, new double[] {0, 0, 0, 0, 0, 1});
        OdorWorldEntity flower2 = oc.getWorld().addEntity(310, 40,EntityType.Flax.INSTANCE, new double[] {0, 0, 0, 0, 1, 0});
        OdorWorldEntity flower3 = oc.getWorld().addEntity(330, 40,EntityType.Tulip.INSTANCE, new double[] {0, 0, 0, 1, 0, 0});
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
        SmellSensor smellSensor = new SmellSensor();
        mouse.addSensor(smellSensor);
        sim.couple(smellSensor, sensorNodes);
    }

    public EdgeOfChaos(SimbrainDesktop desktop) {
        super(desktop);
    }

    public EdgeOfChaos() {
        super();
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
