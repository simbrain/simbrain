package org.simbrain.custom_sims.simulations.test;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.NetBuilder;
import org.simbrain.network.connections.RadialSimpleConstrainedKIn;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule.InputType;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.layouts.HexagonalGridLayout;
import org.simbrain.network.neuron_update_rules.TimedAccumulatorRule;
import org.simbrain.network.update_actions.ConcurrentBufferedUpdate;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.math.ProbDistribution;
import org.simbrain.util.randomizer.PolarizedRandomizer;
import org.simbrain.workspace.gui.SimbrainDesktop;

/**
 * Playground for testing new features. A lot of stuff is commented out but
 * should work.
 */
public class TestSim extends RegisteredSimulation {


    public TestSim(){super();};
    
    /**
     * @param desktop
     */
    public TestSim(SimbrainDesktop desktop) {
        super(desktop);
    }

    /**
     * Run the simulation!
     */
    public void run() {

        // Clear workspace
        sim.getWorkspace().clearWorkspace();

        // Basic setup
        NetBuilder net = sim.addNetwork(10, 10, 450, 450, "Test network");
        Network network = net.getNetwork();

        // Graveyeard of old tests below

        // nb1.addNeurons(0, 0, 20, "horizontal line", "LinearRule");
        // nb1.addNeurons(0, 89, 20, "vertical line", "LinearRule");
        // nb1.addNeurons(89, 89, 49, "grid", "LinearRule");
        // NeuronGroup inputs = net1.addNeuronGroup(0, 300, 6, "horizontal
        // line",
        // "DecayRule");
        // inputs.setLabel("Inputs");
        // NeuronGroup outputs = net1.addNeuronGroup(0, 0, 3, "horizontal line",
        // "DecayRule");
        // outputs.setUpperBound(10);
        // outputs.setLabel("Outputs");
        // // nb1.connectAllToAll(inputs, outputs);
        // SynapseGroup in_out = net1.addSynapseGroup(inputs, outputs);
        // in_out.setExcitatoryRatio(.5);
        // in_out.randomizeConnectionWeights(); // TODO Not working?

        // Create the odor world
        // OdorWorldBuilder world = sim.addOdorWorld(460, 10, 450, 450,
        // "My first world");
        // world.getWorld().setObjectsBlockMovement(false);
        // RotatingEntity mouse = world.addAgent(20, 20, "Mouse");
        // RotatingEntity mouse2 = world.addAgent(200, 200, "Mouse");
        // RotatingEntity mouse3 = world.addAgent(400, 200, "Mouse");
        //
        // OdorWorldEntity cheese = world.addEntity(150, 150, "Swiss.gif",
        // new double[] { 0, 1, 0, 0 });
        // cheese.getSmellSource().setDispersion(200);

        // Coupling agent to network
        // sim.couple(mouse, inputs); // Agent sensors to neurons
        // sim.couple(outputs, mouse); // Neurons to movement effectors

        // Add vehicles
        // Vehicle vehicleBuilder = new Vehicle(sim, net, world);
        // vehicleBuilder.setWeightSize(10);
        // NeuronGroup pursuer1 = vehicleBuilder.addPursuer(0, 400, mouse, 1);
        // pursuer1.setLabel("Pursuer 1");
        // NeuronGroup pursuer2 = vehicleBuilder.addPursuer(240, 400, mouse2,
        // 1);
        // pursuer2.setLabel("Pursuer 2");
        // NeuronGroup avoider1 = vehicleBuilder.addAvoider(480, 400, mouse3,
        // 1);
        // avoider1.setLabel("Avoider 1");

        //
        // Cortical Branching simulation (for comparison with beanshell script).
        //

        // Simulation Parameters
        int NUM_NEURONS = 1024; // 4096
        int GRID_SPACE = 25;
        int RADIUS = 100; // 200
        int KIN = 10; // 41
        int REFRACTORY = 10;
        double SPONTANEOUS_ACT = 10E-5; // 10E-5
        double KAPPA = 0.9; // 0.9
        double B_VALUE = 1.6;

        // Build Network
        network.setTimeStep(0.5);
        HexagonalGridLayout layout = new HexagonalGridLayout(GRID_SPACE,
                GRID_SPACE, (int) Math.sqrt(NUM_NEURONS));
        layout.setInitialLocation(new Point(10, 10));
        List<Neuron> neurons = new ArrayList<Neuron>(NUM_NEURONS);
        List<Neuron> outNeurons = new ArrayList<Neuron>(NUM_NEURONS);
        for (int i = 0; i < NUM_NEURONS; i++) {
            Neuron neuron = new Neuron(network);
            neuron.setPolarity(Polarity.EXCITATORY);
            TimedAccumulatorRule tar = new TimedAccumulatorRule();
            tar.setInputType(InputType.WEIGHTED);
            tar.setMaxState(REFRACTORY);
            tar.setKappa(KAPPA);
            tar.setB(B_VALUE);
            tar.setBaseProb(SPONTANEOUS_ACT);
            neuron.setUpdateRule(tar);
            neurons.add(neuron);
        }
        NeuronGroup ng1 = new NeuronGroup(network, neurons);
        ng1.setLabel("CorticalBranching");
        network.addGroup(ng1);
        ng1.setLayout(layout);
        ng1.applyLayout(new Point2D.Double(0.0, 0.0));

        PolarizedRandomizer exRand = new PolarizedRandomizer(
                Polarity.EXCITATORY, ProbDistribution.LOGNORMAL);
        PolarizedRandomizer inRand = new PolarizedRandomizer(
                Polarity.INHIBITORY, ProbDistribution.UNIFORM);
        exRand.setParam1(2);
        exRand.setParam2(1);
        inRand.setParam1(1.5);
        inRand.setParam2(3);

        RadialSimpleConstrainedKIn con = new RadialSimpleConstrainedKIn(KIN,
                RADIUS);
        SynapseGroup sg = SynapseGroup.createSynapseGroup(ng1, ng1, con, 1.0,
                exRand, inRand);
        sg.setLabel("Recurrent Synapses");
        network.addGroup(sg);
        for (Neuron n : ng1.getNeuronList()) {
            ((TimedAccumulatorRule) n.getUpdateRule()).init(n);
        }

        // print(sg.size());

        sg.setUpperBound(200, Polarity.EXCITATORY);
        sg.setLowerBound(0, Polarity.EXCITATORY);
        sg.setLowerBound(-200, Polarity.INHIBITORY);
        sg.setUpperBound(0, Polarity.INHIBITORY);
        network.getUpdateManager().clear();
        network.getUpdateManager().addAction(ConcurrentBufferedUpdate
                .createConcurrentBufferedUpdate(network));

    }

    @Override
    public String getName() {
        return "Test Sim";
    }

    @Override
    public TestSim instantiate(SimbrainDesktop desktop) {
        return new TestSim(desktop);
    }

}
