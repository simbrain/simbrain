package org.simbrain.custom_sims.simulations.cortex_simple;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.custom_sims.helper_classes.NetBuilder;
import org.simbrain.network.connections.Sparse;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.neuron_update_rules.IntegrateAndFireRule;
import org.simbrain.network.update_actions.ConcurrentBufferedUpdate;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.math.ProbDistribution;
import org.simbrain.util.randomizer.PolarizedRandomizer;
import org.simbrain.workspace.gui.SimbrainDesktop;

/**
 * Model of canonical cortex (Douglas and Martin, 2004) using rat barrel cortex
 * as a reference (Lefort, Tomm, Sarria and Petersen, 2009). Users should be
 * able to inject current and see it propagate consistently with empirical
 * studies.
 * 
 * Also see Haeusler and Mass, 2007.
 * 
 * @author Zach Tosi
 * @author Jeff Yoshimi
 */
public class CortexSimple extends RegisteredSimulation {

    // Simulation Parameters
    int NUM_NEURONS = 120;
    int GRID_SPACE = 25;
    // TODO: Membrane properties
    // TODO: Build using z coordinates

    // References
    Network network;

    @Override
    public void run() {

        // Clear workspace
        sim.getWorkspace().clearWorkspace();

        // Build network
        NetBuilder net = sim.addNetwork(10, 10, 550, 800,
                "Cortical Simulation");
        network = net.getNetwork();
        buildNetwork();

        // Set up control panel
        //controlPanel();
    }

    private void controlPanel() {
        ControlPanel panel = ControlPanel.makePanel(sim, "Controller", 5, 10);
        panel.addButton("Inject current", () -> {
        });
    }

    void buildNetwork() {

        network.setTimeStep(0.5);

        // Make the layers
        int btwnLayerSpacing = 150;
        NeuronGroup layer_23 = buildLayer(10, 10, 100);
        layer_23.setLabel("Layer 2/3");
        NeuronGroup layer_4 = buildLayer(-500,
                (int) layer_23.getMaxY() + btwnLayerSpacing, 100);
        layer_4.setLabel("Layer 4");
        NeuronGroup layer_56 = buildLayer(10,
                (int) layer_4.getMaxY() + btwnLayerSpacing, 100);
        layer_56.setLabel("Layer 5/6");

        // Connect layers
        SynapseGroup rec_23 = connectLayers(layer_23, layer_23, .12);
        SynapseGroup rec_4 = connectLayers(layer_4, layer_4, .24);
        SynapseGroup rec_56 = connectLayers(layer_56, layer_56, .24);
        SynapseGroup sg_4_23 = connectLayers(layer_4, layer_23, .14);
        SynapseGroup sg_23_4 = connectLayers(layer_23, layer_4, .01);
        SynapseGroup sg_4_56 = connectLayers(layer_4, layer_56, .08);
        SynapseGroup sg_56_4 = connectLayers(layer_56, layer_4, .007);
        SynapseGroup sg_23_56 = connectLayers(layer_23, layer_56, .08);
        SynapseGroup sg_56_23 = connectLayers(layer_56, layer_23, .03);
        // Todo; Add labels
        
        // Use concurrent buffered update
        network.getUpdateManager().clear();
        network.getUpdateManager().addAction(ConcurrentBufferedUpdate
                .createConcurrentBufferedUpdate(network));
    }

    private NeuronGroup buildLayer(int x, int y, int numNeurons) {

        GridLayout layout = new GridLayout(GRID_SPACE, GRID_SPACE,
                (int) Math.sqrt(numNeurons));
        List<Neuron> neurons = new ArrayList<Neuron>(numNeurons);
        for (int i = 0; i < numNeurons; i++) {
            Neuron neuron = new Neuron(network);
            IntegrateAndFireRule rule = new IntegrateAndFireRule();
            rule.setResistance(200);
            rule.setTimeConstant(30);
            rule.setBackgroundCurrent(0);
            neuron.setUpdateRule(rule);
            neuron.setUpperBound(50); // For wand
            neurons.add(neuron);
        }
        NeuronGroup ng = new NeuronGroup(network, neurons);
        network.addGroup(ng);
        ng.setLayout(layout);
        ng.applyLayout(new Point2D.Double(x,y));
        return ng;
    }

    private SynapseGroup connectLayers(NeuronGroup src, NeuronGroup tar,
            double sparsity) {

        PolarizedRandomizer exRand = new PolarizedRandomizer(
                Polarity.EXCITATORY, ProbDistribution.LOGNORMAL);
        PolarizedRandomizer inRand = new PolarizedRandomizer(
                Polarity.INHIBITORY, ProbDistribution.LOGNORMAL);

        Sparse con = new Sparse(sparsity, false, false);

        SynapseGroup sg = SynapseGroup.createSynapseGroup(src, tar, con, 0.5,
                exRand, inRand);
        sg.setLabel("Synapses");

        sg.setUpperBound(1, Polarity.EXCITATORY);
        sg.setLowerBound(0, Polarity.EXCITATORY);
        sg.setLowerBound(-1, Polarity.INHIBITORY);
        sg.setUpperBound(0, Polarity.INHIBITORY);

        network.addGroup(sg);

        return sg;

    }

    public CortexSimple(SimbrainDesktop desktop) {
        super(desktop);
    }

    public CortexSimple() {
        super();
    };

    @Override
    public String getName() {
        return "Cortical circuit";
    }

    @Override
    public CortexSimple instantiate(SimbrainDesktop desktop) {
        return new CortexSimple(desktop);
    }

}
