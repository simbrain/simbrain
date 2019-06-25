package org.simbrain.custom_sims.helper_classes;

import org.simbrain.network.NetworkComponent;
import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.connections.OneToOne;
import org.simbrain.network.core.*;
import org.simbrain.network.desktop.NetworkDesktopComponent;
import org.simbrain.network.desktop.NetworkPanelDesktop;
import org.simbrain.network.groups.Group;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.nodes.NeuronNode;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.network.layouts.LineLayout.LineOrientation;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.network.subnetworks.WinnerTakeAll;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A wrapper for a {@link Network}, with access to the {@link NetworkComponent}, and {@link
 * org.simbrain.network.gui.NetworkPanel} which can be used to set the position of a window in the Simbrain desktop.
 * Helper methods for creating neurons, neuron groups, etc. are also included.
 */
public class NetworkWrapper {

    // TODO: Consider adding static methods for adding neurons, neuron groups, etc.


    private final NetworkDesktopComponent desktopComponent;

    private final NetworkComponent networkComponent;

    private final Network network;

    private double GRID_SPACE = 50; // todo; make this settable

    /**
     * Create an instance of the wrapper.
     */
    public NetworkWrapper(NetworkDesktopComponent desktopComponent) {
        this.desktopComponent = desktopComponent;
        this.networkComponent = desktopComponent.getWorkspaceComponent();
        this.network = networkComponent.getNetwork();
    }


    /**
     * Add a neuron at a specified location.
     */
    public Neuron addNeuron(int x, int y) {
        Neuron neuron = new Neuron(network, "LinearRule");
        neuron.setLocation(x, y);
        network.addNeuron(neuron);
        return neuron;
    }

    /**
     * Add neurons at a specified location with a specified layout.
     */
    public void addNeurons(int x, int y, int numNeurons, String layoutName, String type) {

        List<Neuron> newNeurons = new ArrayList();
        for (int i = 0; i < numNeurons; i++) {
            Neuron neuron = new Neuron(network, type);
            network.addNeuron(neuron);
            newNeurons.add(neuron);
        }

        // Lay out neurons
        if (layoutName.equalsIgnoreCase("none")) {
            return;
        }
        if (layoutName.toLowerCase().contains("line")) {
            if (layoutName.equalsIgnoreCase("vertical line")) {
                LineLayout lineLayout = new LineLayout(x, y, 50, LineOrientation.VERTICAL);
                lineLayout.layoutNeurons(newNeurons);

            } else {
                LineLayout lineLayout = new LineLayout(x, y, 50, LineOrientation.HORIZONTAL);
                lineLayout.layoutNeurons(newNeurons);
            }
        } else if (layoutName.equalsIgnoreCase("grid")) {
            GridLayout gridLayout = new GridLayout(GRID_SPACE, GRID_SPACE, (int) Math.sqrt(numNeurons));
            gridLayout.setInitialLocation(new Point(x, y));
            gridLayout.layoutNeurons(newNeurons);
        }
    }

    /**
     * Make a single source -> target connection.
     *
     * @param source     the source neuron
     * @param target     the target neuron
     * @param lowerBound lower bound for synapse
     * @param upperBound upper bound for synapse
     */
    public void connect(Neuron source, Neuron target, double value, double lowerBound, double upperBound) {
        Synapse synapse = new Synapse(source, target);
        synapse.setStrength(value);
        synapse.setLowerBound(lowerBound);
        synapse.setUpperBound(upperBound);
        source.getNetwork().addSynapse(synapse);
    }

    /**
     * Make a single source -> target connection.
     *
     * @param source the source neuron
     * @param target the target neuron
     */
    public Synapse connect(Neuron source, Neuron target, double value) {
        Synapse synapse = new Synapse(source, target);
        synapse.setStrength(value);
        source.getNetwork().addSynapse(synapse);
        return synapse;
    }

