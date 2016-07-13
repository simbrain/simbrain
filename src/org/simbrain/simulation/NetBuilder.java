package org.simbrain.simulation;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.simbrain.network.NetworkComponent;
import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.network.layouts.LineLayout.LineOrientation;
import org.simbrain.workspace.PotentialProducer;
import org.simbrain.world.odorworld.entities.RotatingEntity;
import org.simbrain.world.odorworld.sensors.SmellSensor;

//TODO: Not sure this is the best name.  Use it a while then decide.
//  Maybe change to NetHelper.   

/**
 * A wrapper for a NetworkComponent that makes it easy to add stuff to a
 * network.
 */
public class NetBuilder {

    private final NetworkComponent networkComponent;

    private final Network network;

    /**
     * @param networkComponent
     */
    public NetBuilder(NetworkComponent networkComponent) {
        this.networkComponent = networkComponent;
        this.network = networkComponent.getNetwork();
    }

    double GRID_SPACE = 50; // todo; make this settable

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
     */
    public void connect(Neuron source, Neuron target, double value) {
        Synapse synapse = new Synapse(source, target);
        //TODO: assume value > 0
        synapse.setLowerBound(-2 * value);
        synapse.setUpperBound(2 * value);
        synapse.setStrength(value);
        source.getNetwork().addSynapse(synapse);
    }


    public void connectAllToAll(NeuronGroup source, NeuronGroup target) {
        AllToAll connector = new AllToAll();
        connector.connectAllToAll(source.getNeuronList(),
                target.getNeuronList());
    }

    public SynapseGroup addSynapseGroup(NeuronGroup source,
            NeuronGroup target) {
        SynapseGroup sg = SynapseGroup.createSynapseGroup(source, target);
        network.addGroup(sg);
        return sg;
    }

    public NeuronGroup addNeuronGroup(int x, int y, int numNeurons,
            String layoutName, String type) {

        NeuronGroup ng = new NeuronGroup(network, new Point2D.Double(x, y),
                numNeurons);
        ng.setNeuronType(type);
        network.addGroup(ng);

        // LAYOUT NEURONS
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
