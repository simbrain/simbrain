package org.simbrain.custom_sims.simulations.mpfs_som;

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
import org.simbrain.network.subnetworks.SOMNetwork;
import org.simbrain.network.update_actions.ConcurrentBufferedUpdate;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.math.ProbDistribution;
import org.simbrain.util.randomizer.PolarizedRandomizer;
import org.simbrain.workspace.gui.SimbrainDesktop;

/**
 * Self-organizing map represents the moral political family scale...
 *
 * @author Karie Moorman
 * @author Jeff Yoshimi
 */
public class MpfsSOM extends RegisteredSimulation {


    // References
    Network network;

    @Override
    public void run() {

        // Clear workspace
        sim.getWorkspace().clearWorkspace();

        // Build network
        NetBuilder net = sim.addNetwork(10, 10, 550, 800,
                "MPFS SOM");
        network = net.getNetwork();
        buildNetwork();

        // Set up control panel
        // controlPanel();
    }

    private void controlPanel() {
        ControlPanel panel = ControlPanel.makePanel(sim, "Controller", 5, 10);
        panel.addButton("Test", () -> {
            System.out.println("Test");
        });
    }

    void buildNetwork() {
        // Basic setup
        NetBuilder net = sim.addNetwork(10, 10, 450, 450, "Test network");
        Network network = net.getNetwork();
        network.addGroup(
                new SOMNetwork(network, 20, 29, new Point2D.Double(0, 0)));

    }

    public MpfsSOM(SimbrainDesktop desktop) {
        super(desktop);
    }

    public MpfsSOM() {
        super();
    };

    @Override
    public String getName() {
        return "Moral Political Family Scale SOM";
    }

    @Override
    public MpfsSOM instantiate(SimbrainDesktop desktop) {
        return new MpfsSOM(desktop);
    }

}