    public Synapse connect(Neuron source, Neuron target, SynapseUpdateRule rule, double value) {
        Synapse synapse = new Synapse(source, target, rule);
        synapse.setStrength(value);
        source.getNetwork().addSynapse(synapse);
        return synapse;
    }

    public void connectOneToOne(NeuronGroup source, NeuronGroup target) {
        OneToOne connector = new OneToOne();
        connector.connectOneToOne(source.getNeuronList(), target.getNeuronList());
    }

    public List<Synapse> connectAllToAll(NeuronGroup source, NeuronGroup target) {
        AllToAll connector = new AllToAll();
        return connector.connectAllToAll(source.getNeuronList(), target.getNeuronList());
    }

    public void connectAllToAll(NeuronGroup inputs, Neuron target) {
        AllToAll connector = new AllToAll();
        connector.connectAllToAll(inputs.getNeuronList(), Collections.singletonList(target));
    }

    public SynapseGroup addSynapseGroup(NeuronGroup source, NeuronGroup target) {
        SynapseGroup sg = SynapseGroup.createSynapseGroup(source, target);
        network.addGroup(sg);
        return sg;
    }

    public NeuronGroup addNeuronGroup(double x, double y, int numNeurons, String layoutName, NeuronUpdateRule rule) {

        NeuronGroup ng;
        ng = new NeuronGroup(network, new Point2D.Double(x, y), numNeurons);
        ng.setNeuronType(rule);

        layoutNeuronGroup(ng, x, y, layoutName);
        network.addGroup(ng);
        return ng;

    }

    public NeuronGroup addNeuronGroup(double x, double y, int numNeurons, String layoutName, String type) {
        return addNeuronGroup(x, y, numNeurons, layoutName, new LinearRule());
    }

    private void layoutNeuronGroup(NeuronGroup ng, double x, double y, String layoutName) {

        if (layoutName.toLowerCase().contains("line")) {
            if (layoutName.equalsIgnoreCase("vertical line")) {
                LineLayout lineLayout = new LineLayout(x, y, 50, LineOrientation.VERTICAL);
                ng.setLayout(lineLayout);
            } else {
                LineLayout lineLayout = new LineLayout(x, y, 50, LineOrientation.HORIZONTAL);
                ng.setLayout(lineLayout);
            }
        } else if (layoutName.equalsIgnoreCase("grid")) {
            GridLayout gridLayout = new GridLayout(GRID_SPACE, GRID_SPACE, (int) Math.sqrt(ng.size()));
            ng.setLayout(gridLayout);
        }
        ng.applyLayout();

    }

    public WinnerTakeAll addWTAGroup(double x, double y, int numNeurons) {
        WinnerTakeAll wta = new WinnerTakeAll(network, numNeurons);
        wta.setLocation(x, y);
        layoutNeuronGroup(wta, x, y, "line");
        return wta;
    }

    public Group addSubnetwork(double x, double y, int numNeurons, String type) {
        if (type.equalsIgnoreCase("wta")) {
            WinnerTakeAll ret = new WinnerTakeAll(network, numNeurons);
            network.addGroup(ret);
            return ret;
        }
        return null;

    }


    //TODO: Setting location not always working
    public NeuronGroup addNeuronGroup(double x, double y, int numNeurons) {
        return addNeuronGroup(x, y, numNeurons, "line", "LinearRule");
    }

    /**
     * Get a reference to a graphical {@link NeuronNode}.
     */
    public NeuronNode getNode(Neuron neuron) {
        return getNetworkPanel().getNode(neuron);
    }

    /**
     * Get a reference to the logical network object
     */
    public Network getNetwork() {
        return network;
    }

    /**
     * Get a reference to the network component.
     */
    public NetworkComponent getNetworkComponent() {
        return networkComponent;
    }

    /**
     * Get a reference to the network panel.
     */
    public NetworkPanelDesktop getNetworkPanel() {
        return (NetworkPanelDesktop) desktopComponent.getNetworkPanel();
    }


}
