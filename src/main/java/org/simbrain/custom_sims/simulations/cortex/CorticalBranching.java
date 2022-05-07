package org.simbrain.custom_sims.simulations.cortex;

import org.simbrain.custom_sims.Simulation;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.connections.Direction;
import org.simbrain.network.connections.FixedDegree;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.core.SynapseGroup2;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.layouts.HexagonalGridLayout;
import org.simbrain.network.neuron_update_rules.TimedAccumulatorRule;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.stats.ProbabilityDistribution;
import org.simbrain.util.stats.distributions.NormalDistribution;
import org.simbrain.workspace.gui.SimbrainDesktop;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class CorticalBranching extends Simulation {

    // Simulation Parameters
    int NUM_NEURONS = 1024; // 4096
    int GRID_SPACE = 25;
    int RADIUS = 200; // 200
    int KIN = 40; // In-degree. 41
    int REFRACTORY = 10;
    double SPONTANEOUS_ACT = 1E-4; // 10E-5
    double KAPPA = 1; // 0.9
    double B_VALUE = 1.5;

    private Network net;

    public CorticalBranching(SimbrainDesktop desktop) {
        super(desktop);
    }


    @Override
    public void run() {
        sim.getWorkspace().clearWorkspace();
        NetworkComponent nc = sim.addNetwork(100, 50, 600, 600, "Cortical Branching");
        net = nc.getNetwork();
        buildNetwork();
        // net.getUpdateManager().clear();
        // net.getUpdateManager().addAction(ConcurrentBufferedUpdate.createConcurrentBufferedUpdate(net));
    }

    private void buildNetwork() {
        HexagonalGridLayout layout = new HexagonalGridLayout(GRID_SPACE, GRID_SPACE, (int) Math.sqrt(NUM_NEURONS));
        layout.setInitialLocation(new Point(10, 10));
        List<Neuron> neurons = new ArrayList<Neuron>(NUM_NEURONS);
        List<Neuron> outNeurons = new ArrayList<Neuron>(NUM_NEURONS);
        for (int i = 0; i < NUM_NEURONS; i++) {
            Neuron neuron = new Neuron(net);
            neuron.setPolarity(SimbrainConstants.Polarity.EXCITATORY);
            TimedAccumulatorRule tar = new TimedAccumulatorRule();
            tar.setMaxState(REFRACTORY);
            tar.setKappa(KAPPA);
            tar.setB(B_VALUE);
            tar.setBaseProb(SPONTANEOUS_ACT);
            neuron.setUpdateRule(tar);
            neurons.add(neuron);
        }
        NeuronGroup ng1 = new NeuronGroup(net, neurons);
        ng1.setLabel("CorticalBranching");
        net.addNetworkModel(ng1);
        ng1.setLayout(layout);
        ng1.applyLayout(new Point2D.Double(0.0 ,0.0));

        ProbabilityDistribution exRand = new NormalDistribution(1.5, .5);
        ProbabilityDistribution inRand = new NormalDistribution(-1.5, 3.0);

        FixedDegree con = new FixedDegree();
        con.setDirection(Direction.IN);
        con.setDegree(KIN);
        con.setUseRadius(true);
        con.setRadius(RADIUS);

        SynapseGroup2 sg = SynapseGroup.createSynapseGroup(ng1, ng1, con);
            // sg.setRandomizers(exRand, inRand);
            // sg.setConnectionManager(con);
            // sg.makeConnections();

        // TODO: Band-aid... issue with synapse bounds needs addressing
        for(Synapse s : sg.getAllSynapses()) {
            s.setUpperBound(10000);
            s.forceSetStrength(exRand.sampleDouble());
        }

        sg.setLabel("Recurrent Synapses");
        net.addNetworkModel(sg);

    }

    public CorticalBranching() {
        super();
    }

    private String getSubmenuName() {
        return "Brain";
    }

    @Override
    public String getName() {
        return "Cortical Branching";
    }

    @Override
    public CorticalBranching instantiate(SimbrainDesktop desktop) {
        return new CorticalBranching(desktop);
    }
}