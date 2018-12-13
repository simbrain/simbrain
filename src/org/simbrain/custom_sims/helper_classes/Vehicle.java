package org.simbrain.custom_sims.helper_classes;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.world.odorworld.entities.EntityType;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.ObjectSensor;

// TODO: Rename this?  VehicleBuilder?  VehicleHelper?

/**
 * A custom class that makes it easy to add Braitenberg vehicles to a
 * simulation. A vehicle network is added to a network and it is coupled to a
 * corresponding sensor in a world.
 */
public class Vehicle {

    /**
     * The simulation object.
     */
    private final Simulation sim;

    /**
     * Reference to the network to put the vehicle in.
     */
    private final NetBuilder net;

    /**
     * The world with agents and sensors to attach to the vehicle.
     */
    private final OdorWorldBuilder world;

    /**
     * Size of sensor-motor weights. Determines how "sharply" agents turn.
     */
    private int weightSize = 250;

    /**
     * Size of weights from sensors to straight movement.
     */
    private int forwardWeights = 5;

    /**
     * If true connect sensor nodes to straight movement node.
     */
    private boolean connectSensorToStraightMovement = true;

    /**
     * What type of vehicle to add.
     */
    public enum VehicleType {
        PURSUER, AVOIDER
    }

    /**
     * Construct the vehicle builder. Needs a reference to the network to build
     * the network, the world to reference the sensor, and the sim to make the
     * couplings.
     *
     * @param sim   the parent simulation object
     * @param net   the network to add the vehicle subnetworks to
     * @param world the world to link the vehicles to
     */
    public Vehicle(Simulation sim, NetBuilder net, OdorWorldBuilder world) {
        this.sim = sim;
        this.net = net;
        this.world = world;
    }

    /**
     * Add a vehicle.
     *
     * @param x           x location
     * @param y           y location
     * @param agent       reference to the agent to couple to
     * @param vehicleType Pursuer, Avoider, etc.
     * @param objectType  what kind of object this vehicles pursues or avoids
     * @return a reference to the resulting neuron group
     */
    public NeuronGroup addVehicle(int x, int y, OdorWorldEntity agent, VehicleType vehicleType, EntityType objectType,
        ObjectSensor leftSensor, ObjectSensor rightSensor) {

        // Create the network
        NeuronGroup vehicle = new NeuronGroup(net.getNetwork());
        // These have to be updated first to update properly
        // unless priority is used
        Neuron leftInput = net.addNeuron(x, y + 100);
        leftInput.setLabel(objectType + " (L)");
        leftInput.setClamped(true);
        setNodeDefaults(leftInput, vehicle);
        Neuron rightInput = net.addNeuron(x + 100, y + 100);
        rightInput.setLabel(objectType + " (R)");
        rightInput.setClamped(true);
        setNodeDefaults(rightInput, vehicle);
        Neuron leftTurn = net.addNeuron(x, y);
        leftTurn.setLabel("Left");
        setNodeDefaults(leftTurn, vehicle);
        Neuron straight = net.addNeuron(x + 50, y);
        straight.setLabel("Speed");
        straight.setActivation(3);
        setNodeDefaults(straight, vehicle);
        straight.setClamped(true);
        Neuron rightTurn = net.addNeuron(x + 100, y);
        rightTurn.setLabel("Right");
        setNodeDefaults(rightTurn, vehicle);

        net.getNetwork().addGroup(vehicle);

        // Set weights here
        if (vehicleType == VehicleType.PURSUER) {
            net.connect(leftInput, leftTurn, weightSize, -2 * weightSize, 2 * weightSize);
            net.connect(rightInput, rightTurn, weightSize, -2 * weightSize, 2 * weightSize);
        } else if (vehicleType == VehicleType.AVOIDER) {
            net.connect(leftInput, rightTurn, weightSize, -2 * weightSize, 2 * weightSize);
            net.connect(rightInput, leftTurn, weightSize, -2 * weightSize, 2 * weightSize);
        }

        if (connectSensorToStraightMovement) {
            net.connect(leftInput, straight, forwardWeights);
            net.connect(rightInput, straight, forwardWeights);
        }

        // Couple network to agent.
        sim.couple(leftSensor, leftInput);
        sim.couple(rightSensor, rightInput);
        sim.couple(straight, agent.getEffector("Go-straight"));
        sim.couple(leftTurn, agent.getEffector("Go-left"));
        sim.couple(rightTurn, agent.getEffector("Go-right"));

        return vehicle;
    }

    /**
     * Add a pursuer.
     */
    public NeuronGroup addPursuer(int x, int y, OdorWorldEntity agent, EntityType objectType, ObjectSensor left, ObjectSensor right) {
        return addVehicle(x, y, agent, VehicleType.PURSUER, objectType, left, right);
    }

    /**
     * Add an avoider.
     */
    public NeuronGroup addAvoider(int x, int y, OdorWorldEntity agent, EntityType objectType, ObjectSensor left, ObjectSensor right) {
        return addVehicle(x, y, agent, VehicleType.AVOIDER, objectType, left, right);
    }

    /**
     * Helper method to set default value for vehicle nodes.
     *
     * @param neuron the neuron to update.
     * @param ng     the neuron group the node is in.
     */
    private void setNodeDefaults(Neuron neuron, NeuronGroup ng) {
        neuron.setLowerBound(-100);
        neuron.setUpperBound(200);
        ng.addNeuron(neuron);
    }

    /**
     * @return the weightSize
     */
    public int getWeightSize() {
        return weightSize;
    }

    /**
     * @param weightSize the weightSize to set
     */
    public void setWeightSize(int weightSize) {
        this.weightSize = weightSize;
    }

}
