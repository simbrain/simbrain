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

// TODO: Rename this?  VehicleBuilder?  VehicleHelper?

/**
 * A wrapper for a NetworkComponent that makes it easy to add stuff to a
 * network.
 */
public class Vehicle {

    private final Simulation sim;

    private final NetBuilder net;

    private final OdorWorldBuilder world;

    /**
     * @param net
     * @param world
     */
    public Vehicle(Simulation sim, NetBuilder net, OdorWorldBuilder world) {
        this.sim = sim;
        this.net = net;
        this.world = world;
    }

    // Encapsulate in a labelled neuron group
    
    // Generalize pursuer, avoider, etc.

    public void addPursuer(int x, int y, RotatingEntity agent,
            int stimulusDimension) {

        // Create the network
        Neuron leftTurn = net.addNeuron(x, y);
        leftTurn.setLabel("Left");
        leftTurn.setLowerBound(-10);
        leftTurn.setUpperBound(100);
        Neuron straight = net.addNeuron(x + 50, y);
        straight.setLabel("Straight");
        straight.setActivation(3);
        straight.setLowerBound(10);
        straight.setUpperBound(100);
        straight.setClamped(true);
        Neuron rightTurn = net.addNeuron(x + 100, y);
        rightTurn.setLabel("Right");
        rightTurn.setLowerBound(-10);
        rightTurn.setUpperBound(100);
        Neuron leftInput = net.addNeuron(x, y + 100);
        leftInput.setClamped(true);
        leftInput.setLowerBound(-10);
        leftInput.setUpperBound(100);
        Neuron rightInput = net.addNeuron(x + 100, y + 100);
        rightInput.setClamped(true);
        rightInput.setLowerBound(-10);
        rightInput.setUpperBound(100);

        // Set weights here
        net.connect(leftInput, leftTurn, 50);
        net.connect(rightInput, rightTurn, 50);

        // Couple network to agent
        sim.couple(agent.getSensor("Smell-Left"), stimulusDimension - 1,
                leftInput);
        sim.couple(agent.getSensor("Smell-Right"), stimulusDimension - 1,
                rightInput);
        sim.couple(straight, agent.getEffector("Go-straight"));
        sim.couple(leftTurn, agent.getEffector("Go-left"));
        sim.couple(rightTurn, agent.getEffector("Go-right"));

    }

}
