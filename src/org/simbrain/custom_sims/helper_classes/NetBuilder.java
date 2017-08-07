package org.simbrain.custom_sims.helper_classes;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.simbrain.network.NetworkComponent;
import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.connections.OneToOne;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.Group;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.network.layouts.LineLayout.LineOrientation;
import org.simbrain.network.subnetworks.WinnerTakeAll;

//TODO: Not sure this is the best name.  Use it a while then decide.
//  Maybe change to NetHelper.     Or NetworkBuilder

/**
 * A wrapper for a NetworkComponent that makes it easy to add stuff to a
 * network.
 */
public class NetBuilder {

    private final NetworkComponent networkComponent;

    private final Network network;

    private double GRID_SPACE = 50; // todo; make this settable

    /**
     * @param networkComponent
     */
    public NetBuilder(NetworkComponent networkComponent) {
        this.networkComponent = networkComponent;
        this.network = networkComponent.getNetwork();
    }

    public Neuron addNeuron(int x, int y) {
        Neuron neuron = new Neuron(network, "LinearRule");
        neuron.setLocation(x, y);
        network.addNeuron(neuron);
        return neuron;
    }

    public void addNeurons(int x, int y, int numNeurons, String layoutName,
            String type) {

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
                LineLayout lineLayout = new LineLayout(x, y, 50,
                        LineOrientation.VERTICAL);
                lineLayout.layoutNeurons(newNeurons);

            } else {
                LineLayout lineLayout = new LineLayout(x, y, 50,
                        LineOrientation.HORIZONTAL);
                lineLayout.layoutNeurons(newNeurons);
            }
        } else if (layoutName.equalsIgnoreCase("grid")) {
            GridLayout gridLayout = new GridLayout(GRID_SPACE, GRID_SPACE,
                    (int) Math.sqrt(numNeurons));
            gridLayout.setInitialLocation(new Point(x, y));
            gridLayout.layoutNeurons(newNeurons);
        }
    }

    /**
     * Make a single source -> target connection.
     *
     * @param source the source neuron
     * @param target the target neuron
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
    public void connect(Neuron source, Neuron target, double value) {
        Synapse synapse = new Synapse(source, target);
        synapse.setStrength(value);
        source.getNetwork().addSynapse(synapse);
    }

    public void connectOneToOne(NeuronGroup source, NeuronGroup target) {
        OneToOne connector = new OneToOne();
        connector.connectOneToOne(source.getNeuronList(),
                target.getNeuronList());
    }

    public void connectAllToAll(NeuronGroup source, NeuronGroup target) {
        AllToAll connector = new AllToAll();
        connector.connectAllToAll(source.getNeuronList(),
                target.getNeuronList());
    }

    public void connectAllToAll(NeuronGroup inputs, Neuron target) {
        AllToAll connector = new AllToAll();
        connector.connectAllToAll(inputs.getNeuronList(),
                Collections.singletonList(target));
    }

    public SynapseGroup addSynapseGroup(NeuronGroup source,
            NeuronGroup target) {
        SynapseGroup sg = SynapseGroup.createSynapseGroup(source, target);
        network.addGroup(sg);
        return sg;
    }

    public NeuronGroup addNeuronGroup(double x, double y, int numNeurons,
            String layoutName, String type) {

        NeuronGroup ng;

        if(type.equalsIgnoreCase("wta")) {
            ng = new WinnerTakeAll(network, numNeurons);
            ng.setLocation(x, y);
        } else {
            ng = new NeuronGroup(network, new Point2D.Double(x, y),
                    numNeurons);
            ng.setNeuronType(type);
        }
        network.addGroup(ng);

        // Lay out neurons
        if (layoutName.toLowerCase().contains("line")) {
            if (layoutName.equalsIgnoreCase("vertical line")) {
                LineLayout lineLayout = new LineLayout(x, y, 50,
                        LineOrientation.VERTICAL);
                ng.setLayout(lineLayout);
            } else {
                LineLayout lineLayout = new LineLayout(x, y, 50,
                        LineOrientation.HORIZONTAL);
                ng.setLayout(lineLayout);
            }
        } else if (layoutName.equalsIgnoreCase("grid")) {
            GridLayout gridLayout = new GridLayout(GRID_SPACE, GRID_SPACE,
                    (int) Math.sqrt(numNeurons));
            ng.setLayout(gridLayout);
        }
        ng.applyLayout();
        return ng;
    }

    public WinnerTakeAll addWTAGroup(double x, double y, int numNeurons) {
        return (WinnerTakeAll) addNeuronGroup(x, y, numNeurons, "line", "wta");
    }

    public Group addSubnetwork(double x, double y, int numNeurons, String type) {
        if(type.equalsIgnoreCase("wta")) {
            WinnerTakeAll ret = new WinnerTakeAll(network, numNeurons);
            network.addGroup(ret);
            return ret;
        }
        return  null;

    }

    public NeuronGroup addNeuronGroup(double x, double y, int numNeurons) {
        return addNeuronGroup(x, y, numNeurons, "line", "LinearRule");
    }

    public Network getNetwork() {
        return network;
    }

    /**
     * @return the networkComponent
     */
    public NetworkComponent getNetworkComponent() {
        return networkComponent;
    }

}
