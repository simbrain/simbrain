package org.simbrain.custom_sims.simulations.edge_of_chaos;

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
public class EdgeOfChaos extends RegisteredSimulation {


    public EdgeOfChaos(){super();};
    
    /**
     * @param desktop
     */
    public EdgeOfChaos(SimbrainDesktop desktop) {
        super(desktop);
    }

    /**
     * Run the simulation!
     */
    public void run() {

        // Clear workspace
        sim.getWorkspace().clearWorkspace();

        // Basic setup
        NetBuilder net = sim.addNetwork(10, 10, 450, 450, "Edge of Chaos");
        Network network = net.getNetwork();


        // Simulation Parameters
        int NUM_NEURONS = 130; 
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
        return "Edge of Chaos";
    }

    @Override
    public EdgeOfChaos instantiate(SimbrainDesktop desktop) {
        return new EdgeOfChaos(desktop);
    }

}
